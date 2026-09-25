import { apiRequest } from "./client";
import type { AttemptPage, AttemptResponse } from "../types/coding";

export function getAttempts(page: number, size = 10): Promise<AttemptPage> {
  const params = new URLSearchParams({ page: String(page), size: String(size), sort: "createdAt,desc" });
  return apiRequest(`/api/attempts?${params.toString()}`);
}

export function getAttempt(attemptId: string): Promise<AttemptResponse> {
  return apiRequest(`/api/attempts/${attemptId}`);
}
