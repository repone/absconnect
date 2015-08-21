/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mmone.ota.hotel;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 * @author mauro.larese
 */
public class BaseBuilder {
    public static final int JNDIVER_100_AFTER_SINGLE_GUEST = 100;
    public static final int JNDIVER_101_AFTER_LIKE_ON_OTAUSERS = 101;
     
    private int jndiVersion = 0;
    public int getJndiVersion() {  
        Logger.getLogger(BaseBuilder.class.getName()).log(Level.INFO, "jndiVersion="+jndiVersion );
        //return jndiVersion;     
        return JNDIVER_101_AFTER_LIKE_ON_OTAUSERS;
    }
    public void setJndiVersion(int jndiVersion) { this.jndiVersion = jndiVersion; }
    private InitialContext initialContext=null;
    public InitialContext getInitialContext() { return initialContext;  } 
    public void setInitialContext(InitialContext initialContext) {  
        this.initialContext = initialContext;  
        
        if(initialContext==null) clearParameters();
        else setParameters(this.initialContext);
    }
    private void clearParameters(){
        jndiVersion = 0;
    }
    /*
     * jndiVersion 100
     *      correzione singleGuest 
     * jndiVersion 101
     *      MOdifica per accesso
     */
    private void setParameters(InitialContext icx){
         try {
             jndiVersion = (Integer) getInitialContext().lookup("version/AbsConnectService/number");
         } catch (NamingException namingException) {
             jndiVersion= 0;
         }
         
    }
    
    
    
}
