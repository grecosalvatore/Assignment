package it.polito.dp2.RNS.sol2;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
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
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import it.polito.dp2.RNS.PlaceReader;
import it.polito.dp2.RNS.lab2.ModelException;
import it.polito.dp2.RNS.lab2.ServiceException;
import it.polito.dp2.RNS.lab2.UnknownIdException;
import it.polito.dp2.RNS.sol2.*;
import it.polito.dp2.RNS.sol2.ShortestPathsRequest.Relationships;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

public class MyClientNeo4j {

	private  Client client;
	private static final String serviceBaseUri = System.getProperty("it.polito.dp2.RNS.lab2.URL");
	private Map<String, Node> mapPlaceIDtoNode;
	private Map<String,PlaceReader> mapUrlToPlace;
	private JAXBContext jc;
	private javax.xml.validation.Validator validator;
	
	protected MyClientNeo4j() throws SAXException, JAXBException {
		this.client = ClientBuilder.newClient();
		
		// create validator that uses the DataTypes schema
    	SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
    	Schema schema = sf.newSchema(new File("custom/dataTypesSchema.xsd"));
    	validator = schema.newValidator();
    	
		// create JAXB context related to the classed generated from the DataTypes schema
        jc = JAXBContext.newInstance("it.polito.dp2.RNS.sol2");
		System.out.println("JAX-RS client created");
		return;
	}
	
	public void loadGraph(Set<PlaceReader> placeList) throws ModelException, ServiceException{
		if (placeList != null){
			this.mapPlaceIDtoNode = new HashMap<String,Node>();
			this.mapUrlToPlace = new HashMap<String,PlaceReader>();
			loadNodes(placeList);
			loadRelationships(placeList);
		}else{
			throw new ModelException();
		}
		return;
	}
	
	protected void loadNodes(Set<PlaceReader> placeList) throws ModelException, ServiceException{
		for (PlaceReader place : placeList){
			String placeId = place.getId();
			CreateNodeType node = new CreateNodeType();
			node.setId(placeId);
			//validate the node against schema
			if (nodeValidation(node) == true){
				//if no error occurred in the validation send a request to neo4j
				Node responseNode = sendCreationNodeRequest(node);
				//validate the response object against schema
				if (nodeValidation(responseNode) == true){
					//save in local DB
					this.mapPlaceIDtoNode.put(placeId, responseNode);
					//save mapping url - placereader
					this.mapUrlToPlace.put(responseNode.getSelf(),place);
				}else{
					throw new ModelException();
				}
			}else{
				throw new ModelException();
			}
		}
		return;
	}
	
	//send a creation node to neo4j and return a nodeObject
	protected Node sendCreationNodeRequest(CreateNodeType node) throws ModelException, ServiceException{
		// build the web target	
		WebTarget target = client.target(getBaseURI()).path("node");
		Node responseNode;
		try{
			Response response = target
					   .request()
					   .accept(MediaType.APPLICATION_JSON)
					   .post(Entity.json(node));
			if (response.getStatus()!=201) {
				System.out.println("Error in remote operation: "+response.getStatus()+" "+response.getStatusInfo());
				if (response.getStatus()>=500)
					throw new ServiceException();
				else
					throw new ModelException();
			}
			
			responseNode = response.readEntity(Node.class);
		} catch (ProcessingException | IllegalStateException e) {
			throw new ServiceException("JAX-RS client has raised an exception: " + e.getMessage());
		}
		return responseNode;
	}
	
	protected boolean nodeValidation(Object node){
		try {
	    	JAXBSource source = new JAXBSource(jc, node);
	    	validator.validate(source);
		} catch (org.xml.sax.SAXException se) {
		      System.out.println("Validation Failed");
		      // print error messages
		      Throwable t = se;
		      while (t!=null) {
			      String message = t.getMessage();
			      if (message!= null)
			    	  System.out.println(message);
			      t = t.getCause();
		      }
		      return false;
		} catch (IOException e) {
			System.out.println("Unexpected I/O Exception");
			return false;
		} catch (JAXBException e) {
			System.out.println("Unexpected JAXB Exception");
			return false;
		}
		return true;
	}
	
