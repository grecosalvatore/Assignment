package it.polito.dp2.RNS.sol3.service.RnsService;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.InternalServerErrorException;

import it.polito.dp2.RNS.ConnectionReader;
import it.polito.dp2.RNS.GateReader;
import it.polito.dp2.RNS.GateType;
import it.polito.dp2.RNS.ParkingAreaReader;
import it.polito.dp2.RNS.PlaceReader;
import it.polito.dp2.RNS.RnsReader;
import it.polito.dp2.RNS.RnsReaderException;
import it.polito.dp2.RNS.RnsReaderFactory;
import it.polito.dp2.RNS.RoadSegmentReader;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.*;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.ConnectedTo.To;
import it.polito.dp2.RNS.sol3.service.db.RnsDB;

public class InitRns {
	private static InitRns initDB = new InitRns();
	private static Logger logger ;
	
	private RnsReader monitor;
	private RnsDB db = RnsDB.getRnsDB();
	//private Neo4jServiceManager neo4jService = Neo4jServiceManager.getNeo4jServiceManager(); 
	private Neo4jServiceManager neo4jService;
	private  InitRns(){
		RnsReaderFactory factory = RnsReaderFactory.newInstance();
		try {
			monitor = factory.newRnsReader();
		}
		catch (RnsReaderException e) {
			logger.log(Level.SEVERE, "Error during initialization of the DB");
			throw new InternalServerErrorException();
		}
		neo4jService = new Neo4jServiceManager();
		logger = Logger.getLogger(InitRns.class.getName());
		//Load places in the DB
		initPlace();
		logger.log(Level.INFO, "Initialization of Neo4j DB...");
		//Load places in Neo4j
		neo4jService.initPlaces(monitor.getPlaces(null));
		neo4jService.initConnections(monitor.getConnections());
		logger.log(Level.INFO, "Neo4j DB succesfully initialized");
	}
	
	public static InitRns getInitRns() {
		return initDB;
	}
	
	private void initPlace(){
		logger.log(Level.INFO, "Initialization of Places...");
		initGates();
		logger.log(Level.INFO, "Gates initialized");
		initParkingAreas();
		logger.log(Level.INFO, "Parking Areas initialized");
		initRoadSegments();
		logger.log(Level.INFO, "Road Segments initialized");
		logger.log(Level.INFO, "Places succesfully initialized");
		logger.log(Level.INFO, "Initialization of Connections...");
		initConnections();
		logger.log(Level.INFO, "Connections succesfully initialized");
		return;
			
	}
	
	private void initGates(){
		Set<GateReader> GatesSet = monitor.getGates(null);
		for(GateReader p: GatesSet) {
			Place place = new Place();
			place.setCapacity(BigInteger.valueOf(p.getCapacity()));
			place.setPlaceId(p.getId());
			place.setGate(p.getType().toString());
			Set<PlaceReader> connectedPlaces = p.getNextPlaces();
			
			ConnectedTo ct = new ConnectedTo();
			
			for (PlaceReader connP : connectedPlaces){
				To to = new To();
				to.setPlaceId(connP.getId());
				to.setPlace(null);
				to.setPlaceLink(null);
				ct.getTo().add(to);
			}
			
			place.setConnectedTo(ct);
			
			if (db.addPlace(place)==false){
				//error
				logger.log(Level.SEVERE, "Error during initialization of Gates");
				throw new InternalServerErrorException();
			}
			
		}	
		return;
	}
	
	private void initParkingAreas(){
		Set<ParkingAreaReader> ParkingAreaSet = monitor.getParkingAreas(null);
		for(ParkingAreaReader p: ParkingAreaSet) {
			Place place = new Place();
			place.setCapacity(BigInteger.valueOf(p.getCapacity()));
			place.setPlaceId(p.getId());
			
			ParkingArea pat = new ParkingArea();
			
			Set<String> services = p.getServices();
			for (String service : services){
				pat.getService().add(service);
			}
			place.setParkingArea(pat);
			
			Set<PlaceReader> connectedPlaces = p.getNextPlaces();
			ConnectedTo ct = new ConnectedTo();
			
			for (PlaceReader connP : connectedPlaces){
				To to = new To();
				to.setPlaceId(connP.getId());
				to.setPlace(null);
				to.setPlaceLink(null);
				ct.getTo().add(to);
			}
			
			place.setConnectedTo(ct);
			
			if (db.addPlace(place)==false){
				//error
				logger.log(Level.SEVERE, "Error during initialization of Parking Areas");
				throw new InternalServerErrorException();
			}
			
		}	
		return;
	}
	
	private void initRoadSegments(){
		Set<RoadSegmentReader> RoadSegmentSet = monitor.getRoadSegments(null);
		for(RoadSegmentReader p : RoadSegmentSet) {
			Place place = new Place();
			place.setCapacity(BigInteger.valueOf(p.getCapacity()));
			place.setPlaceId(p.getId());
			
			RoadSegment rst = new RoadSegment();
			rst.setName(p.getName());
			rst.setRoadName(p.getRoadName());
			
			place.setRoadSegment(rst);
			
			Set<PlaceReader> connectedPlaces = p.getNextPlaces();
			ConnectedTo ct = new ConnectedTo();
			
			for (PlaceReader connP : connectedPlaces){
				To to = new To();
				to.setPlaceId(connP.getId());
				to.setPlace(null);
				to.setPlaceLink(null);
				ct.getTo().add(to);
			}
			
			place.setConnectedTo(ct);
			
			if (db.addPlace(place)==false){
				//error
				logger.log(Level.SEVERE, "Error during initialization of Road Segments");
				throw new InternalServerErrorException();
			}
			
		}	
		return;
	}
	
	private void initConnections(){
		Set<ConnectionReader> ConnectionSet = monitor.getConnections();
		for (ConnectionReader c : ConnectionSet){
			if (db.addConnection(c.getFrom().getId(), c.getTo().getId()) == false){
				//error
				logger.log(Level.SEVERE, "Error during initialization of Connections");
				throw new InternalServerErrorException();
				
			}
		}
		return ;
	}
	
}
