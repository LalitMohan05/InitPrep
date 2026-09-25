// Development uses Vite's /api proxy by default. Production must be configured
// with the public HTTPS Gateway URL through VITE_API_URL.
const API_BASE_URL = (import.meta.env.VITE_API_URL ?? "").replace(/\/+$/, "");
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
  if (!import.meta.env.DEV && !API_BASE_URL) {
    throw new ApiError(
      "VITE_API_URL is not configured. Set it to the public HTTPS URL of the InitPrep API Gateway in the deployment environment.",
      0,
      "",
      path,
    );
  }
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
    const body = typeof payload === "object" && payload !== null ? payload as Record<string, unknown> : null;
    const backendMessage = body && ("message" in body || "detail" in body || "error" in body)
      ? String(body.message ?? body.detail ?? body.error)
      : body && "errors" in body
        ? `Backend validation errors: ${JSON.stringify(body.errors)}`
        : undefined;
    const message =
      backendMessage ?? (response.status === 400
        ? "The backend rejected the request. Check the response details below."
        : response.status === 401
          ? "Your session has expired. Please sign in again."
          : response.status === 403
            ? "The authenticated request was denied by an authorization rule."
            : response.status === 404
              ? "The requested resource was not found."
              : response.status >= 500
                ? "The backend could not complete the request. See its response below."
                : "The request could not be completed. See the response below.");
    throw new ApiError(message, response.status, text, requestUrl);
  }

  return payload as T;
}
