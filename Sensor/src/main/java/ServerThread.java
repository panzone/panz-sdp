import java.io.IOException;
import java.net.Socket;

public class ServerThread implements Runnable {

	Socket client;

	public ServerThread(Socket c){
		client = c;
	}

	@Override
	public void run(){
		try {
			while(Sensor.getInstance().isRunning()){
				SensorMessage mes = SensorMessage.receiveMessage(client);

				if(mes == null){
					// If I receive a null it means that there is a problem with the
					// client socket... better close it.
					client.close();
					return;
				}

				MainSensore.printlnDebug("Received "+mes.typeMessage);
				switch(mes.typeMessage){
					case ASK:
						respondeASK();
						return;
					case RESP:
						break;
					case CH_SUCC:
						respondeCH_SUCC(mes.succ);
						break;
					case CH_PREC:
						respondeCH_PREC(mes.prec);
						break;
					case TOK:
						synchronized(Sensor.getInstance().monitor){
							Sensor.getInstance().setToken(mes.tok);
							Sensor.getInstance().monitor.notifyAll();
						}
						break;
					case RESTART:
						Sensor.getInstance().closeSensor();
						client.close();
						MainSensore.restart = true;
						return;
					case DEL:
						Sensor.getInstance().closeSensor();
						client.close();
						return;
					case CLOSE:
						client.close();
						return;
					case PING:
						SensorMessage.createPONGMessage().sendMessage(client);
						break;
					case PONG:
						break;
					case ASK_PREC:
						respondeASK_PREC();
						client.close();
						return;
					default:
						System.err.println("Messaggio malformattato, ignoro...");
						client.close();
						return;
					}
				}
			}
		catch (IOException e) {

			}
	}

	private synchronized void respondeASK() throws IOException{
		while(Sensor.getInstance().duringHandshake == true);
		while(Sensor.getInstance().getToken() == null );


		Sensor.getInstance().keepToken = true;
		Sensor.getInstance().duringHandshake = true;
		SensorInfo s = null;
		if(Sensor.getInstance().getNextSocket() == null)
			s = new SensorInfo(Sensor.getInstance().idSensor, Sensor.getInstance().getServerSocket().getInetAddress(), Sensor.getInstance().getServerSocket().getLocalPort());
		else
			s = new SensorInfo(Sensor.getInstance().idNext, Sensor.getInstance().getNextSocket().getInetAddress(), Sensor.getInstance().getNextSocket().getPort()    );
		if(Sensor.getInstance().getPrecSocket()!=null){
		MainSensore.printlnDebug("Send RESP with ("+Sensor.getInstance().getPrecSocket().idSensor+","+s.idSensor+")");}
		SensorMessage sm = SensorMessage.createRESPMessage(Sensor.getInstance().getPrecSocket(), s);
		sm.sendMessage(client);


		sm = SensorMessage.receiveMessage(client);
		if(sm.typeMessage != SensorMessage.TypeMess.CH_SUCC){
			System.err.println("Aspettavo CH_SUCC, chiudo");
			System.exit(2);
		}
		respondeCH_SUCC(sm.succ);
		sm = SensorMessage.receiveMessage(client);

		//if(sm.typeMessage != SensorMessage.TypeMess.CLOSE)
		//	System.err.println("Aspettavo close, problemi nell' handshake");
		client.close();

		Sensor.getInstance().keepToken = false;
		Sensor.getInstance().duringHandshake = false;
	}

	private synchronized void respondeCH_SUCC(SensorInfo s) throws IOException{
		if(Sensor.getInstance().getNextSocket() != null){
			SensorMessage.createCLOSEMessage().sendMessage(Sensor.getInstance().getNextSocket());
			Sensor.getInstance().getNextSocket().close();
		}
		if(s==null || s.idSensor.equals(Sensor.getInstance().idSensor)){
			Sensor.getInstance().idNext = null;
			Sensor.getInstance().setNextSocket(null);
		}
		else{
			Sensor.getInstance().idNext = s.idSensor;
			Sensor.getInstance().setNextSocket(new Socket(s.hostSensor, s.portNumber));
			}
	}

	private synchronized void respondeCH_PREC(SensorInfo p){
		Sensor.getInstance().setPrecSocket(p);
	}

	private synchronized void respondeASK_PREC() throws IOException{
		SensorMessage sm = SensorMessage.createRESP_PRECMessage(Sensor.getInstance().getPrecSocket(), (Sensor.getInstance().tok != null)? true: false );
		sm.sendMessage(client);
	}
}
