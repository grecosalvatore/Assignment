package it.polito.dp2.RNS.sol3.service.RnsService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import it.polito.dp2.RNS.sol3.jaxb.neo4j.*;


import it.polito.dp2.RNS.RnsReader;
import it.polito.dp2.RNS.RnsReaderException;
import it.polito.dp2.RNS.RnsReaderFactory;
import it.polito.dp2.RNS.sol3.service.db.Neo4jDB;
import it.polito.dp2.RNS.sol3.service.db.RnsDB;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import it.polito.dp2.RNS.ConnectionReader;
import it.polito.dp2.RNS.PlaceReader;

public class Neo4jServiceManager {
	//private static Neo4jServiceManager neo4jServiceManager = new Neo4jServiceManager();
	private  Client client;
	private static String serviceBaseUri = System.getProperty("it.polito.dp2.RNS.lab3.Neo4JURL");
	private JAXBContext jc;
	private javax.xml.validation.Validator validator;
	private Neo4jDB neo4jDB = Neo4jDB.getNeo4jDB();
	
	 private static Logger logger = Logger.getLogger(Neo4jServiceManager.class.getName());

	
	//private Neo4jServiceManager ()  {
	 public Neo4jServiceManager ()  {
		this.client = ClientBuilder.newClient();
		if (serviceBaseUri == null)
			serviceBaseUri = "http://localhost:8080/RnsSystem/rest";
		
		// create validator that uses the DataTypes schema
    	SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
    	Schema schema;
		try {
			InputStream schemaStream = Neo4jServiceManager.class.getResourceAsStream("/xsd/Neo4jXML.xsd");
            if (schemaStream == null) {
                logger.log(Level.SEVERE, "xml schema file Not found.");
                try {
					throw new IOException();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					logger.log(Level.SEVERE, "Error in init Neo4j: Unexpected IOException.");
				}
            }
            schema = sf.newSchema(new StreamSource(schemaStream));
			validator = schema.newValidator();
	        jc = JAXBContext.newInstance("it.polito.dp2.RNS.sol3.jaxb.neo4j");
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			logger.log(Level.SEVERE, "Error in init Neo4j: Unexpected SAXException.");
			throw new InternalServerErrorException();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			logger.log(Level.SEVERE, "Error in init Neo4j: Unexpected JAXBException.");
			throw new InternalServerErrorException();
		}
		return;
    	
	}
	
	/*public static Neo4jServiceManager getNeo4jServiceManager() {
		return neo4jServiceManager;
	}*/
	
	public List<String> findShortestPath(String origin,String destination){
		List<String> path = sendShortestPathsRequest(origin,destination,0);
		return path;
	}
	
	public void initPlaces(Set<PlaceReader> places){
		for (PlaceReader place : places){
			String placeId = place.getId();
			CreateNodeType node = new CreateNodeType();
			node.setId(placeId);
			//validate the node against schema
			if (nodeValidation(node) == true){
				//if no error occurred in the validation send a request to neo4j
				Node responseNode = sendCreationNodeRequest(node);
				//validate the response object against schema
				if (nodeValidation(responseNode) == true){
					//add the placeId - node tuple to the db
					if (neo4jDB.addNode(placeId, responseNode)== false){
						//error: already present in the db
						logger.log(Level.SEVERE, "Error in the init of neo4jDB.");
						throw new InternalServerErrorException();
					}
				}else{
					//error in the validation of response
					logger.log(Level.SEVERE, "Error in the validation of Neo4j Node Response.");
					throw new InternalServerErrorException();
				}
				
			}else{
				//error in the validation of the request
				logger.log(Level.SEVERE, "Error in the validation of Neo4j Node Request.");
				throw new InternalServerErrorException();
			}
		}
		return;
	}
	
	public void initConnections(Set<ConnectionReader> Connections){
		for (ConnectionReader connection : Connections){
			String fromId = connection.getFrom().getId();
			String toId = connection.getTo().getId();
			Node fromNode = neo4jDB.getNodeByPlaceId(fromId);
			if (fromNode != null){
				Node toNode = neo4jDB.getNodeByPlaceId(toId);
				if (toNode != null){
					CreateRelationshipType relationship = new CreateRelationshipType();
					String fromUri = fromNode.getSelf();
					String toUri = toNode.getSelf();
					relationship.setTo(toUri);	
					relationship.setType("ConnectedTo");
					//validate the node against schema
					if (nodeValidation(relationship) == true){
						//if no error occurred in the validation send a request to neo4j
						Relationship responseRelationship = sendCreationRelationshipRequest(fromUri,relationship);
						//validate the response object against schema
						if (nodeValidation(responseRelationship) == true){
							//save in local DB
							//success
						}else{
							//error in the validation of response
							logger.log(Level.SEVERE, "Error in the validation of Neo4j RelationShip Response.");
							throw new InternalServerErrorException();
						}
					}else{
						//error in the validation of request
						logger.log(Level.SEVERE, "Error in the validation of Neo4j Request.");
						throw new InternalServerErrorException();
					}
				}else{
					//erro to node is null
					logger.log(Level.SEVERE, "Error in the creation of Neo4j request: destination node is not in the DB.");
					throw new InternalServerErrorException();
				}
			}else{
				//error from node is null
				logger.log(Level.SEVERE, "Error in the creation of Neo4j request: origin node is not in the DB.");
				throw new InternalServerErrorException();
			}
			
		}
		return;
	}
	
