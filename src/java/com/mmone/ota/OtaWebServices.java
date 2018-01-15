package com.mmone.ota;

import com.mmone.ota.hotel.Facilities;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.annotation.Resource;
import org.apache.xmlrpc.XmlRpcClient;
import com.mmone.ota.hotel.OTAHotelAvailNotifRSBuilder;
import com.mmone.ota.hotel.OTAHotelDescriptiveInfoBuilder;
import com.mmone.ota.hotel.OTAHotelInvCountBuilder;
import com.mmone.ota.hotel.OTAHotelRoomListBuilder;
import com.mmone.ota.hotel.OTAHotelRateAmountNotifRSBuilder;
import com.mmone.ota.hotel.OTANotifReportRSBuilder;
import com.mmone.ota.hotel.OTAResRetrieveRSBuilder;
import com.mmone.ota.hotel.OTAHotelRatePlanBuilder;
import com.mmone.ota.hotel.OTAHotelAvailGetBuilder;
import com.mmone.ota.hotel.OTAHotelResNotifBuilder;
import com.mmone.ota.hotel.OTAProfileCreateBuilder;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.handler.MessageContext;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.opentravel.ota._2003._05.MessageAcknowledgementType;
import org.opentravel.ota._2003._05.OTAHotelAvailGetRQ;
import org.opentravel.ota._2003._05.OTAHotelAvailGetRS;
import org.opentravel.ota._2003._05.OTAHotelAvailNotifRQ;
import org.opentravel.ota._2003._05.OTAHotelDescriptiveInfoRQ;
import org.opentravel.ota._2003._05.OTAHotelRoomListRS;
import org.opentravel.ota._2003._05.OTAHotelDescriptiveInfoRS;
import org.opentravel.ota._2003._05.OTAHotelInvCountRQ;
import org.opentravel.ota._2003._05.OTAHotelInvCountRS;
import org.opentravel.ota._2003._05.OTAHotelRateAmountNotifRQ;
import org.opentravel.ota._2003._05.OTAHotelRatePlanRQ;
import org.opentravel.ota._2003._05.OTAHotelRatePlanRS;
import org.opentravel.ota._2003._05.OTAHotelResNotifRQ;
import org.opentravel.ota._2003._05.OTAHotelResNotifRS;
import org.opentravel.ota._2003._05.OTAHotelRoomListRQ;
import org.opentravel.ota._2003._05.OTANotifReportRQ;
import org.opentravel.ota._2003._05.OTAProfileCreateRQ;
import org.opentravel.ota._2003._05.OTAProfileCreateRS;
import org.opentravel.ota._2003._05.OTAReadRQ;
import org.opentravel.ota._2003._05.OTAResRetrieveRS;

/**
 *
 * @author umberto.zanatta
 *  
 *  Versioni : version/AbsConnectService/number
 *              100 : correzione per guestCount
 */

@WebService(
        serviceName = "AbsConnectService", 
        portName = "AbsConnectServicePort", 
        endpointInterface = "https.webservices_abs_one_com.absconnectservice._1.AbsConnectPort", targetNamespace = "https://webservices.abs-one.com/AbsConnectService/1.0", 
        wsdlLocation = "WEB-INF/wsdl/OtaWebServices/OTA_AbsConnect.wsdl")
public class OtaWebServices {
    public static final String CONNECTION_NEEDED = "Booking permission needed";
    public static final String TARGET_PRODUCTION = "Production";
    public static final String TARGET_TEST = "Test";
    private InitialContext ctx = null;
    @Resource
    private WebServiceContext wsc;
    private InitialContext getContext() throws NamingException {
        if (ctx == null) {
            ctx = new InitialContext();
        }
          
        return ctx;    
    }
    private HttpServletRequest getRequest(WebServiceContext wsc) {
        MessageContext msgCtxt = wsc.getMessageContext();
        HttpServletRequest req = (HttpServletRequest) msgCtxt.get(MessageContext.SERVLET_REQUEST);

        return req;
    }     
    private DataSource getDataSource(String target, String requestorID) {
        /** Carica la connessione jdbc/abs su GlassFish **/
        DataSource ds = null;
        try {
            if (target.equals(TARGET_PRODUCTION)) {
                ds = (DataSource) getContext().lookup("jdbc/" + requestorID);
                Logger.getLogger(OtaWebServices.class.getName()).log(Level.INFO,  " jdbc : " + requestorID );
                 
            } else if (target.equals(TARGET_TEST)) {
                ds = (DataSource) getContext().lookup("jdbc/test");
                Logger.getLogger(OtaWebServices.class.getName()).log(Level.INFO,  " jdbc : test"  );
            } else {
                ds = (DataSource) getContext().lookup("jdbc/test");
                Logger.getLogger(OtaWebServices.class.getName()).log(Level.INFO,  " jdbc : test"  );
            }
        } catch (NamingException ex) {
            Logger.getLogger(OtaWebServices.class.getName()).log(Level.INFO, "Error loading datasource from server");
            Logger.getLogger(OtaWebServices.class.getName()).log(Level.SEVERE, null, ex);
        }

        return ds;
    }
    
