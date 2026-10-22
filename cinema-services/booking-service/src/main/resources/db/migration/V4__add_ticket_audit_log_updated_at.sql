-- Keep the audit history table aligned with BaseEntity's audit columns.
ALTER TABLE ticket_audit_logs
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

UPDATE ticket_audit_logs
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE ticket_audit_logs
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN updated_at SET NOT NULL;
