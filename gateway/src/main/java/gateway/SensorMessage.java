package gateway;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SensorMessage {

	public enum TypeMess {
	    ASK, RESP, CH_SUCC, CH_PREC, TOK, DEL, CLOSE, PING, PONG, ASK_PREC, RESP_PREC, RESTART
	}
	
	public TypeMess typeMessage;
	
	public Token<Measurement> tok;	
	SensorInfo prec;
	SensorInfo succ;
	boolean hasToken;
	
	public SensorMessage(TypeMess t){
		typeMessage = t;
		tok = null;
		prec = succ = null;
		hasToken = false;
	}
	

	
	public SensorMessage(TypeMess type, SensorInfo p, SensorInfo s){
		typeMessage = type;
		prec = p;
		succ = s;
		tok = null;
	}
	
	public TypeMess getTypeMessage(){
		return typeMessage;
	}
	
	public static SensorMessage createAskMessage(){
		SensorMessage sm = new SensorMessage(TypeMess.ASK);
		return sm;
	}
	
	public static SensorMessage createRESPMessage(SensorInfo p, SensorInfo s){
		SensorMessage sm = new SensorMessage(TypeMess.RESP);
		sm.prec = p;
		sm.succ = s;
		return sm;
	}
	
	public static SensorMessage createCH_SUCCMessage(SensorInfo s){
		SensorMessage sm = new SensorMessage(TypeMess.CH_SUCC);
		sm.succ = s;
		return sm;
	}
	
	public static SensorMessage createCH_PRECessage(SensorInfo p){
		SensorMessage sm = new SensorMessage(TypeMess.CH_PREC);
		sm.prec = p;
		return sm;
	}
	
	public static <T> SensorMessage createTOKMessage(Token<Measurement> t){
		SensorMessage sm = new SensorMessage(TypeMess.TOK);
		sm.tok = t;
		return sm;
	}
	
	public static <T> SensorMessage createDELMessage(SensorInfo s){
		SensorMessage sm = new SensorMessage(TypeMess.DEL);
		sm.prec = s;
		return sm;
	}
	
	public static <T> SensorMessage createCLOSEMessage(){
		SensorMessage sm = new SensorMessage(TypeMess.CLOSE);
		return sm;
	}
	
	
	public void sendMessage(Socket client) throws IOException{		
		Gson gson = new GsonBuilder().create();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
		writer.write(gson.toJson(this));
		writer.newLine();
		writer.flush();
	}
	
	public static SensorMessage receiveMessage(Socket client) throws IOException{
		BufferedReader reader;
		String j = "";
		Gson gson = new GsonBuilder().create();
		reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
		j = reader.readLine();
	    SensorMessage mes = gson.fromJson(j, SensorMessage.class);
	    return mes;
	}
	
	
	public boolean isTokenMessage(){
		if(typeMessage.equals("token"))
			return true;
		return false;
	}



	public static <T> SensorMessage createPINGMessage() {
		SensorMessage sm = new SensorMessage(TypeMess.PING);
		return sm;
		
	}
	public static <T> SensorMessage createPONGMessage() {
		SensorMessage sm = new SensorMessage(TypeMess.PONG);
		return sm;
		
	}
	
	public static <T> SensorMessage createASK_PRECMessage() {
		SensorMessage sm = new SensorMessage(TypeMess.ASK_PREC);
		return sm;
		
	}
	
	public static <T> SensorMessage createRESTARTMessage() {
		SensorMessage sm = new SensorMessage(TypeMess.RESTART);
		return sm;
		
	}
	
	public static <T> SensorMessage createRESP_PRECMessage(SensorInfo s, boolean hasT) {
		SensorMessage sm = new SensorMessage(TypeMess.RESP_PREC);
		sm.prec = s;
		sm.hasToken = hasT;
		return sm;
		
	}
	
}
