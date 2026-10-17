-- commerce-service's copy of how many units of each product can still be sold, kept up to date
-- from fulfillment's InventoryLevelChanged events. A separate table from product_snapshots: the
-- stock events come on another topic and may arrive before the product's ProductCreated.
-- as_of is the event's occurredAt; an older event never overwrites a newer level.
CREATE TABLE product_stock_levels (
    product_id VARCHAR(255) PRIMARY KEY,
    available_quantity INTEGER NOT NULL,
    as_of TIMESTAMPTZ NOT NULL
);
