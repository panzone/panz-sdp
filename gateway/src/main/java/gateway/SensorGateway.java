package gateway;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Path("/sensor")
public class SensorGateway {
	
	  @POST
	  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	  @Produces({MediaType.APPLICATION_JSON})
	  @Path("/registerSensor")
	  public Response registerSensor(@FormParam("id") String idSensor, 
			  @FormParam("host") String str, @FormParam("port") int portNumber) throws UnknownHostException {
		List<SensorInfo> retList;
		SensorInfo s = null;
		Gson gs = new GsonBuilder().create();
		
		synchronized(SensorNetwork.getInstance()){
			s = new SensorInfo(idSensor, InetAddress.getByName(str),portNumber);
			s.creationTime = System.currentTimeMillis();
			retList = SensorNetwork.getInstance().getCopySensorNetwork();
			if(SensorNetwork.getInstance().addSensor(s) == false)
				return Response.status(403).build();
		}
	    ClientGateway.sendPushNotifications("I",s.idSensor);
	    return Response.ok(gs.toJson(retList)).build();
	  }
	  
	  @DELETE
	  @Path("/deleteSensor/{id}")
	  public Response deleteSensor(@PathParam("id") String idSensor) {
		  System.out.println("Request to delete "+idSensor);
		SensorInfo s = SensorNetwork.getInstance().removeSensor(idSensor);
		if(s!=null)
			ClientGateway.sendPushNotifications("R",s.idSensor);
	    return Response.ok().build();
	  }
	  
	  @GET
	  @Path("/crashSensor")
	  public Response crashSensor() {
		 List<SensorInfo> sn = SensorNetwork.getInstance().getCopySensorNetwork();
		 Gson gs = new GsonBuilder().create();
		  
	    return Response.ok(gs.toJson(sn)).build();
	  }
	  
	  @POST
	  @Path("/sendMeasurement")
	  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	  public Response receiveMeasurement(@FormParam("mes") String mes) {
		  Gson gs = new GsonBuilder().create();
		  Measurement m = gs.fromJson(mes, Measurement.class);
		  MeasurementDatabase.getInstance().putMeasurement(m);		
	    return Response.ok().build();
	  }
} 