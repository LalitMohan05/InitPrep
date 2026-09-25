import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import * as mockInterviewApi from "../api/mockInterviewApi";
import type { InterviewQuestion, MockInterviewSession } from "../api/mockInterviewApi";
import { formatApiError } from "../api/client";
import { runCode } from "../api/codingApi";
import * as profileApi from "../api/profileApi";
import type { JudgeSubmissionResponse, ProgrammingLanguage } from "../types/coding";
import { MonacoCodeEditor } from "../components/MonacoCodeEditor";
import { normalizeQuestionText, questionExamples } from "../utils/questionContent";

const languageNames: Record<ProgrammingLanguage, string> = { JAVA: "Java", PYTHON: "Python", CPP: "C++", C: "C", JAVASCRIPT: "JavaScript" };
const editorLanguages: Record<ProgrammingLanguage, string> = { JAVA: "java", PYTHON: "python", CPP: "cpp", C: "c", JAVASCRIPT: "javascript" };
const supportedLanguages = Object.keys(languageNames) as ProgrammingLanguage[];
const roleName = (role: string) => role.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, letter => letter.toUpperCase());
const scoreLabel = (score: number | null | undefined) => score == null ? "Not available" : `${Number(score.toFixed(1))}%`;
const draftKey = (sessionId: string, questionId: string) => `initprep-mock-draft:${sessionId}:${questionId}`;

interface Draft { answer: string; language: ProgrammingLanguage; }
interface Choice { value: string; label: string; }

function parseChoices(source: string | null): Choice[] {
  if (!source?.trim()) return [];
  try {
    const parsed: unknown = JSON.parse(source);
    if (Array.isArray(parsed)) return parsed.map((item, index) => {
      if (typeof item === "string") return { value: String.fromCharCode(65 + index), label: item };
      if (item && typeof item === "object") {
        const option = item as Record<string, unknown>;
        const label = String(option.label ?? option.text ?? option.value ?? "");
        return { value: String(option.key ?? option.id ?? String.fromCharCode(65 + index)), label };
      }
      return { value: String.fromCharCode(65 + index), label: String(item) };
    }).filter(choice => choice.label.trim());
  } catch {
    // The existing question form stores arbitrary multiline option text.
  }
  return source.split(/\r?\n/).map(line => line.trim()).filter(Boolean).map((line, index) => {
    const match = line.match(/^([A-Za-z0-9])\s*[).:\-]\s*(.*)$/);
    return match ? { value: match[1], label: line } : { value: line, label: line || String.fromCharCode(65 + index) };
  });
}

function formatElapsed(startedAt: string, now: number): string {
  const start = new Date(startedAt).getTime();
  const seconds = Math.max(0, Math.floor((now - start) / 1000));
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const remaining = seconds % 60;
  return hours > 0 ? `${String(hours).padStart(2, "0")}:${String(minutes).padStart(2, "0")}:${String(remaining).padStart(2, "0")}` : `${String(minutes).padStart(2, "0")}:${String(remaining).padStart(2, "0")}`;
}