    private static int getPortalCode (String user,InitialContext initialContext) {
        int portalCode = Facilities.DEFAULTS_PORTAL_CODE; 
        try {
            String[] aUser = user.split("@");
            if(aUser.length>1){
                String jndiName = "portal/"+aUser[1];
                portalCode=(Integer) initialContext.lookup( jndiName );
            }
        } catch ( Exception ex) {
            portalCode = Facilities.DEFAULTS_PORTAL_CODE; 
        }

        return portalCode;
    }    
    private Boolean needBooking(String target, String requestorID){ 
        Boolean autNeedBooking = null;
        if (target.equals(TARGET_PRODUCTION)) {
            try {
                System.out.println(" checking = " + "cr/aut" + requestorID + "needbooking"  );
                autNeedBooking = (Boolean) getContext().lookup("cr/aut" + requestorID + "needbooking");
            } catch (NamingException ex) {
                Logger.getLogger(OtaWebServices.class.getName()).log(Level.SEVERE,ex.getMessage());
            }
        } else if (target.equals(TARGET_TEST)) {
            try {
                System.out.println(" checking = " + "cr/auttestneedbooking"  );
                autNeedBooking = (Boolean) getContext().lookup("cr/auttestneedbooking");
            } catch (NamingException ex) {
                Logger.getLogger(OtaWebServices.class.getName()).log(Level.SEVERE, ex.getMessage());
            }
        } else {
            try {
                System.out.println(" checking (default) = " + "cr/auttestneedbooking "  );
                autNeedBooking = (Boolean) getContext().lookup("cr/auttestneedbooking");
            } catch (NamingException ex) {
                Logger.getLogger(OtaWebServices.class.getName()).log(Level.SEVERE,   ex.getMessage());
            }
        }
        
        System.out.println(" autNeedBooking = " + autNeedBooking  );
        if(autNeedBooking==null) autNeedBooking=false;
        System.out.println(" autNeedBooking (after false) = " + autNeedBooking  );
        
        return autNeedBooking;
    }
    private Boolean userIsBookingAllowed(String target, String requestorID){
        Boolean checkBookingLogin = needBooking(target,   requestorID);
        return userIsBookingAllowed(target,requestorID,checkBookingLogin);
    }
    private Boolean userIsBookingAllowed(String target, String requestorID,Boolean needBooking){
        Boolean checkBookingLogin = needBooking ;
        //if(1==1)return true;
        //non richiede autenticazione booking può proseguire
        if(!checkBookingLogin) return true;
        //richiede autenticazione booking 
        DataSource ds = getDataSource(  target,   requestorID);
        QueryRunner run = new QueryRunner(ds);
        String sqlChkUser = "SELECT user_status FROM user WHERE user_username=? ";
        
        Integer user_status = null;
        System.out.println(" Checking booking ....... " );
        try {
            user_status = (Integer)run.query(sqlChkUser, new ScalarHandler("user_status"), requestorID ) ;
            
        } catch (SQLException ex) {
            Logger.getLogger(OtaWebServices.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println(" user_status = " + user_status  );
        if(user_status==null) user_status=0;
        System.out.println(" user_status (default) = " + user_status  );
         
        return (user_status==1);
    }
    private XmlRpcClient getRpcClient(String target, String requestorID) throws Exception {
        String rpcServerUrl = null;
        String rpcUsername = null;
        String rpcPassword = null;
        XmlRpcClient client = null;

        if (target.equals(TARGET_PRODUCTION)) {
            rpcServerUrl = (String) getContext().lookup("cr/rpc" + requestorID + "url");
            rpcUsername = (String) getContext().lookup("cr/rpc" + requestorID + "username");
            rpcPassword = (String) getContext().lookup("cr/rpc" + requestorID + "password");
        } else if (target.equals(TARGET_TEST)) {
            rpcServerUrl = (String) getContext().lookup("cr/rpctesturl");
            rpcUsername = (String) getContext().lookup("cr/rpctestusername");
            rpcPassword = (String) getContext().lookup("cr/rpctestpassword");
        } else {
            rpcServerUrl = (String) getContext().lookup("cr/rpctesturl");
            rpcUsername = (String) getContext().lookup("cr/rpctestusername");
            rpcPassword = (String) getContext().lookup("cr/rpctestpassword");
        }

        client = new XmlRpcClient(rpcServerUrl);
        client.setBasicAuthentication(rpcUsername, rpcPassword);

        System.out.println(" getRpcClient rpcServerUrl: " + rpcServerUrl);
        return client;
    }
    public OTAResRetrieveRS otaRead(OTAReadRQ readRQMsg) {
        OTAResRetrieveRS res = new OTAResRetrieveRS();
        String requestorID = null;

        try {
            requestorID = readRQMsg.getPOS().getSource().get(0).getRequestorID().getID();
        } catch (Exception e) {
            requestorID = "reservation";
        }
         
        try {
            XmlRpcClient rpcClient = getRpcClient(readRQMsg.getTarget(), requestorID);
            Boolean uiba = userIsBookingAllowed(  readRQMsg.getTarget(),   requestorID);  
            getRequest(wsc).setAttribute("ota.ws.initialContext", this.getContext() );
            OTAResRetrieveRSBuilder builder =  new OTAResRetrieveRSBuilder(getDataSource(readRQMsg.getTarget(), requestorID), readRQMsg, wsc.getUserPrincipal().toString(), getRequest(wsc), rpcClient);            
            builder.setInitialContext(getContext()); 
            
            //builder.setPortalCode(   getPortalCode(wsc.getUserPrincipal().toString(), getContext())    );
            if(!uiba) builder.addError(Facilities.EWT_PROTOCOL_VIOLATION,Facilities.ERR_INVALID_REQUEST_CODE, CONNECTION_NEEDED);
            
            if (builder.getRes().getErrors() == null || builder.getRes().getErrors().getError().isEmpty()) {
                res = builder.build(requestorID);
            } else {
                OTAResRetrieveRS resError = new OTAResRetrieveRS();
                resError.setPrimaryLangID(builder.getRes().getPrimaryLangID());
                resError.setTimeStamp(builder.getRes().getTimeStamp());
                resError.setEchoToken(builder.getRes().getEchoToken());
                resError.setVersion(builder.getRes().getVersion());
                resError.setTarget(builder.getRes().getTarget());

                resError.setErrors(builder.getRes().getErrors());
                resError.setWarnings(builder.getRes().getWarnings());

                return resError;
            }
        } catch (Exception e) {
            Logger.getLogger(OtaWebServices.class.getName()).log(Level.SEVERE, null, e);
        }
          
        if (res.getErrors() != null) {
            OTAResRetrieveRS resError = new OTAResRetrieveRS();
            resError.setPrimaryLangID(res.getPrimaryLangID());
            resError.setTimeStamp(res.getTimeStamp());
            resError.setEchoToken(res.getEchoToken());
            resError.setVersion(res.getVersion());
            resError.setTarget(res.getTarget());

            resError.setErrors(res.getErrors());
            resError.setWarnings(res.getWarnings());

            return resError;
        }

        return res;

    }
    public MessageAcknowledgementType otaNotifReport(OTANotifReportRQ notifReportRQMsg) {
        MessageAcknowledgementType res = new MessageAcknowledgementType();
        String requestorID = null;

        try {
            requestorID = notifReportRQMsg.getNotifDetails().getHotelNotifReport().getHotelReservations().getHotelReservation().get(0).getPOS().getSource().get(0).getRequestorID().getID();
        } catch (Exception e) {
            Logger.getLogger(OtaWebServices.class.getName()).log(Level.INFO," switch reservation ",e);
            requestorID = "reservation";
        }
        Logger.getLogger(OtaWebServices.class.getName()).log(Level.INFO,  " otaNotifReport requestorID : " + requestorID );
        Logger.getLogger(OtaWebServices.class.getName()).log(Level.INFO,  " otaNotifReport getUserPrincipal : " + wsc.getUserPrincipal().toString() );
        
        try {
            XmlRpcClient rpcClient = getRpcClient(notifReportRQMsg.getTarget(), requestorID);
            
            Boolean uiba = userIsBookingAllowed(  notifReportRQMsg.getTarget(),   requestorID);  
            OTANotifReportRSBuilder builder = new OTANotifReportRSBuilder(getDataSource(notifReportRQMsg.getTarget(), requestorID), notifReportRQMsg, wsc.getUserPrincipal().toString(), getRequest(wsc));
            if(!uiba) builder.addError(Facilities.EWT_PROTOCOL_VIOLATION,Facilities.ERR_INVALID_REQUEST_CODE, CONNECTION_NEEDED);
            builder.setClient(rpcClient);
            
            builder.setInitialContext(getContext()); 
            if (builder.getRes().getErrors() == null || builder.getRes().getErrors().getError().isEmpty()) {
                res = builder.build();
            } else {
                MessageAcknowledgementType resError = new MessageAcknowledgementType();
                resError.setPrimaryLangID(builder.getRes().getPrimaryLangID());
                resError.setTimeStamp(builder.getRes().getTimeStamp());
                resError.setEchoToken(builder.getRes().getEchoToken());
                resError.setVersion(builder.getRes().getVersion());
                resError.setTarget(builder.getRes().getTarget());

                resError.setErrors(builder.getRes().getErrors());
                resError.setWarnings(builder.getRes().getWarnings());

                return resError;
            }
        } catch (Exception e) {
            Logger.getLogger(OtaWebServices.class.getName()).log(Level.SEVERE, null, e);
        }

        if (res.getErrors() != null) {
            MessageAcknowledgementType resError = new MessageAcknowledgementType();
            resError.setPrimaryLangID(res.getPrimaryLangID());
            resError.setTimeStamp(res.getTimeStamp());
            resError.setEchoToken(res.getEchoToken());
            resError.setVersion(res.getVersion());
            resError.setTarget(res.getTarget());

            resError.setErrors(res.getErrors());
            resError.setWarnings(res.getWarnings());

            return resError;
        }

        return res;

    }
    public MessageAcknowledgementType otaHotelAvailNotif(OTAHotelAvailNotifRQ hotelAvailNotifRQMsg) {
        MessageAcknowledgementType res = new MessageAcknowledgementType();
        String requestorID = null;

        try {
            requestorID = hotelAvailNotifRQMsg.getPOS().getSource().get(0).getRequestorID().getID();
        } catch (Exception e) {
            System.out.println(" Exception  requestorID = hotelAvailNotifRQMsg.getPOS().getSource().get(0).getRequestorID().getID(); ");
            e.printStackTrace();
            requestorID = "reservation"; // predefinito
        }

        try {
            Boolean needBooking = needBooking(hotelAvailNotifRQMsg.getTarget(),   requestorID);
            Boolean uiba = userIsBookingAllowed(  hotelAvailNotifRQMsg.getTarget(),   requestorID,needBooking);    
               
            OTAHotelAvailNotifRSBuilder builder = new OTAHotelAvailNotifRSBuilder(getDataSource(hotelAvailNotifRQMsg.getTarget(), requestorID), getRpcClient(hotelAvailNotifRQMsg.getTarget(), requestorID), hotelAvailNotifRQMsg, wsc.getUserPrincipal().toString(), getRequest(wsc),needBooking);
            if(!uiba) builder.addError(Facilities.EWT_PROTOCOL_VIOLATION,Facilities.ERR_INVALID_REQUEST_CODE, CONNECTION_NEEDED);
            builder.setInitialContext(getContext()); 
            if (builder.getRes().getErrors() == null || builder.getRes().getErrors().getError().isEmpty()) {
                res = builder.build(requestorID);
            } else {
                MessageAcknowledgementType resError = new MessageAcknowledgementType();
                resError.setPrimaryLangID(builder.getRes().getPrimaryLangID());
                resError.setTimeStamp(builder.getRes().getTimeStamp());
                resError.setEchoToken(builder.getRes().getEchoToken());
                resError.setVersion(builder.getRes().getVersion());
                resError.setTarget(builder.getRes().getTarget());

                resError.setErrors(builder.getRes().getErrors());
                resError.setWarnings(builder.getRes().getWarnings());

                return resError;
            }
        } catch (Exception e) {
            Logger.getLogger(OtaWebServices.class.getName()).log(Level.SEVERE, null, e);
        }

        if (res.getErrors() != null) {
            MessageAcknowledgementType resError = new MessageAcknowledgementType();
            resError.setPrimaryLangID(res.getPrimaryLangID());
            resError.setTimeStamp(res.getTimeStamp());
            resError.setEchoToken(res.getEchoToken());
            resError.setVersion(res.getVersion());
            resError.setTarget(res.getTarget());

            resError.setErrors(res.getErrors());
            resError.setWarnings(res.getWarnings());

            return resError;
        }

        return res;

    }
    public OTAHotelAvailGetRS otaHotelAvailGet(OTAHotelAvailGetRQ hotelAvailGetRQMsg) {
        OTAHotelAvailGetRS res = new OTAHotelAvailGetRS();

        String requestorID = null;
        try {
            requestorID = hotelAvailGetRQMsg.getPOS().getSource().get(0).getRequestorID().getID();
        } catch (Exception e) {
            requestorID = "reservation";
        }

        try {
            Boolean needBooking = needBooking(hotelAvailGetRQMsg.getTarget(),   requestorID);
            Boolean uiba = userIsBookingAllowed(  hotelAvailGetRQMsg.getTarget(),   requestorID,needBooking);    
               
            OTAHotelAvailGetBuilder builder = new OTAHotelAvailGetBuilder(getDataSource(hotelAvailGetRQMsg.getTarget(), requestorID), hotelAvailGetRQMsg, wsc.getUserPrincipal().toString(), getRequest(wsc),needBooking);
            if(!uiba) builder.addError(Facilities.EWT_PROTOCOL_VIOLATION,Facilities.ERR_INVALID_REQUEST_CODE, CONNECTION_NEEDED);
            builder.setInitialContext(getContext()); 
            if (builder.getRes().getErrors() == null || builder.getRes().getErrors().getError().isEmpty()) {
                res = builder.build(requestorID);
            } else {
                OTAHotelAvailGetRS resError = new OTAHotelAvailGetRS();
                resError.setPrimaryLangID(builder.getRes().getPrimaryLangID());
                resError.setTimeStamp(builder.getRes().getTimeStamp());
                resError.setEchoToken(builder.getRes().getEchoToken());
                resError.setVersion(builder.getRes().getVersion());
                resError.setTarget(builder.getRes().getTarget());

                resError.setErrors(builder.getRes().getErrors());
                resError.setWarnings(builder.getRes().getWarnings());

                return resError;
            }
        } catch (Exception e) {
            Logger.getLogger(OtaWebServices.class.getName()).log(Level.SEVERE, null, e);
        }

        if (res.getErrors() != null) {
            OTAHotelAvailGetRS resError = new OTAHotelAvailGetRS();
            resError.setPrimaryLangID(res.getPrimaryLangID());
            resError.setTimeStamp(res.getTimeStamp());
            resError.setEchoToken(res.getEchoToken());
            resError.setVersion(res.getVersion());
            resError.setTarget(res.getTarget());

            resError.setErrors(res.getErrors());
            resError.setWarnings(res.getWarnings());

            return resError;
        }

        return res;

    }
    public OTAHotelRoomListRS otaHotelRoomList(OTAHotelRoomListRQ hotelRoomListRQMsg) {
        OTAHotelRoomListRS res = new OTAHotelRoomListRS();
        String requestorID = null;

        try {
            requestorID = hotelRoomListRQMsg.getPOS().getSource().get(0).getRequestorID().getID();
        } catch (Exception e) {
            requestorID = "reservation";
        }

        try {
            Boolean uiba = userIsBookingAllowed(  hotelRoomListRQMsg.getTarget(),   requestorID); 
            OTAHotelRoomListBuilder builder = new OTAHotelRoomListBuilder(getDataSource(hotelRoomListRQMsg.getTarget(), requestorID), hotelRoomListRQMsg, wsc.getUserPrincipal().toString(), getRequest(wsc));
            if(!uiba) builder.addError(Facilities.EWT_PROTOCOL_VIOLATION,Facilities.ERR_INVALID_REQUEST_CODE, CONNECTION_NEEDED);
            builder.setInitialContext(getContext()); 
            if (builder.getRes().getErrors() == null || builder.getRes().getErrors().getError().isEmpty()) {
                res = builder.build(requestorID);
            } else {
                OTAHotelRoomListRS resError = new OTAHotelRoomListRS();
                resError.setPrimaryLangID(builder.getRes().getPrimaryLangID());
                resError.setTimeStamp(builder.getRes().getTimeStamp());
                resError.setEchoToken(builder.getRes().getEchoToken());
                resError.setVersion(builder.getRes().getVersion());
                resError.setTarget(builder.getRes().getTarget());

                resError.setErrors(builder.getRes().getErrors());
                resError.setWarnings(builder.getRes().getWarnings());

                return resError;

            }
        } catch (Exception e) {
            Logger.getLogger(OtaWebServices.class.getName()).log(Level.SEVERE, null, e);
        }

        if (res.getErrors() != null) {
            OTAHotelRoomListRS resError = new OTAHotelRoomListRS();
            resError.setPrimaryLangID(res.getPrimaryLangID());
            resError.setTimeStamp(res.getTimeStamp());
            resError.setEchoToken(res.getEchoToken());
            resError.setVersion(res.getVersion());
            resError.setTarget(res.getTarget());

            resError.setErrors(res.getErrors());
            resError.setWarnings(res.getWarnings());

            return resError;
        }

        return res;

    }
    public OTAHotelInvCountRS otaHotelInvCount(OTAHotelInvCountRQ hotelInvCountRQMsg) {
        OTAHotelInvCountRS res = new OTAHotelInvCountRS();
        String requestorID = null;
        try {
            requestorID = hotelInvCountRQMsg.getPOS().getSource().get(0).getRequestorID().getID();
        } catch (Exception e) {
            requestorID = "reservation";
        }

        try {
            Boolean uiba = userIsBookingAllowed(  hotelInvCountRQMsg.getTarget(),   requestorID);  
            OTAHotelInvCountBuilder builder = new OTAHotelInvCountBuilder(getDataSource(hotelInvCountRQMsg.getTarget(), requestorID), hotelInvCountRQMsg, wsc.getUserPrincipal().toString(), getRequest(wsc));
            if(!uiba) builder.addError(Facilities.EWT_PROTOCOL_VIOLATION,Facilities.ERR_INVALID_REQUEST_CODE, CONNECTION_NEEDED);
            builder.setInitialContext(getContext()); 
            if (builder.getRes().getErrors() == null || builder.getRes().getErrors().getError().isEmpty()) {
                res = builder.build(requestorID);
            } else {
                OTAHotelInvCountRS resError = new OTAHotelInvCountRS();
                resError.setPrimaryLangID(builder.getRes().getPrimaryLangID());
                resError.setTimeStamp(builder.getRes().getTimeStamp());
                resError.setEchoToken(builder.getRes().getEchoToken());
                resError.setVersion(builder.getRes().getVersion());
                resError.setTarget(builder.getRes().getTarget());

                resError.setErrors(builder.getRes().getErrors());
                resError.setWarnings(builder.getRes().getWarnings());

                return resError;

            }
        } catch (Exception e) {
            Logger.getLogger(OtaWebServices.class.getName()).log(Level.SEVERE, null, e);
        }

        if (res.getErrors() != null) {
            OTAHotelInvCountRS resError = new OTAHotelInvCountRS();
            resError.setPrimaryLangID(res.getPrimaryLangID());
            resError.setTimeStamp(res.getTimeStamp());
            resError.setEchoToken(res.getEchoToken());
            resError.setVersion(res.getVersion());
            resError.setTarget(res.getTarget());

            resError.setErrors(res.getErrors());
            resError.setWarnings(res.getWarnings());

            return resError;
        }

        return res;

    }
    public OTAHotelRatePlanRS otaHotelRatePlan(OTAHotelRatePlanRQ hotelRatePlanRQMsg) {
        OTAHotelRatePlanRS res = new OTAHotelRatePlanRS();

        String requestorID = null;
        try {
            requestorID = hotelRatePlanRQMsg.getPOS().getSource().get(0).getRequestorID().getID();
        } catch (Exception e) {
            requestorID = "reservation";
        }

        try {
            Boolean needBooking = needBooking(hotelRatePlanRQMsg.getTarget(),   requestorID);
            Boolean uiba = userIsBookingAllowed(  hotelRatePlanRQMsg.getTarget(),   requestorID,needBooking);   
            OTAHotelRatePlanBuilder builder = new OTAHotelRatePlanBuilder(getDataSource(hotelRatePlanRQMsg.getTarget(), requestorID), hotelRatePlanRQMsg, wsc.getUserPrincipal().toString(), getRequest(wsc),needBooking);
            if(!uiba) builder.addError(Facilities.EWT_PROTOCOL_VIOLATION,Facilities.ERR_INVALID_REQUEST_CODE, CONNECTION_NEEDED);
            builder.setInitialContext(getContext()); 
            if (builder.getRes().getErrors() == null || builder.getRes().getErrors().getError().isEmpty()) {
                res = builder.build(requestorID);
            } else {
                OTAHotelRatePlanRS resError = new OTAHotelRatePlanRS();
                resError.setPrimaryLangID(builder.getRes().getPrimaryLangID());
                resError.setTimeStamp(builder.getRes().getTimeStamp());
                resError.setEchoToken(builder.getRes().getEchoToken());
                resError.setVersion(builder.getRes().getVersion());
                resError.setTarget(builder.getRes().getTarget());

                resError.setErrors(builder.getRes().getErrors());
                resError.setWarnings(builder.getRes().getWarnings());

                return resError;
            }
        } catch (Exception e) {
            Logger.getLogger(OtaWebServices.class.getName()).log(Level.SEVERE, null, e);
        }

        if (res.getErrors() != null) {
            OTAHotelRatePlanRS resError = new OTAHotelRatePlanRS();
            resError.setPrimaryLangID(res.getPrimaryLangID());
            resError.setTimeStamp(res.getTimeStamp());
            resError.setEchoToken(res.getEchoToken());
            resError.setVersion(res.getVersion());
            resError.setTarget(res.getTarget());

            resError.setErrors(res.getErrors());
            resError.setWarnings(res.getWarnings());

            return resError;
        }

        return res;

    }
    public MessageAcknowledgementType otaHotelRateAmountNotif(OTAHotelRateAmountNotifRQ hotelRateAmountNotifRQMsg) {
        MessageAcknowledgementType res = new MessageAcknowledgementType();
        String requestorID = null;

        try {
            requestorID = hotelRateAmountNotifRQMsg.getPOS().getSource().get(0).getRequestorID().getID();
        } catch (Exception e) {
            requestorID = "reservation";
        }
        System.out.println(" otaHotelRateAmountNotif :  requestorID = " + requestorID );
        try {
            Boolean needBooking = needBooking(hotelRateAmountNotifRQMsg.getTarget(),   requestorID);
            Boolean uiba = userIsBookingAllowed(  hotelRateAmountNotifRQMsg.getTarget(),   requestorID,needBooking);    
            OTAHotelRateAmountNotifRSBuilder builder = new OTAHotelRateAmountNotifRSBuilder(getDataSource(hotelRateAmountNotifRQMsg.getTarget(), requestorID), hotelRateAmountNotifRQMsg, getRpcClient(hotelRateAmountNotifRQMsg.getTarget(), requestorID), wsc.getUserPrincipal().toString(), getRequest(wsc),needBooking);
            if(!uiba) builder.addError(Facilities.EWT_PROTOCOL_VIOLATION,Facilities.ERR_INVALID_REQUEST_CODE, CONNECTION_NEEDED);
            builder.setInitialContext(getContext()); 
            if (builder.getRes().getErrors() == null || builder.getRes().getErrors().getError().isEmpty()) {
                res = builder.build(requestorID);
            } else {
                MessageAcknowledgementType resError = new MessageAcknowledgementType();
                resError.setPrimaryLangID(builder.getRes().getPrimaryLangID());
                resError.setTimeStamp(builder.getRes().getTimeStamp());
                resError.setEchoToken(builder.getRes().getEchoToken());
                resError.setVersion(builder.getRes().getVersion());
                resError.setTarget(builder.getRes().getTarget());

                resError.setErrors(builder.getRes().getErrors());
                resError.setWarnings(builder.getRes().getWarnings());

                return resError;
            }
        } catch (Exception e) {
            Logger.getLogger(OtaWebServices.class.getName()).log(Level.SEVERE, e.getMessage());
        }
        if (res.getErrors() != null) {
            MessageAcknowledgementType resError = new MessageAcknowledgementType();
            resError.setPrimaryLangID(res.getPrimaryLangID());
            resError.setTimeStamp(res.getTimeStamp());
            resError.setEchoToken(res.getEchoToken());
            resError.setVersion(res.getVersion());
            resError.setTarget(res.getTarget());

            resError.setErrors(res.getErrors());
            resError.setWarnings(res.getWarnings());

            return resError;
        }

        return res;
    }
    public OTAHotelResNotifRS otaHotelResNotif(OTAHotelResNotifRQ hotelResNotifRQMsg) {
        OTAHotelResNotifRS res = new OTAHotelResNotifRS();
        String requestorID = null;

        try {
            requestorID = hotelResNotifRQMsg.getPOS().getSource().get(0).getRequestorID().getID();
        } catch (Exception e) {
            requestorID = "reservation";
        }
        XmlRpcClient rpcClient = null;
        try {
            rpcClient=getRpcClient(res.getTarget(), requestorID);
        } catch (Exception ex) {
            Logger.getLogger(OtaWebServices.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            Boolean uiba = userIsBookingAllowed(  hotelResNotifRQMsg.getTarget(),   requestorID);  
            OTAHotelResNotifBuilder builder = new OTAHotelResNotifBuilder(rpcClient,getDataSource(hotelResNotifRQMsg.getTarget(), requestorID), hotelResNotifRQMsg, wsc.getUserPrincipal().toString(), getRequest(wsc));
            if(!uiba) builder.addError(Facilities.EWT_PROTOCOL_VIOLATION,Facilities.ERR_INVALID_REQUEST_CODE, CONNECTION_NEEDED);
            Logger.getLogger(OtaWebServices.class.getName()).log(Level.INFO, "OTAHotelResNotif  requestorID = " + requestorID);
            builder.setInitialContext(getContext()); 
            if (builder.getRes().getErrors() == null || builder.getRes().getErrors().getError().isEmpty()) {
                Logger.getLogger(OtaWebServices.class.getName()).log(Level.INFO, "No Errors ... builder");
                res = builder.build(requestorID);
            } else {
                Logger.getLogger(OtaWebServices.class.getName()).log(Level.INFO, "Errors");    
                OTAHotelResNotifRS resError = new OTAHotelResNotifRS();
                resError.setPrimaryLangID(builder.getRes().getPrimaryLangID());
                resError.setTimeStamp(builder.getRes().getTimeStamp());
                resError.setEchoToken(builder.getRes().getEchoToken());
                resError.setVersion(builder.getRes().getVersion());
                resError.setTarget(builder.getRes().getTarget());

                resError.setErrors(builder.getRes().getErrors());
                resError.setWarnings(builder.getRes().getWarnings());

                return resError;
            }
        } catch (Exception e) {
            Logger.getLogger(OtaWebServices.class.getName()).log(Level.SEVERE, null, e);
        }
        if (res.getErrors() != null) {
            OTAHotelResNotifRS resError = new OTAHotelResNotifRS();
            resError.setPrimaryLangID(res.getPrimaryLangID());
            resError.setTimeStamp(res.getTimeStamp());
            resError.setEchoToken(res.getEchoToken());
            resError.setVersion(res.getVersion());
            resError.setTarget(res.getTarget());

            resError.setErrors(res.getErrors());
            resError.setWarnings(res.getWarnings());

            return resError;
        }

        return res;
    }
    public OTAHotelDescriptiveInfoRS otaHotelDescriptiveInfo(OTAHotelDescriptiveInfoRQ hotelDescriptiveInfoRQMsg) {
        OTAHotelDescriptiveInfoRS res = new OTAHotelDescriptiveInfoRS();
        String requestorID = null;

        try {
            requestorID = hotelDescriptiveInfoRQMsg.getPOS().getSource().get(0).getRequestorID().getID();
        } catch (Exception e) {
            requestorID = "reservation";
        }

        try {
            Boolean uiba = userIsBookingAllowed(  hotelDescriptiveInfoRQMsg.getTarget(),   requestorID);
            OTAHotelDescriptiveInfoBuilder builder = new OTAHotelDescriptiveInfoBuilder(getDataSource(hotelDescriptiveInfoRQMsg.getTarget(), requestorID), getRpcClient(hotelDescriptiveInfoRQMsg.getTarget(), requestorID), hotelDescriptiveInfoRQMsg, wsc.getUserPrincipal().toString(), getRequest(wsc));
            if(!uiba) builder.addError(Facilities.EWT_PROTOCOL_VIOLATION,Facilities.ERR_INVALID_REQUEST_CODE, CONNECTION_NEEDED);
            builder.setInitialContext(getContext()); 
            if (builder.getRes().getErrors() == null || builder.getRes().getErrors().getError().isEmpty()) {
                res = builder.build(requestorID);
            } else {
                OTAHotelDescriptiveInfoRS resError = new OTAHotelDescriptiveInfoRS();
                resError.setPrimaryLangID(builder.getRes().getPrimaryLangID());
                resError.setTimeStamp(builder.getRes().getTimeStamp());
                resError.setEchoToken(builder.getRes().getEchoToken());
                resError.setVersion(builder.getRes().getVersion());
                resError.setTarget(builder.getRes().getTarget());

                resError.setErrors(builder.getRes().getErrors());
                resError.setWarnings(builder.getRes().getWarnings());

                return resError;
            }

        } catch (Exception e) {
            Logger.getLogger(OtaWebServices.class.getName()).log(Level.SEVERE, null, e);
        }

        if (res.getErrors() != null) {
            OTAHotelDescriptiveInfoRS resError = new OTAHotelDescriptiveInfoRS();
            resError.setPrimaryLangID(res.getPrimaryLangID());
            resError.setTimeStamp(res.getTimeStamp());
            resError.setEchoToken(res.getEchoToken());
            resError.setVersion(res.getVersion());
            resError.setTarget(res.getTarget());

            resError.setErrors(res.getErrors());
            resError.setWarnings(res.getWarnings());

            return resError;
        }

        return res;

    }
    public OTAProfileCreateRS otaProfileCreate(OTAProfileCreateRQ profileCreateRQMsg) {
        OTAProfileCreateRS res = new OTAProfileCreateRS();
        String requestorID = null;

        try {
            requestorID = profileCreateRQMsg.getUniqueID().get(0).getID();
        } catch (Exception e) {
            requestorID = "reservation";
        }

        try {
            Boolean uiba = userIsBookingAllowed(  profileCreateRQMsg.getTarget(),   requestorID); 
            OTAProfileCreateBuilder builder = new OTAProfileCreateBuilder(getDataSource(profileCreateRQMsg.getTarget(), requestorID), profileCreateRQMsg, wsc.getUserPrincipal().toString(), getRequest(wsc));
            if(!uiba) builder.addError(Facilities.EWT_PROTOCOL_VIOLATION,Facilities.ERR_INVALID_REQUEST_CODE, CONNECTION_NEEDED);
            builder.setInitialContext(getContext()); 
            if (builder.getRes().getErrors() == null || builder.getRes().getErrors().getError().isEmpty()) {
                res = builder.build(requestorID);
            } else {
                OTAProfileCreateRS resError = new OTAProfileCreateRS();
                resError.setPrimaryLangID(builder.getRes().getPrimaryLangID());
                resError.setTimeStamp(builder.getRes().getTimeStamp());
                resError.setEchoToken(builder.getRes().getEchoToken());
                resError.setVersion(builder.getRes().getVersion());
                resError.setTarget(builder.getRes().getTarget());

                resError.setErrors(builder.getRes().getErrors());
                resError.setWarnings(builder.getRes().getWarnings());

                return resError;
            }

        } catch (Exception e) {
            Logger.getLogger(OtaWebServices.class.getName()).log(Level.SEVERE, null, e);
        }

        if (res.getErrors() != null) {
            OTAProfileCreateRS resError = new OTAProfileCreateRS();
            resError.setPrimaryLangID(res.getPrimaryLangID());
            resError.setTimeStamp(res.getTimeStamp());
            resError.setEchoToken(res.getEchoToken());
            resError.setVersion(res.getVersion());
            resError.setTarget(res.getTarget());

            resError.setErrors(res.getErrors());
            resError.setWarnings(res.getWarnings());

            return resError;
        }

        return res;
    }
    public static void main(String[] args) {
        
        Properties jndiProperties = new Properties();
            //jndiProperties.put("java.naming.provider.url","iiop://93.95.216.56:19437");
            
            //jndiProperties.put("org.omg.CORBA.ORBInitialPort", "19437"); 
            jndiProperties.setProperty("java.naming.factory.initial", "com.sun.enterprise.naming.SerialInitContextFactory");
            jndiProperties.setProperty("java.naming.factory.url.pkgs", "com.sun.enterprise.naming");
            jndiProperties.setProperty("java.naming.factory.state", "com.sun.corba.ee.impl.presentation.rmi.JNDIStateFactoryImpl");
            jndiProperties.setProperty("org.omg.CORBA.ORBInitialHost", "93.95.216.56");   

        try {
            System.out.println("Connecting..");  
            InitialContext ic = new InitialContext(jndiProperties);
            System.out.println("Connected");
            System.out.println("No Error");
             
            int portalValue = (Integer) ic.lookup("portal/unitas");
            int jndiVersion = (Integer) ic.lookup("version/AbsConnectService/number");
             
            portalValue = getPortalCode("S00041", ic);
            System.out.println("from user portal="+portalValue);
            
            if(1==1) return;
            String sqlMl = "select multirate_id from multirate where multirate_code=? and structure_id=?";
            DataSource ds= Facilities.createDataSource("develop", "develop", "jdbc:mysql://fileserver:3306/cmsonei_absdaniele");

            QueryRunner run = new QueryRunner(ds);            
            int m_id = 0;
            m_id = (Integer)run.query(sqlMl, new ScalarHandler("multirate_id"), "TML1", 2);
            
            System.out.println("m_id = " +m_id);
        } catch (Exception e) {
            System.out.println("Error.....");
            e.printStackTrace();
        }
    }
}
