package com.mmone.ota.hotel;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.sql.DataSource;
import javax.mail.Message;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.io.StringWriter;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult; 
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.w3c.dom.Document;
import sun.misc.BASE64Encoder;

// TODO: Auto-generated Javadoc
/**
 * The Class Facilities.
 */
public class Facilities {

    public static final String ERR_INVALID_DATE = "15";
    public static final String ERR_NAME_IS_MISSING_OR_INCOMPLETE = "19";
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
    public static final String ERR_NUMBER_OF_BERTHS_BEDS_INVALID = "205";
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

    public static final String TARGET_PRODUCTION = "Production";
    public static final String TARGET_TEST = "Test";
    public static final String VERSION = "1.0";
    public static final String DEFAULT_CURRENCY_CODE = "EUR";
    public static final int DEFAULTS_PORTAL_CODE = 1;
    public static final String RESID_TYPE_PMS = "10";
    public static final String RESID_TYPE_SINGLE_PMS = "11";
    public static final String ADULT_QUALYFING_CODE = "10";
    public static final String CHILD_QUALYFING_CODE = "8";
    public static final String NORMAL_RATE = "NR";
    public static final String SPECIAL_RATE = "SP";
    public static final String MULTI_RATE = "MR";
    public static final String INVENTORY = "IU";
    public static final String[] dateParsers = new String[]{"yyyyMMdd", "yyyy-MM-dd"};

    // ResStatus
    public static final String[] resStatus = new String[] { "Book", "Modify", "Cancel" };
    
    // PhoneTechType
    public static final String OTA_PTT_VOICE = "1";
    public static final String OTA_PTT_FAX = "3";

    // PhoneLocationType
    public static final String OTA_PLT_HOME = "6";
    public static final String OTA_PLT_MOBILE = "8";
    
    // trattamenti
    public static final int MPT_ROOM_ONLY = 14;
    public static final int MM_ROOM_ONLY = 1;
    //----------------------------------------
    public static final int MPT_BB = 3;
    public static final int MM_BB = 2;
    //----------------------------------------
    public static final int MPT_HALF_BOARD = 12;
    public static final int MM_HALF_BOARD = 3;
    //----------------------------------------
    public static final int MPT_FULL_BOARD = 10;
    public static final int MM_FULL_BOARD = 4;
    //----------------------------------------
    public static final int MPT_ALL_INCLUSIVE = 1;
    public static final int MM_ALL_INCLUSIVE = 5;

    // environment
    public static final int TEST_ENVIRONMENT = 1;
    public static final int PRODUCTION_ENVIRONMENT = 2;

    // rate
    public static final int OTA_RPT_1 = 13; // normal rate
    public static final int OTA_RPT_2 = 12; // special rate
    public static final int OTA_RPT_3 = 11; // multirate

    public static final String OTA_UIT_HOTEL = "10";
    public static final String OTA_UIT_CUSTOMER = "1";    
    
    public static final Map<Integer, Integer> RATE_MAP = new HashMap<Integer, Integer>();
    static {
        RATE_MAP.put(1, OTA_RPT_1);
        RATE_MAP.put(2, OTA_RPT_2);
    }

    public static final Map<Integer, String> MEAL_MAP = new HashMap<Integer, String>();
    static {
        MEAL_MAP.put(MM_ROOM_ONLY, "OB");
        MEAL_MAP.put(MM_BB, "BB");
        MEAL_MAP.put(MM_HALF_BOARD, "HB");
        MEAL_MAP.put(MM_FULL_BOARD, "FB");
        MEAL_MAP.put(MM_ALL_INCLUSIVE, "AI");
    }
     
    // HotelResNotif
    public static final int RESERVATION_INSERT_TYPE_PMS = 4;
    public static final int RESERVATION_PROFILE_TYPE = 1;
    public static final String RESERVATION_ROOM_CODE_PMS = "---";
    public static final int RESERVATION_STATUS_CONFIRMED = 1;
    public static final int RESERVATION_STATUS_CANCEL = 2;
    public static final int RESERVATION_STATUS_MODIFIED = 102;
    public static final String TEXT_FORMAT_PLAIN_TEXT = "PlainText";
    public static final int RESERVATION_DETAIL_ROOM_ID = 0;
    public static final int RESERVATION_DETAIL_LIST_ID = 1;

    public static final Map<String, Integer> RESSTATUS_MAP = new HashMap<String, Integer>();
    static {
        RESSTATUS_MAP.put(resStatus[0], RESERVATION_STATUS_CONFIRMED);
        RESSTATUS_MAP.put(resStatus[1], RESERVATION_STATUS_MODIFIED);
        RESSTATUS_MAP.put(resStatus[2], RESERVATION_STATUS_CANCEL);
    }
    
