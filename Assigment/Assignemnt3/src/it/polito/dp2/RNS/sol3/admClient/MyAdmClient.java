package it.polito.dp2.RNS.sol3.admClient;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import it.polito.dp2.RNS.ConnectionReader;
import it.polito.dp2.RNS.GateReader;
import it.polito.dp2.RNS.GateType;
import it.polito.dp2.RNS.ParkingAreaReader;
import it.polito.dp2.RNS.PlaceReader;
import it.polito.dp2.RNS.RoadSegmentReader;
import it.polito.dp2.RNS.VehicleReader;
import it.polito.dp2.RNS.VehicleState;
import it.polito.dp2.RNS.VehicleType;
import it.polito.dp2.RNS.lab3.AdmClient;
import it.polito.dp2.RNS.lab3.ServiceException;
import it.polito.dp2.RNS.lab3.UnknownPlaceException;
import it.polito.dp2.RNS.lab3.WrongPlaceException;
import it.polito.dp2.RNS.sol3.admClient.library.MyPlaceType;
import it.polito.dp2.RNS.sol3.admClient.library.MyVehicleType;
import it.polito.dp2.RNS.sol3.jaxb.admClient.EnumVehicleState;
import it.polito.dp2.RNS.sol3.jaxb.admClient.EnumVehicleType;
import it.polito.dp2.RNS.sol3.jaxb.admClient.IdentifiedEntityType;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.CurrentVehicles;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.Vehicle;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.Vehicles;

public class MyAdmClient implements AdmClient{
	private static String serviceBaseUri = System.getProperty("it.polito.dp2.RNS.lab3.URL");
	//private AdmDB db = AdmDB.getAdmDB(); 
	private Client client;
	private AdmDB db;
	
	public  MyAdmClient() throws ServiceException{
		if (serviceBaseUri == null) {
            String servicePort = System.getProperty("PORT");
            if (servicePort == null) {
                servicePort = "8080";
            }
            serviceBaseUri = "http://localhost:" + servicePort + "/RnsSystem/rest";
        }
		//InitAdmClient initAdmClient = InitAdmClient.getAdmInit();
		client = ClientBuilder.newClient();
		
		InitAdmClient initClient = new InitAdmClient();
		db = initClient.getDB();
		return;
	}

	@Override
	public Set<ConnectionReader> getConnections() {
		// TODO Auto-generated method stub
		return db.getRns().getConnections();
	}

	@Override
	public Set<GateReader> getGates(GateType type) {
		// TODO Auto-generated method stub
		return db.getRns().getGates(type);
	}

	@Override
	public Set<ParkingAreaReader> getParkingAreas(Set<String> services) {
		// TODO Auto-generated method stub
		return db.getRns().getParkingAreas(services);
	}

