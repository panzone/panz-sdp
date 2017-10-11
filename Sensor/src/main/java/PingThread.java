import java.io.IOException;
import java.net.Socket;

public class PingThread implements Runnable {

	
	@Override
	public void run(){
		
		while(Sensor.getInstance().isRunning()){
			try {
				Thread.sleep(5000);
				if(Sensor.getInstance().isRunning() == false) break;
		
			if(Sensor.getInstance().getNextSocket()!= null){
					SensorMessage.createPINGMessage().sendMessage(Sensor.getInstance().getNextSocket());
				} }catch (IOException e) {
					//Cant' send the ping, handle this...
					MainSensore.printlnDebug("Percepito crash, inizio procedura ripristino...");
					Sensor.getInstance().handleCrashNode();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	
}