# BookDhara — Progress

> Read this first in every new session. It says what is done, what is next, and the key decisions.

## What BookDhara is
A used-book sharing app for India. People list books they have read at a low price. Buyers pay with money, credits, or both. Sellers earn credits that help them get future books. Books move by courier (shipped) or by local pickup in the same city.

Tagline: *"Kitaabein behti rahein."*

## Key decisions
- **Platform:** Android first (Play Store), web app (PWA) first to test the idea.
- **Backend:** Java + Spring Boot. **Database:** PostgreSQL.
- **Hosting (free tiers):** Vercel (frontend), Render (backend), Neon (Postgres).
- **Workflow:** Claude writes code in the cloud and pushes to GitHub; hosting auto-deploys on push.
  - Until the first deploy: push directly to `main`.
  - After the first deploy: every change goes through a branch + Pull Request, and Akanksha merges.
- **Money:**
  - Buyer pays: book price + shipping + platform fee.
  - Credits (1 credit = ₹1) can pay only for the book price. Shipping and fee are always cash.
  - Seller chooses cash or credits (credits get a bonus, e.g. ₹40 or 50 credits).
  - Money is held until the buyer confirms delivery, then released to the seller.
- **Local pickup:** buyer pays in the app; seller enters the buyer's 4-digit handover code to get paid. Platform fee is non-refundable once a meetup is set.
- **Shipping:** courier aggregator API (e.g. Shiprocket). Exact rates still to be checked.

## First version (MVP) scope
1. Phone login (OTP)
2. ISBN scan to list a book
3. Search + "near me"
4. Buy (shipped or local pickup with handover code)
5. Credits wallet
6. Ratings

## Status
- [x] Name chosen: BookDhara
- [x] GitHub org + repo created, connected to Claude
- [x] Screen designs v1 (Home, Book detail, Checkout, Pickup code, List a book, Wallet): https://claude.ai/artifact/LWq8k5YwGrrTtWGFhhCCc7
- [ ] Database + API design
- [x] Backend setup: Spring Boot 3.5, Java 21, Gradle, Flyway, Dockerfile, GitHub Actions CI (green)
- [ ] Frontend setup (Flutter: web + Android)
- [ ] Hosting connected (Vercel / Render / Neon)
- [x] Feature: phone OTP login (backend)
  - `POST /api/auth/otp/request` {phone} -> sends OTP (returns `devCode` while no SMS provider)
  - `POST /api/auth/otp/verify` {phone, otp} -> JWT + user + `needsProfile`
  - `GET /api/me`, `PATCH /api/me` {name} (Bearer token)
  - SMS provider not chosen yet (`SmsSender` interface; `LoggingSmsSender` placeholder)
- [ ] Feature: login screens (frontend)

## Next step
Deploy backend (Neon DB + Render), then Flutter app with login screens.

## Build notes
- The cloud session cannot download Maven/npm packages, so GitHub Actions is the build and test loop: push, then `gh run list -R bookdhara/bookdhara-app`.
- Env vars for the backend: `DB_URL` (jdbc:postgresql://...), `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (32+ chars), `CORS_ALLOWED_ORIGINS`, `OTP_EXPOSE_IN_RESPONSE`.
