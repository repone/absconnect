package com.mmone.ota.rpc;

import java.io.IOException;
import java.util.Map;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;
 
// TODO: Auto-generated Javadoc
/**
 * The Interface RpcBackEnd.
 */
interface RpcBackEnd<T,S>{
	
	/**
	 * Gets the parameters.
	 * 
	 * @return the parameters
	 */
	Vector getParameters();
	
	void setParameters(Vector parameters); 
	 
	/**
	 * Gets the function.
	 * 
	 * @return the function
	 */
	String getFunction();
	 
	/**
	 * Execute.
	 * 
	 * @param client the client
	 * 
	 * @return the t
	 * 
	 * @throws XmlRpcException the xml rpc exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	T execute(XmlRpcClient client)  throws XmlRpcException, IOException;
 
	/**
	 * Execute.
	 * 
	 * @param client the client
	 * @param parameters the parameters
	 * 
	 * @return the t
	 * 
	 * @throws XmlRpcException the xml rpc exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	T execute(XmlRpcClient client,Vector parameters)  throws XmlRpcException, IOException;
 
	/**
	 * Gets the results.
	 * 
	 * @param values the values
	 * 
	 * @return the results
	 */
	S getResults(Map values);
	
	 
	/**
	 * Gets the results.
	 * 
	 * @param data the data
	 * @param index the index
	 * 
	 * @return the results
	 */
	S getResults(T data,int index);
}
