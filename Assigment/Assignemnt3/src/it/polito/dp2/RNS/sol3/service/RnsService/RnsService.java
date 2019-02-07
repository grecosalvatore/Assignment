package it.polito.dp2.RNS.sol3.service.RnsService;



import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.SAXException;

import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.ConnectedTo;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.ConnectedTo.To;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.Connection;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.Connections;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.Place;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.Places;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.Position;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.Rns;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.State;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.SuggestedPath;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.SuggestedPath.Path;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.Vehicle;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.Vehicles;
import it.polito.dp2.RNS.sol3.service.db.RnsDB;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;



public class RnsService {
		private RnsDB db = RnsDB.getRnsDB();
		private InitRns initDB = InitRns.getInitRns();
		//private Neo4jServiceManager neo4jService = Neo4jServiceManager.getNeo4jServiceManager(); 
		private Neo4jServiceManager neo4jService ; 
		private DateTimeManager timestampManager = new DateTimeManager();
		private static Logger logger = Logger.getLogger(RnsService.class.getName());
		
		public RnsService() {
			neo4jService = new Neo4jServiceManager();
			return;
		}
		
		//get the root resource with links to some resources
		public Rns getRns(UriInfo uriInfo) {
			logger.log(Level.INFO, "GET rns info request");
	    	return fillRns(uriInfo);
		}
		
