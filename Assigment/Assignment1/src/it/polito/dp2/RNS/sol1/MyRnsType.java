package it.polito.dp2.RNS.sol1;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.polito.dp2.RNS.ConnectionReader;
import it.polito.dp2.RNS.GateReader;
import it.polito.dp2.RNS.GateType;
import it.polito.dp2.RNS.ParkingAreaReader;
import it.polito.dp2.RNS.PlaceReader;
import it.polito.dp2.RNS.RnsReader;
import it.polito.dp2.RNS.RoadSegmentReader;
import it.polito.dp2.RNS.VehicleReader;
import it.polito.dp2.RNS.VehicleState;
import it.polito.dp2.RNS.sol1.jaxb.*;
import javafx.print.PageOrientation;

public class MyRnsType implements RnsReader{
	private RnsType rns;
	private Set<VehicleReader> vehicleSet;	
	private Set<PlaceReader> placeSet; 
	private Set<GateReader> gateSet;
	private Set<RoadSegmentReader> roadSegmentSet;
	private Set<ParkingAreaReader> parkingAreaSet;
	private Set<ConnectionReader> connectionsSet;
	private Map<String,PlaceType> mappingNamePlace;
	private Map<String,VehicleType> mappingNameVehicle;
	
	public MyRnsType(RnsType r){
		this.rns = r;
		mappingPlaces();
		mappingVehicles();
		vehicleSet = new HashSet<VehicleReader> ();
		placeSet = new HashSet<PlaceReader> ();
		gateSet = new HashSet<GateReader> ();
		roadSegmentSet = new HashSet<RoadSegmentReader>();
		parkingAreaSet = new HashSet<ParkingAreaReader>();
		connectionsSet = new HashSet<ConnectionReader>();
		
		//initialize all data
		loadPlace();
		loadGate();
		loadParkingArea();
		loadRoadSegment();
		loadVehicle();
		loadConnection();
	}


	@Override
	public Set<ConnectionReader> getConnections() {
		// TODO Auto-generated method stub
		if (connectionsSet == null)
			return null;
		return connectionsSet;
	}


	@Override
	public Set<GateReader> getGates(GateType type) {
		// TODO Auto-generated method stub
		//If type == null return all gates
		if ((type == null)){
			return gateSet;
		}else{
			//otherwise return all the gates with type = type
			Set<GateReader> selectedGateSet = new HashSet<GateReader>();
			for ( GateReader gate : gateSet){
				if (gate.getType().equals(type.value())){
					selectedGateSet.add(gate);
				}			
			}
			return selectedGateSet;	
		}	
	}


	@Override
	public Set<ParkingAreaReader> getParkingAreas(Set<String> service) {
		// TODO Auto-generated method stub
		if (service == null){
			//if the set of services is null return all the parkingareas
			return parkingAreaSet;
		}else{	
			Set<ParkingAreaReader> selectedPA = new HashSet<ParkingAreaReader>();
			for ( ParkingAreaReader pa : parkingAreaSet){
				if (pa.getServices().containsAll(service)==true){
					selectedPA.add(pa);
				}
			}
			if (selectedPA.isEmpty())
				return null;
			else
				return selectedPA;
		}
	}


	@Override
	public PlaceReader getPlace(String placeId) {
		// TODO Auto-generated method stub
		if ((placeId == null)||(placeId.equals("")))
			return null;
		//Search the place name in tha hashmap (placeName,placeType)
		PlaceType place = mappingNamePlace.get(placeId);
		if (place != null){
			return new MyPlaceType(place,mappingNamePlace);
		}
		//if don't find any place with this id return null
		return null;
	}


	@Override
	public Set<PlaceReader> getPlaces(String prefixId) {
		// TODO Auto-generated method stub
		if ((prefixId == null)||(prefixId.equals(""))){
			//if prefixId is null return all the places
			return placeSet;
		}else{
			//if prefixId is not null return all place with prefix that start with prefixId
			Set<PlaceReader> selectedPlaceSet = new HashSet<PlaceReader>();
			for ( PlaceReader place : placeSet){
				String id = place.getId();
				if (id.startsWith(prefixId)){
					selectedPlaceSet.add(place);
				}			
			}
			if (selectedPlaceSet.isEmpty()){
				// if don't find any place with this prefix return null
				return null;
			}else{
				//return only the place that start with prefixId
				return selectedPlaceSet;
			}
		}
	}

	@Override
	public Set<RoadSegmentReader> getRoadSegments(String roadName) {
		// TODO Auto-generated method stub
		//If roadName == null return all RoadSegments of all roads
		if ((roadName == null)||(roadName.equals(""))){
			return roadSegmentSet;
		}else{
			//otherwise return all the gates with type = type
			Set<RoadSegmentReader> selectedRoadSegmentSet = new HashSet<RoadSegmentReader>();
			for ( RoadSegmentReader rs : roadSegmentSet){
				if (rs.getRoadName().equals(roadName)){
					selectedRoadSegmentSet.add(rs);
				}			
			}
			if (selectedRoadSegmentSet.isEmpty()){
				return null;
			}else{
				return selectedRoadSegmentSet;	
			}
		}	
	}


