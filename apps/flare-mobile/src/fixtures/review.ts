// SIMULATED DEMO DATA for mock mode: what the duty manager's Needs review shows after the D06 spam and
// unknown-QR-code scenarios. Locations and QR code IDs match data/airport/airport.json; the texts are invented.
import type { ReviewList } from '@/api/types';

const minutesAgo = (minutes: number) => new Date(Date.now() - minutes * 60_000).toISOString();

const departuresToilets = {
  id: '20000000-0000-4000-8000-000000000009',
  label: 'DEP-WC-02',
  name: { ro: 'Toalete plecări, zona porților nord', en: 'Departures toilets, north gates' },
};

export const fixtureReview: ReviewList = {
  items: [
    { reportId: '30000000-0000-4000-8000-000000000001', text: 'Toaleta e murdara', createdAt: minutesAgo(3), reason: 'QR_CODE_STRICT_MODE', scannedQrCodeId: 'xw9fwv52ua', location: departuresToilets },
    { reportId: '30000000-0000-4000-8000-000000000002', text: 'Toaleta e murdara', createdAt: minutesAgo(5), reason: 'RATE_LIMIT', scannedQrCodeId: 'xw9fwv52ua', location: departuresToilets },
    { reportId: '30000000-0000-4000-8000-000000000003', text: 'Toaleta e murdară', createdAt: minutesAgo(9), reason: 'UNKNOWN_QR_CODE', scannedQrCodeId: 'zzzzzzzzzz', location: null },
    { reportId: '30000000-0000-4000-8000-000000000004', text: 'lol', createdAt: minutesAgo(12), reason: 'SEND_ANYWAY', scannedQrCodeId: 'vgdfdvghwy', location: { id: '20000000-0000-4000-8000-000000000010', label: 'ARR-SEAT-01', name: { ro: 'Zona de așteptare sosiri', en: 'Arrivals meeting area seating' } } },
  ],
  alerts: [
    { qrCodeId: 'xw9fwv52ua', location: departuresToilets, kind: 'STRICT_MODE', since: minutesAgo(6), messageCount: 7 },
  ],
};
