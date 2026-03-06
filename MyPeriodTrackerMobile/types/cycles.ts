export type Prediction = {
  predictedStartDate: string;
  confidence: 'HIGH' | 'MEDIUM' | 'LOW';
  reasons: string[];
};

export type CycleStats = {
  averageCycleLength: number;
  shortestCycle: number;
  longestCycle: number;
  standardDeviation: number;
};

export type CycleHistoryResponse = {
  totalCycles: number;
  cycles: { startDate: string }[];
  stats: CycleStats | null;
};