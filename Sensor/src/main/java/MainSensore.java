import java.io.IOException;


public class MainSensore {
	public static volatile boolean restart = true;

	public static void main(String[] args) {
		if(args.length > 5){
			System.err.println("Numero argomenti scorretto");
			return;
		}
		while(restart){
			restart = false;
			if(args.length == 5 && args[4].equals("debug")){
				Sensor.getInstance().debug = true;
			}
		try {
			Sensor.getInstance().initSensor(args[3],Integer.parseInt(args[2]), args[0], args[1] );
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
		Sensor.getInstance().mainSensor();
		if(restart == true){
			Sensor.deleteSensor();
		}
	}
	
	}
	
	public static void printlnDebug(String s){
		if(Sensor.getInstance().debug)
			System.out.println(s);
	}
}