    public static final Map<Integer, String> sRATE_MAP = new HashMap<Integer, String>();
    static {
        sRATE_MAP.put(1, "Normal");
        sRATE_MAP.put(2, "Special");
        sRATE_MAP.put(3, "Multirate");
    }
    
    public static final Map<Integer, String> sRATE_CODES_MAP = new HashMap<Integer, String>();
    static {
        sRATE_CODES_MAP.put(1, NORMAL_RATE);
        sRATE_CODES_MAP.put(2, SPECIAL_RATE);
        sRATE_CODES_MAP.put(3, MULTI_RATE);
    }
    
    public static final int[] PERMISSIONS = {
        TEST_ENVIRONMENT, // TEST
        PRODUCTION_ENVIRONMENT // PRODUCTION
    };
    public static final int[] MM_TO_MPT = {
        /*00*/ MPT_ROOM_ONLY,
        /*01*/ MPT_ROOM_ONLY,
        /*02*/ MPT_BB,
        /*03*/ MPT_HALF_BOARD,
        /*04*/ MPT_FULL_BOARD,
        /*05*/ MPT_ALL_INCLUSIVE,
        /*06*/ MPT_ROOM_ONLY,
        /*07*/ MPT_ROOM_ONLY,
        /*08*/ MPT_ROOM_ONLY,
        /*09*/ MPT_ROOM_ONLY,
        /*10*/ MPT_ROOM_ONLY,
        /*11*/ MPT_ROOM_ONLY,
        /*12*/ MPT_ROOM_ONLY,
        /*13*/ MPT_ROOM_ONLY,
        /*14*/ MPT_ROOM_ONLY
    };
    public static final int[] MPT_TO_MM = {
        /*00*/ MM_ROOM_ONLY,
        /*01*/ MM_ALL_INCLUSIVE,
        /*02*/ MM_ROOM_ONLY,
        /*03*/ MM_BB,
        /*04*/ MM_ROOM_ONLY,
        /*05*/ MM_ROOM_ONLY,
        /*06*/ MM_ROOM_ONLY,
        /*07*/ MM_ROOM_ONLY,
        /*08*/ MM_ROOM_ONLY,
        /*09*/ MM_ROOM_ONLY,
        /*10*/ MM_FULL_BOARD,
        /*11*/ MM_ROOM_ONLY,
        /*12*/ MM_HALF_BOARD,
        /*13*/ MM_ROOM_ONLY,
        /*14*/ MM_ROOM_ONLY
    };
    // Table pricelist
    public static final int PRICE_LIST_LIST_ID = 0;
    public static final int PRICE_LIST_STRUCTURE_ID = 1;
    public static final int PRICE_LIST_ROOM_ID = 2;
    public static final int PRICE_LIST_PERIOD_ID = 3;
    public static final int PRICE_LIST_DATE_FROM = 4;
    public static final int PRICE_LIST_DATE_TO = 5;
    public static final int PRICE_LIST_PRICE_SET = 6;
    public static final int PRICE_LIST_EXTRABED_SET = 7;

    public static final int EXTRABED_SPECIFICATION_ID = 0;
    public static final int EXTRABED_GUEST_TYPE = 1;
    public static final int EXTRABED_GUEST_AGE = 2;
    public static final int EXTRABED_PRICE_VALUE = 3;
    public static final int EXTRABED_PRICE_TYPE = 4;
    public static final int EXTRABED_BED_NUMBER = 5;
    
