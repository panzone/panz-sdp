package gateway;

public class User implements Cloneable {

	String username;
	String host;
	int port;
	
	public User(String u, String h, int p){
		username = u;
		h = host;
		port = p;
	}
	
	public String getUsername(){
		return username;
	}
	
	public String getHost(){
		return host;
	}
	
	public int getPortNumber(){
		return port;
	}
	
	public User clone(){
		User r = new User(username,host,port);
		return r;
	}
	
	
}
