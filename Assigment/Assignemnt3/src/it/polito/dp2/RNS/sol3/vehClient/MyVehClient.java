package it.polito.dp2.RNS.sol3.vehClient;

import java.util.List;

import it.polito.dp2.RNS.VehicleState;
import it.polito.dp2.RNS.VehicleType;
import it.polito.dp2.RNS.lab3.EntranceRefusedException;
import it.polito.dp2.RNS.lab3.ServiceException;
import it.polito.dp2.RNS.lab3.UnknownPlaceException;
import it.polito.dp2.RNS.lab3.VehClient;
import it.polito.dp2.RNS.lab3.WrongPlaceException;

public class MyVehClient implements VehClient{
	//private VehServiceManager service = VehServiceManager.getVehServiceManager(); 
	private VehServiceManager service;
	
	public MyVehClient() throws ServiceException{
		 service = new VehServiceManager();
	}
	
	@Override
	public List<String> enter(String plateId, VehicleType type, String inGate, String destination)
			throws ServiceException, UnknownPlaceException, WrongPlaceException, EntranceRefusedException {
		// TODO Auto-generated method stub
		return service.enterRequest(plateId, type, inGate, destination);
	}

	@Override
	public List<String> move(String newPlace) throws ServiceException, UnknownPlaceException, WrongPlaceException {
		// TODO Auto-generated method stub
		return service.moveRequest(newPlace);
	}

	@Override
	public void changeState(VehicleState newState) throws ServiceException {
		// TODO Auto-generated method stub
		service.changeStateRequest(newState);
		return;
		
	}

	@Override
	public void exit(String outGate) throws ServiceException, UnknownPlaceException, WrongPlaceException {
		// TODO Auto-generated method stub
		service.exitRequest(outGate);
		return;
	}

}
