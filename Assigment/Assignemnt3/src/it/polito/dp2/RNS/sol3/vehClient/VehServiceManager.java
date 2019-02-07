package it.polito.dp2.RNS.sol3.vehClient;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import it.polito.dp2.RNS.VehicleState;
import it.polito.dp2.RNS.VehicleType;
import it.polito.dp2.RNS.lab3.EntranceRefusedException;
import it.polito.dp2.RNS.lab3.ServiceException;
import it.polito.dp2.RNS.lab3.UnknownPlaceException;
import it.polito.dp2.RNS.lab3.WrongPlaceException;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.Position;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.Rns;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.State;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.SuggestedPath;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.SuggestedPath.Path;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.Vehicle;

public class VehServiceManager {
	private static String serviceBaseUri = System.getProperty("it.polito.dp2.RNS.lab3.URL");
	private Client client;
	private Rns rnsInfo;
	private SuggestedPath currentSuggestedPath;
	private Vehicle vehicle;
	private String currentPosition = null;
	
	public VehServiceManager() throws ServiceException{
		if (serviceBaseUri == null) {
            String servicePort = System.getProperty("PORT");
            if (servicePort == null) {
                servicePort = "8080";
            }
            serviceBaseUri = "http://localhost:" + servicePort + "/RnsSystem/rest";
        }
		client = ClientBuilder.newClient();
		loadInfoResources();
	}
	
	
	public void loadInfoResources() throws ServiceException{
		//get base info of the web service (all link to principals resources)
		Response serverResponse = client.target(UriBuilder.fromUri(serviceBaseUri).path("/rns").build())
                .request()
                .accept(MediaType.APPLICATION_XML)
                .get();
		
		if (serverResponse.getStatusInfo().getStatusCode() != 200){
			// error -> the server is not reachable
			throw new ServiceException();
		}
		
		rnsInfo = serverResponse.readEntity(Rns.class);
		return;
	}
	
	//this method send an enter request to the system, if the operation successed send a get to the vehicle and save the related info
	public List<String> enterRequest(String plateId, VehicleType type, String inGate, String destination) throws ServiceException, WrongPlaceException, UnknownPlaceException, EntranceRefusedException{
		//create the request
		Vehicle v = new Vehicle();
		v.setPlateId(plateId);
		v.setVehicleType(type.value());
		v.setOrigin(inGate);
		v.setDestination(destination);
		
		Response serverResponse = client.target(UriBuilder.fromUri(rnsInfo.getVehicles()).path("/"+plateId).build())
                .request()
                .accept(MediaType.APPLICATION_XML)
                .put(Entity.xml(v));
		
		if (serverResponse.getStatusInfo().getStatusCode() != 200){
			// error
			if (serverResponse.getStatusInfo().getStatusCode() >= 500)
				throw new ServiceException();
			if (serverResponse.getStatusInfo().getStatusCode() == 403)
				throw new WrongPlaceException();
			if (serverResponse.getStatusInfo().getStatusCode() == 400)
				throw new UnknownPlaceException();
			//example 404 bad request -> all others case are entrance refused
			throw new EntranceRefusedException();
		}
		SuggestedPath suggestedPath = serverResponse.readEntity(SuggestedPath.class);
		List<String> path = new ArrayList<String>();
		if (suggestedPath.getPath().isEmpty()==false){
			for (Path p : suggestedPath.getPath())
				path.add(p.getPalceId());
		}
		currentSuggestedPath = suggestedPath;
		
		getVehicleFromService(plateId); //read the vehicle info from the service and save information in main memory
		currentPosition = inGate;
		return path;
	}
	
	//send a get to the server to read the information about the vehicle
	private void getVehicleFromService(String plateId) throws ServiceException{

		Response serverResponse = client.target(UriBuilder.fromUri(rnsInfo.getVehicles()).path("/"+plateId).build())
                .request()
                .accept(MediaType.APPLICATION_XML)
                .get();
		
		if (serverResponse.getStatusInfo().getStatusCode() != 200){

			// error
			if (serverResponse.getStatusInfo().getStatusCode() >= 500)
				throw new ServiceException();
			if (serverResponse.getStatusInfo().getStatusCode() == 404)
				vehicle = null; //vehicle not found
				
		}
		
		vehicle = serverResponse.readEntity(Vehicle.class);
		return;
	}
	
