/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mmone.ota.hotel;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.sql.DataSource;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.ColumnListHandler;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;
import org.opentravel.ota._2003._05.HotelReservationType;

/**
 *
 * @author mlarese
 * Giro del fumo nell'utilizzo di questi servizi nel caso di OTAReadRQ
 * - Effettuata una richiesta di tipo OTAReadRQ
 * - vengono scricate tutte le prenotazoni attraverso retrieveReservations
 * - viene creata una response di tipo OTAResRetrieveRS
 * - viene scritto un record di richiesta prenotazioni con insertDownloadRequestRecordCommitRequired
 * - subito dopo viene inviata una richiesta di tipo OTANotifReportRQ
 * - vengono aggiornate le richieste relative al token comune alle requests tramite setReservationsAsDownloaded 
 *
 */
public class ReservationDownloadServices {
    public static final int DOWNLOAD_LIMIT = 250;
    public static final String SQL_DELETE_RESERVATION_DOWNLOAD_REQUEST_RECORD = ""
            + " DELETE FROM ota_reservation_download_request"
            + " WHERE request_token=? "
            + "";
    public static final String SQL_INSERT_RESERVATION_DOWNLOAD_REQUEST_RECORD = ""
            + " INSERT INTO ota_reservation_download_request"
            + " ( request_token, request_date, hotel_code , context_id,creation_date )"
            + " VALUES "
            + " ( ?, ?, ?,?,?)"
            + "";
    public static final String SQL_INSERT_RESERVATION_DOWNLOADED_RECORD = ""
            + " INSERT INTO ota_reservation_download"
            + " ( context_id, confirmation_number, reservation_id, reservation_ch_id, last_check_date,creation_date )"
            + " VALUES "
            + " ( ?, ?, ?, ?, ?, ?)"
            + "";
  
    
    /*
    ALTER TABLE cmsonei_abs2.ota_reservation_download
    CHANGE reservation_id reservation_id VARCHAR(255) NOT NULL;
    **/
    public static final String SQL_INSERT_RESERVATION_DOWNLOADED_RECORD_AUTO = ""
            + " INSERT INTO ota_reservation_download"
            + " ( context_id, confirmation_number, reservation_id, reservation_ch_id, last_check_date,creation_date )"
            + " VALUES "
            + " ( ?, ?, ?, ?,  CURRENT_TIMESTAMP   ,  CURRENT_TIMESTAMP)"
            + "";
    public static final String SQL_UPDATE_RESERVATION_DOWNLOADED_RECORD = ""
            + " UPDATE ota_reservation_download"
            + " SET "
            + " last_check_date=? "
            + " WHERE "
            + " context_id=? "
            + " AND reservation_ch_id=? "
            + "";
    public static final String SQL_UPDATE_RESERVATION_DOWNLOADED_RECORD_AUTO = ""
            + " UPDATE ota_reservation_download"
            + " SET "
            + " last_check_date=CURRENT_TIMESTAMP "
            + " WHERE "
            + " context_id=? "
            + " AND reservation_ch_id=? "
            + "";
    
        public static final String SQL_SELECT_DOWNLOADED_RECORDS_BY_TOKEN = ""
            + " SELECT * "
            + " FROM  "
            + " ota_reservation_download_request "
            + " WHERE "
            + " request_token=? "
            + "";
     
     public static String SELECT_RESERVATIONS_NO_PORTAL_ID = ""
            + " SELECT "
            + "   reservation.*, "
            + "   ota_reservation_download.context_id AS context_id, "
            + "    ((SELECT MAX(reservation_status_date) FROM reservation_status"
            + " WHERE reservation_status.reservation_id=reservation.reservation_id)) AS reservation_status_date, "
            + "    ota_reservation_download.last_check_date "
            + " FROM reservation "
            + " LEFT JOIN ota_reservation_download ON ( "
            + "    ota_reservation_download.reservation_id = reservation.reservation_id "
            + "    AND ota_reservation_download.context_id = ? "
            + " ) "
            + " WHERE  ( "
            + "           NOT  ("
            + "                            (SELECT MAX(reservation_status_date) FROM reservation_status "
            + "                                WHERE reservation_status.reservation_id=reservation.reservation_id)"
            + "                             = last_check_date "
            + "           ) "
            + "           OR   (last_check_date IS NULL) "
            + "        ) "
            + "        AND structure_id = ? "
            + "        AND (reservation_type=1) " 
            + "";
     
