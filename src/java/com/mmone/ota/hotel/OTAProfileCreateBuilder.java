/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mmone.ota.hotel;

import java.sql.PreparedStatement;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.xml.datatype.DatatypeConfigurationException;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.opentravel.ota._2003._05.CustomerType;
import org.opentravel.ota._2003._05.ErrorType;
import org.opentravel.ota._2003._05.ErrorsType;
import org.opentravel.ota._2003._05.HotelPrefType;
import org.opentravel.ota._2003._05.OTAProfileCreateRQ;
import org.opentravel.ota._2003._05.OTAProfileCreateRS;
import org.opentravel.ota._2003._05.PersonNameType;
import org.opentravel.ota._2003._05.PreferencesType.PrefCollection;
import org.opentravel.ota._2003._05.ProfileType.UserID;
import org.opentravel.ota._2003._05.PropertyNamePrefType;
import org.opentravel.ota._2003._05.WarningsType;
import org.opentravel.ota._2003._05.WarningType;
import org.opentravel.ota._2003._05.SuccessType;

/**
 *
 * @author umberto.zanatta
 */
public class OTAProfileCreateBuilder  extends BaseBuilder{

    private OTAProfileCreateRQ request;
    private OTAProfileCreateRS res = new OTAProfileCreateRS();
    private DataSource ds;
    private QueryRunner run;
    private String user;
    private String langID;
    private String echoToken;
    private String requestorID;
    private String target = Facilities.TARGET_PRODUCTION;
    private BigDecimal version = new BigDecimal(Facilities.VERSION);
    private Map<String, String> logData = new LinkedHashMap<String, String>();

    String hotelCode = null;

    // PersonName
    private final int GIVENNAME = 0;
    private final int SURNAME = 1;
    // Address
    private final int ADDRESSLINE = 0;
    private final int CITYNAME = 1;
    private final int POSTALCODE = 2;
    private final int STATEPROV = 3;
    private final int COUNTRYNAME = 4;
    private final int COMPANYNAME = 5;
    // UserID
    private final int TYPE = 0;
    private final int ID = 1;
    //Telephone
    private final int VOICE = 1;
    private final int FAX = 3;
    private final int MOBILE = 5;

    //Array da riempire
    private Map<Integer, String> personName = new LinkedHashMap<Integer, String>();
    private Map<Integer, String> address = new LinkedHashMap<Integer, String>();
    private Map<Integer, String> userID = new LinkedHashMap<Integer, String>();
    private Map<Integer, String> telephone = new LinkedHashMap<Integer, String>();
    private String language;
    private String email; 
    
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

