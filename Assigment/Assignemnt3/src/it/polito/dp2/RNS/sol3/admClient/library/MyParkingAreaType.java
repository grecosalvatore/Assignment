package it.polito.dp2.RNS.sol3.admClient.library;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.polito.dp2.RNS.ParkingAreaReader;
import it.polito.dp2.RNS.PlaceReader;
import it.polito.dp2.RNS.sol3.jaxb.admClient.*;

public class MyParkingAreaType extends MyPlaceType implements ParkingAreaReader{
	private ParkingAreaType parkingArea;
	private Set<String> serviceSet;
	
	public MyParkingAreaType(PlaceType p,ParkingAreaType pa,Map<String,PlaceType> map) {
		super(p,map);
		this.parkingArea = pa;
		// TODO Auto-generated constructor stub
	}

	@Override
	public int getCapacity() {
		// TODO Auto-generated method stub
		return super.getCapacity();
	}

	@Override
	public Set<PlaceReader> getNextPlaces() {
		// TODO Auto-generated method stub
		return super.getNextPlaces();
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return super.getId();
	}

	@Override
	public Set<String> getServices() {
		// TODO Auto-generated method stub
		if (serviceSet == null){
			serviceSet = new HashSet<String>();
		}
		if (serviceSet.isEmpty()){
			List<String> serviceList = parkingArea.getService();
			for(String s: serviceList) {
				// declaration of the interface for host info reading
				serviceSet.add(new String(s));
			}
		}
		return serviceSet;
	}

}
