/**
 * Returns how many days into the current cycle we are,
 * given the most recent period start date string (YYYY-MM-DD).
 */
export function computeCycleDay(startDate: string): number {
  const start = new Date(startDate);
  const today = new Date();
  // Zero out time portion so we count whole days only
  start.setHours(0, 0, 0, 0);
  today.setHours(0, 0, 0, 0);
  const diffMs = today.getTime() - start.getTime();
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
  return diffDays + 1; // Day 1 = the start date itself
}

/**
 * Returns a human-readable string for how many days until
 * a future date string (YYYY-MM-DD), e.g. "in 5 days", "today", "2 days ago".
 */
export function daysUntil(dateString: string): string {
  const target = new Date(dateString);
  const today = new Date();
  target.setHours(0, 0, 0, 0);
  today.setHours(0, 0, 0, 0);
  const diffMs = target.getTime() - today.getTime();
  const diffDays = Math.round(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays === 0) return 'today';
  if (diffDays === 1) return 'in 1 day';
  if (diffDays > 1) return `in ${diffDays} days`;
  if (diffDays === -1) return '1 day ago';
  return `${Math.abs(diffDays)} days ago`;
}

/**
 * Returns a background color for a confidence badge.
 */
export function confidenceColor(confidence: 'HIGH' | 'MEDIUM' | 'LOW'): string {
  switch (confidence) {
    case 'HIGH':   return '#22C55E';
    case 'MEDIUM': return '#F59E0B';
    case 'LOW':    return '#EF4444';
    default:       return '#94A3B8'; 
  }
}