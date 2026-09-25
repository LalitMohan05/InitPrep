/** Normalize legacy strings whose newlines were stored as the two characters \\ and n. */
export function normalizeQuestionText(value?: string | null, starterCode = false): string {
  if (!value || value.includes("\n") || !value.includes("\\n")) return value ?? "";

  if (starterCode) {
    const escapedNewlines = value.match(/\\n/g)?.length ?? 0;
    const looksLikeCode = /\b(class|def|function|public|import|package)\b|[{}]/.test(value);
    if (escapedNewlines < 2 || !looksLikeCode) return value;
  }

  return value.replace(/\\n/g, "\n");
}

export function questionExamples(value?: string | null): string[] {
  const normalized = normalizeQuestionText(value);
  return normalized.split(/\n\s*\n/).map(example => example.trim()).filter(Boolean);
}
