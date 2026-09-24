import { FormEvent, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { formatApiError } from "../api/client";
import { useAuth } from "../context/AuthContext";

export function LoginPage() {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const destination = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname ?? "/dashboard";
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSubmitting(true);
    try { await signIn(email, password); navigate(destination, { replace: true }); }
    catch (cause) { setError(formatApiError(cause, "Unable to sign in. Please try again.")); }
    finally { setSubmitting(false); }
  }

  return <section className="auth-panel">
    <p className="eyebrow">WELCOME BACK</p>
    <h1>Sign in to InitPrep</h1>
    <p className="form-intro">Continue building your interview confidence.</p>
    <form onSubmit={handleSubmit} className="auth-form">
      <label>Email address<input type="email" autoComplete="email" value={email} onChange={event => setEmail(event.target.value)} required /></label>
      <label>Password<input type="password" autoComplete="current-password" value={password} onChange={event => setPassword(event.target.value)} required /></label>
      {error && <pre className="error-message" role="alert">{error}</pre>}
      <button className="primary-button" type="submit" disabled={submitting}>{submitting ? "Signing in…" : "Sign in"}</button>
    </form>
    <p className="form-switch">New to InitPrep? <Link to="/register">Create an account</Link></p>
  </section>;
}
