package com.divistay.payment;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapts the MockPay SDK to DiviStay's {@link PaymentGateway} interface (design pattern: Adapter).
 * Replacing the provider (e.g. with Stripe or Tranzila) means writing another adapter;
 * nothing in the booking logic changes.
 */
public class MockPayGatewayAdapter implements PaymentGateway {

    private final MockPayClient client;

    public MockPayGatewayAdapter(MockPayClient client) {
        this.client = client;
    }

    @Override
    public PaymentResult authorize(String idempotencyKey, long amount, CardDetails card, String description) {
        Map<String, String> request = new HashMap<>();
        request.put("amount_minor", Long.toString(amount));
        request.put("currency", "ILS");
        request.put("card_number", card.number());
        request.put("exp", card.expiryMonth() + "/" + card.expiryYear());
        request.put("cvc", card.cvc());
        request.put("capture", "false");
        request.put("description", description);
        request.put("idempotency_key", idempotencyKey);

        MockPayClient.Response response = client.charge(request);
        return switch (response.code()) {
            case "00" -> PaymentResult.approved(response.transactionId());
            case "51" -> PaymentResult.declined("The card doesn't have enough credit. Try another card.");
            default -> PaymentResult.declined("The card was declined. Try another card.");
        };
    }

    @Override
    public void capture(String reference) {
        expectOk(client.post(reference, "capture"), "capture");
    }

    @Override
    public void voidAuthorization(String reference) {
        expectOk(client.post(reference, "void"), "void");
    }

    private static void expectOk(MockPayClient.Response response, String action) {
        if (!"00".equals(response.code())) {
            throw new IllegalStateException("Payment " + action + " failed: " + response.message());
        }
    }
}
