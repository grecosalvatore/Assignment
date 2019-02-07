package it.polito.dp2.RNS.sol3.admClient;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import it.polito.dp2.RNS.lab3.ServiceException;
import it.polito.dp2.RNS.sol3.jaxb.admClient.IdentifiedEntityType;
import it.polito.dp2.RNS.sol3.jaxb.admClient.ParkingAreaType;
import it.polito.dp2.RNS.sol3.jaxb.admClient.PlaceType;
import it.polito.dp2.RNS.sol3.jaxb.admClient.RoadSegmentType;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.ConnectedTo.To;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.Place;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.Places;
import it.polito.dp2.RNS.sol3.jaxb.rnsSystem.Rns;

public class InitAdmClient {
	private Client client;
	private AdmDB db ;

	private static String serviceBaseUri = System.getProperty("it.polito.dp2.RNS.lab3.URL");
	
	public InitAdmClient() throws ServiceException{
		if (serviceBaseUri == null) {
            String servicePort = System.getProperty("PORT");
            if (servicePort == null) {
                servicePort = "8080";
            }
            serviceBaseUri = "http://localhost:" + servicePort + "/RnsSystem/rest";
        }
		client = ClientBuilder.newClient();
		db = new AdmDB();
		loadInfoResources();
		loadPlaces();
		return;
	}
	
	
	public AdmDB getDB(){
		return db;
	}
	
	
	public void loadInfoResources() throws ServiceException{
		
		//get base info of the web service (all link to principals resources)
		Response serverResponse = client.target(UriBuilder.fromUri(serviceBaseUri).path("/rns").build())
                .request()
                .accept(MediaType.APPLICATION_XML)
                .get();
		
		if (serverResponse.getStatusInfo().getStatusCode() != 200){
			// error in the server response
			throw new ServiceException();
		}
		
		Rns rnsInfo = serverResponse.readEntity(Rns.class);
		db.setRnsInfoUri(rnsInfo);
		return;
	}
	
	public void loadPlaces() throws ServiceException{
		
		Response serverResponse = client.target(UriBuilder.fromUri(db.getRnsInfoUri().getPlaces()).path("").build())
                .request()
                .accept(MediaType.APPLICATION_XML)
                .get();
		if (serverResponse.getStatusInfo().getStatusCode() != 200){
			// error
			throw new ServiceException();
			
		}
		Places places = serverResponse.readEntity(Places.class);
		if (places.getPlace()==null||places.getPlace().isEmpty()){
			//error empty place set
		}else{
			List<PlaceType> loadList = new ArrayList<PlaceType>();
			for (Place p : places.getPlace()){
				PlaceType place = new PlaceType();
				IdentifiedEntityType ie = new IdentifiedEntityType();
				ie.setId(p.getPlaceId());
				place.setIdentifiedEntity(ie);
				place.setCapacity(p.getCapacity());
				if (p.getGate()!=null){
					//is gate
					place.setGate(p.getGate());					
				}else{
					if (p.getParkingArea()!=null){
						ParkingAreaType pa = new ParkingAreaType();
						for (String service : p.getParkingArea().getService()){
							pa.getService().add(service);
						}
						place.setParkingArea(pa);
					}else{
						if (p.getRoadSegment() != null){
							RoadSegmentType rs = new RoadSegmentType();
							rs.setName(p.getRoadSegment().getName());
							rs.setRoadName(p.getRoadSegment().getRoadName());
							place.setRoadSegment(rs);
						}
					}
				}
				List<To> toList = p.getConnectedTo().getTo();
				if (toList.isEmpty()==false){
					for (To to : toList){
						place.getIsConnectedTo().add(to.getPlaceId());
					}
				}
				
				loadList.add(place);
			}
			db.getRns().loadPlace(loadList);
		}
		return;
	}
}
