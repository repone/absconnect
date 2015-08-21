/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mmone.ota.hotel;

import java.math.BigDecimal;
import org.opentravel.ota._2003._05.ErrorType;
import org.opentravel.ota._2003._05.ErrorsType;
import org.opentravel.ota._2003._05.WarningsType;
import org.opentravel.ota._2003._05.WarningType;
import java.sql.SQLException;
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
import org.opentravel.ota._2003._05.HotelRoomListType;
import org.opentravel.ota._2003._05.OTAHotelRoomListRQ;
import org.opentravel.ota._2003._05.OTAHotelRoomListRS;
import org.opentravel.ota._2003._05.ParagraphType;
import org.opentravel.ota._2003._05.RatePlanType;
import org.opentravel.ota._2003._05.RoomTypeType;
import org.opentravel.ota._2003._05.SuccessType;

/**
 *
 * @author umberto.zanatta
 */
public class OTAHotelRoomListBuilder  extends BaseBuilder{
    private DataSource ds;
    private OTAHotelRoomListRQ request;
    private OTAHotelRoomListRS res = new OTAHotelRoomListRS();

    private QueryRunner run;
    private String user;
    private String langID;
    private String echoToken;
    private String requestorID;
    private String target = Facilities.TARGET_PRODUCTION;
    private BigDecimal version = new BigDecimal(Facilities.VERSION);
    
    private Map<String,String> logData = new LinkedHashMap<String, String>();
     
    String hotelCode = null;
    String ratePlanCode = null;
    Integer ihotelCode = null;

    public final void addError(String type, String code, String message){
        if (res.getErrors()==null) res.setErrors(new ErrorsType());

        ErrorType et = new ErrorType();
        et.setCode(code);
        et.setType(type);
        et.setValue(message);

        res.getErrors().getError().add(et);
    }

    public final void addWarning(String type, String code, String message){
        if (res.getWarnings()==null) res.setWarnings(new WarningsType());

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

        Logger.getLogger(OTAHotelRoomListBuilder.class.getName()).log(Level.INFO, msg.toString());
    }
    
    public OTAHotelRoomListBuilder(DataSource ds, OTAHotelRoomListRQ request, String user, HttpServletRequest httpRequest) {
        super();

        //this.httpRequest = httpRequest;
        //this.context = context;
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
        logData.put("EchoToken", request.getEchoToken());
        logData.put("Target", target);
        
        if (ds == null) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "No connection with the database");
        }

        run = new QueryRunner(ds);
    }

    public OTAHotelRoomListRS build(String requestorID) throws Exception {
        this.requestorID = requestorID;
        setHotelCode();
        setRatePlan();
        
        logData.put("HotelCode", hotelCode);
        logData.put("requestorID", requestorID);
         
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
        
        if (thereIsError())  {
            logInfoRequest(logData);
            return res;
        }

        if (!ratePlanCode.equals("IU") && ratePlanCode != null) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_INVALID_RATE_CODE, "RatePlanCode is invalid");
        }

        if (thereIsError()) {
            logInfoRequest(logData);    
            return res;
        }
        
        String SQL_SELECT_ROOM_BY_STRUCTURE_ID = "" 
                +" SELECT room_name,room_code,room_min_pax,room_max_pax FROM room "
                +" LEFT JOIN room_details ON room.room_id = room_details.room_id AND room_details.language='IT'  "
                +" WHERE structure_id = ? AND room_status = 1";

        List<Map<String,Object>> rows = null;

        try {
            rows =  run.query(
                SQL_SELECT_ROOM_BY_STRUCTURE_ID,
                new MapListHandler(),
                hotelCode
            );
        } catch (Exception e) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "System error. Please contact the administrator");
        }

        if(thereIsError()) return res;
       
        OTAHotelRoomListRS.HotelRoomLists roomLists = new OTAHotelRoomListRS.HotelRoomLists();
        HotelRoomListType hotelRoomListType = new HotelRoomListType();
        HotelRoomListType.RoomStays roomStays = new HotelRoomListType.RoomStays();
        HotelRoomListType.RoomStays.RoomStay roomStay = new HotelRoomListType.RoomStays.RoomStay();
        HotelRoomListType.RoomStays.RoomStay.RoomTypes roomTypes = new HotelRoomListType.RoomStays.RoomStay.RoomTypes();

        hotelRoomListType.setHotelCode(hotelCode);
        // cicla tra i risultati
        for (Map<String, Object> record : rows) {
            RoomTypeType roomType = new RoomTypeType();

            String roomCode = record.get("room_code").toString();
            ParagraphType roomDescription = new ParagraphType();
            roomDescription.setLanguage("IT");
            roomDescription.setName( (String)record.get("room_name"));
             
            //System.out.println("RoomCode: " + roomCode);

            roomType.setInvBlockCode(roomCode);
            
            roomType.setRoomDescription(roomDescription);
            roomTypes.getRoomType().add(roomType);
        }

        roomStay.setRoomTypes(roomTypes);
        roomStays.getRoomStay().add(roomStay);
        hotelRoomListType.setRoomStays(roomStays);
        roomLists.getHotelRoomList().add(hotelRoomListType);
        res.setHotelRoomLists(roomLists);
 

        logInfoRequest(logData);
        
        if(!thereIsError()) {
            res.setSuccess(new SuccessType());
        }
        
        return res;
    }

    private void setHotelCode () {
        HotelRoomListType hotelRoomList = new HotelRoomListType();

        try {
            hotelRoomList = request.getHotelRoomLists().getHotelRoomList().get(0);
            hotelCode = hotelRoomList.getHotelCode();
        } catch (Exception e) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_INVALID_RATE_CODE, "HotelCode is null");
        }
    }

    private void setRatePlan() {
        RatePlanType ratePlanList = new RatePlanType();

        try {
            ratePlanList = request.getHotelRoomLists().getHotelRoomList().get(0).getRoomStays().getRoomStay().get(0).getRatePlans().getRatePlan().get(0);
            ratePlanCode = ratePlanList.getRatePlanCode();
        } catch (Exception e) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_INVALID_RATE_CODE, "RatePlanCode is null");
        }
    }
    
    public OTAHotelRoomListRS getRes() {
            return res;
    }

    private boolean thereIsError(){
        return !(res.getErrors()==null || res.getErrors().getError().isEmpty());
    }
}