    public static final String[] FLDS_PRICE_LIST = new String[]{
        "list_id",
        "structure_id",
        "room_id",
        "period_id",
        "pricelist_date_from",
        "pricelist_date_to",
        "pricelist_price_set",
        "pricelist_extrabed_set"
    };
    public static Object[] getEmptyPriceListValues() {
        return new Object[]{
                    0 /*list_id*/,
                    0 /*structure*/,
                    0 /*room*/,
                    0 /*period_id*/,
                    new Date(new java.util.Date().getTime()) /*pricelist_date_from*/,
                    new Date(new java.util.Date().getTime()) /*pricelist_date_to*/,
                    0 /*pricelist_price_set*/,
                    0 /*pricelist_extrabed_set*/
        };
    }
    public static final int PRICE_LIST_TREATMENT_PRICELIST_ID = 0;
    public static final String[] FLDS_PRICE_LIST_TREATMENT = new String[]{
        /* "treatment_id", */
        /* "price", */
        "extrabed_specification_id"
    };
    public static Object[] getEmptyPriceListTreatmentValues() {
        return new Object[]{
                    0 /*extrabed_specification_id*/};
    }
    public static final String[] FLDS_EXTRABED = new String[]{
        "extrabed_specification_id",
        "extrabed_guest_type", /* 0 adulti, 1 bambini */
        "extrabed_guest_age", /* 0 adulti */
        "extrabed_price_value",
        "extrabed_price_type", /* P V C */
        "extrabed_bed_number"
    };
    public static Object[] getEmptyExtrabedValues() {
        return new Object[]{
                    0 /*extrabed_specification_id*/,
                    0 /*extrabed_guest_type*/,
                    0 /*extrabed_guest_age*/,
                    0 /*extrabed_price_value*/,
                    "V" /*extrabed_price_type*/,
                    0 /*extrabed_bed_number*/};
    }
    public static final String TABLE_PRICELIST = "pricelist";
    public static final String TABLE_PRICELIST_TREATMENT = "pricelist_treatment";
    public static final String TABLE_EXTRABED = "extrabed";
    public static final String STM_INSERT_INTO_PRICELIST =
            com.mmone.ota.hotel.Facilities.insertIntoGenerator(TABLE_PRICELIST, FLDS_PRICE_LIST);
    public static final String STM_UPDATE_PRICELIST =
            com.mmone.ota.hotel.Facilities.updateGenerator(TABLE_PRICELIST, FLDS_PRICE_LIST, "pricelist_id");
    //public static final String STM_INSERT_INTO_PRICELIST_TREATMENT =
    //        com.mmone.ota.hotel.Facilities.insertIntoGenerator(TABLE_PRICELIST_TREATMENT, FLDS_PRICE_LIST_TREATMENT);
    public static final String STM_UPDATE_PRICELIST_TREATMENT =
            com.mmone.ota.hotel.Facilities.updateGenerator(TABLE_PRICELIST_TREATMENT, FLDS_PRICE_LIST_TREATMENT, "pricelist_id", "treatment_id");
    public static final String STM_INSERT_INTO_EXTRABED =
            com.mmone.ota.hotel.Facilities.insertIntoGenerator(TABLE_EXTRABED, FLDS_EXTRABED);
    public static final String SELECT_MAX_EXTRABED_SPECIFICATION_ID =
            "SELECT MAX(extrabed_specification_id) "
            + "FROM extrabed";
    public static final String SELECT_PRICE_LIST =
            "SELECT p.* , pt.*, rd.*, ppd.*, t.* "
            + "FROM pricelist AS p "
            + "INNER JOIN pricelist_treatment AS pt ON p.pricelist_id = pt.pricelist_id "
            + "INNER JOIN room_details as rd ON p.room_id = rd.room_id "
            + "LEFT JOIN pricelist_period_details AS ppd ON ppd.period_id = p.period_id "
            + "INNER JOIN treatment as t ON t.treatment_id = pt.treatment_id "
            + "WHERE p.structure_id = ? "
            + "AND p.list_id = ? "
            + "AND p.pricelist_date_to >= CURRENT_DATE "
            + "AND p.pricelist_active = 1 "
            + "AND rd.language = ? "
            + "AND pt.treatment_id IN (SELECT treatment_id "
            + "    FROM structure_treatment WHERE structure_treatment.structure_id = ? ) " + //ABS_STRUCTURE_ID
            "ORDER BY p.pricelist_date_from, p.room_id ";
    public static final String SELECT_PRICE_LIST_PERIOD =
            "SELECT p.* , pt.*, rd.*, ppd.*, t.* "
            + "FROM pricelist AS p "
            + "INNER JOIN pricelist_treatment AS pt ON p.pricelist_id = pt.pricelist_id "
            + "INNER JOIN room_details as rd ON p.room_id = rd.room_id "
            + "LEFT JOIN pricelist_period_details AS ppd ON ppd.period_id = p.period_id "
            + "INNER JOIN treatment as t ON t.treatment_id = pt.treatment_id "
            + "WHERE p.structure_id = ? "
            + "AND p.list_id = ? "
            + "AND p.pricelist_date_to >= CURRENT_DATE "
            + "AND p.pricelist_date_to >= ? " // st
            + "AND p.pricelist_date_from <=? " // en
            + "AND p.pricelist_active = 1 "
            + "AND rd.language = ? "
            + "AND pt.treatment_id IN (SELECT treatment_id "
            + "    FROM structure_treatment WHERE structure_treatment.structure_id = ? ) " + //ABS_STRUCTURE_ID
            "ORDER BY p.pricelist_date_from, p.room_id ";
    
