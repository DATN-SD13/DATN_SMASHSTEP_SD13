# Customer storefront

## Routes

Customer application remains in `CUSTOMER_FRONTEND`, port 5174. Admin remains on 5173.

- `/`, `/san-pham`, `/san-pham/:id`: public catalogue and live campaign pricing.
- `/gio-hang`: guest/customer cart. Persistent cart contains only variant IDs and quantities.
- `/thanh-toan`: authenticated customer checkout.
- `/don-hang`, `/don-hang/:ma`: authenticated customer's own invoices.
- `/goi-y-giay`: six steps mapping directly to actual catalogue filters.
- `/tai-khoan`: existing profile, address book and password flow.

## Shipping configuration

GHN fee lookup is server-side. Set these environment variables on the process launching BACKEND:

| Variable | Value |
| --- | --- |
| `GHN_TOKEN` | Token issued to the store by GHN. Keep outside Git. |
| `GHN_SHOP_ID` | Shop belonging to that token; its registered pickup address is used. |
| `GHN_GRAMS_PER_PAIR` | Actual packed weight per pair, in grams. Uses the same setting as POS. |
| `GHN_BASE_URL` | Carrier environment URL, shared with POS. Default is the production GHN gateway. |

Restart the backend after configuring its environment. No database migration is required.
Customer checkout now reuses the team's `GhnPhiVanChuyenService` two-level province/ward catalogue
and shipping preview. POS keeps its employee-scoped quote cache; customers do not call POS controllers
or establish a POS identity. Prices are calculated again on order submission.

Choose the province and ward from GHN's current two-level selectors; saved legacy addresses must
be reselected. Packages above the carrier's 50kg limit must be split. Fee lookup is a preview:
it does not create a shipment, buy a label or arrange pickup. There is no invented free-shipping fallback.
Recipient name, phone and delivery address are sent to GHN for the preview; the form states this.
Carrier tokens stay on the backend. Configure GHN_TOKEN outside Git; no token is hardcoded in source.

## Order lifecycle

`/api/customer/shop/**` requires CUSTOMER role, KHACH_HANG JWT identity and an active customer.
The server takes customer ID from the authenticated subject. It never trusts a request's customer ID.
Existing `/api/ban-hang/**` restrictions remain unchanged.

Checkout recalculates current prices, stock, voucher and shipping in the backend. Only the highest
valid campaign percentage applies to each variant. Product badges have a separate maximum across
eligible variants; product representative price and its crossed-out original remain paired.

The existing invoice creation lock serializes idempotency checks and invoice insertion. Variant
rows are locked, legacy POS reservations respected, and stock deducted once. Client retries reuse
the same request ID and payload, including after reload. Request fingerprints live in existing
invoice history, with no new table/column. No payment is recorded when placing an unpaid COD order.

Only the customer's own new web orders at status 0 can have shipping edited or be canceled.
Shipping changes recalculate shipping only; sold unit prices and voucher amounts remain historical.
Cancellation restores stock and consumed voucher usage once. Admin cancellation uses the same
web-order inventory helper. Admin completion of a web COD invoice records its collected COD amount;
staff must only mark complete after actual collection. Existing legacy/POS invoices do not enter
this web-order-specific collection path.

All active payment methods are read from existing data. Only active COD supports this online order
flow; bank/card methods are shown as unavailable until a genuine payment gateway is integrated.
No simulated payment success or transfer confirmation is accepted from the customer.

Notifications derive from own recent invoices and usable vouchers. Read markers are local browser
state. No review, cart, notification, recommendation or shipment table was added.

## Checks

- `BACKEND`: JDK 17, `mvnw.cmd compile`.
- Run integration fixtures only against a disposable existing database whose name contains `Audit`.
  New commerce fixtures assert that guard and are transactionally rolled back. Carrier responses are
  mocked only inside tests. Do not run them against the live SmashStep database.
- `CUSTOMER_FRONTEND`: `npm run build`, `node --test scripts/commerce.test.mjs`.
- `FRONTEND`: `npm run build`.
- `git diff -- Database` must stay empty. No migrations, seeds or runtime schema updates are needed.
