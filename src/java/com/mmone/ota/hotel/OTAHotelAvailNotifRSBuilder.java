package com.mmone.ota.hotel;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import org.opentravel.ota._2003._05.ErrorType;
import org.opentravel.ota._2003._05.ErrorsType;
import org.opentravel.ota._2003._05.WarningsType;
import org.opentravel.ota._2003._05.WarningType;
import javax.sql.DataSource;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.Duration;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
 
import org.apache.xmlrpc.XmlRpcClient;
import org.opentravel.ota._2003._05.AvailStatusMessageType.RestrictionStatus;
import org.opentravel.ota._2003._05.LengthsOfStayType;
import org.opentravel.ota._2003._05.MessageAcknowledgementType;
import org.opentravel.ota._2003._05.OTAHotelAvailNotifRQ;
import org.opentravel.ota._2003._05.SuccessType;

//SetLimit, AdjustLimit, RemoveLimit.
public class OTAHotelAvailNotifRSBuilder extends BaseBuilder{
    public static final String AVAIL_ACTION_SET = "set";

    public static final String INVENTORY_IU = "1";
    public static final String INVENTORY_CM = "2";
    public static final String RATE_PLAN_CODE_NR = "NR";
    public static final String RATE_PLAN_CODE_IU = "IU";
    public static final String SET_MIN_LOS = "SetMinLOS";
    public static final int XRPC_SET_ALLOTMENT_RESULT_ERROR = -1;
    public static final int XRPC_SET_ALLOTMENT_RESULT_NO_VIRTUAL_ROOM = 1;
    public static final int XRPC_SET_ALLOTMENT_RESULT_VIRTUAL_ROOM_OK = 0;
    private Map<String, String> logData = new LinkedHashMap<String, String>();
    private DataSource ds;
    private OTAHotelAvailNotifRQ request;
    private QueryRunner run;
    private MessageAcknowledgementType res = new MessageAcknowledgementType();
    private HttpServletRequest httpRequest;
    private String user;
    private String hotelCode = null;
    private String structureName = null;
    private String structureEmail = null;
    private String langID;
    private String echoToken;
    private String target = Facilities.TARGET_PRODUCTION;
    private BigDecimal version = new BigDecimal(Facilities.VERSION);
    private String bookingLimit = null;
    private String bookingLimitMessageType = null;
    private boolean saveAllotment = false;
    private String startDt = null;
    private String endDt = null;
    private String invCode = null;
    private String sInvCode = null;
    private String ratePlanCode = null;
    private String sRatePlanCode = null;
    private String requestorID;
    private int lengthOfStay = 0;
    private BigInteger bLengthOfStay = null;
    private String minMaxMessageType = SET_MIN_LOS;
    private XmlRpcClient client;
    private InitialContext ctx = null;
    int therelease = 0; //MinAdvancedBookingOffset
    boolean isCheckIn = false;
    boolean isCheckOut = false;
    boolean isMinStay = false;
    boolean isTheRelease = false;
    int checkout = 0;
    int checkin = 0;
    private int sendEmail = 0; 
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

    private InitialContext getContext() throws NamingException {
        if (ctx == null) {
            ctx = new InitialContext();
        }
        return ctx;
    }

    public String getCrContext(String context) {
        try {
            return (String) getContext().lookup(context);
        } catch (Exception e) {
            return null;
        }
    }

