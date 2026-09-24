import { apiRequest } from "./client";
import type { RegisterResponse, TokenResponse, User } from "../types/auth";

export function login(email: string, password: string): Promise<TokenResponse> {
  return apiRequest("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}

export function register(email: string, password: string): Promise<RegisterResponse> {
  return apiRequest("/api/auth/register", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}

export function getCurrentUser(): Promise<User> {
  return apiRequest("/api/auth/me");
}
