-- orders.status was a free VARCHAR(20): only the Java enum OrderStatus limited it, so a typo or a bad
-- manual UPDATE could store a status no code understands ('PAIDD') and the order would be skipped by
-- every query and job that looks for a real one. The database now refuses any other value.
-- When a value is added to OrderStatus, a new migration must widen this list: OrderRepositoryAdapterTest (everyOrderStatusOfTheEnum_...)
-- fails until it does.
ALTER TABLE orders
    ADD CONSTRAINT chk_orders_status
    CHECK (status IN ('AWAITING_PAYMENT', 'PAID', 'CANCELLED', 'REFUNDED'));