		//Get the list of all vehicles in the system
		public Vehicles getvehicles(UriInfo uriInfo) {
			logger.log(Level.INFO, "GET vehicles request");
			Vehicles vehicles = new Vehicles();
			Collection<Vehicle> vehicleList = db.getVehicles();
			if (vehicleList != null){
				for (Vehicle v : vehicleList){
					Vehicle vehicle = new Vehicle();
					vehicle = fillVehicleInfo(uriInfo,v);
					vehicles.getVehicle().add(vehicle);
				}
				return vehicles;
			}else{
				//create an empty list of vehicles with only the self element setted
				Vehicles emptyVehicles = new Vehicles();
				emptyVehicles.setSelf(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/vehicles/"));
				emptyVehicles.getVehicle().clear();
				return emptyVehicles;
			}
		}
		
		//Get a single place given the placeId, filled with all URIs
		public Response getVehicle(UriInfo uriInfo,String plateId) {
			logger.log(Level.INFO, "GET vehicle "+plateId+"o request");
			Vehicle v = db.getVehicle(plateId);
			if (v == null)
				return Response.status(Response.Status.NOT_FOUND).entity("Vehicle "+plateId+" is not in the system").build();
			Vehicle vehicle = fillVehicleInfo(uriInfo,v);		
			return Response.ok(vehicle).build();
		}
		
		//vehicle request to enter the system
		public synchronized Response tryEnterVehicle(UriInfo uriInfo,Vehicle vehicle){
			logger.log(Level.INFO, "PUT Vehicle " + vehicle.getPlateId()+" request to enter the system");
			if (db.getVehicle(vehicle.getPlateId()) != null){
				//vehicle is already in the db
				logger.log(Level.INFO, "Enter request: vehicle is already in the system.");
				return Response.status(Response.Status.FORBIDDEN).entity("Vehicle already in the system").build();
			}
			String originId = vehicle.getOrigin();
			Place origin = db.getPlace(originId);
			if (origin != null){
				//check if origin is an IN / INOUT gate
				if ((origin.getParkingArea()==null)&&(origin.getRoadSegment()==null)){
					if (origin.getGate().equals("IN")||origin.getGate().equals("INOUT")){
						String destinationId = vehicle.getDestination();
						Place destination = db.getPlace(destinationId);
						if (destination != null){
							List<String> pathId = neo4jService.findShortestPath(originId,destinationId);
							if (pathId != null){
								if (pathId.isEmpty())
									return Response.status(Response.Status.BAD_REQUEST).entity("Destination place is not reachable starting from this origin").build();
								SuggestedPath sp = new SuggestedPath();
								Vehicle addVehicle = new Vehicle();
								addVehicle = vehicle;
								Position position = new Position();
								position.setPlaceId(vehicle.getOrigin());
								addVehicle.setPosition(position);
								State state = new State();
								state.setVehicleState("IN_TRANSIT");
								addVehicle.setState(state);
								try {
									XMLGregorianCalendar currentTimestamp = timestampManager.getCurrentXmlDate();
									addVehicle.setEntryTime(currentTimestamp);
								} catch (DatatypeConfigurationException e) {
									// TODO Auto-generated catch block
									logger.log(Level.SEVERE, "Enter request: error with timestamp configuration.");
									throw new InternalServerErrorException();
								}
								sp = fillSuggestedPathInfo(uriInfo,vehicle,pathId);
								if ( db.addVehicle(addVehicle) == true){
									//vehicle added succesfully
									if (db.setSuggestedPath(addVehicle.getPlateId(), sp) == true)
										return Response.ok(sp).build();
								}
								logger.log(Level.SEVERE, "Error: impossible add the vehicle in the DB.");
								throw new InternalServerErrorException();//impossible add the vehicle in the db
							}else{
								//error : neo4j returned a null path
								logger.log(Level.SEVERE, "Error: Neo4j server error.");
								throw new InternalServerErrorException();
							}
						}else{
							//error : destination is not in the db
							logger.log(Level.INFO, "Enter request: Destination is not in the DB.");
							return Response.status(Response.Status.BAD_REQUEST).entity("Destination place is not in the system").build();
						}
					}else{
						//error is not a in or inout gate
						logger.log(Level.INFO, "Enter request: origin is not IN/INOUT gate.");
						return Response.status(Response.Status.FORBIDDEN).entity("Origin place is not an IN/INOUT Gate").build();
					}
				}else{
					//error is not a gate
					logger.log(Level.INFO, "Enter request: origin is not a gate.");
					return Response.status(Response.Status.FORBIDDEN).entity("Origin place is not an IN/INOUT Gate").build();
				}
			}else{
				//error : origin place is not in the system
				logger.log(Level.INFO, "Enter request: origin is not in the system.");
				return Response.status(Response.Status.BAD_REQUEST).entity("Origin place is not in the system").build();
			}
		}
		
		//Get the list of all places in the system
		public Places getPlaces(UriInfo uriInfo) {
			logger.log(Level.INFO, "GET places request");
			Places places = new Places();
			Collection<Place> listPlace = db.getPlaces();
			if (listPlace != null){
				for (Place p : listPlace){
					Place place = new Place();
					place = fillPlaceInfo(uriInfo,p);
					places.getPlace().add(place);
				}
				return places;
			}else{
				//create an empty list of places with only the self element setted
				Places emptyPlaces = new Places();
				emptyPlaces.setSelf(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/places/"));
				return emptyPlaces;
			}
		}
		
		//Get the list of all gates in the system
		public Places getGates(UriInfo uriInfo) {
			logger.log(Level.INFO, "GET gates request");
			Places places = new Places();
			Collection<Place> gates = db.getGates();
			if (gates != null){
				for (Place p : gates){
					Place place = new Place();
					place = fillPlaceInfo(uriInfo,p);	
					places.getPlace().add(place);
				}
				return places;
			}else{
				//create an empty list of places with only the self element setted
				Places emptyPlaces = new Places();
				emptyPlaces.setSelf(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/places/"));
				return emptyPlaces;
			}
		}
		
		//Get the list of all road segments in the system
		public Places getRoadSegments(UriInfo uriInfo) {
			logger.log(Level.INFO, "GET road segments request");
			Places places = new Places();
			Collection<Place> roadSegments = db.getRoadSegments();
			if (roadSegments != null){
				for (Place p : roadSegments){
					Place place = new Place();
					place = fillPlaceInfo(uriInfo,p);	
					places.getPlace().add(place);
				}
				return places;
			}else{
				//create an empty list of places with only the self element setted
				Places emptyPlaces = new Places();
				emptyPlaces.setSelf(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/places/"));
				return emptyPlaces;
			}
		}
		
		//Get the list of all parking areas in the system
		public Places getParkingAreas(UriInfo uriInfo) {
			logger.log(Level.INFO, "GET parking areas request");
			Places places = new Places();
			Collection<Place> parkingAreas = db.getParkingAreas();
			if (parkingAreas != null){
				for (Place p : parkingAreas){
					Place place = new Place();
					place = fillPlaceInfo(uriInfo,p);	
					places.getPlace().add(place);
				}
				return places;
			}else{
				//create an empty list of places with only the self element setted
				Places emptyPlaces = new Places();
				emptyPlaces.setSelf(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/places/"));
				return emptyPlaces;
			}
		}
		
		public Response getSuggestedPath(UriInfo uriInfo,String plateId){
			logger.log(Level.INFO, "GET Vehicle " + plateId+" current suggested path request");
			SuggestedPath sp = db.getSuggestedPathByPlateId(plateId);
			if (sp == null)
				return Response.status(Response.Status.NOT_FOUND).entity("Vehicle "+plateId+" is not in the system").build();
			return Response.ok(sp).build();
		}
		
		public Response getVehicleState(UriInfo uriInfo,String plateId){
			logger.log(Level.INFO, "GET Vehicle " + plateId+" current state request");
			Vehicle v = db.getVehicle(plateId);
			if (v == null)
				return Response.status(Response.Status.NOT_FOUND).entity("Vehicle "+plateId+" is not in the system").build();
			State state = new State();
			state.setSelf(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/vehicles/"+v.getPlateId()+"/state"));
			state.setVehicleState(v.getState().getVehicleState());
			return Response.ok(state).build();
		}
		
		public synchronized Response changeVehicleState(UriInfo uriInfo,String plateId,State newState){
			logger.log(Level.INFO, "PUT Vehicle " + plateId+" request to change state");
			Vehicle vehicle = db.getVehicle(plateId);
			if (vehicle == null)
				return Response.status(Response.Status.NOT_FOUND).entity("Vehicle "+plateId+" is not in the system").build();
			State oldState = vehicle.getState();
			if (newState.getVehicleState().equals("IN_TRANSIT")||newState.getVehicleState().equals("PARKED")){
				//old state and new state are equal
				if (newState.equals(oldState)){
					newState.setSelf(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/vehicles/"+vehicle.getPlateId()+"/state"));
					return Response.ok(newState).build();
				}else{
					//update state
					if (db.changeState(plateId, newState)==true){	
						newState.setSelf(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/vehicles/"+vehicle.getPlateId()+"/state"));
						return Response.ok(newState).build();
					}else{
						//internal server error -> impossible update the db
						logger.log(Level.SEVERE, "Error: imposible update vehicle state.");
						throw new InternalServerErrorException();
					}
				}
			}else{
				//bad request
				return Response.status(Response.Status.BAD_REQUEST).entity("New state is different from PARKED/IN_TRANSIT").build();
			}
		}
		
		public synchronized Response deleteVehicle(String plateId){
			logger.log(Level.INFO, "DELETE Vehicle " + plateId+" by admin request");
			if (db.deleteVehicle(plateId))
				return Response.ok("Vehicle succesfully deleted").build();
			else
				return Response.status(Response.Status.NOT_FOUND).entity("Vehicle "+plateId+" is not in the system").build();
		}
		
		public synchronized Response exitVehicleRequest(String plateId){
			logger.log(Level.INFO, "DELETE Vehicle " + plateId+" request to exit the system");
			Vehicle v = db.getVehicle(plateId);
			if (v == null){ // vehicle not found
				return Response.status(Response.Status.NOT_FOUND).entity("Vehicle "+plateId+" is not in the system").build();
			}else{
				Place currentPlace = db.getPlace(v.getPosition().getPlaceId());
				if (currentPlace.getGate() == null){
					//current position is not a Gate
					return Response.status(Response.Status.FORBIDDEN).entity("Current position is not an OUT/INOUT gate").build();
				}else{
					if (currentPlace.getGate().equals("OUT")||currentPlace.getGate().equals("INOUT")){
						if (db.deleteVehicle(plateId)== true)
							return Response.status(Response.Status.OK).entity("Vehicle "+plateId+" successfully exited the system").build();
						else
							throw new InternalServerErrorException();
					}else{
						//current position is not an INOUT or OUT gate
						return Response.status(Response.Status.FORBIDDEN).entity("Current position is not an OUT/INOUT gate").build();
					
					}
				}
			}
				
		}
		
		public Response getVehicleCurrentPosition(UriInfo uriInfo,String plateId){
			logger.log(Level.INFO, "GET Vehicle " + plateId+" current position request");
			Vehicle v = db.getVehicle(plateId);
			if (v == null) //vehicle not found
				return Response.status(Response.Status.NOT_FOUND).entity("Vehicle "+plateId+" is not in the system").build();
			Position position = new Position();
			position.setSelf(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/vehicles/"+v.getPlateId()+"/currentPosition"));
			position.setSelf(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/places/"+v.getPosition().getPlaceId()));
			position.setPlaceId(v.getPosition().getPlaceId());
			return Response.ok(position).build();
		}
		
		public synchronized Response changeCurrentPosition(UriInfo uriInfo,String plateId,Position newPosition){
			logger.log(Level.INFO, "PUT Vehicle " + plateId+" request to change position to "+newPosition.getPlaceId());
			Vehicle vehicle = db.getVehicle(plateId);
			if (vehicle != null){
				Place newPlace = db.getPlace(newPosition.getPlaceId());
				if ( newPlace != null){
					if (newPlace.getPlaceId().equals(vehicle.getPosition().getPlaceId())){
						//new position is equal to current position -> nothing change
						SuggestedPath sp = db.getSuggestedPathByPlateId(plateId);
						return Response.ok(sp).build();
					}else{
						// try to change position
						SuggestedPath sp = db.getSuggestedPathByPlateId(plateId);
						Position oldPosition = vehicle.getPosition();
						if (checkIfInSuggestedPath(sp,oldPosition.getPlaceId(),newPosition.getPlaceId()) == true){
							System.out.println("new position is in the suggested path");
							//new pos is the next pos in the suggested path
							if (db.changePositionWithoutSuggestedPath(plateId, newPosition)==true)
								return Response.ok(sp).build();
							else
								throw new InternalServerErrorException();//internal server error
						}else{
							//new pos is not the next in suggested path
							//check if the current position is connectedTo the new position
							if (checkIfIsNextTo(oldPosition.getPlaceId(),newPosition.getPlaceId())==true){
								//the old place is connected to the new place
								//look if exist a suggested path from the new position
								//from= new position -> destination= vehicle destination
								List<String> pathId = neo4jService.findShortestPath(newPosition.getPlaceId(),vehicle.getDestination());
								if (pathId != null&&(pathId.isEmpty()==false)){
									SuggestedPath newSp = new SuggestedPath();
									newSp = fillSuggestedPathInfo(uriInfo,vehicle,pathId);
									if (db.changePositionWithSuggestedPath(newSp,plateId,newPosition)==true){
										return Response.ok(newSp).build();
									}else{
										//internal server error : impossible update sp
										throw new InternalServerErrorException();
									}
								}else{
									//error : null path -> impossible arrive to the destination from new current position
									SuggestedPath newSp = new SuggestedPath();
									newSp = fillSuggestedPathInfo(uriInfo,vehicle,null);
									//empty suggested path
									if (db.changePositionWithSuggestedPath(newSp,plateId,newPosition)==true){
										return Response.ok(newSp).build();
									}else{
										//internal server error : impossible update sp
										throw new InternalServerErrorException();
									}
								}
							}else{
								//error : the old place is not connected to the new place
								return Response.status(Response.Status.FORBIDDEN).entity("New position is unreachable from previous current position").build();
							}
						}
					}
				}else{
					//new position not found in the db
					return Response.status(Response.Status.BAD_REQUEST).entity("New position place is not in the system").build();
				}
			}else{
				//vehicle not found
				return Response.status(Response.Status.NOT_FOUND).entity("Vehicle "+plateId+" is not in the system").build();
			}
		}
		
		//this method return true if the next place is the next on the suggested path,false otherwise
		private boolean checkIfInSuggestedPath(SuggestedPath sp,String oldP,String newP){
			String previous = null;
			for (Path p : sp.getPath()){
				if (previous==null)
					previous=p.getPalceId();
				if ((oldP.equals(previous))&&(newP.equals(p.getPalceId()))){
					//next place is the next in the suggested path
					return true;
				}else{
					previous=p.getPalceId();
				}
			}
			return false;			
		}
		
		private boolean checkIfIsNextTo(String oldP,String newP){
			Place oldPlace = db.getPlace(oldP);
			List<To> toPlaces = oldPlace.getConnectedTo().getTo();
			for (To toPlace : toPlaces){
				if (toPlace.getPlaceId().equals(newP))
					return true;
			}
			return false;			
		}
		
		public Vehicles getCurrentVehiclesByPlaceId(UriInfo uriInfo,String placeId){
			logger.log(Level.INFO, "GET current vehicles in place " + placeId+" request");
			Vehicles vehicles = new Vehicles();
			Collection<Vehicle> vehicleList = db.getCurrentVehiclesByPlaceId(placeId);
			if (vehicleList != null){
				for (Vehicle v : vehicleList){
					Vehicle vehicle = new Vehicle();
					vehicle = fillVehicleInfo(uriInfo,v);
					vehicles.getVehicle().add(vehicle);					
				}
			}
			vehicles.setSelf(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/places/"+placeId+"/currentVehicles"));
			return vehicles;
		}
		
		
		//Get the list of the connections on the db filled with all URIs
		public Connections getConnections(UriInfo uriInfo){
			logger.log(Level.INFO, "GET connections request");
			Connections connections = new Connections();
	    	connections.setSelf(myUriBuilder(uriInfo.getAbsolutePathBuilder(),null));
	    	Collection<Connection> listConnection = db.getConnections();
	    	if (listConnection == null||listConnection.isEmpty()){
	    		//create an empty list of places with only the self element setted
				Connections emptyConnections = new Connections();
				emptyConnections.setSelf(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/connections/"));
				return emptyConnections;
	    	}
			for (Connection c : listConnection){
				Connection connection = new Connection();
				connection.setFrom(c.getFrom());
				connection.setTo(c.getTo());
		    	connection.setFromLink(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/places/"+c.getFrom()));
		    	connection.setToLink(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/places/"+c.getTo()));
		    	
				connections.getConnection().add(connection);
			}
			return connections;
			
		}
		
		//Get a single place given the placeId, filled with all URIs
		public Response getPlace(UriInfo uriInfo,String placeId) {
			logger.log(Level.INFO, "GET place " + placeId + " request");
			Place p = db.getPlace(placeId);
			if (p == null)
				return Response.status(Response.Status.NOT_FOUND).entity("Place "+placeId+" is not in the system").build();
			Place place = fillPlaceInfo(uriInfo,p);		
			return Response.ok(place).build();
		}
		
		
		private Place fillPlaceInfo(UriInfo uriInfo,Place p){
			Place place = new Place();
			place = p;
			place.setSelf(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/places/"+p.getPlaceId()));
	    	place.setCurrentVehiclesLink(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/places/"+p.getPlaceId()+"/currentVehicles"));
	    	ConnectedTo connectedTo = new ConnectedTo();
	    	connectedTo.setSelf(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/places/"+p.getPlaceId()+"/connectedTo"));
	    	for (To to: p.getConnectedTo().getTo()){
	    		To tmpTo = new To();
	    		tmpTo.setPlaceId(to.getPlaceId());
	    		tmpTo.setPlaceLink(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/places/"+tmpTo.getPlaceId()));
	    		connectedTo.getTo().add(tmpTo);
	    	}
	    	place.setConnectedTo(connectedTo);
			return place;
		}
		
		private Vehicle fillVehicleInfo(UriInfo uriInfo,Vehicle v){
			Vehicle vehicle = new Vehicle();
			vehicle = v;
			vehicle.setSelf(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/vehicles/"+v.getPlateId()));
			vehicle.setDestinationLink(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/places/"+v.getOrigin()));
			vehicle.setOriginLink(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/places/"+v.getDestination()));
			vehicle.setState(v.getState());
			vehicle.getState().setSelf(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/vehicles/"+v.getPlateId()+"/state"));
			Position position = new Position();
			position.setSelf(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/vehicles/"+v.getPlateId()+"/currentPosition"));
			position.setPlaceId(v.getOrigin());
			position.setPlaceLink(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/places/"+v.getOrigin()));
			vehicle.setSuggestedPathLink(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/vehicles/"+v.getPlateId()+"/suggestedPath"));
			vehicle.setChangeStateLink(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/vehicles/"+v.getPlateId()+"/state"));
			vehicle.setDeleteVehicleLink(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/vehicles/"+v.getPlateId()));
			vehicle.setCurrentPositionLink(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/vehicles/"+v.getPlateId()+"/currentPosition"));
			return vehicle;
		}
		
		private SuggestedPath fillSuggestedPathInfo(UriInfo uriInfo,Vehicle v,List<String> path){
			SuggestedPath sp = new SuggestedPath();
			sp.setStartId(v.getOrigin());
			sp.setEndId(v.getDestination());
			sp.setVehicle(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/vehicles/"+v.getPlateId()));
			sp.setSelf(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/vehicles/"+v.getPlateId()+"/suggestedPath"));
			sp.setStart(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/places/"+v.getOrigin()));
			sp.setEnd(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/places/"+v.getDestination()));
			sp.setCurrentPositionLink(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/vehicles/"+v.getPlateId()+"/currentPosition"));
			if (path == null){
				//destination unreachable
				return sp;
			}
			for (String place : path){
				Path p = new Path();
				p.setPalceId(place);
				p.setPlaceLink(myUriBuilder(uriInfo.getBaseUriBuilder(),"/rns/places/"+place));
				sp.getPath().add(p);
			}
			return sp;
		}
		
		private String myUriBuilder(UriBuilder base,String pathRes){
			UriBuilder baseUri = base;
			if (pathRes != null)
				base.path(pathRes);
			return base.build().toString();
		}
		
		private Rns fillRns(UriInfo uriInfo){
	    	Rns rns = new Rns();
	    	rns.setSelf(myUriBuilder(uriInfo.getAbsolutePathBuilder(),null));
	    	rns.setPlaces(myUriBuilder(uriInfo.getAbsolutePathBuilder(),"/places"));
	    	rns.setVehicles(myUriBuilder(uriInfo.getAbsolutePathBuilder(),"/vehicles"));
	    	rns.setConnections(myUriBuilder(uriInfo.getAbsolutePathBuilder(),"/connections"));
	    	rns.setGates(myUriBuilder(uriInfo.getAbsolutePathBuilder(),"/places/gates"));
	    	rns.setParkingAreas(myUriBuilder(uriInfo.getAbsolutePathBuilder(),"/places/parkingAreas"));
	    	rns.setRoadSegments(myUriBuilder(uriInfo.getAbsolutePathBuilder(),"/places/roadSegments"));
	    	return rns;
		}

	

}
