
# SettleUp UI

A React + Vite + Tailwind front-end for your Assignment‑2 microservices.

## 0) Prereqs

- Backend services running locally on ports:
  - membership-service: 8081
  - expense-service: 8082
  - settlement-service: 8083
- Or run everything in Docker Compose (see Section 3).

## 1) Dev workflow (fastest feedback)

```bash
cd settleup-ui
npm install
npm run dev
```

Open http://localhost:5173 and use the nav. In dev, a Vite proxy maps:
- `/api/membership` → `http://localhost:8081`
- `/api/expense` → `http://localhost:8082`
- `/api/settlement` → `http://localhost:8083`

## 2) Smoke test sequence (step-wise)

1. **Status page:** visit **Status**. All services should show `OK` (it pings `/actuator/health` and `/v3/api-docs`).
2. **Groups:** create a group (name + baseCurrency). Confirm it appears in the list.
3. **Members:** pick that group, add 2–3 members (email + role). Confirm they list.
4. **Categories:** add a couple categories to the same group.
5. **Expenses:** select the same group, pick a payer, enter currency + total, enter split amounts for each member, then **Save**. Confirm it appears in the list (data from `/groups/{id}/expenses`).
6. **Settlement:** select the same group and click **Compute plan**. Transfers should appear based on your backend's calculation.

Proceed only when each step works.

## 3) Docker (as fourth service)

Add this UI to your existing root `docker-compose.yml` (service names must match).

```yaml
  ui:
    build:
      context: ./settleup-ui
    image: settleup-ui:latest
    container_name: settleup-ui
    ports:
      - "5173:80"   # UI available on http://localhost:5173
    depends_on:
      - membership-service
      - expense-service
      - settlement-service
```

> The UI container proxies `/api/*` to the service names:
> - `membership-service:8081`
> - `expense-service:8082`
> - `settlement-service:8083`

If your service names differ, update `nginx.conf` accordingly.

## 4) Production build (without Docker)

```bash
npm install
npm run build
npx serve -s dist  # or any static file server
```

But to avoid CORS on production, prefer the Dockerized Nginx proxy.

## 5) Config

All API calls are routed via path prefixes in `src/config.ts`. If you need to change routing strategy, update:
- `src/config.ts`
- `vite.config.ts` (dev proxy)
- `nginx.conf` (prod proxy)

## 6) Notes

- This UI uses the exact endpoints found in your codebase:
  - **Membership:** `/groups`, `/groups/{groupId}/members`, `/groups/{groupId}/categories`
  - **Expense:** `/expenses`, `/expenses/{id}`, `/groups/{groupId}/expenses`
  - **Settlement:** `POST /settlements/compute` (body: `{ groupId, baseCurrency }`), plus transfers views
- If CORS is enabled/locked down on the Spring apps, the dev proxy and Nginx reverse proxy sidestep it.
- Background image is from Unsplash; replace as desired.
