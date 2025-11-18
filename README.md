# Personal Branding SaaS API

Spring Boot service that powers authentication, reference data, and provider integrations (Meta, TikTok, YouTube). The application follows a clean architecture layering so new connectors can reuse the same contracts without leaking infrastructure details.

## Local Development

1. Export the environment variables listed in [`../docs/environment.md`](../docs/environment.md) (a starter file lives at `.env.example`). You can also adapt the helper script under `../infra/env.txt` if you are on Windows.
2. `cd personal-branding-saas-api && mvn clean install` to download dependencies and run tests.
3. Start supporting services via `docker compose -f ../infra/docker-compose.yml up -d`.
4. Boot the API with `mvn spring-boot:run` (the `local` profile is enabled automatically when `APP_PROFILE=local` or via `-Dspring.profiles.active=local`).

## Email delivery

Transactional emails (password reset, verification) are sent through the configured HTTP provider (Resend by default). Set `MAIL_PROVIDER_API_KEY` (and optionally `MAIL_PROVIDER_BASE_URL`) along with `MAIL_FROM`, making sure the sender address is verified. If you prefer SMTP, adjust `MailService` accordingly.

## Provider Onboarding

- **Meta / Instagram** – requires a Business Manager app, tester roles, linked IG business accounts, and specific redirect URIs. Follow the step-by-step checklist in [`../docs/meta-connector-setup.md`](../docs/meta-connector-setup.md) before attempting OAuth locally. Those instructions now also cover the prerequisite Page + IG linking TikTok needs when sharing Business assets.
- **TikTok** – uses the shared OAuth DTO/selection flow introduced for Meta. Once the Meta checklist above is complete, populate the `app.platform.tiktok.*` settings (client key/secret, redirect URI pointing at `http://localhost:3000/tiktok/callback`) and restart the API.
- **YouTube** – provision a Google Cloud project + OAuth client as described in [`../docs/youtube-connector-setup.md`](../docs/youtube-connector-setup.md). Set `app.platform.youtube.*` to the generated client credentials so the Spring service can perform PKCE + offline access token exchanges.
- **Future providers** – When adding YouTube/Threads/etc., mirror the same pattern: persist an OAuth state, exchange the code via a provider client, map provider-specific failures to `PlatformException`, and return `OAuthCompletionResult` so the SPA can handle selections and errors consistently.

Refer to `docs/hardening-plan.md` for the full roadmap and to `docs/architecture.md` for layering guidelines.
