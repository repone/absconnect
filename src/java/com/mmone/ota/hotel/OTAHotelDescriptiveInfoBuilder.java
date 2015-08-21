package com.mmone.ota.hotel;

import java.io.IOException;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;
import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;
import org.opentravel.ota._2003._05.AddressesType;
import org.opentravel.ota._2003._05.ContactInfoRootType;
import org.opentravel.ota._2003._05.ContactInfosType;
import org.opentravel.ota._2003._05.CountryNameType;
import org.opentravel.ota._2003._05.EmailsType;
import org.opentravel.ota._2003._05.ErrorType;
import org.opentravel.ota._2003._05.ErrorsType;
import org.opentravel.ota._2003._05.HotelInfoType;
import org.opentravel.ota._2003._05.ImageItemsType;
import org.opentravel.ota._2003._05.MultimediaDescriptionType;
import org.opentravel.ota._2003._05.MultimediaDescriptionsType;
import org.opentravel.ota._2003._05.WarningsType;
import org.opentravel.ota._2003._05.WarningType;

import org.opentravel.ota._2003._05.OTAHotelDescriptiveInfoRQ;
import org.opentravel.ota._2003._05.OTAHotelDescriptiveInfoRS;
import org.opentravel.ota._2003._05.PhonesType;
import org.opentravel.ota._2003._05.StateProvType;
import org.opentravel.ota._2003._05.SuccessType;    
import org.opentravel.ota._2003._05.URLsType;
import org.opentravel.ota._2003._05.ContactInfoType.CompanyName;
import org.opentravel.ota._2003._05.HotelInfoType.HotelName;
import org.opentravel.ota._2003._05.HotelInfoType.Position;
import org.opentravel.ota._2003._05.ImageDescriptionType.ImageFormat;
import org.opentravel.ota._2003._05.ImageItemsType.ImageItem;
import org.opentravel.ota._2003._05.OTAHotelDescriptiveInfoRS.HotelDescriptiveContents;
import org.opentravel.ota._2003._05.OTAHotelDescriptiveInfoRS.HotelDescriptiveContents.HotelDescriptiveContent;
import org.opentravel.ota._2003._05.PhonesType.Phone;
 
import com.mmone.ota.rpc.Facilities;
import com.mmone.ota.rpc.MinisiteInfoElement;
import com.mmone.ota.rpc.MinisitePhotosElement;
import com.mmone.ota.rpc.RpcBackEndFacade;
import java.math.BigDecimal;
import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;

// TODO: Auto-generated Javadoc
/**
 * The Class OTAHotelDescriptiveInfoBuilder.
 */
public class OTAHotelDescriptiveInfoBuilder  extends BaseBuilder{ 
	
	/** The Constant EMAIL_TYPE_BUSINESS. */
	private static final String EMAIL_TYPE_BUSINESS = "2";	
	/** The Constant EMAIL_TYPE_HOME. */
	private static final String EMAIL_TYPE_HOME = "1";	
	/** The Constant PHONE_LOCATION_TYPE_MOBILE. */
	private static final String PHONE_LOCATION_TYPE_MOBILE = "8"; 	
	/** The Constant PHONE_LOCATION_TYPE_OFFICE. */
	private static final String PHONE_LOCATION_TYPE_OFFICE = "7"; 	
	/** The Constant PHONE_TEACH_TYPE_VOCE. */
	private static final String PHONE_TEACH_TYPE_VOCE = EMAIL_TYPE_HOME;	
	/** The Constant PHONE_TEACH_TYPE_FAX. */
	private static final String PHONE_TEACH_TYPE_FAX = "3";	
	/** The logger. */
	private static final Logger logger = Logger.getLogger(OTAHotelDescriptiveInfoBuilder.class.getName());
	private DataSource ds;	
	private XmlRpcClient client;	
	/** The minisite info element. */
	private MinisiteInfoElement minisiteInfoElement;	
	/** The v minisite photos element. */
	private Vector<MinisitePhotosElement>vMinisitePhotosElement;	
	/** The initialized. */
	private boolean initialized = false;	
	private String langID;
        private String user;
	private String echoToken;
        private String requestorID;        
	/** The hotel code. */
	private BigInteger hotelCode;
        private String target = Facilities.TARGET_PRODUCTION;
        private BigDecimal version = new BigDecimal(Facilities.VERSION);
	/** The request. */
	private OTAHotelDescriptiveInfoRQ request;
        /** The respond. */
        private OTAHotelDescriptiveInfoRS res = new OTAHotelDescriptiveInfoRS();	
        /** The hotel structure. */
	private Map hotelStructure;
	/** The hotel city. */
	private Map hotelCity;
	/** The hotel city details. */
	private Map hotelCityDetails;
	/** The hotel city country. */
	private Map hotelCityCountry;
	/** The hotel city province. */
	private Map hotelCityProvince;
	private QueryRunner run;
	 
    
        public final void addError(String type, String code, String message){
            if(res.getErrors()==null) res.setErrors(new ErrorsType());

            ErrorType et = new ErrorType();
                et.setCode(code);
                et.setType(type);
                et.setValue(message);

            res.getErrors().getError().add(et);
        }

