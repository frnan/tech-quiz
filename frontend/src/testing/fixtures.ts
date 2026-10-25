import { Mock } from 'vitest';

import { AnswerResult, QuizQuestion, Topic } from '../app/core/models';
import { QuizApiService } from '../app/core/quiz-api.service';

export const TOPICS: Topic[] = [
  { id: 'java', name: 'Java', description: 'The language', questionCount: 10 },
  { id: 'docker', name: 'Docker', description: 'Containers', questionCount: 4 },
];

/** A question whose options are `<id>-a` (correct), `<id>-b` and `<id>-c`. */
export function question(id: string, topicId = 'java', withCode = false): QuizQuestion {
  return {
    id,
    topicId,
    prompt: `Prompt of ${id} with \`code\``,
    code: withCode ? { language: 'java', source: 'int x = 1;' } : null,
    options: ['a', 'b', 'c'].map((letter) => ({ id: `${id}-${letter}`, text: `Option ${letter}` })),
  };
}

export function result(questionId: string, chosen: string): AnswerResult {
  const correctOptionId = `${questionId}-a`;
  return {
    correct: chosen === correctOptionId,
    correctOptionId,
    explanation: `Explanation of ${questionId}`,
    references: ['https://example.com/ref'],
  };
}

export type ApiMock = { [K in keyof QuizApiService]: Mock<QuizApiService[K]> };

/** An API whose answer() grades like {@link result}. */
export function apiMock(): ApiMock {
  return {
    topics: vi.fn<QuizApiService['topics']>().mockResolvedValue(TOPICS),
    createQuiz: vi.fn<QuizApiService['createQuiz']>(),
    answer: vi
      .fn<QuizApiService['answer']>()
      .mockImplementation(async (questionId, optionId) => result(questionId, optionId)),
  };
}

/** Lets pending promises and signal effects run. */
export async function settle(): Promise<void> {
  for (let i = 0; i < 5; i++) {
    await Promise.resolve();
  }
}
