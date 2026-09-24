// In development, route through Vite so the browser does not need cross-origin
// access to the Gateway. Vite forwards these requests to localhost:8080.
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? (import.meta.env.DEV ? "" : "http://localhost:8080");
const TOKEN_KEY = "initprep_access_token";

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly responseBody: string,
    public readonly requestUrl: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export function formatApiError(error: unknown, fallback: string): string {
  if (!(error instanceof ApiError)) return fallback;
  if (error.status === 0) return error.message;

  const response = error.responseBody || "<empty response body>";
  return `${error.message}\nHTTP status: ${error.status}\nRequest: ${error.requestUrl}\nResponse: ${response}`;
}

export function saveToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function readToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

export async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const requestUrl = `${API_BASE_URL}${path}`;
  const headers = new Headers(options.headers);
  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const token = readToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);

  let response: Response;
  try {
    response = await fetch(requestUrl, { ...options, headers });
  } catch (cause) {
    const detail = cause instanceof Error ? cause.message : "Unknown network error";
    throw new ApiError(
      `Could not reach the API through ${requestUrl}. Check that the Vite server and API Gateway are running. (${detail})`,
      0,
      "",
      requestUrl,
    );
  }
  if (response.status === 204) return undefined as T;

  const text = await response.text();
  let payload: unknown;
  try {
    payload = text ? JSON.parse(text) : undefined;
  } catch {
    payload = text;
  }

  if (!response.ok) {
    if (response.status === 401) clearToken();
    const message =
      typeof payload === "object" && payload !== null && "message" in payload
        ? String(payload.message)
        : response.status === 401
          ? "Your session has expired. Please sign in again."
          : "The request could not be completed. Please try again.";
    throw new ApiError(message, response.status, text, requestUrl);
  }

  return payload as T;
}
