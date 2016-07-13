package com.mmone.ota.hotel;

import org.opentravel.ota._2003._05.ErrorType;
import org.opentravel.ota._2003._05.ErrorsType;
import org.opentravel.ota._2003._05.OTAHotelRatePlanRQ;
import org.opentravel.ota._2003._05.SuccessType;
import org.opentravel.ota._2003._05.WarningsType;
import org.opentravel.ota._2003._05.WarningType;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.opentravel.ota._2003._05.HotelRatePlanType;
import org.opentravel.ota._2003._05.OTAHotelRatePlanRS;
import org.opentravel.ota._2003._05.OTAHotelRatePlanRS.RatePlans;
import org.opentravel.ota._2003._05.ParagraphType;
import org.opentravel.ota._2003._05.RatePlanCandidatesType;
import org.opentravel.ota._2003._05.RateUploadType;
import org.opentravel.ota._2003._05.RateUploadType.BaseByGuestAmts;

// TODO: Auto-generated Javadoc
/**
 * The Class OtaHotelRatePlanBuilder.
 */
public class OTAHotelRatePlanBuilder  extends BaseBuilder{

    private DataSource ds;
    private OTAHotelRatePlanRQ request;
    private QueryRunner run;
    private OTAHotelRatePlanRS res = new OTAHotelRatePlanRS();
    private String user;
    private String langID;
    private String echoToken;
    private String requestorID;
    private String target = Facilities.TARGET_PRODUCTION;
    private BigDecimal version = new BigDecimal(Facilities.VERSION);
    private Map<String,String> logData = new LinkedHashMap<String, String>();
     
    String hotelCode = null;
    Integer ihotelCode = null;

    String sqlOffers = ""
            + " SELECT offer.offer_code,offer_detail.offer_title"
            + " FROM offer"
            + "  LEFT JOIN offer_detail ON offer_detail.offer_id = offer.offer_id"
            + " WHERE"
            + "  language=?"
            + "  AND offer_status=1"
            + "  AND structure_id=?"
            + "  AND ((offer_private_date_publish_start BETWEEN ? AND ?)"
            + "  OR (offer_private_date_publish_end BETWEEN ? AND ?))"
            + "";

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

