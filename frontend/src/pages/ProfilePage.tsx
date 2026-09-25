import { FormEvent, useEffect, useState } from "react";
import { ApiError, formatApiError } from "../api/client";
import * as profileApi from "../api/profileApi";
import type { ProfileFormValues, UserProfile } from "../api/profileApi";
import { useAuth } from "../context/AuthContext";

const EMPTY_FORM: ProfileFormValues = {
  fullName: "", bio: "", college: "", branch: "", graduationYear: "",
  githubUrl: "", linkedinUrl: "", portfolioUrl: "", preferredLanguage: "", targetRole: "", targetCompany: "",
};

const languageLabels: Record<NonNullable<UserProfile["preferredLanguage"]>, string> = {
  JAVA: "Java", PYTHON: "Python", CPP: "C++", JAVASCRIPT: "JavaScript", GO: "Go",
};
const roleLabels: Record<NonNullable<UserProfile["targetRole"]>, string> = {
  BACKEND_DEVELOPER: "Backend Developer",
  FRONTEND_DEVELOPER: "Frontend Developer",
  FULLSTACK_DEVELOPER: "Full Stack Developer",
  DEVOPS_ENGINEER: "DevOps Engineer",
  MACHINE_LEARNING_ENGINEER: "Machine Learning Engineer",
};

function toForm(profile: UserProfile): ProfileFormValues {
  return {
    fullName: profile.fullName ?? "",
    bio: profile.bio ?? "",
    college: profile.college ?? "",
    branch: profile.branch ?? "",
    graduationYear: profile.graduationYear == null ? "" : String(profile.graduationYear),
    githubUrl: profile.githubUrl ?? "",
    linkedinUrl: profile.linkedinUrl ?? "",
    portfolioUrl: profile.portfolioUrl ?? "",
    preferredLanguage: profile.preferredLanguage ?? "",
    targetRole: profile.targetRole ?? "",
    targetCompany: profile.targetCompany ?? "",
  };
}

function safeExternalUrl(value: string | null): string | null {
  if (!value) return null;
  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:" ? url.href : null;
  } catch {
    return null;
  }
}

function validateProfile(values: ProfileFormValues): string | null {
  const name = values.fullName.trim();
  if (name.length < 3 || name.length > 100) return "Full name must be between 3 and 100 characters.";
  if (values.bio.length > 500) return "Bio must be 500 characters or fewer.";
  if (values.college.length > 100 || values.branch.length > 100 || values.targetCompany.length > 100) {
    return "College, branch, and target company must be 100 characters or fewer.";
  }
  if (values.graduationYear) {
    const year = Number(values.graduationYear);
    if (!Number.isInteger(year) || year < 2000 || year > 2100) return "Graduation year must be a whole year from 2000 to 2100.";
  }
  if (values.githubUrl.trim() && !/^https:\/\/(www\.)?github\.com\/[A-Za-z0-9-]+\/?$/.test(values.githubUrl.trim())) {
    return "Enter a valid GitHub profile URL, such as https://github.com/username.";
  }
  if (values.linkedinUrl.trim() && !/^https:\/\/(www\.)?linkedin\.com\/in\/[A-Za-z0-9-_%]+\/?$/.test(values.linkedinUrl.trim())) {
    return "Enter a valid LinkedIn profile URL, such as https://www.linkedin.com/in/username.";
  }
  if (values.portfolioUrl.trim()) {
    const url = safeExternalUrl(values.portfolioUrl.trim());
    if (!url) return "Enter a valid portfolio URL beginning with http:// or https://.";
  }
  return null;
}

