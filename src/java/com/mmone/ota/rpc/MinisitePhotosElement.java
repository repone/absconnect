package com.mmone.ota.rpc;

import java.util.Map;
import java.util.Vector;

// TODO: Auto-generated Javadoc
/**
 * The Class MinisitePhotosElement.
 */
public class MinisitePhotosElement {
	
	/** The id. */
	private Integer id;
	
	/** The title. */
	private String title;
	
	/** The url. */
	private String url;
	
	/**
	 * Instantiates a new minisite photos element.
	 * 
	 * @param values the values
	 */
	public MinisitePhotosElement(Map values){
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
		id = (Integer) values.get("photo_id");
		title  = en((String) values.get("photo_title"));
		url  = en((String) values.get("photo_url"));
		 
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
	 * Gets the title.
	 * 
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the title.
	 * 
	 * @param title the new title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Gets the url.
	 * 
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Sets the url.
	 * 
	 * @param url the new url
	 */
	public void setUrl(String url) {
		this.url = url;
	}
	 
	
	
}
