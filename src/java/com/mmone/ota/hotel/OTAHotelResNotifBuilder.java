/*
 * Aggiornamento prenotazioni 
 */
package com.mmone.ota.hotel;

import com.sun.org.apache.xerces.internal.dom.ElementNSImpl;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import java.beans.XMLEncoder;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.xmlrpc.XmlRpcClient;
import org.opentravel.ota._2003._05.AddressType;
import org.opentravel.ota._2003._05.CancelPenaltyType;
import org.opentravel.ota._2003._05.CommentType.Comment;
import org.opentravel.ota._2003._05.CompanyInfoType;
import org.opentravel.ota._2003._05.CompanyNameType;
import org.opentravel.ota._2003._05.CountryNameType;
import org.opentravel.ota._2003._05.CustomerType;
import org.opentravel.ota._2003._05.ErrorType;
import org.opentravel.ota._2003._05.ErrorsType;
import org.opentravel.ota._2003._05.FormattedTextTextType;
import org.opentravel.ota._2003._05.GuestCountType.GuestCount;
import org.opentravel.ota._2003._05.HotelReservationIDsType.HotelReservationID;
import org.opentravel.ota._2003._05.HotelReservationType;
import org.opentravel.ota._2003._05.OTAHotelResNotifRQ;
import org.opentravel.ota._2003._05.OTAHotelResNotifRS;
import org.opentravel.ota._2003._05.ParagraphType;
import org.opentravel.ota._2003._05.PersonNameType;
import org.opentravel.ota._2003._05.ProfileType;
import org.opentravel.ota._2003._05.ProfilesType.ProfileInfo;
import org.opentravel.ota._2003._05.RatePlanType;
import org.opentravel.ota._2003._05.RateType.Rate;
import org.opentravel.ota._2003._05.RequiredPaymentsType.GuaranteePayment;
import org.opentravel.ota._2003._05.ResGlobalInfoType;
import org.opentravel.ota._2003._05.ResGuestsType;
import org.opentravel.ota._2003._05.RoomStayType.RoomRates;
import org.opentravel.ota._2003._05.RoomStaysType;
import org.opentravel.ota._2003._05.RoomTypeType;
import org.opentravel.ota._2003._05.StateProvType;
import org.opentravel.ota._2003._05.SuccessType;
import org.opentravel.ota._2003._05.TPAExtensionsType;
import org.opentravel.ota._2003._05.TotalType;
import org.opentravel.ota._2003._05.WarningType;
import org.opentravel.ota._2003._05.WarningsType;
import org.opentravel.ota._2003._05.YesNoType;

/**
 *
 * @author umberto.zanatta
 */
public class OTAHotelResNotifBuilder  extends BaseBuilder{

    private DataSource ds;
    private QueryRunner run;
    private OTAHotelResNotifRQ request;
    private OTAHotelResNotifRS res = new OTAHotelResNotifRS();
    private String user;
    private String langID;
    private String echoToken;
    private String target = Facilities.TARGET_PRODUCTION;
    private String requestorID;
    private String remoteAddress;
    private BigDecimal version = new BigDecimal(Facilities.VERSION);
    String hotelCode = null;
    String resIDValue = null;
    String resIDDate = null;
    private Map<String, String> logData = null;
    private Map<String, String> profileCustomer = null;
    private Map<String, String> reservation = null;
    // reservation_detail_item ->  reservation_detail_type -> reservation_detail_name, price, trattamento
    private Map<Integer, Map<String, String>> reservationDetail = null;
    private Map<String, String> reservationDetailArray = null;
     
    private BigInteger numRooms = null;
    private String TpaXml = null;
    private XmlRpcClient rpcClient;
    
    private static Map<String, Boolean> cacheUpdated = new Hashtable<String, Boolean>();
    
    
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

        Logger.getLogger(OTAHotelResNotifBuilder.class.getName()).log(Level.INFO, msg.toString());
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

    public OTAHotelResNotifBuilder(XmlRpcClient rpcClient, DataSource ds, OTAHotelResNotifRQ request, String user, HttpServletRequest httpRequest) {
        super();
        
        this.rpcClient=rpcClient;
        
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
        this.remoteAddress = httpRequest.getRemoteAddr();

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
       
        run = new QueryRunner(ds);
    }

    public OTAHotelResNotifRS build(String requestorID) throws Exception {
        this.requestorID = requestorID;
         
        List<HotelReservationType> hotelReservations = request.getHotelReservations().getHotelReservation();

        if (hotelReservations != null) {
            if (hotelReservations.size() < 1) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation is invalid");
                return res;
            }
        }

        for (HotelReservationType hotelReservation : hotelReservations) {
            String resStatus = null;
            RoomStaysType roomStaysType = null;
            ResGuestsType resGuestsType = null;
            List<RoomStaysType.RoomStay> lRoomStaysType = null;
            List<ResGuestsType.ResGuest> lResGuestsType = null;            
            List<ProfileInfo> profileInfos = null;
            ProfileInfo profileInfo = null;
            ProfileType profile = null;
            CustomerType customer = null;
            String language = null;
            List<PersonNameType> personName = null;
            String givenName = null;
            String surName = null;
            List<CustomerType.Telephone> telephones = null;
            List<CustomerType.Email> emails = null;
            String email = "";
            List<CustomerType.Address> addresses = null;
            AddressType address = null;
            List<String> addressLines = null;
            String addressLine = "";
            String cityName = "";
            String postalCode = "";
            StateProvType stateProvType = null;
            String stateProv = "";
            //String stateCode = "";
            CountryNameType countryNameType = null;
            String countryName = "";
            String countryCode = "";
            CompanyInfoType companyInfoType = null;
            List<CompanyNameType> companyNames = null;
            CompanyNameType companyNameType = null;
            String companyName = "";
            numRooms = new BigInteger("0");
            profileCustomer = new LinkedHashMap<String, String>();
            reservation = new LinkedHashMap<String, String>();
            reservationDetail = new LinkedHashMap<Integer, Map<String, String>>();
            logData = new LinkedHashMap<String, String>();
            YesNoType shareAllMarket = YesNoType.NO;
            boolean bShareAllMarket=false;
                    
            logData.put("Class", this.getClass().getName());
            logData.put("TimeStamp", res.getTimeStamp().toString());
            logData.put("user", this.user);
            logData.put("RemoteAddr", this.remoteAddress);
            logData.put("EchoToken", this.echoToken);
            logData.put("Target", this.request.getTarget());
            
            reservation.put("reservation_guest_language", "it");
            
            try { 
                 //com.sun.org.apache.xerces.internal.dom.ElementNSImp
                
                TPAExtensionsType supData =  hotelReservation.getTPAExtensions();
                //logData.put("TPAExt size", ""+supData.getAny().size());
                Integer iSupData =  supData.getAny().size();
                 
                if(supData.getAny().size()>0){  
                    StringWriter sxml = new StringWriter(); 
                    XMLSerializer serial = new XMLSerializer( sxml, null );
                    serial.asDOMSerializer();  
                    serial.serialize( (ElementNSImpl)supData.getAny().get(0) );
                    sxml.close(); 
                    this.TpaXml = sxml.toString();
                    logData.put("TpaXml", this.TpaXml);
                    //Logger.getLogger(OTAHotelResNotifBuilder.class.getName()).log(Level.INFO, "xml  " +  sxml.toString() ); 
                }
                
            } catch (Exception e) {
                Logger.getLogger(OTAHotelResNotifBuilder.class.getName()).log(Level.WARNING, "sSupData error",e);
                 
            }    
            
            try {
                resStatus = hotelReservation.getResStatus();
            } catch (Exception e) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/@ResStatus is invalid");
                return res;
            }

            if (resStatus != null) {
                if (!resStatus.equals(Facilities.resStatus[0]) && !resStatus.equals(Facilities.resStatus[1]) &&
                    !resStatus.equals(Facilities.resStatus[2])) {
                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/@ResStatus is invalid");
                    return res;
                }
            } else {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/@ResStatus is invalid");
                return res;
            }

            reservation.put("reservation_status", resStatus);
            //setUniqueID(hotelReservation); // Nr prenotazione PMS

