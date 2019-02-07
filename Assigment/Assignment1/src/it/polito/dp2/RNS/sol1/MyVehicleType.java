package it.polito.dp2.RNS.sol1;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import it.polito.dp2.RNS.PlaceReader;
import it.polito.dp2.RNS.VehicleReader;
import it.polito.dp2.RNS.VehicleState;
import it.polito.dp2.RNS.sol1.jaxb.*;

public class MyVehicleType implements VehicleReader{
	private VehicleType vehicle;
	private Map<String,PlaceType> mapNamePlace = new HashMap<String,PlaceType>();
	
	public MyVehicleType(VehicleType v,Map<String,PlaceType> map){
		this.vehicle = v;
		this.mapNamePlace = map;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		MyIdentifiedEntityType ie = new MyIdentifiedEntityType(vehicle.getIdentifiedEntity());
		return ie.getId();
	}

	@Override
	public PlaceReader getDestination() {
		// TODO Auto-generated method stub
		MyPlaceType destinationPlace = new MyPlaceType(this.mapNamePlace.get(this.vehicle.getDestination()),this.mapNamePlace);
		return destinationPlace;
	}

	@Override
	public Calendar getEntryTime() {
		// TODO Auto-generated method stub
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(vehicle.getEntryTime().toGregorianCalendar().getTimeInMillis());
		return calendar;
	}

	@Override
	public PlaceReader getOrigin() {
		// TODO Auto-generated method stub
		MyPlaceType originPlace = new MyPlaceType(this.mapNamePlace.get(this.vehicle.getOrigin()),this.mapNamePlace);
		return originPlace;
	}

	@Override
	public PlaceReader getPosition() {
		// TODO Auto-generated method stub
		MyPlaceType positionPlace = new MyPlaceType(this.mapNamePlace.get(this.vehicle.getPosition()),this.mapNamePlace);
		return positionPlace;
	}

	@Override
	public VehicleState getState() {
		// TODO Auto-generated method stub
		return VehicleState.valueOf(this.vehicle.getVehicleState().value());
	}

	@Override
	public it.polito.dp2.RNS.VehicleType getType() {
		// TODO Auto-generated method stub
		return it.polito.dp2.RNS.VehicleType.valueOf(vehicle.getVehicleType().value());
	}

	


}
