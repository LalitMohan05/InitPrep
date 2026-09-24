import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import * as authApi from "../api/authApi";
import { clearToken, readToken, saveToken } from "../api/client";
import type { User } from "../types/auth";

interface AuthContextValue {
  user: User | null;
  loading: boolean;
  signIn(email: string, password: string): Promise<void>;
  signUp(email: string, password: string): Promise<void>;
  signOut(): void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(Boolean(readToken()));

  const signOut = useCallback(() => {
    clearToken();
    setUser(null);
  }, []);

  useEffect(() => {
    if (!readToken()) return;
    authApi.getCurrentUser()
      .then(setUser)
      .catch(() => signOut())
      .finally(() => setLoading(false));
  }, [signOut]);

  const signIn = useCallback(async (email: string, password: string) => {
    const result = await authApi.login(email, password);
    saveToken(result.accessToken);
    setUser(await authApi.getCurrentUser());
  }, []);

  const signUp = useCallback(async (email: string, password: string) => {
    const result = await authApi.register(email, password);
    saveToken(result.accessToken);
    setUser(await authApi.getCurrentUser());
  }, []);

  const value = useMemo(() => ({ user, loading, signIn, signUp, signOut }), [user, loading, signIn, signUp, signOut]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
