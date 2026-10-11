-- The Stripe PaymentIntent id behind a paid order's charge -- null until paid, needed to
-- refund the exact charge later (see RefundOrderUseCase). orders.status itself is a plain
-- VARCHAR with no CHECK constraint, so adding the REFUNDED enum value needs no migration.
ALTER TABLE orders ADD COLUMN payment_reference VARCHAR(255);
