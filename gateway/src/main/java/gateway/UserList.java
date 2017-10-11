package gateway;

import java.util.ArrayList;
import java.util.List;

public class UserList {

	
	ArrayList<User> memory;
	
	private static UserList database;
	
	protected UserList(){
		memory = new ArrayList<User>();
	}
	
	public static UserList getInstance(){
		if(database == null)
			database = new UserList();
		return database;
	}
	
	
	public boolean addUser(User u){
		synchronized(memory){
			if(containsUser(u)){
				return false;
			}
			memory.add(u);
		}
		return true;
	}
	
	public boolean removeUser(String u){
		synchronized(memory){
			for(User us : memory){
				if(us.username.equals(u)){
					memory.remove(us);
					return true;
				}
			}
		}
		return false;
	}
	
	public User getUser(String name){
		synchronized(memory){
			for(User us : memory){
				if(us.username.equals(name)){
					return us.clone();
				}
			}
		}
		return null;
		
	}
	
	private boolean containsUser(User u){
		for(User us : memory){
			if(us.username.equals(u.username)){
				return true;
			}
		}
		return false;
	}
	
	
	public List<User> getListCopy(){
		List<User> ret = null;
		synchronized(memory){
			ret = (List<User>) memory.clone();
		}
		return ret;
	}
}
