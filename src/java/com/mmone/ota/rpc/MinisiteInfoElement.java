package com.mmone.ota.rpc;

import java.util.Map;
import java.util.Vector;

// TODO: Auto-generated Javadoc
/**
 * The Class MinisiteInfoElement.
 */
public class MinisiteInfoElement {
	
	/** The id. */
	private Integer id;
	
	/** The name. */
	private String name;
	
	/** The rating. */
	private String rating ;
	
	/** The description. */
	private String description ;
	
	/** The payment method. */
	private String paymentMethod;
	
	/** The checkincheckout policy. */
	private String checkincheckoutPolicy ;
	
	/** The rates policy. */
	private String ratesPolicy ;
	
	/** The reservation policy. */
	private String reservationPolicy;
	
	/** The cancellation policy. */
	private String cancellationPolicy;
	
	/** The privacy policy. */
	private String privacyPolicy;
	
	/** The how to reach. */
	private String howToReach;
	
	/** The reservation start note. */
	private String reservationStartNote;
	
	/** The reservation end note. */
	private String reservationEndNote;
	
	/** The enquiry end note. */
	private String enquiryEndNote;
	
	/**
	 * Instantiates a new minisite info element.
	 * 
	 * @param values the values
	 */
	public MinisiteInfoElement(Map values){
		setValues(values);
	}
	private String en(Object data){
            if(data==null) return null;
            return com.mmone.ota.hotel.Facilities.asciiEncoding(data.toString());
        }
	/**
	 * Sets the values.
	 * 
	 * @param values the new values
	 */
	public void setValues(Map values){
		id = (Integer) values.get("structure_id");
		rating = en((String) values.get("structure_rating"));
		name = en((String) values.get("structure_name"));
		description = en((String) values.get("structure_description"));
		paymentMethod =en( (String) values.get("structure_payment_method"));
		checkincheckoutPolicy = en((String) values.get("structure_checkincheckout_policy"));
		ratesPolicy = en((String) values.get("structure_rates_policy"));
		reservationPolicy = en((String) values.get("structure_reservation_policy"));
		cancellationPolicy = en((String) values.get("structure_cancellation_policy"));
		privacyPolicy = en((String) values.get("structure_privacy_policy"));
		howToReach = en((String) values.get("structure_how_to_reach"));
		reservationStartNote = en((String) values.get("structure_reservation_start_note"));
		reservationEndNote = en((String) values.get("structure_reservation_end_note"));
		enquiryEndNote = en((String) values.get("structure_enquiry_end_note"));
	}
	
	/**
	 * Gets the id.
	 * 
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}
	
	/**
	 * Sets the id.
	 * 
	 * @param id the new id
	 */
	public void setId(Integer id) {
		this.id = id;
	}
	
	/**
	 * Gets the name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the name.
	 * 
	 * @param name the new name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Gets the rating.
	 * 
	 * @return the rating
	 */
	public String getRating() {
		return rating;
	}
	
	/**
	 * Sets the rating.
	 * 
	 * @param rating the new rating
	 */
	public void setRating(String rating) {
		this.rating = rating;
	}
	
	/**
	 * Gets the description.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Sets the description.
	 * 
	 * @param description the new description
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * Gets the payment method.
	 * 
	 * @return the payment method
	 */
	public String getPaymentMethod() {
		return paymentMethod;
	}
	
	/**
	 * Sets the payment method.
	 * 
	 * @param paymentMethod the new payment method
	 */
	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}
	
	/**
	 * Gets the checkincheckout policy.
	 * 
	 * @return the checkincheckout policy
	 */
	public String getCheckincheckoutPolicy() {
		return checkincheckoutPolicy;
	}
	
	/**
	 * Sets the checkincheckout policy.
	 * 
	 * @param checkincheckoutPolicy the new checkincheckout policy
	 */
	public void setCheckincheckoutPolicy(String checkincheckoutPolicy) {
		this.checkincheckoutPolicy = checkincheckoutPolicy;
	}
	
	/**
	 * Gets the rates policy.
	 * 
	 * @return the rates policy
	 */
	public String getRatesPolicy() {
		return ratesPolicy;
	}
	
	/**
	 * Sets the rates policy.
	 * 
	 * @param ratesPolicy the new rates policy
	 */
	public void setRatesPolicy(String ratesPolicy) {
		this.ratesPolicy = ratesPolicy;
	}
	
	/**
	 * Gets the reservation policy.
	 * 
	 * @return the reservation policy
	 */
	public String getReservationPolicy() {
		return reservationPolicy;
	}
	
	/**
	 * Sets the reservation policy.
	 * 
	 * @param reservationPolicy the new reservation policy
	 */
	public void setReservationPolicy(String reservationPolicy) {
		this.reservationPolicy = reservationPolicy;
	}
	
	/**
	 * Gets the cacellation policy.
	 * 
	 * @return the cacellation policy
	 */
	public String getCacellationPolicy() {
		return cancellationPolicy;
	}
	
	/**
	 * Sets the cacellation policy.
	 * 
	 * @param cacellationPolicy the new cacellation policy
	 */
	public void setCacellationPolicy(String cacellationPolicy) {
		this.cancellationPolicy = cacellationPolicy;
	}
	
	/**
	 * Gets the privacy policy.
	 * 
	 * @return the privacy policy
	 */
	public String getPrivacyPolicy() {
		return privacyPolicy;
	}
	
	/**
	 * Sets the privacy policy.
	 * 
	 * @param privacyPolicy the new privacy policy
	 */
	public void setPrivacyPolicy(String privacyPolicy) {
		this.privacyPolicy = privacyPolicy;
	}
	
	/**
	 * Gets the how to reach.
	 * 
	 * @return the how to reach
	 */
	public String getHowToReach() {
		return howToReach;
	}
	
	/**
	 * Sets the how to reach.
	 * 
	 * @param howToReach the new how to reach
	 */
	public void setHowToReach(String howToReach) {
		this.howToReach = howToReach;
	}
	
	/**
	 * Gets the reservation start note.
	 * 
	 * @return the reservation start note
	 */
	public String getReservationStartNote() {
		return reservationStartNote;
	}
	
	/**
	 * Sets the reservation start note.
	 * 
	 * @param reservationStartNote the new reservation start note
	 */
	public void setReservationStartNote(String reservationStartNote) {
		this.reservationStartNote = reservationStartNote;
	}
	
	/**
	 * Gets the reservation end note.
	 * 
	 * @return the reservation end note
	 */
	public String getReservationEndNote() {
		return reservationEndNote;
	}
	
	/**
	 * Sets the reservation end note.
	 * 
	 * @param reservationEndNote the new reservation end note
	 */
	public void setReservationEndNote(String reservationEndNote) {
		this.reservationEndNote = reservationEndNote;
	}
	
	/**
	 * Gets the enquiry end note.
	 * 
	 * @return the enquiry end note
	 */
	public String getEnquiryEndNote() {
		return enquiryEndNote;
	}
	
	/**
	 * Sets the enquiry end note.
	 * 
	 * @param enquiryEndNote the new enquiry end note
	 */
	public void setEnquiryEndNote(String enquiryEndNote) {
		this.enquiryEndNote = enquiryEndNote;
	}
	
	
}