        Logger.getLogger(OTAProfileCreateBuilder.class.getName()).log(Level.INFO, msg.toString());
    }

    public OTAProfileCreateBuilder(DataSource ds, OTAProfileCreateRQ request, String user, HttpServletRequest httpRequest) {
        super();

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

        logData.put("Class", this.getClass().getName());
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

    public OTAProfileCreateRS build(String requestorID) throws Exception {
        this.requestorID = requestorID;
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
            return res;
        }

        logData.put("HotelCode", hotelCode);

        // Carica i parametri della richiesta
        setLanguage();
        setPersonName();
        setTelephone();
        setEmail();
        setAddress();        
        setUserID();

        logInfoRequest(logData);

        if (thereIsError()) {
            return res;
        }

        fillGuestData();
       
        if (!thereIsError()) {
            res.setSuccess(new SuccessType());
        }

        return res;
    }

    private void setLanguage() {
        CustomerType customerType = request.getProfile().getCustomer();
       
        try {
            language = customerType.getLanguage();
        } catch (Exception e) {

        }

        if (language != null) {
            if (language.length() != 2) {
              addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "@Lanugage length must be exactly 2");
            }
        }
    }
    
    private void setPersonName() {
        CustomerType customerType = request.getProfile().getCustomer();
        List<PersonNameType> personNames = null;

        try {
            personNames = customerType.getPersonName();
        } catch (Exception e) {
            
        }

        if (personNames != null) {
            if (personNames.size() > 1) {
              addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Too many PersonName elements");
              return;
            } else {
                try {
                    personName.put(GIVENNAME, personNames.get(0).getGivenName().get(0).toString());
                } catch (Exception e) {
                    addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_VALUE, "GivenName is null");
                    return;
                }

                try {
                    personName.put(SURNAME, personNames.get(0).getSurname().toString());
                } catch (Exception e) {
                    addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_VALUE, "Surname is invalid");
                    return;
                }
            }
        } else {
            addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_VALUE, "Surname is null");
        }
    }

    private void setTelephone() {
        List<CustomerType.Telephone> telephones = null;

        try {
            telephones = request.getProfile().getCustomer().getTelephone();
        } catch (Exception e) {
            
        }

        if (telephones != null) {
            if (telephones.size() > 3) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Too many Telephone elements");
                return;
            } else {
                for (CustomerType.Telephone _telephone : telephones) {
                    int phoneTechType = 0;

                    try {
                        phoneTechType = new Integer(_telephone.getPhoneTechType().toString());
                    } catch (Exception e) {
                        addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_VALUE, "@PhoneTechType is invalid");
                        return;
                    }

                    if (phoneTechType != VOICE && phoneTechType != FAX && phoneTechType != MOBILE) {
                        addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_VALUE, "@PhoneTechType is invalid");
                        return;
                    }

                    try {
                        telephone.put(phoneTechType, _telephone.getPhoneNumber().toString());
                    } catch (Exception e) {
                        addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_VALUE, "@PhoneNumber is invalid");
                        return;
                    }
                }
            }
        } else {
            addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_VALUE, "Telephone is null");
        }
    }

    private void setEmail() {
        List<CustomerType.Email> emails = null;

        try {
            emails = request.getProfile().getCustomer().getEmail();
        } catch (Exception e) {

        }

        if (emails != null) {
            if (emails.size() > 1) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Too many Email elements");
                return;
            } else {
                try {
                    email = emails.get(0).getValue().toString();
                    //@@@@@@ emails.get(0).getShareMarketInd();
                    if (email.isEmpty()) {
                        addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_VALUE, "Email is invalid");
                        return;
                    }
                } catch (Exception e) {
                    addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_VALUE, "Email is invalid");
                    return;
                }
                logData.put("Email", email);
            }
        } else {
            addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_VALUE, "Email is null");
        }
    }

    private void setAddress() {
        List<CustomerType.Address> addresses = null;
        List<String> addressLines = null;
        
        String addressLine = null;
        String cityName = null;
        String postalCode = null;
        String stateProv = null;
        String countryName = null;
        String companyName = null;
        
        try {
            addresses = request.getProfile().getCustomer().getAddress();
        } catch (Exception e) {
            
        }

        if (addresses != null) {
            if (addresses.size() > 1) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Too many Address elements");
            } else {
                //AddressLine
                try {
                    addressLines = addresses.get(0).getAddressLine();
                } catch (Exception e) {
                
                }

                if (addressLines != null) {
                    if (addressLines.size() > 1) {
                        addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Too many AddressLine elements");
                        return;
                    } else {
                        try {
                            addressLine = addressLines.get(0).toString();
                            address.put(ADDRESSLINE, addressLine);
                        } catch (Exception e) {
                            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "AddressLine is null");
                            return;
                        }
                    }
                }

                //CityName
                try {
                    cityName = addresses.get(0).getCityName();
                    address.put(CITYNAME, cityName);
                } catch (Exception e) {
                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "CityName is null");
                    return;
                }

                //PostalCode
                try {
                    postalCode = addresses.get(0).getPostalCode();
                    address.put(POSTALCODE, postalCode);
                } catch (Exception e) {
                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "PostalCode is null");
                    return;
                }

                //StateProv
                try {
                    stateProv = addresses.get(0).getStateProv().getValue();
                    address.put(STATEPROV, stateProv);
                } catch (Exception e) {
                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "StateProv is null");
                    return;
                }

                //CountryName
                try {
                    countryName = addresses.get(0).getCountryName().getCode();
                    if (countryName.length() != 2) {
                        addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "@Code length must be exactly 2");
                    } else {
                        address.put(COUNTRYNAME, countryName);
                    }
                } catch (Exception e) {
                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "@Code is null");
                    return;
                }

                //CompanyName
                try {
                    companyName = addresses.get(0).getCompanyName().getValue();
                    address.put(COMPANYNAME, companyName);
                } catch (Exception e) {
                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "CompanyName is null");
                    return;
                }               
            }
        } else {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Address is null");
        }
    }

    private void setUserID() {
        List<UserID> userIDs = null;
                                
        try {
            userIDs = request.getProfile().getUserID();
        } catch (Exception e) {
            addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_VALUE, "UserID is null");
            return;
        }

        if (userIDs != null) {
            if (userIDs.size() > 1) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Too many UserID elements");
                return;
            } else {
                try {
                    String type = userIDs.get(0).getType().toString();
                    if (!type.equals("1")) {
                        addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_VALUE, "@Type must be 1");
                        return;
                    }
                    userID.put(TYPE, userIDs.get(0).getType().toString());
                } catch (Exception e) {
                    addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_VALUE, "@Type is null");
                    return;
                }

                try {
                    userID.put(ID, userIDs.get(0).getID());
                } catch (Exception e) {
                    addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_VALUE, "@ID is null");
                    return;
                }

                logData.put("ID", userIDs.get(0).getID());
            }
        } else {
            addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_VALUE, "UserID is null");
        }
    }

    private void setHotelCode() {
        List<PrefCollection> prefCollections = null;
        List<HotelPrefType> hotelPrefs = null;
        List<PropertyNamePrefType> propertyNamePrefs = null;

        try {
            prefCollections = request.getProfile().getPrefCollections().getPrefCollection();
        } catch (Exception e) {
        }

        if (prefCollections != null) {
            if (prefCollections.size() > 1) {
                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Too many PrefCollection elements");
                return;
            } else {
                try {
                    hotelPrefs = prefCollections.get(0).getHotelPref();
                } catch (Exception e) {
                }

                if (hotelPrefs != null) {
                    if (hotelPrefs.size() > 1) {
                        addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Too many HotelPref elements");
                        return;
                    } else {
                        try {
                            propertyNamePrefs = hotelPrefs.get(0).getPropertyNamePref();
                        } catch (Exception e) {
                        }

                        if (propertyNamePrefs != null) {
                            if (propertyNamePrefs.size() > 1) {
                                addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "Too many PropertyNamePref elements");
                                return;
                            } else {
                                try {
                                    hotelCode = hotelPrefs.get(0).getPropertyNamePref().get(0).getHotelCode().toString();
                                } catch (Exception e) {
                                    addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "@HotelCode is invalid");
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        } else {
            addError(Facilities.EWT_PROTOCOL_VIOLATION, Facilities.ERR_INVALID_VALUE, "PrefCollections is null");
        }
    }

    private void fillGuestData() {
        Connection conn = null;

        try {
            conn = ds.getConnection();
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            
        }

        String sqlInsertGuest = ""
                    + " INSERT INTO guest ("
                    + "     structure_id,"
                    + "     guest_name, "
                    + "     guest_surname, "
                    + "     guest_company, "
                    + "     guest_email, "
                    + "     guest_address, "
                    + "     guest_city, "
                    + "     guest_state, "
                    + "     guest_zipcode, "
                    + "     guest_country, "
                    + "     guest_phone, "
                    + "     guest_fax, "
                    + "     guest_mobile, "
                    + "     guest_language, "
                    + "     guest_status, "
                    + "     guest_newsletter_language"
                    + ")"
                    + " VALUES (" + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" + ") "
                    + " ON DUPLICATE KEY UPDATE "
                    + "     guest_name = ?, "
                    + "     guest_surname = ?, "
                    + "     guest_company = ?, "
                    + "     guest_address = ?, "
                    + "     guest_city = ?, "
                    + "     guest_state = ?, "
                    + "     guest_zipcode = ?, "
                    + "     guest_country = ?, "
                    + "     guest_phone = ?, "
                    + "     guest_fax = ?, "
                    + "     guest_mobile = ?, "
                    + "     guest_language = ?, "
                    + "     guest_newsletter_language = ?"
                    + "";

        String sqlInsertOtaGuest = ""
                + " INSERT INTO ota_guest ("
                + "     guest_id,"
                + "     user_id)"
                + " VALUES (" + "?, ?" + ") "
                + " ON DUPLICATE KEY UPDATE "
                + "     guest_id = ?, "
                + "     user_id = ?"
                + "";
        
        PreparedStatement psSqlInsGuest = null;
        PreparedStatement psSqlInsOtaGuest = null;
        
        // Aggiorna tabella guest
        try {
            //Tabella Guest
            psSqlInsGuest = conn.prepareStatement(sqlInsertGuest, Statement.RETURN_GENERATED_KEYS);
            fillInsertGuest(psSqlInsGuest);
            psSqlInsGuest.execute();

            ResultSet keys = psSqlInsGuest.getGeneratedKeys();

            int guestId = 0 ;
            if (keys.next()) {
                guestId = keys.getInt(1);
                logData.put("GuestId", Integer.toString(guestId));
            }

            //Tabella OtaGuest
            psSqlInsOtaGuest = conn.prepareStatement(sqlInsertOtaGuest);
            fillInsertOtaGuest(psSqlInsOtaGuest, guestId);
            psSqlInsOtaGuest.execute();
        } catch (SQLException e) {
            addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Server. Insert data failed");
            Logger.getLogger(OTAProfileCreateBuilder.class.getName()).log(Level.SEVERE, null, e);
            DbUtils.rollbackAndCloseQuietly(conn);
        } finally {
            DbUtils.commitAndCloseQuietly(conn);
        }
    }

    private void fillInsertGuest(PreparedStatement ps) throws SQLException {
        int j = 1;
        
        try{
            String msg =  "hotelcode=" +hotelCode + "&"
                          +"DATABASE=" +requestorID + "&"
                          +"GIVENNAME=" +personName.get(GIVENNAME).toString() + "&"
                          +"SURNAME=" +personName.get(SURNAME).toString() + "&"
                          +"SURNAME=" +personName.get(SURNAME).toString() + "&"
                          +"";

            Logger.getLogger(OTAProfileCreateBuilder.class.getName()).log(Level.INFO,msg);
        }catch(Exception e){ }    
        //INSERT
        ps.setObject(j++, hotelCode);
        ps.setObject(j++, personName.get(GIVENNAME).toString());
        ps.setObject(j++, personName.get(SURNAME).toString());
        ps.setObject(j++, address.get(COMPANYNAME).toString());
        ps.setObject(j++, email);
        ps.setObject(j++, address.get(ADDRESSLINE).toString());
        ps.setObject(j++, address.get(CITYNAME).toString());
        ps.setObject(j++, address.get(STATEPROV).toString());
        ps.setObject(j++, address.get(POSTALCODE).toString());
        ps.setObject(j++, address.get(COUNTRYNAME).toString());
        
        try {
            ps.setObject(j++, telephone.get(VOICE).toString());
        } catch (Exception e) {
            ps.setObject(j - 1, null);
        }
        try {
            ps.setObject(j++, telephone.get(FAX).toString());
        } catch (Exception e) {
            ps.setObject(j - 1, null);
        }
        try {
            ps.setObject(j++, telephone.get(MOBILE).toString());
        } catch (Exception e) {
            ps.setObject(j - 1, null);
        }
        ps.setObject(j++, language);
        ps.setObject(j++, 1);
        ps.setObject(j++, language.toUpperCase());

        //UPDATE
        ps.setObject(j++, personName.get(GIVENNAME).toString());
        ps.setObject(j++, personName.get(SURNAME).toString());
        ps.setObject(j++, address.get(COMPANYNAME).toString());
        ps.setObject(j++, address.get(ADDRESSLINE).toString());
        ps.setObject(j++, address.get(CITYNAME).toString());
        ps.setObject(j++, address.get(STATEPROV).toString());
        ps.setObject(j++, address.get(POSTALCODE).toString());
        ps.setObject(j++, address.get(COUNTRYNAME).toString());
        try {
            ps.setObject(j++, telephone.get(VOICE).toString());
        } catch (Exception e) {
            ps.setObject(j - 1, null);
        }
        try {
            ps.setObject(j++, telephone.get(FAX).toString());
        } catch (Exception e) {
            ps.setObject(j - 1, null);
        }
        try {
            ps.setObject(j++, telephone.get(MOBILE).toString());
        } catch (Exception e) {
            ps.setObject(j - 1, null);
        }
        ps.setObject(j++, language);
        ps.setObject(j++, language.toUpperCase());
    }

    private void fillInsertOtaGuest(PreparedStatement ps, int guestId) throws SQLException {
        int j = 1;

        //INSERT
        ps.setInt(j++, guestId);
        ps.setObject(j++, userID.get(ID).toString());

        //UPDATE
        ps.setInt(j++, guestId);
        ps.setObject(j++, userID.get(ID).toString());
    }
    
    public OTAProfileCreateRS getRes() {
        return res;
    }

    private boolean thereIsError() {
        return !(res.getErrors() == null || res.getErrors().getError().isEmpty());
    }
}
