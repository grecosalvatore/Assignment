package it.polito.dp2.RNS.sol2;

import javax.xml.bind.JAXBException;

import org.xml.sax.SAXException;

import it.polito.dp2.RNS.RnsReader;
import it.polito.dp2.RNS.RnsReaderException;
import it.polito.dp2.RNS.RnsReaderFactory;
import it.polito.dp2.RNS.lab2.PathFinder;
import it.polito.dp2.RNS.lab2.PathFinderException;

public class PathFinderFactory extends it.polito.dp2.RNS.lab2.PathFinderFactory{

	@Override
	public PathFinder newPathFinder() throws PathFinderException {
		// TODO Auto-generated method stub
		try {
			RnsReader monitor = RnsReaderFactory.newInstance().newRnsReader();							// instantiate a new NFV reader
			return new MyPathFinder(monitor);
		} catch(RnsReaderException re) {		
			throw new PathFinderException();
		}
	}

}
