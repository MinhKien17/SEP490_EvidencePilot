import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Home from './pages/Home.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';

// BƯỚC 1: Import các trang mà bạn vừa làm vào đây
import Profile from './pages/Profile.jsx';
import ReviewRequests from './pages/Instructor/ReviewRequests.jsx';
import CreateDataset from './pages/Instructor/CreateDataset.jsx';
import Dashboard from './pages/Instructor/Dashboard.jsx'
function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        {/* BƯỚC 2: Khai báo các tuyến đường mới để hết bị trắng trang */}
        <Route path="/profile" element={<Profile />} />
        
        {/* Hãy chú ý gõ chính xác chữ thường khớp với URL trên trình duyệt nhé */}
        <Route path="/instructor/dashboard" element={<Dashboard />} />
        <Route path="/instructor/requests" element={<ReviewRequests />} />
        <Route path="/instructor/dataset" element={<CreateDataset />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
