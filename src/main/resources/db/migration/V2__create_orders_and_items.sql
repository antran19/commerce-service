CREATE TABLE orders (
    id UUID PRIMARY KEY,
    buyer_id VARCHAR(255) NOT NULL,
    auction_id VARCHAR(255),
    total_amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    paid_at TIMESTAMPTZ
);

-- An auction can settle into at most one order -- this is the DB-level guard against a
-- redelivered AuctionWonEvent (Kafka is at-least-once) producing a second order.
CREATE UNIQUE INDEX idx_orders_one_per_auction ON orders (auction_id) WHERE auction_id IS NOT NULL;
CREATE INDEX idx_orders_buyer_id ON orders (buyer_id);

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id VARCHAR(255) NOT NULL,
    seller_id VARCHAR(255) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,
    quantity INTEGER NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
