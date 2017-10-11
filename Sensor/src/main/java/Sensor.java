import java.io.IOException;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/* A sensor uses 4 main threads:
 * 1) Token thread
 * 2) Command line thread
 * 3) Server thread
 * 4) Measurements thread
 *
 * The server thread uses a multithreading approach: when it receives a connection,
 * it creates a secondary thread for handle it
 */

public class Sensor {

	ServerSocket sensorSocket;
	SensorInfo precSensor;
	Socket nextSocket;
	String idNext;

	WebTarget gateway;
	Gson gson;

	/* Just for having an object for the wait-notify of the token thread */
	Object monitor;
	public Buffer<Measurement> measurementBuffer;
	volatile Token<Measurement> tok;
	final int tokSize = 15;

	public Client cl;
	public String idSensor;
	volatile boolean isRunning;

	public volatile boolean keepToken = false;

	public volatile boolean duringHandshake = false;

	Thread tokenThread;
	Thread clThread;
	Simulator simulator;
	Thread simThread;
	Thread pingThread;

	public boolean debug = false;

	/* This class is a singleton */

	private static Sensor instance;

	public static void deleteSensor(){
		instance = null;
	}

	public static Sensor getInstance(){
		if(instance == null)
			instance = new Sensor();
		return instance;
	}

	protected Sensor(){

	}

	public void initSensor(String gatewayAddress, int port,
			String typeSensor, String idSensor) throws IOException{
		gson = new GsonBuilder().create();
		isRunning = true;

		this.idSensor = idSensor;
		ClientConfig config = new ClientConfig();
	    cl = ClientBuilder.newClient(config);
	    gateway = cl.target("http://"+gatewayAddress+"/gateway/rest");

	    monitor = new Object();

	    tok = null;

	    measurementBuffer = getCorrectSimulator(typeSensor, idSensor);
	    sensorSocket = new ServerSocket(port);
	}

	private Buffer<Measurement> getCorrectSimulator(String s, String id){
		Buffer<Measurement> measurementBuffer = null;
		if(s.equals("temperatura")) {
			measurementBuffer = new SimpleBuffer<Measurement>();
			simulator = new TemperatureSimulator(id, measurementBuffer);
		} else if(s.equals("luminosita")) {
			measurementBuffer = new SimpleBuffer<Measurement>();
			simulator = new LightSimulator(id, measurementBuffer);
		} else if(s.equals("accellerometro")) {
			measurementBuffer = new SlidingWindowBuffer<Measurement>();
			simulator = new AccelerometerSimulator(id, measurementBuffer);
		} else {
			System.err.println("Can't understand the type, quit");
			System.exit(3);
		}
		return measurementBuffer;
	}

	private void registerSensorToGateway() throws IOException{
		//Gateway registration
		Form form = new Form();
		form.param("id", idSensor);
		form.param("host", sensorSocket.getInetAddress().getHostAddress());
		form.param("port", Integer.toString(sensorSocket.getLocalPort()));

		Response response = gateway.path("sensor").path("registerSensor").
					request().post(Entity.entity(form,MediaType.APPLICATION_FORM_URLENCODED), Response.class);

		if(response.getStatus() != 200){
			System.err.println("Sensor error (maybe ID already exists?)");
			//I don't have any thread, I can exit right now
			System.exit(1);
		}

		String listJSON = response.readEntity(String.class);

		Type listType = new TypeToken<List<SensorInfo>>(){}.getType();
		List<SensorInfo> array = gson.fromJson(listJSON,listType);

		// If I'm the only sensor in the network I have to create the token
		if(array.size() == 0){
			synchronized(monitor){
				MainSensore.printlnDebug("Creo token");
				tok = new Token<Measurement>(15);
				// I have the token: notify
				monitor.notifyAll();
			}
		}
		else{
			makeHandshakeNetwork(array.get(0));
		}
	}

