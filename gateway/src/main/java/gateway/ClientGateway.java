package gateway;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Path("/client")
public class ClientGateway {

	@GET
@Produces(MediaType.TEXT_HTML)
public String sayHtmlHello() {
	return "<html> " + "<title>" + "Hello Jersey" + "</title>"
			+ "<body><h1>" + "Hello Jersey" + "</body></h1>" + "</html> ";
}

	  @POST
	  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	  @Path("/registerUser")
	  public Response registerUser(@FormParam("username") String username,
			  @FormParam("host") String host, @FormParam("port") int port) {

		if(UserList.getInstance().addUser(new User(username,host,port)) == false)
			return Response.status(409).build();
		return Response.ok().build();
	  }

	  @DELETE
	  @Path("/removeUser/{user}")
	  public Response registerUser(@PathParam("user") String username) {

		  if(UserList.getInstance().removeUser(username) == false)
			  return Response.status(201).build();
		  return Response.ok().build();
	  }

	  @DELETE
	  @Path("closeNode/{id}")
	  public Response closeNode(@PathParam("id") String idSensor) throws IOException{
		  SensorInfo s = SensorNetwork.getInstance().getSensor(idSensor);
		  if(s == null)
			  return Response.status(403).build();

		  Socket sock = new Socket(s.hostSensor, s.portNumber);
		  SensorMessage.createDELMessage(null).sendMessage(sock);
		  sock.close();

		  return Response.ok().build();
	  }

	  @POST
	  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	  @Path("createNode")
	  public Response createNode(@FormParam("name") String idSensor,
			  @FormParam("type") String type, @FormParam("port") String port,
			  @FormParam("gateway") String gate) throws IOException{


		  System.out.println("cerco di creare "+idSensor+" di tipo "+type);

		  String[] startOptions = new String[] {"java", "-jar", "/Users/panzone/Sensore.jar",type, idSensor, port, gate};
		  new ProcessBuilder(startOptions).start();

		  return Response.ok().build();
	  }


	  @GET
	  @Produces({MediaType.APPLICATION_JSON})
	  @Path("/getMeasurements")
	  public Response getMesuraments() {
		  List<Measurement> retList = MeasurementDatabase.getInstance().getDatabaseCopy();
		  Gson gs = new GsonBuilder().create();
		  return Response.ok(gs.toJson(retList)).build();
	  }


	  @GET
	  @Produces({MediaType.APPLICATION_JSON})
	  @Path("/getSensorList")
	  public Response getSensorNetwork() {
		  List<SensorInfo> retList = SensorNetwork.getInstance().getCopySensorNetwork();
		  Gson gs = new GsonBuilder().create();
		  return Response.ok(gs.toJson(retList)).build();
	  }

	  @GET
	  @Produces({MediaType.APPLICATION_JSON})
	  @Path("/getRecentMeasurement/{id}")
	  public Response getRecentMeasurement(@PathParam("id") String idSensor) {
		  List<Measurement> retList = MeasurementDatabase.getInstance().getDatabaseCopy();
		  Measurement ret = null;
		  for(Measurement m: retList){
			 if(m.getId().equals(idSensor)){
				 if(ret == null)
					 ret = m;
				 else if(m.getTimestamp()> ret.getTimestamp())
					 ret = m;
			 }
		  }
		  Gson gs = new GsonBuilder().create();
		  return Response.ok(gs.toJson(ret)).build();
		  }

	  @GET
	  @Produces({MediaType.APPLICATION_JSON})
	  @Path("/getAverageNode")
	  public Response getAverageSensor(@QueryParam("id") String idSensor,
			  @QueryParam("from") Long from,
			  @QueryParam("to") Long to) {
		  List<Measurement> retList = MeasurementDatabase.getInstance().getDatabaseCopy();
		  double average = 0;
		  int sum = 0;
		  if(from > to)
			  Response.status(409).build();

		  for(Measurement m: retList){
			  if(m.getId().equals(idSensor))
				  if(m.getTimestamp() >= from.longValue() && m.getTimestamp() <= to.longValue()){
					  average += Double.parseDouble(m.getValue());
					  sum++;
				  }
		  }
		  average /= sum;
		  if(sum == 0)
			  return Response.status(404).build();
		  Double r = new Double(average);

		  Gson gs = new GsonBuilder().create();
		  return Response.ok(gs.toJson(r)).build();
	}

	  @GET
	  @Produces({MediaType.APPLICATION_JSON})
	  @Path("/getAverageType")
	  public Response getAverageType(@QueryParam("id") String type,
			  @QueryParam("from") Long from,
			  @QueryParam("to") Long to) {
		  List<Measurement> retList = MeasurementDatabase.getInstance().getDatabaseCopy();
		  double average = 0;
		  int sum = 0;
		  if(from > to)
			  Response.status(409).build();

		  for(Measurement m: retList){
			  if(m.getType().equals(type))
				  if(m.getTimestamp() >= from.longValue() && m.getTimestamp() <= to.longValue()){
					  average += Double.parseDouble(m.getValue());
					  sum++;
				  }
		  }
		  if(sum == 0)
			  return Response.status(404).build();
		  average /= sum;
		  Double r = new Double(average);

		  Gson gs = new GsonBuilder().create();
		  return Response.ok(gs.toJson(r)).build();
		  }



	  public static void sendPushNotifications(String type, String s) {
		  for(User u: UserList.getInstance().getListCopy()){
			  Socket pushNot;
			try {
				pushNot = new Socket(u.host,u.port);
				  Gson gson = new GsonBuilder().create();
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(pushNot.getOutputStream()));
					writer.write(gson.toJson(type+s));
					writer.newLine();
					writer.flush();
					pushNot.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		  }

	  }
}
