import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CommandLineInput implements Runnable {

	@Override
	public void run() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		try{
			/*
			 * Avoid locking this thread...
			 */
			while(Sensor.getInstance().isRunning()){
				while(!br.ready()){
					Thread.sleep(100);
					if(Sensor.getInstance().isRunning == false)
						return;
				}

			String input = br.readLine();
			if(input.equals("exit")){
					Sensor.getInstance().closeSensor();
			}
			else
				System.out.println("Comando non compreso, usa exit per chiudere il nodo");
		}
		} catch (IllegalStateException e){

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
