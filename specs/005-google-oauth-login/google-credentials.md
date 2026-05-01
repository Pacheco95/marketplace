# Setting Up Google OAuth Credentials

This guide walks you through creating a Google OAuth 2.0 client ID and secret for local development.

## Prerequisites

- A Google account
- Access to [Google Cloud Console](https://console.cloud.google.com)

---

## Step 1 — Create or Select a Project

1. Open [Google Cloud Console](https://console.cloud.google.com).
2. Click the project selector in the top navigation bar.
3. Click **New Project**, give it a name (e.g. `marketplace-dev`), and click **Create**.
4. Wait for the project to be created, then make sure it is selected in the top bar.

---

## Step 2 — Configure the OAuth Consent Screen

Before creating credentials you must configure what Google will show users during the sign-in flow.

1. In the left sidebar go to **APIs & Services → OAuth consent screen**.
2. Select **External** user type and click **Create**.
3. Fill in the required fields:
   - **App name**: `Marketplace (dev)`
   - **User support email**: your Google account email
   - **Developer contact email**: your Google account email
4. Leave all other fields blank and click **Save and Continue** through the Scopes, Test Users, and Summary steps.
5. Back on the consent screen overview, the publishing status will be **Testing**. This is fine for local development — only accounts you add as test users will be able to sign in.
6. Click **+ Add Users**, add your own Google account (and any other developers), and save.

> **Note**: You do not need to publish the app or go through Google's verification process for local development.

---

## Step 3 — Create OAuth 2.0 Credentials

1. In the left sidebar go to **APIs & Services → Credentials**.
2. Click **+ Create Credentials → OAuth client ID**.
3. Set **Application type** to **Web application**.
4. Give it a name, e.g. `marketplace-local`.
5. Under **Authorized JavaScript origins** click **+ Add URI** and add:

   ```
   http://localhost:3000
   ```

   This is required for Google One Tap to work from the Nuxt frontend.

6. Under **Authorized redirect URIs** add **two** entries:

   **Entry 1** — Keycloak's broker callback (Google → Keycloak):

   ```
   http://localhost:8180/realms/marketplace/broker/google/endpoint
   ```

   **Entry 2** — Backend callback (Keycloak → Spring Boot):

   ```
   http://localhost:8080/api/v1/auth/callback
   ```

   Both are required. The first is where Google sends the auth code after the user consents; Keycloak then exchanges it and redirects to the second.

7. Click **Create**.

---

## Step 4 — Copy Your Credentials

A dialog will appear with your **Client ID** and **Client Secret**. Copy both values — you will need them in the next step. You can always retrieve them later from the Credentials page.

---

## Step 5 — Populate the `.env` File

From the repository root:

```sh
cp .env.example .env
```

Open `.env` and fill in the values:

```dotenv
GOOGLE_CLIENT_ID=<your-client-id>
GOOGLE_CLIENT_SECRET=<your-client-secret>
NUXT_PUBLIC_GOOGLE_CLIENT_ID=<your-client-id>
KEYCLOAK_CLIENT_SECRET=dev-secret
```

- `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` are used by Keycloak to federate Google sign-in.
- `NUXT_PUBLIC_GOOGLE_CLIENT_ID` is used by the Nuxt frontend to initialise Google One Tap. It must be the same client ID.
- `KEYCLOAK_CLIENT_SECRET` is the shared secret between the Spring Boot backend and Keycloak. Any string works for local development.

---

## Step 6 — Start the Stack

```sh
docker compose up -d
```

Once all four services are healthy, open [http://localhost:3000/login](http://localhost:3000/login) and click **Login with Google** to verify the flow end-to-end.

See [quickstart.md](./quickstart.md) for the full verification walkthrough.

---

## Troubleshooting

| Symptom                                         | Cause                                                                                         | Fix                                                                                                                                                                           |
| ----------------------------------------------- | --------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `redirect_uri_mismatch` error from Google       | The redirect URI in the auth request doesn't match the one registered in Google Cloud Console | Confirm **both** `http://localhost:8180/realms/marketplace/broker/google/endpoint` and `http://localhost:8080/api/v1/auth/callback` are listed under Authorized redirect URIs |
| `Access blocked: This app's request is invalid` | OAuth consent screen not configured, or you are not listed as a test user                     | Add your account under **OAuth consent screen → Test users**                                                                                                                  |
| One Tap popup never appears                     | JavaScript origin not registered, or browser is blocking third-party cookies                  | Confirm `http://localhost:3000` is listed under Authorized JavaScript origins; try in an incognito window                                                                     |
| `400: invalid_client` from Keycloak             | `GOOGLE_CLIENT_ID` or `GOOGLE_CLIENT_SECRET` in `.env` is wrong or missing                    | Double-check the values against the Google Cloud Console credentials page                                                                                                     |
| Keycloak fails to import the realm              | `.env` values are empty — Docker Compose substitutes blank strings                            | Ensure `.env` exists and all four variables are populated before running `docker compose up`                                                                                  |
