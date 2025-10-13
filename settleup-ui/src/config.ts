
/**
 * All API calls go through these prefixes so dev proxy (Vite) and prod (Nginx) can route correctly.
 * Do not include trailing slashes.
 */
export const MEMBERSHIP_BASE = '/api/membership';
export const EXPENSE_BASE = '/api/expense';
export const SETTLEMENT_BASE = '/api/settlement';
