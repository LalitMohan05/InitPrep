import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import * as attemptsApi from "../api/attemptsApi";
import * as mockInterviewApi from "../api/mockInterviewApi";
import * as profileApi from "../api/profileApi";
import { getQuestionDetails, getQuestions } from "../api/questionsApi";
import { useAuth } from "../context/AuthContext";
import { formatApiError } from "../api/client";
import type { AttemptResponse } from "../types/coding";
import type { QuestionType } from "../types/questions";
import type { MockInterviewSession } from "../api/mockInterviewApi";
import type { UserProfile } from "../api/profileApi";

type Activity = {
  key: string;
  date: string;
  title: string;
  detail: string;
  score: number | null;
  href: string;
  kind: "Coding attempt" | "Mock interview";
};

interface DashboardData {
  attempts: AttemptResponse[];
  attemptCount: number;
  interviews: MockInterviewSession[];
  interviewCount: number;
  questionCounts: Partial<Record<QuestionType, number>>;
  profile: UserProfile | null;
  activity: Activity[];
}

const questionTypes: QuestionType[] = ["CODING", "MCQ", "THEORY"];
const typeLabels: Record<QuestionType, string> = { CODING: "Coding", MCQ: "MCQ", THEORY: "Theory" };
const formatDate = (value: string) => {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Date unavailable" : date.toLocaleDateString(undefined, { month: "short", day: "numeric" });
};
const resultLabel = (attempt: AttemptResponse) => (attempt.result || attempt.status || "Submitted").replace(/_/g, " ");
const scoreLabel = (score: number | null | undefined) => score == null ? "—" : `${Math.round(score)}%`;

