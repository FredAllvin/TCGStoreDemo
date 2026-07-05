# Deploying a store

One deployment = one store. The public demo and every future client store run the
same code with different `.env` + branding (set in the admin panel).

## What you need

- A VPS with Docker installed (Hetzner CX22 ~€4/mo, DigitalOcean, etc. — Ubuntu 24.04 is fine)
- A domain (or subdomain) with an **A record** pointing at the VPS IP
- This repo pushed to a private GitHub repo (recommended) or copied to the server

Install Docker on a fresh Ubuntu VPS:

```bash
curl -fsSL https://get.docker.com | sh
```

## First deployment

```bash
git clone <your-repo-url> tcgshop && cd tcgshop
cp deploy/.env.example .env
nano .env        # set SITE_DOMAIN, passwords, profiles — see comments in the file
docker compose -f compose.prod.yaml up -d --build
```

That's it. Caddy fetches a TLS certificate automatically (takes ~30 s the first
time), Flyway creates the database schema, and with the `demo` profile the
placeholder catalog is seeded. Visit `https://<your-domain>`.

Admin panel: `https://<your-domain>/admin`
- demo profile: `demo` / `demo123` (reset every night at 04:00)
- client store: whatever you set in `.env`

## Updating

```bash
git pull
docker compose -f compose.prod.yaml up -d --build
```

Orders, uploads and settings survive updates (they live in Docker volumes).

## Enabling Stripe (test mode first)

1. Create a Stripe account, stay in **test mode**.
2. Dashboard → Developers → API keys → copy the `sk_test_…` secret key.
3. Dashboard → Developers → Webhooks → Add endpoint:
   - URL: `https://<your-domain>/webhooks/stripe`
   - Events: `checkout.session.completed`, `checkout.session.async_payment_succeeded`,
     `checkout.session.async_payment_failed`, `checkout.session.expired`
   - Copy the signing secret (`whsec_…`).
4. In `.env`: `PAYMENTS_PROVIDER=stripe`, `STRIPE_SECRET_KEY=sk_test_…`,
   `STRIPE_WEBHOOK_SECRET=whsec_…`, then `docker compose -f compose.prod.yaml up -d`.
5. Pay with test card `4242 4242 4242 4242` (any future date/CVC). The order flips
   to PAID when the webhook arrives.

Local webhook testing without a server:

```bash
stripe listen --forward-to localhost:8080/webhooks/stripe
# it prints a whsec_… — put it in STRIPE_WEBHOOK_SECRET for the dev run
```

Going live for a real client later = swapping to live keys (`sk_live_…`) and a live
webhook endpoint after the client's Stripe account is verified. Stripe can also
enable Klarna and Swish as payment methods in Checkout — same integration.

## Backups (do this for real client stores)

Nightly database dump via cron on the host (`crontab -e`):

```cron
15 3 * * * cd /root/tcgshop && docker compose -f compose.prod.yaml exec -T db pg_dump -U tcgshop tcgshop | gzip > /root/backups/tcgshop-$(date +\%F).sql.gz
```

Keep copies off the server too (rsync/rclone to your machine or object storage).
Uploaded images live in the `uploads` volume:

```bash
docker run --rm -v tcgshop_uploads:/data -v /root/backups:/out alpine tar czf /out/uploads-$(date +%F).tar.gz -C /data .
```

Restore a dump:

```bash
gunzip -c backup.sql.gz | docker compose -f compose.prod.yaml exec -T db psql -U tcgshop tcgshop
```

## Per-client checklist

1. New VPS (or same VPS, different domain + compose project name) + DNS A record
2. `cp deploy/.env.example .env` → client domain, strong passwords, `SPRING_PROFILES=prod`
3. `docker compose -f compose.prod.yaml up -d --build`
4. Log in to `/admin` → Settings: store name, logo, colors, contact, shipping options
5. Client adds their products (or you import them as part of onboarding)
6. Stripe onboarding on **the client's own Stripe account** (money goes straight to them)
7. Set up the backup cron
8. Hand over admin credentials, have them change the password on the Lösenord page
