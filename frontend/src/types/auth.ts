export interface User {
  id: string;
  email: string;
  role: string;
  enabled: boolean;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
}

export interface RegisterResponse extends TokenResponse {
  message: string;
}
