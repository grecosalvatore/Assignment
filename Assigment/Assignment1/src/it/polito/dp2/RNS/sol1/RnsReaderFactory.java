package it.polito.dp2.RNS.sol1;

import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import it.polito.dp2.RNS.RnsReader;
import it.polito.dp2.RNS.RnsReaderException;
import it.polito.dp2.RNS.sol1.jaxb.*;

//Deserializer Class
public class RnsReaderFactory extends it.polito.dp2.RNS.RnsReaderFactory{

	@Override
	public RnsReader newRnsReader() throws RnsReaderException {
		// TODO Auto-generated method stub
		String fileName = System.getProperty("it.polito.dp2.RNS.sol1.RnsInfo.file");
		
		MyRnsType myRns;
		try {
			JAXBContext jc = JAXBContext.newInstance("it.polito.dp2.RNS.sol1.jaxb");

			Unmarshaller u = jc.createUnmarshaller();

			SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = sf.newSchema(new File("." + File.separator + "xsd" + File.separator + "rnsInfo.xsd"));
			u.setSchema(schema);

			File xml = new File(fileName);
			JAXBElement<RnsType> rns = u.unmarshal(new StreamSource(xml), RnsType.class);
			RnsType rnsType = rns.getValue();

			myRns = new MyRnsType(rnsType);
		} catch (JAXBException | SAXException e) {
			e.printStackTrace();
			throw new RnsReaderException(e, "Unmarshal failed");
		}
		return myRns;
	}

}
