package it.polito.dp2.RNS.sol1;
import java.util.HashMap;
import java.util.Map;

import it.polito.dp2.RNS.ConnectionReader;
import it.polito.dp2.RNS.PlaceReader;
import it.polito.dp2.RNS.sol1.jaxb.PlaceType;;

public class MyConnectionType implements ConnectionReader{
	String from;
	String to;
	private Map<String,PlaceType> mapNamePlace = new HashMap<String,PlaceType>();
	
	public MyConnectionType(Map<String,PlaceType> map){
		this.mapNamePlace = map;
		from = null;
		to = null;
		return;
	}
	
	@Override
	public PlaceReader getFrom() {
		// TODO Auto-generated method stub
		return new MyPlaceType(mapNamePlace.get(from),mapNamePlace);
	}

	@Override
	public PlaceReader getTo() {
		// TODO Auto-generated method stub
		return new MyPlaceType(mapNamePlace.get(to),mapNamePlace);
	}
	
	public void setFrom(String s){
		from = s;
		return;
	}
	
	public void setTo(String s){
		to = s;
		return;
	}

}
