-- Rollback (수동):
-- DROP INDEX idx_fcm_outbox_user_id ON fcm_outbox;
-- ALTER TABLE fcm_outbox DROP COLUMN user_id, DROP COLUMN firebase_message_id;

ALTER TABLE fcm_outbox
    ADD COLUMN user_id BIGINT NULL,
    ADD COLUMN firebase_message_id VARCHAR(255) NULL;

CREATE INDEX idx_fcm_outbox_user_id ON fcm_outbox(user_id);
