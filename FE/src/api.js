import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080', 
  headers: {
    'Content-Type': 'application/json',
    // BẮT BUỘC: Thêm dòng này để vượt qua trang cảnh báo bảo mật mặc định của ngrok free
    'ngrok-skip-browser-warning': 'true',
  },
});

// Tự động đính kèm Token vào Header nếu có (phục vụ cho các API cần đăng nhập)
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;