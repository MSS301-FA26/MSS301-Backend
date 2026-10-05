ALTER TABLE ticket_pricing_rules ADD COLUMN IF NOT EXISTS effective_from TIMESTAMP;
ALTER TABLE ticket_pricing_rules ADD COLUMN IF NOT EXISTS effective_to TIMESTAMP;