            // *****************************************************************
            // HotelReservation/RoomStays
            // *****************************************************************
            try {
                roomStaysType = hotelReservation.getRoomStays();
            } catch (Exception e) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/RoomStays is invalid.");
                return res;
            }

            if (roomStaysType != null) {
                lRoomStaysType = roomStaysType.getRoomStay();
            } else {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/RoomStays/RoomStay is invalid");
                return res;
            }

            if (lRoomStaysType.size() > 0) {
                int j = 1;
                for (RoomStaysType.RoomStay roomStayType : lRoomStaysType) { // per ogni camera
                    reservationDetailArray = new LinkedHashMap<String, String>();
                    setBasicPropertyInfo(roomStayType);
                    if (thereIsError()) {
                        return res;
                    }
                    logData.put("HotelCode", hotelCode); //controlla Hotel Code in ogni ciclo

                    setRoomType(roomStayType, j);
                    if (thereIsError()) {
                        return res;
                    }
                    setRatePlan(roomStayType, j);
                    if (thereIsError()) {
                        return res;
                    }
                    setRoomRate(roomStayType, j);
                    if (thereIsError()) {
                        return res;
                    }
                    //setGuestCount(roomStayType);
                    //if (thereIsError()) {
                    //    return res;
                    //}
                    //setTimeSpan(roomStayType);
                    //if (thereIsError()) {
                    //    return res;
                    //}
                    //setTotal(roomStayType, j);
                    //if (thereIsError()) {
                    //    return res;
                    //}

                    reservationDetail.put(j, reservationDetailArray);
                    j += 2;
                }

                reservation.put("reservation_numrooms", numRooms.toString());
                logData.put("Num Rooms", numRooms.toString());
            } else {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Size of HotelReservations/HotelReservation/RoomStays element is less than one");
                return res;
            }

            // *****************************************************************
            // HotelReservation/ResGuests
            // *****************************************************************
            try {
                resGuestsType = hotelReservation.getResGuests();
            } catch (Exception e) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/ResGuests is invalid");
                return res;
            }

            if (resGuestsType != null) {
                lResGuestsType = resGuestsType.getResGuest();
            } else {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/ResGuests/ResGuest is invalid");
                return res;
            }

            if (lResGuestsType.size() == 1) {
                ResGuestsType.ResGuest resGuest = lResGuestsType.get(0);
                //resGuestRPH = resGuest.getResGuestRPH();

                try {
                    profileInfos = resGuest.getProfiles().getProfileInfo();
                } catch (Exception e) {
                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo is invalid");
                    return res;
                }

                if (profileInfos.size() == 1) {
                    profileInfo = profileInfos.get(0);

                    try {
                        profile = profileInfo.getProfile();
                    } catch (Exception e) {
                        addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile is invalid");
                        return res;
                    }
                    
                    
                    try {
                        shareAllMarket=profile.getShareAllMarketInd();
                    } catch (Exception e) {
                        shareAllMarket = YesNoType.NO;
                    }
                    if(shareAllMarket==null) shareAllMarket=YesNoType.NO;
                    
                    if(shareAllMarket.equals(YesNoType.YES)){
                        bShareAllMarket=true;
                    }else{
                        bShareAllMarket=false;
                    }
                    /** 
                     * OTA_ResRetrieveRS/ReservationsList/HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/ 
                     * @ShareAllMarketInd
                     */
                    try {
                        companyInfoType = profile.getCompanyInfo();
                    } catch (Exception e) {
                        //addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/CompanyInfo is invalid");
                        //return res;
                    }

                    try {
                        companyNames = companyInfoType.getCompanyName();
                    } catch (Exception e) {
                        //addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/CompanyInfo/CompanyName is invalid");
                        //return res;
                    }

                    if (companyNames != null) {
                        if (companyNames.size() == 1) {
                            companyNameType = companyNames.get(0);

                            try {
                                companyName = companyNameType.getValue().toString();
                            } catch (Exception e) {
                                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/CompanyInfo/CompanyName is invalid");
                                return res;
                            }
                        } else {
                            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Size of HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/CompanyInfo/CompanyName must be 1");
                            return res;
                        } // CompanyName Size
                    } // CompanyName

                    profileCustomer.put("reservation_company", companyName);

                    try {
                        customer = profile.getCustomer();
                    } catch (Exception e) {
                        addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer is invalid");
                        return res;
                    }

                    try {
                        language = customer.getLanguage();
                    } catch (Exception e) {
                        addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/@Language is invalid");
                        return res;
                    }
                    //@@
                    profileCustomer.put("reservation_guest_language", language);
                    reservation.put("reservation_guest_language", language);

                    // PersonName
                    try {
                        personName = customer.getPersonName();
                    } catch (Exception e) {
                        addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/PersonName is invalid");
                        return res;
                    }

                    if (personName.size() == 1) {
                        List<String> givenNames = null;
                        PersonNameType personNameType = personName.get(0);

                        try {
                            givenNames = personNameType.getGivenName();
                        } catch (Exception e) {
                            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/PersonName/GivenName is invalid");
                            return res;
                        }

                        if (givenNames.size() == 1) {
                            givenName = givenNames.get(0).toString();
                        } else {
                            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Too many HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/PersonName/GivenName elements");
                            return res;
                        }

                        if (givenName.isEmpty()) {
                            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/PersonName/GivenName is empty");
                            return res;
                        }

                        profileCustomer.put("reservation_name", givenName);

                        try {
                            surName = personNameType.getSurname();
                        } catch (Exception e) {
                            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/PersonName/GivenName is invalid");
                            return res;
                        }

                        if (surName.isEmpty()) {
                            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/PersonName/SurName is empty");
                            return res;
                        }

                        profileCustomer.put("reservation_surname", surName);
                    } else {
                        addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Too many HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/PersonName elements");
                        return res;
                    } // PersonName Size

                    // Telephone
                    try {
                        telephones = customer.getTelephone();
                    } catch (Exception e) {
                        //addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "Too many HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/Telephone is invalid");
                        //return res;
                    }

                    profileCustomer.put("reservation_phone", "");
                    profileCustomer.put("reservation_fax", "");
                    profileCustomer.put("reservation_mobile", "");

                    if (telephones != null) {
                        if (telephones.size() > 1 && telephones.size() <= 3) {
                            for (CustomerType.Telephone telephone : telephones) {
                                String phoneLocationType = null;
                                String phoneTechType = null;
                                String phoneNumber = null;

                                try {
                                    phoneLocationType = telephone.getPhoneLocationType();
                                } catch (Exception e) {
                                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/Telephone/@PhoneLocationType is invalid");
                                    return res;
                                }

                                try {
                                    phoneTechType = telephone.getPhoneTechType();
                                } catch (Exception e) {
                                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/Telephone/@PhoneTechType is invalid");
                                    return res;
                                }

                                try {
                                    phoneNumber = telephone.getPhoneNumber();
                                } catch (Exception e) {
                                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/Telephone is invalid");
                                    return res;
                                }

                                // Solo combinazioni possibili
                                if ((phoneLocationType.equals(Facilities.OTA_PLT_HOME)
                                        && phoneTechType.equals(Facilities.OTA_PTT_VOICE))
                                        || (phoneLocationType.equals(Facilities.OTA_PLT_HOME)
                                        && phoneTechType.equals(Facilities.OTA_PTT_FAX))
                                        || (phoneLocationType.equals(Facilities.OTA_PLT_MOBILE)
                                        && phoneTechType.equals(Facilities.OTA_PTT_VOICE))) {
                                    // Numero valido
                                } else {
                                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/Telephone has invalid attributes");
                                    return res;
                                }

                                if (phoneLocationType.equals(Facilities.OTA_PLT_HOME)) {
                                    if (phoneTechType.equals(Facilities.OTA_PTT_VOICE)) {
                                        profileCustomer.put("reservation_phone", phoneNumber);
                                    } else {
                                        profileCustomer.put("reservation_fax", phoneNumber);
                                    }
                                } else {
                                    profileCustomer.put("reservation_mobile", phoneNumber);
                                }
                            } // For
                        } else {
                            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Too many or too few HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/Telephone elements");
                            return res;
                        } // Telephone size
                    } // Telephone

                    // Email
                    try {
                        emails = customer.getEmail();
                    } catch (Exception e) {
                        addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/Email is invalid");
                        return res;
                    }

                    if (emails.size() == 1) {
                        try {
                            email = emails.get(0).getValue().toString();
                        } catch (Exception e) {
                            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/Email is invalid");
                            return res;
                        }

                        if (email.isEmpty()) {
                            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/Email is empty");
                            return res;
                        }
                    } else {
                        addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Too many HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/Email elements");
                        return res;
                    } // Email Size

                    profileCustomer.put("reservation_email", email);

                    // Address
                    try {
                        addresses = customer.getAddress();
                    } catch (Exception e) {
                        addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/Address is invalid");
                        return res;
                    }

                    if (addresses.size() == 1) {
                        try {
                            addressLines = addresses.get(0).getAddressLine();
                        } catch (Exception e) {
                            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/Address/AddressLine is invalid");
                            return res;
                        }

                        if (addressLines.size() == 1) {
                            addressLine = addressLines.get(0).toString();

                            if (addressLine.isEmpty()) {
                                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/Address/AddressLine is empty");
                                return res;
                            }
                        } else {
                            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Too many HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/Address/AddressLine elements");
                            return res;
                        }

                        profileCustomer.put("reservation_address", addressLine);

                        // CityName
                        try {
                            cityName = addresses.get(0).getCityName().toString();
                        } catch (Exception e) {
                            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/Address/CityName is invalid");
                            return res;
                        }

                        profileCustomer.put("reservation_city", cityName);

                        // PostalCode
                        try {
                            postalCode = addresses.get(0).getPostalCode().toString();
                        } catch (Exception e) {
                            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/Address/PostalCode is invalid");
                            return res;
                        }

                        profileCustomer.put("reservation_zipcode", postalCode);

                        // StateProv
                        try {
                            stateProvType = addresses.get(0).getStateProv();
                        } catch (Exception e) {
                            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/Address/StateProv is invalid");
                            return res;
                        }

                        try {
                            stateProv = stateProvType.getValue();
                        } catch (Exception e) {
                            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/Address/StateProv is invalid");
                            return res;
                        }

                        profileCustomer.put("reservation_state", stateProv);

                        // CountryName
                        try {
                            countryNameType = addresses.get(0).getCountryName();
                        } catch (Exception e) {
                            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/Address/CountryName is invalid");
                            return res;
                        }

                        try {
                            countryName = countryNameType.getValue();
                        } catch (Exception e) {
                            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/Address/CountryName is invalid");
                            return res;
                        }

                        try {
                            countryCode = countryNameType.getCode();
                        } catch (Exception e) {
                            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/Address/CountryName/@Code is invalid");
                            return res;
                        }

                        profileCustomer.put("reservation_country", countryCode);
                    } else {
                        addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Too many HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo/Profile/Customer/Address elements");
                        return res;
                    } // AddressSize
                } else {
                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Too many HotelReservations/HotelReservation/ResGuests/ResGuest/Profiles/ProfileInfo elements.");
                    return res;
                } // ProfileInfoSize
            } else {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Too many HotelReservations/HotelReservation/ResGuests/ResGuest elements");
                return res;
            } // ResGuest Size

            // *****************************************************************
            // HotelReservation/ResGlobalInfo
            // *****************************************************************
            ResGlobalInfoType resGlobalInfo = null;

            try {
                resGlobalInfo = hotelReservation.getResGlobalInfo();
            } catch (Exception e) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_HOTEL_CODE, "HotelReservations/HotelReservation/ResGlobalInfo is null");
                return res;
            }

            setGuestCount(resGlobalInfo);
            if (thereIsError()) {
                return res;
            }
            setTimeSpan(resGlobalInfo);
            if (thereIsError()) {
                return res;
            }
            setComment(resGlobalInfo);
            if (thereIsError()) {
                return res;
            }
            setDepositPayment(resGlobalInfo);
            if (thereIsError()) {
                return res;
            }
            setCancelPanalty(resGlobalInfo);
            if (thereIsError()) {
                return res;
            }
            setTotal(resGlobalInfo);
            if (thereIsError()) {
                return res;
            }
            setHotelReservationID(resGlobalInfo);
            if (thereIsError()) {
                return res;
            }

            Integer result=fillSqlData();
            
            if(result!=null && result.intValue()>0){
                Vector parameters=new Vector();   
                Integer subscriptionType = 0;
                
                if(bShareAllMarket )
                    subscriptionType=8;
                
                parameters.add(new Integer(hotelCode)); //1
                parameters.add(result); //2 
                parameters.add(email);  //3
                parameters.add(subscriptionType);  //4
                     
                try { 
                    rpcClient.execute("backend.subscribeEmailToMailOneContactLists", parameters); 
                } catch (Exception e) {

                }
            }
            

            if (!thereIsError()) {
                res.setSuccess(new SuccessType());
            }

            logInfoRequest(logData);
        }        
        
        return res;
    }

