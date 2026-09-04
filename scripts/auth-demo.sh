#!/usr/bin/env bash
# Demonstrates the authenticated path through the API gateway (port 8080).
# Requires the api-gateway + orchestrator + ledger running, plus infra up.
# Needs: bash, curl, jq
set -euo pipefail

GW=http://localhost:8080

echo "==> 0. A request with NO token is rejected (expect 401)"
curl -s -o /dev/null -w "   status=%{http_code}\n" -X POST "$GW/api/v1/payments" \
  -H 'Content-Type: application/json' \
  -d '{"merchantId":"merchant_acme","amount":1.00,"currency":"USD","paymentMethodToken":"tok_visa","captureImmediately":true}'

echo "==> 1. Exchange client credentials for a JWT"
TOKEN=$(curl -s -X POST "$GW/auth/token" \
  -H 'Content-Type: application/json' \
  -d '{"clientId":"merchant_acme_client","clientSecret":"s3cr3t"}' | jq -r .accessToken)
echo "   token: ${TOKEN:0:24}..."

echo "==> 2. Create + capture a payment THROUGH the gateway"
RESP=$(curl -s -X POST "$GW/api/v1/payments" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: gw-order-2001' \
  -d '{"merchantId":"merchant_acme","amount":49.99,"currency":"USD","paymentMethodToken":"tok_visa","captureImmediately":true}')
echo "$RESP" | jq '{id, status, capturedAmount}'
PAYMENT_ID=$(echo "$RESP" | jq -r .id)

echo "==> 3. Read ledger balances through the gateway"
sleep 3
curl -s -H "Authorization: Bearer $TOKEN" "$GW/api/v1/ledger/accounts/merchant_acme" | jq

echo "==> Auth demo complete (payment $PAYMENT_ID)."
