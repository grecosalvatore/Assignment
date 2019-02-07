package it.polito.dp2.RNS.sol3.admClient.library;

import java.util.Map;
import java.util.Set;

import it.polito.dp2.RNS.PlaceReader;
import it.polito.dp2.RNS.RoadSegmentReader;
import it.polito.dp2.RNS.sol3.jaxb.admClient.*;

public class MyRoadSegmentType  extends MyPlaceType implements RoadSegmentReader{
	private RoadSegmentType roadSegment;
	
	public MyRoadSegmentType(PlaceType p ,RoadSegmentType rs,Map<String,PlaceType> map) {
		super(p,map);
		this.roadSegment = rs;
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
	public String getName() {
		// TODO Auto-generated method stub
		return roadSegment.getName();
	}

	@Override
	public String getRoadName() {
		// TODO Auto-generated method stub
		return roadSegment.getRoadName();
	}
}