	@Override
	public PlaceReader getPlace(String placeId) {
		// TODO Auto-generated method stub
		PlaceReader place = db.getRns().getPlace(placeId);
		if (place == null)
			try {
				throw new UnknownPlaceException();
			} catch (UnknownPlaceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return place;
	}

	@Override
	public Set<PlaceReader> getPlaces(String prefixId) {
		// TODO Auto-generated method stub
		return db.getRns().getPlaces(prefixId);
	}

	@Override
	public Set<RoadSegmentReader> getRoadSegments(String roadName) {
		// TODO Auto-generated method stub
		return db.getRns().getRoadSegments(roadName);
	}

	@Override
	public VehicleReader getVehicle(String plateId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<VehicleReader> getVehicles(Calendar since, Set<VehicleType> types, VehicleState state) {
		// TODO Auto-generated method stub
		return new HashSet<VehicleReader> ();
	}

	@Override
	public Set<VehicleReader> getUpdatedVehicles(String place) throws ServiceException {
		// TODO Auto-generated method stub
		if (place==null)
			return allVehiclesRequest();
		else
			return currentVehiclesRequest(place);
	}

	@Override
	public VehicleReader getUpdatedVehicle(String id) throws ServiceException {
		// TODO Auto-generated method stub
		return vehicleRequest(id);
	}
	
	public VehicleReader vehicleRequest(String plateId) throws ServiceException{
		//get base info of the web service (all link to principals resources)
		Response serverResponse = client.target(UriBuilder.fromUri(db.getRnsInfoUri().getVehicles()).path("/"+plateId).build())
                .request()
                .accept(MediaType.APPLICATION_XML)
                .get();
		
		if (serverResponse.getStatusInfo().getStatusCode() != 200){
			// error
			if (serverResponse.getStatusInfo().getStatusCode() >= 500)
				throw new ServiceException();
			if (serverResponse.getStatusInfo().getStatusCode() == 404)
				return null;
			if (serverResponse.getStatusInfo().getStatusCode() == 400)
				return null;
		}
		Vehicle vehicle = serverResponse.readEntity(Vehicle.class);
		it.polito.dp2.RNS.sol3.jaxb.admClient.VehicleType v = new it.polito.dp2.RNS.sol3.jaxb.admClient.VehicleType();
		v = fillVehicleInfo(vehicle);
		MyVehicleType myVehicle = new MyVehicleType(v,db.getRns().getMappingNamePlace());
		return myVehicle;
	}
	
	public  Set<VehicleReader> allVehiclesRequest() throws ServiceException{
		Response serverResponse = client.target(UriBuilder.fromUri(db.getRnsInfoUri().getVehicles()).path("").build())
                .request()
                .accept(MediaType.APPLICATION_XML)
                .get();
		
		if (serverResponse.getStatusInfo().getStatusCode() != 200){
			// error
			if (serverResponse.getStatusInfo().getStatusCode() >= 500)
				throw new ServiceException();
			
			return null;
		}
		
		Vehicles vehicles = serverResponse.readEntity(Vehicles.class);
		if (vehicles.getVehicle().isEmpty())
			return new HashSet<VehicleReader>();
		Set<VehicleReader> vehicleList = new HashSet<VehicleReader>();
		for (Vehicle vehicle:vehicles.getVehicle()){
			it.polito.dp2.RNS.sol3.jaxb.admClient.VehicleType v = new it.polito.dp2.RNS.sol3.jaxb.admClient.VehicleType();
			v = fillVehicleInfo(vehicle);
			MyVehicleType myVehicle = new MyVehicleType(v,db.getRns().getMappingNamePlace());
			vehicleList.add(myVehicle);
		}
		return vehicleList;
	}
	
	public  Set<VehicleReader> currentVehiclesRequest(String placeId) throws ServiceException{
		Response serverResponse = client.target(UriBuilder.fromUri(db.getRnsInfoUri().getPlaces()).path("/currentVehicles").build())
                .request()
                .accept(MediaType.APPLICATION_XML)
                .get();
		
		if (serverResponse.getStatusInfo().getStatusCode() != 200){
			// error
			if (serverResponse.getStatusInfo().getStatusCode() >= 500)
				throw new ServiceException();
			if (serverResponse.getStatusInfo().getStatusCode() == 404)
				//throw new UnknownPlaceException();
				return null;
		}
		
		Vehicles vehicles = serverResponse.readEntity(Vehicles.class);
		if (vehicles.getVehicle().isEmpty())
			return new HashSet<VehicleReader>();
		Set<VehicleReader> vehicleList = new HashSet<VehicleReader>();
		for (Vehicle vehicle:vehicles.getVehicle()){
			it.polito.dp2.RNS.sol3.jaxb.admClient.VehicleType v = new it.polito.dp2.RNS.sol3.jaxb.admClient.VehicleType();
			v = fillVehicleInfo(vehicle);
			MyVehicleType myVehicle = new MyVehicleType(v,db.getRns().getMappingNamePlace());
			vehicleList.add(myVehicle);
		}
		return vehicleList;
	}
	
	
	public it.polito.dp2.RNS.sol3.jaxb.admClient.VehicleType fillVehicleInfo(Vehicle vehicle){
		it.polito.dp2.RNS.sol3.jaxb.admClient.VehicleType v = new it.polito.dp2.RNS.sol3.jaxb.admClient.VehicleType();;
		IdentifiedEntityType ie = new IdentifiedEntityType();
		ie.setId(vehicle.getPlateId());
		v.setIdentifiedEntity(ie);
		v.setOrigin(vehicle.getOrigin());
		v.setEntryTime(vehicle.getEntryTime());
		v.setPosition(vehicle.getPosition().getPlaceId());
		v.setVehicleState(EnumVehicleState.valueOf(vehicle.getState().getVehicleState()));
		v.setDestination(vehicle.getDestination());
		v.setVehicleType(EnumVehicleType.valueOf(vehicle.getVehicleType()));
		return v;
	}

}
