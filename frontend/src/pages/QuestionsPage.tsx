import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import * as questionsApi from "../api/questionsApi";
import { useAuth } from "../context/AuthContext";
import { QuestionForm } from "../components/QuestionForm";
import type { QuestionPage } from "../types/questions";

interface Props { managementMode?: boolean; }

export function QuestionsPage({ managementMode = false }: Props) {
  const { user } = useAuth();
  const admin = user?.role === "ADMIN";
  const navigate = useNavigate();
  const [filters, setFilters] = useState({ difficulty: "", type: "", company: "", topic: "" });
  const [pageNumber, setPageNumber] = useState(0);
  const [result, setResult] = useState<QuestionPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [refreshKey, setRefreshKey] = useState(0);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setResult(await questionsApi.getQuestions({ ...filters, page: pageNumber, size: 10 }));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Could not load questions.");
    } finally {
      setLoading(false);
    }
  }, [filters, pageNumber, refreshKey]);

  useEffect(() => { void load(); }, [load]);

  function changeFilter(name: keyof typeof filters, value: string) {
    setFilters(current => ({ ...current, [name]: value }));
    setPageNumber(0);
  }

  async function removeQuestion(id: string, title: string) {
    if (!window.confirm(`Delete “${title}”? This cannot be undone.`)) return;
    setNotice("");
    try {
      await questionsApi.deleteQuestion(id);
      setNotice("Question deleted.");
      setRefreshKey(value => value + 1);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Could not delete the question.");
    }
  }

  return <section className="content-page">
    <div className="page-heading">
      <div><p className="eyebrow">PRACTICE LIBRARY</p><h1>{managementMode ? "Manage questions" : "Questions"}</h1><p className="page-subtitle">Browse interview questions by topic and difficulty.</p></div>
      {admin && <Link className="secondary-button" to="/manage-questions">Question management</Link>}
    </div>

    {admin && managementMode && <QuestionForm onCreated={() => { setNotice("Question created."); setRefreshKey(value => value + 1); }} />}
    {notice && <p className="success-message" role="status">{notice}</p>}

    <div className="filter-bar" aria-label="Question filters">
      <label>Difficulty<select value={filters.difficulty} onChange={event => changeFilter("difficulty", event.target.value)}><option value="">All difficulties</option><option>EASY</option><option>MEDIUM</option><option>HARD</option></select></label>
      <label>Type<select value={filters.type} onChange={event => changeFilter("type", event.target.value)}><option value="">All types</option><option>CODING</option><option>THEORY</option><option>MCQ</option></select></label>
      <label>Company<input value={filters.company} onChange={event => changeFilter("company", event.target.value)} placeholder="Company name" /></label>
      <label>Topic<input value={filters.topic} onChange={event => changeFilter("topic", event.target.value)} placeholder="Topic name" /></label>
    </div>

    {loading ? <p className="page-state">Loading questions…</p> : error ? <div className="notice-panel error-panel" role="alert">{error}</div> : !result?.content.length ? <div className="empty-state"><h2>No questions found</h2><p>Try changing or clearing the filters.</p></div> : <>
      <div className="question-list">
        {result.content.map(question => <article className="question-row" key={question.id}>
          <div className="question-copy">
            <Link className="question-title" to={`/questions/${question.id}`} state={{ question }}>{question.title}</Link>
            <div className="question-meta"><span className={`difficulty difficulty-${question.difficulty.toLowerCase()}`}>{question.difficulty}</span><span>{question.type}</span></div>
            {question.companies?.length > 0 && <p className="tag-line">Companies: {question.companies.map(item => item.name).join(", ")}</p>}
            {question.topics?.length > 0 && <p className="tag-line">Topics: {question.topics.map(item => item.name).join(", ")}</p>}
          </div>
          {admin && <div className="row-actions"><button className="text-button" onClick={() => navigate(`/questions/${question.id}`, { state: { question } })}>Edit</button><Link className="text-button" to={`/questions/${question.id}/test-cases`}>Test cases</Link><button className="text-button danger-text" onClick={() => void removeQuestion(question.id, question.title)}>Delete</button></div>}
        </article>)}
      </div>
      <div className="pagination">
        <span>{result.totalElements} questions · Page {result.number + 1} of {Math.max(result.totalPages, 1)}</span>
        <div><button className="secondary-button" disabled={result.first} onClick={() => setPageNumber(value => Math.max(0, value - 1))}>Previous</button><button className="secondary-button" disabled={result.last} onClick={() => setPageNumber(value => value + 1)}>Next</button></div>
      </div>
    </>}
  </section>;
}
