ALTER TABLE ticket_pricing_rules ADD COLUMN cinema_id BIGINT;

CREATE INDEX idx_ticket_pricing_rules_cinema ON ticket_pricing_rules(cinema_id);
