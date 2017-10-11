import java.io.IOException;
import java.util.ArrayList;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/* This thread handles the token. It uses a wait-signal: this thread waits until
 * the server thread doesn't receive a token. The server thread then send the token
 * this thread and uses a signal.
 */

public class HandleTokenThread implements Runnable {

	@Override
	public void run() {
		while(Sensor.getInstance().isRunning()){

			synchronized(Sensor.getInstance().getMonitor()){
				try {
					while(Sensor.getInstance().getToken() == null){
						Sensor.getInstance().getMonitor().wait();
						//Potrei essere stato risvegliato per uscire...
						if(Sensor.getInstance().isRunning==false)
							return;
					}
				// Just for debug!!
					if(Sensor.getInstance().debug)
						Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			ArrayList<Measurement> list = (ArrayList<Measurement>) Sensor.getInstance().measurementBuffer.readAllAndClean();
			for(Measurement t : list){
				if(Sensor.getInstance().getToken().getNumElemToken() >= 15)
					sendTokenToGateway();
				try {
					Sensor.getInstance().getToken().addToToken(t);
				} catch (Exception e) {
					System.err.println("Token pieno, impossibile salvare l' elemento");
				}
			}
			if(Sensor.getInstance().getNextSocket() == null)
				continue;

			synchronized(Sensor.getInstance().getNextSocket()){
				try {
					Token t;
					synchronized(Sensor.getInstance().getToken()){
						t = Sensor.getInstance().getToken();
						while(Sensor.getInstance().keepToken == true);
						SensorMessage.createTOKMessage(t).sendMessage(Sensor.getInstance().getNextSocket());
						MainSensore.printlnDebug("Ho mandato TOK");
						Sensor.getInstance().setToken(null);
					}
				} catch (IOException e) {
					System.err.println("Impossibile inviare token, inizio procecura di recupero");
					Sensor.getInstance().handleCrashNode();
				}
			}
		}
	}

	private void sendTokenToGateway(){
		ArrayList<Measurement> tokMeasurement = (ArrayList<Measurement>) Sensor.getInstance().getToken().getAllElemToken();
		for(Measurement m : tokMeasurement){
			Gson gs = new GsonBuilder().create();
			String json = gs.toJson(m);
			Form form = new Form();
			form.param("mes", json);

			try{
			Response response = Sensor.getInstance().gateway.path("sensor").path("sendMeasurement").
					request().post(Entity.entity(form,MediaType.APPLICATION_FORM_URLENCODED), Response.class);


			if(response.getStatus()!= 200){
				System.err.println("Gateway ha rifiutato i dati inviati, li scarto.");
			}
			MainSensore.printlnDebug("Inviata misurazione al gateway!");

			} catch(Exception e){
				System.err.println("Qualche errore nell' invio del token al gateway, chiudo");
				Sensor.getInstance().closeSensor();
			}
		}
	}

}
