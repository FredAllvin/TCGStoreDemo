# TCG Shop Platform

Reusable webshop for trading card game stores (Pokémon, Magic, Yu-Gi-Oh!, One Piece…).
One codebase — each store gets its own deployment with its own branding, products and
payment account. Built with Spring Boot 4 / Java 21, Thymeleaf, PostgreSQL.

## Features

**Storefront** (Swedish + English, SEK/EUR-ready)
- Categories per game with Singles/Sealed subcategories, search, in-stock filter
- Card singles sold the way shops actually sell them:
  - raw condition variants (NM/LP/MP/HP/DMG, foil/reverse) with per-condition price & stock
  - graded slabs (PSA/BGS/CGC + grade + cert number), quantity 1
- Session cart, guest checkout, free-shipping threshold, order confirmation with status

**Payments**
- `mock` provider (demo pay page) or **Stripe Checkout** (test/live) behind one interface
- Webhook with signature verification + idempotency; unpaid orders auto-release stock after 1 h

**Admin panel** (`/admin`)
- Dashboard: orders to ship, 30-day revenue, low stock
- Products with variant editor and validated image uploads, categories, order workflow
  (paid → shipped, cancel + restock), store settings (name, logo, colors, currency,
  shipping options), change password

**Security**: BCrypt, CSRF, server-side price authority, atomic stock reservation,
upload re-encoding, login rate limiting, secrets via env vars.

## Development

```bash
docker compose up -d          # local PostgreSQL
./mvnw spring-boot:run        # with SPRING_PROFILES_ACTIVE=dev
```

- Store: http://localhost:8080 (seeded with placeholder data)
- Admin: http://localhost:8080/admin — `admin` / `admin` (dev profile only)
- Tests: `./mvnw test` (integration tests start their own Postgres via Testcontainers — Docker must be running)

Profiles: `dev` (local, seeded), `demo` (adds demo ribbon + nightly reset, stack on top
of prod for the public demo), `prod` (env-driven config).

## Deployment

See [DEPLOY.md](DEPLOY.md) — VPS + Docker Compose + Caddy (automatic HTTPS).

## Project layout

```
src/main/java/com/tcgstore/shop/
  config/     security, MVC, properties
  domain/     JPA entities (Product → ProductVariant kind=STANDARD|RAW|GRADED)
  repo/       Spring Data repositories
  service/    cart, checkout (stock reservation), orders, images, settings
  service/payment/  PaymentProvider: mock + Stripe Checkout
  web/        storefront controllers + Stripe webhook
  web/admin/  admin panel controllers
  demo/       seeder, placeholder images, nightly demo reset
src/main/resources/
  db/migration/   Flyway schema
  templates/      Thymeleaf (shop/, admin/, layout/, error/)
  messages*.properties  sv (default) + en
```

Prices are stored as `long` minor units (öre/cents) — never floating point.
