import java.util.ArrayList;
import java.util.List;

public class SimpleBuffer<T> implements Buffer<T> {

	ArrayList<T> bufferMemory;
	
	public SimpleBuffer(){
		bufferMemory = new ArrayList<T>();
	}
	
	@Override
	public void add(T t) {
		synchronized(bufferMemory){
			bufferMemory.add(t);
		}
	}

	@Override
	public List<T> readAllAndClean() {
		ArrayList<T> tmp;
		synchronized(bufferMemory){
			tmp = (ArrayList<T>) bufferMemory.clone();
			bufferMemory.clear();
		}
		return tmp;
	}
}