	@Override
	public VehicleReader getVehicle(String vehicleId) {
		// TODO Auto-generated method stub
		if ((vehicleId == null)||(vehicleId.equals("")))
			return null;
		//Search the place name in tha hashmap (placeName,placeType)
		VehicleType vehicle = mappingNameVehicle.get(vehicleId);
		if (vehicle != null){
			return new MyVehicleType(vehicle,mappingNamePlace);
		}
		//if don't find any place with this id return null
		return null;
	}


	@Override
	public Set<VehicleReader> getVehicles(Calendar since, Set<it.polito.dp2.RNS.VehicleType> types, VehicleState state) {
		// TODO Auto-generated method stub
		if(vehicleSet==null||vehicleSet.isEmpty()){
			return new HashSet<VehicleReader>();
		}else{
			Set<VehicleReader> stateSelection = new HashSet<VehicleReader>();
			//selcted by STATE
			if (state==null){
				stateSelection = vehicleSet;
			}else{
				for (VehicleReader v : vehicleSet){
					if (v.getState().equals(state.value()))
						stateSelection.add(v);
				}
			}
			if (stateSelection == null)
				return null;
			//selected by TYPES
			Set<VehicleReader> typesSelection = new HashSet<VehicleReader>(); 
			if (types == null){
				typesSelection = stateSelection;
			}else{
				for (VehicleReader v : stateSelection){
					for (it.polito.dp2.RNS.VehicleType t : types){
						if (v.getState().equals(t.value()))
							typesSelection.add(v);
						}
				}
			}
			Set<VehicleReader> selectedVehicle = new HashSet<VehicleReader>(); 
			if (since == null){
				selectedVehicle = typesSelection;
			}else{
				for (VehicleReader v : typesSelection){
					if (v.getEntryTime().after(since)||v.getEntryTime().equals(since))
						selectedVehicle.add(v);
				}
			}
			return selectedVehicle;	
		}
	}
	
/*===========================================================================================================
 * 											LOAD METHODS
 ============================================================================================================*/
	//All these methods read the RNS and load in the properly set all the data structures
	
	//this method load in the placeSet data structure all the places beloning the RNS
	public void loadPlace(){
		List<PlaceType> placeList = this.rns.getPlace();
		for(PlaceType p: placeList) {
			placeSet.add(new MyPlaceType(p,mappingNamePlace));
		}
	return;
	}
	//this method load in the roadSegmentSet data structure all the roadSegments beloning the RNS
	public void loadRoadSegment(){
		List<PlaceType> placeList = this.rns.getPlace();
		for(PlaceType p: placeList) {
			if ((p.getRoadSegment()!=null)){
				roadSegmentSet.add(new MyRoadSegmentType(p,p.getRoadSegment(),mappingNamePlace));
			}
		}
	return;
	}
	//this method load in the gateSet data structure all the gates beloning the RNS
	public void loadGate(){
		List<PlaceType> placeList = this.rns.getPlace();
		for(PlaceType p: placeList) {
			if ((p.getGate()!=null)&&(p.getGate()!= "")){
				gateSet.add(new MyGateType(p,p.getGate(),mappingNamePlace));
			}
		}
	return;
	}
	//this method load in the parkingAreaSet data structure all the parkingAreas beloning the RNS
	public void loadParkingArea(){
		List<PlaceType> placeList = this.rns.getPlace();
		for(PlaceType p: placeList) {
			if ((p.getParkingArea()!=null)){
				parkingAreaSet.add(new MyParkingAreaType(p,p.getParkingArea(),mappingNamePlace));
			}
		}
	return;
	}
	
	public void loadConnection(){
		List<PlaceType> placeList = this.rns.getPlace();
		for(PlaceType p: placeList) {
			String source = p.getIdentifiedEntity().getId();
			for (String destination : p.getIsConnectedTo()){
				MyConnectionType connection = new MyConnectionType(mappingNamePlace);
				connection.setFrom(source);
				connection.setTo(destination);
				connectionsSet.add(connection);
			}
		}
		return;
	}
	
	public void loadVehicle(){
		List<VehicleType> vehicleList = rns.getVehicle();		
		if (vehicleList == null||vehicleList.isEmpty()){
			vehicleSet = null;
			return;
		}
		for(VehicleType v: vehicleList) {
			vehicleSet.add(new MyVehicleType(v,mappingNamePlace));
		}
		return;
	}
	
/*=======================================================================================================
 * 										END LOAD METHODS
 ========================================================================================================*/

	
	
	public void mappingPlaces(){
		//create the hashmap
		mappingNamePlace = new HashMap<String, PlaceType> ();
		//fill the hashmap
		for (PlaceType p : this.rns.getPlace()){
			String name = new String(p.getIdentifiedEntity().getId());
			PlaceType place = p;
			mappingNamePlace.put(name,place);
		}
		return ;
	}
	
	public void mappingVehicles(){
		//create the hashmap
		mappingNameVehicle = new HashMap<String, VehicleType> ();
		List<VehicleType> vehicleList = this.rns.getVehicle();
		if (vehicleList == null||vehicleList.isEmpty()){
			mappingNameVehicle = null;
			return;
		}
		//fill the hashmap
		for (VehicleType v : vehicleList){
			String name = new String(v.getIdentifiedEntity().getId());
			VehicleType vehicle = v;
			mappingNameVehicle.put(name,vehicle);
		}
		return ;		
	}


}
