package it.polito.dp2.RNS.sol3.service.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import it.polito.dp2.RNS.ConnectionReader;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.*;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.SuggestedPath.Path;



public class RnsDB {
	private static RnsDB rnsDB = new RnsDB();

	private HashMap<String,Place> placeById; 
	private ConcurrentHashMap<String,Vehicle> vehicleById;
	private ConcurrentHashMap<String,SuggestedPath> suggestedPathByVehicle;
	//private ConcurrentHashMap<String,Place> vehiclesByPlace;
	private Set<Connection> connections;
	private List<String> gatesId,roadSegmentsId,parkingAreasId;
	
	private RnsDB() {
		placeById = new HashMap<String,Place>();
		vehicleById = new ConcurrentHashMap<String,Vehicle>();
		suggestedPathByVehicle = new  ConcurrentHashMap<String,SuggestedPath>();
		connections = new HashSet<Connection>();
		gatesId = new ArrayList<String>();
		roadSegmentsId = new ArrayList<String>();
		parkingAreasId = new ArrayList<String>();
		
	}
	
	public static RnsDB getRnsDB() {
		return rnsDB;
	}
	
	/*------ INIT FUNCTIONS ------*/
	
	public boolean addPlace(Place p){
		String placeId = p.getPlaceId();
		if (placeById.containsKey(placeId)){
			//the place is already in the system
			return false;
		}else{
			//the place is added to the DB
			Place tmpPlace = p;
			tmpPlace.setCurrentVehiclesLink(null);
			placeById.put(placeId, tmpPlace);
			if (p.getGate()!= null){
				//This place is a gate
				gatesId.add(placeId);
			}else{
				if (p.getParkingArea()!=null){
					//This place is a parking area
					parkingAreasId.add(placeId);
				}else{
					if (p.getRoadSegment()!=null){
						//This place is a road segment
						roadSegmentsId.add(placeId);
					}else{
						//error
					}
				}
			}
			return true;
		}
	}
	
	public boolean addConnection(String from,String to){
		//if ((placeById.contains(from))&&(placeById.contains(to))){
			Connection tmpConn = new Connection();
			tmpConn.setFrom(from);
			tmpConn.setFromLink(null);
			tmpConn.setTo(to);
			tmpConn.setToLink(null);
			connections.add(tmpConn);
			return true;
		//}else{
			//from or to place not present in the DB
			//return false;
		//}
	}
	
	//this method is thread-safe -> putIfAbsent is atomic
	public boolean addVehicle(Vehicle vehicle){
		String plateId = vehicle.getPlateId();
		if (vehicleById.putIfAbsent(plateId, vehicle)==null)
			return true; //no previous value -> vehicle is added
		else
			return false;
		/*if (vehicleById.contains(plateId)){
			return false;
		}
		vehicleById.put(plateId, vehicle);		
		return true;*/	
	}
	
	//this method need synchronization (if a vehicle is deleted while update suggestedpath vehicleById can be false)
	public synchronized  boolean setSuggestedPath (String plateId,SuggestedPath sp) {
		if (vehicleById.containsKey(plateId)==true){
			suggestedPathByVehicle.put(plateId, sp);
			return true;
		}
		return false;
	}
	
	
	
	
	/*------ END INIT FUNCTIONS ------*/
	
	
/* GET on places not need synch -> read only access*/
	public Collection<Place> getPlaces(){
		if (placeById.isEmpty()){
			return null;
		}
		return placeById.values();
	}
	
	public Collection<Connection> getConnections(){
		if (connections.isEmpty())
			return null;
		Set<Connection> tmpConnections = new HashSet<Connection>();
		for (Connection c : connections){
			tmpConnections.add(c);
		}
		return tmpConnections;
	}
	
	public Collection<Place> getGates(){
		Set<Place> gates = new HashSet<Place>();
		for (String id : gatesId){
			Place gate;
			gate = placeById.get(id);
			gates.add(gate);
		}
		if (gates.isEmpty())
			return null;
		return gates;
	}
	
