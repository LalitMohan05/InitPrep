import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { formatApiError } from "../api/client";
import { useAuth } from "../context/AuthContext";

export function RegisterPage() {
  const { signUp } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSubmitting(true);
    try { await signUp(email, password); navigate("/dashboard", { replace: true }); }
    catch (cause) { setError(formatApiError(cause, "Unable to create your account. Please try again.")); }
    finally { setSubmitting(false); }
  }

  return <section className="auth-panel">
    <p className="eyebrow">GET STARTED</p>
    <h1>Create your account</h1>
    <p className="form-intro">Use your account to keep your preparation in one place.</p>
    <form onSubmit={handleSubmit} className="auth-form">
      <label>Email address<input type="email" autoComplete="email" value={email} onChange={event => setEmail(event.target.value)} required /></label>
      <label>Password<input type="password" autoComplete="new-password" value={password} onChange={event => setPassword(event.target.value)} minLength={8} maxLength={32} required /><span className="field-hint">8–32 characters, with uppercase, lowercase, number, and symbol.</span></label>
      {error && <pre className="error-message" role="alert">{error}</pre>}
      <button className="primary-button" type="submit" disabled={submitting}>{submitting ? "Creating account…" : "Create account"}</button>
    </form>
    <p className="form-switch">Already registered? <Link to="/login">Sign in</Link></p>
  </section>;
}
