import type { Category, IssueCard, TeamId } from '@/api/types';
import type { IconName } from '@/components/Icon';

/**
 * Location codes are AREA-TYPE-NN (docs/design/components.md, data/airport/airport.json), so the
 * middle part says what kind of place it is. Unknown types fall back to the Category icon.
 */
const placeIcons: Record<string, IconName> = {
  WC: 'toilet',
  BELT: 'luggage',
  LIFT: 'lift',
  ESC: 'escalator',
  INFO: 'info',
  SEAT: 'seat',
  ENT: 'door',
  FAM: 'family',
};

const categoryIcons: Record<Category, IconName> = {
  CLEANING_OR_SPILL: 'droplet',
  TOILET: 'toilet',
  LIFT_OR_ESCALATOR: 'lift',
  BROKEN_OR_MAINTENANCE: 'wrench',
  SIGN_OR_INFORMATION: 'info',
  ASSISTANCE_NEEDED: 'accessibility',
  LOST_ITEM_OR_PERSON: 'search',
  SAFETY_OR_SECURITY: 'shield',
  OTHER: 'map-pin',
};

export const teamIcons: Record<TeamId, IconName> = {
  CLEANING: 'droplet',
  MAINTENANCE: 'wrench',
  SECURITY: 'shield',
  PASSENGER_ASSISTANCE: 'accessibility',
};

/**
 * The icon on an Issue card shows the place first, because the place comes from the QR code the
 * Passenger scanned, while the Category is the LLM's guess and can be wrong. The security checkpoint
 * entrance gets the shield rather than a door.
 */
export function issueIcon(issue: Pick<IssueCard, 'location' | 'category'>): IconName {
  const [area, type] = issue.location?.label.split('-') ?? [];
  if (area === 'SEC' && type === 'ENT') return 'shield';
  return (type && placeIcons[type]) || categoryIcons[issue.category];
}
