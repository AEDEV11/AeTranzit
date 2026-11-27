# AeTranzit — Firebase Deployment & Admin Bootstrap Guide

This guide covers deploying **Firestore Security Rules**, the **`viewTicket` Cloud Function (Node.js 20, v2)**, and **Firebase Hosting** for project `aetranzit`, plus bootstrapping the first `ADMIN` account.

---

## 1. Prerequisites

1. **Node.js 20+** installed locally (`node -v`).
2. **Firebase CLI** installed (or run via `npx firebase-tools@latest`).
3. **Firebase Blaze (Pay-as-you-go) Plan** enabled on project `aetranzit` (required by Google Cloud Build / Cloud Run for Firebase Functions v2, within generous free-tier allowances).

---

## 2. Authenticate & Install Function Dependencies

From the repository root:

```bash
# 1. Sign in to Firebase CLI
firebase login

# 2. Verify active project (.firebaserc defaults to `aetranzit`)
firebase use aetranzit

# 3. Install Cloud Functions dependencies
npm --prefix functions install
```

---

## 3. Deploy Rules, Cloud Function & Web Receipt Hosting

```bash
firebase deploy --only firestore:rules,functions,hosting
```

Once deployed:
- **Landing Portal:** `https://aetranzit.web.app/`
- **Passenger Ticket Receipt URL:** `https://aetranzit.web.app/t/<ticketId>`
  - Scanning the QR code on [`DigitalTicketScreen`](../app/src/main/java/com/aedev/aetranzit/ui/conductor/ticket/DigitalTicketScreen.kt) or tapping the SMS link opens `/t/<ticketId>`, which Firebase Hosting routes to the `viewTicket` Cloud Function via server-side Admin SDK (keeping `tickets` closed to public client queries).

---

## 4. Bootstrapping the First `ADMIN` Account in Firestore Console

To prevent privilege escalation, [`firestore.rules`](../firestore.rules) blocks any client from self-assigning `role: "ADMIN"` on `users/{uid}` creation.

To bootstrap your first **Admin** account for Cloud Sync:

1. **Create the account in the Android app (or Firebase Auth Console):**
   - Option A (Recommended): Register the user in **Firebase Console → Authentication → Users → Add user** (copy the generated **User UID**), then create the matching document in **Firestore Database → `users` collection** with Document ID = `<Firebase Auth UID>`:
     ```json
     {
       "id": "<Firebase Auth UID>",
       "name": "System Administrator",
       "email": "admin@aetranzit.ph",
       "role": "ADMIN",
       "isActive": true,
       "updatedAt": 1764200000000
     }
     ```
     Then in the Android app, enable **Cloud Sync** in Settings and sign in with `admin@aetranzit.ph`.
   - Option B (Promote an existing synced Conductor): Register first as `CONDUCTOR` with Cloud Sync enabled so `users/<Firebase Auth UID>` is created automatically, then open **Firebase Console → Firestore Database → `users/<Firebase Auth UID>`** and change `role` from `"CONDUCTOR"` to `"ADMIN"`.
2. Subsequent staff activation/deactivation can be managed directly from **Admin Dashboard → Manage Staff** inside the Android app.

---

## 5. Custom Domain (`aetranzit.aesprt.com`) vs Default Hosting (`aetranzit.web.app`)

[`TicketPayloadFormatter.WEB_TICKET_BASE_URL`](../app/src/main/java/com/aedev/aetranzit/domain/usecase/TicketPayloadFormatter.kt) is currently configured to `https://aetranzit.aesprt.com/t/`.

1. Open **Firebase Console → Hosting → Add custom domain** and enter `aetranzit.aesprt.com`.
2. Add the provided `TXT` verification and `A` DNS records at your DNS provider for `aesprt.com`.
3. Wait for SSL certificate provisioning to complete.
4. *(Optional fallback)* If you want to test immediately before DNS propagation completes, temporarily set `WEB_TICKET_BASE_URL` in [`TicketPayloadFormatter.kt`](../app/src/main/java/com/aedev/aetranzit/domain/usecase/TicketPayloadFormatter.kt) to `"https://aetranzit.web.app/t/"` and switch back to `"https://aetranzit.aesprt.com/t/"` once SSL is active.
