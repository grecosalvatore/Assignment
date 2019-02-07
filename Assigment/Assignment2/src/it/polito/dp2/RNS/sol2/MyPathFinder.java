package it.polito.dp2.RNS.sol2;

import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.xml.sax.SAXException;

import it.polito.dp2.RNS.RnsReader;
import it.polito.dp2.RNS.lab2.BadStateException;
import it.polito.dp2.RNS.lab2.ModelException;
import it.polito.dp2.RNS.lab2.PathFinder;
import it.polito.dp2.RNS.lab2.PathFinderException;
import it.polito.dp2.RNS.lab2.ServiceException;
import it.polito.dp2.RNS.lab2.UnknownIdException;

public class MyPathFinder implements PathFinder{
	private RnsReader RNS;
	private Boolean operatingState = false;//current state start in initialState
	private MyClientNeo4j myClient;
	public MyPathFinder(RnsReader rns) throws  PathFinderException{
		this.RNS = rns;
		try {
			myClient = new MyClientNeo4j();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return;
	}

	//This method return true if the current state is operating state and false if the current state is initial state
	@Override
	public boolean isModelLoaded() {
		// TODO Auto-generated method stub
		return this.operatingState;
	}

	@Override
	public void reloadModel() throws ServiceException, ModelException {
		// TODO Auto-generated method stub
		System.out.println("Reload model Request...\n");
		this.operatingState = false;
		myClient.loadGraph(this.RNS.getPlaces(null));
		//current state become: OperatingState
		this.operatingState = true;
	}

	@Override
	public Set<List<String>> findShortestPaths(String source, String destination, int maxlength)
			throws UnknownIdException, BadStateException, ServiceException {
		// TODO Auto-generated method stub
		System.out.println("Find Shortest Path Request ...\n");
		System.out.println("source " + source);
		System.out.println("destination " + destination);
		if (this.operatingState == false){
			throw  new BadStateException();
		}
		 Set<List<String>> responsePath = myClient.sendShortestPathsRequest(source, destination, maxlength);
		return responsePath;
	}

}
