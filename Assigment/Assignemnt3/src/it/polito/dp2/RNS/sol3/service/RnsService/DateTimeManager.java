package it.polito.dp2.RNS.sol3.service.RnsService;



import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class DateTimeManager {

    private static final String dateFormatString = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String inputDateFormatString = "yyyy-MM-dd'T'HH:mm:ssZ";

    public DateTimeManager() {
    }

    //this method return an xmlgregoriancalendar with the current timestamp
    public XMLGregorianCalendar getCurrentXmlDate() throws DatatypeConfigurationException {
        GregorianCalendar currentTimestamp = new GregorianCalendar();
        currentTimestamp.setTime(new Date());//current timestamp

        return DatatypeFactory.newInstance().newXMLGregorianCalendar(currentTimestamp);
    }

    public XMLGregorianCalendar convertCalendar(String stringDate) throws ParseException, DatatypeConfigurationException {
        DateFormat dateFormat = new SimpleDateFormat(inputDateFormatString);

        // create a new calendar
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateFormat.parse(stringDate));

        return toXMLGregorianCalendar(cal);
    }


    // convert a xml gregorian calendar date to a calendar 
    public Calendar fromXMLGregorianCalendar(XMLGregorianCalendar xc)
            throws DatatypeConfigurationException {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(xc.toGregorianCalendar().getTimeInMillis());
        return c;
    }

    // convert a calendar instance into a gregorian calendar 
    public XMLGregorianCalendar toXMLGregorianCalendar(Calendar c)
            throws DatatypeConfigurationException {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(c.getTimeInMillis());
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
    }

}
