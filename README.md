# commerce-service

Order service for Project Nexus: the cart, checkout, payment with Stripe, order life cycle, shipping, refunds and
returns, and the orders that follow a won auction. Extracted from the original monorepo into its own standalone
Spring Boot project, per the course's polyrepo requirement (each microservice is its own repo). Port `8085`,
database `commerce_db` (PostgreSQL, `localhost:5436`), registers with Eureka as `commerce-service`.

## What it does

An order is `AWAITING_PAYMENT`, then `PAID`, or `CANCELLED`, or `REFUNDED` after a payment.

- **Direct purchase.** The buyer fills a cart, then `POST /api/v1/checkout` turns it into an order. The order,
  the emptied cart, the idempotency record and the outbox events are saved in one transaction, so a crash cannot
  leave an order without its stock reservation. Send an `Idempotency-Key`: a retry within 24 hours returns the
  same order instead of creating a second one. An order nobody pays within 30 minutes is cancelled by
  `ExpireUnpaidOrdersJob` (every minute).
- **After an auction.** commerce-service listens to `AuctionWon` on `auction-events` and creates the winner's
  order itself, because the auction service must not create orders. The 30-minute expiry does not apply to these
  orders: the winner has 24 hours, handled by auction-service. `AuctionPaymentTimeout` cancels the order of a
  winner who did not pay.
- **Payment.** `POST /orders/{orderId}/checkout-session` creates a Stripe Checkout session, then the buyer comes
  back through `confirm-payment`, or Stripe calls the webhook if the buyer never returns. Both settle the order
  through the same code, so the order is paid once.
- **After payment.** The seller marks an order shipped. An admin or support person can refund it; a shipped and
  refunded order can then get a return request, which is received or rejected.

## API

All paths are under `/api/v1`. Every call needs a JWT with the privilege shown, except the Stripe webhook.

| Method | Path | Privilege | Purpose |
|---|---|---|---|
| GET | `/carts/me` | `CART.VIEW` | my cart |
| POST | `/carts/items` | `CART.CREATE` | add a product |
| PUT, DELETE | `/carts/items/{productId}` | `CART.UPDATE`, `CART.REMOVE_ITEM` | change the quantity, remove |
| POST | `/checkout` | `CHECKOUT.START` | turn the cart into an order |
| GET | `/orders/me` | `ORDER.LIST` | my orders |
| GET | `/orders/selling` | `ORDER.LIST` | orders containing my products |
| GET | `/orders` | `ORDER.LIST` | all orders (admin and support staff) |
| GET | `/orders/{orderId}` | `ORDER.VIEW` | one order |
| GET | `/orders/{orderId}/invoice` | `ORDER.VIEW` | the invoice |
| PUT | `/orders/{orderId}/shipping-address` | `CHECKOUT.START` | set the delivery address |
| POST | `/orders/{orderId}/checkout-session` | `CHECKOUT.START` | start the Stripe payment |
| POST | `/orders/{orderId}/confirm-payment` | `CHECKOUT.START` | the buyer's way back from Stripe |
| POST | `/orders/{orderId}/cancel` | `ORDER.CANCEL` | cancel an unpaid order |
| POST | `/orders/{orderId}/mark-shipped` | `ORDER.MARK_SHIPPED` | the seller says it was shipped |
| POST | `/orders/{orderId}/refund` | `ORDER.REFUND` | refund an order |
| POST | `/orders/{orderId}/returns` | `ORDER.REFUND` | open a return for a shipped, refunded order |
| POST | `/returns/{returnId}/receive`, `/reject` | `ORDER.REFUND` | finish a return |
| POST | `/payments/webhook` | none (Stripe signature) | Stripe tells us a payment succeeded |

## Events

Written to an outbox table and relayed to Kafka every 5 seconds: `OrderCreated`, `OrderPaid`, `OrderShipped`,
`OrderCancelled`, `OrderRefunded`, `OrderReturnReceived` and `InventoryReservationRequested`. Rows already sent
are deleted after 7 days. The service listens to `auction-events` (`AuctionWon`, `AuctionPaymentTimeout`),
`catalog-events` and `fulfillment-events`. From those it keeps local copies of product data and stock levels,
and cancels an order whose stock reservation failed.

## Configuration

| Setting | Default | Meaning |
|---|---|---|
| `STRIPE_SECRET_KEY` | empty | Stripe API key used to create payment sessions (a `sk_test_...` key locally) |
| `STRIPE_WEBHOOK_SECRET` | empty | `whsec_...` of the webhook; while empty the webhook rejects every call |
| `nexus.checkout.allowed-redirect-origins` | `http://localhost:5173,http://localhost:3000` | where Stripe may send the buyer back to |
| `nexus.order.unpaid-timeout-minutes` | 30 | when an unpaid direct order is cancelled |
| `nexus.cart.max-inactive-days` | 30 | when an untouched cart is removed |

Never put a Stripe key in the repository; pass it as an environment variable.

## Prerequisites

This project resolves `com.nexus:common-*` from
[antran19/common-libs](https://github.com/antran19/common-libs)'s GitHub Packages registry, declared in
`pom.xml`. Reading GitHub Packages needs authentication even for a public repo: add a Personal Access Token
(`read:packages`) to your own `~/.m2/settings.xml` under a server with id `github`. Without a token, clone
`common-libs` and run `mvn clean install` there; Maven checks the local `~/.m2` cache first.

## Build and test

```bash
mvn clean package
```

Runs the whole test suite, including Testcontainers-backed integration tests, so Docker must be running.

## Run

```bash
java -jar target/commerce-service-0.1.0-SNAPSHOT.jar
```

It expects PostgreSQL on `localhost:5436`, Kafka on `localhost:9092` and the Eureka discovery-server. The
`infra` repo has the docker-compose setup that runs the whole cluster. The `Dockerfile` copies a pre-built jar,
so run `mvn clean package` before `docker build`.
