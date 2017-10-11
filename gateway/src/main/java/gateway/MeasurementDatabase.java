package gateway;

import java.util.ArrayList;
import java.util.List;

public class MeasurementDatabase {

	
	ArrayList<Measurement> memory;
	
	private static MeasurementDatabase database;
	
	protected MeasurementDatabase(){
		memory = new ArrayList<Measurement>();
	}
	
	public static MeasurementDatabase getInstance(){
		if(database == null)
			database = new MeasurementDatabase();
		return database;
	}
	
	
	public void putMeasurement(Measurement m){
		synchronized(memory){
			memory.add(m);
		}
	}
	
	public List<Measurement> getDatabaseCopy(){
		List<Measurement> ret = null;
		synchronized(memory){
			ret = (List<Measurement>) memory.clone();
		}
		return ret;
	}
	
	
	
}
