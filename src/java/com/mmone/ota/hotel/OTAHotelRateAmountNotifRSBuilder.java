package com.mmone.ota.hotel;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;

import javax.sql.DataSource;
import javax.xml.datatype.DatatypeConfigurationException;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.xmlrpc.XmlRpcClient;
import org.opentravel.ota._2003._05.ErrorType;
import org.opentravel.ota._2003._05.ErrorsType;
import org.opentravel.ota._2003._05.MessageAcknowledgementType;
import org.opentravel.ota._2003._05.OTAHotelRateAmountNotifRQ;
import org.opentravel.ota._2003._05.RateAmountMessageType;
import org.opentravel.ota._2003._05.RateUploadType;
import org.opentravel.ota._2003._05.RateUploadType.AdditionalGuestAmounts;
import org.opentravel.ota._2003._05.RateUploadType.BaseByGuestAmts.BaseByGuestAmt;
import org.opentravel.ota._2003._05.StatusApplicationControlType;
import org.opentravel.ota._2003._05.SuccessType;
import org.opentravel.ota._2003._05.WarningsType;
import org.opentravel.ota._2003._05.WarningType;

// TODO: Auto-generated Javadoc
/**
 * The Class OTAHotelRateAmountNotifRSBuilder.
 */
public class OTAHotelRateAmountNotifRSBuilder  extends BaseBuilder{

    private static final Logger logger = Logger.getLogger(OTAHotelRateAmountNotifRSBuilder.class.getName());
    private DataSource ds;
    private XmlRpcClient client;
    private OTAHotelRateAmountNotifRQ request;
    private QueryRunner run;
    private MessageAcknowledgementType res = new MessageAcknowledgementType();
    private int hotelCode;
    private String user;
    private String langID;
    private String echoToken;
    private String requestorID;
    private String target = Facilities.TARGET_PRODUCTION;
    private HttpServletRequest httpRequest;
    private BigDecimal version = new BigDecimal(Facilities.VERSION);
    private Map<String, String> logData = new LinkedHashMap<String, String>();
    private int lastUsedSpecificationId = -1;
    private Connection connection = null;
     
