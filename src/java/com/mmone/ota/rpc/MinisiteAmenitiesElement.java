package com.mmone.ota.rpc;

import java.util.Map;
import java.util.Vector;

// TODO: Auto-generated Javadoc
/**
 * The Class MinisitePhotosElement.
 */
public class MinisiteAmenitiesElement {
	
	/** The id. */
	private Integer id;
	
	/** The name. */
	private String name;
	
	/** The fee paying. */
	private int feePaying;
	
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
	 * Gets the fee paying.
	 * 
	 * @return the fee paying
	 */
	public int getFeePaying() {
		return feePaying;
	}

	/**
	 * Sets the fee paying.
	 * 
	 * @param feePaying the new fee paying
	 */
	public void setFeePaying(int feePaying) {
		this.feePaying = feePaying;
	}

	/**
	 * Instantiates a new minisite photos element.
	 * 
	 * @param values the values
	 */
	public MinisiteAmenitiesElement(Map values){
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
		id = (Integer) values.get("amenity_id");
		name  = en((String) values.get("amenity_name"));
		feePaying  =  (Integer) values.get("amenity_fee-paying");
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

  	
	
}
