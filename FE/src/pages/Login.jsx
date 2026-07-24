import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api.js';

export default function Login() {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [form, setForm] = useState({ email: '', passwordHash: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showGuideModal, setShowGuideModal] = useState(false);

  const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID || '1009187311116-v5gq2mlj91u639vj2lh7l9lqcf43fdt1.apps.googleusercontent.com';

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  async function handleSubmit(e) {
    if (e) e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const res = await api.post('/api/auth/login', {
        email: form.email,
        password: form.passwordHash
      });
      const token = res.data.token ?? res.data.accessToken ?? res.data.jwt;
      const role = res.data.user?.role ?? res.data.role;

      if (!token) throw new Error('Token not found in response');

      login(token, role);

      if (role === 'ADMIN') {
        navigate('/admin/dashboard');
      } else if (role === 'INSTRUCTOR') {
        navigate('/instructor/dashboard');
      } else {
        navigate('/student/projects');
      }
    } catch (err) {
      const msg = err.response?.data?.message
        ?? err.response?.data?.error
        ?? err.message
        ?? 'Login failed. Please check your credentials.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }

  async function handleFptGoogleLogin(email) {
    setError('');
    setLoading(true);
    try {
      // Try to log in with dev seed password 'Password@123' first
      const res = await api.post('/api/auth/login', {
        email,
        password: 'Password@123'
      });
      const token = res.data.token ?? res.data.accessToken ?? res.data.jwt;
      const role = res.data.user?.role ?? res.data.role;

      if (!token) throw new Error('Token not found in response');

      login(token, role);
      if (role === 'ADMIN') {
        navigate('/admin/dashboard');
      } else if (role === 'INSTRUCTOR') {
        navigate('/instructor/dashboard');
      } else {
        navigate('/student/projects');
      }
    } catch (err) {
      // Fallback: fill email and let user enter password manually
      setForm({ email, passwordHash: '' });
      const msg = err.response?.data?.message
        ?? err.response?.data?.error
        ?? err.message
        ?? 'Google login failed. Please enter password manually.';
      setError(msg + ' (Email has been populated)');
    } finally {
      setLoading(false);
    }
  }

  async function handleGoogleCredentialResponse(response) {
    setError('');
    try {
      const credential = response.credential;
      const base64Url = credential.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
      }).join(''));

      const payload = JSON.parse(jsonPayload);
      const email = payload.email;

      if (!email || !email.endsWith('@fpt.edu.vn')) {
        setError('Đăng nhập thất bại: Chỉ chấp nhận tài khoản Google FPT (@fpt.edu.vn).');
        return;
      }

      await handleFptGoogleLogin(email);
    } catch (err) {
      setError('Lỗi giải mã token Google. Vui lòng thử lại.');
    }
  }

  useEffect(() => {
    const initializeGoogleSignIn = () => {
      if (window.google) {
        try {
          window.google.accounts.id.initialize({
            client_id: googleClientId,
            callback: handleGoogleCredentialResponse
          });
          window.google.accounts.id.renderButton(
            document.getElementById('google-signin-button'),
            { 
              theme: 'outline', 
              size: 'large', 
              width: 380, 
              text: 'signin_with',
              shape: 'rectangular',
              logo_alignment: 'left'
            }
          );
        } catch (e) {
          console.warn('Google Identity Services initialization failed. Using fallback.');
        }
      }
    };

    if (!document.getElementById('google-gsi-script')) {
      const script = document.createElement('script');
      script.id = 'google-gsi-script';
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.defer = true;
      script.onload = initializeGoogleSignIn;
      document.body.appendChild(script);
    } else {
      const timer = setTimeout(initializeGoogleSignIn, 500);
      return () => clearTimeout(timer);
    }
  }, [googleClientId]);

  return (
    <div className="min-h-screen bg-gray-50 flex border-t-4 border-indigo-600">
      {/* Left Column: Form */}
      <div className="flex-1 flex items-center justify-center p-8 sm:p-12 lg:p-24 bg-white">
        <div className="w-full max-w-md relative">
          
          {/* Back to Home Button */}
          <div className="mb-8">
            <Link to="/" className="inline-flex items-center text-xs font-semibold text-gray-500 hover:text-indigo-600 transition-colors gap-1.5 group">
              <svg className="w-4 h-4 transform group-hover:-translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
              </svg>
              Quay lại Trang chủ
            </Link>
          </div>

          <div className="mb-10 text-center sm:text-left">
            <h1 className="text-3xl font-bold text-gray-900 mb-2">Welcome back</h1>
            <p className="text-gray-500">Sign in to Evidence Pilot to manage your projects.</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Email Address
              </label>
              <input
                type="email"
                name="email"
                value={form.email}
                onChange={handleChange}
                required
                placeholder="you@example.com"
                className="w-full border border-gray-300 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-600 focus:border-transparent transition-all shadow-sm"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Password
              </label>
              <input
                type="password"
                name="passwordHash"
                value={form.passwordHash}
                onChange={handleChange}
                required
                placeholder="••••••••"
                className="w-full border border-gray-300 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-600 focus:border-transparent transition-all shadow-sm"
              />
            </div>

            {error && (
              <div className="p-3 bg-red-50 text-red-600 rounded-lg text-sm border border-red-100">
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 bg-blue-600 text-white rounded-xl font-semibold hover:bg-blue-700 transition-all shadow-lg hover:shadow-blue-500/30 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? 'Signing in...' : 'Sign In'}
            </button>
          </form>

          {/* Social Sign-In Divider */}
          <div className="relative my-6">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-200"></div>
            </div>
            <div className="relative flex justify-center text-xs">
              <span className="px-2 bg-white text-gray-500">Đăng nhập nhanh với Google FPT</span>
            </div>
          </div>

          {/* Google Sign-In Button */}
          <div className="flex flex-col items-center justify-center gap-3">
            {/* Real Google OAuth Button */}
            <div id="google-signin-button" className="w-full flex justify-center z-20"></div>

            {/* Error fallback: invalid_client / OAuth client not found */}
            <div className="flex flex-col items-center gap-1.5 mt-1">
              <button
                type="button"
                onClick={() => setShowGuideModal(true)}
                className="text-[11px] text-gray-400 hover:text-gray-650 hover:underline transition-all cursor-pointer"
              >
                Làm sao để cấu hình Google Client ID thật?
              </button>
            </div>
          </div>

          <p className="text-center text-sm text-gray-600 mt-8">
            Don't have an account?{' '}
            <Link to="/register" className="text-blue-600 font-semibold hover:underline">
              Create an account
            </Link>
          </p>
        </div>
      </div>

      {/* Right Column: Decorative */}
      <div className="hidden lg:flex flex-1 relative overflow-hidden items-center justify-center">
        {/* Full screen background image */}
        <img 
          src="https://images.unsplash.com/photo-1550751827-4bd374c3f58b?q=80&w=1600&auto=format&fit=crop" 
          alt="Evidence Network" 
          className="absolute inset-0 w-full h-full object-cover"
        />
        {/* Dark overlay for text readability */}
        <div className="absolute inset-0 bg-blue-900/60 mix-blend-multiply"></div>
        <div className="absolute inset-0 bg-gradient-to-t from-blue-900/80 via-blue-900/20 to-transparent"></div>
        
        <div className="relative z-10 text-center px-12 max-w-lg flex flex-col items-center">
          <h2 className="text-4xl font-bold text-white mb-6 leading-tight drop-shadow-md">Manage Your Evidence Graphically</h2>
          <p className="text-blue-50 text-lg drop-shadow-md">
            Evidence Pilot provides an intuitive interface to link claims with source evidence securely and collaboratively.
          </p>
        </div>
      </div>

      {/* MODAL 1: GOOGLE CLIENT ID CONFIGURATION GUIDE */}
      {showGuideModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4 animate-in fade-in duration-200">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg p-6 border border-slate-100 flex flex-col gap-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                <svg className="w-5 h-5 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                Cấu hình Google Client ID thật
              </h3>
              <button
                onClick={() => setShowGuideModal(false)}
                className="text-slate-400 hover:text-slate-600 text-lg"
              >&times;</button>
            </div>
            
            <div className="text-sm text-slate-600 space-y-3 leading-relaxed">
              <p>Do Google bảo mật tên miền nghiêm ngặt, nút Đăng nhập Google thật chỉ hoạt động khi bạn sử dụng một Client ID được cấu hình đúng cho cổng chạy cục bộ (ví dụ: <code className="bg-slate-100 text-rose-600 px-1 py-0.5 rounded font-mono">http://localhost:5173</code>).</p>
              
              <div className="bg-amber-50 border-l-4 border-amber-500 p-3 rounded text-amber-800 text-xs">
                <strong>Lưu ý:</strong> Giao diện Đăng nhập Google đang báo lỗi <code>401: invalid_client</code> vì Client ID mặc định chưa được liên kết với tài khoản FPT của bạn hoặc đã hết hạn.
              </div>

              <h4 className="font-bold text-slate-800 mt-4">Các bước tự cấu hình trong 2 phút:</h4>
              <ol className="list-decimal list-inside space-y-2 text-slate-600 text-xs">
                <li>Truy cập <a href="https://console.cloud.google.com/" target="_blank" rel="noopener noreferrer" className="text-indigo-600 underline font-semibold">Google Cloud Console</a>.</li>
                <li>Tạo một dự án mới hoặc chọn dự án hiện tại.</li>
                <li>Vào mục <strong>APIs & Services</strong> &gt; <strong>Credentials</strong>.</li>
                <li>Click <strong>Create Credentials</strong> &gt; <strong>OAuth client ID</strong> (Chọn loại <strong>Web application</strong>).</li>
                <li>Trong mục <strong>Authorized JavaScript origins</strong>, thêm:
                  <code className="block bg-slate-100 p-1.5 rounded font-mono mt-1 text-slate-800 text-xs">http://localhost:5173</code>
                </li>
                <li>Click <strong>Create</strong>, copy mã <strong>Client ID</strong> nhận được.</li>
                <li>Mở file <code className="bg-slate-100 text-slate-800 px-1 py-0.5 rounded font-mono">.env</code> trong dự án của bạn và dán vào:
                  <code className="block bg-slate-100 p-1.5 rounded font-mono mt-1 text-slate-800 text-xs">VITE_GOOGLE_CLIENT_ID=mã_client_id_của_bạn</code>
                </li>
              </ol>
            </div>

            <div className="flex justify-end gap-3 border-t border-slate-100 pt-4 mt-2">
              <button
                type="button"
                onClick={() => setShowGuideModal(false)}
                className="px-4 py-2 bg-indigo-600 text-white font-bold rounded-xl text-sm hover:bg-indigo-700 transition-colors shadow-md cursor-pointer"
              >
                Đồng ý
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

