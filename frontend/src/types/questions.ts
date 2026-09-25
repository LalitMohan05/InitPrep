export type Difficulty = "EASY" | "MEDIUM" | "HARD";
export type QuestionType = "CODING" | "THEORY" | "MCQ";
export type QuestionRole = "BACKEND_DEVELOPER" | "FRONTEND_DEVELOPER" | "FULL_STACK_DEVELOPER" | "DEVOPS_ENGINEER" | "MACHINE_LEARNING_ENGINEER";

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
  roles?: QuestionRole[];
}

export interface QuestionDetails {
  id: string;
  title: string;
  description: string;
  difficulty?: Difficulty | null;
  type?: QuestionType | null;
  constraints?: string | null;
  examples?: string | null;
  hints?: string | null;
  starterCode?: string | null;
  expectedComplexity?: string | null;
  options?: string | null;
  roles?: QuestionRole[];
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
  roles: QuestionRole[];
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
