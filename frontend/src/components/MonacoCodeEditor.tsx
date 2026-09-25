import Editor from "@monaco-editor/react";
import { useTheme } from "../context/ThemeContext";

interface Props {
  value: string;
  onChange(value: string): void;
  language?: string;
  className?: string;
  height?: string;
  ariaLabel: string;
  readOnly?: boolean;
}

export function MonacoCodeEditor({ value, onChange, language = "java", className, height = "100%", ariaLabel, readOnly = false }: Props) {
  const { theme } = useTheme();

  return <div className={className} aria-label={ariaLabel}>
    <Editor
      height={height}
      language={language}
      theme={theme === "dark" ? "vs-dark" : "light"}
      value={value}
      onChange={nextValue => onChange(nextValue ?? "")}
      options={{
        ariaLabel,
        readOnly,
        domReadOnly: readOnly,
        automaticLayout: true,
        minimap: { enabled: false },
        wordWrap: "off",
        lineNumbers: "on",
        tabSize: 4,
        insertSpaces: true,
        matchBrackets: "always",
        bracketPairColorization: { enabled: true },
        scrollBeyondLastLine: false,
        scrollbar: { horizontal: "auto", vertical: "auto" },
        fontSize: 14,
        lineHeight: 24,
        padding: { top: 16, bottom: 16 },
      }}
    />
  </div>;
}
