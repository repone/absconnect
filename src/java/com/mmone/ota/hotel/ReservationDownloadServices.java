/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mmone.ota.hotel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
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
            + " ( context_id, confirmation_number, reservation_id, last_check_date,creation_date )"
            + " VALUES "
            + " ( ?, ?, ?, ?, ?)"
            + "";
    public static final String SQL_INSERT_RESERVATION_DOWNLOADED_RECORD_AUTO = ""
            + " INSERT INTO ota_reservation_download"
            + " ( context_id, confirmation_number, reservation_id, last_check_date,creation_date )"
            + " VALUES "
            + " ( ?, ?, ?,  (SELECT MAX(reservation_status_date) FROM reservation_status WHERE reservation_status.reservation_id=?)   ,  ?)"
            + "";
    public static final String SQL_UPDATE_RESERVATION_DOWNLOADED_RECORD = ""
            + " UPDATE ota_reservation_download"
            + " SET "
            + " last_check_date=? "
            + " WHERE "
            + " context_id=? "
            + " AND reservation_id=? "
            + "";
    public static final String SQL_UPDATE_RESERVATION_DOWNLOADED_RECORD_AUTO = ""
            + " UPDATE ota_reservation_download"
            + " SET "
            + " last_check_date=(SELECT MAX(reservation_status_date) FROM reservation_status WHERE reservation_status.reservation_id=?) "
            + " WHERE "
            + " context_id=? "
            + " AND reservation_id=? "
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

    //carica id prenotazioni
    public static List<Object> retrieveReservationsId(DataSource ds, String hotelCode, Object contextId,int portalId) throws SQLException {
        QueryRunner run = new QueryRunner(ds);
        return run.query(SELECT_RESERVATIONS, new ColumnListHandler("reservation_id"), contextId, hotelCode,portalId);
    }
    
    //carica le prenotazioni
    public static List<Map<String, Object>> retrieveReservations(DataSource ds, String hotelCode, Object contextId,int portalId) throws SQLException {
        QueryRunner run = new QueryRunner(ds);
        return run.query(SELECT_RESERVATIONS, new MapListHandler(), contextId, hotelCode,portalId);
    }
    
    public static List<Map<String, Object>> retrieveReservationsOnlyBooking(DataSource ds, String hotelCode, Object contextId,int portalId) throws SQLException {
        QueryRunner run = new QueryRunner(ds);
        return run.query(SELECT_RESERVATIONS_ONLY_BOOKING, new MapListHandler(), contextId, hotelCode,portalId);
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
            DataSource ds, Object contextId, String confirmationNumber, Object reservationId, Timestamp lastCheckDate) throws Exception {

        QueryRunner qr = new QueryRunner(ds);
        Timestamp today = new Timestamp(new java.util.Date().getTime());

        try {
            if (lastCheckDate == null) {
                qr.update(SQL_INSERT_RESERVATION_DOWNLOADED_RECORD_AUTO, contextId, confirmationNumber, reservationId, reservationId, today);
            } else {
                qr.update(SQL_INSERT_RESERVATION_DOWNLOADED_RECORD, contextId, confirmationNumber, reservationId, lastCheckDate, today);
            }
            return;
        } catch (SQLException e) {
            /*errore se record esiste già*/
            //System.err.println("SQLIntegrityConstraintViolationException" );
        } catch (Exception e) {
            throw e;
        }

        try {
            if (lastCheckDate == null) {
                qr.update(SQL_UPDATE_RESERVATION_DOWNLOADED_RECORD_AUTO, reservationId, contextId, reservationId);
            } else {
                qr.update(SQL_UPDATE_RESERVATION_DOWNLOADED_RECORD, lastCheckDate, contextId, reservationId);
            }
        } catch (Exception e) {
            throw e;
        }

    }

    public static Map<String, Object> setReservationsAsDownloaded(DataSource ds, String token, String confirmation_number,String resIdType) throws Exception {
        List<Map<String, Object>> reservetions = retrieveDownloadedRecords(token, ds);
        Map<String, Object> dw = ReservationDownloadServices.retrieveContextAndHotelCOdeByToken(ds, token);

        //Connection conn = ds.getConnection();
        //conn.setAutoCommit(false);
        if(resIdType.equals(  Facilities.RESID_TYPE_PMS    )){
            for (Map<String, Object> record : reservetions){ 
                insertOrUpdateDownloadRecordCommitRequired(ds, dw.get("context_id"), confirmation_number, record.get("reservation_id"));
            }
            deleteReservationRequestRecordCommitRequired(ds, token);
        }else{
            for (Map<String, Object> record : reservetions) {
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

    public static void main(String[] args) {
        try {  
            DataSource ds= Facilities.createDataSource("absaja_user", "Nim9ofdekyozEp", "jdbc:mysql://93.95.221.43:3306/absaja_db");             
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
