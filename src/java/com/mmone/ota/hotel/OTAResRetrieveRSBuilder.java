package com.mmone.ota.hotel;
 
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.datatype.DatatypeConfigurationException;
import org.opentravel.ota._2003._05.ErrorType;
import org.opentravel.ota._2003._05.ErrorsType;
import org.opentravel.ota._2003._05.HotelReservationType;
import org.opentravel.ota._2003._05.WarningsType;
import org.opentravel.ota._2003._05.WarningType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource; 
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;
import org.opentravel.ota._2003._05.AdditionalDetailType;
import org.opentravel.ota._2003._05.AdditionalDetailsType;
import org.opentravel.ota._2003._05.CommentType;
import org.opentravel.ota._2003._05.CompanyInfoType;
import org.opentravel.ota._2003._05.CompanyNameType;
import org.opentravel.ota._2003._05.CountryNameType;
import org.opentravel.ota._2003._05.CustomerType;
import org.opentravel.ota._2003._05.DateTimeSpanType;
import org.opentravel.ota._2003._05.GuaranteeType;
import org.opentravel.ota._2003._05.GuestCountType;
import org.opentravel.ota._2003._05.GuestCountType.GuestCount;
import org.opentravel.ota._2003._05.HotelReservationIDsType;
import org.opentravel.ota._2003._05.HotelReservationIDsType.HotelReservationID;
import org.opentravel.ota._2003._05.OTAReadRQ;
import org.opentravel.ota._2003._05.OTAResRetrieveRS;
import org.opentravel.ota._2003._05.ParagraphType;
import org.opentravel.ota._2003._05.PaymentCardType;
import org.opentravel.ota._2003._05.PersonNameType;
import org.opentravel.ota._2003._05.ProfilesType;
import org.opentravel.ota._2003._05.RatePlanType;
import org.opentravel.ota._2003._05.RateType;
import org.opentravel.ota._2003._05.ResGlobalInfoType;
import org.opentravel.ota._2003._05.ResGuestsType;
import org.opentravel.ota._2003._05.RoomStayType.RoomRates;
import org.opentravel.ota._2003._05.RoomStayType.RoomTypes;
import org.opentravel.ota._2003._05.RoomStaysType;
import org.opentravel.ota._2003._05.RoomTypeType;
import org.opentravel.ota._2003._05.StateProvType;
import org.opentravel.ota._2003._05.SuccessType;
import org.opentravel.ota._2003._05.TPAExtensionsType;
import org.opentravel.ota._2003._05.TotalType;
import org.opentravel.ota._2003._05.UniqueIDType;
import org.w3c.dom.Document;

public class OTAResRetrieveRSBuilder  extends BaseBuilder{
    public static final String OTA_EAT_BUSINESS = "2";
    public static final String OTA_EAT_HOME = "1";
    public static final String OTA_PRT_CUSTOMER = "1";
    public static final String OTA_PRT_COMPANY = "3";
    public static final String OTA_PRT_TRAVEL_AGENT = "4";
    public static final String OTA_PTT_FAX = "3";
    public static final String OTA_PTT_VOCE = "1";
    public static final String RESERVATION_STATUS_CODE_WAITING_TO_CONFIRM = "0";
    public static final String RESERVATION_STATUS_CODE_CONFIRMED = "1";
    public static final String RESERVATION_STATUS_CODE_CANCEL_BY_STRUCTURE = "2";
    public static final String RESERVATION_STATUS_CODE_CANCEL_BY_GUEST = "3";
    public static final String RESERVATION_STATUS_CODE_CANCEL_NO_SHOW = "4";
    public static final String RESERVATION_STATUS_CODE_CANCEL_WRONG_CC = "5";
    public static final String RESERVATION_STATUS_CODE_CANCEL_BY_ADMIN = "6";
    public static final String RESERVATION_STATUS_CODE_CANCEL_GUEST_REQUEST = "7";
    public static final String RESERVATION_STATUS_CODE_REQUEST_TO_CANCEL_BY_GUEST = "20";
    public static final String RESERVATION_STATUS_CODE_REQUEST_TO_MODIFY_BY_GUEST = "21";
    public static final String RESERVATION_STATUS_CODE_SET_TO_MANUAL = "100";
    public static final String RESERVATION_STATUS_CODE_SET_TO_RESERVATION = "101";
    public static final String RESERVATION_STATUS_MODIFY_BY_MANAGEMENT_SOFTWARE = "102";
    public static final String RESERVATION_STATUS_CONFIRMED_BY_MANAGEMENT_SOFTWARE = "103";
    public static final byte[] KEY_CIPHER = {'M', 'M', 'O', 'N', 'E', '2', '0', '0', '7', '!'};
    public static final String RES_RETRIEVE_ACTION_READ = "read";
    public static final String RES_RETRIEVE_ACTION_EXIST = "exist";
    public static final String S_KEY_CIPHER = new String(KEY_CIPHER);
    public static final Map<String, String> RESERVATION_STATUS_TO_MM_CODE = new Hashtable<String, String>();

    static { 

        RESERVATION_STATUS_TO_MM_CODE.put(RESERVATION_STATUS_CODE_WAITING_TO_CONFIRM, "Hold");
        RESERVATION_STATUS_TO_MM_CODE.put(RESERVATION_STATUS_CODE_CONFIRMED, "Book");
        RESERVATION_STATUS_TO_MM_CODE.put(RESERVATION_STATUS_CODE_CANCEL_BY_STRUCTURE, "Cancel");
        RESERVATION_STATUS_TO_MM_CODE.put(RESERVATION_STATUS_CODE_CANCEL_BY_GUEST, "Cancel");
        RESERVATION_STATUS_TO_MM_CODE.put(RESERVATION_STATUS_CODE_CANCEL_NO_SHOW, "Cancel");
        RESERVATION_STATUS_TO_MM_CODE.put(RESERVATION_STATUS_CODE_CANCEL_WRONG_CC, "Cancel");
        RESERVATION_STATUS_TO_MM_CODE.put(RESERVATION_STATUS_CODE_CANCEL_BY_ADMIN, "Cancel");
        RESERVATION_STATUS_TO_MM_CODE.put(RESERVATION_STATUS_CODE_CANCEL_GUEST_REQUEST, "Cancel");
        RESERVATION_STATUS_TO_MM_CODE.put(RESERVATION_STATUS_CODE_REQUEST_TO_CANCEL_BY_GUEST, "Hold");
        RESERVATION_STATUS_TO_MM_CODE.put(RESERVATION_STATUS_CODE_REQUEST_TO_MODIFY_BY_GUEST, "Hold");
        RESERVATION_STATUS_TO_MM_CODE.put(RESERVATION_STATUS_CODE_SET_TO_MANUAL, "Modify");
        RESERVATION_STATUS_TO_MM_CODE.put(RESERVATION_STATUS_CODE_SET_TO_RESERVATION, "Hold");
        RESERVATION_STATUS_TO_MM_CODE.put(RESERVATION_STATUS_MODIFY_BY_MANAGEMENT_SOFTWARE, "Modify");
        RESERVATION_STATUS_TO_MM_CODE.put(RESERVATION_STATUS_CONFIRMED_BY_MANAGEMENT_SOFTWARE, "Book");
    }
    public static final String DOWNLOAD_TYPE_ALL = "ALL";
    public static final String DOWNLOAD_TYPE_ONLY_BOOKING = "only-booking";
    public static final String DOWNLOAD_TYPE_LIMITED = "download-limited";
    
    
    private DataSource ds;
    private OTAReadRQ request;
    private QueryRunner run;
    private OTAResRetrieveRS res = new OTAResRetrieveRS();
    private String hotelCode = null;
    private String context_id = null;
    private String langID;
    private String user = null;
    private String echoToken;
    private String requestorID;
    private String target = Facilities.TARGET_PRODUCTION;
    private BigDecimal version = new BigDecimal(Facilities.VERSION);
    private Map<String,String> logData = new LinkedHashMap<String, String>();
    private Map<Object, Object> listini = new Hashtable<Object, Object>();
    private Logger logger = Logger.getLogger( "javax.enterprise.system.util" );
    private String uniqueId = null;
    private boolean isDebug = false;
    private String  downloadType = DOWNLOAD_TYPE_ALL;
    