     public static String SELECT_RESERVATIONS = ""
            + " SELECT "
            + "   reservation.*, "
            + "   ota_reservation_download.context_id AS context_id, "
            + "    ((SELECT MAX(reservation_status_date) FROM reservation_status"
            + " WHERE reservation_status.reservation_id=reservation.reservation_id)) AS reservation_status_date, "
            + "    ota_reservation_download.last_check_date "
            + " FROM reservation "
            + " LEFT JOIN ota_reservation_download ON ( "
            + "    ota_reservation_download.reservation_id = reservation.reservation_id "
            + "    AND ota_reservation_download.context_id = ? "
            + " ) "
            + " WHERE  ( "
            + "           NOT  ("
            + "                            (SELECT MAX(reservation_status_date) FROM reservation_status "
            + "                                WHERE reservation_status.reservation_id=reservation.reservation_id)"
            + "                             = last_check_date "
            + "           ) "
            + "           OR   (last_check_date IS NULL) "
            + "        ) "
            + "        AND structure_id = ? "
            + "        AND (reservation_type=1) "
            + "        AND (reservation.portal_id>=? OR reservation.portal_id>0)  "
           // + "        AND (reservation.portal_id=?) "
            + "";
     
    public static String SELECT_RESERVATIONS_ONLY_BOOKING = ""
            + SELECT_RESERVATIONS
            + "        AND  NOT (reservation_number like 'M/%'  AND reservation_insert_type=2)  " 
            + ""; 
    
    public static String SELECT_RESERVATIONS_ID = ""
            + " SELECT "
            + "    reservation.reservation_id, "
            + "    (SELECT MAX(reservation_status_date) FROM reservation_status WHERE reservation_status.reservation_id=reservation.reservation_id)) AS reservation_status_date, "
            + "    ota_reservation_download.last_check_date "
            + " FROM reservation "
            + " LEFT JOIN ota_reservation_download ON ( "
            + "    ota_reservation_download.reservation_id = reservation.reservation_id "
            + "    AND ota_reservation_download.context_id = ? "
            + " ) "
            + " WHERE  ( "
            + "           NOT  ((SELECT MAX(reservation_status_date) FROM reservation_status WHERE reservation_status.reservation_id=reservation.reservation_id) = last_check_date ) "
            + "           OR   (last_check_date IS NULL) "
            + "        ) "
            + "        AND structure_id = ? "
            + "        AND (reservation_type=1 OR reservation_type=2) "
            + "        AND (reservation.portal_id>=? OR reservation.portal_id>0)  "
           // + "        AND (reservation.portal_id=?) "
            + "";
    public static final String SQL_CLEAN_RESERVATION_DOWNLOAD_REQUEST_RECORD = ""
            + " DELETE FROM ota_reservation_download_request "
            + " WHERE creation_date<? "
            + "";

