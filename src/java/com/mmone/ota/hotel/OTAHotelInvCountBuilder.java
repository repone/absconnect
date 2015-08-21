package com.mmone.ota.hotel;

import java.math.BigDecimal;
import org.apache.commons.lang.StringUtils;
import org.opentravel.ota._2003._05.ErrorType;
import org.opentravel.ota._2003._05.ErrorsType;
import org.opentravel.ota._2003._05.WarningsType;
import org.opentravel.ota._2003._05.WarningType;
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
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.opentravel.ota._2003._05.BaseInvCountType;
import org.opentravel.ota._2003._05.InvCountType;
import org.opentravel.ota._2003._05.OTAHotelInvCountRQ;
import org.opentravel.ota._2003._05.OTAHotelInvCountRS;
import org.opentravel.ota._2003._05.StatusApplicationControlType;
import org.opentravel.ota._2003._05.SuccessType;

// TODO: Auto-generated Javadoc
/**
 * The Class OtaHotelRatePlanBuilder.
 */
public class OTAHotelInvCountBuilder  extends BaseBuilder{ 
    public static final String RATE_PLAN_CODE_IU = "IU";
    private DataSource ds;
    private OTAHotelInvCountRQ request;
    private QueryRunner run;
    private OTAHotelInvCountRS res = new OTAHotelInvCountRS();
    private String user;
    private String langID;
    private String target = Facilities.TARGET_PRODUCTION;
    private String echoToken;
    private String requestorID;
    private BigDecimal version = new BigDecimal(Facilities.VERSION);
    String hotelCode = null;
    Integer ihotelCode = null; 
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

    private void logInfoRequest(Map<String, String> infoRequest) {
        StringBuilder msg = new StringBuilder();
        msg.append("\n****");

        for (String key : infoRequest.keySet()) {
            msg.append("- ").append(key).append(" = ").append(infoRequest.get(key)).append(" ");
        }

        Logger.getLogger(OTAHotelInvCountBuilder.class.getName()).log(Level.INFO, msg.toString());
    }

