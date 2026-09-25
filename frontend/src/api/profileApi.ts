import { apiRequest } from "./client";

export type PreferredLanguage = "JAVA" | "PYTHON" | "CPP" | "JAVASCRIPT" | "GO";
export type TargetRole =
  | "BACKEND_DEVELOPER"
  | "FRONTEND_DEVELOPER"
  | "FULLSTACK_DEVELOPER"
  | "DEVOPS_ENGINEER"
  | "MACHINE_LEARNING_ENGINEER";

export interface UserProfile {
  id: string;
  userId: string;
  fullName: string;
  bio: string | null;
  college: string | null;
  branch: string | null;
  graduationYear: number | null;
  githubUrl: string | null;
  linkedinUrl: string | null;
  portfolioUrl: string | null;
  preferredLanguage: PreferredLanguage | null;
  targetRole: TargetRole | null;
  targetCompany: string | null;
}

export interface ProfileFormValues {
  fullName: string;
  bio: string;
  college: string;
  branch: string;
  graduationYear: string;
  githubUrl: string;
  linkedinUrl: string;
  portfolioUrl: string;
  preferredLanguage: PreferredLanguage | "";
  targetRole: TargetRole | "";
  targetCompany: string;
}

export function getProfile(): Promise<UserProfile> {
  return apiRequest("/api/users/profile");
}

export function createProfile(values: ProfileFormValues): Promise<UserProfile> {
  return apiRequest("/api/users/profile", {
    method: "POST",
    body: JSON.stringify(toRequest(values)),
  });
}

export function updateProfile(values: ProfileFormValues): Promise<UserProfile> {
  return apiRequest("/api/users/profile", {
    method: "PATCH",
    body: JSON.stringify(toRequest(values)),
  });
}

function toRequest(values: ProfileFormValues) {
  return {
    fullName: values.fullName.trim(),
    bio: optionalText(values.bio),
    college: optionalText(values.college),
    branch: optionalText(values.branch),
    graduationYear: values.graduationYear ? Number(values.graduationYear) : null,
    githubUrl: optionalText(values.githubUrl),
    linkedinUrl: optionalText(values.linkedinUrl),
    portfolioUrl: optionalText(values.portfolioUrl),
    preferredLanguage: values.preferredLanguage || null,
    targetRole: values.targetRole || null,
    targetCompany: optionalText(values.targetCompany),
  };
}

function optionalText(value: string): string | null {
  return value.trim() || null;
}
