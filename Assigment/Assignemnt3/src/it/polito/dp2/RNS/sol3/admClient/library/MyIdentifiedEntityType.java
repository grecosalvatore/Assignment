package it.polito.dp2.RNS.sol3.admClient.library;

import it.polito.dp2.RNS.IdentifiedEntityReader;
import it.polito.dp2.RNS.sol3.jaxb.admClient.*;

public class MyIdentifiedEntityType implements IdentifiedEntityReader{
	private IdentifiedEntityType identifiedEntity;
	
	public MyIdentifiedEntityType(IdentifiedEntityType ie){
		this.identifiedEntity = ie;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return this.identifiedEntity.getId();
	}
}