function InterviewReport({ session }: { session: MockInterviewSession }) {
  const questions = session.questions ?? [];
  return <section className="content-page mock-report-page">
    <p className="breadcrumb"><Link to="/mock-interview">Mock Interview</Link> / Report</p>
    <header className="mock-report-header"><div><p className="eyebrow">INTERVIEW REPORT</p><h1>Interview Complete</h1><p>{roleName(session.targetRole)} · {session.difficulty} · {session.totalQuestions} questions</p></div><span className="mock-report-score">{scoreLabel(session.overallScore)}<small>Overall score</small></span></header>
    <section className="mock-report-section"><h2>Performance</h2>
      <div className="mock-report-metrics">
        <div><span>Overall score</span><strong>{scoreLabel(session.overallScore)}</strong></div>
        <div><span>Coding score</span><strong>{scoreLabel(session.codingScore)}</strong></div>
        <div><span>MCQ score</span><strong>{scoreLabel(session.mcqScore)}</strong></div>
        <div><span>Theory score</span><strong>{scoreLabel(session.theoryScore)}</strong></div>
        <div><span>Questions attempted</span><strong>{session.attemptedQuestions} / {session.totalQuestions}</strong></div>
        <div><span>Questions correct</span><strong>{session.correctQuestions}</strong></div>
        <div><span>Coding accepted</span><strong>{session.codingAccepted} / {session.codingAttempted}</strong></div>
        <div><span>Coding performance</span><strong>{scoreLabel(session.codingScore)}</strong></div>
      </div>
    </section>
    <section className="mock-report-section"><h2>Interview responses</h2><div className="mock-response-list">
      {questions.map(item => <details key={item.questionId} className="mock-response-item"><summary><span>Q{item.sequenceNumber} · {item.questionTitleSnapshot}</span><span>{item.attempt?.result?.replace(/_/g, " ") ?? "Not answered"} {item.attempt?.score != null ? `· ${scoreLabel(item.attempt.score)}` : ""}</span></summary>
        <div className="mock-response-content">
          {item.question.type === "THEORY" && item.theoryEvaluation && <div className="mock-evaluation-grid">
            {item.theoryEvaluation.strengths && <div><h3>Strengths</h3><p>{item.theoryEvaluation.strengths}</p></div>}
            {item.theoryEvaluation.weaknesses && <div><h3>Weaknesses</h3><p>{item.theoryEvaluation.weaknesses}</p></div>}
            {item.theoryEvaluation.feedback && <div><h3>Feedback</h3><p>{item.theoryEvaluation.feedback}</p></div>}
          </div>}
          {item.attempt?.type === "CODING" && <p>{item.attempt.passedTestCases != null && item.attempt.totalTestCases != null ? `${item.attempt.passedTestCases} / ${item.attempt.totalTestCases} tests passed` : "Coding result saved."}</p>}
        </div>
      </details>)}
    </div></section>
    <div className="mock-report-insights">
      <section className="mock-report-section"><h2>Strengths</h2>{session.strengths.length ? <ul>{session.strengths.map((item, index) => <li key={`${index}-${item}`}>{item}</li>)}</ul> : <p>Not available for this interview.</p>}</section>
      <section className="mock-report-section"><h2>Weak areas</h2>{session.weaknesses.length ? <ul>{session.weaknesses.map((item, index) => <li key={`${index}-${item}`}>{item}</li>)}</ul> : <p>Not available for this interview.</p>}</section>
      <section className="mock-report-section"><h2>Recommended topics</h2>{session.recommendedTopics.length ? <ul>{session.recommendedTopics.map((item, index) => <li key={`${index}-${item}`}>{item}</li>)}</ul> : <p>Not available for this interview.</p>}</section>
    </div>
    <Link className="secondary-button mock-back-button" to="/mock-interview">Back to Mock Interview</Link>
  </section>;
}

