import java.io.IOException;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.util.List;
import java.util.Scanner;

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

public class MainClient {
	
	static Gson gson = new GsonBuilder().create();
	static String username;

	public static void main(String[] args){
		Scanner s = new Scanner(System.in);
		ServerSocket clientSocket = null;
		
		System.out.print("Indirizzo ip/porta gateway: ");
		String gatewayAddr = s.nextLine();
		String gatewayInfo = "http://"+gatewayAddr;
		
		ClientConfig config = new ClientConfig();
	    Client cl = ClientBuilder.newClient(config);
	    WebTarget gateway = cl.target(gatewayInfo+"/gateway/rest/client");
		
	    try {
			clientSocket = new ServerSocket(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
	    while(true){
	    
	    	while(true){
	    		System.out.print("Username: ");
	    		username = s.nextLine();
	    		if(username.length()>0){
	    			break;
	    		}
	    		System.out.println("Il nome utente deve avere almeno un carattere");
	    	}
	    
	    	Form form = new Form();
	    	form.param("username", username);
	    	form.param("host", clientSocket.getInetAddress().getHostAddress());
	    	form.param("port", Integer.toString(clientSocket.getLocalPort()));

	    	Response response = gateway.path("registerUser").
	    			request().post(Entity.entity(form,MediaType.APPLICATION_FORM_URLENCODED), Response.class);
		
	    	if(response.getStatus() == 200){
	    		break;
	    	}
	    	
	    	System.out.println("Errore nella registrazione (forse username gia esistente?)");
	    }
		
		System.out.println("Connesso a "+gatewayInfo+" con username \""+username+"\"");
		
		
		// Avvio il thread per le notifiche push...
		PushThread push = new PushThread(clientSocket);
		Thread pushNot = new Thread(push);
		pushNot.start();
		
		while(true){
			System.out.println("Che comando ?\n0 = esci\n1 = ritorna lista dei sensori\n2 = scollega sensore");
			System.out.println("3 = prendi la massima misurazione dal nodo");
			System.out.println("4 = Media delle misurazioni di un nodo");
			System.out.println("5 = Media delle misurazioni per tipo");
			System.out.println("6 = Crea nuovo sensore");
				
			System.out.println("Inserire comando: ");
			String input = s.nextLine();
			
			switch(Integer.parseInt(input)){
			case 0:
				closeUser(gateway);
				push.isRunning = false;
				try {
					clientSocket.close();
					pushNot.join();
				} catch (Exception e) {
					e.printStackTrace();
				}
				return;
			case 1:
				getNodeList(gateway);
				break;
			case 2:
				closeNode(gateway,s);
				break;
			case 3:
				getMaxNode(gateway,s);
				break;
			case 4:
				getAverageNode(gateway,s, "Node");
				break;
			case 5:
				getAverageNode(gateway,s, "Type");
				break;
			case 6:
				newSensor(gateway,s,gatewayAddr);
				break;
			}			
		}
	}
	
	private static void newSensor(WebTarget gateway, Scanner s, String gate) {
		System.out.println("Sensor name ?");
		String input = s.nextLine();
		System.out.println("Sensor type ?");
		String type = s.nextLine();
		System.out.println("Port ?");
		String port = s.nextLine();
		
		Form form = new Form();
    	form.param("name", input);
    	form.param("type", type);
    	form.param("port", port);
    	form.param("gateway", gate);

		
    	Response response = gateway.path("createNode").
    			request().post(Entity.entity(form,MediaType.APPLICATION_FORM_URLENCODED), Response.class);
		if(response.getStatus() != 200){
			System.err.println("Errore nella creazione nuovo nodo");
			return;
		}
	}
	
	private static void closeUser(WebTarget gateway) {
		
		Response resp = gateway.path("removeUser").path(username).request().delete();
		if(resp.getStatus() != 200){
			System.err.println("Errore, forse utente non esistente?");
			return;
		}
		
		System.out.println("Utente scollegato dal gateway");
	}
	
	private static void closeNode(WebTarget gateway, Scanner s) {
		System.out.println("che nodo?");
		String input = s.nextLine();
		
		Response resp = gateway.path("closeNode").path(input).request().delete();
		if(resp.getStatus() != 200){
			System.err.println("Errore nella chiusura del nodo, forse id non esistente?");
		}
	}

	public static void getNodeList(WebTarget gateway){
		Response resp = gateway.path("getSensorList").request().get();
		
		if(resp.getStatus() != 200){
			System.out.println("Errore");
			return;
		}
			
	    String listJSON = resp.readEntity(String.class);
	    
	    Type listSensorType = new TypeToken<List<SensorInfo>>(){}.getType();
	    List<SensorInfo> sensorList = gson.fromJson(listJSON,listSensorType);
	    
	    for(SensorInfo m : sensorList){
	    	System.out.println(m.idSensor+" (up for "+((System.currentTimeMillis()-m.creationTime)/1000)+" seconds)");
	    }   
	}
	
	public static void getAverageNode(WebTarget gateway, Scanner s,String type){
		System.out.println(type+" ?");
		String input = s.nextLine();
		System.out.println("From :");
		Long from = Long.parseLong(s.nextLine());
		System.out.println("To :");
		Long to = Long.parseLong(s.nextLine());
		
		Response resp = gateway.path("getAverage"+type).queryParam("id", input).queryParam("from", from ).queryParam("to", to).request().get();
		if(resp.getStatus() != 200){
			System.out.println("Errore");
			return;
		}	
		
		double d = gson.fromJson(resp.readEntity(String.class), Double.class).doubleValue();
		System.out.println("Media "+d);
	}
	
	public static void getMaxNode(WebTarget gateway, Scanner s){
		System.out.println("Che nodo?");
		String input = s.nextLine();
		
		Response resp = gateway.path("getRecentMeasurement").path(input).request().get();
		
		Measurement m = gson.fromJson(resp.readEntity(String.class), Measurement.class);
		System.out.println("M = "+m.getId()+" "+m.getType()+" "+m.getValue()+" "+m.getTimestamp());
	}
}
