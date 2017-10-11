import java.util.ArrayList;
import java.util.List;

public class SlidingWindowBuffer<T> implements Buffer<Measurement> {
	
	ArrayList<Measurement> bufferMemory;
	ArrayList<Measurement> tmpBuffer;
	volatile long lastUpdate;
	
	public SlidingWindowBuffer(){
		bufferMemory = new ArrayList<Measurement>();
		tmpBuffer =  new ArrayList<Measurement>();
	}
	
	
	@Override
	public synchronized void add(Measurement t) {	
		tmpBuffer.add(t);

		synchronized(bufferMemory){
			if(System.currentTimeMillis() - lastUpdate > 1000){
				double average = 0;
				for(Measurement m: tmpBuffer)
					average = Double.parseDouble(m.getValue());
				
				average/=tmpBuffer.size();
				Measurement mes = new Measurement(t.getId(), t.getType(), Double.toString(average), t.getTimestamp());
				bufferMemory.add(mes);
				tmpBuffer.clear();
				lastUpdate = System.currentTimeMillis();
			}
		}
		
	}

	@Override
	public List<Measurement> readAllAndClean() {
		List<Measurement> tmp;
		synchronized(bufferMemory){
			tmp = (List<Measurement>) bufferMemory.clone();
			bufferMemory.clear();
		}
		return tmp;
	}

}
