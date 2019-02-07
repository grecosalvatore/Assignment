package it.polito.dp2.RNS.sol3.vehClient;

import it.polito.dp2.RNS.lab3.ServiceException;
import it.polito.dp2.RNS.lab3.VehClient;
import it.polito.dp2.RNS.lab3.VehClientException;

public class VehClientFactory extends it.polito.dp2.RNS.lab3.VehClientFactory{

	@Override
	public VehClient newVehClient() throws VehClientException {
		// TODO Auto-generated method stub
		try {
			return new MyVehClient();
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Error: Impossible create the vehicle client");
			return null;
		}
	}

}