        public final void addWarning(String type, String code, String message){
            if(res.getWarnings()==null) res.setWarnings(new WarningsType());

            WarningType et = new WarningType();
                et.setCode(code);
                et.setType(type);
                et.setValue(message);

            res.getWarnings().getWarning().add(et);
        }

	/**
	 * Instantiates a new oTA hotel descriptive info builder.
	 * 
	 * @param ds the ds
	 * @param client the client
	 * @param request the request
	 */
	public OTAHotelDescriptiveInfoBuilder(DataSource ds, XmlRpcClient client,
			OTAHotelDescriptiveInfoRQ request, String user, HttpServletRequest httpRequest ) {
		
		super();

                String sHotelCode = null;

                if(ds==null) {
                    addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Server. System Error: No connection with the database");
                    return;
                }

                this.request = request;
                this.langID = request.getPrimaryLangID();
                this.user = user;
                this.client = client;
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
                    //logData.put("TimeStamp", res.getTimeStamp().toString());
                } catch (DatatypeConfigurationException ex) {

                }

                res.setEchoToken(echoToken);
                //logData.put("EchoToken", request.getEchoToken());

                res.setTarget(target);
                res.setPrimaryLangID(langID);
                res.setVersion(version);

                run = new QueryRunner(ds);

		try {
                    sHotelCode = request.getHotelDescriptiveInfos().getHotelDescriptiveInfo().get(0).getHotelCode();
		} catch (Exception e) {
                    Logger.getLogger(OTAHotelDescriptiveInfoBuilder.class.getName()).log(Level.WARNING, null, e);
                    addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Server. System Error: " + e.getMessage());
		}

                if( StringUtils.isEmpty(sHotelCode)) {
                    addError(Facilities.EWT_REQUIRED_FIELD_MISSING, Facilities.ERR_INVALID_HOTEL_CODE, "HotelCode null");
                    return;
                }

                hotelCode=new BigInteger(sHotelCode);
                        
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

                if (thereIsError()) return;
		RpcBackEndFacade backEndFacade = 
                    new RpcBackEndFacade(request.getPrimaryLangID(), hotelCode ,client);

                try {
                    minisiteInfoElement = new MinisiteInfoElement( (Map) backEndFacade.rpcBackendGetMinisiteInfoExec().get(0));
                } catch (Exception e) {
                    addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Client. Authorization Error: You don't have permission to select the structure");
                    return;
                }

                try {
                    Vector<Map>vPhotos = backEndFacade.rpcBackendGetMinisitePhotosExec();
                    vMinisitePhotosElement = new Vector<MinisitePhotosElement>();
                    for (Map photoMap : vPhotos) {
                        vMinisitePhotosElement.add(   new MinisitePhotosElement(  photoMap    ) );
                    }
                } catch (Exception e) {
                    addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Client. Authorization Error: You don't have permission to select the structure");
                    return;
                }

