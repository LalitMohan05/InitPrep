import { apiRequest } from "./client";
import type {
  QuestionDetails,
  QuestionPage,
  QuestionWriteRequest,
  TestCase,
  TestCaseWriteRequest,
} from "../types/questions";

export interface QuestionFilters {
  difficulty?: string;
  type?: string;
  company?: string;
  topic?: string;
  page: number;
  size: number;
}

export function getQuestions(filters: QuestionFilters): Promise<QuestionPage> {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(filters)) {
    if (value !== undefined && value !== "") params.set(key, String(value));
  }
  return apiRequest(`/api/questions?${params.toString()}`);
}

export function getQuestionDetails(id: string): Promise<QuestionDetails> {
  return apiRequest(`/api/questions/${id}/details`);
}

export function createQuestion(request: QuestionWriteRequest): Promise<unknown> {
  return apiRequest("/api/questions", { method: "POST", body: JSON.stringify(request) });
}

export function updateQuestion(id: string, request: Partial<QuestionWriteRequest>): Promise<unknown> {
  return apiRequest(`/api/questions/${id}`, { method: "PATCH", body: JSON.stringify(request) });
}

export function deleteQuestion(id: string): Promise<void> {
  return apiRequest(`/api/questions/${id}`, { method: "DELETE" });
}

export function getTestCases(questionId: string): Promise<TestCase[]> {
  return apiRequest(`/api/questions/${questionId}/test-cases`);
}

export function createTestCase(questionId: string, request: TestCaseWriteRequest): Promise<TestCase> {
  return apiRequest(`/api/questions/${questionId}/test-cases`, { method: "POST", body: JSON.stringify(request) });
}

export function updateTestCase(questionId: string, testCaseId: string, request: TestCaseWriteRequest): Promise<TestCase> {
  return apiRequest(`/api/questions/${questionId}/test-cases/${testCaseId}`, { method: "PATCH", body: JSON.stringify(request) });
}

export function deleteTestCase(questionId: string, testCaseId: string): Promise<void> {
  return apiRequest(`/api/questions/${questionId}/test-cases/${testCaseId}`, { method: "DELETE" });
}

export function createTestCases(questionId: string, testCases: TestCaseWriteRequest[]): Promise<TestCase[]> {
  return apiRequest(`/api/questions/${questionId}/test-cases/bulk`, {
    method: "POST",
    body: JSON.stringify({ testCases }),
  });
}
