import { FormEvent, useState } from "react";
import { createQuestion } from "../api/questionsApi";
import type { Difficulty, QuestionType } from "../types/questions";

interface Props { onCreated(): void; }

export function QuestionForm({ onCreated }: Props) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formElement = event.currentTarget;
    setBusy(true);
    setError("");
    const form = new FormData(formElement);
    const ids = (name: string) => String(form.get(name) ?? "").split(",").map(value => value.trim()).filter(Boolean);
    const optional = (name: string) => String(form.get(name) ?? "").trim() || undefined;
    try {
      await createQuestion({
        title: String(form.get("title")).trim(),
        description: String(form.get("description")).trim(),
        type: String(form.get("type")) as QuestionType,
        difficulty: String(form.get("difficulty")) as Difficulty,
        constraints: optional("constraints"),
        examples: optional("examples"),
        hints: optional("hints"),
        starterCode: optional("starterCode"),
        expectedComplexity: optional("expectedComplexity"),
        options: optional("options"),
        correctAnswer: optional("correctAnswer"),
        companyIds: ids("companyIds"),
        topicIds: ids("topicIds"),
      });
      formElement.reset();
      onCreated();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Could not create the question.");
    } finally {
      setBusy(false);
    }
  }

  return <details className="management-panel" open>
    <summary>Create question</summary>
    <form className="data-form" onSubmit={submit}>
      <label>Title<input name="title" maxLength={200} required /></label>
      <label>Description<textarea name="description" rows={5} required /></label>
      <div className="form-row">
        <label>Question type<select name="type" defaultValue="CODING"><option>CODING</option><option>THEORY</option><option>MCQ</option></select></label>
        <label>Difficulty<select name="difficulty" defaultValue="EASY"><option>EASY</option><option>MEDIUM</option><option>HARD</option></select></label>
      </div>
      <label>Constraints<textarea name="constraints" rows={2} /></label>
      <label>Examples<textarea name="examples" rows={3} /></label>
      <label>Hints<textarea name="hints" rows={2} /></label>
      <label>Starter code<textarea name="starterCode" rows={3} /></label>
      <label>Expected complexity<input name="expectedComplexity" maxLength={100} /></label>
      <label>Options<textarea name="options" rows={3} /></label>
      <label>Correct answer<input name="correctAnswer" /></label>
      <label>Company IDs <span className="field-hint">Optional; enter UUIDs separated by commas.</span><input name="companyIds" /></label>
      <label>Topic IDs <span className="field-hint">Optional; enter UUIDs separated by commas.</span><input name="topicIds" /></label>
      {error && <p className="error-message" role="alert">{error}</p>}
      <button className="primary-button compact-button" disabled={busy}>{busy ? "Creating…" : "Create question"}</button>
    </form>
  </details>;
}