	public Collection<Place> getParkingAreas(){
		Set<Place> parkingAreas = new HashSet<Place>();
		for (String id : parkingAreasId){
			Place parkingArea;
			parkingArea = placeById.get(id);
			parkingAreas.add(parkingArea);
		}
		if (parkingAreas.isEmpty())
			return null;
		return parkingAreas;
	}
	
	public Collection<Place> getRoadSegments(){
		Set<Place> roadSegments = new HashSet<Place>();
		for (String id : roadSegmentsId){
			Place roadSegment;
			roadSegment = placeById.get(id);
			roadSegments.add(roadSegment);
		}
		if (roadSegments.isEmpty())
			return null;
		return roadSegments;
	}
	
	public Place getPlace(String placeId){
		Place place = placeById.get(placeId);
		if (place==null)
			return null;
		else
			return place;
	}	
/* -------------------------------------------------*/
	
	public synchronized Collection<Vehicle> getCurrentVehiclesByPlaceId(String placeId){
		if (vehicleById.isEmpty())
			return null;
		Set<Vehicle> vehicles = new HashSet<Vehicle>();
		for (Vehicle v : vehicleById.values()){
			String p = v.getPosition().getPlaceId();
			if (p.equals(placeId))
				vehicles.add(v);
		}
		return vehicles;
	}
	
	
	
	//thread-safe method -> return the vehicle if present and null if absent (atomic)
	public Vehicle getVehicle(String plateId){
		return vehicleById.get(plateId);
		/*Vehicle vehicle = vehicleById.get(plateId);
		if (vehicle==null)
			return null;
		else
			return vehicle;*/
	}
	
	public synchronized SuggestedPath getSuggestedPathByPlateId(String plateId){
		if (suggestedPathByVehicle.containsKey(plateId)){
			return suggestedPathByVehicle.get(plateId);
		}else{
			if (vehicleById.containsKey(plateId)){
				// there is not suggested path return a empty sugg path
				Vehicle v = vehicleById.get(plateId);
				SuggestedPath sp = new SuggestedPath();
				sp.setStartId(v.getPosition().getPlaceId());
				sp.setEndId(v.getDestination());
				//path is null
				return sp;
			}else{
				//vehicle not found
				return null;
			}
		}
	}
	
	public synchronized Collection<Vehicle> getVehicles(){
		if (vehicleById.isEmpty())
			return null;
		else
			return vehicleById.values();
	}
	
	//this method need synchronization because delete a vehicle need a delete in two different structures
	public synchronized boolean deleteVehicle(String plateId){
		if (vehicleById.containsKey(plateId)){
			vehicleById.remove(plateId);
			suggestedPathByVehicle.remove(plateId);
			return true;
		}else{
			return false;
		}
	}
	
	public synchronized boolean changeState(String plateId,State newState){
		Vehicle oldVehicle = vehicleById.get(plateId);
		if (oldVehicle!=null){
			Vehicle newVehicle = oldVehicle;
			newVehicle.setState(newState);
			return vehicleById.replace(plateId, oldVehicle, newVehicle);
		}else
			return false;
	}
	
	public synchronized boolean changePositionWithoutSuggestedPath(String plateId,Position newPosition){
		Vehicle oldVehicle = vehicleById.get(plateId);
		if (oldVehicle!=null){
			Vehicle newVehicle = oldVehicle;
			newVehicle.setPosition(newPosition);
			return vehicleById.replace(plateId, oldVehicle, newVehicle);
		}else
			return false;
	}
	
	//this methods need synchronization because update both vehicle and suggestedPath maps
	public synchronized boolean changePositionWithSuggestedPath(SuggestedPath newSp,String plateId,Position newPosition){
		Vehicle oldVehicle = vehicleById.get(plateId);
		if (oldVehicle!=null){
			Vehicle newVehicle = oldVehicle;
			newVehicle.setPosition(newPosition);
			if (vehicleById.replace(plateId, oldVehicle, newVehicle)==true){
				suggestedPathByVehicle.replace(plateId, newSp);
				return true;
			}
		}
		return false;
	}
	
	
	
	
	
	
	
}
