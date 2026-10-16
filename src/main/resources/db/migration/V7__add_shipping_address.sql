-- Shipping address for an order, filled in by the buyer before paying. Nullable: orders that
-- predate this migration have none, and auction orders are created without one (the winner
-- supplies it while the order is still awaiting payment).
ALTER TABLE orders
    ADD COLUMN ship_recipient_name VARCHAR(255),
    ADD COLUMN ship_phone VARCHAR(30),
    ADD COLUMN ship_address_line VARCHAR(500),
    ADD COLUMN ship_city VARCHAR(100);
