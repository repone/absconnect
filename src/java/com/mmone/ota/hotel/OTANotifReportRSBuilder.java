package com.mmone.ota.hotel;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import org.opentravel.ota._2003._05.ErrorType;
import org.opentravel.ota._2003._05.ErrorsType;
import org.opentravel.ota._2003._05.WarningsType;
import org.opentravel.ota._2003._05.WarningType;
import javax.sql.DataSource;
import javax.xml.datatype.DatatypeConfigurationException;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.xmlrpc.XmlRpcClient;
import org.opentravel.ota._2003._05.OTANotifReportRQ;
import org.opentravel.ota._2003._05.MessageAcknowledgementType;
import org.opentravel.ota._2003._05.SuccessType;

public class OTANotifReportRSBuilder  extends BaseBuilder{

    private DataSource ds;
    private OTANotifReportRQ request;
    private QueryRunner run;
    private MessageAcknowledgementType res = new MessageAcknowledgementType();
    private String langID;
    private String user;
    private String echoToken;
    private String target = Facilities.TARGET_PRODUCTION;
    private BigDecimal version = new BigDecimal(Facilities.VERSION);
    private Map<String,String> logData = new LinkedHashMap<String, String>();
    private XmlRpcClient client = null;

    public XmlRpcClient getClient() {
        return client;
    }

    public void setClient(XmlRpcClient client) {
        this.client = client;
    } 
    
    public void addError(String type, String code, String message) {
        if (res.getErrors() == null) {
            res.setErrors(new ErrorsType());
        }

        ErrorType et = new ErrorType();
        et.setCode(code);
        et.setType(type);
        et.setValue(message);

        res.getErrors().getError().add(et);
    }

    public void addWarning(String type, String code, String message) {
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
        StringBuffer msg = new StringBuffer();
        msg.append("\n****");

        for (String key : infoRequest.keySet()) {
            msg.append("- " + key + " = " + infoRequest.get(key) + " ");
        }

        Logger.getLogger(OTANotifReportRSBuilder.class.getName()).log(Level.INFO, msg.toString());
    }

    public OTANotifReportRSBuilder(DataSource ds, OTANotifReportRQ request, String user, HttpServletRequest httpRequest) {
        super();

        if (ds == null) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "No connection with the database");
        }

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

        logData.put("Class" , this.getClass().getName());
        logData.put("TimeStamp", res.getTimeStamp().toString());
        logData.put("user", user);
        logData.put("RemoteAddr", httpRequest.getRemoteAddr());
        logData.put("EchoToken", request.getEchoToken());
        logData.put("Target", target);
        
        logInfoRequest(logData);
        
        res.setTarget(target);
        res.setPrimaryLangID(langID);
        res.setVersion(version);

        run = new QueryRunner(ds);
    }

    public MessageAcknowledgementType build() {
        String hotelCode = null;
        String context_id = null;
        String token = request.getEchoToken();
        String confirmation_number = null;
        String resIdType = null;
        String resIdSourceContext = null;

        try {
            confirmation_number = request.getNotifDetails().getHotelNotifReport().getHotelReservations().getHotelReservation().get(0).getResGlobalInfo().getHotelReservationIDs().getHotelReservationID().get(0).getResIDValue();
        } catch (Exception e) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_CONFIRMATION_NUMBER, "ResID_Value is invalid");
        }

        if (confirmation_number.isEmpty()) {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_CONFIRMATION_NUMBER, "ResID_Value is null");
        }

        try {
            resIdType = request.getNotifDetails().getHotelNotifReport().getHotelReservations().getHotelReservation().get(0).getResGlobalInfo().getHotelReservationIDs().getHotelReservationID().get(0).getResIDType();
        } catch (Exception e) {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "ResID_Type is invalid");
        }

        if (!resIdType.equals(Facilities.RESID_TYPE_PMS)) { 
            if (!resIdType.equals(Facilities.RESID_TYPE_SINGLE_PMS))  
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "ResID_Type is null or invalid");
        }

        if (thereIsError()) {
            return res;
        }
        
        Logger.getLogger(OTANotifReportRSBuilder.class.getName()).log(Level.INFO, "token="+token);
        
        try {
            Map dw = ReservationDownloadServices.setReservationsAsDownloadedRpc(client,ds , token, confirmation_number,resIdType);
        } catch (Exception ex) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_INVALID_VALUE, "EchoToken is invalid");
            Logger.getLogger(OTANotifReportRSBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (!thereIsError()) {
            res.setSuccess(new SuccessType());
        }

        return res;
    }

    private boolean thereIsError() {
        return !(res.getErrors() == null || res.getErrors().getError().isEmpty());
    }

    public MessageAcknowledgementType getRes() {
        return res;
    }
}