    public static final String SELECT_STRUCTURE_PAYMENT =
            "SELECT * FROM structure_payment "
            + "WHERE portal_id=? AND structure_id=?";

    public static final String SELECT_EXTRABED =
            "SELECT * FROM extrabed "
            + "WHERE extrabed_specification_id = ? ORDER BY extrabed_bed_number,extrabed_guest_type,extrabed_guest_age";
    
    public static DataSource createDataSource(String user, String pwd, String url) throws NamingException, NoInitialContextException {

        MysqlDataSource mds = new MysqlDataSource();

        mds.setUrl(url);
        mds.setUser(user);
        mds.setPassword(pwd);

        return mds;
    }

    public static String insertIntoGenerator(String tableName, String[] fields) {
        String gen = "INSERT INTO " + tableName + "("
                + StringUtils.join(fields, ',')
                + ") values ("
                + "?"
                + StringUtils.repeat(",?", fields.length - 1)
                + ")";

        return gen;
    }

    public static String updateGenerator(String tableName, String[] fields, String keyName) {
        String gen = "UPDATE " + tableName + " SET "
                + StringUtils.join(fields, "=? ,") + "=?" + " WHERE " + keyName + "=?";

        return gen;
    }

    public static String updateGenerator(String tableName, String[] fields, String keyName, String keyName1) {
        String gen = "UPDATE " + tableName + " SET "
                + StringUtils.join(fields, " = ? ,") + " = ?" + " WHERE " + keyName + " = ? AND " + keyName1 + " = ?";

        return gen;
    }

    public static void fillStatement(PreparedStatement stmt, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }

    public static int executeInsertInto(Connection conn, String sql, Object[] parameters) throws SQLException {

        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        fillStatement(stmt, parameters);

        int res = stmt.executeUpdate();
        int lastId = 0;
        // call this to get the last inserted key
        ResultSet rs = stmt.getGeneratedKeys();
        if (rs.next()) {
            lastId = rs.getInt(1);
        }

        return lastId;
    }