	protected void loadRelationships(Set<PlaceReader> placeList) throws ModelException, ServiceException{
		for (PlaceReader fromPlace : placeList){
			String fromID = fromPlace.getId();
			Set<PlaceReader> connections = fromPlace.getNextPlaces();
			if (mapPlaceIDtoNode.containsKey(fromID)){
				//the place is in the db
				for (PlaceReader toPlace : connections){
					String toID = toPlace.getId();
					CreateRelationshipType relationship = new CreateRelationshipType();
					if (mapPlaceIDtoNode.containsKey(toID)){
						System.out.println("Sending "+fromID + " IsConnectedTo " + toID);
						String toUri = mapPlaceIDtoNode.get(toID).getSelf();
						relationship.setTo(toUri);
						relationship.setType("ConnectedTo");
						//validate the node against schema
						if (nodeValidation(relationship) == true){
							//if no error occurred in the validation send a request to neo4j
							Relationship responseRelationship = sendCreationRelationshipRequest(fromID,relationship);
							
							//validate the response object against schema
							if (nodeValidation(responseRelationship) == true){
								//save in local DB
							}else{
								throw new ModelException();
							}
						}
					}else{
						//error : to node not present in the db
						throw new ModelException();
					}
				}
			}else{
				//error: the place is not in the db
				throw new ModelException();
			}
		}
		return;
	}
	
	protected Relationship sendCreationRelationshipRequest (String fromId,CreateRelationshipType relationship) throws ServiceException, ModelException{
		Node fromNode = mapPlaceIDtoNode.get(fromId);
		Relationship responseRelationship;
		try{
			String targetUri = fromNode.getSelf();
			
			// build the web target
			WebTarget target = client.target(UriBuilder.fromUri(targetUri+"/relationships"));
			
			
			Response response = target
					   .request()
					   .accept(MediaType.APPLICATION_JSON)
					   .post(Entity.json(relationship));
			if (response.getStatus()!=201) {
				System.out.println("Error in remote operation: "+response.getStatus()+" "+response.getStatusInfo());
				if (response.getStatus()>=500)
					throw new ServiceException();
				else
					throw new ModelException();
			}
			
			responseRelationship = response.readEntity(Relationship.class);
		} catch (ProcessingException | IllegalStateException e) {
			throw new ServiceException("JAX-RS client has raised an exception: " + e.getMessage());
		}
		return responseRelationship;
	}
	
	public Set<List<String>> sendShortestPathsRequest(String source, String destination, int maxlength) throws UnknownIdException, ServiceException{
		int max_depth;
		ObjectFactory of;
		try{
			if (this.mapPlaceIDtoNode.containsKey(source)){
				if (this.mapPlaceIDtoNode.containsKey(destination)){
					Node sourceNode = this.mapPlaceIDtoNode.get(source);
					Node destinationNode = this.mapPlaceIDtoNode.get(destination);
					ShortestPathsRequest requestShortPaths = new ShortestPathsRequest();		
					if (maxlength<=0)
						max_depth = Integer.MAX_VALUE;
					else
						max_depth = maxlength;
					requestShortPaths.setMaxDepth(BigInteger.valueOf(max_depth));
					requestShortPaths.setAlgorithm("shortestPath");
					requestShortPaths.setTo(destinationNode.getSelf());
					of = new ObjectFactory();
					Relationships rel = of.createShortestPathsRequestRelationships();
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
							System.out.println("Error in remote operation: "+response.getStatus()+" "+response.getStatusInfo());
							if (response.getStatus()<500&&response.getStatus()>400)
								throw new UnknownIdException();
							else
								throw new ServiceException();
						}
						
						List<ShortPath> shortestPaths = response.readEntity(new GenericType<List<ShortPath>>() {});
						Set<List<String>> reponsePaths = new HashSet<List<String>>();
						//iterate all paths
						for (ShortPath paths :shortestPaths){
							if (nodeValidation(paths)==true){
								System.out.println("scanning path");
								List<String> pathUrl = paths.getNodes();
								List<String> pathID = new ArrayList<String>();
								System.out.println("path : ");
								for (String nodeUrl : pathUrl){
									PlaceReader place = this.mapUrlToPlace.get(nodeUrl);
									pathID.add(place.getId());
									System.out.println(place.getId());
								}
								reponsePaths.add(pathID);
							}else{
								//error in the validation of the response
								System.out.println("Error in the validation of short path response\n");
							}
						}
						return 	reponsePaths;
						
					}
				}else{
					//error destination node is not in the local db
					throw new UnknownIdException();
				}
			}else{
				//error source node in not in the local db
				throw  new UnknownIdException();
			}
		} catch (ProcessingException | IllegalStateException e) {
			throw new ServiceException("JAX-RS client has raised an exception: " + e.getMessage());
		}
		return null;
	}
	
	
	protected static UriBuilder getBaseURI() {
		return UriBuilder.fromUri(serviceBaseUri + "/data");
	}
	
	
}