    public ReservationDownloadServices() {
    }

//    //elimina i record da reservation_download_request più vecchi di 15 minuti
//    public static void cleanReservationRequestRecordCommitRequired(Connection conn) throws Exception{
//        java.util.Date cleaningDate = DateUtils.addMinutes(new java.util.Date(),-15);
//        QueryRunner run = new QueryRunner();
//
//        try {
//            run.update(conn, SQL_CLEAN_RESERVATION_DOWNLOAD_REQUEST_RECORD, cleaningDate);
//        } catch (Exception e) {
//            throw e;
//        }
//
//    }
    //cancella un record dalle   reservation_download_request in base al   request_token
    public static void deleteReservationRequestRecordCommitRequired(DataSource ds, String token) throws Exception {
        PreparedStatement dps = null;
        try {
            QueryRunner run = new QueryRunner(ds);
            run.update(SQL_DELETE_RESERVATION_DOWNLOAD_REQUEST_RECORD, token);
        } catch (Exception e) {
            throw e;
        }
    }
 //carica le prenotazioni
    public static List<Map<String, Object>> retrieveReservations(DataSource ds, String hotelCode, Object contextId,int portalId) throws SQLException {
        QueryRunner run = new QueryRunner(ds);
        return run.query(SELECT_RESERVATIONS, new MapListHandler(), contextId, hotelCode,portalId);
    }
    //carica id prenotazioni
    public static List<Object> retrieveReservationsId(DataSource ds, String hotelCode, Object contextId,int portalId) throws SQLException {
        QueryRunner run = new QueryRunner(ds);
        return run.query(SELECT_RESERVATIONS, new ColumnListHandler("reservation_id"), contextId, hotelCode,portalId);
    }
    
    public static List<Map<String, Object>> retrieveReservationsByRpc(
        XmlRpcClient client, 
        boolean onlyBooking, 
        String hotelCode, 
        String contextId,
        boolean onlyId,
        boolean checkChannels
    ) throws XmlRpcException, IOException {
        Vector result = new Vector();
        Vector parameters = new Vector();

        parameters.add(new Integer(hotelCode)); 
        parameters.add(contextId);
        
        if(onlyId) parameters.add("reservation_id"); 
        else parameters.add("*"); 
        
        if(onlyBooking) parameters.add(new Integer(1)); 
        else parameters.add(new Integer(0)); 
        
        if(checkChannels) parameters.add(new Integer(1)); 
        else parameters.add(new Integer(0)); 
        
        result = (Vector) client.execute("backend.getOTAReservations", parameters);         
        return new ArrayList<Map<String, Object>>(result);
    }
    //carica le prenotazioni
    
    public static List<Map<String, Object>> retrieveReservationsIdByRpc(
        XmlRpcClient client,  
        String hotelCode, 
        String contextId,
        boolean checkChannels,
        boolean onlyBooking 
    ) throws XmlRpcException, IOException  {
        boolean onlyId = true;
        return retrieveReservationsByRpc(client, onlyBooking, hotelCode, contextId, onlyId, checkChannels);
    }
    
    //carica le prenotazioni
    public static List<Map<String, Object>> retrieveReservationsOnlyBooking(DataSource ds, String hotelCode, Object contextId,int portalId) throws SQLException {
        QueryRunner run = new QueryRunner(ds);
        return run.query(SELECT_RESERVATIONS_ONLY_BOOKING, new MapListHandler(), contextId, hotelCode,portalId);
    }
    //carica le prenotazioni rpc
    public static List<Map<String, Object>> retrieveReservationsOnlyBookingRPC(
        XmlRpcClient client,  
        String hotelCode, 
        String contextId,
        boolean checkChannels
    ) throws XmlRpcException, IOException {
        boolean onlyBooking = true; 
        boolean onlyId = false;
        return retrieveReservationsByRpc(client, onlyBooking, hotelCode, contextId, onlyId, checkChannels);
    }
    //carica le prenotazioni rpc
    public static List<Map<String, Object>> retrieveReservationsAllRPC(
        XmlRpcClient client,  
        String hotelCode, 
        String contextId,
        boolean checkChannels
    ) throws XmlRpcException, IOException {
        boolean onlyBooking = false; 
        boolean onlyId = false;
        return retrieveReservationsByRpc(client, onlyBooking, hotelCode, contextId, onlyId, checkChannels);
    }
    
    public static List<Map<String, Object>> retrieveReservationsNoPortalId(DataSource ds, String hotelCode, Object contextId) throws SQLException {
        QueryRunner run = new QueryRunner(ds);
        return run.query(SELECT_RESERVATIONS_NO_PORTAL_ID, new MapListHandler(), contextId, hotelCode);
    }
    