    public static int executeUpdate(Connection conn, String sql ) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql);  
        int res = stmt.executeUpdate();
        return res;
    }
    
    public static int executeUpdate(Connection conn, String sql, Object[] parameters, Object whereParam) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql);
        fillStatement(stmt, parameters);
        stmt.setObject(parameters.length + 1, whereParam);
        int res = stmt.executeUpdate();
        return res;
    }

    public static int executeUpdate(Connection conn, String sql, Object[] parameters, Object whereParam, Object whereParam1) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql);
        fillStatement(stmt, parameters);
        stmt.setObject(parameters.length + 1, whereParam);
        stmt.setObject(parameters.length + 2, whereParam1);
        int res = stmt.executeUpdate();
        return res;
    }

    private static Facilities instance = new Facilities();

    public static int getIntAttribute(String key) throws Exception {
        int a = Facilities.class.getDeclaredField(key).getInt(instance);
        return a;
    }

    public static String asciiEncoding(String s) {
        if (s == null) {
            return null;
        }

        Charset inCharset = Charset.forName("UTF-8");
        Charset outCharset = Charset.forName("ASCII");

        CharsetEncoder encoder = inCharset.newEncoder();
        CharsetDecoder decoder = outCharset.newDecoder();

        encoder.onUnmappableCharacter(CodingErrorAction.IGNORE);

        ByteBuffer bbuf = null;

        try {
            bbuf = encoder.encode(CharBuffer.wrap(s));
        } catch (CharacterCodingException e) {
            System.out.println(e.getMessage());
        }

        CharBuffer cbuf = null;

        try {
            cbuf = decoder.decode(bbuf);
        } catch (CharacterCodingException e) {
            System.out.println(e.getMessage());
        }

        if (cbuf == null) {
            return null;
        }

        return cbuf.toString();
    }

    public static long dateDiff(String date1, String date2) throws Exception {
        long diff = DateUtils.parseDate(date2, dateParsers).getTime() - DateUtils.parseDate(date1, dateParsers).getTime();
        return (diff / (1000 * 60 * 60 * 24));
    }

    public static void sendEmail(String url, String structureName, String structureEmail, Map<String, String> roomsData) throws MessagingException {
        
        if(1==1) return;
        Properties props = new Properties();

        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", "localhost");
        props.put("mail.from", "noreply-bookingone@mm-one.com");
        Session session = Session.getInstance(props, null);

        BASE64Encoder link64 = new BASE64Encoder();
        String link = null;

        //BACKEND_WEB_PROTOCOL."://".ABS_WEB_URL."/backend/index.php?d=".$dateEncryp

        for (String key : roomsData.keySet()) {
            String[] date = key.split("-");
            String curDate = date[2] + "-" + date[1] + "-" + date[0];
            link = link64.encode(curDate.getBytes());
            break;
        }

        // da customizzare per portale
        String msgTxt = "Gentile " + structureName + ","
                + "<br /><br />"
                + "La modifiche effettuate sul gestionale comportano la re-distribuzione delle disponibilit&agrave; "
                + "nei vari listini per tipologia di camera, "
                + "<a href=\"http://" + url + "/backend/index.php?d=" + link + "\">clicca</a> per accedere.<br /><br />";

        msgTxt = msgTxt + ""               
                + "Questo &egrave; un messaggio generato in automatico La preghiamo di non rispondere a questa email.<br /><br />"
                + "Per qualsiasi necessit&agrave; o chiarimento non esiti di contattarci all'<strong>Help Desk</strong> di MM-ONE tramite email: "
                + "<a href=\"mailto:support.abs@mm-one.com\">support.abs@mm-one.com</a> o al numero di telefono: 0421-65261.<br /><br />"
                + "Cordiali Saluti.";

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom();
            msg.setRecipients(Message.RecipientType.TO, structureEmail);
            msg.setSubject("ALERT ALLOTMENT - BOOKING ONE");
            msg.setSentDate(new java.util.Date());
            msg.setContent(msgTxt, "text/html; charset=UTF-8");
            Transport.send(msg);
        } catch (MessagingException mex) {
            System.out.println("Send failed, exception: " + mex);
        }
    }
    
    
    public static String docToXmlString(Document doc) throws  Exception{ 
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        StringWriter writer1 = new StringWriter();
        Result result = new StreamResult(writer1);
        Source source = new DOMSource(doc);
        transformer.transform(source, result);
        writer1.close();
        String xml1 = writer1.toString();

        return xml1 ;
    }
    public static String mapToXml(List<Map<String, Object>> records){ 
        XStream xstream = new XStream();
        xstream.alias("guests", List.class);
        xstream.alias("guest", Map.class);
        
        xstream.registerConverter(new MapEntryConverter()); 
        String xml = xstream.toXML(records); 
        return xml;
    }
    
    public static Document mapToDocument(List<Map<String, Object>> records,Map<String,String> aliases) throws  Exception{  
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        org.w3c.dom.Document doc = db.newDocument()   ; 
        org.w3c.dom.Element rootElement = doc.createElement("ExtendedData");
	doc.appendChild(rootElement); 
        org.w3c.dom.Node nodePs = rootElement.appendChild( doc.createElement("Ps") ) ;
        org.w3c.dom.Node nodeGuests = nodePs.appendChild( doc.createElement("Guests") ) ;
        
        for (Map<String, Object> record : records) {
            org.w3c.dom.Element nodeRecord = doc.createElement("Guest") ;
            for (String fieldName : record.keySet()) {
                org.w3c.dom.Element nodeField = doc.createElement( aliases.get( fieldName) ) ;
                nodeField.setTextContent((String)record.get(fieldName) );
                nodeRecord.appendChild(nodeField);
            }
            
            nodeGuests.appendChild(nodeRecord);
    
        }
         
        return doc;
    }
    
    private static class MapEntryConverter implements Converter {

        public boolean canConvert(Class clazz) {
            return AbstractMap.class.isAssignableFrom(clazz);
        }

        public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
             
            AbstractMap map = (AbstractMap) value;
            
            for (Object obj : map.entrySet()) {
                Map.Entry entry = (Map.Entry) obj;
                writer.startNode(entry.getKey().toString());
                writer.setValue(entry.getValue().toString());
                writer.endNode();
                //System.out.println("marshall " + entry.getValue().toString());
            }
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }   
    
    public Map getDayByDayPrices(String json,String treatment){
        
        Map<String,String> result = new java.util.Hashtable();
        /*
        JSONObject  r =(JSONObject ) JSONValue.parse( json );
        JSONObject roomPriceDetail = (JSONObject) r.get("roomPriceDetail");
        
        Set days = roomPriceDetail.keySet();
       
        for (Iterator it = days.iterator(); it.hasNext();) {
            String day = (String) it.next();
             
            JSONObject prices = (JSONObject) roomPriceDetail.get(day);
            Set treatments = prices.keySet();
            //Float price = 0f ;
            String price = "0" ;
            try {
                //price = new Float(prices.get(treatment).toString());
                price = new String(prices.get(treatment).toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            result.put(day,price);
        }
        */
        return result;
    }
     
    
            
    
}
