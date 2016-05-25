package com.mmone.ota.rpc;

import java.math.BigDecimal;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.opentravel.ota._2003._05.OTAHotelDescriptiveInfoRQ;
import org.opentravel.ota._2003._05.OTAHotelDescriptiveInfoRS;


// TODO: Auto-generated Javadoc
/**
 * The Class Facilities.
 */
public class Facilities {
        public static final String ERR_INVALID_DATE = "15";
        public static final String ERR_PRICE_CANNOT_BE_VALIDATED = "58";
        public static final String ERR_BOOKING_REFERENCE_INVALID = "87";
        public static final String ERR_MANDATORY_BOOKING_DETAILS_MISSING = "113";
        public static final String ERR_BOARD_BASIS_OR_MEAL_PLAN_INVALID = "115";
        public static final String ERR_ROOM_OR_UNIT_TYPE_INVALID = "131";
        public static final String ERR_ROOM_OR_UNIT_TYPE_NO_AVAILABILITY = "132";
        public static final String ERR_END_DATE_INVALID = "135";
        public static final String ERR_START_DATE_INVALID = "136";
        public static final String ERR_ROOM_UNIT_CODE_INCORRECT = "141";
        public static final String ERR_PRICE_INCORRECT_FOR_ROOM_UNIT = "143";
        public static final String ERR_NUMBER_OF_ROOMS_UNITS_REQUIRED = "152";
        public static final String ERR_ROOM_OR_UNIT_PRICE_REQUIRED = "154";
        public static final String ERR_LANGUAGE_CODE_INVALID = "184";
        public static final String ERR_INVALID_CONFIRMATION_NUMBER = "245";
        public static final String ERR_INVALID_RATE_CODE = "249";
        public static final String ERR_NO_RESERVATIONS_FOUND_FOR_SEARCH_CRITERIA = "284";
        public static final String ERR_INVALID_BOOKING_SOURCE = "305";
        public static final String ERR_INVALID_VALUE = "320";
        public static final String ERR_REQUIRED_FIELD_MISSING = "321";
        public static final String ERR_INVALID_HOTEL_CODE = "392";
        public static final String ERR_INVALID_PROPERTY_CODE = "400";
        public static final String ERR_SYSTEM_ERROR = "448";
        public static final String ERR_INVALID_REQUEST_CODE = "459";

        public static final String EWT_UNKNOWN = "1";
        public static final String EWT_NO_IMPLEMENTATION = "2";
        public static final String EWT_PROTOCOL_VIOLATION = "7";
        public static final String EWT_REQUIRED_FIELD_MISSING = "10";

	/** The Constant xmlNameSpace. */
	public static final String xmlNameSpace = "http://www.opentravel.org/OTA/2003/05";

        public static final String TARGET_PRODUCTION = "Production";
        public static final String TARGET_TEST = "Test";
        public static final String VERSION = "1.0";

        public static final int TEST_ENVIRONMENT = 1;
        public static final int PRODUCTION_ENVIRONMENT = 2;


        static {
            System.out.println(Facilities.class.getName() + "v 1.00001");
        }
        
	/**
	 * Gets the today.
	 * 
	 * @return the today
	 * 
	 * @throws DatatypeConfigurationException the datatype configuration exception
	 */
	public static XMLGregorianCalendar getToday() throws DatatypeConfigurationException{
            GregorianCalendar today =  new GregorianCalendar();
            DatatypeFactory factory;
            try {
                factory = DatatypeFactory.newInstance();
                XMLGregorianCalendar calendar = factory.newXMLGregorianCalendar(
                    today.get(GregorianCalendar.YEAR),
                    today.get(GregorianCalendar.MONTH) + 1,
                    today.get(GregorianCalendar.DAY_OF_MONTH),
                    today.get(GregorianCalendar.HOUR),
                    today.get(GregorianCalendar.MINUTE),
                    today.get(GregorianCalendar.SECOND),  0,0);

                return calendar; 
            } catch (DatatypeConfigurationException ex) {
                Logger.getLogger(Facilities.class.getName()).log(Level.SEVERE, null, ex);
                throw ex;
            }

	}
	
}
