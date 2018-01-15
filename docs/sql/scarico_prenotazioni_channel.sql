ALTER TABLE ota_reservation_download
 ADD reservation_ch_id VARCHAR(200) NOT NULL;
ALTER TABLE ota_reservation_download
  ADD INDEX (context_id, reservation_ch_id);

UPDATE ota_reservation_download
SET
  reservation_ch_id = reservation_id + ''