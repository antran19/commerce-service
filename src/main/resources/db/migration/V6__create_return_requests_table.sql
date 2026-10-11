CREATE TABLE return_requests (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    resolved_by VARCHAR(255)
);

CREATE INDEX idx_return_requests_order_id ON return_requests (order_id);
