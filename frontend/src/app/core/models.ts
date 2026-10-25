/** Shapes of the backend's /api/v1 responses (backend `com.techquiz.*` records). */

export interface Topic {
  readonly id: string;
  readonly name: string;
  readonly description: string;
  readonly questionCount: number;
}

export interface CodeSnippet {
  readonly language: string;
  readonly source: string;
}

export interface QuizOption {
  readonly id: string;
  readonly text: string;
}

export interface QuizQuestion {
  readonly id: string;
  readonly topicId: string;
  readonly prompt: string;
  readonly code: CodeSnippet | null;
  readonly options: readonly QuizOption[];
}

export interface Quiz {
  readonly questions: readonly QuizQuestion[];
}

/** One of three ways to build a quiz (backend `QuizRequest`). */
export interface QuizRequest {
  readonly topicIds?: readonly string[];
  readonly size?: number;
  readonly questionIds?: readonly string[];
}

export interface AnswerResult {
  readonly correct: boolean;
  readonly correctOptionId: string;
  readonly explanation: string;
  readonly references: readonly string[];
}

export type QuizMode = 'quick' | 'custom' | 'retry';

export const QUIZ_SIZES = [10, 20, 30] as const;