export function MockInterviewSessionPage() {
  const { sessionId = "" } = useParams();
  const [session, setSession] = useState<MockInterviewSession | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [answer, setAnswer] = useState("");
  const [language, setLanguage] = useState<ProgrammingLanguage>("JAVA");
  const [preferredLanguage, setPreferredLanguage] = useState<ProgrammingLanguage>("JAVA");
  const [saving, setSaving] = useState(false);
  const [running, setRunning] = useState(false);
  const [runResult, setRunResult] = useState<JudgeSubmissionResponse | null>(null);
  const [runError, setRunError] = useState("");
  const [now, setNow] = useState(Date.now());

  useEffect(() => {
    let active = true;
    mockInterviewApi.getInterview(sessionId)
      .then(value => value.status === "COMPLETED" ? mockInterviewApi.getInterviewReport(sessionId) : value)
      .then(value => { if (active) setSession(value); })
      .catch(cause => { if (active) setError(formatApiError(cause, "Could not load this interview.")); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [sessionId]);

  useEffect(() => {
    let active = true;
    profileApi.getProfile().then(profile => {
      if (active && profile.preferredLanguage && supportedLanguages.includes(profile.preferredLanguage as ProgrammingLanguage)) {
        setPreferredLanguage(profile.preferredLanguage as ProgrammingLanguage);
      }
    }).catch(() => undefined);
    return () => { active = false; };
  }, []);

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);

  const current: InterviewQuestion | undefined = useMemo(
    () => session?.questions?.find(item => !item.attempt),
    [session],
  );
  const currentKey = current ? draftKey(sessionId, current.question.id) : "";

  useEffect(() => {
    if (!current || !currentKey) return;
    let draft: Draft | null = null;
    try { draft = JSON.parse(localStorage.getItem(currentKey) ?? "null") as Draft | null; } catch { draft = null; }
    setAnswer(draft?.answer ?? (current.question.type === "CODING" ? current.question.starterCode ?? "" : ""));
    setLanguage(draft?.language && supportedLanguages.includes(draft.language) ? draft.language : preferredLanguage);
    setRunResult(null);
    setRunError("");
  }, [currentKey, current, preferredLanguage]);

  function updateDraft(nextAnswer: string, nextLanguage = language) {
    setAnswer(nextAnswer);
    if (currentKey) {
      try { localStorage.setItem(currentKey, JSON.stringify({ answer: nextAnswer, language: nextLanguage } satisfies Draft)); } catch { /* Session answers are still saved when submitted. */ }
    }
  }

  async function runCurrentCode() {
    if (!current || !answer.trim()) { setRunError("Write a solution before running it."); return; }
    setRunning(true);
    setRunError("");
    setRunResult(null);
    try { setRunResult(await runCode({ questionId: current.question.id, sourceCode: answer, language })); }
    catch (cause) { setRunError(formatApiError(cause, "Could not run this solution.")); }
    finally { setRunning(false); }
  }

  async function submitCurrentAnswer() {
    if (!current || !answer.trim()) { setError(current?.question.type === "MCQ" ? "Select an answer before continuing." : "Add your answer before continuing."); return; }
    setSaving(true);
    setError("");
    try {
      const updated = await mockInterviewApi.submitInterviewAnswer(sessionId, current.question.id, answer, current.question.type === "CODING" ? language : undefined);
      try { localStorage.removeItem(currentKey); } catch { /* Ignore unavailable storage. */ }
      setSession(updated);
      setAnswer("");
      setRunResult(null);
    } catch (cause) {
      setError(formatApiError(cause, "Could not save this answer."));
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <section className="content-page"><p className="page-state">Loading interview…</p></section>;
  if (error && !session) return <section className="content-page"><div className="notice-panel error-panel" role="alert">{error}</div><Link to="/mock-interview">Back to Mock Interview</Link></section>;
  if (!session) return null;
  if (session.status === "COMPLETED") return <InterviewReport session={session} />;
  if (!current) return <section className="content-page"><p className="page-state">Refreshing interview progress…</p></section>;

  const question = current.question;
  const choices = question.type === "MCQ" ? parseChoices(question.options) : [];
  const answeredItems = (session.questions ?? []).filter(item => item.attempt);

  return <section className="mock-session-page">
    <header className="mock-session-topbar">
      <Link to="/mock-interview" className="mock-exit-link">← Interview history</Link>
      <div className="mock-session-meta"><span>{roleName(session.targetRole)}</span><span>{session.difficulty}</span><span>Technical interview</span></div>
      <div className="mock-timer" aria-label="Interview elapsed time"><span>Elapsed</span><strong>{formatElapsed(session.startedAt, now)}</strong></div>
    </header>
    <div className="mock-session-progress"><div><span>Question {current.sequenceNumber} / {session.totalQuestions}</span><span>{session.attemptedQuestions} completed</span></div><div className="mock-progress-track"><span style={{ width: `${session.attemptedQuestions / session.totalQuestions * 100}%` }} /></div></div>
    <main className="mock-question-layout">
      <article className="mock-question-pane">
        <div className="mock-question-kicker"><span>{question.type}</span><span>{question.difficulty}</span></div>
        <h1>{question.title}</h1>
        <div className="mock-question-description">{normalizeQuestionText(question.description)}</div>
        {question.constraints && <section><h2>Constraints</h2><pre>{normalizeQuestionText(question.constraints)}</pre></section>}
        {question.examples && <section><h2>Examples</h2>{questionExamples(question.examples).map((example, index) => <pre key={index}>{example}</pre>)}</section>}
      </article>
      <section className="mock-answer-pane" aria-label="Your answer">
        {question.type === "CODING" && <>
          <div className="mock-answer-toolbar"><label>Language<select value={language} onChange={event => { const next = event.target.value as ProgrammingLanguage; setLanguage(next); updateDraft(answer, next); }}>{supportedLanguages.map(value => <option key={value} value={value}>{languageNames[value]}</option>)}</select></label><span>Solution</span></div>
          <div className="mock-code-editor"><MonacoCodeEditor className="mock-code-editor-inner" value={answer} onChange={value => updateDraft(value)} language={editorLanguages[language]} ariaLabel="Mock interview solution" /></div>
          {runError && <div className="notice-panel error-panel" role="alert">{runError}</div>}
          {runResult && <div className="mock-run-result"><strong>{runResult.status.replace(/_/g, " ")}</strong><span>{runResult.passedTestCases ?? 0} / {runResult.totalTestCases ?? 0} public tests passed</span>{runResult.compilerOutput && <pre>{runResult.compilerOutput}</pre>}{runResult.runtimeOutput && <pre>{runResult.runtimeOutput}</pre>}</div>}
          <div className="mock-answer-actions"><button className="secondary-button" type="button" disabled={running || saving} onClick={() => void runCurrentCode()}>{running ? "Running…" : "Run"}</button><button className="primary-button" type="button" disabled={running || saving} onClick={() => void submitCurrentAnswer()}>{saving ? "Submitting…" : "Submit & Next"}</button></div>
        </>}
        {question.type === "MCQ" && <>
          <h2>Select one answer</h2>
          {choices.length ? <div className="mock-choice-list">{choices.map(choice => <label className={`mock-choice${answer === choice.value ? " selected" : ""}`} key={choice.value}><input type="radio" name="mock-interview-answer" value={choice.value} checked={answer === choice.value} onChange={() => updateDraft(choice.value)} /><span>{choice.label}</span></label>)}</div> : <div className="notice-panel error-panel">This MCQ does not have readable options. Please return to the question library and update its options.</div>}
          <div className="mock-answer-actions"><button className="primary-button" type="button" disabled={saving || choices.length === 0} onClick={() => void submitCurrentAnswer()}>{saving ? "Saving…" : "Submit & Next"}</button></div>
        </>}
        {question.type === "THEORY" && <>
          <label className="mock-theory-label" htmlFor="mock-theory-answer">Your response</label>
          <textarea id="mock-theory-answer" className="mock-theory-answer" value={answer} onChange={event => updateDraft(event.target.value)} placeholder="Structure your answer as you would in a technical interview." />
          <div className="mock-answer-actions"><button className="primary-button" type="button" disabled={saving} onClick={() => void submitCurrentAnswer()}>{saving ? "Evaluating…" : "Submit & Next"}</button></div>
        </>}
        {error && <div className="notice-panel error-panel" role="alert">{error}</div>}
        <p className="mock-save-note">Your submitted answers are saved to this session. An unfinished answer is kept in this browser if you refresh.</p>
      </section>
    </main>
    {answeredItems.length > 0 && <details className="mock-previous-answers"><summary>Previous answers ({answeredItems.length})</summary><div>{answeredItems.map(item => <article key={item.questionId}><strong>Q{item.sequenceNumber}: {item.questionTitleSnapshot}</strong><span>{item.attempt?.result?.replace(/_/g, " ")} · {scoreLabel(item.attempt?.score)}</span>{item.theoryEvaluation?.feedback && <p>{item.theoryEvaluation.feedback}</p>}</article>)}</div></details>}
  </section>;
}
