export type UserRole = "USER" | "ADMIN";

export interface User {
  id: string;
  email: string;
  role: UserRole;
  enabled: boolean;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
}

export interface RegisterResponse extends TokenResponse {
  message: string;
}