    //carica le prenotazioni
    //public static List<Map<String,Object>> retrieveReservations(DataSource ds, String hotelCode, Object contextId) throws SQLException{
    //    return retrieveReservations(ds, hotelCode, contextId);
    // }
    //carica le prenotazioni relative ad un token di una richiesta precedente
    public static List<Map<String, Object>> retrieveDownloadedRecords(
            String token, 
            DataSource ds
            ) throws Exception, DownloadRequestNotFoundException, SQLException, DownloadRequestMalformedException {

        QueryRunner qr = new QueryRunner(ds);
        Map dwnRecs = qr.query(SQL_SELECT_DOWNLOADED_RECORDS_BY_TOKEN, new MapHandler(), token);

        if (dwnRecs == null || dwnRecs.size() == 0) {
            throw new DownloadRequestNotFoundException("Download request not found for token " + token);
        }

        String hotelCode = (String) dwnRecs.get("hotel_code");
        Object contextId = dwnRecs.get("context_id");

        if (hotelCode == null) {
            throw new DownloadRequestMalformedException("HotelCode is null");
        }

        if (contextId == null) {
            throw new DownloadRequestMalformedException("CONTEXT_ID is null");
        }
        List<Map<String, Object>> reservations = null;
        try {
            reservations = retrieveReservationsNoPortalId(ds, hotelCode, contextId);
        } catch (Exception e) {
            throw e;
        }

        return reservations;

    }

    public static List<Map<String, Object>> retrieveDownloadedRecordsRpc(
            String token, 
            XmlRpcClient client,
            DataSource ds
            ) throws Exception, DownloadRequestNotFoundException, SQLException, DownloadRequestMalformedException {

        QueryRunner qr = new QueryRunner(ds);
        Map dwnRecs = qr.query(SQL_SELECT_DOWNLOADED_RECORDS_BY_TOKEN, new MapHandler(), token);

        if (dwnRecs == null || dwnRecs.size() == 0) {
            throw new DownloadRequestNotFoundException("Download request not found for token " + token);
        }

        String hotelCode = (String) dwnRecs.get("hotel_code");
        Object contextId = dwnRecs.get("context_id");

        if (hotelCode == null) {
            throw new DownloadRequestMalformedException("HotelCode is null");
        }

        if (contextId == null) {
            throw new DownloadRequestMalformedException("CONTEXT_ID is null");
        }
        
        List<Map<String, Object>> reservations = null;
        try { 
            reservations = retrieveReservationsAllRPC(
              client, 
              hotelCode, 
              (String)contextId,
              true
            );
        } catch (Exception e) {
            throw e;
        }

        return reservations;

    }

    
    public static List<Map<String, Object>> retrieveDownloadedRecords(
            String hotelCode,
            Object contextId,
            DataSource ds,
            int portalId
            ) throws Exception, DownloadRequestNotFoundException, SQLException, DownloadRequestMalformedException {

        QueryRunner qr = new QueryRunner(ds);
        if (contextId == null) {
            throw new DownloadRequestMalformedException("CONTEXT_ID null");
        }
        List<Map<String, Object>> reservations = null;
        try {
            reservations = retrieveReservations(ds, hotelCode, contextId,portalId);
        } catch (Exception e) {
            throw e;
        }

        return reservations;

    }

    public static Map<String, Object> retrieveContextAndHotelCOdeByToken(DataSource ds, String token) throws SQLException {
        QueryRunner run = new QueryRunner(ds);
        return run.query(SQL_SELECT_DOWNLOADED_RECORDS_BY_TOKEN, new MapHandler(), token);
    }