	/* The protocol is:
	 * 1) Send ASK to N
	 * 2) N answers with RESP (P,S)
	 * 3) Send CH_SUCC to N with my address
	 * 4) Send CH_PREC to S with my address
	 */
	private void makeHandshakeNetwork(SensorInfo info) throws IOException{
		MainSensore.printlnDebug("Inizio handshake");
		SensorMessage m;
		Socket client = new Socket(info.hostSensor, info.portNumber);
		MainSensore.printlnDebug("Set prec socket to "+info.idSensor);
		precSensor = info;

		SensorMessage.createAskMessage().sendMessage(client);
		MainSensore.printlnDebug("Invio ASK");
		m = SensorMessage.receiveMessage(client);

		if(m.getTypeMessage() != SensorMessage.TypeMess.RESP){
			System.err.println("Aspettavo RESP, ricevuto "+m.getTypeMessage());
			System.exit(1);
		}
		MainSensore.printlnDebug("Ricevuto RESP");

		SensorInfo succ = m.succ;
		m = SensorMessage.createCH_SUCCMessage(new SensorInfo(idSensor,sensorSocket.getInetAddress(), sensorSocket.getLocalPort()));
		m.sendMessage(client);
		MainSensore.printlnDebug("Invio CH_SUCC");
		SensorMessage.createCLOSEMessage().sendMessage(client);
		MainSensore.printlnDebug("Invio CLOSE");
		client.close();

		Sensor.getInstance().idNext = succ.idSensor;
		MainSensore.printlnDebug("Set next socket to "+idNext);
		Sensor.getInstance().setNextSocket(new Socket(succ.hostSensor,succ.portNumber));
		MainSensore.printlnDebug("Invio CH_PREC");
		SensorMessage.createCH_PRECessage(new SensorInfo(idSensor,sensorSocket.getInetAddress(), sensorSocket.getLocalPort())).sendMessage(nextSocket);
		MainSensore.printlnDebug("Handshake completato!");
	}

	public void closeSensor(){
		// Devo effettuare la disconnesione dalla rete. Per fare ciò:
		// Invio CH_SUCC a precSensor con nextSensor
		// Invio CH_PREC a nextSensor con precSensor

		Response response = gateway.path("sensor").path("deleteSensor").
	    		path(idSensor).request().delete();

		if(response.getStatus() != 200){
			// Ci son stati degli errori durante la cancellazione,
			// chiudo urgentemente...
			System.err.println("Errore grave durante cancellazione, abort...");
			System.exit(response.getStatus());
		}

		try{
		// E' necessario fare questo check a parte perchè
		// se son l' unico nodo avrò sempre il token quindi
		// andrei in loop al check successivo
		if(nextSocket == null){
			//Son l' unico nodo, setto e chiudo
			isRunning = false;
			sensorSocket.close();
			return;
		}

		//Mi metto in attesa di non avere il token
		while(Sensor.getInstance().getToken() != null);

		isRunning = false;
		sensorSocket.close();
			// Ora posso occuparmi di avvisare gli altri nodi della mia chiusura
			synchronized(nextSocket){
				Socket precSocket = new Socket(precSensor.hostSensor, precSensor.portNumber);
				SensorInfo s;

				s = new SensorInfo(null,nextSocket.getInetAddress(),nextSocket.getPort());
				if(isEqual(s,precSocket))
					s = null;
				SensorMessage.createCH_SUCCMessage(s).sendMessage(precSocket);
				SensorMessage.createCLOSEMessage().sendMessage(precSocket);
				precSocket.close();

				s = precSensor;
				if(isEqual(s, nextSocket))
					s = null;
				SensorMessage.createCH_PRECessage(s).sendMessage(nextSocket);
				SensorMessage.createCLOSEMessage().sendMessage(nextSocket);
				nextSocket.close();
			}
		} catch (IOException e){
			MainSensore.printlnDebug("Someone didn't responde during CLOSE");
		}
	}

	private static boolean isEqual(SensorInfo s, Socket p){
		if(p.getInetAddress().equals(s.hostSensor) && p.getPort() == s.portNumber)
			return true;
		return false;

	}

