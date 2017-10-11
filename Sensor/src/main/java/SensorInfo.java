import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class SensorInfo {
	public int portNumber;
	public InetAddress hostSensor;
	public String idSensor;
	public long creationTime;
	
	public SensorInfo(String id, InetAddress host, int port){
		idSensor = id;
		hostSensor = host;
		portNumber = port;
	}
	
	public Socket connect() throws IOException{
		Socket s = new Socket(hostSensor,portNumber);
		return s;
	}
	
}
