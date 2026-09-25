export type ProgrammingLanguage = "JAVA" | "PYTHON" | "CPP" | "C" | "JAVASCRIPT";

export interface JudgeTestCase {
  id: string;
  input: string;
  expectedOutput: string;
  hidden: boolean;
}

export interface QuestionJudgeData {
  questionId: string;
  testCases: JudgeTestCase[];
}

export interface CodingFeedback {
  summary: string | null;
  mistake: string | null;
  explanation: string | null;
  suggestion: string | null;
  complexityAnalysis: string | null;
  optimizedApproach: string | null;
}

export interface RunCodeRequest {
  questionId: string;
  sourceCode: string;
  language: ProgrammingLanguage;
}

export interface TestCaseResult {
  passed: boolean;
  input: string;
  expectedOutput: string;
  actualOutput: string;
  hidden: boolean;
}

export interface JudgeSubmissionResponse {
  status: string;
  passedTestCases: number | null;
  totalTestCases: number | null;
  executionTime: number | null;
  memoryUsed: number | null;
  compilerOutput: string | null;
  runtimeOutput: string | null;
  testCaseResults: TestCaseResult[] | null;
  failedTestCase: TestCaseResult | null;
}

export interface AttemptResponse {
  id: string;
  questionId: string;
  type?: "CODING" | "THEORY" | "MCQ" | null;
  answer?: string | null;
  language: string | null;
  status: string;
  result: string | null;
  score: number | null;
  passedTestCases?: number | null;
  totalTestCases?: number | null;
  executionTime?: number | null;
  memoryUsed?: number | null;
  compilerOutput: string | null;
  runtimeOutput: string | null;
  failedTestCase: TestCaseResult | null;
  createdAt: string;
  updatedAt: string;
}

export interface AttemptPage {
  content: AttemptResponse[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
