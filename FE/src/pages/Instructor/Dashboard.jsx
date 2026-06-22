import { Link, useNavigate } from 'react-router-dom';

export default function Dashboard() {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem('token');
    navigate('/');
  };

  const today = new Date().toLocaleDateString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  return (
    <div className="min-h-screen bg-[#fcfcfc] font-sans text-[#333333]">
      
      {/* 1. Header (Màu Xanh Navy đậm y hệt trang Home) */}
      <header className="bg-[#1e3a8a] text-white border-b border-[#152e75] sticky top-0 z-10 shadow-sm">
        <div className="w-full px-8 h-16 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <span className="font-bold text-xl tracking-wider">Evidence Pilot</span>
          </div>
          <div className="flex items-center space-x-6">
            <div className="flex items-center space-x-2 bg-white/10 px-3 py-1.5 rounded border border-white/20">
              <div className="w-2 h-2 rounded-full bg-green-400 animate-pulse"></div>
              <span className="text-xs font-semibold text-blue-50 tracking-wide uppercase">Instructor Mode</span>
            </div>
            <button 
              onClick={handleLogout}
              className="text-sm font-medium text-blue-200 hover:text-white transition"
            >
              Sign Out
            </button>
          </div>
        </div>
      </header>

      {/* 2. Main Layout (Nền xám nhạt giống phần Features) */}
      <main className="max-w-6xl mx-auto px-6 py-10">
        
        {/* Welcome Banner */}
        <div className="mb-10 flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-white p-8 rounded border border-gray-200 shadow-sm">
          <div>
            <h1 className="text-3xl font-bold text-[#1e3a8a] mb-2">Welcome back, Professor! 👋</h1>
            <p className="text-gray-600 text-sm">Manage your datasets, evaluate student submissions, and track research evidence.</p>
          </div>
          <div className="bg-[#f0f4f8] px-4 py-2.5 rounded border border-gray-200 text-right md:self-center self-start">
            <p className="text-xs text-gray-500 font-semibold uppercase tracking-wider">Current Session</p>
            <p className="text-sm font-bold text-[#1e3a8a] mt-0.5">{today}</p>
          </div>
        </div>

        {/* Quick Statistics Overview */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 mb-12">
          <div className="bg-white p-5 rounded border border-gray-200 shadow-sm flex items-center space-x-4">
            <div className="p-3 bg-blue-50 text-[#1e3a8a] rounded">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 7v10c0 2.21 3.58 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.58 4 8 4s8-1.79 8-4M4 7c0-2.21 3.58-4 8-4s8 1.79 8 4m0 5c0 2.21-3.58 4-8 4s-8-1.79-8-4" /></svg>
            </div>
            <div>
              <p className="text-xs font-bold text-gray-400 uppercase tracking-wider">System Datasets</p>
              <p className="text-xl font-bold text-[#333333] mt-0.5">Active</p>
            </div>
          </div>
          <div className="bg-white p-5 rounded border border-gray-200 shadow-sm flex items-center space-x-4">
            <div className="p-3 bg-amber-50 text-amber-600 rounded">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
            </div>
            <div>
              <p className="text-xs font-bold text-gray-400 uppercase tracking-wider">Pending Claims</p>
              <p className="text-xl font-bold text-[#333333] mt-0.5">Awaiting Review</p>
            </div>
          </div>
          <div className="bg-white p-5 rounded border border-gray-200 shadow-sm flex items-center space-x-4">
            <div className="p-3 bg-green-50 text-green-600 rounded">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" /></svg>
            </div>
            <div>
              <p className="text-xs font-bold text-gray-400 uppercase tracking-wider">Account Status</p>
              <p className="text-xl font-bold text-[#333333] mt-0.5">Verified</p>
            </div>
          </div>
        </div>

        {/* 3. Main Workspaces (Thiết kế Card giống hệt section "Why use Evidence Pilot?") */}
        <h2 className="text-2xl font-bold text-center text-[#1e3a8a] mb-8">Main Workspaces</h2>
        
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          
          {/* Profile Card */}
          <Link 
            to="/profile" 
            className="group bg-white p-6 rounded border border-gray-200 shadow-sm hover:shadow-md transition-all text-left flex flex-col justify-between"
          >
            <div>
              <div className="text-xl font-semibold text-[#1e3a8a] mb-2 flex items-center">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5.121 17.804A13.937 13.937 0 0112 16c2.5 0 4.847.655 6.879 1.804M15 10a3 3 0 11-6 0 3 3 0 016 0zm6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                My Profile
              </div>
              <p className="text-sm text-gray-600 leading-relaxed">
                View your personal account details, manage academic credentials, and configure notification preferences.
              </p>
            </div>
            <div className="mt-6 pt-4 border-t border-gray-100 text-sm font-semibold text-[#2563eb] group-hover:text-[#1d4ed8] transition flex items-center">
              Go to Profile <span className="ml-1 transform group-hover:translate-x-1 transition-transform">→</span>
            </div>
          </Link>

          {/* Review Requests Card */}
          <Link 
            to="/instructor/requests" 
            className="group bg-white p-6 rounded border border-gray-200 shadow-sm hover:shadow-md transition-all text-left flex flex-col justify-between"
          >
            <div>
              <div className="text-xl font-semibold text-[#1e3a8a] mb-2 flex items-center">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-4 7h4m-4 4h4m-1-8h.01" /></svg>
                Review Requests
              </div>
              <p className="text-sm text-gray-600 leading-relaxed">
                Evaluate data verification claims submitted by students, audit uploaded files, and provide structured feedback.
              </p>
            </div>
            <div className="mt-6 pt-4 border-t border-gray-100 text-sm font-semibold text-[#2563eb] group-hover:text-[#1d4ed8] transition flex items-center">
              Review Submissions <span className="ml-1 transform group-hover:translate-x-1 transition-transform">→</span>
            </div>
          </Link>

          {/* Manage Dataset Card */}
          <Link 
            to="/instructor/dataset" 
            className="group bg-white p-6 rounded border border-gray-200 shadow-sm hover:shadow-md transition-all text-left flex flex-col justify-between"
          >
            <div>
              <div className="text-xl font-semibold text-[#1e3a8a] mb-2 flex items-center">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 13h6m-3-3v6m-9 1V7a2 2 0 012-2h6l2 2h6a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2z" /></svg>
                Manage Datasets
              </div>
              <p className="text-sm text-gray-600 leading-relaxed">
                Upload raw reference documents, create semantic knowledge baselines, and organize core RAG source materials.
              </p>
            </div>
            <div className="mt-6 pt-4 border-t border-gray-100 text-sm font-semibold text-[#2563eb] group-hover:text-[#1d4ed8] transition flex items-center">
              Manage Datasets <span className="ml-1 transform group-hover:translate-x-1 transition-transform">→</span>
            </div>
          </Link>

        </div>
      </main>
    </div>
  );
}