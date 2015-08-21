package com.mmone.ota.rpc;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;

// TODO: Auto-generated Javadoc
/**
 * The Class RpcBackEndImp.
 */
public abstract class  RpcBackEndImp<T,S> implements RpcBackEnd<T,S>{
	
	/**
	 * Instantiates a new rpc back end imp.
	 * 
	 * @param parameters the parameters
	 */
	public RpcBackEndImp(Vector parameters) {
		super(); 
		this.parameters = parameters;
	}
	
	/**
	 * Instantiates a new rpc back end imp.
	 * 
	 * @param language the language
	 * @param structNumber the struct number
	 */
	public RpcBackEndImp(
			String language,
			BigInteger structNumber
	){
		super(); 
		getParameters().add(language);
		getParameters().add(structNumber.intValue());
	
	}	

	/** The parameters. */
	private Vector  parameters;
	
	/* (non-Javadoc)
	 * @see ota.rpc.RpcBackEnd#execute(org.apache.xmlrpc.XmlRpcClient)
	 */
	@Override
	public T execute(XmlRpcClient client) throws XmlRpcException,
			IOException {
		T result = (T) client.execute(getFunction()  , getParameters());	
		return result;
	}

	/* (non-Javadoc)
	 * @see ota.rpc.RpcBackEnd#execute(org.apache.xmlrpc.XmlRpcClient, java.util.Vector)
	 */
	@Override
	public T execute(XmlRpcClient client,  Vector parameters ) throws XmlRpcException,
			IOException {
		T result = (T) client.execute(getFunction()  , parameters);	
		return result;
	}
 
	 
	/* (non-Javadoc)
	 * @see ota.rpc.RpcBackEnd#getParameters()
	 */
	@Override
	public Vector getParameters(){
		if (parameters==null) parameters = new Vector();
		return parameters;
	}
	
	/* (non-Javadoc)
	 * @see ota.rpc.RpcBackEnd#setParameters(java.util.Vector)
	 */
	@Override
	public void setParameters(Vector parameters) {
		this.parameters = parameters;
	}

}
