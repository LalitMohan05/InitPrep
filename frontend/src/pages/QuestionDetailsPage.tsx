import { FormEvent, useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import * as questionsApi from "../api/questionsApi";
import { MonacoCodeEditor } from "../components/MonacoCodeEditor";
import { useAuth } from "../context/AuthContext";
import type { QuestionDetails, QuestionSummary } from "../types/questions";
import type { QuestionRole } from "../types/questions";
import { normalizeQuestionText, questionExamples } from "../utils/questionContent";

const targetRoles: { value: QuestionRole; label: string }[] = [
  { value: "BACKEND_DEVELOPER", label: "Backend Developer" },
  { value: "FRONTEND_DEVELOPER", label: "Frontend Developer" },
  { value: "FULL_STACK_DEVELOPER", label: "Full Stack Developer" },
  { value: "DEVOPS_ENGINEER", label: "DevOps Engineer" },
  { value: "MACHINE_LEARNING_ENGINEER", label: "Machine Learning Engineer" },
];

export function QuestionDetailsPage() {
  const { questionId = "" } = useParams();
  const { user } = useAuth();
  const admin = user?.role === "ADMIN";
  const location = useLocation();
  const navigate = useNavigate();
  const summary = (location.state as { question?: QuestionSummary } | null)?.question;
  const [question, setQuestion] = useState<QuestionDetails | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [editing, setEditing] = useState(false);
  const [starterCode, setStarterCode] = useState("");
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState("");

  useEffect(() => {
    let active = true;
    questionsApi.getQuestionDetails(questionId)
      .then(value => { if (active) { setQuestion(value); setStarterCode(normalizeQuestionText(value.starterCode, true)); } })
      .catch(cause => { if (active) setError(cause instanceof Error ? cause.message : "Could not load this question."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [questionId]);

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!question) return;
    const data = new FormData(event.currentTarget);
    const roles = data.getAll("roles").map(String) as QuestionRole[];
    if (roles.length === 0) {
      setError("Select at least one target role.");
      return;
    }
    setSaving(true);
    setError("");
    try {
      await questionsApi.updateQuestion(questionId, {
        title: String(data.get("title")).trim(),
        description: String(data.get("description")),
        constraints: String(data.get("constraints") ?? ""),
        examples: String(data.get("examples") ?? ""),
        hints: String(data.get("hints") ?? ""),
        starterCode: String(data.get("starterCode") ?? ""),
        expectedComplexity: String(data.get("expectedComplexity") ?? ""),
        roles,
      });
      setQuestion({
        ...question,
        title: String(data.get("title")).trim(),
        description: String(data.get("description")),
        constraints: String(data.get("constraints") ?? ""),
        examples: String(data.get("examples") ?? ""),
        hints: String(data.get("hints") ?? ""),
        starterCode: String(data.get("starterCode") ?? ""),
        expectedComplexity: String(data.get("expectedComplexity") ?? ""),
        roles,
      });
      setStarterCode(String(data.get("starterCode") ?? ""));
      setEditing(false);
      setNotice("Question updated.");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Could not update this question.");
    } finally {
      setSaving(false);
    }
  }

  async function remove() {
    if (!question || !window.confirm(`Delete “${question.title}”? This cannot be undone.`)) return;
    try {
      await questionsApi.deleteQuestion(questionId);
      navigate("/questions", { replace: true });
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Could not delete this question.");
    }
  }

  if (loading) return <p className="page-state">Loading question…</p>;
  if (error && !question) return <section className="content-page"><div className="notice-panel error-panel" role="alert">{error}</div><Link to="/questions">Back to questions</Link></section>;
  if (!question) return null;

  return <section className="content-page">
    <p className="breadcrumb"><Link to="/questions">Questions</Link> / Details</p>
    {notice && <p className="success-message" role="status">{notice}</p>}
    {error && <div className="notice-panel error-panel" role="alert">{error}</div>}
    <article className="detail-panel">
      <div className="page-heading detail-heading">
        <div><p className="eyebrow">{summary ? `${summary.difficulty} · ${summary.type}` : "QUESTION"}</p><h1>{question.title}</h1></div>
        {admin && <div className="row-actions"><button className="secondary-button" onClick={() => { if (!editing) setStarterCode(normalizeQuestionText(question.starterCode, true)); setEditing(value => !value); }}>{editing ? "Cancel edit" : "Edit question"}</button><button className="secondary-button danger-button" onClick={() => void remove()}>Delete</button><Link className="secondary-button" to={`/questions/${questionId}/test-cases`}>Manage test cases</Link></div>}
      </div>
      {editing && admin ? <form className="data-form edit-question-form" onSubmit={save}>
        <label>Title<input name="title" maxLength={200} defaultValue={question.title} required /></label>
        <label>Description<textarea name="description" rows={8} defaultValue={normalizeQuestionText(question.description)} required /></label>
        <label>Constraints<textarea name="constraints" rows={3} defaultValue={normalizeQuestionText(question.constraints)} /></label>
        <label>Examples<textarea name="examples" rows={6} defaultValue={normalizeQuestionText(question.examples)} /></label>
        <label>Hints<textarea name="hints" rows={3} defaultValue={normalizeQuestionText(question.hints)} /></label>
        <label className="starter-code-field">Starter Code
          <span className="field-hint">Code shown to candidates when they open the coding question.</span>
          <input type="hidden" name="starterCode" value={starterCode} />
          <MonacoCodeEditor className="starter-code-monaco" ariaLabel="Starter code" value={starterCode} onChange={setStarterCode} language="java" height="340px" />
        </label>
        <label>Expected complexity<textarea name="expectedComplexity" rows={2} defaultValue={normalizeQuestionText(question.expectedComplexity)} /></label>
        <fieldset className="role-checkboxes"><legend>Target roles</legend>{targetRoles.map(role => <label key={role.value}><input type="checkbox" name="roles" value={role.value} defaultChecked={question.roles?.includes(role.value)} />{role.label}</label>)}</fieldset>
        <button className="primary-button compact-button" disabled={saving}>{saving ? "Saving…" : "Save changes"}</button>
      </form> : <>
        <div className="question-detail-sections">
          <section><h2>Problem</h2><pre>{normalizeQuestionText(question.description)}</pre></section>
          {question.constraints && <section><h2>Constraints</h2><pre>{normalizeQuestionText(question.constraints)}</pre></section>}
          {question.examples && <section><h2>Examples</h2>{questionExamples(question.examples).map((example, index) => <pre className="example-block" key={index}>{example}</pre>)}</section>}
          {question.hints && <section><h2>Hints</h2><pre>{normalizeQuestionText(question.hints)}</pre></section>}
          {question.starterCode && <section><h2>Starter code</h2><pre className="starter-preview">{normalizeQuestionText(question.starterCode, true)}</pre></section>}
          {question.expectedComplexity && <section><h2>Expected complexity</h2><pre>{normalizeQuestionText(question.expectedComplexity)}</pre></section>}
          {question.roles?.length ? <section><h2>Target roles</h2><p>{question.roles.map(role => targetRoles.find(item => item.value === role)?.label ?? role).join(", ")}</p></section> : null}
        </div>
          {question.type === "CODING" && <div className="preparation-note"><strong>Ready to work on it?</strong><p>Open the coding workspace to write and run a solution.</p><Link className="secondary-button solve-link" to={`/questions/${questionId}/solve`}>Open coding workspace</Link></div>}
      </>}
    </article>
  </section>;
}