//    private void setUniqueID(HotelReservationType hotelReservationType) {
//        List<UniqueIDType> uniqueIDs = null;
//        String reservationId = null;
//        String idContext = null;
//        String type = null;
//
//        try {
//            uniqueIDs = hotelReservationType.getUniqueID();
//        } catch (Exception e) {
//            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/UniqueID is invalid");
//            return;
//        }
//
//        if (uniqueIDs != null) {
//            if (uniqueIDs.size() == 1) {
//                try {
//                    idContext = uniqueIDs.get(0).getIDContext().toString();
//                } catch (Exception e) {
//                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/UniqueID/@ID_Context is invalid");
//                    return;
//                }
//
//                try {
//                    reservationId = uniqueIDs.get(0).getID().toString();
//                } catch (Exception e) {
//                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/UniqueID/@ID is invalid");
//                    return;
//                }
//
//                try {
//                    type = uniqueIDs.get(0).getType().toString();
//                } catch (Exception e) {
//                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/UniqueID/@Type is invalid");
//                    return;
//                }
//
//                if (!type.equals(Facilities.OTA_UIT_HOTEL)) {
//                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/UniqueID/@Type value not allow");
//                    return;
//                }
//            } else {
//                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_HOTEL_CODE, "Too many HotelReservations/HotelReservation/UniqueID elements");
//                return;
//            }
//        }
//    }

    private void setBasicPropertyInfo(RoomStaysType.RoomStay roomStayType) {
        try {
            hotelCode = roomStayType.getBasicPropertyInfo().getHotelCode();
        } catch (Exception e) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_INVALID_HOTEL_CODE, "HotelReservations/HotelReservation/RoomStays/RoomStay/BasciPropertyInfo/@HotelCode is invalid");
            return;
        }

        if (hotelCode == null) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_INVALID_HOTEL_CODE, "HotelReservations/HotelReservation/RoomStays/RoomStay/BasciPropertyInfo/@HotelCode is invalid");
            return;
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

        if (thereIsError()) {
            return;
        }
    }

    private void setRoomType(RoomStaysType.RoomStay roomStayType, int j) {
        List<RoomTypeType> roomTypes = null;       

        try {
            roomTypes = roomStayType.getRoomTypes().getRoomType();
        } catch (Exception e) {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/RoomStays/RoomStay/RoomTypes is invalid");
            return;
        }

        if (roomTypes.size() == 1) {
            //for (RoomTypeType roomType : roomTypes) {
            RoomTypeType roomType = roomTypes.get(0);
            String roomTypeCode = null;
            String roomDescriptionName = null;
            roomType.setNumberOfUnits( BigInteger.valueOf(1)   );
            // Room type in ABS => '---'
            try {
                roomTypeCode = roomType.getRoomTypeCode().toString();
            } catch (Exception e) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/RoomTypes/RoomType/@RoomTypeCode is invalid");
                return;
            }

            try {
                roomDescriptionName = roomType.getRoomDescription().getName().toString();
            } catch (Exception e) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/RoomTypes/RoomType/RoomDescription/@Name is null");
                return;
            }

            numRooms = numRooms.add(new BigInteger("1"));
            reservationDetailArray.put("reservation_detail_name", roomDescriptionName);
            //}
        } else {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Too few HotelReservations/HotelReservation/RoomStays/RoomStay/RoomTypes elements");
            return;
        } // RoomTypes size
    }

    private void setRatePlan(RoomStaysType.RoomStay roomStayType, int j) {
        List<RatePlanType> ratePlans = null;

        try {
            ratePlans = roomStayType.getRatePlans().getRatePlan();
        } catch (Exception e) {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/RoomStays/RoomStay/RatePlans is invalid");
        }

        if (ratePlans.size() == 1) {
            //for (RatePlanType ratePlan : ratePlans) {
            RatePlanType ratePlan = ratePlans.get(0);
            String ratePlanType = null;
            String ratePlanCode = null;
            RatePlanType.MealsIncluded mealsIncluded = null;
            String mealPlanCodes = null;

            // Listino
            try {
                ratePlanCode = ratePlan.getRatePlanCode().toString();
            } catch (Exception e) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/RatePlans/RatePlan/@RatePlanCode is null");
                return;
            }

            try {
                ratePlanType = ratePlan.getRatePlanType().toString();
            } catch (Exception e) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/RatePlans/RatePlan/@RatePlanType is null");
                return;
            }

            if (!ratePlanType.equals(Integer.toString(Facilities.OTA_RPT_1)) && !ratePlanType.equals(Integer.toString(Facilities.OTA_RPT_3))) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/RatePlans/RatePlan/@RatePlanType value is invalid");
                return;
            }

            try {
                mealsIncluded = ratePlan.getMealsIncluded();

                if (mealsIncluded.getMealPlanCodes().size() > 1) {
                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Too many HotelReservations/HotelReservation/RoomStays/RoomStay/RatePlans/RatePlan/MealsIncluded elements");
                    return;
                }
                mealPlanCodes = mealsIncluded.getMealPlanCodes().get(0).toString();

                // Conversione in codice MM-ONE
            } catch (Exception e) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/RatePlans/RatePlan/MealsIncluded is invalid");
                return;
            }

            int iMealPlanCodes = new Integer(mealPlanCodes);

            reservationDetailArray.put("reservation_detail_room_board", Facilities.MEAL_MAP.get(Facilities.MPT_TO_MM[iMealPlanCodes]));
            //}
        } else {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Too many HotelReservations/HotelReservation/RoomStays/RoomStay/RatePlans elements");
            return;
        } // RatePlans size
    }

    private void setRoomRate(RoomStaysType.RoomStay roomStayType, int j) {
        List<RoomRates.RoomRate> roomRates = null;

        try {
            roomRates = roomStayType.getRoomRates().getRoomRate();
        } catch (Exception e) {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/RoomStays/RoomStay/RoomRates is invalid");
        }

        if (roomRates.size() == 1) {
            //for (RoomRates.RoomRate roomRate : roomRates) {
            RoomRates.RoomRate roomRate = roomRates.get(0);
            int cAdult = 0;
            //String roomTypeCode = null;
            BigInteger numberOfUnits = BigInteger.valueOf(1);
            //String ratePlanCode = null;
            List<Rate> rate = null;
            BigDecimal amountAfterTax = null;
            String currencyCode = null;
            List<RoomRates.RoomRate.GuestCounts.GuestCount> guestCounts = null;

//                    // Listino
//                    try {
//                        roomTypeCode = roomRate.getRoomTypeCode().toString();
//                    } catch (Exception e) {
//                        addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/RoomRates/RoomRate/@RoomTypeCode is invalid");
//                        return;
//                    }
//
//                    try {
//                        numberOfUnits = roomRate.getNumberOfUnits();
//                    } catch (Exception e) {
//                        addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/RoomRates/RoomRate/@NumberOfUnits is invalid");
//                        return;
//                    }
//
//                    try {
//                        ratePlanCode = roomRate.getRatePlanCode().toString();
//                    } catch (Exception e) {
//                        addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/RoomRates/RoomRate/@RatePlanCode is invalid");
//                        return;
//                    }

            try {
                rate = roomRate.getRates().getRate();
            } catch (Exception e) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/RoomStays/RoomStay/RoomRates/RoomRate/Rates/Rate is invalid");
                return;
            }

            if (rate.size() > 1) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Too many HotelReservations/HotelReservation/RoomStays/RoomStay/RoomRates/RoomRate/Rates/Rate");
                return;
            }

            try {
                amountAfterTax = rate.get(0).getTotal().getAmountAfterTax();
            } catch (Exception e) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_PRICE_CANNOT_BE_VALIDATED, "HotelReservations/HotelReservation/RoomStays/RoomStay/RoomRates/RoomRate/Rates/Rate/@AmountAfterTax is invalid");
                return;
            }

            try {
                currencyCode = rate.get(0).getTotal().getCurrencyCode();
            } catch (Exception e) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_PRICE_CANNOT_BE_VALIDATED, "HotelReservations/HotelReservation/RoomStays/RoomStay/RoomRates/RoomRate/Rates/Rate/@CurrencyCode is invalid");
                return;
            }

            if (!currencyCode.equals(Facilities.DEFAULT_CURRENCY_CODE)) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_PRICE_CANNOT_BE_VALIDATED, "HotelReservations/HotelReservation/RoomStays/RoomStay/RoomRates/RoomRate/Rates/Rate/@CurrencyCode value is not allow");
                return;
            }

            reservationDetailArray.put("reservation_detail_price", amountAfterTax.toString());

            try {
                guestCounts = roomRate.getGuestCounts().getGuestCount();
            } catch (Exception e) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/RoomStays/RoomStay/RoomRates/RoomRate/GuestCounts is invalid");
                return;
            }

            String guestAdult = null;
            String guestChildren = null;
            for (RoomRates.RoomRate.GuestCounts.GuestCount guestCount : guestCounts) {
                String ageQualifyngCode = null;
                int age = 18;
                int count = 0;

                try {
                    ageQualifyngCode = guestCount.getAgeQualifyingCode().toString();
                } catch (Exception e) {
                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/RoomRates/RoomRate/Rates/Rate/GuestCounts/GuestCount/@AgeQualifyngCode is invalid");
                    return;
                }

                if (!ageQualifyngCode.equals(Facilities.ADULT_QUALYFING_CODE) && !ageQualifyngCode.equals(Facilities.CHILD_QUALYFING_CODE)) {
                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/RoomRates/RoomRate/Rates/Rate/GuestCounts/GuestCount/@AgeQualifyngCode value is invalid");
                    return;
                }

                try {
                    age = guestCount.getAge();
                } catch (Exception e) {
                    //addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/RoomRates/RoomRate/Rates/Rate/GuestCounts/GuestCount/@Age is invalid");
                    //return;
                }

                if (age < 0) {
                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/RoomRates/RoomRate/Rates/Rate/GuestCounts/GuestCount/@Age value is invalid");
                    return;
                }

                if (age < 18 && ageQualifyngCode.equals(Facilities.ADULT_QUALYFING_CODE)) {
                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/RoomRates/RoomRate/Rates/Rate/GuestCounts/GuestCount/@Age value is invalid");
                    return;
                }

                try {
                    count = guestCount.getCount();
                } catch (Exception e) {
                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/RoomRates/RoomRate/Rates/Rate/GuestCounts/GuestCount/@Count is invalid");
                    return;
                }

                if (count < 1) { // numero ospiti > 1
                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/RoomRates/RoomRate/Rates/Rate/GuestCounts/GuestCount/@Count less than 1");
                    return;
                }

                if (ageQualifyngCode.equals(Facilities.ADULT_QUALYFING_CODE) && cAdult == 0) {
                    cAdult = 1;
                } else if (ageQualifyngCode.equals(Facilities.ADULT_QUALYFING_CODE) && cAdult == 1) {
                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "More than one element HotelReservations/HotelReservation/RoomStays/RoomStay/RoomRates/RoomRate/Rates/Rate/GuestCounts/GuestCount/@AgeQualifyngCode with value 10. Please use @Count.");
                    return;
                }

                if (ageQualifyngCode.equals(Facilities.ADULT_QUALYFING_CODE)) {
                    guestAdult = "A:" + Integer.toString(count);
                }

                if (ageQualifyngCode.equals(Facilities.CHILD_QUALYFING_CODE)) {
                    String _guestChildren[] = new String[count];
                    for (int i = 0; i < count; i++) {
                        _guestChildren[i] = "C:" + Integer.toString(age);
                    }
                    guestChildren = StringUtils.join(_guestChildren, "-");
                }

            } // For

            if (cAdult == 0) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "No HotelReservations/HotelReservation/RoomStays/RoomStay/RoomRates/RoomRate/Rates/Rate/GuestCounts/GuestCount/@AgeQualifyngCode equals to 10");
                return;
            }

            if (guestChildren == null) {
                guestChildren = "C:-";
            }

                reservationDetailArray.put("reservation_detail_room_guest", guestAdult + "|" + guestChildren);
            //}
        } else {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Too few HotelReservations/HotelReservation/RoomStays/RoomStay/RoomRates/RoomRate elements");
            return;
        } // RoomRates size
    }

    private void setGuestCount(RoomStaysType.RoomStay roomStayType) {
        List<GuestCount> guestCounts = null;
        String ageQualifyngCode = null;
        int count = 0;
        String resGuestRPH = null;

        try {
            guestCounts = roomStayType.getGuestCounts().getGuestCount();
        } catch (Exception e) {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/RoomStays/RoomStay/GuestCounts is invalid");
            return;
        }

        for (GuestCount guestCount : guestCounts) {
            int age = 18;
            try {
                ageQualifyngCode = guestCount.getAgeQualifyingCode().toString();
            } catch (Exception e) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/GuestCounts/GuestCount/@AgeQualyfingCode is invalid");
                return;
            }

            if (!ageQualifyngCode.equals(Facilities.ADULT_QUALYFING_CODE) && !ageQualifyngCode.equals(Facilities.CHILD_QUALYFING_CODE)) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/GuestCounts/GuestCount/@AgeQualifyngCode value is not allow");
                return;
            }

            try {
                age = guestCount.getAge();
            } catch (Exception e) {
                //addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/GuestCounts/GuestCount/@Age is invalid");
                //return;
            }

            if (age < 0) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/GuestCounts/GuestCount/@Age less than 1");
                return;
            }

            if (age < 18 && ageQualifyngCode.equals(Facilities.ADULT_QUALYFING_CODE)) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/GuestCounts/GuestCount/@Age value is invalid");
                return;
            }

            try {
                count = guestCount.getCount();
            } catch (Exception e) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/GuestCounts/GuestCount/@Count is invalid");
                return;
            }

            if (count < 1) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/GuestCounts/GuestCount/@Count less than 1");
                return;
            }

            //try {
            //    resGuestRPH = guestCount.getResGuestRPH().toString();
            //} catch (Exception e) {
            //    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/GuestCounts/GuestCount/@ResGuestRPH is invalid");
            //    return;
            //}

            //if (resGuestRPH != null) {
            //    if (resGuestRPH.isEmpty()) {
            //        addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/GuestCounts/GuestCount/@ResGuestRPH value is invalid");
            //        return;
            //    }
            //}
        }
    }

    private void setTimeSpan(RoomStaysType.RoomStay roomStayType) {
        String start = null;
        String end = null;
        Date dStartDate = null;
        Date dEndDate = null;
        
        try {
            start = roomStayType.getTimeSpan().getStart().toString();
        } catch (Exception e) {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_START_DATE_INVALID, "HotelReservations/HotelReservation/RoomStays/RoomStay/TimeSpan/@Start is invalid");
            return;
        }

        try {
            end = roomStayType.getTimeSpan().getEnd().toString();
        } catch (Exception e) {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_END_DATE_INVALID, "HotelReservations/HotelReservation/RoomStays/RoomStay/TimeSpan/@End is invalid");
            return;
        }

        dStartDate = getDate(start, true);
        dEndDate = getDate(end, false);
        
        if (dStartDate.getTime() > dEndDate.getTime()) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_END_DATE_INVALID, "HotelReservations/HotelReservation/RoomStays/RoomStay/TimeSpan/@Start greater than HotelReservations/HotelReservation/RoomStays/RoomStay/TimeSpan/@End");
            return;
        }
    }