    private void logInfoRequest(Map<String, String> infoRequest) {
        StringBuilder msg = new StringBuilder();
        msg.append("\n****");

        for (String key : infoRequest.keySet()) {
            msg.append("- ").append(key).append(" = ").append(infoRequest.get(key)).append(" ");
        }
        
        Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.INFO, msg.toString());
    }
    private boolean needBooking = false;
    public OTAHotelAvailNotifRSBuilder(DataSource ds, XmlRpcClient client, OTAHotelAvailNotifRQ request, String user, HttpServletRequest httpRequest,boolean needBooking) {
        super();

        if (ds == null) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Server. System Error: No connection with the database");
            return;
        }
        this.needBooking= needBooking;
        this.request = request;
        this.langID = request.getPrimaryLangID();
        this.user = user;
        this.ds = ds;
        this.echoToken = request.getEchoToken();
        this.target = request.getTarget();
        this.version = request.getVersion();
        this.client = client;
        this.httpRequest = httpRequest;
        
        if (!target.equals(Facilities.TARGET_PRODUCTION) && !target.equals(Facilities.TARGET_TEST)) {
            this.target = Facilities.TARGET_PRODUCTION;
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_REQUEST_CODE, "Target is invalid.");
        }

        if (langID == null || langID.equals("")) {
            addWarning(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_LANGUAGE_CODE_INVALID, "Language is invalid. Default to it.");
            langID = "it";
        }

        if (echoToken == null || echoToken.equals("")) {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_REQUEST_CODE, "EchoToken is invalid.");
        }

        if (!version.toString().equals(Facilities.VERSION)) {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_REQUEST_CODE, "Version is invalid.");
        }

        try {
            res.setTimeStamp(com.mmone.ota.rpc.Facilities.getToday());
        } catch (DatatypeConfigurationException ex) {
        }

        res.setEchoToken(echoToken);

        res.setTarget(target);
        res.setPrimaryLangID(langID);
        res.setVersion(version);

        //Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, "Access to OTAHotelAvailNotif. User: " + user);

        run = new QueryRunner(ds);

        // verifica permessi su struttura
        setHotelCode();

        String sqlChkUser = "SELECT permissions FROM ota_users WHERE user=? AND structure_id=? AND deleted=?";
        String sUser = user; 
        if(getJndiVersion()>=JNDIVER_101_AFTER_LIKE_ON_OTAUSERS ){  
                sqlChkUser = "SELECT permissions FROM ota_users WHERE user like ? AND structure_id=? AND deleted=?";
                sUser = "%"+user;
        }
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
            //int testEnv = p & Facilities.TEST_ENVIRONMENT;
            int prodEnv = p & Facilities.PRODUCTION_ENVIRONMENT;

            if (request.getTarget().equals("Production") && prodEnv == 0) {
                addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Client. Authorization Error: You don't have permission to select the production environment");
            }
        }

        if (thereIsError()) {
            return;
        }

        String sqlRetrieveStructure = ""
                + "SELECT structure_name,structure_email FROM structure "
                + "  WHERE structure_id = ?";

        Map structure = null;

        try {
            structure = run.query(sqlRetrieveStructure, new MapHandler(), hotelCode);
        } catch (SQLException e) {
            Logger.getLogger(OTAHotelInvCountBuilder.class.getName()).log(Level.SEVERE, null, e);
        }
        
        try {structureName = structure.get("structure_name").toString(); } catch (Exception e1) {}
        try {structureEmail = structure.get("structure_email").toString();} catch (Exception e1) {}
    }

    private void setHotelCode() {
        try {
            hotelCode = request.getAvailStatusMessages().getHotelCode();
        } catch (Exception e) {
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Server. System Error: " + e.getMessage());
        }

        if (hotelCode == null) {
            addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_HOTEL_CODE, "HotelCode null");
        }
    }

    private void setBookingLimit(int index) {
        try {
            bookingLimit = request.getAvailStatusMessages().getAvailStatusMessage().get(index).getBookingLimit().toString();

            bookingLimitMessageType = request.getAvailStatusMessages().getAvailStatusMessage().get(index).getBookingLimitMessageType();



        } catch (Exception e) {
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Server. System Error: " + e.getMessage());
        }

        if (bookingLimit == null) {
            addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_MANDATORY_BOOKING_DETAILS_MISSING, "BookingLimit null");
        }
        if (bookingLimitMessageType == null) {
            addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_MANDATORY_BOOKING_DETAILS_MISSING, "BookingLimitMessageType null");
        } else {
            //if(bookingLimitMessageType.equals("RemoveLimit")) saveAllotment=false; // tutte le disponibilità di tutti i listini vanno messi a zero compresi i CM
            if (bookingLimitMessageType.equals("SetLimit")) {
                saveAllotment = true;
            } else if (bookingLimitMessageType.equals("AdjustLimit")) {
                saveAllotment = true;
            } else {
                addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_MANDATORY_BOOKING_DETAILS_MISSING, "BookingLimitMessageType invalid. Use SetLimit or AdjustLimit");
            }
        }

        logData.put("Allotment", bookingLimit);
    }

    private void setStartDate(int index) {
        try {
            startDt = request.getAvailStatusMessages().getAvailStatusMessage().get(index).getStatusApplicationControl().getStart().toString();
        } catch (Exception e) {
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Server. System Error: " + e.getMessage());
        }

        if (startDt == null) {
            addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_START_DATE_INVALID, "Start date null");
        }

        logData.put("StartDate", startDt);
    }

    private void setEndDate(int index) {
        try {
            endDt = request.getAvailStatusMessages().getAvailStatusMessage().get(index).getStatusApplicationControl().getEnd().toString();
        } catch (Exception e) {
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Server. System Error: " + e.getMessage());
        }

        if (endDt == null) {
            addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_END_DATE_INVALID, "End date null");
        }

        logData.put("EndDate", endDt);
    }

    private void setRatePlanCode(int index) {
        try {
            sRatePlanCode = request.getAvailStatusMessages().getAvailStatusMessage().get(index).getStatusApplicationControl().getRatePlanCode().toString();
        } catch (Exception e) {
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Server. System Error: " + e.getMessage());
        }

        if (sRatePlanCode == null) {
            ratePlanCode = null;
        } else if (sRatePlanCode.equals(RATE_PLAN_CODE_NR)) {
            ratePlanCode = "1";
        } else if (sRatePlanCode.equals(RATE_PLAN_CODE_IU)) {
            ratePlanCode = "1";
        } else {
            if(this.needBooking){ 
                String sqlMl = "select multirate_id from multirate where multirate_code=? and structure_id=?";
                try {
                    Integer ilistId = (Integer)run.query(sqlMl, new ScalarHandler("multirate_id"), sRatePlanCode, hotelCode); 
                    ratePlanCode = ilistId.toString();
                } catch (Exception e) {
                   ratePlanCode=null;
                }
            }else{
                //@@@@@@ check it
                String sqlMl = "select multirate_id from multirate where multirate_code=? and structure_id=?";
                try {
                    Integer ilistId = (Integer)run.query(sqlMl, new ScalarHandler("multirate_id"), sRatePlanCode, hotelCode); 
                    ratePlanCode = ilistId.toString();
                } catch (Exception e) {
                   ratePlanCode=null;
                }
                
              //ratePlanCode=null;  
            }  
        }

        if (ratePlanCode == null) {
            addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_RATE_CODE, "RatePlanCode null");
        }

        logData.put("RatePlan", sRatePlanCode+" - "+ratePlanCode);
    }

    private void setInvCode(int index) {
        String _invCode = null;

        try {
            _invCode = request.getAvailStatusMessages().getAvailStatusMessage().get(index).getStatusApplicationControl().getInvCode().toString();
        } catch (Exception e) {
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Server. System Error: " + e.getMessage());
        }

        if (_invCode == null) {
            addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_ROOM_UNIT_CODE_INCORRECT, "InvCode null");
        }

        String sqlChkRoom = "SELECT room_id FROM room WHERE room_code=? AND structure_id=? ";

        try {
            invCode = run.query(sqlChkRoom, new ScalarHandler("room_id"), _invCode, hotelCode).toString();
        } catch (Exception ex) {
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, ex);
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "@InvCode is invalid");
        }

        sInvCode =_invCode;
        logData.put("InvCode", invCode);
    }

    private void setLenghtOfStay(int index) {
        String minMaxType = null;
        LengthsOfStayType lenghtsOfStay = null;

        try {
            lenghtsOfStay = request.getAvailStatusMessages().getAvailStatusMessage().get(index).getLengthsOfStay();
        } catch (Exception e) {
            //Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
            return;
        }

        if (lenghtsOfStay != null) {
            try {
                bLengthOfStay = request.getAvailStatusMessages().getAvailStatusMessage().get(index).getLengthsOfStay().getLengthOfStay().get(0).getTime();

            } catch (Exception e) {
                Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
                addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "@Time is invalid");
            }

            try {
                minMaxType = request.getAvailStatusMessages().getAvailStatusMessage().get(index).getLengthsOfStay().getLengthOfStay().get(0).getMinMaxMessageType();
            } catch (Exception e) {
                addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "@MinMaxMessageType is invalid");
            }

            lengthOfStay = bLengthOfStay.intValue();
            if (lengthOfStay < 0) {
                addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_PRICE_CANNOT_BE_VALIDATED, "@Time is less than zero");
            } else {
                logData.put("MinStay", Integer.toString(lengthOfStay));
                isMinStay = true;
            }

            if (!minMaxType.equals(SET_MIN_LOS)) {
                addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_REQUIRED_FIELD_MISSING, "@MinMaxMessageType is invalid");
            }
        }
    }

    private void setTherelease(int index) {
        Duration minAdvanced = null;

        try {
            minAdvanced = request.getAvailStatusMessages().getAvailStatusMessage().get(index).getRestrictionStatus().getMinAdvancedBookingOffset();
        } catch (Exception e) {
            //Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
            return;
        }

        if (minAdvanced != null) {
            try {
                
                RestrictionStatus rst= request.getAvailStatusMessages().getAvailStatusMessage().get(index).getRestrictionStatus();        
                therelease = rst.getMinAdvancedBookingOffset().getDays();
                
                isTheRelease = true;
                logData.put("Release", Integer.toString(therelease));
            } catch (Exception e) {
                Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
                addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "@MinAdavancedBokingOffset is invalid");
            }
        }
    }

    private void setCheckinCheckOut(int index) {
        String status = null;
        String restriction = null;
        RestrictionStatus restrictionStatus = null;

        try {
            restrictionStatus = request.getAvailStatusMessages().getAvailStatusMessage().get(index).getRestrictionStatus();
        } catch (Exception e) {
            //Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
            return;
        }

        if (restrictionStatus != null) {
            try {
                status = request.getAvailStatusMessages().getAvailStatusMessage().get(index).getRestrictionStatus().getStatus().get(0);

                restriction = request.getAvailStatusMessages().getAvailStatusMessage().get(index).getRestrictionStatus().getRestriction().get(0);

            } catch (Exception e) {
                Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
                addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "@Status or @Restriction are invalid");
            }

            //A C D C logcheckin logcheckout 1
            //A o D o logcheckin logcheckout 0
            //System.out.println(" Status " + status);
            //System.out.println(" Restrtiction " + restriction);
            isCheckIn = false;
            isCheckOut = false;

            if (restriction == null || status == null) {
            } else {
                if (restriction.equals("Arrival")) {
                    if (status.equals("Close")) {
                        checkin = 1;
                        isCheckIn = true;
                    } else if (status.equals("Open")) {
                        checkin = 0;
                        isCheckIn = true;
                    } // else errore
                    logData.put("Arrival", status);
                } else if (restriction.equals("Departure")) {
                    if (status.equals("Close")) {
                        checkout = 1;
                        isCheckOut = true;
                    } else if (status.equals("Open")) {
                        checkout = 0;
                        isCheckOut = true;
                    } // else errore
                    logData.put("Departure", status);
                }else if (restriction.equals("Both")) {
                    if (status.equals("Close")) {
                        checkout = 1;
                        isCheckOut = true;
                        checkin = 1;
                        isCheckIn = true;
                    } else if (status.equals("Open")) {
                        checkout = 0;
                        isCheckOut = true;
                        checkin = 0;
                        isCheckIn = true;
                    }  else if (status.equals("AOpenDClose")) {
                        checkout = 1;
                        isCheckOut = true;
                        checkin = 0;
                        isCheckIn = true;
                    } else if (status.equals("ACloseDOpen")) {
                        checkout = 0;
                        isCheckOut = true;
                        checkin = 1;
                        isCheckIn = true;
                    }   
                    logData.put("Departure", status);
                }
            }
        }
    }
    
    private void setCheckinCheckOut_for(int index) {
        String status = null;
        String restriction = null;
        RestrictionStatus restrictionStatus = null;

        try {
            restrictionStatus = request.getAvailStatusMessages().getAvailStatusMessage().get(index).getRestrictionStatus();
        } catch (Exception e) {
            //Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
            return;
        }
        
        isCheckIn = false;
        isCheckOut = false;
        
        if (restrictionStatus != null) {
            
            for (int i = 0; i < restrictionStatus.getStatus().size(); i++) { 
                status=null;
                restriction=null;
                
                try { 
                    status = restrictionStatus.getStatus().get(i);
                    restriction = restrictionStatus.getRestriction().get(i);
                } catch (Exception e) {
                    Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
                    addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "@Status or @Restriction are invalid");
                }
 
                if (restriction == null || status == null) {
                } else {
                    if (restriction.equals("Arrival")) {
                        if (status.equals("Close")) {
                            checkin = 1;
                            isCheckIn = true;
                        } else if (status.equals("Open")) {
                            checkin = 0;
                            isCheckIn = true;
                        } // else errore
                        logData.put("Arrival", status);
                    } else if (restriction.equals("Departure")) {
                        if (status.equals("Close")) {
                            checkout = 1;
                            isCheckOut = true;
                        } else if (status.equals("Open")) {
                            checkout = 0;
                            isCheckOut = true;
                        } // else errore
                        logData.put("Departure", status);
                    }
                }
            }
        }    
    }
    
    private void setVars(int index) {
        try {
            setBookingLimit(index);
            setStartDate(index);
            setEndDate(index);
            setRatePlanCode(index);
            setInvCode(index);
            setLenghtOfStay(index);
            setCheckinCheckOut(index);
            setTherelease(index);
        } catch (Exception e) {
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Setting parameters failed");
        }
    }

    private boolean isRoomCorrect() {
        String sqlChkRoom = "SELECT COUNT(*) as tot FROM room WHERE room_id=? AND structure_id=? ";
        long count = 0;
        try {
            count = (Long) run.query(sqlChkRoom, new ScalarHandler("tot"), invCode, hotelCode);
        } catch (SQLException e) {
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Server. System Error: " + e.getMessage());
        }

        if (count == 0) {
            //camera non associata alla struttura
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_ROOM_UNIT_CODE_INCORRECT, "Room not in the structure");
            return false;
        } else {
            return true;
        }
    }

    private void doSaveFactory() {
        try {
            if (!isRoomCorrect()) {
                return;
            }
            int period = (int) Facilities.dateDiff(startDt, endDt);
            if (period < 0) {
                //date errate
                addError(Facilities.EWT_UNKNOWN, Facilities.ERR_INVALID_DATE, "Start date after End date");
                return;
            }

            // da verificare
            //if (period < 1) {
            //date errate
            //addWarning(Facilities.EWT_UNKNOWN, Facilities.ERR_INVALID_DATE, "Short period (one day)");
            //return;
            //}

            // cambiare per Normal Rate
            if (sRatePlanCode.equals(RATE_PLAN_CODE_NR)) {
                doSaveIU(period);
            } else if (sRatePlanCode.equals(RATE_PLAN_CODE_IU)) {
                doSaveIU(period);
            }else{
              
                /*
                String sqlMl = "select multirate_id from multirate where multirate_code=? and structure_id=?";
                Integer rateId=null; 
                 
                rateId = (Integer) run.query(sqlMl, new ScalarHandler("multirate_id"), sRatePlanCode, hotelCode);
                doSaveIU(period); */
            }

        } catch (Exception ex) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Server. System Error: " + ex.getMessage());
        }


    }

    private void doDeleteFactory() {
        if (!isRoomCorrect()) {
            return;
        }
        try {
            int period = (int) Facilities.dateDiff(startDt, endDt);
            if (sRatePlanCode.equals(RATE_PLAN_CODE_NR)) {
                doDeleteNR(period);
            } else if (sRatePlanCode.equals(RATE_PLAN_CODE_IU)) {
                doDeleteIU(period);
            }

        } catch (Exception ex) {
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }



    }

    private void doDeleteIU(int period) {
    }

    private String getSqlInsertAllotment1() {
        String sqlIns = ""
                + " INSERT INTO allotment ("
//                + (isCheckIn ? "  lock_checkin," : "")
//                + (isCheckOut ? "  lock_checkout," : "")
                + "     structure_id,"
                + "     list_id, "
                + "     room_id, "
                + "     thedate, "
//                + (isMinStay ? "     minstay, " : "")
//                + (isTheRelease ? "     therelease, " : "")
                + "     availability"
                + ")"
                + " VALUES ("
//                + (isCheckIn ? "?," : "")
//                + (isCheckOut ? "?," : "")
                + "?,?,?,?,"
//                + (isMinStay ? "?, " : "")
//                + (isTheRelease ? "?, " : "")
                + " ?) "
                + " ON DUPLICATE KEY UPDATE "
//                + (isCheckIn ? "  lock_checkin=?," : "")
//                + (isCheckOut ? "  lock_checkout=?," : "")
                + "     thedate=?, "
//                + (isMinStay ? "     minstay=?, " : "")
//                + (isTheRelease ? "     therelease=?, " : "")
                + "     availability=?"
                + "";

        return sqlIns;

    }

    private String getSqlInsertAllotment(   ) {
        String slock_checkin=""; 
        if(isCheckIn)               slock_checkin   =  "lock_checkin=?," ;  
        String slock_checkout=""; 
        if(isCheckOut)              slock_checkout  =  "lock_checkout=?," ;  
        String sminstay=""; 
        if(isMinStay)               sminstay  =  "minstay=?," ; 
        String stherelease=""; 
        if(isTheRelease)            stherelease  =      "therelease=?," ;
          
        String sqlIns = ""
                + " INSERT INTO allotment ("
                + " lock_checkin,"  
                + " lock_checkout," 
                + " structure_id,"
                + " list_id, "
                + " room_id, "
                + " thedate, "
                + " minstay, "  
                + " therelease, " 
                + " availability"
                + ")"
                + " VALUES (?,?,?,?,?,?,?,?,?) "
                + " ON DUPLICATE KEY UPDATE "
                + slock_checkin
                + slock_checkout
                + " thedate=?, "
                + sminstay    
                + stherelease    
                + " availability=?"
                + "";
                
                Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.INFO, sqlIns );
        return sqlIns;

    }
     
    private String getSqlInsertAllotmentGateway() {
        String sqlIns = ""
                + " INSERT INTO allotment ("
                + (isCheckIn ? "  lock_checkin," : "")
                + (isCheckOut ? "  lock_checkout," : "")
                + "     structure_id,"
                + "     list_id, "
                + "     room_id, "
                + "     thedate, "
                + (isMinStay ? "     minstay, " : "")
                + (isTheRelease ? "     therelease, " : "")
                + "     availability"
                + ")"
                + " VALUES ("
                + (isCheckIn ? "?," : "")
                + (isCheckOut ? "?," : "")
                + "?,?,?,?,"
                + (isMinStay ? "?, " : "")
                + (isTheRelease ? "?, " : "")
                + " ?) "
                + " ON DUPLICATE KEY UPDATE "
                + (isCheckIn ? "  lock_checkin=?," : "")
                + (isCheckOut ? "  lock_checkout=?," : "")
                + "     thedate=?, "
                + (isMinStay ? "     minstay=?, " : "")
                + (isTheRelease ? "     therelease=?, " : "")
                + "     availability=?"
                + "";

        return sqlIns;

    }
        
    private void doSaveIU(int period) {
        QueryRunner run = new QueryRunner(ds);
        Connection conn = null;

        try {
            java.util.Date dtStart = DateUtils.parseDate(startDt, Facilities.dateParsers);

            java.util.Date today = new java.util.Date();

            if (!DateUtils.isSameDay(dtStart, today)) {
                if (dtStart.before(today)) {
                    addError(Facilities.EWT_UNKNOWN, Facilities.ERR_START_DATE_INVALID, "Start date is in the past");
                    return;
                }
            }

            conn = ds.getConnection();
            conn.setAutoCommit(false);

            String sqlInsInventory = ""
                    + " INSERT INTO inventory ("
                    + "     room_id, "
                    + "     inventory_type_id, " //1
                    + "     allotment_date, "
                    + "     allotment_number, "
                    + "     minimum_allotment_number, "
                    + "     inventory_modified_date, "
                    + "     inventory_modified_user "
                    + ")"
                    + " VALUES (?,?,?,?,?,?,?) "
                    + " ON DUPLICATE KEY UPDATE "
                    + "     allotment_number=?, "
                    + "     minimum_allotment_number=?, "
                    + "     inventory_modified_date=?, "
                    + "     inventory_modified_user=? "
                    + "";

            String sqlInsAllotment = getSqlInsertAllotment();

            //recupero quanti allotment sono presenti nei listini
            String sqlInvent = ""
                    + "SELECT sum(allotment_number) as sum_of_allot "
                    + " FROM inventory "
                    + " WHERE "
                    + " room_id = ? "
                    + " AND allotment_date = ? "
                    + " AND inventory_type_id = ? "
                    + " AND allotment_number IS NOT NULL"
                    + "";

            //recupero quanti allotment sono presenti nei listini
            String sqlAllot = ""
                    + "SELECT sum(availability) as sum_of_avail "
                    + " FROM allotment "
                    + " WHERE "
                    + " room_id = ? "
                    + " AND thedate = ? "
                    + "";

            String sqlResetAllotment = ""
                    + " INSERT INTO allotment ("
                    + "     structure_id,"
                    + "     list_id, "
                    + "     room_id, "
                    + "     thedate, "
                    + "     availability"
                    + ")"
                    + " VALUES (" + "?, ?, ?, ?, ?" + ") "
                    + " ON DUPLICATE KEY UPDATE "
                    + "     thedate = ?, "
                    + "     availability = ?"
                    + "";
            // DEBUG
            //System.out.println("SqlInsAllotment: "  + sqlInsAllotment);

            String sqlInsertInventoryIssues = ""
                    + " INSERT INTO inventory_issues ("
                    + "      structure_id,"
                    + "      room_id,"
                    + "      issue_date,"
                    + "      accomplished"
                    + ")"
                    + " VALUES (" + "?, ?, ?, ?" + ")";

            String sqlUpdateInventoryIssues = ""
                    + " UPDATE inventory_issues SET"
                    + "       accomplished = ?"
                    + " WHERE"
                    + "      structure_id = ?"
                    + " AND"
                    + "      room_id = ?"
                    + " AND"
                    + "      issue_date = ?";
                    

            String sqlUpdateNormalRateRestrictions = ""
                    + " UPDATE allotment SET"
                    + (isCheckIn ? "  lock_checkin = ?," : "")
                    + (isCheckOut ? "  lock_checkout = ?," : "")
                    + (isMinStay ? "     minstay = ?, " : "")
                    + (isTheRelease ? "     therelease = ?, " : "")
                    + " thedate = ? "
                    + " WHERE structure_id = ? AND "
                    + "     list_id = ? AND "
                    + "     room_id = ? AND "
                    + "     thedate = ?";

            String sqlAllotLp = sqlAllot + " AND list_id = ? ";

            PreparedStatement psSqlInsInventory = conn.prepareStatement(sqlInsInventory);
            PreparedStatement psSqlInsAllotment = conn.prepareStatement(sqlInsAllotment);
            PreparedStatement psSqlResetAllotment = conn.prepareStatement(sqlResetAllotment);
            PreparedStatement psSqlInsInventoryIssues = conn.prepareStatement(sqlInsertInventoryIssues);
            PreparedStatement psSqlUpdInventoryIssues = conn.prepareStatement(sqlUpdateInventoryIssues);
            PreparedStatement psSqlUpdNormalRateRestrictions = conn.prepareStatement(sqlUpdateNormalRateRestrictions);

            //recupero listino prioritario
            String sqlListPri = "SELECT list_id from priority_inventory WHERE structure_id = ? ";
            int idListinoPrioritario = -1;

            try {
                idListinoPrioritario = (Integer) run.query(sqlListPri, new ScalarHandler("list_id"), hotelCode);
            } catch (Exception e) {
                addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "System Error: priority inventor non valid");
                Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
            }

            if (idListinoPrioritario <= 0) {
                addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "System Error: priority inventory not found");
                return;
            }

            //System.out.println("idListinoPrioritario " +idListinoPrioritario);
            // carico il disponibile dal listino principale
            // carico il totale in tutti i listini
            // se i due totali non corrispondono ho valori anche in altri listini
            //
            // se non ho valori in altri listini
            //      se valore BookingLimit > availability listino principale availability diventa 
            //                               BookingLimit-sumOfAvailAltriListini=BookingLimit
            //      se valore BookingLimit = availability listino principale non faccio niente
            //      se valore BookingLimit < availability listino principale availability diventa 
            //                               BookingLimit-sumOfAvailAltriListini=BookingLimit
            // se ho valori in altri listini
            //      se BookingLimit > availability listino principale errore
            //      se BookingLimit = availability listino principale non faccio niente
            //      se BookingLimit < availability listino principale availability diventa BookingLimit-sumOfAvailAltriListini

            
            //System.out.println("Period " + period);
            Map<String, String> roomsData = new LinkedHashMap<String, String>();
            StringBuffer track = new StringBuffer();
            
            track.append("\n<br>for (int i = 0; i <= period; i++) ");
            for (int i = 0; i <= period; i++) {
                java.util.Date curDate = DateUtils.addDays(dtStart, i);
                String sCurDate = DateFormatUtils.format(curDate, "dd-MM-yyyy");

                int sumOfCMAllot = 0;
                try {
                    sumOfCMAllot = new Integer(run.query(sqlInvent, new ScalarHandler("sum_of_allot"), invCode, curDate, INVENTORY_CM).toString());
                } catch (Exception e) {
                    //
                }

                int sumOfAvailTuttiListini = 0;
                try {
                    sumOfAvailTuttiListini = new Integer(run.query(sqlAllot, new ScalarHandler("sum_of_avail"), invCode, curDate).toString());
                } catch (Exception e) {
                    //
                }
                sumOfAvailTuttiListini = sumOfAvailTuttiListini + sumOfCMAllot;

                int sumOfAvailLp = 0;
                try {
                    sumOfAvailLp = new Integer(run.query(sqlAllotLp, new ScalarHandler("sum_of_avail"), invCode, curDate, idListinoPrioritario).toString());
                } catch (Exception e) {
                    //
                }
                int sumOfAvailAltriListini = sumOfAvailTuttiListini - sumOfAvailLp;

                int iBookingLimit = new Integer(bookingLimit);
                int curAllotment = iBookingLimit - sumOfAvailAltriListini;

                int onlyIU = 0; // cotrollo in riduzione
                                
                if (curAllotment < 0 && iBookingLimit != 0) {
                    onlyIU = 1;
                    roomsData.put(sCurDate, sInvCode);
                    //addWarning(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "InvCode " + sInvCode + " date " + sCurDate + ": BookingLimit less than priority inventory availability.");
                }
                onlyIU = 0;
                int xrpcresult = modifyAllotment( curDate, AVAIL_ACTION_SET,iBookingLimit,0); 
                
                track.append("\n<br>xrpcresult");
                 if (xrpcresult == XRPC_SET_ALLOTMENT_RESULT_ERROR) {
                     Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.WARNING, "Stop procedure " ); 
                     return;
                 }else if (xrpcresult == XRPC_SET_ALLOTMENT_RESULT_NO_VIRTUAL_ROOM) { //faccio insert a mano
                    if (iBookingLimit == 0) {
                        track.append("\n<br>iBookingLimit == 0");    
                        Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.WARNING, "Bookinglimit = 0 " );    
                        // Inventory IU
                        fillInsertInventoryParameters(psSqlInsInventory, curDate, today, INVENTORY_IU, iBookingLimit);
                        psSqlInsInventory.execute();
                        track.append("\n<br>iBookingLimit == 0 fillInsertInventoryParameters");
                        // Inventory CM
                        fillInsertInventoryParameters(psSqlInsInventory, curDate, today, INVENTORY_CM, iBookingLimit);
                        psSqlInsInventory.execute();

                        // tutti i listini
                        String listId = "1"; // normal rate
                        fillInsertResetAllotment(psSqlResetAllotment, listId, curDate);
                        psSqlResetAllotment.execute();
                        track.append("\n<br>iBookingLimit == 0 fillInsertResetAllotment");
                        listId = "2"; // special rate
                        fillInsertResetAllotment(psSqlResetAllotment, listId, curDate);
                        psSqlResetAllotment.execute();

                        // multilistini
                        List<Map<String, Object>> lMultirate = null;
                        String sqlMultirate = "SELECT multirate_id "
                                + " FROM multirate WHERE structure_id = ?";

                        try {
                            track.append("\n<br>iBookingLimit == 0 lMultirate");
                            lMultirate = run.query(sqlMultirate, new MapListHandler(), hotelCode);
                        } catch (SQLException e) {
                            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
                        }

                        if (lMultirate != null) {
                            for (Map<String, Object> multirate : lMultirate) {
                                listId = multirate.get("multirate_id").toString();
                                fillInsertResetAllotment(psSqlResetAllotment, listId, curDate);
                                psSqlResetAllotment.execute();
                            }
                        }

                        // ABS GATEWAY
                        
                        try {
                            track.append("\n<br>iBookingLimit == 0 updateAbsGateway");
                            updateAbsGateway( curDate); // boolean
                        } catch (SQLException e) {
                            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
                            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Error on update IDS");
                        } catch (Exception e) {
                            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
                            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Error on update structure " + e.getMessage());
                        }

                        // CM GATEWAY
                        try {
                            track.append("\n<br>iBookingLimit == 0 updateCMGateway");
                            updateCMGateway(curDate);
                        } catch (Exception e) {
                        }
                          
                         
 

                    } else { // solo inventario unico e tariffa prioritaria
                        // Inventory IU
                        track.append("\n<br>iBookingLimit > 0  ");
                        Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.WARNING, "Bookinglimit != 0 " );
                        fillInsertInventoryParameters(psSqlInsInventory, curDate, today, INVENTORY_IU, iBookingLimit);
                        psSqlInsInventory.execute();
                        track.append("\n<br>iBookingLimit > 0  fillInsertInventoryParameters");
                        if (onlyIU == 0) { // Tariffa prioritaria
                            track.append("\n<br>iBookingLimit > 0  onlyIU == 0");
                            fillUpdateInventoryIssues(psSqlUpdInventoryIssues, curDate, 1);
                            psSqlUpdInventoryIssues.execute();
                            track.append("\n<br>iBookingLimit > 0  onlyIU == 0 fillUpdateInventoryIssues");
                            fillInsertAllotmentParameters(psSqlInsAllotment, curDate, new Integer(idListinoPrioritario).toString(), curAllotment); // listino prioritario
                            psSqlInsAllotment.execute();
                            track.append("\n<br>iBookingLimit > 0  onlyIU == 0 fillInsertAllotmentParameters");
                            // aggiorna eventuali restrictions nel normal rate
                            try {
                                track.append("\n<br>iBookingLimit > 0  onlyIU == 0 fillUpdateNormalRateRestrictions");
                                fillUpdateNormalRateRestrictions(psSqlUpdNormalRateRestrictions, curDate);
                                psSqlUpdNormalRateRestrictions.execute();
                                track.append("\n<br>iBookingLimit > 0  onlyIU == 0 fillUpdateNormalRateRestrictions -- done");
                            } catch (Exception e) {
                                System.out.println("psSqlUpdNormalRateRestrictions "+e.getMessage()); 
                                Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE,"psSqlUpdNormalRateRestrictions error ",e);
                            }
                        } else if (onlyIU == 1) { // aggiorna giorni errati
                            track.append("\n<br>iBookingLimit > 0  onlyIU == 1 fillUpdateInventoryIssues");
                            fillUpdateInventoryIssues(psSqlUpdInventoryIssues, curDate, 1);
                            psSqlUpdInventoryIssues.execute();
                            track.append("\n<br>iBookingLimit > 0  onlyIU == 1 fillInsertInventoryIssues");
                            fillInsertInventoryIssues(psSqlInsInventoryIssues, curDate, 0);
                            psSqlInsInventoryIssues.execute();
                        }
                    }
                }
            
            }
            
            
            
            
            if (!roomsData.isEmpty() && sendEmail == 0) {
                //Facilities.sendEmail(getCrContext("cr/url" + requestorID), structureName, structureEmail, roomsData);
                //sendEmail = 1;
            }
        } catch (Exception e) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Server. Insert data failed");
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
            DbUtils.rollbackAndCloseQuietly(conn);
        } finally {
            DbUtils.commitAndCloseQuietly(conn);
        }
    }
    
    private int modifyAllotment(java.util.Date curDate,String action,int availability,int reservation){
        Vector parameters=new Vector();   
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        parameters.add(new Integer(hotelCode)); //1
        parameters.add(new Integer(invCode)); //2
        parameters.add(new Integer(-1)); //3 offerta
        parameters.add(new Integer(availability)); //4 disponibilità
        parameters.add(new Integer(reservation)); //5 prenotazione
        parameters.add(action); //6  Azione : set,increase,decrease
        parameters.add(df.format(curDate).toString());  //7
        parameters.add(df.format(curDate).toString());  //8
        Vector result = new Vector();
        int ret = XRPC_SET_ALLOTMENT_RESULT_ERROR;
        try { 
            result = (Vector) client.execute("backend.modifyAllotment", parameters); 
             
        } catch (Exception e) {
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, "", e);
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Error on updating  allotment (modifyAllotment)");
            return ret ;
        }
        
        try { 
            Map hret = (Map)result.get(0); 
            ret = new Integer(  (String)hret.get("unique_allotment_service_response") );  
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.WARNING, "Xrpc done " );
             
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.WARNING, "ret value " + ret );     
        } catch (NullPointerException e) {
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, "", e);    
            return XRPC_SET_ALLOTMENT_RESULT_NO_VIRTUAL_ROOM ;
        } catch (ClassCastException e) {
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, "", e);
            return XRPC_SET_ALLOTMENT_RESULT_NO_VIRTUAL_ROOM ;    
        } catch (Exception e) {
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, "", e);
            return XRPC_SET_ALLOTMENT_RESULT_NO_VIRTUAL_ROOM ;
        }
        
        return ret;
    }
    private void fillInsertResetAllotment(PreparedStatement ps, String rplan, java.util.Date curDate) throws SQLException {
        int j = 1;

        ps.setObject(j++, hotelCode);
        ps.setObject(j++, rplan);
        ps.setObject(j++, invCode);
        ps.setObject(j++, curDate);
        ps.setObject(j++, "0");

        //---------------- UPDATE  ---------------------------
        ps.setObject(j++, curDate);
        ps.setObject(j++, "0");

    }

    private void fillInsertAllotmentParameters(PreparedStatement ps, java.util.Date curDate, String rplan, int curAllotment) throws SQLException {
        int j = 1;
        Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.INFO, 
                ps.toString() 
        );
        ps.setObject(1, checkin);
        ps.setObject(2, checkout);
        ps.setObject(3, hotelCode);
        ps.setObject(4, rplan);
        ps.setObject(5, invCode);
        ps.setObject(6, curDate);
        ps.setObject(7, lengthOfStay); 
        ps.setObject(8, therelease);
        ps.setObject(9, curAllotment);
        j=9;
        //----------------   -----------------------------
        if (isCheckIn) {
            j++;
            ps.setObject(j, checkin);
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.INFO,  "J="+j + " checkin=" +checkin );
        }
        if (isCheckOut) {
            j++;
            ps.setObject(j, checkout);
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.INFO,  "J="+j + " checkout=" +checkout );
        }
        j++;
        ps.setObject(j, curDate);
        Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.INFO,  "J="+j + " curDate=" +curDate );
        if (isMinStay) {
            j++;
            ps.setObject(j, lengthOfStay);
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.INFO,  "J="+j + " lengthOfStay=" +lengthOfStay );
        }
        if (isTheRelease) {
            j++;    
            ps.setObject(j, therelease);
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.INFO,  "J="+j + " therelease=" +therelease );
        }
        j++;
        ps.setObject(j, curAllotment);
        Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.INFO,  "J="+j + " curAllotment=" +curAllotment );
        
         
    }

    private void fillUpdateNormalRateRestrictions(PreparedStatement ps, java.util.Date curDate) throws SQLException {
        int j = 1;
        
        Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.INFO, "[fillUpdateNormalRateRestrictions] 1- " + ps.toString());
        if (isCheckIn) {
            ps.setObject(j++, checkin);
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.INFO,  "J="+j + " checkin=" +checkin );
        }
        if (isCheckOut) {
            ps.setObject(j++, checkout);
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.INFO,  "J="+j + " checkout=" +checkout );
        }
        if (isMinStay) {
            ps.setObject(j++, lengthOfStay);
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.INFO,  "J="+j + " lengthOfStay=" +lengthOfStay );
        }
        if (isTheRelease) {
            ps.setObject(j++, therelease);
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.INFO,  "J="+j + " therelease=" +therelease );
        }
        
        ps.setObject(j++, curDate);
        ps.setObject(j++, hotelCode);
        ps.setObject(j++, "1");
        ps.setObject(j++, invCode);
        ps.setObject(j++, curDate);
    }

    private void fillInsertAbsGatewayAllotmentParameters(PreparedStatement ps, String remoteHotelCode, java.util.Date curDate, String rplan, String remoteRoomId, int curAllotment) throws SQLException {
        int j = 1;

        if (isCheckIn) ps.setObject(j++, checkin); 
        if (isCheckOut) ps.setObject(j++, checkout);
        ps.setObject(j++, remoteHotelCode);
        ps.setObject(j++, rplan);
        ps.setObject(j++, remoteRoomId);
        ps.setObject(j++, curDate);
        
        if (isMinStay) ps.setObject(j++, lengthOfStay); 
        if (isTheRelease) ps.setObject(j++, therelease); 
        ps.setObject(j++, curAllotment);
         
        if (isCheckIn) ps.setObject(j++, checkin); 
        if (isCheckOut) ps.setObject(j++, checkout);
        
        ps.setObject(j++, curDate);
        if (isMinStay) ps.setObject(j++, lengthOfStay); 
        if (isTheRelease) ps.setObject(j++, therelease); 
        ps.setObject(j++, curAllotment);
    }

    private void fillInsertAbsGatewayAllotmentParameters_old(PreparedStatement ps, String remoteHotelCode, java.util.Date curDate, String rplan, String remoteRoomId, int curAllotment) throws SQLException {
        int j = 1;

        ps.setObject(j++, remoteHotelCode);
        ps.setObject(j++, rplan);
        ps.setObject(j++, remoteRoomId);
        ps.setObject(j++, curDate);
        ps.setObject(j++, curAllotment);

        //----------------   -----------------------------
        if (isCheckIn) {
           ps.setObject(j++, checkin);
        }
        if (isCheckOut) {
           ps.setObject(j++, checkout);
        }
        ps.setObject(j++, curDate);
        if (isMinStay) {
           ps.setObject(j++, lengthOfStay);
        }
        if (isTheRelease) {
           ps.setObject(j++, therelease);
        }
        ps.setObject(j++, curAllotment);
    }
    
    private void fillInsertInventoryParameters(PreparedStatement psSqlInsInventory, java.util.Date curDate, java.util.Date today, String inventoryTypeId, int curAllotment) throws SQLException {

        int j = 1;
        psSqlInsInventory.setObject(j++, invCode); //room_id
        psSqlInsInventory.setObject(j++, inventoryTypeId);     //inventory_type_id
        psSqlInsInventory.setObject(j++, curDate); //allotment_date
        psSqlInsInventory.setObject(j++, curAllotment); //allotment_number BookingLimit
        psSqlInsInventory.setObject(j++, 0); //minimum_allotment_number  a 0 momentaneamente
        psSqlInsInventory.setObject(j++, today); //inventory_modified_date
        psSqlInsInventory.setObject(j++, -1); //inventory_modified_user
        //-----------------------------------------------------
        psSqlInsInventory.setObject(j++, curAllotment); //allotment_number
        psSqlInsInventory.setObject(j++, 0); //minimum_allotment_number
        psSqlInsInventory.setObject(j++, today); //inventory_modified_date
        psSqlInsInventory.setObject(j++, -1); //inventory_modified_user
    }

    private void fillInsertInventoryIssues(PreparedStatement psSqlInsInventoryIssues, java.util.Date curDate, int accomplished) throws SQLException {
        int j = 1;
        
        psSqlInsInventoryIssues.setObject(j++, hotelCode);
        psSqlInsInventoryIssues.setObject(j++, invCode);
        psSqlInsInventoryIssues.setObject(j++, curDate);
        psSqlInsInventoryIssues.setObject(j++, accomplished);
    }

    private void fillUpdateInventoryIssues(PreparedStatement psSqlInsInventoryIssues, java.util.Date curDate, int accomplished) throws SQLException {
        int j = 1;

        psSqlInsInventoryIssues.setObject(j++, accomplished);
        psSqlInsInventoryIssues.setObject(j++, hotelCode);
        psSqlInsInventoryIssues.setObject(j++, invCode);
        psSqlInsInventoryIssues.setObject(j++, curDate);
    }


    private void doSaveNR(int period) {
        QueryRunner qr = new QueryRunner(ds);
        Connection conn = null;

        try {
            java.util.Date dtStart = DateUtils.parseDate(startDt, Facilities.dateParsers);
            java.util.Date today = new java.util.Date();

            if (dtStart.before(today)) {
                addError(Facilities.EWT_UNKNOWN, Facilities.ERR_START_DATE_INVALID, "Start date is in the past ");
                return;
            }

            conn = ds.getConnection();
            conn.setAutoCommit(false);

            String sqlIns = getSqlInsertAllotment();
            PreparedStatement ps = conn.prepareStatement(sqlIns);
            for (int i = 0; i <= period; i++) {
                java.util.Date curDate = DateUtils.addDays(dtStart, i);
                String sCurDate = DateFormatUtils.format(curDate, "dd-MM-yyyy");

                try {
                    fillInsertAllotmentParameters(ps, curDate, ratePlanCode, new Integer(bookingLimit));
                    ps.execute();
                } catch (Exception e) {
                    addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Server. System Error: " + e.getMessage() + " Date " + sCurDate);
                    Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, "", e);
                }

            }

        } catch (Exception e) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Server. System Error: " + e.getMessage());
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, "", e);
            DbUtils.rollbackAndCloseQuietly(conn);
        } finally {
            DbUtils.commitAndCloseQuietly(conn);
        }
    }

    private void doDeleteNR(int period) {
        QueryRunner qr = new QueryRunner(ds);
        Connection conn = null;

        try {
            java.util.Date dtStart = DateUtils.parseDate(startDt, Facilities.dateParsers);

            conn = ds.getConnection();
            conn.setAutoCommit(false);

            String sqlDel = "DELETE FROM allotment WHERE structure_id=? AND list_id=? AND room_id=? AND thedate=?  ";

            PreparedStatement ps = conn.prepareStatement(sqlDel);

            for (int i = 0; i <= period; i++) {
                java.util.Date curDate = DateUtils.addDays(dtStart, i);

                int j = 1;
                ps.setObject(j++, hotelCode);
                ps.setObject(j++, ratePlanCode);
                ps.setObject(j++, invCode);
                ps.setObject(j++, curDate);

                ps.execute();
            }

        } catch (Exception e) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Server. System Error: " + e.getMessage());
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, "", e);
            DbUtils.rollbackAndCloseQuietly(conn);
        } finally {
            DbUtils.commitAndCloseQuietly(conn);
        }

    }

    public MessageAcknowledgementType build(String requestorID) {
        this.requestorID = requestorID;
        
        for (int i = 0; i < request.getAvailStatusMessages().getAvailStatusMessage().size(); i++) {

            isCheckIn = false;
            isCheckOut = false;
            isMinStay = false;
            isTheRelease = false;

            logData.put("Class", this.getClass().getName());
            logData.put("TimeStamp", res.getTimeStamp().toString());
            logData.put("user", user);
            logData.put("RemoteAddr", httpRequest.getRemoteAddr());
            logData.put("EchoToken", echoToken);
            logData.put("Target", target);
            logData.put("HotelCode", hotelCode);
            
            setVars(i);
            if (!thereIsError()) {
                try {
                    if (saveAllotment) {
                        doSaveFactory();
                    } else {
                        doDeleteFactory();
                    }
                } catch (Exception e) {
                    Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, "", e);
                    addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Server. System Error: " + e.getMessage());
                }
            }
            
            logInfoRequest(logData);
            logData = new LinkedHashMap<String, String>(); // nuova mappa
        } //end for

        if (!thereIsError()) {
            res.setSuccess(new SuccessType());
        }

        return res;
    }

    private boolean updateAbsGateway( java.util.Date curDate) throws SQLException {
        // ABS GATEWAY
        List<Map<String, Object>> absGatewayMapUser = null;
        String sqlAbsGatewayMapUser = "SELECT abs_gateway_id, remote_structure_id FROM abs_gateway_map_user WHERE structure_id = ?";

        // recuper abs gateway collegati e id struttura remota
        try {
            absGatewayMapUser = run.query(sqlAbsGatewayMapUser, new MapListHandler(), hotelCode);
        } catch (SQLException e) {
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, "", e);
            return false;
        }

        if (absGatewayMapUser != null) { // esiste almeno un collegamento
            for (Map<String, Object> record : absGatewayMapUser) { // ciclo per ogni gateway collegato
                String absGatewayId = record.get("abs_gateway_id").toString();
                String remoteStructureId = record.get("remote_structure_id").toString();

                Map absGateway = null;
                String sqlAbsGateway = "SELECT abs_host, abs_database, abs_database_user, abs_database_password"
                        + " FROM abs_gateway WHERE abs_gateway_id = ?";

                try {
                    absGateway = run.query(sqlAbsGateway, new MapHandler(), absGatewayId);
                } catch (SQLException e) {
                    Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, "", e);
                    return false;
                }

                // lettura room id
                List<Map<String, Object>> absGatewayRemoteRoom = null;
                String sqlAbsGatewayRemoteRoom = "SELECT abs_gateway_remote_room_id"
                        + " FROM abs_gateway_map_room WHERE abs_gateway_id = ? AND structure_id = ? AND abs_gateway_local_room_id = ?";

                try {
                    absGatewayRemoteRoom = run.query(sqlAbsGatewayRemoteRoom, new MapListHandler(), absGatewayId, hotelCode,invCode);
                } catch (SQLException e) {
                    Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, "", e);
                    return false;
                }

                // connessione DB
                Connection connAbsGateway = jdbcGatewayConnection(absGateway.get("abs_host").toString(), absGateway.get("abs_database").toString(),
                absGateway.get("abs_database_user").toString(), absGateway.get("abs_database_password").toString());

                if (connAbsGateway != null) {
                    connAbsGateway.setAutoCommit(false);

                    for (Map<String, Object> roomRecord : absGatewayRemoteRoom) {
                        
                        String remoteRoomId = roomRecord.get("abs_gateway_remote_room_id").toString();
                        PreparedStatement psSqlInsertAbsGatewayAllotment = connAbsGateway.prepareStatement(getSqlInsertAllotmentGateway());

                        // normal rate
                        fillInsertAbsGatewayAllotmentParameters(psSqlInsertAbsGatewayAllotment, remoteStructureId, curDate, "1", remoteRoomId, 0);
                        psSqlInsertAbsGatewayAllotment.execute();

                        // special rate
                        fillInsertAbsGatewayAllotmentParameters(psSqlInsertAbsGatewayAllotment, remoteStructureId, curDate, "2", remoteRoomId, 0);
                        psSqlInsertAbsGatewayAllotment.execute();
                    }
                    
                    connAbsGateway.commit();
                    connAbsGateway.close();
                } else {
                    addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Error on updating remote structure");
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    private boolean updateCMGateway(java.util.Date curDate) throws Exception {

        Vector parameters = null;
        String CM_STORAGE_METHOD = "2";

        // recupera i dati dei CM della struttura se presenti
        List<Map<String, Object>> CMGateway = null;
        String sqlCMGateway = ""
                + "SELECT user_id,channel_manager.cm_id,cm_structure_id,cm_storage_method FROM channel_manager_map_user "
                + "LEFT JOIN channel_manager ON channel_manager_map_user.cm_id = channel_manager.cm_id "
                + "WHERE structure_id = ? AND cm_status = ?";

        try {
            CMGateway = run.query(sqlCMGateway, new MapListHandler(), hotelCode, 1);
        } catch (SQLException e) {
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, "", e);
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Error on updating CM");
            return false;
        }

        if (CMGateway == null) {
            return false;
        }
        // cicla per ogni CM collegato alla struttura
        for (Map<String, Object> cm : CMGateway) {
            parameters=new Vector();
            String userId = cm.get("user_id").toString();
            String cmStructureId = cm.get("cm_structure_id").toString();
            String cmId = cm.get("cm_id").toString();
            DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
            
            try{
                System.out.println(" userId : " + userId);
                System.out.println(" cmStructureId : " + cm.get("cm_structure_id").toString());
                System.out.println(" cm_id : " + cm.get("cm_id").toString() );
                System.out.println(" curDate : " + df.format(curDate).toString() );
            }catch(Exception e){}
            
            
            parameters.add(new Integer(cmId));
            parameters.add(new Integer(hotelCode));
            parameters.add(df.format(curDate).toString());
            parameters.add(df.format(curDate).toString());
            
            try {
                System.out.println(" client.execute .... " );
                Object result = client.execute("backend.resetCMallotment", parameters);
                System.out.println(" client.execute end " );
            } catch (Exception e) {
                Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, "", e);
                addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Error on updating CM resetCMallotment");
                return false;
            }

            // scrive allotment su DB
            if (cm.get("cm_storage_method").toString().equals(CM_STORAGE_METHOD)) {
                // room
                List<Map<String, Object>> CMrooms = null;
                String sqlCMRooms = "SELECT cm_room_id FROM channel_manager_map_room WHERE cm_id = ? AND cm_structure_id = ?";

                try {
                    CMrooms = run.query(sqlCMRooms, new MapListHandler(), cmId, cmStructureId);
                } catch (Exception e) {
                    Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, "", e);
                    addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Error on updating CM");
                    return false;
                }

                // rate
                for (Map<String, Object> CMroom : CMrooms) {
                    
                    String cmRoomId = CMroom.get("cm_room_id").toString();

                    List<Map<String, Object>> CMlists = null;
                    String sqlCMLists = "SELECT cm_pricelist_id FROM channel_manager_map_pricelist WHERE cm_id = ? AND cm_room_id = ?";

                    try {
                        CMlists = run.query(sqlCMLists, new MapListHandler(), cmId, cmRoomId);
                    } catch (Exception e) {
                        Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, "", e);
                        addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Error on updating CM");
                        return false;
                    }

                    for (Map<String, Object> CMlist : CMlists) {
                        String sqlInsertCM = ""
                                + "INSERT INTO channel_manager_allotments ("
                                + "cm_id, "
                                + "cm_structure_id, "
                                + "cm_room_id, "
                                + "cm_list_id, "
                                + "allotment_date, "
                                + "allotment_number, "
                                + "allotment_modified_user) "
                                + "VALUES(?, ?, ?, ?, ?, ?, ?) "
                                + "ON DUPLICATE KEY UPDATE "
                                + "cm_id = ?, "
                                + "cm_structure_id = ?, "
                                + "cm_room_id = ?, "
                                + "cm_list_id = ?, "
                                + "allotment_date = ?, "
                                + "allotment_number = ?, "
                                + "allotment_modified_user = ?";

                        try {
                            run.update(sqlInsertCM,
                                    cmId, cmStructureId,
                                    cmRoomId,
                                    CMlist.get("cm_pricelist_id").toString(),
                                    curDate, 0, -1,
                                    cmId, cmStructureId,
                                    cmRoomId,
                                    CMlist.get("cm_pricelist_id").toString(),
                                    curDate, 0, -1);
                        } catch (SQLException e) {
                            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, "", e);
                            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Error on updating CM");
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    // Connette al database dei gateway
    private Connection jdbcGatewayConnection(String host, String dbName, String userName, String password) {
        Connection conn = null;

        if (getCrContext("cr/environment").equals("Test")) {
            host = "93.95.221.43";
        }

        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection("jdbc:mysql://" + host + ":3306/" + dbName, userName, password);
            //conn = DriverManager.getConnection("jdbc:mysql://" + host + ":3306/" + dbName + "?user=" + userName + "&password=" + password);
        } catch (Exception e) {
            Logger.getLogger(OTAHotelAvailNotifRSBuilder.class.getName()).log(Level.SEVERE, "", e);
        }

        return conn;
    }

    private boolean thereIsError() {
        return !(res.getErrors() == null || res.getErrors().getError().isEmpty());
    }

    public MessageAcknowledgementType getRes() {
        return res;
    }
    
    public static void main(String[] args) {
        System.out.println(
        "getSqlInsertAllotmentWithPara(true,true,true,true)"
        );
    }
}
