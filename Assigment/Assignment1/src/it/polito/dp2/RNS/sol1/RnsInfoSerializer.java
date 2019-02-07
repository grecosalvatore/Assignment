package it.polito.dp2.RNS.sol1;

import java.io.File;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import it.polito.dp2.RNS.*;
import it.polito.dp2.RNS.sol1.jaxb.*;
import it.polito.dp2.RNS.sol1.jaxb.VehicleType;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;


public class RnsInfoSerializer {
		private RnsReader monitor;
		private DateFormat dateFormat;
		public static String FileName;
		private RnsType rns;
		public ObjectFactory of;
		/**
 * Default constructror
 * @throws RnsReaderException 
 */
public RnsInfoSerializer() throws RnsReaderException {
	of = new ObjectFactory();
	it.polito.dp2.RNS.RnsReaderFactory factory = it.polito.dp2.RNS.RnsReaderFactory.newInstance();
	monitor = factory.newRnsReader();
	dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm");
	
}

public RnsInfoSerializer(RnsReader monitor) {
	super();
	this.monitor = monitor;
	dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm");
}

/**
 * @param args
 */
public static void main(String[] args) {
	RnsInfoSerializer wf;
	
	//check of parameter
	if (args.length != 1){
		System.out.println("Error: wrong number of arguments - <FileName>");
		System.exit(1);
	}else{
		//first parameter is the xml ouput filename
		FileName = args[0];
	}
	
	try {
		wf = new RnsInfoSerializer();
		wf.createRns();
		wf.marshaller();
	
		
	} catch (RnsReaderException e) {
		System.err.println("Could not instantiate data generator.");
		e.printStackTrace();
		System.exit(1);
	}
	
}


public void createRns() {
	printLine(' ');
	rns = new RnsType();
	createPlaces();
	createVehicles();
	System.out.println("Rns succesfully created!");
}

private void createPlaces() {
	createGates();
	createRoadSegments();
	createParkingAreas();
	
}


private void marshaller(){
	// create a JAXBContext capable of handling classes generated into
    // the primer.po package
    JAXBContext jc;
	try {
		jc = JAXBContext.newInstance( "it.polito.dp2.RNS.sol1.jaxb" );

	    // create a Marshaller and marshal to a file
	    Marshaller m = jc.createMarshaller();
	    Schema schema = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI).newSchema(new File("xsd/rnsInfo.xsd"));
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);   
		m.setSchema(schema);
	   // m.marshal( of.createRns(rns), System.out );
		 m.marshal( of.createRns(rns), new File(FileName));
	} catch (JAXBException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (SAXException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  

    
 
	
}

private void createGates() {
	// Get the list of Gates
	Set<GateReader> set = monitor.getGates(null);
	
	// For each Gate create related data
	for (GateReader gate: set) {
		PlaceType place = new PlaceType();
		place.setCapacity(BigInteger.valueOf(gate.getCapacity()));
		IdentifiedEntityType ie = new IdentifiedEntityType();
		ie.setId(gate.getId());
		place.setIdentifiedEntity(ie);
		place.setGate(gate.getType().value());
		
		Set<ConnectionReader> setCon = monitor.getConnections();
		
		// For each connection, print related data
		for (ConnectionReader conn: setCon) {
			if (conn.getFrom().getId().equals(gate.getId())){
				place.getIsConnectedTo().add(conn.getTo().getId());
			}
		}
		rns.getPlace().add(place);
	}
}

private void createRoadSegments() {
	// Get the list of Road Segments
	Set<RoadSegmentReader> set = monitor.getRoadSegments(null);
	
	// For each Road segment create related data
	for (RoadSegmentReader seg: set) {
		PlaceType place = new PlaceType();
		place.setCapacity(BigInteger.valueOf(seg.getCapacity()));
		IdentifiedEntityType ie = new IdentifiedEntityType();
		ie.setId(seg.getId());
		place.setIdentifiedEntity(ie);
		RoadSegmentType rst = new RoadSegmentType(); 
		rst.setName(seg.getName());
		rst.setRoadName(seg.getRoadName());
		place.setRoadSegment(rst);
		Set<ConnectionReader> setCon = monitor.getConnections();
		System.out.println(seg.getId()+"\n");
		// For each connection, print related data
		for (ConnectionReader conn: setCon) {
			if (conn.getFrom().getId().equals(seg.getId())){
				place.getIsConnectedTo().add(conn.getTo().getId());
			}
		}
		rns.getPlace().add(place);
	}
}

private void createParkingAreas() {
	// Get the list of Parking Areas
	Set<ParkingAreaReader> set = monitor.getParkingAreas(null);
	
	
	// For each Parking Area print related data
	for (ParkingAreaReader pa: set) {
		PlaceType place = new PlaceType();
		place.setCapacity(BigInteger.valueOf(pa.getCapacity()));
		IdentifiedEntityType ie = new IdentifiedEntityType();
		ie.setId(pa.getId());
		place.setIdentifiedEntity(ie);
		ParkingAreaType pat = new ParkingAreaType(); 
		for (String service:pa.getServices()){
			pat.getService().add(service);
		}
		place.setParkingArea(pat);
		
		Set<ConnectionReader> setCon = monitor.getConnections();
		
		// For each connection, print related data
		for (ConnectionReader conn: setCon) {
			if (conn.getFrom().getId().equals(pa.getId())){
				place.getIsConnectedTo().add(conn.getTo().getId());
			}
		}
		rns.getPlace().add(place);
	}
	
}



private void createVehicles() {
	// Get the list of Vehicles
	Set<VehicleReader> set = monitor.getVehicles(null,null,null);
	
	// For each Road segment create related data
	for (VehicleReader v: set) {
	
		VehicleType vehicle = new VehicleType();
		vehicle.setVehicleType(EnumVehicleType.valueOf(v.getType().value()));
		IdentifiedEntityType ie = new IdentifiedEntityType();
		ie.setId(v.getId());
		vehicle.setIdentifiedEntity(ie);
		vehicle.setOrigin(v.getOrigin().getId());
		vehicle.setPosition(v.getPosition().getId());
		vehicle.setDestination(v.getDestination().getId());
		vehicle.setVehicleState(EnumVehicleState.valueOf(v.getState().value()));
		
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTimeInMillis(v.getEntryTime().getTimeInMillis());
		DatatypeFactory dtf;
		//managing date and time issues
		try {
			dtf = DatatypeFactory.newInstance();
			XMLGregorianCalendar xgc = dtf.newXMLGregorianCalendar(gc);
			vehicle.setEntryTime(xgc);
		} catch (DatatypeConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		rns.getVehicle().add(vehicle);
	}
	
	
}


	
	private void printLine(char c) {
		System.out.println(makeLine(c));
	}


	
	private StringBuffer makeLine(char c) {
		StringBuffer line = new StringBuffer(132);
		
		for (int i = 0; i < 132; ++i) {
			line.append(c);
		}
		return line;
	}

}
