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
- [ ] Project setup (Spring Boot backend, frontend)
- [ ] Hosting connected (Vercel / Render / Neon)
- [ ] Feature: login

## Next step
Get Akanksha's feedback on the screens, then database and API design.