	//this method send a request of change position to the server -> a suggestedPath is returned in case of success
	public List<String> moveRequest(String newP) throws ServiceException, WrongPlaceException, UnknownPlaceException{
		//create the request
		Position newPosition = new Position();
		newPosition.setPlaceId(newP);

		Response serverResponse = client.target(UriBuilder.fromUri(vehicle.getCurrentPositionLink()).build())
                .request()
                .accept(MediaType.APPLICATION_XML)
                .put(Entity.xml(newPosition));
		

		if (serverResponse.getStatusInfo().getStatusCode() != 200){
			// error
			if (serverResponse.getStatusInfo().getStatusCode() >= 500)
				throw new ServiceException();
			if (serverResponse.getStatusInfo().getStatusCode() == 403){
				List<String> path = new ArrayList<String>();
				for (Path p : currentSuggestedPath.getPath()){
					path.add(p.getPalceId());
				}

				return null;
			}
			if (serverResponse.getStatusInfo().getStatusCode() == 400)
				throw new UnknownPlaceException();
		}
		//the vehicle succesfully change the position -> suggested path is returned
		SuggestedPath newSuggestedPath = serverResponse.readEntity(SuggestedPath.class);
		
		List<String> path = new ArrayList<String>();
		if (newSuggestedPath.getPath().isEmpty()==false){
			for (Path p : newSuggestedPath.getPath()){
				path.add(p.getPalceId());
			}
			currentPosition = newP;
			if (newSuggestedPath.getStartId().equals(currentSuggestedPath.getStartId())&&newSuggestedPath.getEndId().equals(currentSuggestedPath.getEndId()))
				return null;
				
			currentSuggestedPath = newSuggestedPath;
			return path;
		}

		currentPosition = newP;
		return path;
	}
	
	//this method send to the server a request of change state
	public void changeStateRequest(VehicleState newState) throws ServiceException{
		
		State newStateData = new State();
		newStateData.setVehicleState(newState.value());
		
		Response serverResponse = client.target(UriBuilder.fromUri(vehicle.getChangeStateLink()).build())
                .request()
                .accept(MediaType.APPLICATION_XML)
                .put(Entity.xml(newStateData));
		
		if (serverResponse.getStatusInfo().getStatusCode() != 200){
			// error
			if (serverResponse.getStatusInfo().getStatusCode() >= 500)
				throw new ServiceException();
			if (serverResponse.getStatusInfo().getStatusCode() == 404)
				return; //vehicle not found
		}
		//operation successed
		vehicle.setState(newStateData); //update local info
		return;
	}
	
	//this method send a request to the server to exit the system from the outGate.
	//if the vehicle is already in the outGate send only the exit request, otherwise before try to move
	public void exitRequest(String outGate) throws ServiceException, WrongPlaceException, UnknownPlaceException{
		System.out.println("exit request with current position " + currentPosition + " by "+vehicle.getPlateId());
		if (currentPosition.equals(outGate)==false){
			moveRequest(outGate); //the vehicle is not currently in the outGate position, then before try to move there			
		}//this move can create a UnknownPlaceException

		Response serverResponse = client.target(UriBuilder.fromUri(vehicle.getCurrentPositionLink()).build())
                .request()
                .accept(MediaType.APPLICATION_XML)
                .delete();
		

		if (serverResponse.getStatusInfo().getStatusCode() != 200){
			// error
			if (serverResponse.getStatusInfo().getStatusCode() >= 500)
				throw new ServiceException();
			if (serverResponse.getStatusInfo().getStatusCode() == 403) //forbidden -> is not out/inout gate
				throw new WrongPlaceException();
			if (serverResponse.getStatusInfo().getStatusCode() == 404) //not found -> vehicle is not in the system
				throw new ServiceException(); // vehicle not found
			
		}
		//success operation
		return;
	}
}
