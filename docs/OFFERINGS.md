# Offerings / ચઢાવો — Feature Specification

## 1. Non-Negotiable Product Rule

> **CRITICAL ARCHITECTURAL RULE:**
> 
> Under **NO circumstances** should this feature be referred to, modeled as, or implemented as an **Auction**, **Bidding System**, or **Commercial Sale**.
> 
> The official product terms in all codebases, UI elements, database columns, and documentation are:
> - Gujarati: **ચઢાવો**
> - English: **Offerings**

---

## 2. Cultural & Functional Context

In Gujarati village traditions, a **ચઢાવો (Offering)** represents a voluntary, devotional, or community contribution towards:
- Temple flag hoisting (*Dhwajarohan*)
- Festival Aarti seva
- Gaushala green fodder support
- Sacred literature and articles
- Village festival arrangements

It is **NOT** a competitive auction. Participants contribute voluntary seva amounts to support community and spiritual functions.

---

## 3. Forbidden Terminology vs Approved Terminology

| FORBIDDEN AUCTION TERMINOLOGY | APPROVED OFFERINGS TERMINOLOGY | GUJARATI EQUIVALENT |
| :--- | :--- | :--- |
| Auction / Auctions | Offering / Offerings | **ચઢાવો** |
| Bid / Place a Bid | Submit Offering / Add Offering | **ચઢાવો ઉમેરો / નોંધાવો** |
| Current Bid / Minimum Bid | Offering Amount | **ચઢાવાની રકમ** |
| Highest Bidder | Offering Contributor | **ચઢાવો દાતા** |
| Auction Winner | Offering Completed | **સંપન્ન સેવા** |
| Auction Timer / Countdown | Offering Duration / Dates | **તારીખ અને સમયગાળો** |

---

## 4. Offering Lifecycle

```text
[ Citizen / Temple Committee Submits Offering ]
                      │
                      ▼
               PENDING_APPROVAL
                      │
        ┌─────────────┴─────────────┐
        ▼                           ▼
    REJECTED                     APPROVED
                                    │
                                    ▼
                                  ACTIVE
                      (Community Participation Open)
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        ▼                           ▼                           ▼
    COMPLETED                   CANCELLED                    EXPIRED
```

### Lifecycle States
1. **DRAFT**: Citizen or committee member drafting the offering.
2. **PENDING_APPROVAL**: Submitted to village admin for moderation.
3. **APPROVED**: Vetted and approved by village authority.
4. **ACTIVE**: Published in mobile app for village community members.
5. **COMPLETED**: Offering seva successfully conducted and concluded.
6. **CANCELLED**: Cancelled by organizers.
7. **EXPIRED**: Duration passed without event.
8. **REJECTED**: Rejected by administrator with optional note.

---

## 5. Security & Authorization

- Anyone can browse **APPROVED** and **ACTIVE** offerings.
- Staff create and manage offerings; resident accounts cannot create or edit them.
- Authenticated community members can contribute bids to active offerings while bidding is open.
- Only staff roles can moderate offerings and manage bid records. Apply migration `010_admin_created_offerings.sql` to enforce these rules in Supabase RLS.
