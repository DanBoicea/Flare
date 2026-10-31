// SIMULATED DEMO DATA. The same four personas as the flare-dev seed (data/airport/airport.json),
// for mock mode only. Task D05 can extend these fixtures.
import type { Me, Team } from '@/api/types';

export const fixtureStaff: Me[] = [
  { id: '00000000-0000-4000-8000-000000000001', displayName: 'Ana (simulat)', roles: ['STAFF'], teams: ['CLEANING'] },
  { id: '00000000-0000-4000-8000-000000000002', displayName: 'Mihai (simulat)', roles: ['STAFF'], teams: ['MAINTENANCE', 'PASSENGER_ASSISTANCE'] },
  { id: '00000000-0000-4000-8000-000000000003', displayName: 'Ioana (simulat)', roles: ['STAFF'], teams: ['SECURITY'] },
  { id: '00000000-0000-4000-8000-000000000004', displayName: 'Radu (simulat)', roles: ['DUTY_MANAGER'], teams: [] },
];

export const fixtureTeams: Team[] = [
  { id: 'CLEANING', name: { ro: 'Curățenie', en: 'Cleaning' } },
  { id: 'MAINTENANCE', name: { ro: 'Mentenanță', en: 'Maintenance' } },
  { id: 'SECURITY', name: { ro: 'Securitate', en: 'Security' } },
  { id: 'PASSENGER_ASSISTANCE', name: { ro: 'Asistență pasageri', en: 'Passenger assistance' } },
];
