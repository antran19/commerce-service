package com.nexus.commerce.infrastructure.config;

import com.nexus.commerce.application.port.out.CartRepositoryPort;
import com.nexus.commerce.application.port.out.CheckoutRequestRepositoryPort;
import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.PaymentGatewayPort;
import com.nexus.commerce.application.port.out.ProductSnapshotRepositoryPort;
import com.nexus.commerce.application.port.out.ProductStockRepositoryPort;
import com.nexus.commerce.application.port.out.ReturnRequestRepositoryPort;
import com.nexus.commerce.application.usecase.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public AddCartItemUseCase addCartItemUseCase(CartRepositoryPort cartRepositoryPort,
                                                 ProductSnapshotRepositoryPort productSnapshotRepositoryPort,
                                                 ProductStockRepositoryPort productStockRepositoryPort) {
        return new AddCartItemUseCase(cartRepositoryPort, productSnapshotRepositoryPort, productStockRepositoryPort);
    }

    @Bean
    public UpdateCartItemUseCase updateCartItemUseCase(CartRepositoryPort cartRepositoryPort,
                                                       ProductStockRepositoryPort productStockRepositoryPort) {
        return new UpdateCartItemUseCase(cartRepositoryPort, productStockRepositoryPort);
    }

    @Bean
    public RemoveCartItemUseCase removeCartItemUseCase(CartRepositoryPort cartRepositoryPort) {
        return new RemoveCartItemUseCase(cartRepositoryPort);
    }

    @Bean
    public GetCartUseCase getCartUseCase(CartRepositoryPort cartRepositoryPort) {
        return new GetCartUseCase(cartRepositoryPort);
    }

    @Bean
    public StartCheckoutUseCase startCheckoutUseCase(CartRepositoryPort cartRepositoryPort,
                                                       OrderRepositoryPort orderRepositoryPort,
                                                       EventPublisherPort eventPublisherPort,
                                                       ProductSnapshotRepositoryPort productSnapshotRepositoryPort,
                                                       ProductStockRepositoryPort productStockRepositoryPort,
                                                       CheckoutRequestRepositoryPort checkoutRequestRepositoryPort) {
        return new StartCheckoutUseCase(cartRepositoryPort, orderRepositoryPort, eventPublisherPort,
                productSnapshotRepositoryPort, productStockRepositoryPort, checkoutRequestRepositoryPort);
    }

    @Bean
    public CancelDefaultedAuctionOrderUseCase cancelDefaultedAuctionOrderUseCase(OrderRepositoryPort orderRepositoryPort,
                                                                                 EventPublisherPort eventPublisherPort) {
        return new CancelDefaultedAuctionOrderUseCase(orderRepositoryPort, eventPublisherPort);
    }

    @Bean
    public SyncProductStockUseCase syncProductStockUseCase(ProductStockRepositoryPort productStockRepositoryPort) {
        return new SyncProductStockUseCase(productStockRepositoryPort);
    }

    @Bean
    public SyncProductSnapshotUseCase syncProductSnapshotUseCase(ProductSnapshotRepositoryPort productSnapshotRepositoryPort) {
        return new SyncProductSnapshotUseCase(productSnapshotRepositoryPort);
    }

    @Bean
    public CreateOrderFromAuctionUseCase createOrderFromAuctionUseCase(OrderRepositoryPort orderRepositoryPort,
                                                                        EventPublisherPort eventPublisherPort) {
        return new CreateOrderFromAuctionUseCase(orderRepositoryPort, eventPublisherPort);
    }

    @Bean
    public CreateCheckoutSessionUseCase createCheckoutSessionUseCase(OrderRepositoryPort orderRepositoryPort,
                                                                       PaymentGatewayPort paymentGatewayPort,
                                                                       RedirectUrlPolicy redirectUrlPolicy) {
        return new CreateCheckoutSessionUseCase(orderRepositoryPort, paymentGatewayPort, redirectUrlPolicy);
    }

    // Comma-separated origins of the front end (scheme://host[:port]); the default is the Vite dev server.
    @Bean
    public RedirectUrlPolicy redirectUrlPolicy(
            @Value("${nexus.checkout.allowed-redirect-origins:http://localhost:5173,http://localhost:3000}") String origins) {
        return new RedirectUrlPolicy(java.util.Arrays.asList(origins.split(",")));
    }

    @Bean
    public PaidOrderSettler paidOrderSettler(OrderRepositoryPort orderRepositoryPort,
                                             PaymentGatewayPort paymentGatewayPort,
                                             EventPublisherPort eventPublisherPort) {
        return new PaidOrderSettler(orderRepositoryPort, paymentGatewayPort, eventPublisherPort);
    }

    @Bean
    public ConfirmPaymentUseCase confirmPaymentUseCase(OrderRepositoryPort orderRepositoryPort,
                                                         PaymentGatewayPort paymentGatewayPort,
                                                         PaidOrderSettler paidOrderSettler) {
        return new ConfirmPaymentUseCase(orderRepositoryPort, paymentGatewayPort, paidOrderSettler);
    }

    @Bean
    public HandlePaymentWebhookUseCase handlePaymentWebhookUseCase(OrderRepositoryPort orderRepositoryPort,
                                                                   PaymentGatewayPort paymentGatewayPort,
                                                                   PaidOrderSettler paidOrderSettler) {
        return new HandlePaymentWebhookUseCase(orderRepositoryPort, paymentGatewayPort, paidOrderSettler);
    }

    @Bean
    public RefundOrderUseCase refundOrderUseCase(OrderRepositoryPort orderRepositoryPort,
                                                   PaymentGatewayPort paymentGatewayPort,
                                                   EventPublisherPort eventPublisherPort) {
        return new RefundOrderUseCase(orderRepositoryPort, paymentGatewayPort, eventPublisherPort);
    }

    @Bean
    public GetOrderUseCase getOrderUseCase(OrderRepositoryPort orderRepositoryPort) {
        return new GetOrderUseCase(orderRepositoryPort);
    }

    @Bean
    public GetOrderInvoiceUseCase getOrderInvoiceUseCase(OrderRepositoryPort orderRepositoryPort) {
        return new GetOrderInvoiceUseCase(orderRepositoryPort);
    }

    @Bean
    public SetShippingAddressUseCase setShippingAddressUseCase(OrderRepositoryPort orderRepositoryPort) {
        return new SetShippingAddressUseCase(orderRepositoryPort);
    }

    @Bean
    public ListOrdersUseCase listOrdersUseCase(OrderRepositoryPort orderRepositoryPort) {
        return new ListOrdersUseCase(orderRepositoryPort);
    }

    @Bean
    public ListAllOrdersUseCase listAllOrdersUseCase(OrderRepositoryPort orderRepositoryPort) {
        return new ListAllOrdersUseCase(orderRepositoryPort);
    }

    @Bean
    public ListOrdersSellingUseCase listOrdersSellingUseCase(OrderRepositoryPort orderRepositoryPort) {
        return new ListOrdersSellingUseCase(orderRepositoryPort);
    }

    @Bean
    public MarkOrderShippedUseCase markOrderShippedUseCase(OrderRepositoryPort orderRepositoryPort,
                                                           EventPublisherPort eventPublisherPort) {
        return new MarkOrderShippedUseCase(orderRepositoryPort, eventPublisherPort);
    }

    @Bean
    public CancelOrderUseCase cancelOrderUseCase(OrderRepositoryPort orderRepositoryPort,
                                                  EventPublisherPort eventPublisherPort) {
        return new CancelOrderUseCase(orderRepositoryPort, eventPublisherPort);
    }

    @Bean
    public CancelOrderDueToStockFailureUseCase cancelOrderDueToStockFailureUseCase(
            OrderRepositoryPort orderRepositoryPort, EventPublisherPort eventPublisherPort) {
        return new CancelOrderDueToStockFailureUseCase(orderRepositoryPort, eventPublisherPort);
    }

    @Bean
    public CreateReturnRequestUseCase createReturnRequestUseCase(OrderRepositoryPort orderRepositoryPort,
                                                                    ReturnRequestRepositoryPort returnRequestRepositoryPort) {
        return new CreateReturnRequestUseCase(orderRepositoryPort, returnRequestRepositoryPort);
    }

    @Bean
    public ReceiveReturnUseCase receiveReturnUseCase(ReturnRequestRepositoryPort returnRequestRepositoryPort,
                                                        OrderRepositoryPort orderRepositoryPort,
                                                        EventPublisherPort eventPublisherPort) {
        return new ReceiveReturnUseCase(returnRequestRepositoryPort, orderRepositoryPort, eventPublisherPort);
    }

    @Bean
    public RejectReturnUseCase rejectReturnUseCase(ReturnRequestRepositoryPort returnRequestRepositoryPort) {
        return new RejectReturnUseCase(returnRequestRepositoryPort);
    }

    @Bean
    public ExpireUnpaidOrdersUseCase expireUnpaidOrdersUseCase(
            OrderRepositoryPort orderRepositoryPort, EventPublisherPort eventPublisherPort,
            @Value("${nexus.order.unpaid-timeout-minutes:30}") long timeoutMinutes) {
        return new ExpireUnpaidOrdersUseCase(orderRepositoryPort, eventPublisherPort,
                java.time.Duration.ofMinutes(timeoutMinutes));
    }

    @Bean
    public ExpireInactiveCartsUseCase expireInactiveCartsUseCase(
            CartRepositoryPort cartRepositoryPort,
            @Value("${nexus.cart.max-inactive-days:30}") long maxInactiveDays) {
        return new ExpireInactiveCartsUseCase(cartRepositoryPort, java.time.Duration.ofDays(maxInactiveDays));
    }

    @Bean
    public PurgeCheckoutRequestsUseCase purgeCheckoutRequestsUseCase(
            CheckoutRequestRepositoryPort checkoutRequestRepositoryPort,
            @Value("${nexus.checkout.idempotency-retention-hours:24}") long retentionHours) {
        return new PurgeCheckoutRequestsUseCase(checkoutRequestRepositoryPort,
                java.time.Duration.ofHours(retentionHours));
    }
}
