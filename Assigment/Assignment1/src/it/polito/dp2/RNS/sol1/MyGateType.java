package it.polito.dp2.RNS.sol1;


import java.util.Map;
import java.util.Set;

import it.polito.dp2.RNS.GateReader;
import it.polito.dp2.RNS.GateType;
import it.polito.dp2.RNS.PlaceReader;
import it.polito.dp2.RNS.sol1.jaxb.*;

public class MyGateType extends MyPlaceType implements GateReader{
	private String gate;
	public MyGateType(PlaceType p ,String g,Map<String,PlaceType> map) {
		super(p,map);
		this.gate = g;
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
		return super.getId();
	}

	@Override
	public GateType getType() {
		// TODO Auto-generated method stub
		return GateType.valueOf(this.gate);
	}

}
