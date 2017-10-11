package gateway;
import java.util.ArrayList;
import java.util.List;

public class Token<T> {

	ArrayList<T> memory;
	int maxElem;
	
	public Token(int sizeMax){
		maxElem = sizeMax;
		memory = new ArrayList<T>(sizeMax);
	}

	public void addToToken(T m) throws Exception{
		if(memory.size() < maxElem)
			memory.add(m);
		else
			throw new Exception();
	}
	
	public T removeFromToken(){
		if(memory.size()> 0)
			return memory.remove(0);
		return null;
	}
	
	public int getNumElemToken(){
		return memory.size();
	}
	
	public List<T> getAllElemToken(){
		ArrayList<T> tmp;
		tmp = (ArrayList<T>) memory.clone();
		memory.clear();
		return tmp;
	}
	
	public String toString(){
		String s = "";
		for(T m :memory){
			s+=m;
			s+=", ";
		}
		return s;
	}
}
