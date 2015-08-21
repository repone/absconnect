package com.mmone.ota.rpc;

import java.math.BigInteger;
import java.util.Map;
import java.util.Vector;

// TODO: Auto-generated Javadoc
/**
 * The Class RpcBackendGetMinisitePhotos.
 */
public class RpcBackendGetMinisitePhotos extends RpcBackEndImp<Vector,MinisitePhotosElement> {
	 
	/**
	 * Instantiates a new rpc backend get minisite photos.
	 * 
	 * @param parameters the parameters
	 */
	public RpcBackendGetMinisitePhotos(Vector  parameters) {
		super(parameters);  
	}

	/**
	 * Instantiates a new rpc backend get minisite photos.
	 * 
	 * @param language the language
	 * @param structNumber the struct number
	 */
	public RpcBackendGetMinisitePhotos(
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
		// TODO Auto-generated method stub
		return "backend.getMinisitePhotos";
	}


	/* (non-Javadoc)
	 * @see ota.rpc.RpcBackEndImp#getResults(java.util.Map)
	 */
	@Override
	public MinisitePhotosElement getResults(Map values) {
		MinisitePhotosElement m = new MinisitePhotosElement(values);
		return m;
	}

	/* (non-Javadoc)
	 * @see ota.rpc.RpcBackEndImp#getResults(java.lang.Object, int)
	 */
	@Override
	public MinisitePhotosElement getResults(Vector data, int index) {
		return getResults((Map) data.get(index));	
	}

}