                try {
                    loadStructure();
                } catch (SQLException ex) {
                    addError(Facilities.EWT_UNKNOWN, Facilities.ERR_SYSTEM_ERROR, "SOAP Client. Authorization Error: You don't have permission to select the structure");
                    return;
                }
	}

	
	/**
	 * Gets the ds.
	 * 
	 * @return the ds
	 */
	public DataSource getDs() {
            return ds;
	}

	 

	/**
	 * Gets the client.
	 * 
	 * @return the client
	 */
	public XmlRpcClient getClient() {
		return client;
	}

	 

	/**
	 * Gets the minisite info element.
	 * 
	 * @return the minisite info element
	 */
	public MinisiteInfoElement getMinisiteInfoElement() {
		return minisiteInfoElement;
	}

	/**
	 * Checks if is initialized.
	 * 
	 * @return true, if is initialized
	 */
	public boolean isInitialized() {
		return initialized;
	}

	/**
	 * Gets the request.
	 * 
	 * @return the request
	 */
	public OTAHotelDescriptiveInfoRQ getRequest() {
		return request;
	}

	/**
	 * Load structure.
	 * 
	 * @throws SQLException the SQL exception
	 */
	public void loadStructure() throws SQLException{
		
		
		hotelStructure = run.query( 
			"SELECT * FROM structure where structure_id=? ",  
			new MapHandler(),
			hotelCode.intValue()
		);
		
		hotelCity = run.query( 
			"SELECT * FROM city where city_id=? ",  
			new MapHandler(),
			hotelStructure.get("city_id") 
		);
		
		 
		hotelCityDetails = run.query( 
			"SELECT * FROM city_details where city_id=? ",  
			new MapHandler(),
			hotelStructure.get("city_id") 
		);
		
		hotelCityCountry = run.query( 
			"SELECT * FROM country_detail where country_id=? ",  
			new MapHandler(),
			hotelStructure.get("country_id") 
		);
		
		hotelCityProvince = run.query( 
			"SELECT * FROM province_details where province_id=? ",  
			new MapHandler(),
			hotelStructure.get("province_id") 
		);
		
	}
	
	private String en(Object data){
            if(data==null) return null;
            return com.mmone.ota.hotel.Facilities.asciiEncoding(data.toString());
        }
	
	/**
	 * Builds the hotel info type.
	 * 
	 * @return the hotel info type
	 */
	public HotelInfoType buildHotelInfoType(){
		
		HotelInfoType hotelInfoType =  new HotelInfoType();
		HotelName oHotelName = new HotelName();
			oHotelName.setValue((String)hotelStructure.get("structure_name"));
			//oHotelName.setHotelShortName((String)hotelStructure.get("structure_name"));
		
		hotelInfoType.setHotelName(oHotelName);
		
		Position position = new Position();
		
			try {
				position.setLatitude( hotelStructure.get("structure_latitude").toString());
				position.setLongitude( hotelStructure.get("structure_longitude").toString());
			} catch (Exception e) {
				Logger.getLogger(OTAHotelDescriptiveInfoBuilder.class.getName()).log(Level.WARNING, null, e);
                                addError(Facilities.ERR_SYSTEM_ERROR, Facilities.ERR_SYSTEM_ERROR, "SOAP Server. System Error: " + e.getMessage());
			}
			
		
		hotelInfoType.setPosition(position);
		
		return hotelInfoType;
	}
	
	/**
	 * Adds the phone.
	 * 
	 * @param phonesType the phones type
	 * @param sPhone the s phone
	 * @param techType the tech type Codice OTA PTT. 1 = Voce, 3 = Fax.
	 * @param phoneLocationType the phone location type Codice OTA PLT. 6 = Home, 7 = Office, 8 = Mobile.
	 * @param defaultInd the default ind
	 */
	private void addPhone(PhonesType phonesType,String sPhone,String techType, String phoneLocationType, boolean defaultInd){ 
		if( !StringUtils.isEmpty(sPhone)){
			Phone phone = new Phone();
			phone.setPhoneNumber(sPhone);
			phone.setPhoneTechType(techType);
			phone.setPhoneLocationType( phoneLocationType);
			phone.setFormattedInd(true);
			phone.setDefaultInd(defaultInd);
			phonesType.getPhone().add(phone);
		}
	}
	
	/**
	 * Adds the email.
	 * 
	 * @param emailsType the emails type 1 = Home, 2 = Bussiness.
	 * @param sEmail the s email
	 * @param emailType the email type
	 * @param defaultInd the default ind
	 */
	private void addEmail(EmailsType emailsType,String sEmail,String emailType, boolean defaultInd){ 
		if( !StringUtils.isEmpty(sEmail)){
			EmailsType.Email email = new EmailsType.Email();
			email.setEmailType( emailType );
			email.setDefaultInd(defaultInd);
			email.setValue(sEmail);
			emailsType.getEmail().add(email);
		}
	}
	
	
	
	/**
	 * Builds the contact info root type.
	 * 
	 * @return the contact info root type
	 */
	ContactInfoRootType buildContactInfoRootType(){
		ContactInfoRootType contactInfoRootType = new ContactInfoRootType();
		
		CompanyName companyName = new CompanyName();
		companyName.setValue(  en( (String)hotelStructure.get("structure_company")));
		
			contactInfoRootType.setCompanyName(companyName);
			 
			AddressesType addressesType = new AddressesType();
				AddressesType.Address address =  new AddressesType.Address();
				addressesType.getAddress().add(address );
					address.setCityName( en((String)hotelCityDetails.get("city_name")) );
					
					CountryNameType countryNameType = new CountryNameType();
						countryNameType.setValue( en( (String)hotelCityCountry.get("country_name")) );
						//countryNameType.setCode(null);
						address.setCountryName( countryNameType );
					
					address.setFormattedInd(false);
					StateProvType stateProvType = new StateProvType();
						stateProvType.setValue(   en( (String)hotelCityProvince.get("province_name")) );
						 
						address.setStateProv( stateProvType ) ;
						address.setPostalCode( (String)hotelStructure.get("structure_zipcode")  );
						address.getAddressLine().add(   en( (String)hotelStructure.get("structure_address")) );
						
						
					PhonesType phonesType = new PhonesType();
						
						addPhone( phonesType ,  (String)hotelStructure.get("structure_phone1"),PHONE_TEACH_TYPE_VOCE,PHONE_LOCATION_TYPE_OFFICE,true );
						addPhone( phonesType ,  (String)hotelStructure.get("structure_phone2"),PHONE_TEACH_TYPE_VOCE,PHONE_LOCATION_TYPE_OFFICE,false );
						addPhone( phonesType ,  (String)hotelStructure.get("structure_mobile"),PHONE_TEACH_TYPE_VOCE,PHONE_LOCATION_TYPE_MOBILE,false);
						addPhone( phonesType ,  (String)hotelStructure.get("structure_fax"),PHONE_TEACH_TYPE_FAX,PHONE_LOCATION_TYPE_MOBILE,false);
							  
						contactInfoRootType.setPhones(phonesType);
						
					EmailsType emailsType = new EmailsType();
						
						addEmail(emailsType, (String)hotelStructure.get("structure_email"), EMAIL_TYPE_BUSINESS, true);
						contactInfoRootType.setEmails(emailsType);
						
						
					URLsType urlsType = new URLsType() 	;
						String urlName =  en((String)hotelStructure.get("structure_website")  );
						
						if( !StringUtils.isEmpty( urlName  ) ){
							URLsType.URL url = new URLsType.URL();
								url.setValue(      en( (String)hotelStructure.get("structure_website") )   );
								url.setDefaultInd(true);
								
							urlsType.getURL().add(url);
						}
						
						contactInfoRootType.setURLs(urlsType);
					
 				contactInfoRootType.setAddresses(addressesType);
				
				
		
		return contactInfoRootType;
	}
	
	/**
	 * Builds the hotel descriptive content.
	 * 
	 * @return the hotel descriptive content
	 */
	public HotelDescriptiveContent buildHotelDescriptiveContent( ) {
			HotelDescriptiveContent   hotelDescriptiveContent = new HotelDescriptiveContent();
			hotelDescriptiveContent.setHotelCode( hotelCode.toString());
			//hotelDescriptiveContent.setHotelName(   en( (String)hotelStructure.get("structure_name")  )   );
			hotelDescriptiveContent.setLanguageCode(langID);
		 
			hotelDescriptiveContent.setHotelInfo(buildHotelInfoType());
			
			ContactInfosType contactInfosType = new ContactInfosType();
				PhonesType phonesType = new PhonesType();
				hotelDescriptiveContent.setContactInfos(contactInfosType);
				 
				contactInfosType.getContactInfo().add(buildContactInfoRootType());
			
			MultimediaDescriptionsType multimediaDescriptionsType = new MultimediaDescriptionsType();
				MultimediaDescriptionType multimediaDescriptionType = new MultimediaDescriptionType();
				multimediaDescriptionType.setImageItems( new ImageItemsType() );
				
					for (MinisitePhotosElement photoElement : vMinisitePhotosElement) {
						ImageItem  imageItem  = new ImageItem ();
						ImageFormat imageFormat = new ImageFormat();
						imageFormat.setURL(   photoElement.getUrl() );
						imageFormat.setTitle( photoElement.getTitle() );
						imageItem.getImageFormat().add(imageFormat);
						 
						multimediaDescriptionType.getImageItems().getImageItem().add(imageItem);
					}
						
					multimediaDescriptionsType.getMultimediaDescription().add(multimediaDescriptionType);
				hotelDescriptiveContent.setMultimediaDescriptions(multimediaDescriptionsType);
				
			return hotelDescriptiveContent;
	}		
	
	/**
	 * Builds the ota hotel descriptive info rs.
	 * 
	 * @return the oTA hotel descriptive info rs
	 * 
	 * @throws DatatypeConfigurationException the datatype configuration exception
	 * @throws XmlRpcException the xml rpc exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws MinisiteElementNotFound the minisite element not found
	 */

	public OTAHotelDescriptiveInfoRS build (String requestorID)
			throws DatatypeConfigurationException, XmlRpcException, IOException, MinisiteElementNotFound
	{
		 
//                if(initialized){

            this.requestorID = requestorID;
            HotelDescriptiveContents hotelDescriptiveContents = new HotelDescriptiveContents();
                HotelDescriptiveContent hotelDescriptiveContent =  buildHotelDescriptiveContent( );
                hotelDescriptiveContents.getHotelDescriptiveContent().add(hotelDescriptiveContent);

            res.setHotelDescriptiveContents(hotelDescriptiveContents);

            if(!thereIsError())  {
                res.setSuccess(new SuccessType());
		return res;
            } else {
                return res;
            }
	}

        private boolean thereIsError(){
            return !(res.getErrors()==null || res.getErrors().getError().isEmpty());
        }

        public OTAHotelDescriptiveInfoRS getRes() {
            return res;
        }

}
