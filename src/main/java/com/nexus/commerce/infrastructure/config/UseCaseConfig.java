package com.nexus.commerce.infrastructure.config;

import com.nexus.commerce.application.port.out.CartRepositoryPort;
import com.nexus.commerce.application.port.out.EventPublisherPort;
import com.nexus.commerce.application.port.out.OrderRepositoryPort;
import com.nexus.commerce.application.port.out.PaymentGatewayPort;
import com.nexus.commerce.application.usecase.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public AddCartItemUseCase addCartItemUseCase(CartRepositoryPort cartRepositoryPort) {
        return new AddCartItemUseCase(cartRepositoryPort);
    }

    @Bean
    public UpdateCartItemUseCase updateCartItemUseCase(CartRepositoryPort cartRepositoryPort) {
        return new UpdateCartItemUseCase(cartRepositoryPort);
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
                                                       EventPublisherPort eventPublisherPort) {
        return new StartCheckoutUseCase(cartRepositoryPort, orderRepositoryPort, eventPublisherPort);
    }

    @Bean
    public CreateOrderFromAuctionUseCase createOrderFromAuctionUseCase(OrderRepositoryPort orderRepositoryPort,
                                                                        EventPublisherPort eventPublisherPort) {
        return new CreateOrderFromAuctionUseCase(orderRepositoryPort, eventPublisherPort);
    }

    @Bean
    public CreateCheckoutSessionUseCase createCheckoutSessionUseCase(OrderRepositoryPort orderRepositoryPort,
                                                                       PaymentGatewayPort paymentGatewayPort) {
        return new CreateCheckoutSessionUseCase(orderRepositoryPort, paymentGatewayPort);
    }

    @Bean
    public ConfirmPaymentUseCase confirmPaymentUseCase(OrderRepositoryPort orderRepositoryPort,
                                                         PaymentGatewayPort paymentGatewayPort,
                                                         EventPublisherPort eventPublisherPort) {
        return new ConfirmPaymentUseCase(orderRepositoryPort, paymentGatewayPort, eventPublisherPort);
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
    public ListOrdersUseCase listOrdersUseCase(OrderRepositoryPort orderRepositoryPort) {
        return new ListOrdersUseCase(orderRepositoryPort);
    }

    @Bean
    public ListAllOrdersUseCase listAllOrdersUseCase(OrderRepositoryPort orderRepositoryPort) {
        return new ListAllOrdersUseCase(orderRepositoryPort);
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
}
