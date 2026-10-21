-- Remembers which order a checkout request (identified by the client's Idempotency-Key header)
-- produced, so a retried request gets the SAME order back instead of a second one or a confusing
-- CART_EMPTY. The unique constraint is the database-level guard behind that guarantee.
-- Rows are only useful for a short while; PurgeCheckoutRequestsJob deletes the old ones.
CREATE TABLE checkout_requests (
    id UUID PRIMARY KEY,
    buyer_id VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_checkout_requests_buyer_key UNIQUE (buyer_id, idempotency_key)
);

CREATE INDEX idx_checkout_requests_created_at ON checkout_requests (created_at);
