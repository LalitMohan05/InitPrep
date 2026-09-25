import { Navigate, Route, Routes } from "react-router-dom";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { AppLayout } from "./layouts/AppLayout";
import { LoginPage } from "./pages/LoginPage";
import { PlaceholderPage } from "./pages/PlaceholderPage";
import { RegisterPage } from "./pages/RegisterPage";
import { QuestionsPage } from "./pages/QuestionsPage";
import { QuestionDetailsPage } from "./pages/QuestionDetailsPage";
import { TestCaseManagerPage } from "./pages/TestCaseManagerPage";
import { AdminRoute } from "./components/AdminRoute";
import { CodingWorkspacePage } from "./pages/CodingWorkspacePage";
import { useAuth } from "./context/AuthContext";

function HomeRedirect() {
  const { user, loading } = useAuth();
  if (loading) return <main className="page-state">Loading…</main>;
  return <Navigate to={user ? "/dashboard" : "/login"} replace />;
}

export function App() {
  return <Routes>
    <Route element={<AppLayout />}>
      <Route path="/" element={<HomeRedirect />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/dashboard" element={<PlaceholderPage title="Dashboard" />} />
        <Route path="/questions" element={<QuestionsPage />} />
        <Route path="/questions/:questionId/solve" element={<CodingWorkspacePage />} />
        <Route path="/questions/:questionId" element={<QuestionDetailsPage />} />
        <Route path="/attempts" element={<PlaceholderPage title="My Attempts" />} />
        <Route path="/attempts/:attemptId" element={<PlaceholderPage title="Attempt Details" />} />
        <Route path="/profile" element={<PlaceholderPage title="Profile" />} />
        <Route element={<AdminRoute />}>
          <Route path="/manage-questions" element={<QuestionsPage managementMode />} />
          <Route path="/manage-test-cases" element={<TestCaseManagerPage />} />
          <Route path="/manage-test-cases/:questionId" element={<TestCaseManagerPage />} />
          <Route path="/questions/:questionId/test-cases" element={<TestCaseManagerPage />} />
        </Route>
      </Route>
      <Route path="*" element={<HomeRedirect />} />
    </Route>
  </Routes>;
}