export function ProfilePage() {
  const { user } = useAuth();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [values, setValues] = useState<ProfileFormValues>(EMPTY_FORM);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    let active = true;
    profileApi.getProfile()
      .then(existing => {
        if (!active) return;
        setProfile(existing);
        setValues(toForm(existing));
      })
      .catch(cause => {
        if (!active) return;
        if (cause instanceof ApiError && cause.status === 404) {
          setCreating(true);
        } else {
          setError(formatApiError(cause, "Could not load your profile."));
        }
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  function change<K extends keyof ProfileFormValues>(key: K, value: ProfileFormValues[K]) {
    setValues(current => ({ ...current, [key]: value }));
    setError("");
    setSuccess("");
  }

  function cancelEdit() {
    if (profile) setValues(toForm(profile));
    setEditing(false);
    setError("");
    setSuccess("");
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const validationError = validateProfile(values);
    if (validationError) { setError(validationError); return; }

    setSaving(true);
    setError("");
    setSuccess("");
    try {
      const saved = creating ? await profileApi.createProfile(values) : await profileApi.updateProfile(values);
      setProfile(saved);
      setValues(toForm(saved));
      setCreating(false);
      setEditing(false);
      setSuccess(creating ? "Profile created." : "Profile updated.");
    } catch (cause) {
      setError(formatApiError(cause, "Could not save your profile."));
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <section className="content-page profile-page"><p className="page-state">Loading profile…</p></section>;

  const formVisible = creating || editing;
  const developerLinks = [
    ["GitHub", profile?.githubUrl ?? null],
    ["LinkedIn", profile?.linkedinUrl ?? null],
    ["Portfolio", profile?.portfolioUrl ?? null],
  ] as const;

  return <section className="content-page profile-page">
    <header className="profile-header">
      <div className="profile-identity">
        <div className="profile-avatar" aria-hidden="true">{profile?.fullName?.trim().charAt(0).toUpperCase() || user?.email?.charAt(0).toUpperCase() || "I"}</div>
        <div><p className="eyebrow">YOUR ACCOUNT</p><h1>{profile?.fullName || (creating ? "Create your profile" : "Your profile")}</h1><p className="profile-account-meta">{user?.email} <span aria-hidden="true">·</span> {user?.role}</p></div>
      </div>
      {!formVisible && profile && <button className="secondary-button profile-edit-button" type="button" onClick={() => { setEditing(true); setSuccess(""); }}>Edit profile</button>}
    </header>

    {success && <p className="profile-success" role="status">{success}</p>}
    {error && !formVisible && <div className="notice-panel error-panel" role="alert">{error}<button className="text-button" type="button" onClick={() => { setError(""); setLoading(true); profileApi.getProfile().then(existing => { setProfile(existing); setValues(toForm(existing)); }).catch(cause => setError(formatApiError(cause, "Could not load your profile."))).finally(() => setLoading(false)); }}>Retry</button></div>}

    {formVisible && <form className="profile-form" onSubmit={event => void submit(event)} noValidate>
      <div className="profile-form-heading"><div><h2>{creating ? "Create Profile" : "Edit Profile"}</h2><p>Your email, account ID, and role are managed by your account and cannot be changed here.</p></div></div>
      {error && <div className="notice-panel error-panel" role="alert">{error}</div>}

      <fieldset className="profile-fieldset">
        <legend>Personal Information</legend>
        <div className="profile-fields">
          <label className="profile-field profile-field-wide">Full name <span className="required-mark">Required</span>
            <input value={values.fullName} onChange={event => change("fullName", event.target.value)} required minLength={3} maxLength={100} autoComplete="name" />
          </label>
          <label className="profile-field profile-field-wide">Bio
            <textarea value={values.bio} onChange={event => change("bio", event.target.value)} maxLength={500} rows={4} placeholder="A short introduction" />
            <span className="profile-field-hint">{values.bio.length}/500</span>
          </label>
          <label className="profile-field">College<input value={values.college} onChange={event => change("college", event.target.value)} maxLength={100} /></label>
          <label className="profile-field">Branch<input value={values.branch} onChange={event => change("branch", event.target.value)} maxLength={100} /></label>
          <label className="profile-field">Graduation year<input type="number" inputMode="numeric" min={2000} max={2100} step={1} value={values.graduationYear} onChange={event => change("graduationYear", event.target.value)} placeholder="e.g. 2027" /></label>
        </div>
      </fieldset>

      <fieldset className="profile-fieldset">
        <legend>Interview Preferences</legend>
        <div className="profile-fields">
          <label className="profile-field">Preferred programming language
            <select value={values.preferredLanguage} onChange={event => change("preferredLanguage", event.target.value as ProfileFormValues["preferredLanguage"])}>
              <option value="">Select a language</option><option value="JAVA">Java</option><option value="PYTHON">Python</option><option value="CPP">C++</option><option value="JAVASCRIPT">JavaScript</option><option value="GO">Go</option>
            </select>
          </label>
          <label className="profile-field">Target role
            <select value={values.targetRole} onChange={event => change("targetRole", event.target.value as ProfileFormValues["targetRole"])}>
              <option value="">Select a role</option><option value="BACKEND_DEVELOPER">Backend Developer</option><option value="FRONTEND_DEVELOPER">Frontend Developer</option><option value="FULLSTACK_DEVELOPER">Full Stack Developer</option><option value="DEVOPS_ENGINEER">DevOps Engineer</option><option value="MACHINE_LEARNING_ENGINEER">Machine Learning Engineer</option>
            </select>
          </label>
          <label className="profile-field">Target company<input value={values.targetCompany} onChange={event => change("targetCompany", event.target.value)} maxLength={100} placeholder="e.g. Acme" /></label>
        </div>
      </fieldset>

      <fieldset className="profile-fieldset">
        <legend>Developer Links</legend>
        <div className="profile-fields">
          <label className="profile-field">GitHub<input type="url" value={values.githubUrl} onChange={event => change("githubUrl", event.target.value)} placeholder="https://github.com/username" /></label>
          <label className="profile-field">LinkedIn<input type="url" value={values.linkedinUrl} onChange={event => change("linkedinUrl", event.target.value)} placeholder="https://www.linkedin.com/in/username" /></label>
          <label className="profile-field profile-field-wide">Portfolio<input type="url" value={values.portfolioUrl} onChange={event => change("portfolioUrl", event.target.value)} placeholder="https://your-portfolio.example" /></label>
        </div>
      </fieldset>

      <div className="profile-form-actions"><button className="primary-button profile-save-button" type="submit" disabled={saving}>{saving ? "Saving…" : creating ? "Create profile" : "Save changes"}</button>{editing && <button className="secondary-button" type="button" disabled={saving} onClick={cancelEdit}>Cancel</button>}</div>
    </form>}

    {!formVisible && profile && <div className="profile-sections">
      <section className="profile-section"><h2>Personal Information</h2><dl className="profile-value-grid">
        <div><dt>Full name</dt><dd>{profile.fullName || "Not provided"}</dd></div>
        <div className="profile-value-wide"><dt>Bio</dt><dd className="profile-bio">{profile.bio || "Not provided"}</dd></div>
        <div><dt>College</dt><dd>{profile.college || "Not provided"}</dd></div>
        <div><dt>Branch</dt><dd>{profile.branch || "Not provided"}</dd></div>
        <div><dt>Graduation year</dt><dd>{profile.graduationYear ?? "Not provided"}</dd></div>
      </dl></section>
      <section className="profile-section"><h2>Interview Preferences</h2><dl className="profile-value-grid">
        <div><dt>Preferred programming language</dt><dd>{profile.preferredLanguage ? languageLabels[profile.preferredLanguage] : "Not provided"}</dd></div>
        <div><dt>Target role</dt><dd>{profile.targetRole ? roleLabels[profile.targetRole] : "Not provided"}</dd></div>
        <div><dt>Target company</dt><dd>{profile.targetCompany || "Not provided"}</dd></div>
      </dl></section>
      <section className="profile-section"><h2>Developer Links</h2><dl className="profile-value-grid profile-link-grid">
        {developerLinks.map(([label, value]) => {
          const href = safeExternalUrl(value);
          return <div key={label}><dt>{label}</dt><dd>{href ? <a href={href} target="_blank" rel="noreferrer">{value}</a> : value ? "Invalid link" : "Not provided"}</dd></div>;
        })}
      </dl></section>
    </div>}
  </section>;
}
