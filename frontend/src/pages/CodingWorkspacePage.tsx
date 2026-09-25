import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import * as codingApi from "../api/codingApi";
import { formatApiError } from "../api/client";
import { getQuestionDetails } from "../api/questionsApi";
import { MonacoCodeEditor } from "../components/MonacoCodeEditor";
import type { AttemptResponse, CodingFeedback, JudgeSubmissionResponse, JudgeTestCase, ProgrammingLanguage } from "../types/coding";
import type { QuestionDetails } from "../types/questions";
import { normalizeQuestionText, questionExamples } from "../utils/questionContent";

const languages: ProgrammingLanguage[] = ["JAVA", "PYTHON", "CPP", "C", "JAVASCRIPT"];
const readable = (value?: string | null) => value?.trim() || "Not provided.";
const monacoLanguages: Record<ProgrammingLanguage, string> = {
  JAVA: "java",
  PYTHON: "python",
  CPP: "cpp",
  C: "c",
  JAVASCRIPT: "javascript",
};

export function CodingWorkspacePage() {
  const { questionId = "" } = useParams();
  const [question, setQuestion] = useState<QuestionDetails | null>(null);
  const [testCases, setTestCases] = useState<JudgeTestCase[]>([]);
  const [sourceCode, setSourceCode] = useState("");
  const [language, setLanguage] = useState<ProgrammingLanguage>("JAVA");
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [runResult, setRunResult] = useState<JudgeSubmissionResponse | null>(null);
  const [submitResult, setSubmitResult] = useState<AttemptResponse | null>(null);
  const [runError, setRunError] = useState("");
  const [action, setAction] = useState<"RUN" | "SUBMIT" | null>(null);
  const [problemCollapsed, setProblemCollapsed] = useState(false);
  const [editorFocused, setEditorFocused] = useState(false);
  const [panelCollapsed, setPanelCollapsed] = useState(false);
  const [panelExpanded, setPanelExpanded] = useState(false);
  const [selectedCase, setSelectedCase] = useState(0);
  const [feedback, setFeedback] = useState<CodingFeedback | null>(null);
  const [feedbackError, setFeedbackError] = useState("");
  const [feedbackLoading, setFeedbackLoading] = useState(false);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setLoadError("");
    setQuestion(null);
    setTestCases([]);
    setSourceCode("");
    setRunResult(null);
    setSubmitResult(null);
    setFeedback(null);
    Promise.all([getQuestionDetails(questionId), codingApi.getPublicTestCases(questionId)])
      .then(([details, publicData]) => {
        if (!active) return;
        setQuestion(details);
        setSourceCode(normalizeQuestionText(details.starterCode, true));
        setTestCases((publicData.testCases ?? []).filter(testCase => !testCase.hidden));
      })
      .catch(cause => { if (active) setLoadError(formatApiError(cause, "Could not load this question.")); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [questionId]);

  async function execute(kind: "RUN" | "SUBMIT") {
    if (question?.type !== "CODING") { setRunError("The coding workspace can run and submit coding questions only."); return; }
    if (!sourceCode.trim()) { setRunError("Add some code before running it."); return; }
    if (kind === "RUN" && testCases.length === 0) { setRunError("There are no public test cases available to run."); return; }
    setAction(kind);
    setRunError("");
    setFeedback(null);
    setFeedbackError("");
    setRunResult(null);
    setSubmitResult(null);
    setPanelCollapsed(false);
    setPanelExpanded(false);
    setEditorFocused(false);
    try {
      if (kind === "RUN") setRunResult(await codingApi.runCode({ questionId, sourceCode, language }));
      else setSubmitResult(await codingApi.submitCode(questionId, sourceCode, language));
    } catch (cause) {
      setRunError(formatApiError(cause, "Could not complete the judge request."));
    } finally { setAction(null); }
  }

  async function loadFeedback() {
    if (!submitResult) return;
    setFeedbackLoading(true);
    setFeedbackError("");
    try { setFeedback(await codingApi.getAiFeedback(submitResult.id)); }
    catch (cause) { setFeedbackError(formatApiError(cause, "Could not load AI feedback.")); }
    finally { setFeedbackLoading(false); }
  }

  if (loading) return <div className="coding-loading page-state">Loading coding workspace…</div>;
  if (loadError || !question) return <section className="content-page"><div className="notice-panel error-panel" role="alert">{loadError || "Question not found."}</div><Link to="/questions">Back to questions</Link></section>;

  const judge = runResult;
  const failedCase = judge?.failedTestCase ?? submitResult?.failedTestCase ?? null;
  const status = judge?.status ?? submitResult?.result ?? submitResult?.status ?? null;
  const current = testCases[selectedCase];
  const actual = judge?.testCaseResults?.[selectedCase]?.actualOutput;
  const passed = judge?.testCaseResults?.[selectedCase]?.passed;

  const examples = questionExamples(question.examples);
  return <section className={`coding-page${problemCollapsed || editorFocused ? " problem-hidden" : ""}${panelExpanded ? " results-expanded" : ""}${editorFocused ? " editor-focused" : ""}`}>
    <header className="workspace-topbar">
      <div className="workspace-title"><Link to={`/questions/${questionId}`} aria-label="Back to question">←</Link><div><h1>{question.title}</h1><span>{question.difficulty ?? "Difficulty not set"} · {question.type ?? "CODING"}</span></div></div>
      <div className="workspace-actions">{!editorFocused && <button className="text-button focus-toggle" onClick={() => { setPanelExpanded(false); setEditorFocused(true); }}>Focus editor</button>}{editorFocused && <button className="text-button focus-toggle" onClick={() => setEditorFocused(false)}>Exit focus</button>}<label className="language-select">Language<select value={language} onChange={event => setLanguage(event.target.value as ProgrammingLanguage)}>{languages.map(item => <option key={item} value={item}>{item}</option>)}</select></label><button className="secondary-button" disabled={action !== null} onClick={() => void execute("RUN")}>{action === "RUN" ? "Running…" : "Run"}</button><button className="primary-button workspace-submit" disabled={action !== null} onClick={() => void execute("SUBMIT")}>{action === "SUBMIT" ? "Submitting…" : "Submit"}</button></div>
    </header>

    <div className="workspace-body">
      {!panelExpanded && <>
        {!problemCollapsed && <aside className="workspace-problem">
          <div className="pane-heading"><h2>Problem</h2><button className="text-button" onClick={() => setProblemCollapsed(true)}>Hide</button></div>
          <div className="problem-scroll"><h3>{question.title}</h3><div className="workspace-prose">{readable(normalizeQuestionText(question.description))}</div>
            {question.constraints && <section><h4>Constraints</h4><div className="workspace-prose">{normalizeQuestionText(question.constraints)}</div></section>}
            {examples.length > 0 && <section className="examples-section"><h4>Examples</h4>{examples.map((example, index) => <pre className="workspace-example" key={index}>{example}</pre>)}</section>}
            {question.hints && <section><h4>Hints</h4><div className="workspace-prose">{normalizeQuestionText(question.hints)}</div></section>}
            {question.expectedComplexity && <section><h4>Expected Complexity</h4><div className="workspace-prose">{normalizeQuestionText(question.expectedComplexity)}</div></section>}
          </div>
        </aside>}
        <section className="workspace-editor" aria-label="Code editor">
          <div className="editor-heading">{problemCollapsed && !editorFocused && <button className="text-button" onClick={() => setProblemCollapsed(false)}>Show problem</button>}<span>Solution</span></div>
          <MonacoCodeEditor className="coding-monaco" ariaLabel="Solution source code" value={sourceCode} onChange={setSourceCode} language={monacoLanguages[language]} />
        </section>
      </>}
    </div>

    <section className={`workspace-results${panelCollapsed ? " is-collapsed" : ""}`} aria-label="Test cases and results">
      <div className="results-heading"><div><strong>{runResult || submitResult ? (submitResult ? "Submission result" : "Run result") : "Test cases"}</strong>{status && <span className={`judge-status status-${status.toLowerCase().replace(/[^a-z0-9]+/g, "-")}`}>{status.replace(/_/g, " ")}</span>}</div><div className="results-controls"><button className="text-button" onClick={() => { if (panelExpanded) setPanelExpanded(false); else { setPanelCollapsed(false); setPanelExpanded(true); } }}>{panelExpanded ? "Collapse results" : "Expand results"}</button><button className="text-button" onClick={() => { setPanelExpanded(false); setPanelCollapsed(value => !value); }}>{panelCollapsed ? "Show" : "Hide"}</button></div></div>
      {!panelCollapsed && <div className="results-content">
        {runError && <div className="workspace-error" role="alert">{runError}</div>}
        {submitResult && <div className="submission-summary"><p>Attempt <code>{submitResult.id}</code> saved · Attempt status: {submitResult.status.replace(/_/g, " ")} · Result: {submitResult.result?.replace(/_/g, " ") ?? "—"} · Score: {submitResult.score ?? "—"}</p>{submitResult.failedTestCase?.hidden && <p>A hidden test case failed. Its details are not shown.</p>}</div>}
        <div className="test-tabs" role="tablist" aria-label="Public test cases">{testCases.map((testCase, index) => <button key={testCase.id} className={index === selectedCase ? "active" : ""} role="tab" aria-selected={index === selectedCase} onClick={() => setSelectedCase(index)}>Case {index + 1}</button>)}{testCases.length === 0 && <span>No public test cases.</span>}</div>
        {judge && <div className="result-metrics"><span>{judge.passedTestCases ?? "—"} / {judge.totalTestCases ?? "—"} passed</span><span>{judge.executionTime == null ? "—" : `${judge.executionTime} ms`}</span><span>{judge.memoryUsed == null ? "—" : `${judge.memoryUsed} KB`}</span></div>}
        {current && <div className="case-io"><div><h4>Input</h4><pre>{current.input || "(empty)"}</pre></div><div><h4>Expected output</h4><pre>{current.expectedOutput || "(empty)"}</pre></div>{judge && <div><h4>Actual output {passed === undefined ? "" : <span className={passed ? "case-passed" : "case-failed"}>{passed ? "Passed" : "Failed"}</span>}</h4><pre>{actual ?? "No output returned for this case."}</pre></div>}{submitResult && <div><h4>Per-case output</h4><pre>The saved submission response does not include individual test case output.</pre></div>}</div>}
        {failedCase && <div className="failed-case"><h4>Failed test case</h4>{failedCase.hidden ? <p>A hidden test case failed. Its details are not shown.</p> : <div className="case-io"><div><h4>Input</h4><pre>{failedCase.input || "(empty)"}</pre></div><div><h4>Expected</h4><pre>{failedCase.expectedOutput || "(empty)"}</pre></div><div><h4>Actual</h4><pre>{failedCase.actualOutput || "(empty)"}</pre></div></div>}</div>}
        {(judge?.compilerOutput || submitResult?.compilerOutput) && <div className="output-block"><h4>Compiler output</h4><pre>{judge?.compilerOutput ?? submitResult?.compilerOutput}</pre></div>}
        {(judge?.runtimeOutput || submitResult?.runtimeOutput) && <div className="output-block"><h4>Runtime output</h4><pre>{judge?.runtimeOutput ?? submitResult?.runtimeOutput}</pre></div>}
        {submitResult && <div className="feedback-area">{!feedback && <button className="text-button" disabled={feedbackLoading} onClick={() => void loadFeedback()}>{feedbackLoading ? "Loading feedback…" : "Get AI Feedback"}</button>}{feedbackError && <p className="workspace-error">{feedbackError}</p>}{feedback && <div className="feedback-grid">{Object.entries({ Summary: feedback.summary, Mistake: feedback.mistake, Explanation: feedback.explanation, Suggestion: feedback.suggestion, "Complexity analysis": feedback.complexityAnalysis, "Optimized approach": feedback.optimizedApproach }).filter(([, value]) => value).map(([label, value]) => <div key={label}><h4>{label}</h4><p>{value}</p></div>)}</div>}</div>}
      </div>}
    </section>
  </section>;
}