    //carica gli id delle  prenotazioni relative ad un token di una richiesta precedente
    public static List<Object> retrieveDownloadedRecordsId(
            String token,
            DataSource ds,
            int portalId
            ) throws Exception, DownloadRequestNotFoundException, SQLException, DownloadRequestMalformedException {

        QueryRunner qr = new QueryRunner(ds);
        Map dwnRecs = qr.query(SQL_SELECT_DOWNLOADED_RECORDS_BY_TOKEN, new MapHandler(), token);
                 
        if (dwnRecs == null || dwnRecs.size() == 0) {
            throw new DownloadRequestNotFoundException("Download request not found for token " + token);
        }

        String hotelCode = (String) dwnRecs.get("hotel_code");

        Object contextId = dwnRecs.get("context_id");

        if (hotelCode == null) {
            throw new DownloadRequestMalformedException("HotelCode null");
        }

        if (contextId == null) {
            throw new DownloadRequestMalformedException("CONTEXT_ID null");
        }

        List<Object> reservations = null;
        try {
            reservations = retrieveReservationsId(ds, hotelCode, contextId,portalId);
        } catch (Exception e) {
            throw e;
        }

        return reservations;

    }
    // inserisce un record di richiesta di download delle prenotazioni.

    public static List<Map<String, Object>> loadReservationOtherData(Map<String, Object> reservation) {
        Map res = (Map)reservation.get("reservation_details");
        Collection values = res.values();
        ArrayList ret = new ArrayList(values);
        return ret;
    }
    public static void insertDownloadRequestRecordCommitRequired(
            DataSource ds, String requestToken, String hotelCode, Object contextId) throws Exception {

        QueryRunner qr = new QueryRunner(ds);
        Timestamp today = new Timestamp(new java.util.Date().getTime());
        Timestamp requestDate = today;

        try {
            qr.update(SQL_INSERT_RESERVATION_DOWNLOAD_REQUEST_RECORD, requestToken, requestDate, hotelCode, contextId, today);
        } catch (Exception e) {
            throw e;
        }
    }
    //crea o aggiorna un record della tabella delle prenotazioni scaricate.

    public static void insertOrUpdateDownloadRecordCommitRequired(
            DataSource ds, Object contextId, String confirmationNumber, Object reservationId) throws Exception {
        insertOrUpdateDownloadRecordCommitRequired(ds, contextId, confirmationNumber, reservationId, null);
    }
    //crea o aggiorna un record della tabella delle prenotazioni scaricate.

    public static void insertOrUpdateDownloadRecordCommitRequired(
            DataSource ds, Object contextId, String confirmationNumber, Object newReservationId, Timestamp lastCheckDate) throws Exception {

        
        QueryRunner qr = new QueryRunner(ds);
        Timestamp today = new Timestamp(new java.util.Date().getTime());
        
        String confirm=null;
        if (confirmationNumber == null || confirmationNumber.length() <= 0) {
            confirm = "c_c";
        } else if (confirmationNumber.length() <= 30) {
            confirm = confirmationNumber;
        } else { 
            confirm = confirmationNumber.substring(0, 30);
        } 
        
        Object reservationId = reservationIdWithChannelToReservationId(newReservationId);
        // Logger.getLogger(ReservationDownloadServices.class.getName()).log(Level.INFO, "--- reservationId = " + reservationId.toString());
        
        try {
            // if (lastCheckDate == null)  lastCheckDate = today; 
            if (lastCheckDate == null) {
                qr.update(SQL_INSERT_RESERVATION_DOWNLOADED_RECORD_AUTO, contextId, confirm, reservationId, newReservationId);
            } else {
                qr.update(SQL_INSERT_RESERVATION_DOWNLOADED_RECORD, contextId, confirm, reservationId, newReservationId, lastCheckDate, today);
            }
            return;
        } catch (SQLException e) {
            /*errore se record esiste già*/
            Logger.getLogger(ReservationDownloadServices.class.getName()).log(Level.SEVERE, e.getMessage());
        } catch (Exception e) {
            throw e;
        }

        try {
            // if (lastCheckDate == null)  lastCheckDate = today; 
            if (lastCheckDate == null) {
                qr.update(SQL_UPDATE_RESERVATION_DOWNLOADED_RECORD_AUTO, contextId, newReservationId);
            } else {
                qr.update(SQL_UPDATE_RESERVATION_DOWNLOADED_RECORD, lastCheckDate, contextId, newReservationId);
            }
        } catch (Exception e) {
            throw e;
        }

    }

