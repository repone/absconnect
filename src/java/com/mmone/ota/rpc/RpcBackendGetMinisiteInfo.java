package com.mmone.ota.rpc;

import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

// TODO: Auto-generated Javadoc
/**
 * The Class RpcBackendGetMinisiteInfo.
 */
public class RpcBackendGetMinisiteInfo extends RpcBackEndImp<Vector,MinisiteInfoElement> {
	
	/** The parameters. */
	private Vector  parameters;
	
	/**
	 * Instantiates a new rpc backend get minisite info.
	 * 
	 * @param parameters the parameters
	 */
	public RpcBackendGetMinisiteInfo(Vector  parameters){
		super(parameters);
	}
	
	/**
	 * Instantiates a new rpc backend get minisite info.
	 * 
	 * @param language the language
	 * parametro indica la lingua in cui si vuole ottenere le informazioni,
	 * di tipo string e deve assumere uno di questi tre valori (attenzione che il parametro � case sensitive):
	 * 'it'
	 * 'en'
	 * 'de'
	 * @param structNumber the struct number
	 */
	public RpcBackendGetMinisiteInfo(
			String language,
			BigInteger structNumber
	){
		super(language,structNumber);
		
	}
	
	/* (non-Javadoc)
	 * @see ota.rpc.RpcBackEndImp#getFunction()
	 */
	@Override
	public String getFunction() { 
		return "backend.getMinisiteInfo";
	}

 
	/* (non-Javadoc)
	 * @see ota.rpc.RpcBackEndImp#getResults(java.util.Map)
	 */
	@Override
	public MinisiteInfoElement getResults(Map values) { 
		return new MinisiteInfoElement(values);
	}
 
	/* (non-Javadoc)
	 * @see ota.rpc.RpcBackEndImp#getResults(java.lang.Object, int)
	 */
	@Override
	public MinisiteInfoElement getResults(Vector data, int index) { 
		return getResults((Map) data.get(index));	
	}

}
