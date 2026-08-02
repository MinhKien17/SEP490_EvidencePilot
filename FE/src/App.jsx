import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { LanguageProvider } from './context/LanguageContext';
import { ThemeProvider } from './context/ThemeContext';
import ProtectedRoute from './components/ProtectedRoute';
import ScrollToTop from './components/ScrollToTop';

import Home from './pages/home/index.jsx';
import Terms from './pages/Terms.jsx';
import Privacy from './pages/Privacy.jsx';
import About from './pages/About.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import Profile from './pages/Profile.jsx';
import AdminDashboard from './pages/Admin/AdminDashboard.jsx';

// INSTRUCTOR SUB-SYSTEM IMPORTS
import CollectionList from './pages/Instructor/CollectionList.jsx';
import CollectionDetail from './pages/Instructor/CollectionDetail.jsx';
import ReviewRequests from './pages/Instructor/ReviewRequests.jsx';
import InstructorDashboard from './pages/Instructor/Dashboard.jsx';
import ProjectManagement from './pages/Instructor/ProjectManagement.jsx';
import ProjectDetail from './pages/Instructor/ProjectDetail.jsx';

// STUDENT SUB-SYSTEM IMPORTS
import StudentProjects from './pages/Student/Projects.jsx';
import WorkspaceLayout from './pages/Student/WorkspaceLayout.jsx';

function App() {
  return (
    <BrowserRouter>
      <ScrollToTop />
      <AuthProvider>
        <LanguageProvider>
          <ThemeProvider>
          <Routes>
            {/* Public Entry Nodes */}
            <Route path="/" element={<Home />} />
            <Route path="/terms" element={<Terms />} />
            <Route path="/privacy" element={<Privacy />} />
            <Route path="/about" element={<About />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

            {/* Mở khóa trang Profile chung để test mock mượt mà */}
            <Route path="/profile" element={<Profile />} />

            {/* =========================================================================
                🔓 INSTRUCTOR & ADMIN CHẠY THẲNG (Đã gỡ ProtectedRoute)
               ========================================================================= */}
            <Route path="/instructor/profile" element={<Profile />} />
            <Route path="/admin/profile" element={<Profile />} />
            <Route path="/instructor/dashboard" element={<InstructorDashboard />} />
            <Route path="/instructor/projects" element={<ProjectManagement />} />
            <Route path="/instructor/projects/:id" element={<ProjectDetail />} />
            <Route path="/instructor/requests" element={<ReviewRequests />} />
            <Route path="/instructor/collections" element={<CollectionList />} />
            <Route path="/instructor/collections/:id" element={<CollectionDetail />} />           
            <Route path="/admin/dashboard" element={<AdminDashboard />} />
            <Route path="/student/projects" element={
              <ProtectedRoute allowedRoles={['STUDENT']}><StudentProjects /></ProtectedRoute>
            } />
            <Route path="/student/projects/:projectId" element={
              <ProtectedRoute allowedRoles={['STUDENT']}><WorkspaceLayout /></ProtectedRoute>
            } />
            
          </Routes>
          </ThemeProvider>
        </LanguageProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
