import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Home from './pages/Home.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';

// BƯỚC 1: Import các trang mà bạn vừa làm vào đây
import Profile from './pages/Profile.jsx';
import ReviewRequests from './pages/Instructor/ReviewRequests.jsx';
import CreateDataset from './pages/Instructor/CreateDataset.jsx';
import Dashboard from './pages/Instructor/Dashboard.jsx'
import DatasetList from './pages/Instructor/DatasetList.jsx';
function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        {/* BƯỚC 2: Khai báo các tuyến đường mới để hết bị trắng trang */}
        <Route path="/profile" element={<Profile />} />
        
        {/* Các tuyến đường Instructor */}
        <Route path="/instructor/dashboard" element={<Dashboard />} />
        <Route path="/instructor/requests" element={<ReviewRequests />} />
        
        {/* FIX CHỖ NÀY: Chia rõ trang Danh sách và trang Tạo mới */}
        <Route path="/instructor/dataset" element={<DatasetList />} />
        <Route path="/instructor/dataset/create" element={<CreateDataset />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
