---
status: accepted
date: 2026-10-30
amended: 2026-10-31 (staff members can belong to several Teams; duty manager is a role; safeguards confirmed)
---

# Staff have individual accounts and see only their Teams' Issues, from phase 1

Each staff member has their own account and belongs to one or more Teams: Cleaning, Maintenance, Security or Passenger assistance. They see the Issues of all their Teams. People often cover several areas; nobody does only Passenger assistance, for example. Each Issue belongs to at most one Team. Duty manager is a role, not a Team: a duty manager sees every Issue and reviews the Quarantined ones. This replaces the earlier plan of one shared staff view for phase 1. Cleaning staff should not receive security Issues and security should not receive cleaning Issues, and individual accounts make the audit log meaningful.

Routing now decides who sees what, so these safeguards are part of the design:
- Critical Issues are always visible to Security and the duty manager.
- Unclassified, "Other" and low-confidence Issues go to the duty manager.
- Any staff member who can see an Issue can hand it over to another Team.
- Only the duty manager sees the Needs review list (Quarantined Reports and QR code alerts).

This ADR was corrected in place on 2026-10-31 because nothing had been built on it yet. From now on, a change to a decision that has code behind it gets a new, superseding ADR.
