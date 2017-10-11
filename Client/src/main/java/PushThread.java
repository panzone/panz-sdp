import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class PushThread implements Runnable {

	ServerSocket clientSocket;
	public volatile boolean isRunning;
	
	public PushThread(ServerSocket s){
		clientSocket = s;
	}
	
	@Override
	public void run() {
		isRunning = true;
		try {
		while(isRunning){
		
				Socket gateway = clientSocket.accept();
				
				BufferedReader reader;
				String j = "";
				Gson gson = new GsonBuilder().create();
				try {
					reader = new BufferedReader(new InputStreamReader(gateway.getInputStream()));
					j = reader.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
			    String mes = gson.fromJson(j, String.class);
			    if(mes.startsWith("I"))
			    	System.out.println("Sensore "+mes.substring(1)+" connesso alla rete");
			    else
			    	System.out.println("Sensore "+mes.substring(1)+" sconnesso alla rete");
				gateway.close();
				
			}
				
			} catch (IOException e) {
	
			}
	}

}
