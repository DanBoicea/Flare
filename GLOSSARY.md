# Flare

Flare lets people in an airport report a problem or ask for help by scanning a QR code, and lets airport staff see and handle what was reported.

## Language

### Passengers and what they send

**Passenger**:
Anyone in the airport who uses the passenger web app, whether or not they are flying.
_Avoid_: user, customer, reporter

**Report**:
One message from one Passenger describing a problem that staff should handle.
_Avoid_: request, complaint, ticket, submission

**Issue**:
One real-world problem, described by one or more Reports. Staff work on Issues, not on individual Reports.
_Avoid_: incident, case, ticket, task

**Question**:
A message from a Passenger asking for information, answered from the Knowledge base instead of being sent to staff.
_Avoid_: FAQ, help request, inquiry

**Emergency**:
A message suggesting immediate danger to people, such as a medical emergency, a security threat, a missing child or fire. The Passenger is shown emergency instructions at once.
_Avoid_: urgent report, alarm

**Bounced**:
Describes a message the quality check judged to be garbage, a joke or spam. It is sent back to the Passenger to rewrite and never reaches staff.
_Avoid_: rejected, blocked, deleted

**Quarantined**:
Describes a Report that was accepted and kept, but held out of every Team's list until the duty manager reviews it, for example because of rate limits or an unknown QR code.
_Avoid_: spam folder, blocked, bounced

**Unclassified**:
Describes a Report the LLM could not classify, for example during an outage. It goes to the duty manager.
_Avoid_: failed, unknown report

### Places

**Location**:
A named place in the airport, as specific as one particular toilet or one lift.
_Avoid_: spot, point

**Area**:
A larger part of the airport that contains several nearby Locations, such as the arrivals hall. Reports from the same Area may describe the same Issue.
_Avoid_: zone, region, sector

**QR code**:
A printed code placed at one Location that opens the passenger web app. Always say "QR code", never just "code".
_Avoid_: code, tag, sticker

**Location label**:
The short human-readable identifier printed beneath a QR code so staff put each poster at the right Location.
_Avoid_: location code, poster ID

**Strict mode**:
A temporary state of a QR code that is receiving a suspicious flood of messages. While it lasts, that QR code's messages are Quarantined, except Emergencies.
_Avoid_: lockdown, attack mode

### Staff side

**Staff app**:
The mobile app staff use to see and handle Issues. Its main screen is the Issue list.
_Avoid_: employee app, dashboard, admin panel

**Passenger web app**:
The web page a Passenger opens by scanning a QR code.
_Avoid_: form, site, portal

**Staff member**:
A person with their own Staff app account. They belong to one or more Teams and hold one or more roles: staff, duty manager.
_Avoid_: employee, user, agent

**Team**:
A group of staff members responsible for one kind of Issue, such as Cleaning or Security. A staff member can belong to several Teams. Each Issue belongs to at most one Team.
_Avoid_: department, role, unit

**Duty manager**:
The role of a staff member who sees every Issue whatever its Team, receives Unclassified Reports, and reviews Quarantined ones. It is a role, not a Team.
_Avoid_: admin, supervisor, operations manager

**Knowledge base**:
The curated set of airport facts used to answer Questions.
_Avoid_: FAQ, docs, help centre

**Severity**:
How serious an Issue is: Critical, High, Medium or Low.
_Avoid_: priority (priority also weighs the number of Reports and their age), urgency

### Travel

**Ticket**:
A Passenger's plane ticket or boarding pass. Flare never asks for, stores or processes Tickets.
_Avoid_: boarding card (use Ticket)
