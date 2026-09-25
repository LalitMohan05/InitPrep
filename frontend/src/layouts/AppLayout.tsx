import { NavLink, Link, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useTheme } from "../context/ThemeContext";

export function AppLayout() {
  const { user, signOut } = useAuth();
  const { theme, toggleTheme } = useTheme();
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
        <div className="topbar-right">{user ? <><span className="user-email">{user.email}</span><button className="quiet-button" onClick={handleSignOut}>Logout</button></> : <Link className="quiet-button link-button" to="/login">Sign in</Link>}<button className="theme-toggle" type="button" onClick={toggleTheme} aria-label={`Switch to ${theme === "dark" ? "light" : "dark"} theme`} aria-pressed={theme === "dark"}>{theme === "dark" ? "☼ Light" : "☾ Dark"}</button></div>
      </header>
      {user && <nav className="main-nav" aria-label="Main navigation">
        <NavLink to="/dashboard">Dashboard</NavLink>
        <NavLink to="/questions">Questions</NavLink>
        {user.role === "ADMIN" && <><NavLink to="/manage-questions">Manage Questions</NavLink><NavLink to="/manage-test-cases">Manage Test Cases</NavLink></>}
        <NavLink to="/attempts">My Attempts</NavLink>
        <NavLink to="/profile">Profile</NavLink>
      </nav>}
      <main className="main-content"><Outlet /></main>
      <footer className="footer">A focused place to prepare for your next interview.</footer>
    </div>
  );
}
