package it.polito.dp2.RNS.sol3.service.RnsService;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

@Provider
public class RnsDeployer implements ApplicationEventListener{
	private static Logger logger = Logger.getLogger(RnsDeployer.class.getName());

	@Override
	public void  onEvent(ApplicationEvent applicationEvent) {
		// TODO Auto-generated method stub
		 switch (applicationEvent.getType()) {
         case INITIALIZATION_START: {
        	 logger.log(Level.INFO, "Initialization of DB started");
        	 InitRns initDB = InitRns.getInitRns();
        	 logger.log(Level.INFO, "DB succesfully initialized");
         }
         
     }
	}

	@Override
	public RequestEventListener onRequest(RequestEvent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	

}