	protected static UriBuilder getBaseURI() {
		return UriBuilder.fromUri(serviceBaseUri + "/data");
	}
	
	protected boolean nodeValidation(Object node){
		try {
	    	JAXBSource source = new JAXBSource(jc, node);
	    	validator.validate(source);
		} catch (org.xml.sax.SAXException se) {
			logger.log(Level.SEVERE, "Error: validation failed.");		  
			throw new InternalServerErrorException();
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error in init Neo4j: Unexpected I/O Exception.");
			throw new InternalServerErrorException();
		} catch (JAXBException e) {
			logger.log(Level.SEVERE, "Error in init Neo4j: Unexpected JAXB Exception.");
			throw new InternalServerErrorException();	
		}
		return true;
	}
	
	//send a creation node to neo4j and return a nodeObject
	protected Node sendCreationNodeRequest(CreateNodeType node){
		// build the web target
		WebTarget target = client.target(getBaseURI()).path("node");
		Node responseNode;
		
		Response response = target
				   .request()
				   .accept(MediaType.APPLICATION_JSON)
				   .post(Entity.json(node));
		if (response.getStatus()!=201) {
			logger.log(Level.SEVERE, "Error in init Neo4j: Error in the remote operation (node)."+response.getStatus()+" "+response.getStatusInfo());
			throw new InternalServerErrorException();
		}
		
		responseNode = response.readEntity(Node.class);
		return responseNode;
	}
	
	//send a relationship to neo4j
	protected Relationship sendCreationRelationshipRequest (String targetUri,CreateRelationshipType relationship){
		
		
		// build the web target
		WebTarget target = client.target(UriBuilder.fromUri(targetUri+"/relationships"));
		Relationship responseRelationship;
		
		Response response = target
				   .request()
				   .accept(MediaType.APPLICATION_JSON)
				   .post(Entity.json(relationship));
		if (response.getStatus()!=201) {
			logger.log(Level.SEVERE, "Error in init Neo4j: Error in the remote operation (relationship). " +response.getStatus()+" "+response.getStatusInfo());
			throw new InternalServerErrorException();
		}
		
		responseRelationship = response.readEntity(Relationship.class);
		return responseRelationship;
	}
	
	private List<String> sendShortestPathsRequest(String source, String destination, int maxlength) {
		int max_depth;
		logger.log(Level.INFO, "Send shortest path request to Neo4j");
		it.polito.dp2.RNS.sol3.jaxb.neo4j.ObjectFactory of;
		Node sourceNode = neo4jDB.getNodeByPlaceId(source);
		if (sourceNode != null){
			Node destinationNode = neo4jDB.getNodeByPlaceId(destination);
			if (destinationNode != null){
				
				
				ShortestPathsRequest requestShortPaths = new ShortestPathsRequest();		
				if (maxlength<=0)
					max_depth = Integer.MAX_VALUE;
				else
					max_depth = maxlength;
				requestShortPaths.setMaxDepth(BigInteger.valueOf(max_depth));
				requestShortPaths.setAlgorithm("shortestPath");
				requestShortPaths.setTo(destinationNode.getSelf());
				of = new ObjectFactory();
				ShortestPathsRequest.Relationships rel = of.createShortestPathsRequestRelationships();
				rel.setDirection("out");
				rel.setType("ConnectedTo");
				requestShortPaths.setRelationships(rel);
				if (nodeValidation(requestShortPaths)){
					String targetUri = sourceNode.getSelf();
					
					// build the web target
					WebTarget target = client.target(UriBuilder.fromUri(targetUri+"/paths"));
					
					Response response = target
							   .request()
							   .accept(MediaType.APPLICATION_JSON)
							   .post(Entity.json(requestShortPaths));
					if (response.getStatus()!=200) {
						logger.log(Level.SEVERE, "Error in shortPathRequest Neo4j: Error in the remote operation (shortest path). "+response.getStatus()+" "+response.getStatusInfo());
						throw new InternalServerErrorException();
					}
					
					List<ShortPath> shortestPaths = response.readEntity(new GenericType<List<ShortPath>>() {});
					//iterate all paths
					for (ShortPath paths :shortestPaths){
						if (nodeValidation(paths)==true){
							List<String> pathUrl = paths.getNodes();
							List<String> pathID = new ArrayList<String>();
							for (String nodeUrl : pathUrl){
								String placeId = neo4jDB.getPlaceIdByURL(nodeUrl);
								pathID.add(placeId);
							}
							return pathID;
						}else{
							//error in the validation of the response
							logger.log(Level.SEVERE, "Error in shortPathRequest Neo4j: Validation of response fail");
							throw new InternalServerErrorException();
						}
					}
					//no path returned
					return null;
				}else{
					//error in the validation of request
					logger.log(Level.SEVERE, "Error in shortPathRequest Neo4j: Validation error of request");
					throw new InternalServerErrorException();
				}
			}else{
				//error destination node is not in the local db
				logger.log(Level.SEVERE, "Error in shortPathRequest Neo4j: Destination node is not in the DB");
				throw new InternalServerErrorException();
			}
		}else{
			//error source node in not in the local db
			logger.log(Level.SEVERE, "Error in shortPathRequest Neo4j: Origin node is not in the DB");
			throw new InternalServerErrorException();
		}
	}
	
	
	
	
	
}
