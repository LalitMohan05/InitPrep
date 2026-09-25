import { apiRequest } from "./client";
import type { AttemptResponse } from "../types/coding";
import type { Difficulty, QuestionType } from "../types/questions";

export type InterviewDifficulty = Difficulty | "MIXED";
export type InterviewStatus = "IN_PROGRESS" | "COMPLETED" | "ABANDONED";

export interface InterviewQuestion {
  questionId: string;
  questionType: QuestionType;
  difficulty: Difficulty;
  sequenceNumber: number;
  questionTitleSnapshot: string;
  topicSnapshot: string;
  roleSnapshot: string;
  question: {
    id: string;
    title: string;
    description: string;
    difficulty: Difficulty;
    type: QuestionType;
    constraints: string | null;
    examples: string | null;
    starterCode: string | null;
    options: string | null;
    roles: string[];
  };
  attempt: AttemptResponse | null;
  theoryEvaluation: TheoryEvaluation | null;
}

export interface TheoryEvaluation {
  score: number;
  strengths: string | null;
  weaknesses: string | null;
  feedback: string | null;
  recommendedTopics: string[] | null;
}

export interface MockInterviewSession {
  id: string;
  targetRole: string;
  difficulty: InterviewDifficulty;
  totalQuestions: number;
  status: InterviewStatus;
  startedAt: string;
  completedAt: string | null;
  overallScore: number | null;
  attemptedQuestions: number;
  correctQuestions: number;
  codingScore: number | null;
  mcqScore: number | null;
  theoryScore: number | null;
  codingAccepted: number;
  codingAttempted: number;
  strengths: string[];
  weaknesses: string[];
  recommendedTopics: string[];
  questions: InterviewQuestion[] | null;
}

export interface MockInterviewPage {
  content: MockInterviewSession[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export function getHistory(page = 0, size = 10): Promise<MockInterviewPage> {
  return apiRequest(`/api/attempts/mock-interviews?page=${page}&size=${size}&sort=startedAt,desc`);
}

export function startInterview(targetRole: string, difficulty: InterviewDifficulty, totalQuestions: number): Promise<MockInterviewSession> {
  return apiRequest("/api/attempts/mock-interviews", {
    method: "POST",
    body: JSON.stringify({ targetRole, difficulty, totalQuestions }),
  });
}

export function getInterview(sessionId: string): Promise<MockInterviewSession> {
  return apiRequest(`/api/attempts/mock-interviews/${sessionId}`);
}

export function getInterviewReport(sessionId: string): Promise<MockInterviewSession> {
  return apiRequest(`/api/attempts/mock-interviews/${sessionId}/report`);
}

export function completeInterview(sessionId: string): Promise<MockInterviewSession> {
  return apiRequest(`/api/attempts/mock-interviews/${sessionId}/complete`, { method: "POST" });
}

export function submitInterviewAnswer(sessionId: string, questionId: string, answer: string, language?: string): Promise<MockInterviewSession> {
  return apiRequest(`/api/attempts/mock-interviews/${sessionId}/answers`, {
    method: "POST",
    body: JSON.stringify({ questionId, answer, language }),
  });
}
