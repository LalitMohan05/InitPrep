import { FormEvent, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import * as questionsApi from "../api/questionsApi";
import type { QuestionPage, TestCase, TestCaseWriteRequest } from "../types/questions";

function parseBulkCases(value: string): TestCaseWriteRequest[] {
  const parsed: unknown = JSON.parse(value);
  if (!Array.isArray(parsed) || parsed.length === 0) throw new Error("Enter a non-empty JSON array of test cases.");
  return parsed.map((item, index) => {
    if (typeof item !== "object" || item === null) throw new Error(`Test case ${index + 1} must be an object.`);
    const row = item as Record<string, unknown>;
    if (typeof row.input !== "string" || typeof row.expectedOutput !== "string" || typeof row.hidden !== "boolean") {
      throw new Error(`Test case ${index + 1} must have string input, string expectedOutput, and boolean hidden fields.`);
    }
    return { input: row.input, expectedOutput: row.expectedOutput, hidden: row.hidden };
  });
}

export function TestCaseManagerPage() {
  const { questionId } = useParams();
  const [questions, setQuestions] = useState<QuestionPage | null>(null);
  const [items, setItems] = useState<TestCase[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);

  async function loadCases() {
    if (!questionId) return;
    setLoading(true);
    setError("");
    try { setItems(await questionsApi.getTestCases(questionId)); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Could not load test cases."); }
    finally { setLoading(false); }
  }

  useEffect(() => {
    setLoading(true);
    setError("");
    if (questionId) { void loadCases(); return; }
    questionsApi.getQuestions({ page: 0, size: 100 })
      .then(setQuestions)
      .catch(cause => setError(cause instanceof Error ? cause.message : "Could not load questions."))
      .finally(() => setLoading(false));
  }, [questionId]);

  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!questionId) return;
    const formElement = event.currentTarget;
    const data = new FormData(formElement);
    const request = { input: String(data.get("input")), expectedOutput: String(data.get("expectedOutput")), hidden: data.get("hidden") === "on" };
    setBusy(true); setError("");
    try {
      await questionsApi.createTestCase(questionId, request);
      formElement.reset();
      setNotice("Test case created.");
      await loadCases();
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Could not create test case."); }
    finally { setBusy(false); }
  }

  async function edit(item: TestCase, form: HTMLFormElement) {
    const data = new FormData(form);
    const request = { input: String(data.get("input")), expectedOutput: String(data.get("expectedOutput")), hidden: data.get("hidden") === "on" };
    setError("");
    try { await questionsApi.updateTestCase(questionId!, item.id, request); setNotice("Test case updated."); await loadCases(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Could not update test case."); }
  }

  async function remove(item: TestCase) {
    if (!window.confirm("Delete this test case?")) return;
    try { await questionsApi.deleteTestCase(questionId!, item.id); setNotice("Test case deleted."); await loadCases(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Could not delete test case."); }
  }

  async function bulkCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!questionId) return;
    const formElement = event.currentTarget;
    const data = new FormData(formElement);
    setBusy(true); setError("");
    try {
      const cases = parseBulkCases(String(data.get("testCases")));
      await questionsApi.createTestCases(questionId, cases);
      formElement.reset();
      setNotice(`${cases.length} test cases created.`);
      await loadCases();
    } catch (cause) { setError(cause instanceof Error ? cause.message : "Could not create test cases."); }
    finally { setBusy(false); }
  }

  if (loading) return <p className="page-state">Loading {questionId ? "test cases" : "questions"}…</p>;
  if (!questionId) return <section className="content-page">
    <p className="eyebrow">ADMINISTRATION</p><h1>Manage test cases</h1><p className="page-subtitle">Choose a question to view and manage its test cases.</p>
    {error ? <div className="notice-panel error-panel" role="alert">{error}</div> : !questions?.content.length ? <div className="empty-state">No questions are available yet.</div> : <div className="question-list">{questions.content.filter(question => question.type === "CODING").map(question => <article className="question-row" key={question.id}><div><Link className="question-title" to={`/manage-test-cases/${question.id}`}>{question.title}</Link><p className="tag-line">{question.difficulty} · {question.type}</p></div><Link className="secondary-button" to={`/manage-test-cases/${question.id}`}>Manage</Link></article>)}</div>}
    {questions && questions.totalPages > 1 && <p className="field-hint">Showing the first 100 questions. Filter the Questions page to locate others.</p>}
  </section>;

  return <section className="content-page">
    <p className="breadcrumb"><Link to="/manage-test-cases">Manage test cases</Link></p>
    <div className="page-heading"><div><p className="eyebrow">ADMINISTRATION</p><h1>Test cases</h1></div><Link className="secondary-button" to={`/questions/${questionId}`}>View question</Link></div>
    {notice && <p className="success-message" role="status">{notice}</p>}
    {error && <div className="notice-panel error-panel" role="alert">{error}</div>}
    <details className="management-panel" open><summary>Add one test case</summary>
      <form className="data-form" onSubmit={event => void create(event)}>
        <label>Input<textarea name="input" rows={3} required /></label>
        <label>Expected output<textarea name="expectedOutput" rows={3} required /></label>
        <label className="checkbox-label"><input name="hidden" type="checkbox" /> Hidden test case</label>
        <button className="primary-button compact-button" disabled={busy}>{busy ? "Saving…" : "Create test case"}</button>
      </form>
    </details>
    <details className="management-panel"><summary>Bulk create test cases</summary>
      <form className="data-form" onSubmit={event => void bulkCreate(event)}>
        <label>JSON array <textarea name="testCases" rows={7} required placeholder={'[{"input":"1 2","expectedOutput":"3","hidden":false}]'} /></label>
        <span className="field-hint">Each item must contain input, expectedOutput, and hidden. This uses the existing bulk endpoint.</span>
        <button className="primary-button compact-button" disabled={busy}>{busy ? "Creating…" : "Create test cases"}</button>
      </form>
    </details>
    <h2 className="section-title">Existing test cases</h2>
    {error ? null : items.length === 0 ? <div className="empty-state">No test cases found.</div> : <div className="test-case-list">{items.map(item => <details className="test-case-row" key={item.id}>
      <summary><span>Test case</span><span className={item.hidden ? "visibility-hidden" : "visibility-visible"}>{item.hidden ? "Hidden" : "Visible"}</span></summary>
      <form className="data-form" onSubmit={event => { event.preventDefault(); void edit(item, event.currentTarget); }}>
        <label>Input<textarea name="input" rows={3} defaultValue={item.input} required /></label>
        <label>Expected output<textarea name="expectedOutput" rows={3} defaultValue={item.expectedOutput} required /></label>
        <label className="checkbox-label"><input name="hidden" type="checkbox" defaultChecked={item.hidden} /> Hidden test case</label>
        <div className="row-actions"><button className="secondary-button" type="submit">Save test case</button><button className="secondary-button danger-button" type="button" onClick={() => void remove(item)}>Delete</button></div>
      </form>
    </details>)}</div>}
  </section>;
}
