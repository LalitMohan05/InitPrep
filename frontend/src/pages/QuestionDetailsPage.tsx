import { FormEvent, useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import * as questionsApi from "../api/questionsApi";
import { useAuth } from "../context/AuthContext";
import type { QuestionDetails, QuestionSummary } from "../types/questions";

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
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState("");

  useEffect(() => {
    let active = true;
    questionsApi.getQuestionDetails(questionId)
      .then(value => { if (active) setQuestion(value); })
      .catch(cause => { if (active) setError(cause instanceof Error ? cause.message : "Could not load this question."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [questionId]);

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!question) return;
    const data = new FormData(event.currentTarget);
    setSaving(true);
    setError("");
    try {
      await questionsApi.updateQuestion(questionId, {
        title: String(data.get("title")).trim(),
        description: String(data.get("description")).trim(),
      });
      setQuestion({ ...question, title: String(data.get("title")).trim(), description: String(data.get("description")).trim() });
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
        {admin && <div className="row-actions"><button className="secondary-button" onClick={() => setEditing(value => !value)}>{editing ? "Cancel edit" : "Edit question"}</button><button className="secondary-button danger-button" onClick={() => void remove()}>Delete</button><Link className="secondary-button" to={`/questions/${questionId}/test-cases`}>Manage test cases</Link></div>}
      </div>
      {editing && admin ? <form className="data-form edit-question-form" onSubmit={save}>
        <label>Title<input name="title" maxLength={200} defaultValue={question.title} required /></label>
        <label>Description<textarea name="description" rows={10} defaultValue={question.description} required /></label>
        <p className="field-hint">The details API returns only the title and description. Those are the fields available to edit here.</p>
        <button className="primary-button compact-button" disabled={saving}>{saving ? "Saving…" : "Save changes"}</button>
      </form> : <>
        <div className="problem-description">{question.description}</div>
        <div className="preparation-note"><strong>Ready to work on it?</strong><p>Use this page to review the problem. The coding workspace will be added in a later step.</p></div>
      </>}
    </article>
  </section>;
}