	public void mainSensor(){
		try {
			registerSensorToGateway();
			tokenThread = new Thread(new HandleTokenThread());
			clThread = new Thread(new CommandLineInput());
			simThread = new Thread(simulator);
			pingThread = new Thread(new PingThread());

			simThread.start();
			tokenThread.start();
			clThread.start();
			pingThread.start();
			while(isRunning){
				Socket client = sensorSocket.accept();
				Thread serverThread = new Thread(new ServerThread(client));
				serverThread.start();
			}
		} catch (SocketException e) {
			// Se son qui significa che l' accept è fallita. Probabilmente
			// sensorSocket è stata chiusa, in ogni caso non posso accettare
			// nuove connessioni: chiudiamo tutto
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		// Devo terminare in maniera pulita i vari thread
		try {
			synchronized(Sensor.getInstance().getMonitor()){
				getMonitor().notifyAll();
		}
		MainSensore.printlnDebug("Inizio chiusura...");
		tokenThread.join();
		MainSensore.printlnDebug("TokenThread joined!");
		clThread.join();
		MainSensore.printlnDebug("clThread joined!");
		simulator.stopMeGently();
		simThread.join();
		MainSensore.printlnDebug("simThread joined!");

		pingThread.join();
		MainSensore.printlnDebug("pingThread joined!");
		} catch (InterruptedException e) {
				e.printStackTrace();
			}

	}

	/* Get-er e set-er per accedere al singleton dagli altri thread */

	public void handleCrashNode(){
		Response resp = gateway.path("sensor").path("crashSensor").request().get();
		boolean tokenAlive = false;
		boolean foundSuccessor = false;

	    String listJSON = resp.readEntity(String.class);

	    Type listSensorType = new TypeToken<List<SensorInfo>>(){}.getType();
	    List<SensorInfo> sensorList = gson.fromJson(listJSON,listSensorType);

	    SensorInfo next = null;

	    for(SensorInfo s : sensorList){
	    	MainSensore.printlnDebug("Testo con "+s.idSensor +"(next = "+idNext+", prec = "+precSensor.idSensor+")");
	    	if( s.idSensor.equals(idNext)){
	    		MainSensore.printlnDebug("Trovato next");
	    		next = s;
	    	}
	    }
	    if (next == null){
	    	MainSensore.printlnDebug("Errore, next è null!");
	    	System.exit(123);
	    }

	    MainSensore.printlnDebug("Invio cancellazione al gateway...");
	    resp = gateway.path("sensor").path("deleteSensor").path(idNext).request().delete();

	    //Devo gestire il caso particolare in cui son rimasto l' ultimo nodo

	    sensorList.remove(next);

	    if(Sensor.getInstance().getPrecSocket().idSensor.equals(next.idSensor)){
	    	MainSensore.printlnDebug("Rimasto ultimo nodo, setto...");
	    	//Significa che son nel caso in cui son rimasto solo io nella rete... check se ho il token, nel caso ricreo
	    	//e via che si va
	    	Sensor.getInstance().setNextSocket(null);
	    	Sensor.getInstance().setPrecSocket(null);
	    	if(getToken() == null){
		    	MainSensore.printlnDebug("Token scomparso");
		    	synchronized(monitor){
					MainSensore.printlnDebug("Creo token");
					tok = new Token<Measurement>(15);
					// Ho il token = notifico al thread di gestione che può lavorarci
					monitor.notifyAll();
				}
	    	}
	    	return;
	    }

	    for(SensorInfo s : sensorList){
	    	try {
	    		if(s.idSensor.equals(Sensor.getInstance().idSensor)) continue;
	    		MainSensore.printlnDebug("Chiedo a "+s.idSensor+"se era il predecessore di "+next.idSensor);
				Socket socket = new Socket(s.hostSensor, s.portNumber);
				SensorMessage.createASK_PRECMessage().sendMessage(socket);
				SensorMessage messResp = SensorMessage.receiveMessage(socket);
				if(messResp.prec.idSensor.equals(next.idSensor)){
					MainSensore.printlnDebug("trovato successore");
					foundSuccessor = true;
					socket = new Socket(s.hostSensor, s.portNumber);
					SensorMessage.createCH_PRECessage(new SensorInfo(idSensor,sensorSocket.getInetAddress(), sensorSocket.getLocalPort())).sendMessage(socket);
					Sensor.getInstance().setNextSocket(socket);
					Sensor.getInstance().idNext = s.idSensor;
				}
				if(messResp.hasToken == true){
					MainSensore.printlnDebug("trovato token");
					tokenAlive = true;
				}


			} catch (IOException e) {
				// Just ignoring it, some nodes are expected to not answer at this point
			}

	    }
	    if(!foundSuccessor){
	    	MainSensore.printlnDebug("Successore non trovato!");
	    	//uh, problem here!
	    	for(SensorInfo s : sensorList){
	    		Socket socket;
				try {
					gateway.path("sensor").path("deleteSensor").path(s.idSensor).request().delete();
					socket = new Socket(s.hostSensor, s.portNumber);
					SensorMessage.createRESTARTMessage().sendMessage(socket);
					socket.close();
				} catch (IOException e) {
					//Don't care, quit quit quit!
				}
	    	}
				return;



	    }

	    if(tokenAlive == false && getToken() == null){
	    	MainSensore.printlnDebug("Token scomparso");
	    	synchronized(monitor){
				MainSensore.printlnDebug("Creo token");
				tok = new Token<Measurement>(15);
				// Ho il token = notifico al thread di gestione che può lavorarci
				monitor.notifyAll();
			}
	    }
	}

	public synchronized Token getToken(){
		return tok;
	}

	public synchronized void setToken(Token t){
		tok = t;
	}

	public synchronized Socket getNextSocket(){
		return nextSocket;
	}

	public synchronized void setNextSocket(Socket s){
		nextSocket = s;
	}

	public synchronized void setPrecSocket(SensorInfo s){
		precSensor = s;
	}

	public synchronized SensorInfo getPrecSocket(){
		return precSensor;
	}


	public synchronized ServerSocket getServerSocket(){
		return sensorSocket;
	}

	public synchronized Object getMonitor(){
		return monitor;
	}

	public synchronized boolean isRunning(){
		return isRunning;
	}
}
