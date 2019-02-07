package it.polito.dp2.RNS.sol3.service.resources;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


@Provider
@Consumes({"application/json","text/json"})
public class JsonValidationInterceptor implements ReaderInterceptor {
	private Schema schema=null;
	private JAXBContext jc=null;
	String responseBodyTemplate;
	Logger logger;

	public JsonValidationInterceptor() {
		logger = Logger.getLogger(XmlValidationProvider.class.getName());

	    try {
	    	InputStream schemaStream = XmlValidationProvider.class.getResourceAsStream("/xsd/RnsSystem.xsd");
			if (schemaStream == null) {
				logger.log(Level.SEVERE, "xml schema file Not found.");
				throw new IOException();
			}
	    	jc = JAXBContext.newInstance("it.polito.dp2.RNS.sol3.jaxb.rnsSystem");
	    	SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
	    	//sf.setErrorHandler(new MyErrorHandler());
	    	schema = sf.newSchema(new StreamSource(schemaStream));
			InputStream templateStream = XmlValidationProvider.class.getResourceAsStream("/html/BadRequestBodyTemplate.html");
			if (templateStream == null) {
				logger.log(Level.SEVERE, "html template file Not found.");
				throw new IOException();
			}
	        BufferedReader reader = new BufferedReader(new InputStreamReader(templateStream));
	        StringBuilder out = new StringBuilder();
	        String line;
	        while ((line = reader.readLine()) != null) {
	            out.append(line);
	        }
			responseBodyTemplate = out.toString();
            logger.log(Level.INFO, "JsonValidationInterceptor initialized successfully");
		} catch (org.xml.sax.SAXException se) {
			logger.log(Level.SEVERE,"Schema for service is not available or not valid.");
		} catch (JAXBException e) {
			logger.log(Level.SEVERE,"Initialization of JAXBContext failed.");
		} catch (Exception e) {
			logger.log(Level.SEVERE,"Runtime error occurred while initializing Biblio Service for JSON validation");
		}
	}
	
	public void validate(Object item) {
    	try {
			JAXBSource source = new JAXBSource(jc, item);
			Validator v = schema.newValidator();
			//v.setErrorHandler(new MyErrorHandler());
			v.validate(source);
		} catch (SAXException e) {
			String validationErrorMessage = "Request body validation error";
			if (e.getMessage()!=null)
				validationErrorMessage += ": " +e.getMessage();
			Throwable linked = e.getCause();
			while (linked != null) {
				if (linked instanceof SAXParseException && linked.getMessage()!=null)
					validationErrorMessage += ": " + linked.getMessage();
				linked = linked.getCause();
			}
			BadRequestException bre = new BadRequestException("Request body validation error");
			String responseBody = responseBodyTemplate.replaceFirst("___TO_BE_REPLACED___", validationErrorMessage);
			Response response = Response.fromResponse(bre.getResponse()).entity(responseBody).build();
			throw new BadRequestException("Request body validation error", response);
		} catch (JAXBException | IOException | NullPointerException e) {
			throw new InternalServerErrorException();
		}
	}

	@Override
	public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
		Object ret = context.proceed();
		validate(ret);
		return ret;
	}

}
