-- commerce-service's own copy of what it needs from the catalog to price a cart item, kept up
-- to date from catalog-events. Prices and seller ids are never taken from the client anymore.
CREATE TABLE product_snapshots (
    product_id VARCHAR(255) PRIMARY KEY,
    seller_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
