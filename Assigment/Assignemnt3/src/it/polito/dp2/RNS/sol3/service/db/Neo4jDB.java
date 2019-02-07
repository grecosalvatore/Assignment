package it.polito.dp2.RNS.sol3.service.db;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.Place;
import it.polito.dp2.RNS.sol3.jaxb.neo4j.Node;

public class Neo4jDB {
	private static Neo4jDB neo4jDB = new Neo4jDB();
	private HashMap<String,Node> nodeByPlaceId;  // <placeId,node>
	private HashMap<String,String> placeIdByURL; //<url,placeId>
	
	private Neo4jDB() {
		nodeByPlaceId = new HashMap<String,Node>();
		placeIdByURL = new HashMap<String,String>();
	}
	
	public static Neo4jDB getNeo4jDB() {
		return neo4jDB;
	}
	
	public Node getNodeByPlaceId(String placeId){
		if (nodeByPlaceId.containsKey(placeId))
			return nodeByPlaceId.get(placeId);
		return null;
	}
	
	public String getPlaceIdByURL(String url){
		if (placeIdByURL.containsKey(url))
			return placeIdByURL.get(url);
		return null;
	}
	
	public boolean addNode(String placeId,Node node){
		if (nodeByPlaceId.containsKey(placeId))
			return false;
		else{
			nodeByPlaceId.put(placeId, node);
			if (node.getSelf() == null)
				return false;
			placeIdByURL.put(node.getSelf(),placeId);
			return true;
		}
	}
}