    public final void Requestorid(String type, String code, String message) {
        if (res.getErrors() == null) {
            res.setErrors(new ErrorsType());
        }

        ErrorType et = new ErrorType();
        et.setCode(code);
        et.setType(type);
        et.setValue(message);

        res.getErrors().getError().add(et);
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
        msg.append(" <br>\n****");

        for (String key : infoRequest.keySet()) {
            msg.append("- ").append(key).append(" = ").append(infoRequest.get(key)).append(" ");
        }

        Logger.getLogger(OTAHotelRateAmountNotifRSBuilder.class.getName()).log(Level.INFO, msg.toString());
    }
    private boolean needBooking = false;
    public OTAHotelRateAmountNotifRSBuilder(DataSource ds, OTAHotelRateAmountNotifRQ request, XmlRpcClient client, String user, HttpServletRequest httpRequest,boolean needBooking) {
        super();
        
        if (ds == null) {
            Requestorid(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Server. System Error. No connection with the database");
            return;
        }
        this.needBooking = needBooking;
        this.needBooking = true;
        this.request = request;
        this.langID = request.getPrimaryLangID();
        this.user = user;
        this.client = client;
        this.ds = ds;
        this.echoToken = request.getEchoToken();
        this.target = request.getTarget();
        this.version = request.getVersion();
        this.httpRequest = httpRequest;

        if (!target.equals(Facilities.TARGET_PRODUCTION) && !target.equals(Facilities.TARGET_TEST)) {
            this.target = Facilities.TARGET_PRODUCTION;
            Requestorid(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_REQUEST_CODE, "Target is invalid.");
        }

        if (langID == null || langID.equals("")) {
            addWarning(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_LANGUAGE_CODE_INVALID, "Language is invalid. Default to it.");
            langID = "it";
        }

        if (!version.toString().equals(Facilities.VERSION)) {
            Requestorid(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_REQUEST_CODE, "Version is invalid.");
        }

        if (echoToken == null || echoToken.equals("")) {
            Requestorid(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_REQUEST_CODE, "EchoToken is invalid.");
        }

        try {
            res.setTimeStamp(com.mmone.ota.rpc.Facilities.getToday());
        } catch (DatatypeConfigurationException ex) {
        }

        res.setEchoToken(echoToken);
        res.setTarget(target);
        res.setPrimaryLangID(langID);
        res.setVersion(version);

        run = new QueryRunner(ds);

        try {
            String sHotelCode = request.getRateAmountMessages().getHotelCode();
            if (StringUtils.isEmpty(sHotelCode)) {
                Requestorid(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_HOTEL_CODE, "HotelCode null");
                hotelCode = 0;
            } else {
                hotelCode = Integer.parseInt(sHotelCode);
            }
        } catch (Exception e) {
            Requestorid(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "System Error. In HotelCode: " + e.getMessage());
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
            Requestorid(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Client. Authorization Error: You don't have permission to select the structure " + hotelCode);
        } else {
            Integer p = new Integer(permission.get("permissions").toString());
            //int testEnv = p & Facilities.TEST_ENVIRONMENT;
            int prodEnv = p & Facilities.PRODUCTION_ENVIRONMENT;

            if (request.getTarget().equals("Production") && prodEnv == 0) {
                Requestorid(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Client. Authorization Error: You don't have permission to select the production environment");
            }
        }

        if (thereIsError()) {
            return;
        }
    }
    private Connection getConnection() throws SQLException{
        if(connection!=null) 
           if(connection.isValid(10)) return this.connection;     
        
        this.connection = ds.getConnection();
        
        try {
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            Requestorid(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "System Error. SQL error"); 
        }  
        return connection;
    }
    public MessageAcknowledgementType build(String requestorID) {
        this.requestorID = requestorID;
         
        try {
               
            try {
                List<RateAmountMessageType> lrateAmountMessageType = request.getRateAmountMessages().getRateAmountMessage();
                int rateAmountMessageTypeCounter = 0;
                for (RateAmountMessageType rateAmountMessageType : lrateAmountMessageType) {
                    rateAmountMessageTypeCounter++;
                    logData.put("-Class", this.getClass().getName());
                    logData.put("-TimeStamp", res.getTimeStamp().toString());
                    logData.put("-user", user);
                    logData.put("-RemoteAddr", this.httpRequest.getRemoteAddr());
                    logData.put("-EchoToken", this.request.getEchoToken());
                    logData.put("-Target", this.request.getTarget());
                    logData.put("-HotelCode", Integer.toString(hotelCode));
                     
                    if (!thereIsError()) {
                        doInsertIntoPricelistRpc(getConnection(), rateAmountMessageType);
                    }

                    logInfoRequest(logData);
                    logData = new LinkedHashMap<String, String>();
                }
            } catch (Exception e) {
                Requestorid(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "System Error. Inserting data into price list");
                Logger.getLogger(OTAHotelRateAmountNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
                //DbUtils.rollbackAndCloseQuietly(connection);
            } finally {
                DbUtils.commitAndCloseQuietly(connection);
            }
        } catch (Exception e) {
            Requestorid(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, e.getMessage());
            Logger.getLogger(OTAHotelRateAmountNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
        }

        if (!thereIsError()) {
            res.setSuccess(new SuccessType());
        }

        return res;
    }

    private Object[] setExtraBed( RateUploadType.BaseByGuestAmts.BaseByGuestAmt aga,
            int priceListId,  int specificationId, Object[] extrabed,int iMealPlanCodes /*, int extraBed*/) {

        int minAge = 0;
        int maxAge = 0;

        try {
            minAge = aga.getMinAge();
        } catch (Exception e) {
            addWarning(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_REQUIRED_FIELD_MISSING, "@MinAge is invalid or null");
            //return priceListParams;
        }

        try {
            maxAge = aga.getMaxAge();
        } catch (Exception e) {
            addWarning(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_REQUIRED_FIELD_MISSING, "@MaxAge is invalid or null");
            //return priceListParams;
        }

        BigDecimal amount = aga.getAmountAfterTax();

        if (amount == null) {
            Requestorid(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_PRICE_INCORRECT_FOR_ROOM_UNIT, "@AmountAfterTax null");
            return extrabed;
        }

        if (minAge > maxAge) {
            Requestorid(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_PRICE_INCORRECT_FOR_ROOM_UNIT, "@MinAge > @MaxAge");
            return extrabed;
        }

        if (amount.floatValue() < 0.0) {
            Requestorid(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_PRICE_INCORRECT_FOR_ROOM_UNIT, "@AmountAfterTax less than 0");
            return extrabed;
        }

        Pattern p = Pattern.compile("(([3456789]{1})|((10)|(11)|(12){1}))[CVP]{1}"); // fino a nove extrabed
        String code = "V";

        try {
            code = aga.getCode();
        } catch (Exception e) {
        }


        Matcher m = p.matcher(code);
        boolean b = m.matches();
        
        if (!b) {
            Requestorid(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_PRICE_INCORRECT_FOR_ROOM_UNIT, "@Code is invalid");
            return extrabed;
        }

        int extraBed = Character.getNumericValue(code.charAt(0));
        String amountCode = Character.toString(code.charAt(1));
        
        logData.put("-ExtraBed " + extraBed, Integer.toString(extraBed));
        logData.put("-MinAge " + extraBed + " - " + minAge, Integer.toString(minAge));
        logData.put("-MaxAge " + extraBed + " - " +  maxAge, Integer.toString(maxAge));
        logData.put("-ExtraBed Amount " + extraBed + " - " + minAge + " - " + maxAge, amount.toString());


        logData.put("minAge == 0 && maxAge == 0 " + (minAge == 0 && maxAge == 0) ,"");
        logData.put("minAge >= 0 && maxAge <= 18 " + (minAge >= 0 && maxAge <= 18) ,"");

         logData.put("specificationId = " +   specificationId,"");
         logData.put("codiceListino = " +   priceListId,"");
         logData.put("iMealPlanCodes = " +   iMealPlanCodes,"");

        if (minAge == 0 && maxAge == 0) {
             logData.put("passo per  (minAge == 0 && maxAge == 0) " ,"");
            //String key = null;
            //String value = null;
            String extrabedSpecificationId = "EXTRABED_SPECIFICATION_ID";
            String extrabedGuestType = "EXTRABED_GUEST_TYPE";
            String extrabedGuestAge = "EXTRABED_GUEST_AGE";
            String extrabedPriceValue = "EXTRABED_PRICE_VALUE";
            String extrabedPriceType = "EXTRABED_PRICE_TYPE";
            String extrabedBedNumber = "EXTRABED_BED_NUMBER";
            try {
                // obsoleto con pricelist
                //key = "PRICE_LIST_P" + extraBed + "_VALUE_ADULT";
                //value = "PRICE_LIST_P" + extraBed + "_VALUE_TYPE_ADULT";
                //int index = Facilities.getIntAttribute(key);
                //int indexValue = Facilities.getIntAttribute(value);
                //priceListParams[index] = amount;
                //priceListParams[indexValue] = amountCode;

                // nuovo extrabed
                int indexExtrabedSpecificationId = Facilities.getIntAttribute(extrabedSpecificationId);
                int indexExtrabedGuestType = Facilities.getIntAttribute(extrabedGuestType);
                int indexExtrabedGuestAge = Facilities.getIntAttribute(extrabedGuestAge);
                int indexExtrabedPriceValue = Facilities.getIntAttribute(extrabedPriceValue);
                int indexExtrabedPriceType = Facilities.getIntAttribute(extrabedPriceType);
                int indexExtrabedBedNumber = Facilities.getIntAttribute(extrabedBedNumber);

                extrabed[indexExtrabedSpecificationId] = specificationId;
                extrabed[indexExtrabedGuestType] = 0; // adulto
                extrabed[indexExtrabedGuestAge] = 0; // adulto
                extrabed[indexExtrabedPriceValue] = amount;
                extrabed[indexExtrabedPriceType] = amountCode;
                extrabed[indexExtrabedBedNumber] = extraBed;
                // System.out.println("-- Setting extrabed");    
                Facilities.executeInsertInto(getConnection(), Facilities.STM_INSERT_INTO_EXTRABED, extrabed);
                Facilities.executeUpdate(
                    getConnection(),
                    Facilities.STM_UPDATE_PRICELIST_TREATMENT,
                    new Object[]{specificationId},
                    priceListId,
                    iMealPlanCodes
                ) ;
                //System.out.println("-- Setting extrabed done");

            } catch (Exception ex) {
                System.out.println("## ERROR [setExtrabed] minAge == 0 && maxAge == 0 "+ ex.getMessage());
                System.out.println(Facilities.STM_INSERT_INTO_EXTRABED);
                for (int ecount = 0; ecount < extrabed.length; ecount++) {
                    System.out.println("-- parameter " + ecount + extrabed[ecount] );
                }
                logData.put("Error", "[setExtrabed] minAge == 0 && maxAge == 0 "+ ex.getMessage());
                logInfoRequest(logData);
                
                //Requestorid(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "System Error. In extrabed: index A; " + ex.getMessage());
                Logger.getLogger(OTAHotelRateAmountNotifRSBuilder.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (minAge >= 0 && maxAge <= 18) {

            logData.put("passo per  (minAge >= 0 && maxAge <= 18) " ,"");
            String logMsg="\nminAge >= 0 && maxAge <= 18";
            
            for (int i = minAge; i <= maxAge; i++) {
                //String key = null;
                //String value = null;
                logData.put("i ", i + "");
                logMsg+="\n i="+i;
                String extrabedSpecificationId = "EXTRABED_SPECIFICATION_ID";
                String extrabedGuestType = "EXTRABED_GUEST_TYPE";
                String extrabedGuestAge = "EXTRABED_GUEST_AGE";
                String extrabedPriceValue = "EXTRABED_PRICE_VALUE";
                String extrabedPriceType = "EXTRABED_PRICE_TYPE";
                String extrabedBedNumber = "EXTRABED_BED_NUMBER";
                try {
                 
                    // nuovo extrabed
                    int indexExtrabedSpecificationId = Facilities.getIntAttribute(extrabedSpecificationId);
                    int indexExtrabedGuestType = Facilities.getIntAttribute(extrabedGuestType);
                    int indexExtrabedGuestAge = Facilities.getIntAttribute(extrabedGuestAge);
                    int indexExtrabedPriceValue = Facilities.getIntAttribute(extrabedPriceValue);
                    int indexExtrabedPriceType = Facilities.getIntAttribute(extrabedPriceType);
                    int indexExtrabedBedNumber = Facilities.getIntAttribute(extrabedBedNumber);

                    extrabed[indexExtrabedSpecificationId] = specificationId;
                    extrabed[indexExtrabedGuestType] = 1; // bambino
                    extrabed[indexExtrabedGuestAge] = i;
                    extrabed[indexExtrabedPriceValue] = amount;
                    extrabed[indexExtrabedPriceType] = amountCode;
                    extrabed[indexExtrabedBedNumber] = extraBed;
                    
                    logMsg+="\n indexExtrabedSpecificationId="+specificationId;
                    logMsg+="\n indexExtrabedGuestType="+1;
                    logMsg+="\n indexExtrabedGuestAge="+i;
                    logMsg+="\n indexExtrabedPriceValue="+amount;
                    logMsg+="\n indexExtrabedPriceType="+amountCode;
                    logMsg+="\n indexExtrabedBedNumber="+extraBed;
                    
                    // System.out.println("-- Setting extrabed");
                    logMsg+="\n setto extrabed  ";
                    int executeInsertIntoExtrabedId = Facilities.executeInsertInto(getConnection(), Facilities.STM_INSERT_INTO_EXTRABED, extrabed);
                    logMsg+="\n settati extrabed id creato=" + executeInsertIntoExtrabedId;
                    
                    logMsg+="\n specificationId="+specificationId;
                    logMsg+="\n priceListId="+priceListId;
                    logMsg+="\n iMealPlanCodes="+iMealPlanCodes;
                    logMsg+="\n setto treatment  ";
                    Facilities.executeUpdate(
                                getConnection(),
                                Facilities.STM_UPDATE_PRICELIST_TREATMENT,
                                new Object[]{specificationId},
                                priceListId,
                                iMealPlanCodes
                    ) ;
                    logMsg+="\n Fatto  ";
                    // System.out.println("-- Setting extrabed done");    
                } catch (Exception ex) {
                    System.out.println("## ERROR [setExtrabed] minAge == 0 && maxAge == 0 "+ ex.getMessage());
                    System.out.println(Facilities.STM_INSERT_INTO_EXTRABED);
                    for (int ecount = 0; ecount < extrabed.length; ecount++) {
                        System.out.println("-- parameter " + ecount + extrabed[ecount] );
                    }
                    logData.put("Error", "[setExtrabed] minAge >= 0 && maxAge <= 18 "+ ex.getMessage());
                    logInfoRequest(logData);
                     
                    // Requestorid(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "System Error. In extrabed: index " + i + "; " + ex.getMessage());
                    Logger.getLogger(OTAHotelRateAmountNotifRSBuilder.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            System.out.println(logMsg);
        }
        
        return extrabed;
    }

    private int getPricelistId(Integer roomId,Integer iClistino, Date dStartDate,Date dEndDate){
        int pricelist_id=-1;
        try {
            pricelist_id = (Integer) run.query(
                "SELECT pricelist_id from pricelist where  "
                +" room_id=? "
                +" AND list_id=?  "
                +" AND pricelist_date_from=? "
                +" AND pricelist_date_to=? ",
                new ScalarHandler(),
                roomId,
                iClistino,
                dStartDate,
                dEndDate
            );
        } catch (SQLException ex) {
            // Logger.getLogger(OTAHotelRateAmountNotifRSBuilder.class.getName()).log(Level.SEVERE, null, ex);
            //Requestorid(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_RATE_CODE, "RatePlanCode is invalid");
        }
        System.out.println("-- pricelist_id " + pricelist_id);
        return pricelist_id;
    }
    synchronized private void doInsertIntoPricelistRpc(Connection conn, RateAmountMessageType rateAmountMessageType) {

        /*
        ((((((((((()
        /  _____   /|
        /  /____/  /-|
        /  COMMENT /--|  Crea una chiamata rpc e salva le camere ed i trattamenti
        /__________/---|
        |-----------|
        '-----------'
         */
        StatusApplicationControlType statusApplicationControlType = rateAmountMessageType.getStatusApplicationControl();

        Object[] priceListParams = Facilities.getEmptyPriceListValues();
        String codiceListino = statusApplicationControlType.getRatePlanCode();
        Vector parameters = new Vector();

        Map rooms = new Hashtable();
        Map room = new Hashtable();

        Map treatments = null;
        //Map treatment = null;

        
        /*

        _____
        /     \______
        |   _________|__
        |  / LOAD      /
        | /  REQUEST  /
        |/___________/    recupero il codice listino

         */
        Integer iClistino=null;
        if (codiceListino == null) { 
        } else if (codiceListino.equals("NR")) {
            codiceListino = "1";
            iClistino =1;
        } else if (codiceListino.equals("IU")) {
            codiceListino = "1";
            iClistino =1;
        } else { 
            if(this.needBooking){ 
                String sqlMl = "select multirate_id from multirate where multirate_code=? and structure_id=?";
                try {
                    iClistino = (Integer) run.query(sqlMl, new ScalarHandler("multirate_id"), codiceListino, hotelCode); 
                } catch (Exception e) {  } 
                
            }else{
                iClistino=null;
            }
        }   
         
        Integer pricelist_id = null;
        if (codiceListino == null || iClistino==null ) {
            Requestorid(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_RATE_CODE, "RatePlanCode is invalid");
            return;
        } else {
            priceListParams[Facilities.PRICE_LIST_LIST_ID] = iClistino;
            //priceListParams[Facilities.PRICE_LIST_LIST_ID] = 1;
        }

        /*
        _____
        /     \______
        |   _________|__
        |  / LOAD      /
        | /  REQUEST  /
        |/___________/    recupero il codice camera
         */
        String codiceCamera = null;
        try {
            codiceCamera = statusApplicationControlType.getInvCode().toString();
        } catch (Exception e) {
            
        }
        
        int roomId = 0;
        int roomExtraBed = 0;

        if (codiceCamera == null) {
            Requestorid(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_ROOM_UNIT_CODE_INCORRECT, "InvCode null");
        } else {

            /*
            .----.
            |'----'|
            |  DB  |
            | load |   carico la room dalla tabella room   tramite codice camera e id struttura
            '.____.'
             */
            try {
                Map mCamera = run.query(
                        "SELECT room_id,room_use_extrabed FROM room WHERE room_code= ? AND structure_id = ?",
                        new MapHandler(),
                        codiceCamera,
                        hotelCode);

                priceListParams[Facilities.PRICE_LIST_ROOM_ID] = Integer.parseInt(mCamera.get("room_id").toString());
                rooms.put(mCamera.get("room_id").toString(), room);
                roomId = Integer.parseInt(mCamera.get("room_id").toString());
                roomExtraBed = new Integer(mCamera.get("room_use_extrabed").toString());
            } catch (Exception e) {
                Requestorid(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "System Error. Retrieving room id: " + e.getMessage());
                return  ;
            }
        }

        logData.put("Room Code", codiceCamera);
        priceListParams[Facilities.PRICE_LIST_STRUCTURE_ID] = hotelCode;

        /*
        _____
        /     \______
        |   _________|__
        |  / LOAD      /
        | /  REQUEST  /
        |/___________/    recupero start date
         */

        Date dStartDate = null;
        String sStartDate = statusApplicationControlType.getStart();
        if (StringUtils.isEmpty(sStartDate)) {
            Requestorid(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_START_DATE_INVALID, "Start date null");
        } else {
            try {
                dStartDate = new Date(DateUtils.parseDate(sStartDate, com.mmone.ota.hotel.Facilities.dateParsers).getTime());
                priceListParams[Facilities.PRICE_LIST_DATE_FROM] = dStartDate;
            } catch (Exception e) {
                Requestorid(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Start date invalid: " + e.getMessage());
                return ;
            }
        }

        /*
        _____
        /     \______
        |   _________|__
        |  / LOAD      /
        | /  REQUEST  /
        |/___________/    recupero end date
         */
        Date dEndDate = null;
        String sEndDate = statusApplicationControlType.getEnd();
        if (StringUtils.isEmpty(sEndDate)) {
            Requestorid(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_END_DATE_INVALID, "End date null");
        } else {

            try {
                dEndDate = new Date(DateUtils.parseDate(sEndDate, com.mmone.ota.hotel.Facilities.dateParsers).getTime());
                priceListParams[Facilities.PRICE_LIST_DATE_TO] = dEndDate;
            } catch (Exception e) {
                Requestorid(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "System Error. End date invalid: " + e.getMessage());
                return ;
            }

            if (dStartDate != null) {
                if (dStartDate.getTime() > dEndDate.getTime()) {
                    Requestorid(Facilities.EWT_UNKNOWN, Facilities.ERR_END_DATE_INVALID, "Start date greater than end date");
                    return  ;
                }
            }

        }

        logData.put("StartDate", sStartDate);
        logData.put("EndDate", sEndDate);
         
        priceListParams[Facilities.PRICE_LIST_PRICE_SET] = 1;
        priceListParams[Facilities.PRICE_LIST_ROOM_ID] = roomId;

        /*
        _____________
        ....-''``'._ _________)
        ,_  '-.___)           definisco i parametri per la chiamata rpc
        `'-._)_)
        -----'``"-,__(__)

         */
        

        /*
        ((((((((((()
        /  _____   /|
        /  /____/  /-|  Gestione extra bed
        /  COMMENT /--|  controllo tramite parametro
        /__________/---|  AdditionalGuestAmounts se sono previsti extra bed
        |-----------|
        '-----------'
         */

        RateAmountMessageType.Rates.Rate rate = null;
        List<BaseByGuestAmt> bbGuestAmount = null;
        AdditionalGuestAmounts agas = null; // da eliminare

        List<RateAmountMessageType.Rates.Rate> rates = rateAmountMessageType.getRates().getRate();

        int lastId = 0;

        treatments = new Hashtable();

        String sMealPlanCodes = null;
        List pList = null;
        int specificationId = 0;

        for (int i = 0; i < rates.size(); i++) {
            parameters = new Vector();    
            parameters.add(priceListParams[Facilities.PRICE_LIST_LIST_ID]);
            parameters.add(hotelCode);
            parameters.add(sStartDate);  //data i
            parameters.add(sEndDate);  //data f
            sMealPlanCodes = null;
            
            try {
                rate = rateAmountMessageType.getRates().getRate().get(i);
                bbGuestAmount = rate.getBaseByGuestAmts().getBaseByGuestAmt();

                // verifica che ce ne sia almeno uno
                agas = rate.getAdditionalGuestAmounts();

                try {
                    sMealPlanCodes = rate.getMealsIncluded().getMealPlanCodes().get(0);
                } catch (Exception e) {
                    System.out.println("-- for rates Error MealPlanCodes " + e.getMessage());
                    logInfoRequest(logData);
                    Requestorid(Facilities.EWT_UNKNOWN, Facilities.ERR_BOARD_BASIS_OR_MEAL_PLAN_INVALID, "MealPlanCodes invalid: " + e.getMessage());
                    return  ;
                }

            } catch (Exception e) {
                System.out.println("-- for rates Error " + e.getMessage());
                logInfoRequest(logData);
                Requestorid(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Error with rates: " + e.getMessage());
                return  ;
            }
            
            System.out.println("-- for rates i=" + i + " startDate = " + sStartDate + " treatment="+sMealPlanCodes);
            logData.put("Trattamento", sMealPlanCodes);

            Float price = null;
            BigDecimal bprice = null;
            
            try {
                specificationId = (Integer)run.query(Facilities.SELECT_MAX_EXTRABED_SPECIFICATION_ID, new ScalarHandler());
            } catch (Exception e) {
                System.out.println("-- for rates specificationId Error " + e.getMessage());
                logInfoRequest(logData);
                Logger.getLogger(OTAHotelRateAmountNotifRSBuilder.class.getName()).log(Level.SEVERE, null, e);
            }
            
            if(lastUsedSpecificationId>specificationId) specificationId=lastUsedSpecificationId;     
            specificationId = specificationId + 1;
            lastUsedSpecificationId=specificationId;
            // nell'ordine
            // pricelist
            // extrabed
            //if (bbGuestAmount.size() <= 3) { // else errore
            int  iMealPlanCodes = new Integer(sMealPlanCodes);
            
            for (int j = 0; j < bbGuestAmount.size(); j++) {
                String ageQualifyingCode  =  bbGuestAmount.get(j).getAgeQualifyingCode();
                if(ageQualifyingCode==null) ageQualifyingCode=Facilities.ADULT_QUALYFING_CODE;
                System.out.println("j bbGuestAmount " + j + " " + bbGuestAmount.get(0).getAmountAfterTax());
                 
                if (ageQualifyingCode != null) {
                    switch (j) {
                        case 0: // tariffa base, un solo caso
                            if (ageQualifyingCode.equals(Facilities.ADULT_QUALYFING_CODE)) {
                                try {
                                    bprice = bbGuestAmount.get(0).getAmountAfterTax();
                                    price = bprice.floatValue();

                                    logData.put("Tariffa", Float.toString(price));

                                    if (bprice == null) {
                                        Requestorid(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_PRICE_INCORRECT_FOR_ROOM_UNIT, "Price null");
                                    }
                                } catch (Exception e) {
                                    Requestorid(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "@AmountAfterTax is invalid");
                                }

                                if (price < 0.0) {
                                    Requestorid(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_PRICE_INCORRECT_FOR_ROOM_UNIT, "@AmountAfterTax less than 0");
                                }

                                if (thereIsError()) {
                                    return  ;
                                }

                                /*
                                ((((((((((()
                                /  _____   /|
                                /  /____/  /-|  Trattamenti
                                /  COMMENT /--|
                                /__________/---|
                                |-----------|
                                '-----------'
                                 */
                                
                                try {
                                   

                                    /*
                                    _____________
                                    ....-''``'._ _________)
                                    ,_  '-.___)           definisco i parametri per la chiamata rpc della room
                                    `'-._)_)
                                    -----'``"-,__(__)
                                     */

                                    treatments.put(new Integer(Facilities.MPT_TO_MM[iMealPlanCodes]).toString(), Float.toString(price));
                                } catch (Exception e) {
                                    Requestorid(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Error setting treatment parameters: " + e.getMessage());
                                    return  ;
                                }

                                rooms.put(new Integer(roomId).toString(), treatments);
                                parameters.add(rooms);

                                String days = null;

                                boolean mon = false;
                                boolean tue = false;
                                boolean weds = false;
                                boolean thur = false;
                                boolean fri = false;
                                boolean sat = false;
                                boolean sun = false;

                                try {
                                    mon = statusApplicationControlType.isMon();
                                } catch (Exception e) {
                                    mon = true;
                                }

                                try {
                                    tue = statusApplicationControlType.isTue();
                                } catch (Exception e) {
                                    tue = true;
                                }

                                try {
                                    weds = statusApplicationControlType.isWeds();
                                } catch (Exception e) {
                                    weds = true;
                                }

                                try {
                                    thur = statusApplicationControlType.isThur();
                                } catch (Exception e) {
                                    thur = true;
                                }

                                try {
                                    fri = statusApplicationControlType.isFri();
                                } catch (Exception e) {
                                    fri = true;
                                }

                                try {
                                    sat = statusApplicationControlType.isSat();
                                } catch (Exception e) {
                                    sat = true;
                                }

                                try {
                                    sun = statusApplicationControlType.isSun();
                                } catch (Exception e) {
                                    sun = true;
                                }

                                days = ""
                                        + (mon ? "1" : "0")
                                        + (tue ? "1" : "0")
                                        + (weds ? "1" : "0")
                                        + (thur ? "1" : "0")
                                        + (fri ? "1" : "0")
                                        + (sat ? "1" : "0")
                                        + (sun ? "1" : "0")
                                        + "";

                                parameters.add(days);

                               
                                
                                int forCounter = 0;
                                String tmpKey  ="";
                                String tmpData = "";
                                Vector _result = null;
                                try {
                                    Object result = client.execute("backend.setPriceList", parameters);
                                    _result = (Vector) result;
                                    
                                    if(_result!=null){
                                        for (int k = 0; k < _result.size(); k++) {
                                            forCounter=k;
                                            Hashtable data = (Hashtable) _result.get(k);
                                            Enumeration en = data.keys();

                                            while (en.hasMoreElements()) {
                                                String key = (String) en.nextElement();
                                                tmpKey=key;
                                                String _data = (String) data.get(key);
                                                tmpData=_data;
                                                if (key.equals("from")) {
                                                    dStartDate = new Date(DateUtils.parseDate(_data, com.mmone.ota.hotel.Facilities.dateParsers).getTime());
                                                } else if (key.equals("to")) {
                                                    dEndDate = new Date(DateUtils.parseDate(_data, com.mmone.ota.hotel.Facilities.dateParsers).getTime());
                                                }
                                                //System.out.println("Key Data:" + key + " " + _data);
                                            }
                                        }
                                    }

                                } catch (Exception e) {
                                    //Requestorid(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Error on updating pricelist: " + e.getMessage());
                                    Logger.getLogger(OTAHotelRateAmountNotifRSBuilder.class.getName()).log(Level.SEVERE, e.getMessage(), e);
                                    Logger.getLogger(OTAHotelRateAmountNotifRSBuilder.class.getName()).log(Level.SEVERE, "Error k="+forCounter);
                                    Logger.getLogger(OTAHotelRateAmountNotifRSBuilder.class.getName()).log(Level.SEVERE, "Error key="+tmpKey);
                                    
                                    Logger.getLogger(OTAHotelRateAmountNotifRSBuilder.class.getName()).log(Level.SEVERE, "Result="+_result);
                                    
                                     for (Object parameter : parameters) {
                                        Logger.getLogger(OTAHotelRateAmountNotifRSBuilder.class.getName()).log(Level.SEVERE, "Dato="+parameter);

                                    }
                                    //return  ;
                                }

                                if (roomExtraBed == 0) {
                                    priceListParams[Facilities.PRICE_LIST_EXTRABED_SET] = 1;
                                }
                            }
                            break;
                        //case 1:
                        //case 2: // extrabed
                        default:
                            if (ageQualifyingCode.equals(Facilities.CHILD_QUALYFING_CODE) || ageQualifyingCode.equals(Facilities.ADULT_QUALYFING_CODE) /*&& (j == 1 || j == 2)*/) {
                                /*
                                ________
                                .'`   |    |`'.
                                |     '----'  |
                                |  .-------.  |
                                |  |-------|  |  salva gli extrabed
                                |  ; SAVE  ;  |
                                |__:_______:__|
                                 */
                                //priceListParams[Facilities.PRICE_LIST_EXTRABED_SET] = 0;

                                if (roomExtraBed == 1) {
                                    pricelist_id=getPricelistId(  roomId,  iClistino,   dStartDate,  dEndDate); 
                                    Object[] extrabed = Facilities.getEmptyExtrabedValues();
                                    extrabed = setExtraBed( bbGuestAmount.get(j), pricelist_id,  specificationId, extrabed, new Integer(Facilities.MPT_TO_MM[iMealPlanCodes])  );
                                    
                                    try {
                                        Facilities.executeUpdate(
                                            getConnection(), 
                                            "UPDATE  pricelist SET pricelist_extrabed_set = 1 WHERE pricelist_id =" +  pricelist_id      
                                        );        
                                    } catch (Exception e) {
                                        Requestorid(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Error on updating pricelist: " + e.getMessage());
                                        return  ;
                                    }   
                                }

                                //priceListParams[Facilities.PRICE_LIST_EXTRABED_SET] = 1;
                                //for (Object pricelistId : plist) {
                                //    Facilities.executeUpdate(conn, Facilities.STM_UPDATE_PRICELIST, priceListParams, pricelistId);//                                           
                                //}
                            }
                            break;
//                            default:
//                                break;
                        } // switch
                } else {
                   
                    Requestorid(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "@AgeQualyfingCode is invalid");
                    return  ;
                }
            } // guestamt
            //} else {
            //    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "BaseByGuestAmts is invalid");
            //}
        } // for
 
    }

    private boolean thereIsError() {
        return !(res.getErrors() == null || res.getErrors().getError().isEmpty());
    }

    public MessageAcknowledgementType getRes() {
        return res;
    }
}
