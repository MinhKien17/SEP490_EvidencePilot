// import { BrowserRouter, Routes, Route } from 'react-router-dom';
// import { AuthProvider } from './context/AuthContext';
// import { LanguageProvider } from './context/LanguageContext';
// import ProtectedRoute from './components/ProtectedRoute';

// import Home from './pages/Home.jsx';
// import Login from './pages/Login.jsx';
// import Register from './pages/Register.jsx';
// import Profile from './pages/Profile.jsx';
// import AdminDashboard from './pages/Admin/AdminDashboard.jsx';

// // INSTRUCTOR SUB-SYSTEM IMPORTS
// import CollectionList from './pages/Instructor/CollectionList.jsx';
// import CreateCollection from './pages/Instructor/CreateCollection.jsx';
// import ReviewRequests from './pages/Instructor/ReviewRequests.jsx';
// import InstructorDashboard from './pages/Instructor/Dashboard.jsx';

// // STUDENT SUB-SYSTEM IMPORTS
// import StudentProjects from './pages/Student/Projects.jsx';
// import Workspace from './pages/Student/Workspace.jsx';

// function App() {
//   return (
//     <BrowserRouter>
//       <AuthProvider>
//         <LanguageProvider>
//           <Routes>
//             {/* Public Entry Nodes */}
//             <Route path="/" element={<Home />} />
//             <Route path="/login" element={<Login />} />
//             <Route path="/register" element={<Register />} />

//             {/* Shared Authenticated Node (Giữ nguyên cho các thành phần chung nếu cần) */}
//             <Route path="/profile" element={
//               <ProtectedRoute><Profile /></ProtectedRoute>
//             } />

//             {/* =========================================================================
//                 🔥 CẬP NHẬT: THÊM ROUTE PROFILE DÀNH RIÊNG CHO INSTRUCTOR
//                 ========================================================================= */}
//             <Route path="/instructor/profile" element={
//               <ProtectedRoute allowedRoles={['INSTRUCTOR', 'ADMIN']}>
//                 <Profile /> 
//               </ProtectedRoute>
//             } />

//             {/* Instructor / Admin Telemetry Control Hubs */}
//             <Route path="/instructor/dashboard" element={
//               <ProtectedRoute allowedRoles={['INSTRUCTOR', 'ADMIN']}><InstructorDashboard /></ProtectedRoute>
//             } />
//             <Route path="/instructor/requests" element={
//               <ProtectedRoute allowedRoles={['INSTRUCTOR', 'ADMIN']}><ReviewRequests /></ProtectedRoute>
//             } />
            
//             {/* Đồng bộ cấu trúc route riêng biệt cho phân hệ Evidence Collection */}
//             <Route path="/instructor/collections" element={
//               <ProtectedRoute allowedRoles={['INSTRUCTOR', 'ADMIN']}><CollectionList /></ProtectedRoute>
//             } />
//             <Route path="/instructor/collections/create" element={
//               <ProtectedRoute allowedRoles={['INSTRUCTOR', 'ADMIN']}><CreateCollection /></ProtectedRoute>
//             } />

//             {/* Student Sandbox Execution Workspace */}
//             <Route path="/student/projects" element={
//               <ProtectedRoute allowedRoles={['STUDENT']}><StudentProjects /></ProtectedRoute>
//             } />
//             <Route path="/student/projects/:projectId" element={
//               <ProtectedRoute allowedRoles={['STUDENT']}><Workspace /></ProtectedRoute>
//             } />
            
//             {/* Admin Control Hub */}
//             <Route path="/admin/dashboard" element={
//               <ProtectedRoute allowedRoles={['ADMIN']}><AdminDashboard /></ProtectedRoute>
//             } />
//           </Routes>
//         </LanguageProvider>
//       </AuthProvider>
//     </BrowserRouter>
//   );
// }

// export default App;
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { LanguageProvider } from './context/LanguageContext';
import ProtectedRoute from './components/ProtectedRoute';

import Home from './pages/Home.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import Profile from './pages/Profile.jsx';
import AdminDashboard from './pages/Admin/AdminDashboard.jsx';

// INSTRUCTOR SUB-SYSTEM IMPORTS
import CollectionList from './pages/Instructor/CollectionList.jsx';
import CreateCollection from './pages/Instructor/CreateCollection.jsx';
import ReviewRequests from './pages/Instructor/ReviewRequests.jsx';
import InstructorDashboard from './pages/Instructor/Dashboard.jsx';

// STUDENT SUB-SYSTEM IMPORTS
import StudentProjects from './pages/Student/Projects.jsx';
import Workspace from './pages/Student/Workspace.jsx';

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <LanguageProvider>
          <Routes>
            {/* Public Entry Nodes */}
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

            {/* Mở khóa trang Profile chung để test mock mượt mà */}
            <Route path="/profile" element={<Profile />} />

            {/* =========================================================================
                🔓 INSTRUCTOR & ADMIN CHẠY THẲNG (Đã gỡ ProtectedRoute)
               ========================================================================= */}
            <Route path="/instructor/profile" element={<Profile />} />
            <Route path="/instructor/dashboard" element={<InstructorDashboard />} />
            <Route path="/instructor/requests" element={<ReviewRequests />} />
            <Route path="/instructor/collections" element={<CollectionList />} />
            <Route path="/instructor/collections/create" element={<CreateCollection />} />           
            <Route path="/admin/dashboard" element={<AdminDashboard />} />
            <Route path="/student/projects" element={
              <ProtectedRoute allowedRoles={['STUDENT']}><StudentProjects /></ProtectedRoute>
            } />
            <Route path="/student/projects/:projectId" element={
              <ProtectedRoute allowedRoles={['STUDENT']}><Workspace /></ProtectedRoute>
            } />
            
          </Routes>
        </LanguageProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;