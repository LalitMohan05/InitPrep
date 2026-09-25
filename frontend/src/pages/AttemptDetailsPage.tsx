import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import * as attemptsApi from "../api/attemptsApi";
import { getAiFeedback } from "../api/codingApi";
import { formatApiError } from "../api/client";
import { getQuestionDetails } from "../api/questionsApi";
import { MonacoCodeEditor } from "../components/MonacoCodeEditor";
import type { AttemptResponse, CodingFeedback } from "../types/coding";

const readOnly = () => undefined;
const prettyStatus = (value?: string | null) => value?.replace(/_/g, " ") || "Result unavailable";
const formatDate = (value: string) => {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Date unavailable" : date.toLocaleString();
};
const editorLanguage: Record<string, string> = { JAVA: "java", PYTHON: "python", CPP: "cpp", C: "c", JAVASCRIPT: "javascript" };

export function AttemptDetailsPage() {
  const { attemptId = "" } = useParams();
  const [attempt, setAttempt] = useState<AttemptResponse | null>(null);
  const [questionTitle, setQuestionTitle] = useState("Question");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [feedback, setFeedback] = useState<CodingFeedback | null>(null);
  const [feedbackLoading, setFeedbackLoading] = useState(false);
  const [feedbackError, setFeedbackError] = useState("");

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    setAttempt(null);
    setFeedback(null);
    attemptsApi.getAttempt(attemptId)
      .then(async result => {
        if (!active) return;
        setAttempt(result);
        try {
          const question = await getQuestionDetails(result.questionId);
          if (active) setQuestionTitle(question.title);
        } catch {
          if (active) setQuestionTitle("Question unavailable");
        }
      })
      .catch(cause => { if (active) setError(formatApiError(cause, "Could not load this attempt.")); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [attemptId]);

  async function loadFeedback() {
    if (!attempt) return;
    setFeedbackLoading(true);
    setFeedbackError("");
    try { setFeedback(await getAiFeedback(attempt.id)); }
    catch (cause) { setFeedbackError(formatApiError(cause, "Could not load AI feedback.")); }
    finally { setFeedbackLoading(false); }
  }

  if (loading) return <section className="content-page"><p className="page-state">Loading attempt…</p></section>;
  if (error || !attempt) return <section className="content-page"><div className="notice-panel error-panel" role="alert">{error || "Attempt not found."}</div><Link to="/my-attempts">← Back to attempts</Link></section>;

  const result = attempt.result || attempt.status;
  const failed = attempt.failedTestCase;
  const canRequestFeedback = attempt.type === "CODING" && attempt.status === "COMPLETED" && attempt.totalTestCases != null;
  const feedbackSections: [string, string | null][] = feedback ? [
    ["Summary", feedback.summary], ["Mistake", feedback.mistake], ["Explanation", feedback.explanation],
    ["Suggestion", feedback.suggestion], ["Complexity analysis", feedback.complexityAnalysis], ["Optimized approach", feedback.optimizedApproach],
  ].filter((item): item is [string, string] => Boolean(item[1]?.trim())) : [];

  return <section className="content-page attempt-detail-page">
    <p className="breadcrumb"><Link to="/my-attempts">My Attempts</Link> / Attempt details</p>
    <header className="page-heading attempt-detail-heading">
      <div><p className="eyebrow">SUBMISSION DETAILS</p><h1>{questionTitle}</h1><p className="page-subtitle">{prettyStatus(attempt.status)} · {formatDate(attempt.createdAt)} · {attempt.language ?? "Language unavailable"}</p></div>
      <span className={`attempt-status status-${result.toLowerCase().replace(/_/g, "-")}`}>{prettyStatus(result)}</span>
    </header>

    <section className="attempt-detail-section" aria-labelledby="attempt-result-heading">
      <h2 id="attempt-result-heading">Result</h2>
      <div className="attempt-metrics">
        <div><span>Score</span><strong>{attempt.score == null ? "—" : `${Number(attempt.score.toFixed(1))}%`}</strong></div>
        <div><span>Tests passed</span><strong>{attempt.passedTestCases != null && attempt.totalTestCases != null ? `${attempt.passedTestCases} / ${attempt.totalTestCases}` : "—"}</strong></div>
        <div><span>Execution time</span><strong>{attempt.executionTime == null ? "—" : `${attempt.executionTime} ms`}</strong></div>
        <div><span>Memory used</span><strong>{attempt.memoryUsed == null ? "—" : `${attempt.memoryUsed} KB`}</strong></div>
      </div>
      {attempt.compilerOutput && <div className="attempt-output"><h3>Compiler output</h3><pre>{attempt.compilerOutput}</pre></div>}
      {attempt.runtimeOutput && <div className="attempt-output"><h3>Runtime output</h3><pre>{attempt.runtimeOutput}</pre></div>}
      {failed && <section className="attempt-failed-case"><h3>Failed testcase</h3><dl>
        {failed.input != null && <><dt>Input</dt><dd>{failed.input || "(empty)"}</dd></>}
        {failed.expectedOutput != null && <><dt>Expected output</dt><dd>{failed.expectedOutput || "(empty)"}</dd></>}
        {failed.actualOutput != null && <><dt>Actual output</dt><dd>{failed.actualOutput || "(empty)"}</dd></>}
      </dl></section>}
    </section>

    <section className="attempt-detail-section" aria-labelledby="submitted-code-heading">
      <div className="attempt-section-heading"><div><h2 id="submitted-code-heading">Submitted code</h2><p>{attempt.language ?? "Language unavailable"}</p></div></div>
      {attempt.answer != null ? <div className="attempt-code-editor">
        <MonacoCodeEditor
          className="attempt-code-monaco"
          value={attempt.answer}
          onChange={readOnly}
          language={editorLanguage[attempt.language?.trim().toUpperCase() ?? ""] ?? "plaintext"}
          ariaLabel="Submitted code, read only"
          readOnly
        />
      </div> : <p className="page-state">Submitted code is unavailable for this attempt.</p>}
    </section>

    {canRequestFeedback && <section className="attempt-detail-section ai-feedback-section" aria-labelledby="ai-feedback-heading">
      <div className="attempt-section-heading"><div><h2 id="ai-feedback-heading">AI feedback</h2><p>Request feedback for this submission when you’re ready.</p></div><button className="primary-button feedback-button" disabled={feedbackLoading} onClick={() => void loadFeedback()}>{feedbackLoading ? "Getting feedback…" : feedback ? "Refresh feedback" : "Get AI Feedback"}</button></div>
      {feedbackError && <div className="notice-panel error-panel" role="alert">{feedbackError}</div>}
      {feedback && feedbackSections.length > 0 && <div className="attempt-feedback-grid">{feedbackSections.map(([title, content]) => <article key={title}><h3>{title}</h3><p>{content}</p></article>)}</div>}
      {feedback && feedbackSections.length === 0 && <p className="page-state">No feedback content was returned.</p>}
    </section>}
  </section>;
}
