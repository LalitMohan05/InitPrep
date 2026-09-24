import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export function PlaceholderPage({ title }: { title: string }) {
  const { user } = useAuth();
  if (title !== "Dashboard") return <section className="content-page"><p className="eyebrow">INITPREP</p><h1>{title}</h1><p className="page-subtitle">This section will be implemented in a later step.</p></section>;
  return <section className="content-page">
    <p className="eyebrow">INITPREP</p><h1>Dashboard</h1><p className="page-subtitle">Pick up your interview preparation where you need it.</p>
    <div className="dashboard-links">
      <Link to="/questions"><strong>Browse questions</strong><span>Explore practice problems by topic and difficulty.</span></Link>
      <Link to="/attempts"><strong>My attempts</strong><span>Review your previous practice submissions.</span></Link>
      {user?.role === "ADMIN" && <><Link to="/manage-questions"><strong>Manage questions</strong><span>Create, edit, and remove questions.</span></Link><Link to="/manage-test-cases"><strong>Manage test cases</strong><span>Review test data for coding questions.</span></Link></>}
      <Link to="/profile"><strong>Profile</strong><span>View and update your account profile.</span></Link>
    </div>
  </section>;
}