    public static Map<String, Object> setReservationsAsDownloadedRpc(XmlRpcClient client, DataSource ds, String token, String confirmation_number,String resIdType) throws Exception {
        List<Map<String, Object>> reservetions = retrieveDownloadedRecordsRpc(token, client,ds);
        Map<String, Object> dw = ReservationDownloadServices.retrieveContextAndHotelCOdeByToken(ds, token);

        //Connection conn = ds.getConnection();
        //conn.setAutoCommit(false);
        int resTokenCounter=0;
        if(resIdType.equals(  Facilities.RESID_TYPE_PMS    )){
            for (Map<String, Object> record : reservetions){ 
                resTokenCounter++;
                if(resTokenCounter>ReservationDownloadServices.DOWNLOAD_LIMIT)
                    break;
                
                String newResId = (String)record.get("new_reservation_id");
                
                // insertOrUpdateDownloadRecordCommitRequired(ds, dw.get("context_id"), confirmation_number, record.get("reservation_id"));     
                insertOrUpdateDownloadRecordCommitRequired(
                        ds, 
                        dw.get("context_id"), 
                        confirmation_number, 
                        newResId);     
                
            }
            deleteReservationRequestRecordCommitRequired(ds, token);
        }else{
            for (Map<String, Object> record : reservetions) {
                resTokenCounter++;
                if(resTokenCounter>ReservationDownloadServices.DOWNLOAD_LIMIT)
                    break;
                String resNumber =  (String) record.get("new_reservation_number");
                
                if(confirmation_number.equals( resNumber ) ){
                    Logger.getLogger(ReservationDownloadServices.class.getName()).log(Level.INFO, "--- found reservation = " + resNumber );
                    // insertOrUpdateDownloadRecordCommitRequired(ds, dw.get("context_id"), confirmation_number, record.get("reservation_id"));
                    insertOrUpdateDownloadRecordCommitRequired(ds, dw.get("context_id"), confirmation_number, record.get("new_reservation_id"));
                    if(reservetions.size()==1)
                        deleteReservationRequestRecordCommitRequired(ds, token);
                    break;
                }    
            } 
        }
        
        

        //DbUtils.close(conn);
        //DbUtils.commitAndCloseQuietly(conn);
        return dw;
    }

    
    public static Map<String, Object> setReservationsAsDownloaded(DataSource ds, String token, String confirmation_number,String resIdType) throws Exception {
        List<Map<String, Object>> reservetions = retrieveDownloadedRecords(token, ds);
        Map<String, Object> dw = ReservationDownloadServices.retrieveContextAndHotelCOdeByToken(ds, token);

        //Connection conn = ds.getConnection();
        //conn.setAutoCommit(false);
        int resTokenCounter=0;
        if(resIdType.equals(  Facilities.RESID_TYPE_PMS    )){
            for (Map<String, Object> record : reservetions){ 
                resTokenCounter++;
                if(resTokenCounter>ReservationDownloadServices.DOWNLOAD_LIMIT)
                    break;
                insertOrUpdateDownloadRecordCommitRequired(ds, dw.get("context_id"), confirmation_number, record.get("reservation_id"));     
            }
            deleteReservationRequestRecordCommitRequired(ds, token);
        }else{
            for (Map<String, Object> record : reservetions) {
                resTokenCounter++;
                if(resTokenCounter>ReservationDownloadServices.DOWNLOAD_LIMIT)
                    break;
                String resNumber = (String)record.get("reservation_number");    
                if(confirmation_number.equals( resNumber ) ){
                    insertOrUpdateDownloadRecordCommitRequired(ds, dw.get("context_id"), confirmation_number, record.get("reservation_id"));
                    if(reservetions.size()==1)
                        deleteReservationRequestRecordCommitRequired(ds, token);
                    break;
                }    
            } 
        }
        
        

        //DbUtils.close(conn);
        //DbUtils.commitAndCloseQuietly(conn);
        return dw;
    }

