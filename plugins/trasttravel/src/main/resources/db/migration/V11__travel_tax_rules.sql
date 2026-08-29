ALTER TABLE travel_tour ADD COLUMN tax_exempt BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE travel_tour ADD COLUMN tax_rules_json TEXT;
ALTER TABLE travel_booking ADD COLUMN tax_breakdown_json TEXT;