//    private void setTotal(RoomStaysType.RoomStay roomStayType, int j) {
//        BigDecimal amountAfterTax = null;
//        String currencyCode = null;
//
//        try {
//            amountAfterTax = roomStayType.getTotal().getAmountAfterTax();
//        } catch (Exception e) {
//            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_PRICE_CANNOT_BE_VALIDATED, "HotelReservations/HotelReservation/RoomStays/RoomStay/Total/@AmountAfterTax is invalid");
//            return;
//        }
//
//        try {
//            currencyCode = roomStayType.getTotal().getCurrencyCode();
//        } catch (Exception e) {
//            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_PRICE_CANNOT_BE_VALIDATED, "HotelReservations/HotelReservation/RoomStays/RoomStay/Total/@CurrencyCode is invalid");
//            return;
//        }
//
//        if (!currencyCode.equals(Facilities.DEFAULT_CURRENCY_CODE)) {
//            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_PRICE_CANNOT_BE_VALIDATED, "HotelReservations/HotelReservation/RoomStays/RoomStay/Total/@CurrencyCode value is not allow");
//            return;
//        }
//
//    }

    private void setGuestCount(ResGlobalInfoType resGlobalInfo) {
        List<GuestCount> guestCounts = null;
        String ageQualifyngCode = null;
        int count = 0;       
        int cAdult = 0;
        int numPersons = 0;
        
        try {
            guestCounts = resGlobalInfo.getGuestCounts().getGuestCount();
        } catch (Exception e) {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/ResGlobalInfo/GuestCounts is invalid");
            return;
        }

        for (GuestCount guestCount : guestCounts) {
            int age = 18;
            try {
                ageQualifyngCode = guestCount.getAgeQualifyingCode().toString();
            } catch (Exception e) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGlobalInfo/GuestCounts/GuestCount/@AgeQualyfingCode is invalid");
                return;
            }

            if (!ageQualifyngCode.equals(Facilities.ADULT_QUALYFING_CODE) && !ageQualifyngCode.equals(Facilities.CHILD_QUALYFING_CODE)) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGlobalInfo/GuestCounts/GuestCount/@AgeQualifyngCode value is not allow");
                return;
            }

            try {
                age = guestCount.getAge();
            } catch (Exception e) {
                //addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/GuestCounts/GuestCount/@Age is invalid");
                //return;
            }

            if (age < 0) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGlobalInfo/GuestCounts/GuestCount/@Age less than 1");
                return;
            }

            if (age < 18 && ageQualifyngCode.equals(Facilities.ADULT_QUALYFING_CODE)) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGlobalInfo/GuestCounts/GuestCount/@Age value is invalid");
                return;
            }

            try {
                count = guestCount.getCount();
            } catch (Exception e) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGlobalInfo/GuestCounts/GuestCount/@Count is invalid");
                return;
            }

            if (count < 1) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGlobalInfo/GuestCounts/GuestCount/@Count less than 1");
                return;
            }

            if (ageQualifyngCode.equals(Facilities.ADULT_QUALYFING_CODE) && cAdult == 0) {
                cAdult = 1;
            } else if (ageQualifyngCode.equals(Facilities.ADULT_QUALYFING_CODE) && cAdult == 1) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "More than one element HotelReservations/HotelReservation/ResGlobalInfo/GuestCounts/GuestCount/@AgeQualifyngCode with value 10. Please use @Count.");
                return;
            }

            numPersons += count;
            //try {
            //    resGuestRPH = guestCount.getResGuestRPH().toString();
            //} catch (Exception e) {
            //    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/RoomStay/GuestCounts/GuestCount/@ResGuestRPH is invalid");
            //    return;
            //}

            //if (resGuestRPH != null) {
            //    if (resGuestRPH.isEmpty()) {
            //        addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/RoomStays/GuestCounts/GuestCount/@ResGuestRPH value is invalid");
            //        return;
            //    }
            //}
        } // For

        if (cAdult == 0) {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "No HotelReservations/HotelReservation/ResGlobalInfo/GuestCounts/GuestCount/@AgeQualifyngCode equals to 10");
            return;
        }

        reservation.put("reservation_numpersons", Integer.toString(numPersons));
        logData.put("Num Persons", Integer.toString(numPersons));
    }

    // prenotazione globale
    private void setTimeSpan(ResGlobalInfoType resGlobalInfo) {
        String start = null;
        String end = null;
        Date dStartDate = null;
        Date dEndDate = null;
        long dateDiff;
        
        try {
            start = resGlobalInfo.getTimeSpan().getStart().toString();
        } catch (Exception e) {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_START_DATE_INVALID, "HotelReservations/HotelReservation/ResGlobalInfo/TimeSpan/@Start is null");
            return;
        }
        
        if(start==null){
         addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_START_DATE_INVALID, "HotelReservations/HotelReservation/ResGlobalInfo/TimeSpan/@Start is null");
         return;
        }
        
        try {
            end = resGlobalInfo.getTimeSpan().getEnd().toString();
        } catch (Exception e) {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_END_DATE_INVALID, "HotelReservations/HotelReservation/ResGlobalInfo/TimeSpan/@End is null");
            return;
        }

        if(end==null){
         addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_END_DATE_INVALID, "HotelReservations/HotelReservation/ResGlobalInfo/TimeSpan/@End is null");
         return;
        }
        
        logData.put("Start", start);
        logData.put("End", end);
        
        dStartDate = getDate(start, true);
        dEndDate = getDate(end, false);
        
        
        try{
            if (dStartDate.getTime() > dEndDate.getTime()) {
                addError(Facilities.EWT_UNKNOWN, Facilities.ERR_END_DATE_INVALID, "HotelReservations/HotelReservation/ResGlobalInfo/TimeSpan/@Start greater than HotelReservations/HotelReservation/ResGlobalInfo/TimeSpan/@End");
                return;
            }
        }catch(Exception e1){
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_DATE,  e1.getMessage()  );
            return;
        }
                
        dateDiff = dEndDate.getTime() - dStartDate.getTime();

        reservation.put("reservation_checkin_date", start);
        reservation.put("reservation_checkout_date", end);
        reservation.put("reservation_numnights", Long.toString(dateDiff/(1000*60*60*24)));

    }

    private void setComment(ResGlobalInfoType resGlobalInfo) {
        List<Comment> comments = null;
        List<JAXBElement<?>> elements = null;
        FormattedTextTextType comment = null;
        String textFormat = null;

        try {
            comments = resGlobalInfo.getComments().getComment();
        } catch (Exception e) {
            //addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/ResGlobalInfo/Comments is invalid");
            //return;
        }

        if (comments != null) {
            if (comments.size() == 1) {
                elements = comments.get(0).getTextOrImageOrURL();

                if (elements.size() == 1) {
                    comment = (FormattedTextTextType) elements.get(0).getValue();
                    textFormat = comment.getTextFormat().toString();

                    if (!textFormat.equals(Facilities.TEXT_FORMAT_PLAIN_TEXT)) {
                        addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGlobalInfo/Comments/Comment/Text is invalid");
                        return;
                    }
                } else {
                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Size of HotelReservations/HotelReservation/ResGlobalInfo/Comments/Comment must be 1");
                    return;
                }
            } else {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Size of HotelReservations/HotelReservation/ResGlobalInfo/Comments/Comment must be 1");
                return;
            }

            reservation.put("reservation_note", comment.getValue());
        } else {
            //addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGlobalInfo/Comments/Comment is null");
            //return;
        }
    }

    private void setDepositPayment(ResGlobalInfoType resGlobalInfo) {
        List<GuaranteePayment> guaranteePayments = null;
        List<ParagraphType> descriptions = null;
        List<JAXBElement<?>> texts = null;
        FormattedTextTextType text = null;
        String textFormat = null;

        try {
            guaranteePayments = resGlobalInfo.getDepositPayments().getGuaranteePayment();
        } catch (Exception e) {
            //addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/ResGlobalInfo/DeposityPayments/GuaranteePayment is invalid");
            //return;
        }

        if (guaranteePayments != null) {
            if (guaranteePayments.size() == 1) {
                try {
                    descriptions = guaranteePayments.get(0).getDescription();
                } catch (Exception e) {
                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGlobalInfo/DeposityPayments/GuaranteePayment/Description is invalid");
                    return;
                }

                if (descriptions.size() == 1) {
                    try {
                        texts = descriptions.get(0).getTextOrImageOrURL();
                    } catch (Exception e) {
                        addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGlobalInfo/DeposityPayments/GuaranteePayment/Description/Text is invalid");
                        return;
                    }

                    if (texts != null) {
                        if (texts.size() == 1) {
                            try {
                                text = (FormattedTextTextType) texts.get(0).getValue();
                                textFormat = text.getTextFormat().toString();

                                if (!textFormat.equals(Facilities.TEXT_FORMAT_PLAIN_TEXT)) {
                                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGlobalInfo/DeposityPayments/GuaranteePayment/Description/Text/@TextFormat is invalid");
                                    return;
                                }
                            } catch (Exception e) {
                                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGlobalInfo/DeposityPayments/GuaranteePayment/Description/Text is invalid");
                                return;
                            }
                        }

                        reservation.put("reservation_detail_payment_policy", text.getValue());
                    }
                } else {
                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Size of HotelReservations/HotelReservation/ResGlobalInfo/DeposityPayments/GuaranteePayment/Description must be 1");
                    return;
                }
            } else {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Size of HotelReservations/HotelReservation/ResGlobalInfo/DepositPayments/GuaranteePayment must be 1");
                return;
            }
        } // GuaranteePayments
    }

    private void setCancelPanalty(ResGlobalInfoType resGlobalInfo) {
        List<CancelPenaltyType> cancelPenalties = null;
        List<ParagraphType> penaltyDescriptions = null;
        List<JAXBElement<?>> texts = null;
        FormattedTextTextType text = null;
        String textFormat = null;

        try {
            cancelPenalties = resGlobalInfo.getCancelPenalties().getCancelPenalty();
        } catch (Exception e) {
            //addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/ResGlobalInfo/CancelPenalties/CancelPenalty is invalid");
            //return;
        }

        if (cancelPenalties != null) {
            if (cancelPenalties.size() == 1) {
                try {
                    penaltyDescriptions = cancelPenalties.get(0).getPenaltyDescription();
                } catch (Exception e) {
                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGlobalInfo/CancelPenalties/CancelPenalty/PenaltyDescription is invalid");
                    return;
                }

                if (penaltyDescriptions.size() == 1) {
                    try {
                        texts = penaltyDescriptions.get(0).getTextOrImageOrURL();
                    } catch (Exception e) {
                        addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGlobalInfo/CancelPenalties/CancelPenalty/PenaltyDescription/Text is invalid");
                        return;
                    }

                    if (texts.size() == 1) {
                        try {
                            text = (FormattedTextTextType) texts.get(0).getValue();
                            textFormat = text.getTextFormat().toString();

                            if (!textFormat.equals(Facilities.TEXT_FORMAT_PLAIN_TEXT)) {
                                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGlobalInfo/CancelPenalties/CancelPenalty/PenaltyDescription/Text/@TextFormat is invalid is invalid");
                                return;
                            }

                        } catch (Exception e) {
                            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGlobalInfo/CancelPenalties/CancelPenalty/PenaltyDescription/Text is invalid");
                            return;
                        }

                        reservation.put("reservation_detail_cancellation_policy", text.getValue());
                    } else {
                        addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Size of HotelReservations/HotelReservation/ResGlobalInfo/CancelPenalties/CancelPenalty/PenaltyDescription/Text must be 1");
                        return;
                    }
                } else {
                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Size of HotelReservations/HotelReservation/ResGlobalInfo/CancelPenalties/CancelPenalty/PenaltyDescription must be 1");
                    return;
                }
            } else {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Size of HotelReservations/HotelReservation/ResGlobalInfo/CancelPenalties/CancelPenalty must be 1");
                return;
            }
        } // CancelPenalties
    }

    private void setTotal(ResGlobalInfoType resGlobalInfo) {
        TotalType total = null;
        BigDecimal amountAfterTax = null;
        String currencyCode = null;
        
        total = resGlobalInfo.getTotal();

        try {
            amountAfterTax = total.getAmountAfterTax();
        } catch (Exception e) {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_PRICE_CANNOT_BE_VALIDATED, "HotelReservations/HotelReservation/ResGlobalInfo/AmountAfterTax is invalid");
            return;
        }

        try {
            currencyCode = total.getCurrencyCode();
        } catch (Exception e) {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_PRICE_CANNOT_BE_VALIDATED, "HotelReservations/HotelReservation/ResGlobalInfo/AmountAfterTax/@CurrencyCode is invalid");
            return;
        }

        if (!currencyCode.equals(Facilities.DEFAULT_CURRENCY_CODE)) {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_PRICE_CANNOT_BE_VALIDATED, "HotelReservations/HotelReservation/ResGlobalInfo/AmountAfterTax/@CurrencyCode is not allow");
            return;
        }

        reservation.put("reservation_tot_reservation_price", amountAfterTax.toString());
        logData.put("AmountAfterTax", amountAfterTax.toString());
    }
    
    private void setHotelReservationID(ResGlobalInfoType resGlobalInfo) {
        List<HotelReservationID> hotelReservationIDs = null;
        String resIDType = null;

        try {
            hotelReservationIDs = resGlobalInfo.getHotelReservationIDs().getHotelReservationID();
        } catch (Exception e) {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_NAME_IS_MISSING_OR_INCOMPLETE, "HotelReservations/HotelReservation/ResGlobalInfo/HotelReservationIDS is invalid");
            return;
        }

        if (hotelReservationIDs.size() == 1) {
            try {
                resIDValue = hotelReservationIDs.get(0).getResIDValue().toString(); // codice gestionale
            } catch (Exception e) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGlobalInfo/HotelReservationIDS/HotelReservationID/@ResID_Value is invalid");
                return;
            }

            try {
                resIDType = hotelReservationIDs.get(0).getResIDType().toString(); // vale sempre 10
            } catch (Exception e) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGlobalInfo/HotelReservationIDS/HotelReservationID/@ResID_Type is invalid");
                return;
            }

            if (!resIDType.equals(Facilities.OTA_UIT_HOTEL)) { // valore fisso a 10
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGlobalInfo/HotelReservationIDS/HotelReservatuibID/@ResID_Type must be 10");
                return;
            }

            try {
                resIDDate = hotelReservationIDs.get(0).getResIDDate().toString();
            } catch (Exception e) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGlobalInfo/HotelReservationIDS/HotelReservationID/@ResID_Date is invalid");
                return;
            }

            reservation.put("reservation_number", resIDValue);
            reservation.put("reservation_opened_date", resIDDate);

            logData.put("ResID_Value", resIDValue);
            logData.put("ResID_Date", resIDDate);
        } else {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Size of HotelReservations/HotelReservatio/ResGlobalInfo/HotelReservationIDS/HotelReservationID must be 1");
            return;
        }
    }

    private Integer fillSqlData() {
        Connection conn = null;

        try {
            conn = ds.getConnection();
            conn.setAutoCommit(false);
        } catch (SQLException e) {

        }
        
        String sqlCreateReservationExtended = ""
        + " CREATE TABLE IF NOT EXISTS reservation_extended ( "
        + " reservation_extended_id  int(11) NOT NULL auto_increment, "
        + " reservation_id  int(11) NOT NULL, "
        + " reservation_extended_xml  text, "
        + " PRIMARY KEY (reservation_extended_id) , "
        + " UNIQUE reservation_id (reservation_id), "
        + " CONSTRAINT fkreservation_id_reservation_ext "
        + "   FOREIGN KEY fkreservation_id_reservation (reservation_id) "
        + "   REFERENCES reservation (reservation_id) "
        + "   ON DELETE cascade "
        + "   ON UPDATE cascade "
        + "  ) "
        + " ENGINE = InnoDB "
        + " DEFAULT CHARACTER SET = utf8 ";
        
        String sqlInsReservation = ""
                + " INSERT INTO reservation ("
                + "     portal_id, "
                + "     structure_id, "
                + "     partner_id, "
                + "     guest_id, "
                + "     reservation_insert_type, "
                + "     reservation_number, "
                + "     reservation_pincode, "
                + "     reservation_name, "
                + "     reservation_surname, "
                + "     reservation_company, "
                + "     reservation_email, "
                + "     reservation_address, "
                + "     reservation_city, "
                + "     reservation_state, "
                + "     reservation_zipcode, "
                + "     reservation_country, "
                + "     reservation_phone, "
                + "     reservation_fax, "
                + "     reservation_mobile, "
                + "     reservation_guest_allow, "
                + "     reservation_guest_language, "
                + "     reservation_note, "
                + "     reservation_language, "
                + "     reservation_privacy, "
                + "     reservation_accept_term, "
                + "     reservation_newsletter_subscribe, "
                + "     reservation_newsletter_language, "
                + "     reservation_checkin_date, "
                + "     reservation_checkout_date, "
                + "     reservation_numpersons, "
                + "     reservation_numnights, "
                + "     reservation_numrooms, "
                + "     reservation_tot_reservation_price, "
                + "     reservation_payment_type, "
                + "     reservation_status, "
                + "     reservation_update_allotment, "
                + "     reservation_opened_date, "
                + "     reservation_deleted, "
                + "     reservation_remote_address"
                + ")"
                + " VALUES ("
                + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"
                + ") "
                + "";

        String sqlUpdReservation = ""
                + " UPDATE reservation SET"
                + "     reservation_name = ?, "
                + "     reservation_surname = ?, "
                + "     reservation_company = ?, "
                + "     reservation_email = ?, "
                + "     reservation_address = ?, "
                + "     reservation_city = ?, "
                + "     reservation_state = ?, "
                + "     reservation_zipcode = ?, "
                + "     reservation_country = ?, "
                + "     reservation_phone = ?, "
                + "     reservation_fax = ?, "
                + "     reservation_mobile = ?, "
                + "     reservation_note = ?, "
                + "     reservation_checkin_date = ?, "
                + "     reservation_checkout_date = ?, "
                + "     reservation_numpersons = ?, "
                + "     reservation_numnights = ?, "
                + "     reservation_numrooms = ?, "
                + "     reservation_tot_reservation_price = ?, "
                + "     reservation_remote_address = ?"
                + " WHERE "
                + "     reservation_id = ?"
                + "";

        String sqlDelReservation = ""
                + " UPDATE reservation SET "
                + "     reservation_status = ? "
                + " WHERE "
                + "     reservation_id = ?"
                + "";
        
        String sqlInsReservationDetail = ""
                + " INSERT INTO reservation_detail ("
                + "     reservation_id, "
                + "     reservation_detail_type, "
                + "     reservation_detail_item, "
                + "     reservation_detail_code, "
                + "     reservation_detail_name, "
                + "     reservation_detail_price, "
                + "     reservation_detail_room_id, "
                + "     reservation_detail_list_id, "
                + "     reservation_detail_room_board, "
                + "     reservation_detail_tot_room_price, "
                + "     reservation_detail_room_guest, "
                + "     reservation_detail_cancellation_policy, "
                + "     reservation_detail_payment_policy)"
                + " VALUES ("
                + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? "
                + ") "
                + "";

        String sqlDelReservationDetail = ""
                + " DELETE FROM reservation_detail "
                + "     WHERE "
                + "         reservation_id = ?";

        String sqlInsReservationStatus = ""
                + " INSERT INTO reservation_status ("
                + "     reservation_id, "
                + "     reservation_status_code, "
                + "     reservation_status_date "
                + ")"
                + " VALUES ("
                + "?, ?, ?"
                + ")"
                + "";

        String sqlSelReservation = ""
                + " SELECT reservation_id FROM reservation "
                + "     WHERE reservation_number = ? AND reservation_deleted = ?";
        
        PreparedStatement psSqlInsReservation = null;
        PreparedStatement psSqlInsReservationDetail = null;
        PreparedStatement psSqlInsReservationStatus = null;
        PreparedStatement psSqlDelReservationDetail = null;
        Integer reservationId = 0; 
        
        if (Facilities.RESSTATUS_MAP.get(reservation.get("reservation_status")) == Facilities.RESERVATION_STATUS_CONFIRMED) {
            // Nuova prenotazione
            try {
                //Tabella Reservation
                psSqlInsReservation = conn.prepareStatement(sqlInsReservation, Statement.RETURN_GENERATED_KEYS);
                fillReservation(psSqlInsReservation);
                psSqlInsReservation.execute();

                ResultSet keys = psSqlInsReservation.getGeneratedKeys();
 
                if (keys.next()) {
                    reservationId = keys.getInt(1);
                    logData.put("ReservationId", Integer.toString(reservationId));
                }

                int i = 0;
                Map<String, String> value = new LinkedHashMap<String, String>();
                //Tabella ReservationDetail
                psSqlInsReservationDetail = conn.prepareStatement(sqlInsReservationDetail);
                for (Integer key : reservationDetail.keySet()) {
                    value = reservationDetail.get(key);

                    fillReservationDetail(psSqlInsReservationDetail, reservationId, "R", value, key);
                    psSqlInsReservationDetail.execute();

                    fillReservationDetail(psSqlInsReservationDetail, reservationId, "T", value, key + 1);
                    psSqlInsReservationDetail.execute();
                    i = key + 2;
                }

                // **** GRANTOTAL
                fillReservationDetail(psSqlInsReservationDetail, reservationId, "G", value, i);
                psSqlInsReservationDetail.execute();

                psSqlInsReservationStatus = conn.prepareStatement(sqlInsReservationStatus);
                fillReservationStatus(psSqlInsReservationStatus, reservationId);
                psSqlInsReservationStatus.execute();
            } catch (SQLException e) {
                addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Insert data failed");
                Logger.getLogger(OTAHotelResNotifBuilder.class.getName()).log(Level.SEVERE, null, e);
                DbUtils.rollbackAndCloseQuietly(conn);
                return null;
            } finally {
                
            }
        } else if (Facilities.RESSTATUS_MAP.get(reservation.get("reservation_status")) == Facilities.RESERVATION_STATUS_CANCEL) {
            // Prenotazione cancellata
             

            try {
                reservationId = (Integer) run.query(sqlSelReservation, new ScalarHandler(), reservation.get("reservation_number"), 0);
            } catch (SQLException e) {
                Logger.getLogger(OTAHotelResNotifBuilder.class.getName()).log(Level.SEVERE, null, e);
            }

            if (reservationId == 0) {
                addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGlobalInfo/HotelReservationIDS/HotelReservationID/@ResID_Value is invalid");
                return null;
            }

            logData.put("ReservationId", Integer.toString(reservationId));

            try {
                run.update(sqlDelReservation, Facilities.RESERVATION_STATUS_CANCEL, reservationId);

                psSqlInsReservationStatus = conn.prepareStatement(sqlInsReservationStatus);
                fillReservationStatus(psSqlInsReservationStatus, reservationId);
                psSqlInsReservationStatus.execute();

            } catch (SQLException e) {
                addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Insert data failed");
                Logger.getLogger(OTAHotelResNotifBuilder.class.getName()).log(Level.SEVERE, null, e);
                DbUtils.rollbackAndCloseQuietly(conn);
                return null;
            } finally {
                 
            }
        } else if (Facilities.RESSTATUS_MAP.get(reservation.get("reservation_status")) == Facilities.RESERVATION_STATUS_MODIFIED) {
            // Prenotazione modificata
            

            try {
                reservationId = (Integer) run.query(sqlSelReservation, new ScalarHandler(), reservation.get("reservation_number"), 0);
            } catch (SQLException e) {
                Logger.getLogger(OTAHotelResNotifBuilder.class.getName()).log(Level.SEVERE, null, e);
            }
            
            if(reservationId==null) reservationId=0;
            
            if(reservationId==0)
                logData.put("ReservationId", "null");
            else
                logData.put("ReservationId", Integer.toString(reservationId));
            
            if (reservationId == 0) {
                addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_VALUE, "HotelReservations/HotelReservation/ResGlobalInfo/HotelReservationIDS/HotelReservationID/@ResID_Value is invalid");
                return null;
            }

            try {
                run.update(sqlDelReservation, Facilities.RESERVATION_STATUS_MODIFIED, reservationId);
                run.update(sqlDelReservationDetail, reservationId);
            } catch (SQLException e) {
                addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Insert data failed");
                return null;
            }

            

            int i = 0;
            Map<String, String> value = new LinkedHashMap<String, String>();
            //Tabella ReservationDetail
            try {
                psSqlInsReservationDetail = conn.prepareStatement(sqlInsReservationDetail);
                for (Integer key : reservationDetail.keySet()) {
                    value = reservationDetail.get(key);

                    fillReservationDetail(psSqlInsReservationDetail, reservationId, "R", value, key);
                    psSqlInsReservationDetail.execute();

                    fillReservationDetail(psSqlInsReservationDetail, reservationId, "T", value, key + 1);
                    psSqlInsReservationDetail.execute();
                    i = key + 2;
                }

                // **** GRANTOTAL
                fillReservationDetail(psSqlInsReservationDetail, reservationId, "G", value, i);
                psSqlInsReservationDetail.execute();

                psSqlInsReservationStatus = conn.prepareStatement(sqlInsReservationStatus);
                fillReservationStatus(psSqlInsReservationStatus, reservationId);
                psSqlInsReservationStatus.execute();
            } catch (SQLException e) {
                addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "Insert data failed");
                Logger.getLogger(OTAHotelResNotifBuilder.class.getName()).log(Level.SEVERE, null, e);
                DbUtils.rollbackAndCloseQuietly(conn);
                return null;
            } finally {
                 
            }
        }
        
        String sqlInsReservationExtended = ""
                + " INSERT INTO reservation_extended ( reservation_id,  reservation_extended_xml  )"
                + " VALUES ( ?, ? )"
                + " ON DUPLICATE KEY UPDATE reservation_extended_xml=? " ;
        
        if(this.TpaXml!=null){
            Logger.getLogger(OTAHotelResNotifBuilder.class.getName()).log(Level.INFO,"Updating or inserting tpa-xml for reservation " +  reservationId);
            
            if(!cacheUpdated.containsKey(this.requestorID)){
                Logger.getLogger(OTAHotelResNotifBuilder.class.getName()).log(Level.INFO,"Creating xml table ");            
                cacheUpdated.put(this.requestorID, Boolean.TRUE); 
                try {
                    PreparedStatement pstmt = conn.prepareStatement(sqlCreateReservationExtended);
                    pstmt.executeUpdate();
                } catch (Exception ex) {
                    Logger.getLogger(OTAHotelResNotifBuilder.class.getName()).log(Level.SEVERE, null, ex);
                }
            }else{
                Logger.getLogger(OTAHotelResNotifBuilder.class.getName()).log(Level.INFO,"no need to create table "); 
            }
            try {
                PreparedStatement psSqlDelReservationExtended = conn.prepareStatement(sqlInsReservationExtended);
                psSqlDelReservationExtended.setInt(1, reservationId);
                psSqlDelReservationExtended.setString(2, this.TpaXml);
                psSqlDelReservationExtended.setString(3, this.TpaXml);
                psSqlDelReservationExtended.execute();
            } catch (SQLException ex) {
                Logger.getLogger(OTAHotelResNotifBuilder.class.getName()).log(Level.INFO,"Error updating TpaXml " + ex.getMessage());
                Logger.getLogger(OTAHotelResNotifBuilder.class.getName()).log(Level.SEVERE, null, ex);
            }  
            Logger.getLogger(OTAHotelResNotifBuilder.class.getName()).log(Level.INFO,"Updated   tpa-xml "); 
        }
        try {
            DbUtils.commitAndCloseQuietly(conn);
        } catch (Exception ex) {
            Logger.getLogger(OTAHotelResNotifBuilder.class.getName()).log(Level.INFO,"Error closing connection " + ex.getMessage());
            Logger.getLogger(OTAHotelResNotifBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return reservationId;
    }

    private void fillReservation(PreparedStatement ps) throws SQLException {
        int j = 1;
        Random generator = new Random();

        ps.setObject(j++, 1); // portal_id
        ps.setObject(j++, hotelCode); // stucture_id
        ps.setObject(j++, 0); // partner_id
        ps.setObject(j++, 0); // guest_id
        ps.setObject(j++, Facilities.RESERVATION_INSERT_TYPE_PMS); // reservation_insert_type
        ps.setObject(j++, reservation.get("reservation_number")); // reservation_number
        ps.setObject(j++, generator.nextInt(100000)); // reservation_pincode
        ps.setObject(j++, profileCustomer.get("reservation_name")); // reservation_name
        ps.setObject(j++, profileCustomer.get("reservation_surname")); // reservation_surname
        ps.setObject(j++, profileCustomer.get("reservation_company")); // reservation_company
        ps.setObject(j++, profileCustomer.get("reservation_email")); // reservation_email
        ps.setObject(j++, profileCustomer.get("reservation_address")); // reservation_address
        ps.setObject(j++, profileCustomer.get("reservation_city")); // reservation_city
        ps.setObject(j++, profileCustomer.get("reservation_state")); // reservation_state
        ps.setObject(j++, profileCustomer.get("reservation_zipcode")); // reservation_zipcode
        ps.setObject(j++, profileCustomer.get("reservation_country")); // reservation_country

        try {
            ps.setObject(j++, profileCustomer.get("reservation_phone")); // reservation_phone
        } catch (Exception e) {
            ps.setObject(j - 1, "");
        }

        try {
            ps.setObject(j++, profileCustomer.get("reservation_fax")); // reservation_fax
        } catch (Exception e) {
            ps.setObject(j - 1, "");
        }
        
        try {
            ps.setObject(j++, profileCustomer.get("reservation_mobile")); // reservation_mobile
        } catch (Exception e) {
            ps.setObject(j - 1, "");
        }
        
        ps.setObject(j++, 0); // guest_allow

        try {
            ps.setObject(j++, profileCustomer.get("reservation_guest_language")); // guest_language
        } catch (Exception e) {
            ps.setObject(j - 1, "");
        }

        try {
            ps.setObject(j++, reservation.get("reservation_note")); // reservation_note
        } catch (Exception e) {
            ps.setObject(j - 1, "");
        }
            
        ps.setObject(j++,  reservation.get("reservation_guest_language")  ); // reservation_language
        ps.setObject(j++, 1); // reservation_privacy
        ps.setObject(j++, 1); // reservation_accept_term
        ps.setObject(j++, 0); // reservation_newsletter_subscribe
        ps.setObject(j++, "IT"); // reservation_newsletter_language
        ps.setObject(j++, reservation.get("reservation_checkin_date")); // reservation_checkin_date
        ps.setObject(j++, reservation.get("reservation_checkout_date")); // reservation_checkout_date
        ps.setObject(j++, reservation.get("reservation_numpersons")); // reservation_numpersons;
        ps.setObject(j++, reservation.get("reservation_numnights")); // reservation_numnights
        ps.setObject(j++, reservation.get("reservation_numrooms")); // reservation_numrooms
        ps.setObject(j++, reservation.get("reservation_tot_reservation_price")); // reservation_tot_reservation_price
        ps.setObject(j++, "--");
        ps.setObject(j++, 103); // reservation_status confermata da gestionale
        ps.setObject(j++, 0); // reservation_update_allotment
        ps.setObject(j++, reservation.get("reservation_opened_date")); // reservation_opened_date
        ps.setObject(j++, 0); // reservation_deleted
        ps.setObject(j++, this.remoteAddress);
    }
   
    private void fillReservationDetail(PreparedStatement ps, int reservationId, String itemCode, Map<String, String> value, int itemId) throws SQLException {
        int j = 1;

        ps.setObject(j++, reservationId); // reservationId
        ps.setObject(j++, itemCode); // dettaglio
        ps.setObject(j++, itemId); // progressivo        
        if (itemCode.equals("R")) {
            ps.setObject(j++, Facilities.RESERVATION_ROOM_CODE_PMS);
            ps.setObject(j++, value.get("reservation_detail_name"));
        } else if (itemCode.equals("T")) {
            ps.setObject(j++, "-TOTAL-");
            ps.setObject(j++, "Prezzo totale camera");
        } else if (itemCode.equals("G")) {
            ps.setObject(j++, "-GRANTOTAL-");
            ps.setObject(j++, "Totale soggiorno");
        }
        ps.setObject(j++, value.get("reservation_detail_price")); // prezzo
        ps.setObject(j++, Facilities.RESERVATION_DETAIL_ROOM_ID);
        ps.setObject(j++, Facilities.RESERVATION_DETAIL_LIST_ID);
        ps.setObject(j++, value.get("reservation_detail_room_board"));
        ps.setObject(j++, 0.0); // tot prezzo
        if (itemCode.equals("R")) {
            ps.setObject(j++, value.get("reservation_detail_room_guest")); // room_guest A:3|C:-|C:0
        } else {
            ps.setObject(j++, ""); // room_guest A:3|C:-|C:0
        }
        if (!itemCode.equals("G")) {
            ps.setObject(j++, reservation.get("reservation_detail_cancellation_policy")); // cancellation policy
            ps.setObject(j++, reservation.get("reservation_detail_payment_policy")); // payment policy
        } else {            
            ps.setObject(j++, ""); // cancellation policy
            ps.setObject(j++, ""); // payment policy
        }
    }

    private void fillReservationStatus(PreparedStatement ps, int reservationId) throws SQLException {
        int j = 1;

        ps.setObject(j++, reservationId); // reservationId
        ps.setObject(j++, Facilities.RESSTATUS_MAP.get(reservation.get("reservation_status"))); // confermata
        ps.setObject(j++, reservation.get("reservation_opened_date"));
    }
    
    public OTAHotelResNotifRS getRes() {
        return res;
    }

    private boolean thereIsError() {
        return !(res.getErrors() == null || res.getErrors().getError().isEmpty());
    }
    
    public static String objToXml(Object obj){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();  
        XMLEncoder enc = new XMLEncoder(baos); 
        enc.writeObject(obj);
        enc.close(); 
        String sSupData =  baos.toString(   ); 
        
        return sSupData;
    }
    public static void main(String[] args) {
        try {
               
        } catch (Exception e) {
            e.printStackTrace();         
        }   
    }
}
