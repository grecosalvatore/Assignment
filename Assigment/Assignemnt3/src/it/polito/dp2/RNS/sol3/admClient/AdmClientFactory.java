package it.polito.dp2.RNS.sol3.admClient;

import it.polito.dp2.RNS.lab3.AdmClient;
import it.polito.dp2.RNS.lab3.AdmClientException;
import it.polito.dp2.RNS.lab3.ServiceException;

public class AdmClientFactory extends it.polito.dp2.RNS.lab3.AdmClientFactory{

	@Override
	public AdmClient newAdmClient() throws AdmClientException {
		// TODO Auto-generated method stub
		try {
			return new MyAdmClient();
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			//impossible create the admin client for an internal server error
			e.printStackTrace();
			return null;
		}
	}

}
