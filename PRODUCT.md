# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Two audiences use this system:

- **Shoppers** — golfers browsing and buying performance apparel: polos, trousers, and accessories in sizes/colors, with a cart and order history.
- **Store staff** — a small team operating the back office (products, categories, orders, promotions, users, audit log) under real ADMIN/MANAGER/DEVELOPER roles, not a single solo operator.

## Product Purpose

A working e-commerce storefront for a modern performance golf apparel brand (brand name not yet chosen — currently placeholder "Scaffold"), paired with an admin back office for the staff who run it. Success is a shopper completing a purchase smoothly, and staff being able to manage catalog, stock, promotions, and orders without friction.

## Positioning

Modern performance golf apparel: clean athletic cut and technical fabric language, in the register of contemporary golf-lifestyle brands (Malbon Golf, Peter Millar, TravisMathew) rather than heritage/country-club pastiche (no tartan, crests, "Est. 18xx" affectation). This should read in tone, product presentation, and admin polish — not just in copy.

## Operating Context

- Catalog: products with variations (size, color, SKU, inventory count, price adjustment), categories (hierarchical), and images stored in Garage (S3-compatible).
- Shopping flow: browse/search products → product detail with variation selection → cart → order.
- Admin back office: dashboard (revenue, orders, customers, low-stock alerts), product/category/promotion/user management, order detail and status updates, audit log.
- Promotions: discount codes with redemption tracking, applied at cart/quote time.
- Roles are real and distinct: ADMIN, MANAGER, DEVELOPER (actuator/health access) each see different admin surface area — not interchangeable.

## Capabilities and Constraints

- Spring Boot 4.1.0 / Java 25, dual REST (`/api/*`) + server-rendered Thymeleaf interface.
- Tailwind CSS 4 + DaisyUI 5, HTMX for partial updates (search filtering, cart, validation feedback); minimal vanilla JS.
- Stateless JWT auth in httpOnly cookies; Redis-backed caching and rate limiting; PostgreSQL via Flyway migrations.
- Product images via Garage (self-hosted S3-compatible storage).
- Undecided: final brand name (currently placeholder "Scaffold"); currency/country handling (TOFIX notes a planned mini header for country/currency switching, not yet built).

## Brand Commitments

- No confirmed brand name yet — "Scaffold" is a working placeholder, not a committed identity. Do not treat it as a real brand name to design around; flag when a real name is chosen.
- Committed positioning: modern performance golf apparel (see Positioning).
- Confirmed palette: primary `#064E3B` (deep green), secondary `#E4DCCE` (cream). Pinned by the user — do not reinterpret or substitute.
- Standing visual direction: the homepage (and by extension the storefront) is built as the **category standard, played straight** — a full-bleed lifestyle photography hero, a clear primary action, and a clean product grid, executed at the craft level of **Peter Millar** and **Malbon Golf**'s own sites, without irony or a smuggled quirk. This is a deliberate, user-confirmed choice (not a default fallback) made after a full visual-world process dealt a grounded "course-routing/yardage-book" direction and six unrelated wildcard challengers; the user chose the straight canon over all of them. Future homepage/storefront work should hold this line rather than re-opening the world question.

## Evidence on Hand

- Seeded catalog (`V1_16__rename_catalog_to_golf_apparel.sql`): Clothing/Accessories categories with Polos/Trousers subcategories; sample products are a performance polo, a stretch golf trouser, and a leather belt — placeholder demo products, renamed from the original generic seed to be golf-plausible, with real (though non-final) stock photography. Not real inventory, real pricing, or real photography — swap for genuine SKUs and shoots when available.
- No real testimonials, pricing strategy, case studies, or customer evidence exists — future work must not fabricate any.
- `TOFIX.md` is a live backlog of known gaps (stock-empty checks, user address/profile management, payment method UI, country/currency switcher, admin dashboard analytics) — treat as a roadmap signal, not committed scope.

## Product Principles

1. Design for two real operators, not one — shopper-facing and staff-facing surfaces have different success criteria (conversion/clarity vs. task speed/scanability) and should be evaluated separately.
2. Let "modern performance golf" positioning show up as clean athletic confidence — real photography, technical fabric language, generous whitespace — never heritage/clubhouse pastiche.
3. Treat all current product data, images, and the "Scaffold" name as placeholders: functionally real, visually provisional. Don't over-invest polish in content that's expected to be swapped.
4. Respect the ADMIN/MANAGER/DEVELOPER role split as a real constraint on what each admin view should expose, not a formality.
5. Preserve existing HTMX-driven interaction patterns (partial updates, no heavy JS) rather than introducing a competing frontend approach.

## Accessibility & Inclusion

No product-specific accessibility requirement has been established yet.