    public static Object reservationIdWithChannelToReservationId(Object reservationId){ 
        String sReservationId = reservationId.toString();
        String [] aReservationId = sReservationId.split("-");
        if(aReservationId.length==1){
            //
        } else {
            reservationId = new Integer("-"+aReservationId[1]);
        }
        return reservationId;
    }
    
    public static void main(String[] args) {
        /**
         * 
            ALTER TABLE cmsonei_abs2_20171204.ota_reservation_download
            CHANGE reservation_id reservation_id VARCHAR(255) NOT NULL;
         */
        
        Object reservationId = "126-134050134";
        
        reservationId = reservationIdWithChannelToReservationId(reservationId);
        
        
        System.out.println("reservationId " + reservationId);
        
        if(1==1) return;
        
        
        DataSource ds;             
        try {
            ds = Facilities.createDataSource("root", "123abcD", "jdbc:mysql://10.0.20.16:3306/cmsonei_abs2_20171204");
            
            Map ret = retrieveContextAndHotelCOdeByToken(ds, "4ce75c91-be1f-4939-b1ef-e49a66ecdf3f");       
            
            //DataSource ds, Object contextId, String confirmationNumber, Object reservationId
            insertOrUpdateDownloadRecordCommitRequired(ds,"test217ml", "h/01/ml217","100-52034");
             
            
            MapUtils.debugPrint(System.out, "", ret);
            
            
            
            
        } catch (NamingException ex) {
            Logger.getLogger(ReservationDownloadServices.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(ReservationDownloadServices.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(ReservationDownloadServices.class.getName()).log(Level.SEVERE, null, ex);
        }
            
        
        if(1==1) return;
        
        try {  
            Vector a = new Vector();
            Map rec = new HashMap(); rec.put("a",1); rec.put("b",2); rec.put("c",3); a.add(rec);
            rec = new HashMap(); rec.put("a",1); rec.put("b",2); rec.put("c",3); a.add(rec);
            rec = new HashMap(); rec.put("a",1); rec.put("b",2); rec.put("c",3); a.add(rec);
            
            List<Map<String, Object>>l = new ArrayList(a);
            Map p = new HashMap();
            p.put("1", l);
            
            MapUtils.debugPrint(System.out, "", p);
            
            
            if(1==1) return;
            ds= Facilities.createDataSource("absaja_user", "Nim9ofdekyozEp", "jdbc:mysql://93.95.221.43:3306/absaja_db");             
            QueryRunner run = new QueryRunner(ds);
             
            String sql =  "SELECT reservation_status from reservation where reservation_number=?";
                
            Object reservationStatus = run.query(sql, new ScalarHandler("reservation_status"), "1/2009/R10830");

            String resStatus = OTAResRetrieveRSBuilder.RESERVATION_STATUS_TO_MM_CODE.get(OTAResRetrieveRSBuilder.RESERVATION_STATUS_CODE_WAITING_TO_CONFIRM);
            resStatus = OTAResRetrieveRSBuilder.RESERVATION_STATUS_TO_MM_CODE.get(reservationStatus.toString());

            HotelReservationType hotelReservationType = new HotelReservationType();

            hotelReservationType.setResStatus(resStatus);
 
            if(1==1) return;
            
            Connection c = ds.getConnection(); 
            System.out.println(c.isValid(100));
             
            if(1==1) return;
            QueryRunner qr = new QueryRunner(ds);
            String token= "9bc49c0b-4deb-4dad-8c8f-49c959680af7" ;
            Map dwnRecs = qr.query(SQL_SELECT_DOWNLOADED_RECORDS_BY_TOKEN, new MapHandler(), token);

            MapUtils.debugPrint( System.out , "token", dwnRecs);
        } catch ( Exception ex) {
            Logger.getLogger(ReservationDownloadServices.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