export function DashboardPage() {
  const { user } = useAuth();
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");

    async function loadDashboard() {
      const [attemptResult, interviewResult, profileResult, ...questionResults] = await Promise.allSettled([
        attemptsApi.getAttempts(0, 5),
        mockInterviewApi.getHistory(0, 5),
        profileApi.getProfile(),
        ...questionTypes.map(type => getQuestions({ type, page: 0, size: 1 })),
      ]);

      const attemptsPage = attemptResult.status === "fulfilled" ? attemptResult.value : null;
      const interviewPage = interviewResult.status === "fulfilled" ? interviewResult.value : null;
      const profile = profileResult.status === "fulfilled" ? profileResult.value : null;
      const questionCounts: Partial<Record<QuestionType, number>> = {};
      questionResults.forEach((result, index) => {
        if (result.status === "fulfilled") questionCounts[questionTypes[index]] = result.value.totalElements;
      });

      if (!attemptsPage && !interviewPage) {
        throw attemptResult.status === "rejected" ? attemptResult.reason : interviewResult.status === "rejected" ? interviewResult.reason : new Error("Dashboard data is unavailable.");
      }

      const attempts = attemptsPage?.content ?? [];
      const titlePairs = await Promise.all(attempts.map(async attempt => {
        try {
          const question = await getQuestionDetails(attempt.questionId);
          return [attempt.questionId, question.title] as const;
        } catch {
          return [attempt.questionId, "Coding practice"] as const;
        }
      }));
      const titles = Object.fromEntries(titlePairs);
      const activity: Activity[] = [
        ...attempts.map(attempt => ({
          key: `attempt-${attempt.id}`,
          date: attempt.createdAt,
          title: titles[attempt.questionId] ?? "Coding practice",
          detail: resultLabel(attempt),
          score: attempt.score,
          href: `/my-attempts/${attempt.id}`,
          kind: "Coding attempt" as const,
        })),
        ...(interviewPage?.content ?? []).map(session => ({
          key: `interview-${session.id}`,
          date: session.completedAt ?? session.startedAt,
          title: `${session.targetRole.replace(/_/g, " ")} interview`,
          detail: session.status.replace(/_/g, " "),
          score: session.overallScore,
          href: `/mock-interview/${session.id}`,
          kind: "Mock interview" as const,
        })),
      ].sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()).slice(0, 6);

      if (active) setData({
        attempts,
        attemptCount: attemptsPage?.totalElements ?? 0,
        interviews: interviewPage?.content ?? [],
        interviewCount: interviewPage?.totalElements ?? 0,
        questionCounts,
        profile,
        activity,
      });
    }

    loadDashboard()
      .catch(cause => { if (active) setError(formatApiError(cause, "Could not load your dashboard.")); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [reload]);

  const recentScoredAttempts = data?.attempts.filter(attempt => attempt.score != null) ?? [];
  const recentAverage = recentScoredAttempts.length
    ? recentScoredAttempts.reduce((sum, attempt) => sum + (attempt.score ?? 0), 0) / recentScoredAttempts.length
    : null;
  const displayName = data?.profile?.fullName?.trim() || user?.email?.split("@")[0] || "there";
  const totalQuestions = questionTypes.reduce((sum, type) => sum + (data?.questionCounts[type] ?? 0), 0);
  const roleLabel = data?.profile?.targetRole?.replace(/_/g, " ") ?? "Choose a target role";

  return <section className="content-page dashboard-page">
    <header className="dashboard-welcome">
      <div>
        <p className="eyebrow">YOUR PREPARATION</p>
        <h1>Welcome back, {displayName}</h1>
        <p className="page-subtitle">Build momentum with focused practice, mock interviews, and a clear view of your progress.</p>
      </div>
      <Link className="primary-button dashboard-start" to="/mock-interview">Start a mock interview</Link>
    </header>

    {error && <div className="notice-panel error-panel dashboard-error" role="alert">{error}<button className="text-button" onClick={() => setReload(value => value + 1)}>Try again</button></div>}
    {loading && <p className="page-state">Loading your preparation overview…</p>}

    {!loading && data && <>
      <div className="dashboard-stats" aria-label="Preparation summary">
        <article className="dashboard-stat"><span>Coding attempts</span><strong>{data.attemptCount}</strong><small>{recentAverage == null ? "No scored submissions yet" : `${Math.round(recentAverage)}% recent average`}</small></article>
        <article className="dashboard-stat"><span>Mock interviews</span><strong>{data.interviewCount}</strong><small>{data.interviews.filter(session => session.status === "IN_PROGRESS").length} in progress</small></article>
        <article className="dashboard-stat"><span>Questions available</span><strong>{totalQuestions}</strong><small>{questionTypes.map(type => `${data.questionCounts[type] ?? "—"} ${typeLabels[type]}`).join(" · ")}</small></article>
        <article className="dashboard-stat"><span>Target role</span><strong className="dashboard-role">{roleLabel}</strong><small>{data.profile?.preferredLanguage ? `Preferred language: ${data.profile.preferredLanguage}` : "Set your preferences in Profile"}</small></article>
      </div>

      <div className="dashboard-main-grid">
        <section className="dashboard-section dashboard-activity">
          <header className="dashboard-section-heading"><div><h2>Recent activity</h2><p>Your latest coding submissions and interview sessions.</p></div><Link to="/my-attempts">View attempts</Link></header>
          {data.activity.length === 0 ? <div className="dashboard-empty"><p>Your activity will appear here after your first practice session.</p><Link to="/questions">Browse questions</Link></div> : <div className="dashboard-activity-list">
            {data.activity.map(item => <Link className="dashboard-activity-row" to={item.href} key={item.key}>
              <span className={`dashboard-activity-mark ${item.kind === "Mock interview" ? "interview-mark" : ""}`} aria-hidden="true">{item.kind === "Mock interview" ? "M" : "C"}</span>
              <span className="dashboard-activity-copy"><strong>{item.title}</strong><small>{item.kind} · {item.detail}</small></span>
              <span className="dashboard-activity-result"><strong>{scoreLabel(item.score)}</strong><small>{formatDate(item.date)}</small></span>
            </Link>)}
          </div>}
        </section>

        <aside className="dashboard-side-column">
          <section className="dashboard-section dashboard-focus">
            <p className="eyebrow">SUGGESTED NEXT STEP</p>
            <h2>{data.attemptCount + data.interviewCount === 0 ? "Set your baseline" : "Practice with intention"}</h2>
            <p>{data.attemptCount + data.interviewCount === 0 ? "Start a mock interview to get an initial measure of your readiness." : "Keep your routine balanced: solve a problem, then rehearse explaining your approach."}</p>
            <Link className="secondary-button" to="/mock-interview">{data.interviewCount ? "Continue interview practice" : "Start mock interview"}</Link>
          </section>
          <section className="dashboard-section dashboard-shortcuts">
            <h2>Quick access</h2>
            <Link to="/questions"><span><strong>Question bank</strong><small>Browse coding, theory, and MCQ</small></span><b aria-hidden="true">→</b></Link>
            <Link to="/my-attempts"><span><strong>My attempts</strong><small>Review code and judge results</small></span><b aria-hidden="true">→</b></Link>
            <Link to="/mock-interview"><span><strong>Mock interview</strong><small>Practice for {roleLabel}</small></span><b aria-hidden="true">→</b></Link>
            <Link to="/profile"><span><strong>Profile & preferences</strong><small>Personalize your preparation</small></span><b aria-hidden="true">→</b></Link>
          </section>
        </aside>
      </div>
    </>}
  </section>;
}
