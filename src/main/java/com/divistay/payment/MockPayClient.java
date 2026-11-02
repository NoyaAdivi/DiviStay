package com.divistay.payment;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A stand-in for a third-party payment provider's SDK ("MockPay"), with its own API style:
 * string maps in, numeric response codes out. DiviStay never calls it directly; see
 * {@link MockPayGatewayAdapter}. No real money is involved.
 *
 * <p>Test cards (any future expiry, any CVC):
 * <ul>
 *   <li>4242 4242 4242 4242 – approved</li>
 *   <li>4000 0000 0000 0002 – declined</li>
 *   <li>4000 0000 0000 9995 – insufficient funds</li>
 * </ul>
 */
public class MockPayClient {

    public record Response(String code, String transactionId, String message) {
    }

    private record Txn(String id, long amountMinor, String state) {
    }

    private final Map<String, Txn> transactions = new ConcurrentHashMap<>();
    private final Map<String, Response> byIdempotencyKey = new ConcurrentHashMap<>();

    /** POST /v1/charges with capture=false */
    public Response charge(Map<String, String> request) {
        String key = request.get("idempotency_key");
        if (key != null) {
            Response previous = byIdempotencyKey.get(key);
            if (previous != null) {
                return previous;
            }
        }
        String card = request.getOrDefault("card_number", "");
        Response response;
        if (card.endsWith("0002")) {
            response = new Response("05", null, "Do not honor");
        } else if (card.endsWith("9995")) {
            response = new Response("51", null, "Insufficient funds");
        } else {
            String id = "mp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            transactions.put(id, new Txn(id, Long.parseLong(request.get("amount_minor")), "authorized"));
            response = new Response("00", id, "Approved");
        }
        if (key != null) {
            Response raced = byIdempotencyKey.putIfAbsent(key, response);
            if (raced != null) {
                return raced;
            }
        }
        return response;
    }

    /** POST /v1/charges/{id}/capture or /void */
    public Response post(String transactionId, String action) {
        Txn txn = transactions.get(transactionId);
        if (txn == null) {
            return new Response("14", transactionId, "Unknown transaction");
        }
        String next = switch (action) {
            case "capture" -> txn.state().equals("authorized") ? "captured" : null;
            case "void" -> txn.state().equals("authorized") ? "voided" : null;
            default -> null;
        };
        if (next == null) {
            return new Response("12", transactionId, "Invalid transition from " + txn.state());
        }
        transactions.put(transactionId, new Txn(txn.id(), txn.amountMinor(), next));
        return new Response("00", transactionId, next);
    }

    /** For tests and the admin view. */
    public String stateOf(String transactionId) {
        Txn txn = transactions.get(transactionId);
        return txn == null ? null : txn.state();
    }
}