    private String action = RES_RETRIEVE_ACTION_READ;
    private boolean reservationExist= false; 
    private int portalCode = Facilities.DEFAULTS_PORTAL_CODE;
    private boolean hasPortalCode = false;
    private XmlRpcClient client = null;

    public XmlRpcClient getClient() {
        return client;
    }

    public void setClient(XmlRpcClient client) {
        this.client = client;
    }
    
    
    public final void addError(String type, String code, String message) {
        if (res.getErrors() == null) {
            res.setErrors(new ErrorsType());
        }

        ErrorType et = new ErrorType();
        et.setCode(code);
        et.setType(type);
        et.setValue(message);

        res.getErrors().getError().add(et);
    }
    public final void addWarning(String type, String code, String message) {
        if (res.getWarnings() == null) {
            res.setWarnings(new WarningsType());
        }

        WarningType et = new WarningType();
        et.setCode(code);
        et.setType(type);
        et.setValue(message);

        res.getWarnings().getWarning().add(et);
    }
    private void logInfoRequest(Map<String, String> infoRequest) {
        StringBuilder msg = new StringBuilder();
        msg.append("\n****");

        for (String key : infoRequest.keySet()) {
            msg.append("- ").append(key).append(" = ").append(infoRequest.get(key)).append(" ");
        }

        Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.INFO, msg.toString());
    }
    
    public OTAResRetrieveRSBuilder(DataSource ds,OTAReadRQ request, String user, HttpServletRequest httpRequest, XmlRpcClient client) {
        super();
        this.setClient(client);
        
        if (ds == null) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Server. System Error. No connection with the database");
            return;
        }
         
        this.request = request;
        this.langID = request.getPrimaryLangID();
        this.user = user;
        this.ds = ds;
        this.echoToken = request.getEchoToken();
        this.target = request.getTarget();
        this.version = request.getVersion();
        
        String reservationType= request.getReservationType();
        
        if(reservationType!=null){
            if(reservationType.equals("debug_mmone")){
                this.isDebug=true; 
                this.downloadType=DOWNLOAD_TYPE_ALL;
            } else if(reservationType.equals(DOWNLOAD_TYPE_ONLY_BOOKING)){
                this.downloadType=DOWNLOAD_TYPE_ONLY_BOOKING;
            } else if(reservationType.equals(DOWNLOAD_TYPE_ONLY_BOOKING)){
                this.downloadType=DOWNLOAD_TYPE_ONLY_BOOKING;
            }
        }
        UniqueIDType  uniqueIDType = request.getUniqueID();
         
        if(uniqueIDType!=null){ 
            if(uniqueIDType.getType()!=null){
                this.action = uniqueIDType.getType();
            } 
            if(uniqueIDType.getID()!=null){
                this.uniqueId = uniqueIDType.getID();
            } 
        }
        
        if(this.action.equals(RES_RETRIEVE_ACTION_READ)){
        }else if(this.action.equals(RES_RETRIEVE_ACTION_EXIST)){
            if(this.uniqueId==null || this.uniqueId.equals("")){
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_REQUEST_CODE, "UniqueId ID is required.");
            }
        }else{
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_REQUEST_CODE, "UniqueId Type is invalid.");
        }
        
        if (!target.equals(Facilities.TARGET_PRODUCTION) && !target.equals(Facilities.TARGET_TEST)) {
            this.target = Facilities.TARGET_PRODUCTION;
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_REQUEST_CODE, "Target is invalid.");
        }

        if (langID == null || langID.equals("")) {
            addWarning(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_LANGUAGE_CODE_INVALID, "Language is invalid. Default to it.");
            langID = "it";
        }

        if (!version.toString().equals(Facilities.VERSION)) {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_REQUEST_CODE, "Version is invalid.");
        }

        if (echoToken == null || echoToken.equals("")) {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_REQUEST_CODE, "EchoToken is invalid.");
        }

        try {
            res.setTimeStamp(com.mmone.ota.rpc.Facilities.getToday());
        } catch (DatatypeConfigurationException ex) {

        }

        res.setTarget(target);
        res.setPrimaryLangID(langID);
        res.setVersion(version);
        res.setEchoToken(UUID.randomUUID().toString());
     
        logData.put("Class" , this.getClass().getName());
        logData.put("TimeStamp", res.getTimeStamp().toString());
        logData.put("user", user);
        logData.put("RemoteAddr", httpRequest.getRemoteAddr());
        logData.put("EchoToken req", request.getEchoToken());
        logData.put("EchoToken res", res.getEchoToken());
        logData.put("Target", target);
        
        run = new QueryRunner(ds);

        //OTA_ReadRQ/ReadRequests/HotelReadRequest
        List<OTAReadRQ.ReadRequests.HotelReadRequest> lHotelReadRequest = request.getReadRequests().getHotelReadRequest();
        try {
            hotelCode = lHotelReadRequest.get(0).getHotelCode();
        } catch (Exception e) {
        }

        if (hotelCode == null) {
            addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_HOTEL_CODE, "HotelCode null");
            return;
        }

        String sqlChkUser = "SELECT permissions,user FROM ota_users WHERE user=? AND structure_id=? AND deleted=?";
        String sUser = user; 
         
        sqlChkUser = "SELECT permissions,user FROM ota_users WHERE user like ? AND structure_id=? AND deleted=?";
        sUser = "%"+user;
         
        Map permission = null;

        try {
            permission = run.query(sqlChkUser, new MapHandler(), sUser, hotelCode, 0);
        } catch (SQLException e) {
            Logger.getLogger(OTAHotelRoomListBuilder.class.getName()).log(Level.SEVERE, null, e);
        }

        if (permission == null) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Client. Authorization Error: You don't have permission to select the structure " + hotelCode);
        } else {
            Integer p = new Integer(permission.get("permissions").toString());
            String tmpusr = (String)permission.get("user") ;
            String[]aUser=tmpusr.split("#");
            if(aUser.length==1){
                
            }else{
                try {
                    this.portalCode = new Integer(aUser[0]);
                    this.hasPortalCode=true; 
                } catch (Exception exception) { 
                    
                }
            }
             
            //int testEnv = p & Facilities.TEST_ENVIRONMENT;
            int prodEnv = p & Facilities.PRODUCTION_ENVIRONMENT;

            if (request.getTarget().equals("Production") && prodEnv == 0) {
                addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Client. Authorization Error: You don't have permission to select the production environment");
            }
        }

        logData.put("HotelCode", hotelCode);
        res.setTransactionIdentifier(hotelCode);
        
        logInfoRequest(logData);
        
        if (thereIsError()) {
            return;
        }
        
        try {
            //OTA_ReadRQ/ReadRequests/HotelReadRequest/UserID
            context_id = request.getReadRequests().getHotelReadRequest().get(0).getUserID().getIDContext();
            System.out.println(" context_id = " + context_id);
        } catch (Exception e) {
             Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.SEVERE, "******* Error getting  context ");
             Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.SEVERE, null, e);

        }

        if (context_id == null ) {
            addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_REQUIRED_FIELD_MISSING, "@ID_CONTEXT null");
            return;
        } else {
            if (context_id.isEmpty() || context_id.equals("0")) {
                addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_REQUIRED_FIELD_MISSING, "@ID_CONTEXT is invalid");
                return;
            }
        }

        try {
            if (isRequestComplete()) { 
                if(this.action.equals( RES_RETRIEVE_ACTION_READ ))
                    loadListiniData();
                else if(this.action.equals( RES_RETRIEVE_ACTION_EXIST )){ 
                    fnReservationExist(this.uniqueId,new Integer (hotelCode) );
                    return;
                }
            }
        } catch (Exception ex) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Server. System Error. Error loading listini " + ex.getMessage());
            Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        try {
            if (isRequestComplete()) {
                loadReservationData();
            }
        } catch (Exception ex) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Server. System Error. Error loading data " + ex.getMessage());
            Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

    }
    private boolean useRpc = true;
    
    private boolean isRequestComplete() {
        return (hotelCode != null && context_id != null);
    }
    private List<Map<String, Object>> loadReservationData() throws Exception {
        Integer iHotelCode = new Integer(hotelCode);
        List<Map<String, Object>> reservation = null;
        boolean checkChannels = true;
        if(this.downloadType.equals(DOWNLOAD_TYPE_ONLY_BOOKING)){
            if(useRpc)
                reservation=ReservationDownloadServices.retrieveReservationsOnlyBookingRPC(client, hotelCode, context_id, checkChannels);
            else     
                reservation=ReservationDownloadServices.retrieveReservationsOnlyBooking(ds, hotelCode, context_id,getPortalCode());
        } else { 
            if(useRpc)
                reservation=ReservationDownloadServices.retrieveReservationsAllRPC(client, hotelCode, context_id, checkChannels);
            else    
                reservation=ReservationDownloadServices.retrieveReservations(ds, hotelCode, context_id,getPortalCode());
        }
        
        return reservation;
    }
    
    private boolean fnReservationExist(String reservationNumber,Integer ihotelCode) throws Exception {
        String sql = "select count(*) as reservation_count from reservation where structure_id=? AND reservation_number = ?";
        Object countRes = run.query(sql, new ScalarHandler("reservation_count"), ihotelCode , reservationNumber);
        
        this.reservationExist=!countRes.toString().equals("0");
        return this.reservationExist;
    }
    private void loadListiniData() throws Exception {
        /**
        List<Map<String, Object>> lListino = run.query("SELECT * FROM list ORDER BY list_id", new MapListHandler());
        for (Map<String, Object> map : lListino) {
            listini.put(map.get("list_id"), map.get("list_name").toString());
        }
        **/ 
        
        listini.put(1, "Normal rate");
        listini.put(2, "Special rate");
        
    }

    private List<Map<String, Object>> loadReservationOtherData(Map<String, Object> reservation) {
        Map res = (Map)reservation.get("reservation_details");
        Collection values = res.values();
        ArrayList ret = new ArrayList(values);
        return ret;
    }
    //Array con tutte le camere
    private List<Map<String, Object>> loadReservationOtherData(Integer reservationId) throws Exception {
        List<Map<String, Object>> reservationDetail = null;
        if (reservationId == null) {
            reservationDetail = new ArrayList<Map<String, Object>>();
            addWarning(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_NO_RESERVATIONS_FOUND_FOR_SEARCH_CRITERIA, "No reservations found");
        } else {
            String reservationDetailQry = " SELECT  ifnull(  multirate.multirate_code,'rate' ) as mr_code , reservation_detail.* "
                                         +" FROM reservation_detail "
                                         +" left join multirate on multirate.multirate_id=reservation_detail.reservation_detail_list_id "   
                                         +" WHERE reservation_id=? AND reservation_detail_type='R' "    ;
            
            reservationDetail = run.query( reservationDetailQry , new MapListHandler(), reservationId);
            //reservationDetail = run.query("SELECT * FROM reservation_detail WHERE reservation_id=? AND reservation_detail_type='R' ", new MapListHandler(), reservationId);
        }

        return reservationDetail;
    }
    
    
    private List<Map<String, Object>> loadReservationEcommerce(Integer reservationId) throws Exception {
        List<Map<String, Object>> reservationDetail = null;
        if (reservationId == null) {
            reservationDetail = new ArrayList<Map<String, Object>>();
            addWarning(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_NO_RESERVATIONS_FOUND_FOR_SEARCH_CRITERIA, "No reservations found");
        } else {
            reservationDetail = run.query("SELECT * FROM reservation_detail WHERE reservation_id=? AND reservation_detail_type='E' ", new MapListHandler(), reservationId);
        }

        return reservationDetail;
    }
    
    private List<Map<String, Object>> loadReservationGifts(Map<String, Object> reservation) {
        return (List<Map<String, Object>>) reservation.get("reservation_details_gifts");
    }
    
    private List<Map<String, Object>> loadReservationGifts(Integer reservationId) throws Exception {
        List<Map<String, Object>> reservationDetail = null;
        if (reservationId == null) {
            reservationDetail = new ArrayList<Map<String, Object>>(); 
        } else {
            reservationDetail = run.query("SELECT reservation_detail_name FROM reservation_detail WHERE reservation_id=? AND reservation_detail_code = '-GIFT-' ", new MapListHandler(), reservationId);
        }

        return reservationDetail;
    }
    
    private List<Map<String, Object>> loadReservationAcc(Map<String, Object> reservation) {
        return (List<Map<String, Object>>) reservation.get("reservation_acc");
    }
    
    private List<Map<String, Object>> loadReservationAcc(Integer reservationId ,Integer reservationDetId ) throws Exception {
        List<Map<String, Object>> reservationDetail = null;
        if (reservationId == null) {
            reservationDetail = new ArrayList<Map<String, Object>>(); 
        } else {
            reservationDetail = run.query("SELECT * FROM reservation_detail WHERE reservation_id=? AND reservation_detail_type in ('A','E') AND reservation_detail_id>?", new MapListHandler(), reservationId,reservationDetId);
        }

        return reservationDetail;
    }
    
    private List<Map<String, Object>> loadReservationRoomData(Map<String, Object> reservation) {
        List<Map<String, Object>> res = (List<Map<String, Object>>)reservation.get("reservation_details_totals");
        return res;
    }
    
    //Prezzo totale della camera
    private List<Map<String, Object>> loadReservationRoomData(Integer reservationId) throws Exception {
        List<Map<String, Object>> reservationRoomData = null;

        if (reservationId == null) {
            reservationRoomData = new ArrayList<Map<String, Object>>();
            addWarning(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_NO_RESERVATIONS_FOUND_FOR_SEARCH_CRITERIA, "No reservations found");
        } else {
            reservationRoomData = run.query("SELECT * FROM reservation_detail WHERE reservation_id=? AND reservation_detail_type='T' ", new MapListHandler(), reservationId);
        }

        return reservationRoomData;
    }
    //Prezzo totale della camera
    private List<Map<String, Object>> loadReservationPs(Integer reservationDataId) throws Exception {
        List<Map<String, Object>> reservationPs = null;

        if (reservationDataId == null) {
            reservationPs = new ArrayList<Map<String, Object>>();
            addWarning(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_NO_RESERVATIONS_FOUND_FOR_SEARCH_CRITERIA, "No reservations found");
        } else {
            reservationPs = run.query(
                "select  "
                +" reservation_ps_role,"
		+" CAST(reservation_ps_arrive_date AS CHAR) as   reservation_ps_arrive_date,"  
		+" reservation_ps_surname,"
		+" reservation_ps_name,"
		+" reservation_ps_gender,"
		+" CAST(reservation_ps_birthdate AS CHAR) as   reservation_ps_birthdate,"
		+" reservation_ps_birthplace,"
		+" reservation_ps_birthcounty,"
		+" reservation_ps_nationality,"
		+" reservation_ps_cittadinanza,"
		+" reservation_ps_comune_cod,"
		+" reservation_ps_county,"
		+" reservation_ps_state,"
		+" reservation_ps_address,"
		+" reservation_ps_document_type,"
		+" reservation_ps_document_number,"
		+" reservation_ps_document_from"   
                +" from reservation_ps"
                +" left join reservation_ges on "
                +" reservation_ps.reservation_ps_id = reservation_ges.reservation_ges_id "
                +" where reservation_ges.reservation_detail_id = ? "
                , new MapListHandler(), reservationDataId
            );
        }

        return reservationPs;
    }
    private static List<Map<String, Object>> loadReservationPsSt(Integer reservationDataId,QueryRunner run) throws Exception {
        List<Map<String, Object>> reservationPs = null;
        
        if (reservationDataId == null) {
            reservationPs = new ArrayList<Map<String, Object>>();
         
        } else {
            reservationPs = run.query(
                "select  "
                +" reservation_ps_role," 
                //+" CAST(reservation_ps_arrive_date AS CHAR) as   reservation_ps_arrive_date,"    
		+" reservation_ps_surname,"
		+" reservation_ps_name,"
		//+" reservation_ps_gender,"
		+" CAST(reservation_ps_birthdate AS CHAR) as   reservation_ps_birthdate,"
		+" reservation_ps_birthplace,"
		//+" reservation_ps_birthcounty,"
		+" reservation_ps_nationality,"
		//+" reservation_ps_cittadinanza,"
		+" reservation_ps_comune_cod,"
		//+" reservation_ps_county,"
		//+" reservation_ps_state,"
		+" reservation_ps_address,"
		+" reservation_ps_document_type,"
		+" reservation_ps_document_number,"
		+" reservation_ps_document_from"      
                +" from reservation_ps"
                +" left join reservation_ges on "
                +" reservation_ps.reservation_ps_id = reservation_ges.reservation_ges_id "
                //+" where reservation_ges.reservation_detail_id = ? "
                , new MapListHandler()
                //, reservationDataId
            );
        }

        return reservationPs;
    }
    public static Map psFieldsTranslation = new Hashtable();
    static{
        psFieldsTranslation.put("reservation_ps_role", "TipoOspite");
        psFieldsTranslation.put("reservation_ps_arrive_date", "DataArrivo");
        psFieldsTranslation.put("reservation_ps_surname", "Cognome");
        psFieldsTranslation.put("reservation_ps_name", "Nome");
        psFieldsTranslation.put("reservation_ps_gender", "Sesso");
        psFieldsTranslation.put("reservation_ps_birthdate", "DataNascita");
        psFieldsTranslation.put("reservation_ps_birthplace", "LuogoNascita");
        psFieldsTranslation.put("reservation_ps_birthcounty", "ProvinciaNascita");
        psFieldsTranslation.put("reservation_ps_nationality", "Nazionalita");
        psFieldsTranslation.put("reservation_ps_cittadinanza", "Cittadinanza");
        psFieldsTranslation.put("reservation_ps_comune_cod", "ComuneResidenza");
        psFieldsTranslation.put("reservation_ps_county", "ProvinciaComuneResidenza");
        psFieldsTranslation.put("reservation_ps_state", "StatoResidenza");
        psFieldsTranslation.put("reservation_ps_address", "Indirizzo");
        psFieldsTranslation.put("reservation_ps_document_type", "TipoDocumento");
        psFieldsTranslation.put("reservation_ps_document_number", "NumeroDocumento");
        psFieldsTranslation.put("reservation_ps_document_from", "LuogoDocumento");
    }
    public OTAResRetrieveRS build(String requestorID) {
        this.requestorID = requestorID;
        if (!isRequestComplete()) {
            return res;
        }
         
                
        if(this.action.equals( RES_RETRIEVE_ACTION_READ )){
        }else if(this.action.equals( RES_RETRIEVE_ACTION_EXIST )){ 
            if(this.reservationExist){
                String sql =  "SELECT reservation_status from reservation where reservation_number=?";
                try {
                    Object reservationStatus = run.query(sql, new ScalarHandler("reservation_status"), this.uniqueId);
                    
                    String resStatus = resStatus = RESERVATION_STATUS_TO_MM_CODE.get(reservationStatus.toString());
                    
                    if(resStatus==null)
                        resStatus = RESERVATION_STATUS_TO_MM_CODE.get(RESERVATION_STATUS_CODE_CONFIRMED);
                    res.setReservationsList(new OTAResRetrieveRS.ReservationsList());
                    List<HotelReservationType> lHotelReservations = res.getReservationsList().getHotelReservation();
                    HotelReservationType hotelReservationType = new HotelReservationType();
                     
                    hotelReservationType.setResStatus(resStatus);
                    
                    UniqueIDType uniqueID = new UniqueIDType();
                    uniqueID.setType("0");
                    uniqueID.setID(this.uniqueId);
                    uniqueID.setIDContext(context_id);
                    hotelReservationType.getUniqueID().add(uniqueID);

                    lHotelReservations.add(hotelReservationType);
                
                } catch (Exception ex) {
                    Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.SEVERE, null, ex);
                }

                res.setSuccess(new SuccessType());
                
            }else{
                addError( Facilities.EWT_UNKNOWN, Facilities.ERR_NO_RESERVATIONS_FOUND_FOR_SEARCH_CRITERIA, "Reservation " + this.uniqueId + " not found");
            }
                
            return res;
        }
        
            
        try {
            res.setReservationsList(new OTAResRetrieveRS.ReservationsList());
            List<Map<String, Object>> reservations = loadReservationData();
            List<HotelReservationType> lHotelReservations = res.getReservationsList().getHotelReservation();

            int reservationCount = 0;
            for (Map<String, Object> reservation : reservations) {
                 
                reservationCount++;
                if(reservationCount>ReservationDownloadServices.DOWNLOAD_LIMIT)
                    break;
                
                Integer reservationId = new Integer(reservation.get("reservation_id").toString());
                Integer currentPortalId = new Integer(reservation.get("portal_id").toString());
                if(currentPortalId==null) currentPortalId=1;
                
                if(hasPortalCode){
                    if(currentPortalId!=this.portalCode) {
                        Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(
                                    Level.WARNING
                                    ,   " Skipping reservation id="
                                        + reservationId.toString()
                                        + " portal=" + this.portalCode + "(Reservation portal="+currentPortalId+")"
                                        + " Requestor=" + this.requestorID
                                        + " Structure=" + this.hotelCode
                        );
                        continue;
                    }
                }
                   
                try{
                    List<Map<String, Object>> reservationDetails = loadReservationOtherData(reservation);
                    List<Map<String, Object>> reservationRoomData = loadReservationRoomData(reservation);
                    buildSingle(reservation, reservationDetails, reservationRoomData, lHotelReservations);
                }catch(Exception e){
                    e.printStackTrace();
                    Logger.getLogger( OTAResRetrieveRSBuilder.class.getName() )
                            .log(
                                Level.SEVERE
                                , "Error adding reservation"
                                  + "\n - Structure : " + hotelCode
                                  + "\n - Reservation : " + reservationId
                                  + "\n - Requestor : " + requestorID
                                  + "\n - Portal : " + portalCode
                            );
                    
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (!thereIsError()) {
            try {
                //Connection conn = ds.getConnection();
                //conn.setAutoCommit(false);
                ReservationDownloadServices.insertDownloadRequestRecordCommitRequired(ds, res.getEchoToken(), hotelCode, context_id);
                //conn.commit();
                //DbUtils.closeQuietly(conn);∑

            } catch (Exception ex) {
                addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Problems connecting to db");
                Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                res.setSuccess(new SuccessType());
            }

        }
        return res;
    }
    
    private Date objectToDate (Object dtObject) {
        if(dtObject==null) {
            return null;
        } else {
            try {
                return DateUtils.parseDate(dtObject.toString(), null);
            } catch (ParseException ex) {
                return null;
            }
        }    
    }
    
    public OTAResRetrieveRS buildSingle(Map reservation, List<Map<String, Object>> reservationDetail, List<Map<String, Object>> reservationRoomData, List<HotelReservationType> lHotelReservations) {
        //-----------------
        //-----------------   prenotazione
        //-----------------
        HotelReservationType hotelReservationType = new HotelReservationType();
        hotelReservationType.setCreatorID(hotelCode);
        try {
            DatatypeFactory df = DatatypeFactory.newInstance(); 
            GregorianCalendar gc =  new GregorianCalendar(); 
            
            try{
                XMLGregorianCalendar reservationOpenedDate = df.newXMLGregorianCalendar(gc);
                gc.setTimeInMillis(((Date)reservation.get("reservation_opened_date")).getTime());
                hotelReservationType.setCreateDateTime(reservationOpenedDate);
            } catch (Exception ex1) { }
            
            try{
                XMLGregorianCalendar reservationModDate = df.newXMLGregorianCalendar(gc);
                gc.setTimeInMillis(((Date)reservation.get("reservation_status_date")).getTime());
                
                hotelReservationType.setLastModifyDateTime(reservationModDate);
                
                /*
                Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.INFO, "----------  LastReservation date ----" );
                Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.INFO, 
                        reservation.get("reservation_status_date").toString());
                Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.INFO, 
                        ((Date)reservation.get("reservation_status_date")).toString()    );
                */
                
            } catch (Exception ex1) { 
                Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.SEVERE,"----------  LastReservation date ----",ex1  );
            }
            
        } catch (DatatypeConfigurationException ex) { }

        String resStatus = RESERVATION_STATUS_TO_MM_CODE.get(RESERVATION_STATUS_CODE_WAITING_TO_CONFIRM);

        try {
            resStatus = RESERVATION_STATUS_TO_MM_CODE.get(reservation.get("reservation_status").toString());
        } catch (Exception e) {
        }

        hotelReservationType.setResStatus(resStatus);
 
        if (reservation.get("reservation_id") != null) {
            UniqueIDType uniqueID = new UniqueIDType();
            uniqueID.setType(reservation.get("reservation_type").toString());
            
            uniqueID.setID(reservation.get("new_reservation_id").toString() );
            uniqueID.setIDContext(context_id);
                             
            hotelReservationType.getUniqueID().add(uniqueID);
        } else {
            addWarning(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_NO_RESERVATIONS_FOUND_FOR_SEARCH_CRITERIA, "No reservations found");
        }

        hotelReservationType.setRoomStays(new RoomStaysType());
        hotelReservationType.setResGuests(new ResGuestsType());

        //String ccardNum = null;
        //CodeEncodeString crypt = new CodeEncodeString(new String[]{S_KEY_CIPHER});

        //try {
        //bccardNum = new sun.misc.BASE64Decoder().decodeBuffer(reservation.get("reservation_cc_number").toString());
        //    ccardNum = crypt.decrypt (  reservation.get("reservation_cc_number").toString()  ) ;
        //} catch (Exception ex) {
        //    addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR,"CC error");
        //}

        PaymentCardType paymentCardType = new PaymentCardType();
        //paymentCardType.setCardCode(   (String)reservation.get(  "reservation_cc_sec_code"   )  );
        //paymentCardType.setCardNumber( ccardNum   );
        paymentCardType.setCardType((String) reservation.get("reservation_cc_type"));
        paymentCardType.setExpireDate((String) reservation.get("reservation_cc_exp_date"));
        paymentCardType.setSeriesCode((String) reservation.get("reservation_cc_sec_code"));

        paymentCardType.setCardHolderName((String) reservation.get("reservation_cc_holder"));

        String rid = reservation.get("reservation_id").toString();
        String rnum = reservation.get("reservation_number").toString();
        String rNewNum = reservation.get("new_reservation_number").toString();
        
        String tmp = "1";
        BigInteger numberOfUnits = new BigInteger("1") ;   
        boolean ecommerceAdded=false;
        for (Map<String, Object> resDetail : reservationDetail) {
            //-----------------   roomstay
            
            RoomStaysType.RoomStay roomStay = new RoomStaysType.RoomStay();
            //roomStay.setSourceOfBusiness("Internet");
            roomStay.setRoomTypes(new RoomTypes());
             
            try {
                RoomTypeType roomTypeType = new RoomTypeType();
                roomTypeType.setNumberOfUnits(numberOfUnits);
                tmp = resDetail.get("reservation_detail_room_id") == null ? "0" : resDetail.get("reservation_detail_room_id").toString();

                if (tmp.equals("0")) {
                    roomTypeType.setRoomTypeCode("0"); // manuale
                } else {
                    
                    if(useRpc){
                        roomTypeType.setRoomTypeCode((String)resDetail.get("reservation_detail_code"));
                    } else {
                        Map mCamera = run.query(
                            "SELECT room_code FROM room WHERE room_id= ? AND structure_id = ?",
                            new MapHandler(),
                            tmp,
                            hotelCode);

                        roomTypeType.setRoomTypeCode(mCamera.get("room_code").toString());
                    }
                    

                }
                
                Integer tmpReservationDetailId = new Integer(resDetail.get("reservation_detail_id").toString());
                int tmpiReservationDetailId = tmpReservationDetailId;
                
                Integer tmpReservationId = new Integer(reservation.get("reservation_id").toString());
                int tmpiReservationId = tmpReservationId;
                AdditionalDetailsType addDets = new AdditionalDetailsType(); 
                 
                 
                List<Map<String, Object>> resGifts = loadReservationGifts(reservation);
                for (Map<String, Object> gift : resGifts) {
                    try { 
                        roomStay.setDiscountCode((String) gift.get("reservation_detail_name"));
                    } catch (Exception e) {
                    }
                }
                
                List<Map<String, Object>> resAccs = loadReservationAcc(tmpReservationId,tmpReservationDetailId);
                for (Map<String, Object> accs : resAccs) {
                    try {
                        Integer tmpAccDetailId = new Integer(accs.get("reservation_detail_id").toString());
                        int tmpiAccDetailId = tmpAccDetailId;
                        String detailType = (String) accs.get("reservation_detail_type");
                        
                        if(detailType.equals("E")) {
                            if(ecommerceAdded  ) continue; 
                        }
                        if(detailType.equals("A") && tmpiAccDetailId>tmpiReservationDetailId+3) 
                            continue;
                        AdditionalDetailType addDet = new AdditionalDetailType();
                        addDet.setCode((String) accs.get("reservation_detail_code"));
                        addDet.setType(detailType);
                          
                        addDet.setAmount(  new BigDecimal(accs.get("reservation_detail_price").toString())         );
                        addDets.getAdditionalDetail().add(addDet);
                        if(detailType.equals("E")) { 
                            ecommerceAdded = true;
                        } 
                    } catch (Exception e) { }
                }
                roomTypeType.setAdditionalDetails(addDets); 
                roomStay.getRoomTypes().getRoomType().add(roomTypeType);
            } catch (Exception e) {
                addWarning(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_ROOM_OR_UNIT_TYPE_INVALID, "Room type not found reservation id: " + rid);
            }

            // prezzo totale della camera compreso servizi e riduzioni/supplementi
            TotalType totalType = new TotalType();

            try {
                totalType.setCurrencyCode("EUR");
                roomStay.setTotal(totalType);
            } catch (Exception e) {
                addWarning(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_PRICE_CANNOT_BE_VALIDATED, "Price invalid");
            }

            //-----------------   roomrates
            roomStay.setRoomRates(new RoomRates());
            roomStay.setRatePlans(new org.opentravel.ota._2003._05.RoomStayType.RatePlans());

            RoomRates.RoomRate roomRate = new RoomRates.RoomRate();
            roomStay.getRoomRates().getRoomRate().add(roomRate);
              
            int rateId = new Integer(resDetail.get("reservation_detail_list_id").toString());
            String mrCode="";
            if(resDetail.get("mr_code")!=null)
                  mrCode=resDetail.get("mr_code").toString() ;
            
            if (rateId > 2) {
                roomRate.setRatePlanCode(Facilities.sRATE_MAP.get(3));
                roomRate.setRatePlanCategory(mrCode);
            } else {
                roomRate.setRatePlanCode(Facilities.sRATE_MAP.get(rateId));
                roomRate.setRatePlanCategory(Facilities.sRATE_CODES_MAP.get(rateId));
            }
            
            tmp = resDetail.get("reservation_detail_room_id") == null ? "0" : resDetail.get("reservation_detail_room_id").toString();  //@@ not found

            Map mCamera = null;
            try {
                
                if(useRpc){
                        roomRate.setRoomTypeCode((String)resDetail.get("reservation_detail_code"));
                } else {
                     mCamera = run.query(
                        "SELECT room_code FROM room WHERE room_id= ? AND structure_id = ?",
                        new MapHandler(),
                        tmp,
                        hotelCode);

                    roomRate.setRoomTypeCode(mCamera.get("room_code").toString());
                }
            } catch (Exception e) {
                roomRate.setRoomTypeCode("0");
            }

            try {
                //roomRate.setNumberOfUnits(new BigInteger(reservation.get("reservation_numrooms") == null ? "0" : reservation.get("reservation_numrooms").toString()));
                roomRate.setNumberOfUnits(new BigInteger("1"));
            } catch (Exception e) {
                addWarning(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_NUMBER_OF_ROOMS_UNITS_REQUIRED, "Reservation Num Rooms not found");
            }

            String startDate = DateFormatUtils.ISO_DATE_FORMAT.format((Date) reservation.get("reservation_checkin_date"));
            String endDate = DateFormatUtils.ISO_DATE_FORMAT.format((Date) reservation.get("reservation_checkout_date"));

            DateTimeSpanType dateTimeSpanType = new DateTimeSpanType();
            dateTimeSpanType.setStart(startDate);
            dateTimeSpanType.setEnd(endDate);
            roomStay.setTimeSpan(dateTimeSpanType);

            //-------------------  rate
            roomRate.setRates(new RateType());
            RateType.Rate rate = new RateType.Rate();

            // prezzo della camera
            TotalType totalTypeRate = new TotalType();
            totalTypeRate.setAmountAfterTax(new BigDecimal(resDetail.get("reservation_detail_price").toString()));
            totalTypeRate.setCurrencyCode("EUR");
            rate.setBase(totalTypeRate);

            roomRate.getRates().getRate().add(rate);

            RatePlanType ratePlan = new RatePlanType();
            rateId = new Integer(resDetail.get("reservation_detail_list_id").toString());
            
            String sP = null;
            switch (rateId) {
                case 0:
                    sP = "Multirate";
                    break;
                case 1:
                    sP = "Normal rate";
                    break;
                case 2:
                    sP = "Special rate";
                    break;
                default:
                    try {
                        Map mRate = run.query(
                                "SELECT multirate_name FROM multirate_specification WHERE multirate_id = ?",
                                new MapHandler(),
                                rateId);

                        sP = mRate.get("multirate_name").toString();
                    } catch (Exception e) {
                        sP = "Multirate";
                    }
                    break;
            }
            ParagraphType p = new ParagraphType();
            p.setName("Text");
            JAXBElement eP = null;
                   
            // Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.INFO,"rateId="+ rateId);
            
            if (rateId > 2) {
                ratePlan.setRatePlanType(Integer.toString(Facilities.OTA_RPT_3)); // Tipo di listino
                ratePlan.setRatePlanCode(Facilities.sRATE_CODES_MAP.get(3));
                ratePlan.setRatePlanName(mrCode);
                eP = new JAXBElement(new QName("http://www.opentravel.org/OTA/2003/05", "Text"), String.class, Facilities.sRATE_MAP.get(3));  
            } else {
                ratePlan.setRatePlanType(Integer.toString(Facilities.RATE_MAP.get(rateId))); // Tipo di listino
                ratePlan.setRatePlanCode(Facilities.sRATE_CODES_MAP.get(rateId));
                ratePlan.setRatePlanName(Facilities.sRATE_CODES_MAP.get(rateId));
                eP = new JAXBElement(new QName("http://www.opentravel.org/OTA/2003/05", "Text"), String.class, Facilities.sRATE_MAP.get(rateId));
            }
            p.getTextOrImageOrURL().add(eP);
            ratePlan.setRatePlanDescription(p); 
            ratePlan.setMealsIncluded(new RatePlanType.MealsIncluded());
            
            Integer iTreatmentId = 1;
            String treatmentId = null;
            try {
                //Map treatment = run.query("SELECT reservation_detail_room_board FROM reservation_detail WHERE reservation_detail_id = ?", new MapHandler(), resDetail.get("reservation_detail_id").toString());
              
                
                Map sTreatment = run.query(
                    "SELECT treatment_id FROM treatment WHERE treatment_code = ?", 
                    new MapHandler(), 
                    (String)resDetail.get( "reservation_detail_room_board" )
                );
                 
                if(resDetail.get("treatment_id")!=null)
                    iTreatmentId = new Integer(resDetail.get("treatment_id").toString());
                
                treatmentId = "" + Facilities.MM_TO_MPT[iTreatmentId.intValue()];
                ratePlan.getMealsIncluded().getMealPlanCodes().add(treatmentId);
            } catch (Exception ex) {
                
                addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_BOARD_BASIS_OR_MEAL_PLAN_INVALID, "Error searching Meals");
                Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
            roomStay.getRatePlans().getRatePlan().add(ratePlan);

            roomStay.setGuestCounts(new GuestCountType());
            roomStay.getGuestCounts().setIsPerRoom(Boolean.FALSE);
            
            // System.out.println( "Loading guests ...............");
            
            List<Map<String, Object>> guests = null;
            
            if(this.getJndiVersion()>=JNDIVER_100_AFTER_SINGLE_GUEST){
                try {
                    Map singleGuest = new Hashtable(1); 
                    guests = new ArrayList<Map<String, Object>>();
                    singleGuest.put("reservation_detail_room_guest", resDetail.get("reservation_detail_room_guest"));
                    guests.add(singleGuest);
                } catch (Exception e) {
                    System.out.println("Error");
                }
            }else{ 
                try {
                    if (mCamera != null) // prenotazione manuale
                    { 
                        guests = run.query("SELECT reservation_detail_room_guest FROM reservation_detail WHERE reservation_id = ? AND reservation_detail_code = ? AND reservation_detail_type = 'R'", new MapListHandler(), rid, mCamera.get("room_code").toString());
                        //System.out.println( "Prenotazione = SELECT reservation_detail_room_guest FROM reservation_detail WHERE reservation_id = ? AND reservation_detail_code = ? AND reservation_detail_type = 'R'  " );
                        //System.out.println( " reservation_id =  " + rid );
                        //System.out.println( " room_code "  +  mCamera.get("room_code").toString() );
                    } else {
                        //System.out.println( "Prenotazione manuale");
                        guests = run.query("SELECT reservation_detail_room_guest FROM reservation_detail WHERE reservation_id = ? AND reservation_detail_code = ? AND reservation_detail_type = 'R'", new MapListHandler(), rid, "---");
                    }
                } catch (Exception e) {
                    addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Soap Server. System Error. Please contact the administrator");
                }
            }
            
            
            
            if(guests==null){
                // System.out.println( "Guests = null");
            }else{
                // System.out.println( "Guests count = " + guests.size() );
            }
            // calcola gli ospiti
            
            for (Map<String, Object> record : guests) {
                // System.out.println( " Map<String, Object> record : guests    ");
                String fieldGuest = record.get("reservation_detail_room_guest").toString();
                String[] temp = null;

                // scompone la stringa dei
                temp = fieldGuest.split("\\|");

                if (temp.length == 1) { // stringa vuota
                    GuestCount guestCount = new GuestCount();
                    try {
                        System.out.println( " adding reservation_numpersons ");
                        guestCount.setCount(   new Integer(reservation.get("reservation_numpersons").toString()));// vendita a camera
                    } catch (Exception e) {
                         Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.SEVERE, " guestCount.setCount ", e);
                         
                         try {
                            MapUtils.debugPrint(System.out, "record ", record);
                        } catch (Exception e1) {
                        }
                         
                    }
                    roomStay.getGuestCounts().getGuestCount().add(guestCount);
                    continue;
                }

                for (int i = 0; i < temp.length; i++) {
                    // System.out.println( " for (int i = 0; i < temp.length; i++) ");
                    GuestCount guestCount = new GuestCount();
                    String[] field = null;
                    field = temp[i].split(":");
                    if (field[0].equals("A")) { // adulti
                        guestCount.setAgeQualifyingCode(Facilities.ADULT_QUALYFING_CODE);
                        try {
                            guestCount.setCount(new Integer(field[1]));
                        } catch (Exception e) {
                            Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.SEVERE, " guestCount.setCount(new Integer(field[1])) ", e);
                        }
                    } else if (field[0].equals("C")) {
                        if (!field[1].equals("-")) {
                            guestCount.setAgeQualifyingCode(Facilities.CHILD_QUALYFING_CODE);
                            guestCount.setCount(1);
                            try {
                                guestCount.setAge(new Integer(field[1]));
                            } catch (Exception e) {
                            }
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                    // System.out.println( " add(guestCount) "   );    
                    roomStay.getGuestCounts().getGuestCount().add(guestCount);
                }
            }

            String xml = "";
            try {
                List<Map<String, Object>> reservationPs = loadReservationPs((Integer) resDetail.get("reservation_detail_id") );
                 
                TPAExtensionsType tpaExtensions = new TPAExtensionsType();
                tpaExtensions.getAny().add(  Facilities.mapToDocument(reservationPs ,  psFieldsTranslation ).getDocumentElement()    );
                roomStay.setTPAExtensions(tpaExtensions);
            } catch (Exception ex) {
                Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
            hotelReservationType.getRoomStays().getRoomStay().add(roomStay);
            //-----------------   roomstay

            // un solo elemento per prenotazione
            if (hotelReservationType.getResGuests().getResGuest().isEmpty()) {
                ResGuestsType.ResGuest resGuest = new ResGuestsType.ResGuest();
                resGuest.setResGuestRPH(rid);
                resGuest.setPrimaryIndicator(Boolean.TRUE);
                resGuest.setProfiles(new ProfilesType());

                ProfilesType.ProfileInfo profileInfo = new ProfilesType.ProfileInfo();
                org.opentravel.ota._2003._05.ProfileType profileType = new org.opentravel.ota._2003._05.ProfileType();
                profileType.setProfileType(OTA_PRT_CUSTOMER);

                CustomerType customerType = new CustomerType();

                PersonNameType personNameType = new PersonNameType();
                personNameType.getGivenName().add((String) reservation.get("reservation_name"));
                personNameType.setSurname((String) reservation.get("reservation_surname"));
                customerType.getPersonName().add(personNameType);

                CustomerType.Telephone telephone = null;

                telephone = new CustomerType.Telephone();
                telephone.setFormattedInd(Boolean.TRUE);
                telephone.setDefaultInd(Boolean.TRUE);
                telephone.setPhoneNumber((String) reservation.get("reservation_phone"));
                telephone.setPhoneTechType("1"); //Codice OTA PTT. 1 = Voce, 3 = Fax.
                customerType.getTelephone().add(telephone);
                
                
                try {
                    if (reservation.get("reservation_guest_language") != null) {
                        customerType.setLanguage((String) reservation.get("reservation_guest_language"));
                    } else {
                        customerType.setLanguage(langID);
                    }
                } catch (Exception e) {
                     Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.SEVERE, "Language error : customerType.setLanguage = " + e.getMessage());
                }
                
                if (reservation.get("reservation_fax") != null) {
                    telephone = new CustomerType.Telephone();
                    telephone.setFormattedInd(Boolean.TRUE);
                    telephone.setDefaultInd(Boolean.FALSE);
                    telephone.setPhoneNumber((String) reservation.get("reservation_fax"));
                    telephone.setPhoneTechType("3"); //Codice OTA PTT. 1 = Voce, 3 = Fax.
                    customerType.getTelephone().add(telephone);
                }//if

                if (reservation.get("reservation_mobile") != null) {
                    telephone = new CustomerType.Telephone();
                    telephone.setFormattedInd(Boolean.TRUE);
                    telephone.setDefaultInd(Boolean.FALSE);
                    telephone.setPhoneNumber((String) reservation.get("reservation_mobile"));
                    telephone.setPhoneTechType(OTA_PTT_VOCE); //Codice OTA PTT. 1 = Voce, 3 = Fax.
                    customerType.getTelephone().add(telephone);
                }//-------------------------

                if (reservation.get("reservation_email") != null) {
                    CustomerType.Email email = new CustomerType.Email();
                    email.setEmailType(OTA_EAT_HOME); //Codice OTA EAT. 1 = Home, 2 = Bussiness.
                    email.setDefaultInd(Boolean.TRUE);
                    email.setValue((String) reservation.get("reservation_email"));
                    customerType.getEmail().add(email);
                }

                CustomerType.Address address = new CustomerType.Address();
                address.setType("1"); //Codice OTA CLT. 1 = Home.
                address.getAddressLine().add((String) reservation.get("reservation_address"));
                address.setCityName((String) reservation.get("reservation_city"));
                address.setPostalCode((String) reservation.get("reservation_zipcode"));

                StateProvType stateProvType = new StateProvType();
                stateProvType.setStateCode((String) reservation.get("reservation_state"));
                address.setStateProv(stateProvType);

                CountryNameType countryNameType = new CountryNameType();
                countryNameType.setCode((String) reservation.get("reservation_country"));
                address.setCountryName(countryNameType);
                customerType.getAddress().add(address);

                CustomerType.URL url = new CustomerType.URL();
                customerType.getURL().add(url);

                CustomerType.PaymentForm paymentForm = new CustomerType.PaymentForm();

                paymentForm.setPaymentCard(paymentCardType);
                customerType.getPaymentForm().add(paymentForm);
                
                if(  reservation.get("reservation_company")!=null   ){
                    CompanyNameType companyNameType = new CompanyNameType();
                    companyNameType.setCompanyShortName((String)reservation.get("reservation_company") );
                    CompanyInfoType companyInfoType = new CompanyInfoType();
                    companyInfoType.getCompanyName().add(companyNameType);
                     
                    profileType.setCompanyInfo(companyInfoType); 
                }
                
                profileType.setCustomer(customerType);
                 
                profileInfo.setProfile(profileType);
                resGuest.getProfiles().getProfileInfo().add(profileInfo);
                hotelReservationType.getResGuests().getResGuest().add(resGuest);
            }
            //--------- resguest --------------------------------------------------------
        }

        List<RoomStaysType.RoomStay> roomStays = hotelReservationType.getRoomStays().getRoomStay();
        int i = 0;

        // imposta il prezzo totale della camera compresa di servizi/riduzioni
        for (Map<String, Object> resRoomData : reservationRoomData) {
            Object oresRoomData = resRoomData;
            
            roomStays.get(i++).getTotal().setAmountAfterTax(new BigDecimal(resRoomData.get("reservation_detail_price").toString()));
        }
        
        hotelReservationType.setResGlobalInfo(new ResGlobalInfoType());
        hotelReservationType.getResGlobalInfo().setComments(new CommentType());

        String sComment = reservation.get("reservation_note").toString();
        if (sComment == null) {
            sComment = "";
        }

        CommentType.Comment comment = new CommentType.Comment();
        JAXBElement e = new JAXBElement(new QName("http://www.opentravel.org/OTA/2003/05", "Text"), String.class, sComment);
        comment.getTextOrImageOrURL().add(e);
        hotelReservationType.getResGlobalInfo().getComments().getComment().add(comment);

        hotelReservationType.getResGlobalInfo().setGuarantee(new GuaranteeType());
        hotelReservationType.getResGlobalInfo().getGuarantee().setGuaranteesAccepted(new GuaranteeType.GuaranteesAccepted());

        GuaranteeType.GuaranteesAccepted.GuaranteeAccepted guaranteeAccepted = new GuaranteeType.GuaranteesAccepted.GuaranteeAccepted();
        guaranteeAccepted.setPaymentTransactionTypeCode("charge");
        guaranteeAccepted.setPaymentCard(paymentCardType);
        hotelReservationType.getResGlobalInfo().getGuarantee().getGuaranteesAccepted().getGuaranteeAccepted().add(guaranteeAccepted);

        HotelReservationID hotelReservationID = new HotelReservationID();
        hotelReservationID.setResIDType("10");
        //hotelReservationID.setResIDValue(rnum);
        hotelReservationID.setResIDValue(rNewNum);
        hotelReservationID.setForGuest(Boolean.TRUE);

        hotelReservationType.getResGlobalInfo().setHotelReservationIDs(new HotelReservationIDsType());
        hotelReservationType.getResGlobalInfo().getHotelReservationIDs().getHotelReservationID().add(hotelReservationID);

        hotelReservationType.getResGlobalInfo().setTotal(new TotalType());
        hotelReservationType.getResGlobalInfo().getTotal().setAmountAfterTax(new BigDecimal(reservation.get("reservation_tot_reservation_price").toString()));
        hotelReservationType.getResGlobalInfo().getTotal().setCurrencyCode("EUR");

        lHotelReservations.add(hotelReservationType);

        return res;
    }
    public OTAResRetrieveRS getRes() {
        return res;
    }
    private boolean thereIsError() {
        return !(res.getErrors() == null || res.getErrors().getError().isEmpty());
    }
    public static void main(String[] args) {    
          
        try {
            XmlRpcClient client = new XmlRpcClient("http://reservation.cmsone.it/backend/manager/xmlrpc/ser.php");
            client.setBasicAuthentication("otauser", "8eWruyEN");            
            Object res = ReservationDownloadServices.retrieveReservationsAllRPC(
                    client, "217", "test217ml", true
            );
            Map mapR = new Hashtable();
            
            mapR.put("a",res);
            MapUtils.debugPrint(System.out, "", mapR);
            
        } catch (MalformedURLException ex) {
            Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (XmlRpcException ex) {
            Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if(1==2) return;
        
        try {
            String tmpusr="S0003@unitas";
            
            String[]aUser=tmpusr.split("#");
            if(aUser.length==1){
                System.out.println("p=1 fisso" );
            }else{
                try {
                    System.out.println("p="+aUser[0]);
                } catch (Exception exception) {  }
            }
             
            
            if(1==1) return;
            
            DataSource ds= Facilities.createDataSource("root", "d0dgeram", "jdbc:mysql://192.168.1.226:3306/res_test_channels_domus_liberius");
            QueryRunner run = new QueryRunner(ds); 
                
            String user = "unitas";
            
            String  sqlChkUser = "SELECT permissions FROM ota_users WHERE user like ? AND structure_id=? AND deleted=?";
            String sUser = "%"+user;
            
            Map permission = null;

            try {
                permission = run.query(sqlChkUser, new MapHandler(), sUser, "1", 0);
            } catch (SQLException e) {
                Logger.getLogger(OTAHotelRoomListBuilder.class.getName()).log(Level.SEVERE, null, e);
            }
            
            
            
            
            MapUtils.debugPrint(System.out, sUser, permission);
            
            
            if(1==1) return;
            
            
            String chotel="217";
            Integer ihotelCode = new Integer(chotel);
            String reservationNumber = "M/Li8vfXbl7sqUvJb1";
            String sql = "select count(*) as reservation_count from reservation where structure_id=? AND reservation_number = ?";
            Object countRes = run.query(sql, new ScalarHandler("reservation_count"), ihotelCode , reservationNumber);     
              
            
            boolean texist = !countRes.toString().equals("0");
            System.out.println("Exist = " + texist);
            if(1==1) return;
            
            
            Date dt = new Date();
            DatatypeFactory df = DatatypeFactory.newInstance(); 
            GregorianCalendar gc = new GregorianCalendar(); 
            XMLGregorianCalendar reservationModDate = df.newXMLGregorianCalendar(gc);
            gc.setTimeInMillis(dt.getTime());

            System.out.println(reservationModDate.toString());
             

            if(1==1) return;
            try { 
                           
                List<Map<String, Object>> result = loadReservationPsSt(1, run);
               
                Document d = Facilities.mapToDocument(result,psFieldsTranslation); 
                System.out.println( Facilities.docToXmlString(d)   );
             
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            if(1==0) return;
            Map<String,String> map = new HashMap<String, String>  ();
            map.put("reservation_ps_role","chris"); map.put("reservation_ps_arrive_date","faranga");
            List a = new ArrayList();
            a.add(map);
              
            map = new HashMap<String, String>  (); map.put("reservation_ps_role","chris"); map.put("reservation_ps_arrive_date","faranga");
            a.add(map);
            map = new HashMap<String, String>  (); map.put("reservation_ps_role","chris"); map.put("reservation_ps_arrive_date","faranga");
            a.add(map);
            map = new HashMap<String, String>  (); map.put("reservation_ps_role","chris"); map.put("reservation_ps_arrive_date","faranga");
            a.add(map);
            map = new HashMap<String, String>  (); map.put("reservation_ps_role","chris"); map.put("reservation_ps_arrive_date","faranga");
            a.add(map);
              
            try {
                Document d = Facilities.mapToDocument(a,psFieldsTranslation); 
                //System.out.println( Facilities.docToXmlString(d)   );
            } catch (Exception ex) {
                Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
            
        } catch (Exception ex) {
            Logger.getLogger(OTAResRetrieveRSBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        
    }

    /**
     * @return the portalCode
     */
    public int getPortalCode() { 
        return portalCode;
    }

    /**
     * @param portalCode the portalCode to set
     */
    public void setPortalCode(int pPortalCode) {
        this.portalCode = pPortalCode;
    }
      
        
}
