import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import * as attemptsApi from "../api/attemptsApi";
import { formatApiError } from "../api/client";
import { getQuestionDetails } from "../api/questionsApi";
import type { AttemptPage } from "../types/coding";

const PAGE_SIZE = 10;
const formatDate = (value: string) => {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Date unavailable" : date.toLocaleString();
};

export function MyAttemptsPage() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState<AttemptPage | null>(null);
  const [questionTitles, setQuestionTitles] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    attemptsApi.getAttempts(page, PAGE_SIZE)
      .then(async result => {
        if (!active) return;
        setData(result);
        const ids = [...new Set(result.content.map(attempt => attempt.questionId))];
        const titles = await Promise.all(ids.map(async id => {
          try {
            const question = await getQuestionDetails(id);
            return [id, question.title] as const;
          } catch {
            return [id, "Question unavailable"] as const;
          }
        }));
        if (active) setQuestionTitles(previous => ({ ...previous, ...Object.fromEntries(titles) }));
      })
      .catch(cause => { if (active) setError(formatApiError(cause, "Could not load your attempts.")); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [page]);

  return <section className="content-page attempts-page">
    <header className="page-heading">
      <div><p className="eyebrow">PRACTICE HISTORY</p><h1>My Attempts</h1><p className="page-subtitle">Review your coding submissions and results.</p></div>
      {data && <span className="attempt-count">{data.totalElements} {data.totalElements === 1 ? "attempt" : "attempts"}</span>}
    </header>

    {error && <div className="notice-panel error-panel" role="alert">{error}</div>}
    {loading && <p className="page-state">Loading attempts…</p>}
    {!loading && !error && data?.content.length === 0 && <div className="empty-state"><h2>No attempts yet</h2><p>Your coding submissions will appear here.</p><Link className="secondary-button attempts-browse" to="/questions">Browse questions</Link></div>}

    {!loading && !error && data && data.content.length > 0 && <>
      <div className="attempt-table-wrap">
        <table className="attempt-table">
          <thead><tr><th>Question</th><th>Result</th><th>Score</th><th>Language</th><th>Submitted</th><th><span className="visually-hidden">Details</span></th></tr></thead>
          <tbody>{data.content.map(attempt => {
            const result = attempt.result || attempt.status;
            const statusClass = result.toLowerCase().replace(/_/g, "-");
            return <tr key={attempt.id}>
              <td><span className="attempt-question">{questionTitles[attempt.questionId] ?? "Loading question…"}</span><span className="attempt-type">{attempt.type ?? "Attempt"}</span></td>
              <td><span className={`attempt-status status-${statusClass}`}>{result.replace(/_/g, " ")}</span></td>
              <td>{attempt.score == null ? "—" : `${Number(attempt.score.toFixed(1))}%`}</td>
              <td>{attempt.language ?? "—"}</td>
              <td>{formatDate(attempt.createdAt)}</td>
              <td><Link className="text-button" to={`/my-attempts/${attempt.id}`}>View details <span aria-hidden="true">→</span></Link></td>
            </tr>;
          })}</tbody>
        </table>
      </div>
      <div className="attempt-pagination pagination">
        <span>Page {data.number + 1} of {Math.max(data.totalPages, 1)}</span>
        <div><button className="secondary-button" disabled={data.first || loading} onClick={() => setPage(current => Math.max(0, current - 1))}>Previous</button><button className="secondary-button" disabled={data.last || loading} onClick={() => setPage(current => current + 1)}>Next</button></div>
      </div>
    </>}
  </section>;
}
