import type { Category, Severity } from '@/api/types';

/** In the contract's order (contract/openapi.yaml); labels come from the string files. */
export const CATEGORIES: Category[] = [
  'CLEANING_OR_SPILL', 'TOILET', 'LIFT_OR_ESCALATOR', 'BROKEN_OR_MAINTENANCE', 'SIGN_OR_INFORMATION',
  'ASSISTANCE_NEEDED', 'LOST_ITEM_OR_PERSON', 'SAFETY_OR_SECURITY', 'OTHER',
];

export const SEVERITIES: Severity[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
