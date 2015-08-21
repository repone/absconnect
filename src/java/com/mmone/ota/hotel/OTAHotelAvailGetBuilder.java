/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mmone.ota.hotel;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.Duration;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.opentravel.ota._2003._05.AvailStatusMessageType;
import org.opentravel.ota._2003._05.AvailStatusMessageType.RestrictionStatus;
import org.opentravel.ota._2003._05.ErrorType;
import org.opentravel.ota._2003._05.ErrorsType;
import org.opentravel.ota._2003._05.LengthsOfStayType;
import org.opentravel.ota._2003._05.OTAHotelAvailGetRQ;
import org.opentravel.ota._2003._05.OTAHotelAvailGetRS;
import org.opentravel.ota._2003._05.RatePlanCandidatesType;
import org.opentravel.ota._2003._05.StatusApplicationControlType;
import org.opentravel.ota._2003._05.SuccessType;
import org.opentravel.ota._2003._05.TimeUnitType;
import org.opentravel.ota._2003._05.WarningType;
import org.opentravel.ota._2003._05.WarningsType;

/**
 *
 * @author umberto.zanatta
 */
public class OTAHotelAvailGetBuilder extends BaseBuilder{

    private DataSource ds;
    private QueryRunner run;
    private OTAHotelAvailGetRQ request;
    private OTAHotelAvailGetRS res = new OTAHotelAvailGetRS();
    private String user;
    private String langID;
    private String echoToken;
    private String target = Facilities.TARGET_PRODUCTION;
    private String requestorID;
    private BigDecimal version = new BigDecimal(Facilities.VERSION);
    String hotelCode = null;
    Integer ihotelCode = null;
    boolean sendAllRestrictions = false;
    boolean sendAllLengthsOfStay = false;
   
    private Map<String, String> logData = new LinkedHashMap<String, String>();
    
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

    private void logInfoRequest(Map<String,String> infoRequest){
        StringBuilder msg = new StringBuilder();
        msg.append("\n****");

        for (String key : infoRequest.keySet()) {
            msg.append("- ").append(key).append(" = ").append(infoRequest.get(key)).append(" ");
        }

        Logger.getLogger(OTAHotelAvailGetBuilder.class.getName()).log(Level.INFO, msg.toString());
    }

