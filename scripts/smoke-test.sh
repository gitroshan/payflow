#!/usr/bin/env bash
# End-to-end smoke test for the PayFlow platform.
# Requires all five services running and the infra (Kafka/Postgres/Redis) up.
# Needs: bash, curl, jq
set -euo pipefail

ORCH=http://localhost:8081
LEDGER=http://localhost:8082
REFUND=http://localhost:8083
PAYOUT=http://localhost:8084

MERCHANT="merchant_acme"

echo "==> 1. Create + capture a payment (idempotent on Idempotency-Key)"
RESP=$(curl -s -X POST "$ORCH/api/v1/payments" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: order-1001' \
  -d "{\"merchantId\":\"$MERCHANT\",\"amount\":49.99,\"currency\":\"USD\",\"paymentMethodToken\":\"tok_visa\",\"captureImmediately\":true}")
echo "$RESP" | jq
PAYMENT_ID=$(echo "$RESP" | jq -r .id)

echo "==> 2. Replay the SAME request (must return the same payment, no double charge)"
curl -s -X POST "$ORCH/api/v1/payments" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: order-1001' \
  -d "{\"merchantId\":\"$MERCHANT\",\"amount\":49.99,\"currency\":\"USD\",\"paymentMethodToken\":\"tok_visa\",\"captureImmediately\":true}" \
  | jq '{id, status}'

echo "==> 3. Wait for events to propagate to the ledger"
sleep 3

echo "==> 4. Ledger accounts for the merchant (expect a MERCHANT_PAYABLE credit balance)"
curl -s "$LEDGER/api/v1/ledger/accounts/$MERCHANT" | jq

echo "==> 5. Ledger journal entries for this payment"
curl -s "$LEDGER/api/v1/ledger/entries/$PAYMENT_ID" | jq

echo "==> 6. Partial refund of \$10.00"
curl -s -X POST "$REFUND/api/v1/refunds" \
  -H 'Content-Type: application/json' \
  -d "{\"paymentId\":\"$PAYMENT_ID\",\"amount\":10.00,\"reason\":\"customer_request\"}" | jq

echo "==> 7. Open a dispute"
curl -s -X POST "$REFUND/api/v1/disputes" \
  -H 'Content-Type: application/json' \
  -d "{\"paymentId\":\"$PAYMENT_ID\",\"reasonCode\":\"fraudulent\"}" | jq '{id,status,reasonCode}'

echo "==> 8. Demonstrate a decline (amount ending in .13 -> do_not_honor)"
curl -s -X POST "$ORCH/api/v1/payments" \
  -H 'Content-Type: application/json' \
  -d "{\"merchantId\":\"$MERCHANT\",\"amount\":10.13,\"currency\":\"USD\",\"paymentMethodToken\":\"tok_visa\",\"captureImmediately\":true}" \
  | jq '{status, failureReason}'

echo "==> 9. Pay the merchant out"
curl -s -X POST "$PAYOUT/api/v1/payouts" \
  -H 'Content-Type: application/json' \
  -d "{\"merchantId\":\"$MERCHANT\",\"amount\":39.99,\"currency\":\"USD\"}" | jq

echo "==> 10. Run a reconciliation batch (one matching line, one bogus line)"
curl -s -X POST "$PAYOUT/api/v1/reconciliation/run" \
  -H 'Content-Type: application/json' \
  -d "[{\"paymentId\":\"$PAYMENT_ID\",\"amount\":49.99,\"currency\":\"USD\"},{\"paymentId\":\"unknown_pay_x\",\"amount\":5.00,\"currency\":\"USD\"}]" \
  | jq '{batchId, matched, amountMismatch, missingInLedger, missingInProvider}'

echo "==> Smoke test complete."
