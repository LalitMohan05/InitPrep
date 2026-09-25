import { apiRequest } from "./client";
import type { AttemptResponse, CodingFeedback, JudgeSubmissionResponse, QuestionJudgeData, RunCodeRequest, ProgrammingLanguage } from "../types/coding";

export function getPublicTestCases(questionId: string): Promise<QuestionJudgeData> {
  return apiRequest(`/api/questions/${questionId}/public-test-cases`);
}

export function getAiFeedback(attemptId: string): Promise<CodingFeedback> {
  return apiRequest(`/api/attempts/${attemptId}/ai-feedback`);
}

export function runCode(request: RunCodeRequest): Promise<JudgeSubmissionResponse> {
  return apiRequest("/api/attempts/run", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function submitCode(questionId: string, answer: string, language: ProgrammingLanguage): Promise<AttemptResponse> {
  return apiRequest("/api/attempts", {
    method: "POST",
    body: JSON.stringify({ questionId, answer, language, type: "CODING" }),
  });
}
