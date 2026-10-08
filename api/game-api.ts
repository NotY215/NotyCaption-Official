export type Difficulty = 'easy' | 'normal' | 'hard';

export interface DifficultyProgress {
  easy: number;
  normal: number;
  hard: number;
}

export interface PlayerProfile {
  email: string;
  username: string;
  stage: DifficultyProgress;
  score: DifficultyProgress;
}

export interface LeaderboardEntry {
  rank: number;
  username: string;
  score: number;
  stage: number;
}

export interface ScoreSubmission {
  difficulty: Difficulty;
  score: number;
  stage: number;
}

export const GAME_API = {
  profile: '/api/profile.php',
  score: '/api/score.php',
  leaderboard: '/api/leaderboard.php'
} as const;
