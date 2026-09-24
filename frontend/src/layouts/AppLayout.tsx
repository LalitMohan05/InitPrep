import { Link, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export function AppLayout() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  function handleSignOut() {
    signOut();
    navigate("/login", { replace: true });
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <Link className="brand" to={user ? "/dashboard" : "/login"}>
          <span className="brand-mark">IP</span> InitPrep
        </Link>
        <div className="topbar-right">
          {user ? <><span className="user-email">{user.email}</span><button className="quiet-button" onClick={handleSignOut}>Sign out</button></> : <Link className="quiet-button link-button" to="/login">Sign in</Link>}
        </div>
      </header>
      <main className="main-content"><Outlet /></main>
      <footer className="footer">A focused place to prepare for your next interview.</footer>
    </div>
  );
}
