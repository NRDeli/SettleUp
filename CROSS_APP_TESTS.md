# Cross‑App Communication Test Scenarios

This document outlines a set of **end‑to‑end test cases** to verify that the three SettleUp microservices work together coherently. Each scenario chains calls across services via the Swagger UI to exercise cross‑service validation, event‑driven updates, and state consistency. Follow the steps in order and confirm that the expected behaviour occurs at each stage.

> **Note:** The Swagger UIs are available at the following URLs when the services are running:
>
> - **Membership Service:** `http://localhost:8081/swagger-ui/index.html`
> - **Expense Service:** `http://localhost:8082/swagger-ui/index.html`
> - **Settlement Service:** `http://localhost:8083/swagger-ui/index.html`

## Scenario 1 – Happy‑Path Expense and Settlement

1. **Create a group (membership service)**

   - Navigate to the membership service UI and call **`POST /groups`** with
     ```json
     { "name": "Trip to Vegas", "baseCurrency": "USD" }
     ```
   - _Expect:_ The response returns a group object with an `id` (e.g., `1`).

2. **Add two members (membership service)**

   - For each member, call **`POST /groups/{groupId}/members`** using the returned group ID (`1`):
     ```json
     { "email": "alice@example.com", "role": "MEMBER" }
     ```
     and
     ```json
     { "email": "bob@example.com", "role": "MEMBER" }
     ```
   - _Expect:_ Each call returns a member object with its own `id` (e.g., Alice is `1` and Bob is `2`).

3. **Record an expense (expense service)**

   - In the expense service UI, call **`POST /expenses`** with the following payload:
     ```json
     {
       "groupId": 1,
       "payerMemberId": 1,
       "currency": "USD",
       "totalAmount": 100,
       "splits": [
         { "memberId": 1, "shareAmount": 60 },
         { "memberId": 2, "shareAmount": 40 }
       ]
     }
     ```
   - _Expect:_ The service validates that group 1 and both members exist; it returns the created expense with an `id`.  An `expense.recorded` event is published to RabbitMQ for the settlement service.

4. **Compute the settlement plan (settlement service)**

   - Go to the settlement service UI and call **`POST /settlements/compute`** with
     ```json
     { "groupId": 1, "baseCurrency": "USD" }
     ```
   - _Expect:_ The settlement service processes the event from step 3 and calculates that Bob owes Alice \$40.  The response includes a transfer plan such as
     ```json
     {
       "transfers": [
         { "fromMemberId": 2, "toMemberId": 1, "amount": 40 }
       ]
     }
     ```

5. **Record a transfer (settlement service)**

   - Still in the settlement service, call **`POST /transfers`** with
     ```json
     { "groupId": 1, "fromMemberId": 2, "toMemberId": 1, "amount": 40, "note": "Paid via Venmo" }
     ```
   - _Expect:_ The transfer is created and net balances are updated. The response includes a confirmation message.

6. **Recompute the settlement plan (settlement service)**

   - Call **`POST /settlements/compute`** again with the same input as step 4.
   - _Expect:_ No transfers are required because the recorded payment settled the debt; the response returns an empty `transfers` array.

## Scenario 2 – Invalid Expense

1. **Attempt to record an expense for a missing group (expense service)**

   - Call **`POST /expenses`** with a `groupId` that does not exist (e.g., `999`).
   - _Expect:_ The expense service calls the membership service to verify the group and returns a **404** or **400** response with the message “Group not found”.

2. **Attempt to record an expense for a valid group but missing member (expense service)**

   - Use `groupId: 1` but set `payerMemberId: 99` or include a split with `memberId: 99`.
   - _Expect:_ The service responds with an error such as “Payer member does not exist or is not part of the group”. No event is published.

3. **Attempt to record an expense with mismatched splits (expense service)**

   - Provide a `totalAmount` that does not equal the sum of `splits` (e.g., total `100` but splits sum to `110`).
   - _Expect:_ The service returns **400** with the message “Sum of splits must equal total amount”.

## Scenario 3 – Transfer Validation

1. **Attempt to record a transfer with a missing group (settlement service)**

   - Call **`POST /transfers`** using a non‑existent `groupId` (e.g., `999`).
   - _Expect:_ The service returns **404** with “Group not found” and does not record the transfer.

2. **Attempt to record a transfer with invalid members (settlement service)**

   - Use a valid group but set either `fromMemberId` or `toMemberId` to a user not in the group (e.g., `memberId: 99`).
   - _Expect:_ The service returns **404** with “From or To member does not exist or is not part of the group”.

## Scenario 4 – Updating and Deleting Across Services

1. **Update an expense and recompute settlement**

   - Modify the expense from Scenario 1 using **`PUT /expenses/{id}`** – change the `totalAmount` to `120` and update splits accordingly.
   - _Expect:_ The expense service validates the changes, updates the record, and publishes a new event.  Recompute the settlement plan to see the new debt allocation.

2. **Delete a member and test subsequent operations**

   - Delete Bob via **`DELETE /groups/1/members/2`**.
   - _Expect:_ The membership service confirms deletion and returns “Member deleted successfully”.  If you now attempt to record an expense or transfer involving Bob in the other services, you should receive a “member not found” error.

3. **Delete an expense and recompute settlement**

   - Use **`DELETE /expenses/{id}`** to remove the expense recorded in Scenario 1.
   - _Expect:_ The expense service confirms the deletion.  Recompute the settlement plan; net balances return to zero because no expenses exist.

## General Tips

- Use the Swagger “Try it out” feature to build the JSON payloads interactively.  Each endpoint description in the UI includes a one‑line summary of its purpose.
- After every operation that modifies data (create, update, delete), you can verify state in the other services by calling relevant “get” endpoints (e.g., list expenses, list transfers, list members).
- Error responses are designed to be informative; reading the message will tell you why a request was rejected.