        Logger.getLogger(OTAHotelRatePlanBuilder.class.getName()).log(Level.INFO, msg.toString());
    }

    /**
     * Instantiates a new OTA hotel rate amount notif rs builder.
     *
     * @param ds the ds
     * @param request the request
     */
    private Boolean needBooking;
    public OTAHotelRatePlanBuilder(DataSource ds, OTAHotelRatePlanRQ request, String user, HttpServletRequest httpRequest,boolean pneedBooking) {
        super();
        this.needBooking = pneedBooking;
        this.needBooking = true;
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

        res.setEchoToken(request.getEchoToken());

        res.setTarget(target);
        res.setPrimaryLangID(langID);
        res.setVersion(version);

        logData.put("Class" , this.getClass().getName());
        logData.put("TimeStamp", res.getTimeStamp().toString());
        logData.put("user", user);
        logData.put("RemoteAddr", httpRequest.getRemoteAddr());
        logData.put("EchoToken", request.getEchoToken());
        logData.put("Target", target);
        
        run = new QueryRunner(ds);

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

        logData.put("HotelCode", hotelCode);

        logInfoRequest(logData);
        
        if (thereIsError()) {
            return;
        }

        res.setRatePlans(new OTAHotelRatePlanRS.RatePlans());
    }

    public OTAHotelRatePlanRS build(String requestorID) {
        this.requestorID = requestorID;
        List<OTAHotelRatePlanRQ.RatePlans.RatePlan> lRatePlan = request.getRatePlans().getRatePlan();

        for (OTAHotelRatePlanRQ.RatePlans.RatePlan ratePlan : lRatePlan) {
            List<RatePlanCandidatesType.RatePlanCandidate> lRatePlanCandidate = ratePlan.getRatePlanCandidates().getRatePlanCandidate();
            OTAHotelRatePlanRQ.RatePlans.RatePlan.DateRange dr = ratePlan.getDateRange();
             
            
            for (RatePlanCandidatesType.RatePlanCandidate ratePlanCandidate : lRatePlanCandidate) { 
                String listId = ratePlanCandidate.getRatePlanCode().toString();
                if(listId.equals(DOWNLOAD_RATES_LIST)){
                    buildRatesList( listId );
                } else{
                    if(dr!=null)
                        buildRatePlans(ratePlanCandidate,dr);
                    else
                        buildRatePlans(ratePlanCandidate);
                }    
            }
        }

        return res;
    }
    public static final String DOWNLOAD_RATES_LIST = "download-rates-list";
    
    private void buildRatesList(String listId ){
        String sqlMl =  " select multirate_code from multirate " +
                        " where   structure_id=? and multirate_status=1 and multirate_deleted=0";
        List<Object>  listini=null;
        try { 
            listini = run.query(sqlMl, new ColumnListHandler ("multirate_code"), ihotelCode); 
        } catch (Exception e) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Error loading rates");
            return;
        } 
        
        if(this.res.getRatePlans()==null) 
            this.res.setRatePlans( new RatePlans() ); 
        
        this.res.getRatePlans().setHotelCode(hotelCode);
        for (Object listino : listini) {
            String sListino = (String) listino;
            
            HotelRatePlanType ratePlan = new HotelRatePlanType();
            
            ratePlan.setRatePlanCode(sListino);
            
            this.res
                .getRatePlans()
                .getRatePlan().add(ratePlan) ;
        }
        
    }
    private void setHotelCode() {
        try {
            hotelCode = request.getRatePlans().getRatePlan().get(0).getRatePlanCandidates().getRatePlanCandidate().get(0).getHotelRefs().getHotelRef().get(0).getHotelCode();
            ihotelCode = new Integer(hotelCode);
        } catch (Exception e) {
            addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_REQUIRED_FIELD_MISSING, "@HotelCode is null");
            Logger.getLogger(OTAHotelRatePlanBuilder.class.getName()).log(Level.SEVERE, null, e);
        }

    }

    private void setOfferte(HotelRatePlanType.Offers offers, String structureId, String language, Date dateStart, Date dateEnd) {
        try {
            List<HotelRatePlanType.Offers.Offer> loffer = offers.getOffer();
            List<Map<String, Object>> offerte = run.query(sqlOffers, new MapListHandler(), language, structureId, dateStart, dateEnd, dateStart, dateEnd);

            for (Map<String, Object> record : offerte) {
                HotelRatePlanType.Offers.Offer offer = new HotelRatePlanType.Offers.Offer();
                offer.setOfferCode((String) record.get("offer_code"));
                String desc = (String) record.get("offer_title");

                ParagraphType p = new ParagraphType();
                p.setLanguage(language);

                JAXBElement jax = new JAXBElement(new QName("http://www.opentravel.org/OTA/2003/05", "Text"), String.class, desc);
                p.getTextOrImageOrURL().add(jax);

                offer.setOfferDescription(p);

                loffer.add(offer);
            }
        } catch (Exception ex) {
            Logger.getLogger(OTAHotelRatePlanBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    public OTAHotelRatePlanRS buildRatePlans(RatePlanCandidatesType.RatePlanCandidate ratePlanCandidate) {
        OTAHotelRatePlanRQ.RatePlans.RatePlan.DateRange dr = new OTAHotelRatePlanRQ.RatePlans.RatePlan.DateRange();
        
        
        dr.setStart(  DateFormatUtils.format(new Date(), "yyyy-MM-dd")         );
        dr.setEnd("2100-12-31");
        
        return buildRatePlans(ratePlanCandidate, dr);
    }
     public OTAHotelRatePlanRS buildRatePlans(RatePlanCandidatesType.RatePlanCandidate ratePlanCandidate,OTAHotelRatePlanRQ.RatePlans.RatePlan.DateRange dr) {
        RatePlans ratePlans = res.getRatePlans();

        try {
            String listId = null;
            Integer ilistId = null;

            try {
                listId = ratePlanCandidate.getRatePlanCode().toString();

                if (listId.equals(Facilities.NORMAL_RATE) || listId.equals(Facilities.SPECIAL_RATE)) {
                    if (listId.equals(Facilities.NORMAL_RATE))  ilistId = 1;
                    else  ilistId = 2;
                     
                } else {
                    if(this.needBooking){ 
                        String sqlMl = "select multirate_id from multirate where multirate_code=? and structure_id=? and multirate_status=1 and multirate_deleted=0";
                        try {
                            ilistId = (Integer)run.query(sqlMl, new ScalarHandler("multirate_id"), listId, ihotelCode); 
                        } catch (Exception e) {
                            addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_REQUIRED_FIELD_MISSING, "@RatePlanCode null or invalid");
                            return res;
                        }

                    }else{
                        addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_REQUIRED_FIELD_MISSING, "@RatePlanCode null or invalid");
                        return res;
                    }
                    
                    
                }
            } catch (Exception e) {
                addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_REQUIRED_FIELD_MISSING, "@RatePlanCode null or invalid");
                return res;
            }

            ratePlans.setHotelCode(hotelCode);

            List prices = run.query(Facilities.SELECT_PRICE_LIST_PERIOD, new MapListHandler(), ihotelCode, ilistId, dr.getStart(),dr.getEnd(), res.getPrimaryLangID(), ihotelCode);
//            List structurePayment = run.query(Facilities.SELECT_STRUCTURE_PAYMENT, new MapListHandler(), Facilities.DEFAULTS_PORTAL_CODE, ihotelCode);

//            RateUploadType.GuaranteePolicies guaranteePolicies = new RateUploadType.GuaranteePolicies();
//
//            for (int i = 0; i < structurePayment.size(); i++) {
//                Map row = (Map) structurePayment.get(i);
//                GuaranteeType guaranteeType = null;
//                boolean found = false;
//
//                Integer iValue = (Integer) row.get("structure_payment_cc");
//                if (iValue.intValue() == 1) {
//                    guaranteeType = new GuaranteeType();
//                    guaranteeType.setGuaranteeType("CC/DC/Voucher");
//                    guaranteePolicies.getGuaranteePolicy().add(guaranteeType);
//                    found = true;
//                }
//
//                iValue = (Integer) row.get("structure_payment_bt");
//                if (iValue.intValue() == 1) {
//                    guaranteeType = new GuaranteeType();
//                    guaranteeType.setGuaranteeType("PrePay");
//                    guaranteePolicies.getGuaranteePolicy().add(guaranteeType);
//                    found = true;
//                }
//
//                if (!found) {
//                    guaranteeType = new GuaranteeType();
//                    guaranteeType.setGuaranteeType("None");
//                    guaranteePolicies.getGuaranteePolicy().add(guaranteeType);
//                }
//            }

            Map<String, HotelRatePlanType> mHotelRatePlanType = new Hashtable();
            
            String[] patterns = new String[]{"yyyy-MM-dd"}; 
            
            Date st = DateUtils.parseDate(dr.getStart(), patterns);
            Date en = DateUtils.parseDate(dr.getEnd(), patterns);
            
            for (int i = 0; i < prices.size(); i++) {
                Map row = (Map) prices.get(i);
                 
                String dateFrom = DateFormatUtils.format((Date) row.get("pricelist_date_from"), "dd-MM-yyyy");
                String dateTo = DateFormatUtils.format((Date) row.get("pricelist_date_to"), "dd-MM-yyyy");
                
                 
                
                String composePeriod = dateFrom + "@@" + dateTo + "@@" + listId;

                HotelRatePlanType hotelRatePlanType = mHotelRatePlanType.get(composePeriod);

                if (hotelRatePlanType == null) {
                    hotelRatePlanType = new HotelRatePlanType();
                    mHotelRatePlanType.put(composePeriod, hotelRatePlanType);
                    hotelRatePlanType.setRatePlanCode(listId);
                    hotelRatePlanType.setStart(dateFrom);
                    hotelRatePlanType.setEnd(dateTo);
                    hotelRatePlanType.setRates(new HotelRatePlanType.Rates());

                    ratePlans.getRatePlan().add(hotelRatePlanType);
                    hotelRatePlanType.setOffers(new HotelRatePlanType.Offers());

                    setOfferte(hotelRatePlanType.getOffers(), hotelCode, langID, (Date) row.get("pricelist_date_from"), (Date) row.get("pricelist_date_to"));
                } 

                // per ogni camara ho un Rate
                Integer roomId = (Integer) row.get("room_id");

                String sqlChkRoom = "SELECT room_code,room_use_extrabed FROM room WHERE room_id=? AND structure_id=? ";

                String invCode = null;
                Map roomDetails = null;

                try {
                    roomDetails = run.query(sqlChkRoom, new MapHandler(), roomId, hotelCode);
                } catch (Exception ex) {
                    Logger.getLogger(OTAHotelRatePlanBuilder.class.getName()).log(Level.SEVERE, null, ex);
                    addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Error retrieving room");
                    return res;
                }

                try {
                    invCode = roomDetails.get("room_code").toString();
                } catch (Exception e) {

                }

                if (invCode == null)
                    continue;
                
                HotelRatePlanType.Rates.Rate rate = new HotelRatePlanType.Rates.Rate();
                rate.setInvCode(invCode);
                //configuro rate
                rate.setBaseByGuestAmts(new BaseByGuestAmts());

                RateUploadType.BaseByGuestAmts.BaseByGuestAmt baseByGuestAmt =
                        new RateUploadType.BaseByGuestAmts.BaseByGuestAmt();

                BigDecimal amountAfterTax = new BigDecimal(row.get("price").toString());
                baseByGuestAmt.setAmountAfterTax(amountAfterTax);
                baseByGuestAmt.setAgeQualifyingCode(Facilities.ADULT_QUALYFING_CODE);
                rate.getBaseByGuestAmts().getBaseByGuestAmt().add(baseByGuestAmt);

                // extrabed               
                if (roomDetails.get("room_use_extrabed").toString().equals("1")) {
                    List<Map<String, Object>> lExtrabeds = null;

                    try {
                        lExtrabeds = run.query(Facilities.SELECT_EXTRABED, new MapListHandler(), row.get("extrabed_specification_id"));
                    } catch (Exception ex) {
                        Logger.getLogger(OTAHotelRatePlanBuilder.class.getName()).log(Level.SEVERE, null, ex);
                        addError(Facilities.EWT_UNKNOWN, Facilities.ERR_INVALID_VALUE, "Error retrieving extrabed");
                        return res;
                    }

                    int _extrabedGuestType = 0;
                    int _extrabedGuestAge = 0;
                    String _extrabedPrice = "";
                    String _extrabedPriceType = "";
                    int _extrabedNumber = 0;
                    int extrabedGuestType = 0;
                    int extrabedGuestAge = 0;
                    String extrabedPrice = "";
                    String extrabedPriceType = "";
                    int extrabedNumber = 0;
                    int maxAge = 0;
                    int check = 0;
                    
                    for (Map<String, Object> extrabed : lExtrabeds) {
                        extrabedGuestType = (Integer) extrabed.get("extrabed_guest_type");
                        extrabedGuestAge = (Integer) extrabed.get("extrabed_guest_age");
                        extrabedPrice = extrabed.get("extrabed_price_value").toString(); // no float per problemi con BigDecimal
                        extrabedPriceType = (String) extrabed.get("extrabed_price_type");
                        extrabedNumber = (Integer) extrabed.get("extrabed_bed_number");
                        
                        if (check == 0) {
                            _extrabedGuestType = extrabedGuestType;
                            _extrabedGuestAge = extrabedGuestAge;
                            _extrabedPrice = extrabedPrice;
                            _extrabedPriceType = extrabedPriceType;
                            _extrabedNumber = extrabedNumber;
                            check = 1;
                        }

                        // raggruppa extrabed
                        if (_extrabedGuestType == extrabedGuestType && _extrabedPrice.equals(extrabedPrice)
                                && _extrabedPriceType.equals(extrabedPriceType) && _extrabedNumber == extrabedNumber) {                            
                            maxAge = extrabedGuestAge;
                            continue;
                        } else {
                            RateUploadType.BaseByGuestAmts.BaseByGuestAmt baseByGuestAmtExtraBed =
                                    new RateUploadType.BaseByGuestAmts.BaseByGuestAmt();

                            if (_extrabedGuestType == 0) { // adulti
                                try {
                                    baseByGuestAmtExtraBed.setAmountAfterTax(new BigDecimal(_extrabedPrice));
                                } catch (Exception e) {
                                    baseByGuestAmtExtraBed.setAmountAfterTax(new BigDecimal("0.0"));
                                }
                                baseByGuestAmtExtraBed.setAgeQualifyingCode(Facilities.ADULT_QUALYFING_CODE);
                                baseByGuestAmtExtraBed.setCode(Integer.toString(_extrabedNumber) + _extrabedPriceType);
                                rate.getBaseByGuestAmts().getBaseByGuestAmt().add(baseByGuestAmtExtraBed);
                            } else { // bambini
                                try {
                                    baseByGuestAmtExtraBed.setAmountAfterTax(new BigDecimal(_extrabedPrice));
                                } catch (Exception e) {
                                    baseByGuestAmtExtraBed.setAmountAfterTax(new BigDecimal("0.0"));
                                }
                                baseByGuestAmtExtraBed.setAgeQualifyingCode(Facilities.CHILD_QUALYFING_CODE);
                                baseByGuestAmtExtraBed.setMinAge(_extrabedGuestAge);
                                baseByGuestAmtExtraBed.setMaxAge(maxAge);
                                baseByGuestAmtExtraBed.setCode(Integer.toString(_extrabedNumber) + _extrabedPriceType);
                                rate.getBaseByGuestAmts().getBaseByGuestAmt().add(baseByGuestAmtExtraBed);
                            }
                            
                            _extrabedGuestType = extrabedGuestType;
                            _extrabedGuestAge = extrabedGuestAge;
                            _extrabedPrice = extrabedPrice;
                            _extrabedPriceType = extrabedPriceType;
                            _extrabedNumber = extrabedNumber;
                        }
                    }

                    if (check == 1) {
                        RateUploadType.BaseByGuestAmts.BaseByGuestAmt baseByGuestAmtExtraBed =
                                new RateUploadType.BaseByGuestAmts.BaseByGuestAmt();

                        if (_extrabedGuestType == 0) { // adulti
                            try {
                                baseByGuestAmtExtraBed.setAmountAfterTax(new BigDecimal(_extrabedPrice));
                            } catch (Exception e) {
                                baseByGuestAmtExtraBed.setAmountAfterTax(new BigDecimal("0.0"));
                            }
                            baseByGuestAmtExtraBed.setAgeQualifyingCode(Facilities.ADULT_QUALYFING_CODE);
                            baseByGuestAmtExtraBed.setCode(Integer.toString(_extrabedNumber) + _extrabedPriceType);
                            rate.getBaseByGuestAmts().getBaseByGuestAmt().add(baseByGuestAmtExtraBed);
                        } else { // bambini
                            try {
                                baseByGuestAmtExtraBed.setAmountAfterTax(new BigDecimal(_extrabedPrice));
                            } catch (Exception e) {
                                baseByGuestAmtExtraBed.setAmountAfterTax(new BigDecimal("0.0"));
                            }
                            baseByGuestAmtExtraBed.setAgeQualifyingCode(Facilities.CHILD_QUALYFING_CODE);
                            baseByGuestAmtExtraBed.setMinAge(_extrabedGuestAge);
                            baseByGuestAmtExtraBed.setMaxAge(maxAge);
                            baseByGuestAmtExtraBed.setCode(Integer.toString(_extrabedNumber) + _extrabedPriceType);
                            rate.getBaseByGuestAmts().getBaseByGuestAmt().add(baseByGuestAmtExtraBed);
                        }
                    }
                    

                    
                } else{
                
                }
                
                //ParagraphType rateDescriptionText = new ParagraphType();
                //rateDescriptionText.setLanguage(res.getPrimaryLangID());
                //rateDescriptionText.setName("Text");

                //JAXBElement text = new JAXBElement(new QName("http://www.opentravel.org/OTA/2003/05", "Text"), String.class, row.get("room_description"));
                //FormattedTextTextType formattedTextTextType = new FormattedTextTextType();
                //formattedTextTextType.setValue( en((String) row.get("room_description") ) );
                //text.setValue( formattedTextTextType );

                //rateDescriptionText.getTextOrImageOrURL().add(text);
                //rate.setRateDescription(rateDescriptionText);

                rate.setMealsIncluded(new RateUploadType.MealsIncluded());
                Integer mmMealPlanCode = (Integer) row.get("treatment_id");
                Integer mptMealPlanCode = Facilities.MM_TO_MPT[mmMealPlanCode.intValue()];
                rate.getMealsIncluded().getMealPlanCodes().add(mptMealPlanCode.toString());
                rate.setIsRoom(Boolean.TRUE);
                rate.setCurrencyCode(Facilities.DEFAULT_CURRENCY_CODE);
//              rate.setGuaranteePolicies(guaranteePolicies);
                    
                hotelRatePlanType.getRates().getRate().add(rate);
            } //for (int i = 0; i < prices.size(); i++)
        } catch (Exception e) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "System Error. Please contact the administrator");
            Logger.getLogger(OTAHotelRatePlanBuilder.class.getName()).log(Level.SEVERE, null, e);
        }

        if (!thereIsError()) {
            res.setSuccess(new SuccessType());
        }
        return res;
    }

    public OTAHotelRatePlanRS getRes() {
        return res;
    }

    private boolean thereIsError() {
        return !(res.getErrors() == null || res.getErrors().getError().isEmpty());
    }
}
