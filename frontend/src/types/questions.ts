export type Difficulty = "EASY" | "MEDIUM" | "HARD";
export type QuestionType = "CODING" | "THEORY" | "MCQ";

export interface CompanySummary {
  id: string;
  name: string;
}

export interface TopicSummary {
  id: string;
  name: string;
}

export interface QuestionSummary {
  id: string;
  title: string;
  type: QuestionType;
  difficulty: Difficulty;
  companies: CompanySummary[];
  topics: TopicSummary[];
}

export interface QuestionDetails {
  id: string;
  title: string;
  description: string;
}

export interface QuestionPage {
  content: QuestionSummary[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface QuestionWriteRequest {
  title: string;
  description: string;
  type: QuestionType;
  difficulty: Difficulty;
  constraints?: string;
  examples?: string;
  hints?: string;
  starterCode?: string;
  expectedComplexity?: string;
  options?: string;
  correctAnswer?: string;
  companyIds?: string[];
  topicIds?: string[];
}

export interface TestCase {
  id: string;
  input: string;
  expectedOutput: string;
  hidden: boolean;
}

export interface TestCaseWriteRequest {
  input: string;
  expectedOutput: string;
  hidden: boolean;
}
