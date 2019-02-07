package it.polito.dp2.RNS.sol3.admClient.library;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.polito.dp2.RNS.PlaceReader;
import it.polito.dp2.RNS.sol3.jaxb.admClient.*;

public class MyPlaceType implements PlaceReader{
	private PlaceType place;
	private Set<PlaceReader> nextPlaceSet;
	private Map<String,PlaceType> mapNamePlace = new HashMap<String,PlaceType>();
	
	public MyPlaceType(PlaceType p,Map<String,PlaceType> map){
		this.place = p;
		this.mapNamePlace = map;
	}
	
	@Override
	public String getId() {
		// TODO Auto-generated method stub
		MyIdentifiedEntityType ie = new MyIdentifiedEntityType(place.getIdentifiedEntity());
		return ie.getId();
	}

	@Override
	public int getCapacity() {
		// TODO Auto-generated method stub
		return place.getCapacity().intValue();
	}

	@Override
	public Set<PlaceReader> getNextPlaces() {
		// TODO Auto-generated method stub
		if (nextPlaceSet == null){
			nextPlaceSet = new HashSet<PlaceReader>();
		}
		if(nextPlaceSet.isEmpty()){
			List<String> placeList = this.place.getIsConnectedTo();		
			for(String s: placeList) {
				PlaceType placeT = mapNamePlace.get(s);
				nextPlaceSet.add( new MyPlaceType(placeT,this.mapNamePlace) );
			}
		}		
		return nextPlaceSet;	
	}

}