    public Date getDate(String sDate, boolean isStart) {
        Date dDate = null;
        if (StringUtils.isEmpty(sDate)) {
            if (isStart) {
                addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_START_DATE_INVALID, "Start date null");
            } else {
                addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_END_DATE_INVALID, "End date null");
            }
        } else {
            try {
                dDate = new Date(DateUtils.parseDate(sDate, com.mmone.ota.hotel.Facilities.dateParsers).getTime());
            } catch (Exception e) {
                addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, (isStart) ? "Start" : "End" + " date invalid: " + e.getMessage());
            }
        }
        return dDate;
    }
    
    private boolean needBooking = false;
    public OTAHotelAvailGetBuilder(DataSource ds, OTAHotelAvailGetRQ request, String user, HttpServletRequest httpRequest,boolean needBooking) {
        super();

        if (ds == null) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "No connection with the database");
        }
        this.needBooking= needBooking;
        this.request = request;
        this.langID = request.getPrimaryLangID();
        this.user = user;
        this.ds = ds;
        this.echoToken = request.getEchoToken();
        this.target = request.getTarget();
        this.version = request.getVersion();

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

        res.setEchoToken(echoToken);
        res.setTarget(target);
        res.setPrimaryLangID(langID);
        res.setVersion(version);

        try {
            res.setTimeStamp(com.mmone.ota.rpc.Facilities.getToday());
        } catch (DatatypeConfigurationException ex) {
        }

        logData.put("Class" , this.getClass().getName());
        logData.put("TimeStamp", res.getTimeStamp().toString());
        logData.put("user", user);
        logData.put("RemoteAddr", httpRequest.getRemoteAddr());
        logData.put("EchoToken", echoToken);
        logData.put("Target", this.request.getTarget());

        run = new QueryRunner(ds);
    }

    public OTAHotelAvailGetRS build(String requestorID) throws Exception {

        List<OTAHotelAvailGetRQ.HotelAvailRequests.HotelAvailRequest> hotelAvailRequests = request.getHotelAvailRequests().getHotelAvailRequest();

        this.requestorID = requestorID;
        
        if (hotelAvailRequests != null) {
            if (hotelAvailRequests.size() == 1) {
                try {
                    hotelCode = hotelAvailRequests.get(0).getHotelRef().getHotelCode();
                    ihotelCode=new Integer(hotelCode);
                } catch (Exception e) {
            
                }
            } else {
                addError( Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_HOTEL_CODE, "Only a HotelAvailRequest is permitted");
                return res;
            }
        }

        if (hotelCode == null ) {
            addError( Facilities.EWT_UNKNOWN, Facilities.ERR_INVALID_HOTEL_CODE, "@HotelCode is null");
            return res;
        }

        
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
            int prodEnv = p & Facilities.PRODUCTION_ENVIRONMENT;

            if (request.getTarget().equals("Production") && prodEnv == 0) {
                addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Client. Authorization Error: You don't have permission to select the production environment");
            }
        }

        if(thereIsError()) return res;

        logData.put("HotelCode", hotelCode);

        OTAHotelAvailGetRQ.HotelAvailRequests.HotelAvailRequest hotelAvailRequest = hotelAvailRequests.get(0);

        // controllo DateRange
        Date dStartDate = null;
        Date dEndDate = null;

        try {
            OTAHotelAvailGetRQ.HotelAvailRequests.HotelAvailRequest.DateRange dateRange = hotelAvailRequest.getDateRange();
            dStartDate = getDate(dateRange.getStart(), true);
            dEndDate = getDate(dateRange.getEnd(), false);
            logData.put("Start", dateRange.getStart().toString());
            logData.put("End", dateRange.getEnd().toString());
        } catch (Exception e) {
            addError( Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_DATE, "DateRange is invalid");
        }

        if (dStartDate != null && dEndDate != null) {
            if (dStartDate.getTime() > dEndDate.getTime()) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_END_DATE_INVALID, "Start date greater than end date");
            }
        }

        logInfoRequest(logData);
        
        if (thereIsError()) return res;

        OTAHotelAvailGetRQ.HotelAvailRequests.HotelAvailRequest.RestrictionStatusCandidates restrictionStatusCandidates = hotelAvailRequest.getRestrictionStatusCandidates();
        if (restrictionStatusCandidates != null) {
            if (restrictionStatusCandidates.isSendAllRestrictions()) {
                sendAllRestrictions = true;
            }
        }

        OTAHotelAvailGetRQ.HotelAvailRequests.HotelAvailRequest.LengthsOfStayCandidates lengthsOfStayCandidates = hotelAvailRequest.getLengthsOfStayCandidates();
        if (lengthsOfStayCandidates != null) {
            if (lengthsOfStayCandidates.isSendAllLengthsOfStay()) {
                sendAllLengthsOfStay = true;
            }
        }
            
        // inizio analisi
        List<RatePlanCandidatesType.RatePlanCandidate> ratePlanCandidates = hotelAvailRequest.getRatePlanCandidates().getRatePlanCandidate();

        if (ratePlanCandidates == null) {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_RATE_CODE, "RatePlanCanidates is null");
            return res;
        }

        if (ratePlanCandidates.isEmpty()) {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_RATE_CODE, "RatePlanCanidates is null");
            return res;
        }

        res.setAvailStatusMessages(new OTAHotelAvailGetRS.AvailStatusMessages());
        res.getAvailStatusMessages().setHotelCode(hotelCode);
       
        for (RatePlanCandidatesType.RatePlanCandidate ratePlanCandidate : ratePlanCandidates) {
            String ratePlanCode = null;
            int listId = 0;
            
            try {
                ratePlanCode = ratePlanCandidate.getRatePlanCode();
            } catch (Exception e) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_RATE_CODE, "@RatePlanCode is null");
                return res;
            }

            if (!ratePlanCode.equals(Facilities.NORMAL_RATE) && !ratePlanCode.equals(Facilities.SPECIAL_RATE)) {
                //addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_RATE_CODE, "@RatePlanCode is invalid");
                //return res;
            }

            if (ratePlanCode.equals(Facilities.NORMAL_RATE)) {
                listId = 1;
            } else if (ratePlanCode.equals(Facilities.SPECIAL_RATE)) {
                listId = 2;
            }else{
                
                //if(this.needBooking){
                if(true){
                    String sqlMl = "select multirate_id from multirate where multirate_code=? and structure_id=?";
                    try {
                        listId = (Integer)run.query(sqlMl, new ScalarHandler("multirate_id"), ratePlanCode, ihotelCode); 
                    } catch (Exception e) {
                        addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_REQUIRED_FIELD_MISSING, "@RatePlanCode null or invalid");
                        return res;
                    }
                }else{
                    addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_REQUIRED_FIELD_MISSING, "@RatePlanCode null or invalid");
                    return res;
                }     
            }
            
            // inizio analisi inventario
            List<OTAHotelAvailGetRQ.HotelAvailRequests.HotelAvailRequest.RoomTypeCandidates.RoomTypeCandidate> roomTypeCandidates = hotelAvailRequest.getRoomTypeCandidates().getRoomTypeCandidate();
            
            if (roomTypeCandidates == null) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_ROOM_OR_UNIT_TYPE_INVALID, "RoomTypeCandidates is null");
                return res;
            }

            if (roomTypeCandidates.isEmpty()) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_ROOM_OR_UNIT_TYPE_INVALID, "RatePlanCanidates is invalid");
                return res;
            }

            for (OTAHotelAvailGetRQ.HotelAvailRequests.HotelAvailRequest.RoomTypeCandidates.RoomTypeCandidate roomTypeCandidate : roomTypeCandidates) {
                String roomTypeCode = null;

                try {
                    roomTypeCode = roomTypeCandidate.getRoomTypeCode();
                } catch (Exception e) {
                    addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_ROOM_OR_UNIT_TYPE_INVALID, "@RoomTypeCode is invalid");
                    return res;
                }

                String SQL_SELECT_ROOM_BY_STRUCTURE_ID = ""
                    +"SELECT room_id FROM room"
                    +" WHERE structure_id = ? AND room_code = ? AND room_status = 1";

                Map room = null;

                try {
                    room = run.query(SQL_SELECT_ROOM_BY_STRUCTURE_ID,
                            new MapHandler(),
                            hotelCode,
                            roomTypeCode);
                    
                } catch (SQLException e) {
                    Logger.getLogger(OTAHotelAvailGetBuilder.class.getName()).log(Level.SEVERE, null, e);
                }

                if (room == null) {
                    addError(Facilities.EWT_UNKNOWN, Facilities.ERR_ROOM_OR_UNIT_TYPE_INVALID, "@RoomTypeCode is invalid");
                    return res;
                }

                Integer roomId = new Integer(room.get("room_id").toString());

                String SQL_SELECT_ALLOTMENT_BY_STRUCTURE_ID_ROOM_ID_THEDATE = ""
                        + "SELECT availability,minstay,therelease,thedate,lock_checkin,lock_checkout FROM allotment"
                        + " WHERE structure_id = ? AND room_id = ? AND list_id = ? AND thedate >= ? AND thedate <= ?";

                List<Map<String,Object>> allotments =  null;

                try {
                    allotments = run.query(
                        SQL_SELECT_ALLOTMENT_BY_STRUCTURE_ID_ROOM_ID_THEDATE,
                        new MapListHandler()  ,
                        hotelCode,
                        roomId,
                        listId,
                        dStartDate,
                        dEndDate);
                } catch (Exception e) {
                    addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "System error. Please contact the administrator");
                    return res;
                }

               for (Map<String, Object> allotment : allotments) {
                   AvailStatusMessageType availStatusMessage = new AvailStatusMessageType();

                   availStatusMessage.setBookingLimit(new BigInteger(allotment.get("availability").toString()));

                   StatusApplicationControlType statusApplicationControlType = new StatusApplicationControlType();

                   statusApplicationControlType.setStart(allotment.get("thedate").toString());
                   statusApplicationControlType.setEnd(allotment.get("thedate").toString());
                   statusApplicationControlType.setRatePlanCode(ratePlanCode);
                   statusApplicationControlType.setInvCode(roomTypeCode);

                   if (sendAllLengthsOfStay) { //minstay
                        LengthsOfStayType lengthsOfStay = new LengthsOfStayType();

                        LengthsOfStayType.LengthOfStay lengthOfStay = new LengthsOfStayType.LengthOfStay();
                        lengthOfStay.setMinMaxMessageType("MinLOS");
                        lengthOfStay.setTime(new BigInteger(allotment.get("minstay").toString()));
                        lengthOfStay.setTimeUnit(TimeUnitType.DAY);

                        lengthsOfStay.getLengthOfStay().add(lengthOfStay);

                        availStatusMessage.setLengthsOfStay(lengthsOfStay);
                   }
                   
                   availStatusMessage.setStatusApplicationControl(statusApplicationControlType);

                   if (sendAllRestrictions) {
                        RestrictionStatus restrictionStatus = new RestrictionStatus();

                        javax.xml.datatype.DatatypeFactory factory = javax.xml.datatype.DatatypeFactory.newInstance();
                        Duration theRelease = factory.newDurationDayTime("P" + allotment.get("therelease").toString() + "D");
     
                        restrictionStatus.setMinAdvancedBookingOffset(theRelease);
                        availStatusMessage.setRestrictionStatus(restrictionStatus);
                        res.getAvailStatusMessages().getAvailStatusMessage().add(availStatusMessage);

                        for (int i = 0; i < 2; i++) {
                            RestrictionStatus _restrictionStatus = new RestrictionStatus();
                            AvailStatusMessageType _availStatusMessage = new AvailStatusMessageType();

                            _availStatusMessage.setBookingLimit(new BigInteger(allotment.get("availability").toString()));

                            StatusApplicationControlType _statusApplicationControlType = new StatusApplicationControlType();

                            _statusApplicationControlType.setStart(allotment.get("thedate").toString());
                            _statusApplicationControlType.setEnd(allotment.get("thedate").toString());
                            _statusApplicationControlType.setRatePlanCode(ratePlanCode);
                            _statusApplicationControlType.setInvCode(roomTypeCode);

                            if (i == 0) {
                                _restrictionStatus.getRestriction().add("Arrival");
                                Integer status = new Integer(allotment.get("lock_checkin").toString());
                                if (status == 1)
                                    _restrictionStatus.getStatus().add("Close");
                                else
                                    _restrictionStatus.getStatus().add("Open");
                            } else {
                                _restrictionStatus.getRestriction().add("Departure");
                                Integer status = new Integer(allotment.get("lock_checkout").toString());
                                if (status == 1)
                                    _restrictionStatus.getStatus().add("Close");
                                else
                                    _restrictionStatus.getStatus().add("Open");
                            }

                            _availStatusMessage.setStatusApplicationControl(_statusApplicationControlType);
                            _availStatusMessage.setRestrictionStatus(_restrictionStatus);
                            res.getAvailStatusMessages().getAvailStatusMessage().add(_availStatusMessage);
                       }
                   } else {
                        res.getAvailStatusMessages().getAvailStatusMessage().add(availStatusMessage);
                   }
                   
               }

            }

        }
        
        if(!thereIsError()) {
            res.setSuccess(new SuccessType());
        }
        
        return res;
    }
    
    public OTAHotelAvailGetRS getRes() {
        return res;
    }

    private boolean thereIsError() {
        return !(res.getErrors() == null || res.getErrors().getError().isEmpty());
    }
}
