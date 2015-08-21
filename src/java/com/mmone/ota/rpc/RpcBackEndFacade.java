package com.mmone.ota.rpc;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;

public class RpcBackEndFacade {
	private String language;
	private BigInteger structNumber;
	private XmlRpcClient client;
	private Vector lastMinisiteAmenities;
	private Vector lastMinisiteInfo;
	private Vector lastMinisitePhotos;
	
	public Vector getLastMinisitePhotos() {
		return lastMinisitePhotos;
	}

	public void setLastMinisitePhotos(Vector lastMinisitePhotos) {
		this.lastMinisitePhotos = lastMinisitePhotos;
	}

	public RpcBackEndFacade(String language, BigInteger structNumber,
			XmlRpcClient client) {
		super();
		this.language = language;
		this.structNumber = structNumber;
		this.client = client;
	} 
	
	public RpcBackendGetMinisiteAmenities newRpcBackendGetMinisiteAmenities(){
		return new RpcBackendGetMinisiteAmenities(this.language,this.structNumber);
	}
	 
	public RpcBackendGetMinisiteInfo newRpcBackendGetMinisiteInfo(){
		return new RpcBackendGetMinisiteInfo(this.language,this.structNumber);
	}
	
	public RpcBackendGetMinisitePhotos newRpcBackendGetMinisitePhotos(){
		return new RpcBackendGetMinisitePhotos(this.language,this.structNumber);
	}
	
	public Vector  rpcBackendGetMinisiteAmenitiesExec() throws XmlRpcException, IOException{
		lastMinisiteAmenities =  newRpcBackendGetMinisiteAmenities().execute(client);		
		return lastMinisiteAmenities;
	}
	
	public Vector  rpcBackendGetMinisiteInfoExec() throws XmlRpcException, IOException{
		lastMinisiteInfo =  newRpcBackendGetMinisiteInfo().execute(client);	
		return lastMinisiteInfo;
	}
	
	public Vector  rpcBackendGetMinisitePhotosExec() throws XmlRpcException, IOException{
		lastMinisitePhotos = newRpcBackendGetMinisitePhotos().execute(client);		
		return lastMinisitePhotos;
	}
}