    /**
     * Instantiates a new OTA hotel rate amount notif rs builder.
     *
     * @param ds the ds
     * @param request the request
     */
    public OTAHotelInvCountBuilder(DataSource ds, OTAHotelInvCountRQ request, String user, HttpServletRequest httpRequest) {
        super();

        if (ds == null) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "No connection with the database");
        }

        this.request = request;
        this.langID = request.getPrimaryLangID();
        this.user = user;
        this.ds = ds;
        this.target = request.getTarget();
        this.version = request.getVersion();
        this.echoToken = request.getEchoToken();

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

        res.setEchoToken(echoToken);

        res.setTarget(target);
        res.setPrimaryLangID(langID);
        res.setVersion(version);

        logData.put("Class" , this.getClass().getName());
        logData.put("TimeStamp", res.getTimeStamp().toString());
        logData.put("user", user);
        logData.put("RemoteAddr", httpRequest.getRemoteAddr());
        logData.put("EchoToken", echoToken);
        logData.put("Target", this.request.getTarget());        
        
        run = new QueryRunner(ds);
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
    //tabella inventory


    /*
    OTA_HotelInvCountRS/
    Inventories/
    @HotelCode

    Inventories/
    Inventory[
    StatusApplicationControl
    @Start
    @End
    @InvCode
    @IsRoom
    @RatePlanCode
    InvCounts
    InvCount[
    InvCount
    @Count
    ]
    ]



     */
    public OTAHotelInvCountRS build(String requestorID) throws Exception {
        //OTA_HotelInvCountRQ/HotelInvCountRequests/HotelInvCountRequest

        this.requestorID = requestorID;
        // server un try catch per controllare
        List<OTAHotelInvCountRQ.HotelInvCountRequests.HotelInvCountRequest> lHotelInvCountRequest = request.getHotelInvCountRequests().getHotelInvCountRequest();

        String refHotel = null;
        String invCode = null;

        res.setInventories(new InvCountType());

        String SQL_SELECT_INVENTORY_BY_ROOMID_AND_DATE = ""
                + " SELECT room_code,inventory.* FROM inventory"
                + " left join room on inventory.room_id=room.room_id"
                + " WHERE inventory_type_id=1 AND inventory.room_id=? AND allotment_date BETWEEN ? AND ?"
                + " ";

        // per ogni request
        for (OTAHotelInvCountRQ.HotelInvCountRequests.HotelInvCountRequest hotelInvCountRequest : lHotelInvCountRequest) {

            OTAHotelInvCountRQ.HotelInvCountRequests.HotelInvCountRequest.DateRange dateRange = hotelInvCountRequest.getDateRange();

            hotelCode = hotelInvCountRequest.getHotelRef().getHotelCode();

            if (refHotel == null) {
                refHotel = hotelCode;
            }

            Date dStartDate = getDate(dateRange.getStart(), true);
            Date dEndDate = getDate(dateRange.getEnd(), false);

            if (dStartDate != null && dEndDate != null) {
                if (dStartDate.getTime() > dEndDate.getTime()) {
                    addError(Facilities.EWT_UNKNOWN, Facilities.ERR_END_DATE_INVALID, "Start date greater than end date");
                }
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
                //int testEnv = p & Facilities.TEST_ENVIRONMENT;
                int prodEnv = p & Facilities.PRODUCTION_ENVIRONMENT;

                if (request.getTarget().equals("Production") && prodEnv == 0) {
                    addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Client. Authorization Error: You don't have permission to select the production environment");
                }
            }

            if (hotelCode == null) {
                addError(Facilities.EWT_UNKNOWN, Facilities.ERR_INVALID_HOTEL_CODE, "Hotel code is null");
            } else if (!refHotel.equals(hotelCode)) {
                addError(Facilities.EWT_UNKNOWN, Facilities.ERR_INVALID_HOTEL_CODE, "Only one hotel code is allowed");
            }

            logData.put("HotelCode", hotelCode);

            logInfoRequest(logData);

            if (thereIsError()) {
                return res;
            }

            //eseguo per ogni richiesta
            List<BaseInvCountType> lInventory = res.getInventories().getInventory();

            List<OTAHotelInvCountRQ.HotelInvCountRequests.HotelInvCountRequest.RoomTypeCandidates.RoomTypeCandidate> lRoomTypeCandidates = hotelInvCountRequest.getRoomTypeCandidates().getRoomTypeCandidate();

            for (OTAHotelInvCountRQ.HotelInvCountRequests.HotelInvCountRequest.RoomTypeCandidates.RoomTypeCandidate roomTypeCandidate : lRoomTypeCandidates) {

                String roomTypeCode = roomTypeCandidate.getRoomTypeCode();

                String sqlChkRoom = "SELECT room_id FROM room WHERE room_code=? AND structure_id=? ";

                try {
                    invCode = run.query(sqlChkRoom, new ScalarHandler("room_id"), roomTypeCode, hotelCode).toString();
                } catch (Exception ex) {
                    Logger.getLogger(OTAHotelInvCountBuilder.class.getName()).log(Level.SEVERE, null, ex);
                    addError(Facilities.EWT_UNKNOWN, Facilities.ERR_ROOM_UNIT_CODE_INCORRECT, "Room " + roomTypeCode + " not in the structure");
                }

                if (thereIsError()) {
                    return res;
                }

                List<Map<String, Object>> res = run.query(
                        SQL_SELECT_INVENTORY_BY_ROOMID_AND_DATE,
                        new MapListHandler(),
                        invCode,
                        dStartDate,
                        dEndDate);

                for (Map<String, Object> record : res) {
                    BaseInvCountType baseInvCountType = new BaseInvCountType();
                    String sCurData = DateFormatUtils.format((Date) record.get("allotment_date"), "yyyy-MM-dd");

                    baseInvCountType.setStatusApplicationControl(new StatusApplicationControlType());
                    baseInvCountType.getStatusApplicationControl().setStart(sCurData);
                    baseInvCountType.getStatusApplicationControl().setEnd(sCurData);

                    baseInvCountType.getStatusApplicationControl().setIsRoom(true);
                    baseInvCountType.getStatusApplicationControl().setRatePlanCode(RATE_PLAN_CODE_IU);

                    baseInvCountType.getStatusApplicationControl().setInvCode(roomTypeCode);
                    String sInvCount = "0";
                    if (record.get("allotment_number") != null) {
                        sInvCount = record.get("allotment_number").toString();
                    }

                    baseInvCountType.setInvCounts(new BaseInvCountType.InvCounts());
                    BaseInvCountType.InvCounts.InvCount _invCount = new BaseInvCountType.InvCounts.InvCount();
                    baseInvCountType.getInvCounts().getInvCount().add(_invCount);
                    baseInvCountType.getInvCounts().getInvCount().get(0).setCount(new BigInteger(sInvCount));

                    lInventory.add(baseInvCountType);
                }

            }

            refHotel = null;
        }

        res.getInventories().setHotelCode(refHotel);

        if (!thereIsError()) {
            res.setSuccess(new SuccessType());
        }

        return res;
    }

    public OTAHotelInvCountRS getRes() {
        return res;
    }

    private boolean thereIsError() {
        return !(res.getErrors() == null || res.getErrors().getError().isEmpty());
    } 
    
    public static void main(String[] args) {
        System.out.println("ssss");
    }
}
