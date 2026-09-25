import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import * as mockInterviewApi from "../api/mockInterviewApi";
import type { InterviewDifficulty } from "../api/mockInterviewApi";
import * as profileApi from "../api/profileApi";
import type { TargetRole } from "../api/profileApi";
import { formatApiError } from "../api/client";

const roles: { value: TargetRole; label: string }[] = [
  { value: "BACKEND_DEVELOPER", label: "Backend Developer" },
  { value: "FRONTEND_DEVELOPER", label: "Frontend Developer" },
  { value: "FULLSTACK_DEVELOPER", label: "Full Stack Developer" },
  { value: "DEVOPS_ENGINEER", label: "DevOps Engineer" },
  { value: "MACHINE_LEARNING_ENGINEER", label: "Machine Learning Engineer" },
];
const dateLabel = (value: string) => {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Date unavailable" : date.toLocaleString();
};
const roleLabel = (value: string) => roles.find(role => role.value === value)?.label ?? value.replace(/_/g, " ");

export function MockInterviewStartPage() {
  const navigate = useNavigate();
  const [targetRole, setTargetRole] = useState<TargetRole>("BACKEND_DEVELOPER");
  const [difficulty, setDifficulty] = useState<InterviewDifficulty>("MIXED");
  const [numberOfQuestions, setNumberOfQuestions] = useState(10);
  const [history, setHistory] = useState<mockInterviewApi.MockInterviewSession[]>([]);
  const [historyLoading, setHistoryLoading] = useState(true);
  const [historyError, setHistoryError] = useState("");
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    profileApi.getProfile().then(profile => {
      if (active && profile.targetRole) setTargetRole(profile.targetRole);
    }).catch(() => undefined);
    mockInterviewApi.getHistory().then(page => {
      if (active) setHistory(page.content);
    }).catch(cause => {
      if (active) setHistoryError(formatApiError(cause, "Could not load mock interview history."));
    }).finally(() => { if (active) setHistoryLoading(false); });
    return () => { active = false; };
  }, []);

  async function startInterview() {
    setStarting(true);
    setError("");
    try {
      const session = await mockInterviewApi.startInterview(targetRole, difficulty, numberOfQuestions);
      navigate(`/mock-interview/${session.id}`);
    } catch (cause) {
      setError(cause instanceof Error && !(cause instanceof TypeError) ? cause.message : formatApiError(cause, "Could not start the mock interview."));
    } finally {
      setStarting(false);
    }
  }

  return <section className="content-page mock-interview-page">
    <header className="page-heading"><div><p className="eyebrow">INTERVIEW PRACTICE</p><h1>Mock Interview</h1><p className="page-subtitle">Practice a structured technical interview using questions from your library.</p></div></header>
    <section className="mock-start-panel">
      <div className="mock-panel-heading"><div><h2>Set up your interview</h2><p>Questions are selected in a fixed mix of coding, theory, and multiple choice.</p></div><span className="technical-tag">Technical</span></div>
      <div className="mock-start-fields">
        <label>Target role<select value={targetRole} onChange={event => setTargetRole(event.target.value as TargetRole)}>{roles.map(role => <option key={role.value} value={role.value}>{role.label}</option>)}</select></label>
        <label>Difficulty<select value={difficulty} onChange={event => setDifficulty(event.target.value as InterviewDifficulty)}><option value="EASY">Easy</option><option value="MEDIUM">Medium</option><option value="HARD">Hard</option><option value="MIXED">Mixed</option></select></label>
        <label>Number of questions<select value={numberOfQuestions} onChange={event => setNumberOfQuestions(Number(event.target.value))}><option value={5}>5 questions</option><option value={10}>10 questions</option><option value={15}>15 questions</option></select></label>
      </div>
      {error && <div className="notice-panel error-panel" role="alert">{error}</div>}
      <div className="mock-start-actions"><span>Interview type <strong>Technical</strong> · 40% coding · 30% theory · 30% MCQ</span><button className="primary-button" type="button" disabled={starting} onClick={() => void startInterview()}>{starting ? "Preparing questions…" : "Start Interview"}</button></div>
    </section>

    <section className="mock-history-section">
      <div className="mock-section-heading"><div><h2>Interview history</h2><p>Continue an active session or review a completed report.</p></div></div>
      {historyError && <div className="notice-panel error-panel" role="alert">{historyError}</div>}
      {historyLoading ? <p className="page-state">Loading interview history…</p> : history.length === 0 ? <div className="empty-state"><p>No mock interviews yet. Your completed sessions will appear here.</p></div> : <div className="mock-history-list">
        {history.map(session => <Link className="mock-history-row" key={session.id} to={`/mock-interview/${session.id}`}>
          <span><strong>{roleLabel(session.targetRole)}</strong><small>{dateLabel(session.startedAt)}</small></span>
          <span>{session.difficulty} · {session.totalQuestions} questions</span>
          <span className={`attempt-status status-${session.status.toLowerCase().replace(/_/g, "-")}`}>{session.status === "COMPLETED" ? `${session.overallScore == null ? "" : `${Math.round(session.overallScore)}% · `}Complete` : "In progress"}</span>
        </Link>)}
      </div>}
    </section>
  </section>;
}
