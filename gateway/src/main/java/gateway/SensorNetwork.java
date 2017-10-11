package gateway;

import java.util.ArrayList;
import java.util.List;

public class SensorNetwork {

	private static SensorNetwork sn;
	ArrayList<SensorInfo> sensorNetwork;
	
	protected SensorNetwork(){
		sensorNetwork = new ArrayList<SensorInfo>();
	}
	
	public static SensorNetwork getInstance(){
		if(sn == null)
			sn = new SensorNetwork();
		return sn;
	}
	
	public boolean addSensor(SensorInfo s){
		synchronized(sensorNetwork){
			if(containsSensor(s.idSensor))
				return false;
			sensorNetwork.add(s);
		}
		return true;
	}
	
	public SensorInfo getSensor(String id){
		for(SensorInfo si : sensorNetwork){
			if(si.idSensor.equals(id)){
				return si;
			}
		}
		return null;
	}
	
	public synchronized SensorInfo removeSensor(String s){
		for(SensorInfo si : sensorNetwork){
			if(si.idSensor.equals(s)){
				sensorNetwork.remove(si);
				return si;
			}
		}
		return null;
	}
	
	public boolean containsSensor(String id){
		for(SensorInfo s: sensorNetwork){
			if(s.idSensor.equals(id))
				return true;
		}
		return false;
	}
	
	public List<SensorInfo> getCopySensorNetwork(){
		return (List<SensorInfo>) sensorNetwork.clone();
	}
	
}
