"use strict";

const { onRequest } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");

initializeApp();
const db = getFirestore();

const TICKET_ID_REGEX = /^[A-Za-z0-9_-]{4,64}$/;

const PASSENGER_LABELS = {
  REGULAR: { label: "Regular", discounted: false },
  STUDENT: { label: "Student", discounted: true },
  SENIOR: { label: "Senior Citizen", discounted: true },
  PWD: { label: "PWD", discounted: true }
};

function escapeHtml(value) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function extractTicketId(reqPath) {
  const cleanPath = String(reqPath || "").replace(/\/+$/, "");
  const match = cleanPath.match(/^\/t\/([^/?#]+)$/);
  if (!match) {
    return null;
  }
  try {
    const decoded = decodeURIComponent(match[1]).trim();
    return TICKET_ID_REGEX.test(decoded) ? decoded : null;
  } catch (_err) {
    return null;
  }
}

function formatPhp(amount) {
  const num = Number(amount);
  if (!Number.isFinite(num)) {
    return "₱0.00";
  }
  return `₱${num.toLocaleString("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })}`;
}

function formatIssuedAt(epochMillis) {
  const ms = Number(epochMillis);
  if (!Number.isFinite(ms) || ms <= 0) {
    return "N/A";
  }
  try {
    const formatted = new Intl.DateTimeFormat("en-US", {
      timeZone: "Asia/Manila",
      month: "short",
      day: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      hour12: true
    }).format(new Date(ms));
    return `${formatted} PHT`;
  } catch (_err) {
    return new Date(ms).toISOString();
  }
}

function applySecurityHeaders(res) {
  res.set("Content-Type", "text/html; charset=utf-8");
  res.set("Cache-Control", "private, no-store, max-age=0");
  res.set("X-Content-Type-Options", "nosniff");
  res.set("X-Frame-Options", "DENY");
  res.set("Referrer-Policy", "strict-origin-when-cross-origin");
}

function renderNotFoundHtml(rawTicketId) {
  const refHint = rawTicketId
    ? `Ticket ID: #${escapeHtml(String(rawTicketId).slice(0, 8).toUpperCase())}`
    : "Invalid or missing ticket link";

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <meta name="robots" content="noindex, nofollow" />
  <title>Ticket Not Found — AeTranzit</title>
  <link rel="preconnect" href="https://fonts.googleapis.com" />
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Plus+Jakarta+Sans:wght@600;700;800&display=swap" rel="stylesheet" />
  <style>
    :root {
      --navy: #0A192F;
      --navy-card: #112240;
      --blue: #007BFF;
      --teal: #00C9A7;
      --paper: #FFFFFF;
      --ink: #0A192F;
      --muted: #52637A;
      --border: #D5DFEC;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      min-height: 100vh;
      font-family: "Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      background: linear-gradient(165deg, #0A192F 0%, #112240 60%, #0D274D 100%);
      color: var(--ink);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 20px 16px;
    }
    .card {
      width: 100%;
      max-width: 440px;
      background: var(--paper);
      border-radius: 20px;
      overflow: hidden;
      box-shadow: 0 20px 48px rgba(4, 12, 26, 0.45);
      border: 1px solid rgba(255, 255, 255, 0.12);
    }
    .header {
      background: var(--navy);
      padding: 20px 22px;
      border-bottom: 3px solid var(--teal);
    }
    .brand {
      font-family: "Plus Jakarta Sans", sans-serif;
      font-size: 1.2rem;
      font-weight: 800;
      color: #FFFFFF;
      letter-spacing: -0.01em;
    }
    .subtitle {
      font-size: 0.72rem;
      font-weight: 700;
      color: var(--teal);
      letter-spacing: 0.08em;
      text-transform: uppercase;
      margin-top: 2px;
    }
    .body {
      padding: 28px 22px;
      text-align: center;
    }
    .status-pill {
      display: inline-block;
      background: #FFF4E5;
      color: #9A4D00;
      font-size: 0.75rem;
      font-weight: 700;
      letter-spacing: 0.05em;
      text-transform: uppercase;
      padding: 6px 12px;
      border-radius: 999px;
      margin-bottom: 14px;
    }
    h1 {
      font-family: "Plus Jakarta Sans", sans-serif;
      font-size: 1.3rem;
      font-weight: 700;
      color: var(--ink);
      margin-bottom: 10px;
      line-height: 1.3;
    }
    p {
      font-size: 0.92rem;
      line-height: 1.55;
      color: var(--muted);
      margin-bottom: 16px;
    }
    .ref-chip {
      display: inline-block;
      font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
      font-size: 0.82rem;
      font-weight: 600;
      color: var(--ink);
      background: #F4F7FB;
      border: 1px solid var(--border);
      padding: 6px 12px;
      border-radius: 8px;
      margin-bottom: 22px;
    }
    .actions {
      display: flex;
      gap: 10px;
      justify-content: center;
      flex-wrap: wrap;
    }
    .btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-height: 46px;
      padding: 0 18px;
      border-radius: 12px;
      font-family: "Plus Jakarta Sans", sans-serif;
      font-size: 0.9rem;
      font-weight: 700;
      text-decoration: none;
      cursor: pointer;
      border: none;
    }
    .btn-primary {
      background: var(--blue);
      color: #FFFFFF;
    }
    .btn-outline {
      background: transparent;
      color: var(--ink);
      border: 1.5px solid var(--border);
    }
  </style>
</head>
<body>
  <main class="card">
    <header class="header">
      <div class="brand">AeTranzit</div>
      <div class="subtitle">Web Ticket Verification</div>
    </header>
    <section class="body">
      <div class="status-pill">Pending Sync or Not Found</div>
      <h1>Ticket not found or still syncing from the bus</h1>
      <p>
        AeTranzit buses operate offline-first in low-signal areas. If your ticket was just issued,
        please wait until the conductor's device syncs to the cloud and refresh this page.
      </p>
      <div class="ref-chip">${refHint}</div>
      <div class="actions">
        <button type="button" class="btn btn-primary" onclick="window.location.reload()">Refresh Status</button>
        <a href="/" class="btn btn-outline">Verification Home</a>
      </div>
    </section>
  </main>
</body>
</html>`;
}

function renderTicketReceiptHtml(viewModel) {
  const {
    companyName,
    ticketId,
    referenceCode,
    isDroppedOff,
    statusBadgeText,
    seatNumber,
    routeLabel,
    destination,
    busAndPlate,
    passengerLabel,
    isDiscounted,
    farePaid,
    cashTendered,
    changeGiven,
    issuedAt
  } = viewModel;

  const resolvedBrand = (companyName && String(companyName).trim()) || "AeTranzit";
  const badgeClass = isDroppedOff ? "badge badge-dropped" : "badge badge-active";

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <meta name="robots" content="noindex, nofollow" />
  <title>Ticket #${escapeHtml(referenceCode)} — ${escapeHtml(resolvedBrand)} Receipt</title>
  <link rel="preconnect" href="https://fonts.googleapis.com" />
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Plus+Jakarta+Sans:wght@600;700;800&display=swap" rel="stylesheet" />
  <style>
    :root {
      --navy: #0A192F;
      --navy-elevated: #112240;
      --blue: #007BFF;
      --teal: #00C9A7;
      --teal-bg: #D9FBF3;
      --teal-ink: #004D3F;
      --gray-bg: #E6ECF4;
      --gray-ink: #3E4E63;
      --paper: #FFFFFF;
      --section-bg: #F4F7FB;
      --ink: #0A192F;
      --subtle: #52637A;
      --divider: #D5DFEC;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      min-height: 100vh;
      font-family: "Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      background: linear-gradient(165deg, #0A192F 0%, #112240 60%, #0D274D 100%);
      color: var(--ink);
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 20px 14px 36px;
    }
    .shell {
      width: 100%;
      max-width: 440px;
    }
    .ticket-card {
      background: var(--paper);
      border-radius: 22px;
      overflow: hidden;
      border: 1px solid var(--divider);
      box-shadow: 0 22px 50px rgba(4, 12, 26, 0.46);
    }
    .ticket-header {
      background: var(--navy);
      padding: 18px 20px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      border-bottom: 3px solid var(--teal);
    }
    .brand-title {
      font-family: "Plus Jakarta Sans", sans-serif;
      font-size: 1.15rem;
      font-weight: 800;
      color: #FFFFFF;
      letter-spacing: -0.01em;
    }
    .brand-subtitle {
      font-size: 0.68rem;
      font-weight: 700;
      color: var(--teal);
      letter-spacing: 0.08em;
      text-transform: uppercase;
      margin-top: 3px;
    }
    .badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 11px;
      border-radius: 999px;
      font-family: "Plus Jakarta Sans", sans-serif;
      font-size: 0.72rem;
      font-weight: 800;
      letter-spacing: 0.04em;
      white-space: nowrap;
    }
    .badge-active {
      background: var(--teal-bg);
      color: var(--teal-ink);
    }
    .badge-dropped {
      background: var(--gray-bg);
      color: var(--gray-ink);
    }
    .badge-dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: currentColor;
    }
    .ticket-body {
      padding: 20px;
    }
    .hero-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
    }
    .ref-label {
      font-size: 0.72rem;
      font-weight: 700;
      letter-spacing: 0.06em;
      color: var(--subtle);
      text-transform: uppercase;
    }
    .ref-code {
      font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
      font-size: 1.55rem;
      font-weight: 800;
      color: var(--ink);
      margin-top: 3px;
    }
    .seat-box {
      background: var(--blue);
      color: #FFFFFF;
      min-width: 88px;
      padding: 9px 16px;
      border-radius: 14px;
      text-align: center;
    }
    .seat-caption {
      font-family: "Plus Jakarta Sans", sans-serif;
      font-size: 0.68rem;
      font-weight: 800;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      opacity: 0.92;
    }
    .seat-value {
      font-family: "Plus Jakarta Sans", sans-serif;
      font-size: 1.65rem;
      font-weight: 800;
      line-height: 1.15;
      font-variant-numeric: tabular-nums;
    }
    .perforated {
      border: none;
      border-top: 2px dashed var(--divider);
      margin: 18px 0;
    }
    .details {
      display: flex;
      flex-direction: column;
      gap: 11px;
    }
    .detail-row {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      gap: 16px;
      font-size: 0.9rem;
    }
    .detail-label {
      color: var(--subtle);
      flex-shrink: 0;
    }
    .detail-value {
      color: var(--ink);
      font-weight: 600;
      text-align: right;
      word-break: break-word;
    }
    .discount-pill {
      display: inline-block;
      background: var(--teal-bg);
      color: var(--teal-ink);
      font-size: 0.7rem;
      font-weight: 800;
      padding: 2px 7px;
      border-radius: 6px;
      margin-left: 6px;
      vertical-align: middle;
    }
    .fare-box {
      margin-top: 18px;
      background: var(--section-bg);
      border: 1px solid var(--divider);
      border-radius: 16px;
      padding: 15px 16px;
    }
    .fare-hero {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      padding-bottom: 10px;
      margin-bottom: 10px;
      border-bottom: 1px solid var(--divider);
    }
    .fare-hero-label {
      font-family: "Plus Jakarta Sans", sans-serif;
      font-size: 1rem;
      font-weight: 700;
      color: var(--ink);
    }
    .fare-hero-amount {
      font-family: "Plus Jakarta Sans", sans-serif;
      font-size: 1.45rem;
      font-weight: 800;
      color: var(--blue);
      font-variant-numeric: tabular-nums;
    }
    .amount-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 0.88rem;
      padding: 3px 0;
    }
    .amount-value {
      font-weight: 700;
      color: var(--ink);
      font-variant-numeric: tabular-nums;
    }
    .verified-footer {
      margin-top: 16px;
      text-align: center;
      font-size: 0.76rem;
      color: var(--subtle);
      line-height: 1.45;
    }
    .verified-id {
      font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
      font-size: 0.7rem;
      color: var(--subtle);
      margin-top: 4px;
      word-break: break-all;
    }
    .action-bar {
      margin-top: 16px;
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    .btn-print {
      width: 100%;
      min-height: 50px;
      border: none;
      border-radius: 14px;
      background: var(--blue);
      color: #FFFFFF;
      font-family: "Plus Jakarta Sans", sans-serif;
      font-size: 0.96rem;
      font-weight: 700;
      cursor: pointer;
      box-shadow: 0 10px 24px rgba(0, 123, 255, 0.32);
    }
    .btn-print:active {
      transform: translateY(1px);
    }
    .portal-link {
      text-align: center;
      font-size: 0.8rem;
      color: rgba(255, 255, 255, 0.78);
    }
    .portal-link a {
      color: var(--teal);
      text-decoration: none;
      font-weight: 600;
    }
    @media print {
      @page {
        margin: 12mm;
      }
      body {
        background: #FFFFFF !important;
        padding: 0 !important;
        min-height: auto !important;
      }
      .shell {
        max-width: 100% !important;
      }
      .ticket-card {
        box-shadow: none !important;
        border: 1.5px solid #9FB0C7 !important;
      }
      .ticket-header,
      .seat-box,
      .badge,
      .fare-box {
        -webkit-print-color-adjust: exact;
        print-color-adjust: exact;
      }
      .action-bar {
        display: none !important;
      }
    }
  </style>
</head>
<body>
  <main class="shell">
    <article class="ticket-card">
      <header class="ticket-header">
        <div>
          <div class="brand-title">${escapeHtml(resolvedBrand)}</div>
          <div class="brand-subtitle">Official Bus Passenger Receipt</div>
        </div>
        <span class="${badgeClass}">
          <span class="badge-dot"></span>
          ${escapeHtml(statusBadgeText)}
        </span>
      </header>

      <div class="ticket-body">
        <div class="hero-row">
          <div>
            <div class="ref-label">Ticket Ref</div>
            <div class="ref-code">#${escapeHtml(referenceCode)}</div>
          </div>
          <div class="seat-box" aria-label="Seat ${escapeHtml(seatNumber)}">
            <div class="seat-caption">Seat</div>
            <div class="seat-value">${escapeHtml(seatNumber)}</div>
          </div>
        </div>

        <hr class="perforated" />

        <div class="details">
          <div class="detail-row">
            <span class="detail-label">Route</span>
            <span class="detail-value">${escapeHtml(routeLabel)}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">Drop-off Stop</span>
            <span class="detail-value">${escapeHtml(destination)}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">Bus &amp; Plate</span>
            <span class="detail-value">${escapeHtml(busAndPlate)}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">Passenger Type</span>
            <span class="detail-value">
              ${escapeHtml(passengerLabel)}${isDiscounted ? '<span class="discount-pill">-20%</span>' : ""}
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-label">Issued</span>
            <span class="detail-value">${escapeHtml(issuedAt)}</span>
          </div>
        </div>

        <section class="fare-box" aria-label="Fare Breakdown">
          <div class="fare-hero">
            <span class="fare-hero-label">Fare Paid</span>
            <span class="fare-hero-amount">${escapeHtml(farePaid)}</span>
          </div>
          <div class="amount-row">
            <span class="detail-label">Cash Tendered</span>
            <span class="amount-value">${escapeHtml(cashTendered)}</span>
          </div>
          <div class="amount-row">
            <span class="detail-label">Change Given</span>
            <span class="amount-value">${escapeHtml(changeGiven)}</span>
          </div>
        </section>

        <hr class="perforated" />

        <footer class="verified-footer">
          Verified Official Receipt • Powered by AeTranzit
          <div class="verified-id">ID: ${escapeHtml(ticketId)}</div>
        </footer>
      </div>
    </article>

    <div class="action-bar">
      <button type="button" class="btn-print" onclick="window.print()">Print / Save PDF</button>
      <p class="portal-link">
        Powered by AeTranzit • <a href="https://aedev.aesprt.com" target="_blank" rel="noopener noreferrer">AeDev Transit Systems</a>
      </p>
    </div>
  </main>
</body>
</html>`;
}

exports.viewTicket = onRequest(
  {
    region: "us-central1",
    cors: false,
    maxInstances: 10
  },
  async (req, res) => {
    applySecurityHeaders(res);

    if (req.method !== "GET" && req.method !== "HEAD") {
      res.status(405).send("Method Not Allowed");
      return;
    }

    const ticketId = extractTicketId(req.path);
    if (!ticketId) {
      res.status(404).send(renderNotFoundHtml(""));
      return;
    }

    try {
      const ticketSnap = await db.collection("tickets").doc(ticketId).get();
      if (!ticketSnap.exists) {
        res.status(404).send(renderNotFoundHtml(ticketId));
        return;
      }

      const ticket = ticketSnap.data() || {};
      const tripId = typeof ticket.tripId === "string" ? ticket.tripId.trim() : "";

      let tripData = null;
      if (tripId) {
        const tripSnap = await db.collection("trips").doc(tripId).get();
        if (tripSnap.exists) {
          tripData = tripSnap.data() || null;
        }
      }

      const resolvedBusId =
        (typeof ticket.busId === "string" && ticket.busId.trim()) ||
        (tripData && typeof tripData.busId === "string" && tripData.busId.trim()) ||
        "";
      const resolvedRouteId =
        (typeof ticket.routeId === "string" && ticket.routeId.trim()) ||
        (tripData && typeof tripData.routeId === "string" && tripData.routeId.trim()) ||
        "";
      const resolvedCompanyId =
        (typeof ticket.companyId === "string" && ticket.companyId.trim()) ||
        (tripData && typeof tripData.companyId === "string" && tripData.companyId.trim()) ||
        "";

      const [companySnap, busSnap, routeSnap] = await Promise.all([
        resolvedCompanyId
          ? db.collection("companies").doc(resolvedCompanyId).get()
          : Promise.resolve(null),
        resolvedBusId ? db.collection("buses").doc(resolvedBusId).get() : Promise.resolve(null),
        resolvedRouteId ? db.collection("routes").doc(resolvedRouteId).get() : Promise.resolve(null)
      ]);

      const companyData = companySnap && companySnap.exists ? companySnap.data() : null;
      const busData = busSnap && busSnap.exists ? busSnap.data() : null;
      const routeData = routeSnap && routeSnap.exists ? routeSnap.data() : null;

      let companyName =
        (companyData && typeof companyData.name === "string" && companyData.name.trim()) || "";
      if (!companyName && busData && typeof busData.companyId === "string" && busData.companyId.trim()) {
        const fallbackCompSnap = await db.collection("companies").doc(busData.companyId.trim()).get();
        if (fallbackCompSnap.exists) {
          const fallbackComp = fallbackCompSnap.data() || {};
          companyName = (typeof fallbackComp.name === "string" && fallbackComp.name.trim()) || "";
        }
      }
      if (!companyName) {
        companyName = "AeTranzit";
      }

      const busNumber =
        (busData && typeof busData.busNumber === "string" && busData.busNumber.trim()) || "N/A";
      const plateNumber =
        (busData && typeof busData.plateNumber === "string" && busData.plateNumber.trim()) || "N/A";
      const busAndPlate =
        busNumber === "N/A" && plateNumber === "N/A"
          ? "Bus Metadata Syncing"
          : `${busNumber} • ${plateNumber}`;

      const origin =
        routeData && typeof routeData.origin === "string" ? routeData.origin.trim() : "";
      const routeDest =
        routeData && typeof routeData.destination === "string" ? routeData.destination.trim() : "";
      const routeLabel =
        origin && routeDest ? `${origin} → ${routeDest}` : origin || routeDest || "N/A";

      const rawStatus = String(ticket.status || "ACTIVE").toUpperCase();
      const isDroppedOff = rawStatus === "DROPPED_OFF";
      const statusBadgeText = isDroppedOff ? "DROPPED OFF" : "VALID / ACTIVE";

      const rawPassengerType = String(ticket.passengerType || "REGULAR").toUpperCase();
      const passengerMeta = PASSENGER_LABELS[rawPassengerType] || PASSENGER_LABELS.REGULAR;

      const html = renderTicketReceiptHtml({
        companyName,
        ticketId,
        referenceCode: ticketId.slice(0, 8).toUpperCase(),
        isDroppedOff,
        statusBadgeText,
        seatNumber: String(ticket.seatNumber || "—"),
        routeLabel,
        destination: String(ticket.destination || "N/A"),
        busAndPlate,
        passengerLabel: passengerMeta.label,
        isDiscounted: passengerMeta.discounted,
        farePaid: formatPhp(ticket.fareAmount),
        cashTendered: formatPhp(ticket.cashTendered),
        changeGiven: formatPhp(ticket.changeAmount),
        issuedAt: formatIssuedAt(ticket.createdAt)
      });

      res.status(200).send(html);
    } catch (error) {
      console.error("viewTicket failed:", error);
      res.status(500).send(renderNotFoundHtml(ticketId));
    }
  }
);
