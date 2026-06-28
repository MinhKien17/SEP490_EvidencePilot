import { useState, useEffect } from 'react';
import api from '../api.js';

export default function Profile() {
  // --- 1. STATES MANAGEMENT ---
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  
  // Form input states
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  
  // Status alerts
  const [message, setMessage] = useState({ type: "", text: "" });

  // 🔥 TỰ ĐỘNG PHÁT HIỆN ROLE THEO URL NẾU API BACKEND GẶP SỰ CỐ KẾT NỐI
  const isInstructorRoute = window.location.pathname.includes('/instructor');
  const fallbackRole = isInstructorRoute ? 'INSTRUCTOR' : 'STUDENT';

  // --- 2. API INTEGRATION ---
  const fetchUserProfile = async () => {
    setLoading(true);
    try {
      const response = await api.get('/api/users/profile');
      const data = response.data;
      setUser(data);
      setFirstName(data.firstName || "");
      setLastName(data.lastName || "");
    } catch (error) {
      console.error("Failed to query user telemetry profile context:", error);
      showAlert("error", "Failed to load profile synchronization matrix from security context.");
    } finally {
      setLoading(false);
    }
  };

  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    if (!firstName.trim() || !lastName.trim()) {
      showAlert("error", "First name and last name fields cannot be blank execution nodes.");
      return;
    }

    setSubmitting(true);
    clearAlert();

    try {
      const payload = {
        firstName: firstName.trim(),
        lastName: lastName.trim()
      };

      const response = await api.put('/api/users/profile', payload);
      setUser(response.data);
      showAlert("success", "Profile metadata clusters updated successfully on backend core.");
    } catch (error) {
      console.error("Profile payload update rejected:", error);
      showAlert("error", error.response?.data?.message || "Validation error detected during profile synchronization.");
    } finally {
      setSubmitting(false);
    }
  };

  useEffect(() => {
    fetchUserProfile();
  }, []);

  const showAlert = (type, text) => setMessage({ type, text });
  const clearAlert = () => setMessage({ type: "", text: "" });

  if (loading) {
    return (
      <div className="min-h-screen bg-[#f8fafc] flex items-center justify-center font-sans">
        <div className="text-xs font-bold text-gray-400 tracking-widest animate-pulse uppercase">
          Querying secure user cluster metadata...
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#f8fafc] text-[#0f172a] p-8 font-sans">
      <div className="max-w-4xl mx-auto">
        
        {/* Workspace Dashboard Header */}
        <div className="mb-8 border-b border-gray-200 pb-6">
          <h1 className="text-3xl font-black text-[#1e3a8a] tracking-tight">Account Parameters</h1>
          <p className="text-xs text-gray-400 mt-1">
            Manage your personal cryptographic identification, profile identities, and institutional platform authority.
          </p>
        </div>

        {message.text && (
          <div className={`p-4 mb-6 rounded-2xl border text-xs font-bold transition animate-fadeIn ${
            message.type === 'success' 
              ? 'bg-emerald-50 border-emerald-100 text-emerald-700' 
              : 'bg-rose-50 border-rose-100 text-rose-700'
          }`}>
            {message.type === 'success' ? '✓' : '⚠️'} {message.text}
          </div>
        )}

        {/* Master Profile Architecture Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 items-start">
          
          {/* LEFT PANEL: Identity Security Token Badge */}
          <div className="bg-white rounded-3xl border border-gray-200 shadow-sm p-6 flex flex-col items-center text-center space-y-4">
            <div className="w-20 h-20 bg-gradient-to-tr from-[#1e3a8a] to-blue-500 rounded-2xl flex items-center justify-center text-white font-black text-2xl shadow-md">
              {user?.firstName?.charAt(0) || "U"}{user?.lastName?.charAt(0) || "P"}
            </div>
            
            <div className="space-y-1 w-full">
              <h2 className="font-black text-gray-900 text-base tracking-tight">
                {user?.firstName || "Faculty"}{user?.lastName ? ` ${user.lastName}` : " Member"}
              </h2>
              <p className="text-xs text-gray-400 font-medium truncate px-2">{user?.email || "sync_error@institution.edu"}</p>
            </div>

            <div className="w-full pt-4 border-t border-gray-100">
              {/* 🔥 ĐÃ SỬA: Tự động đổi màu và nội dung Badge theo Role thực tế */}
              <span className={`inline-block px-3 py-1 text-[9px] font-black tracking-widest uppercase rounded-lg border ${
                (user?.role || fallbackRole) === 'ADMIN' ? 'bg-rose-50 text-rose-700 border-rose-100' :
                (user?.role || fallbackRole) === 'INSTRUCTOR' ? 'bg-purple-50 text-purple-700 border-purple-100' :
                'bg-blue-50 text-[#1e3a8a] border-blue-100'
              }`}>
                Authority: {user?.role || fallbackRole}
              </span>
            </div>

            <div className="w-full bg-gray-50 rounded-xl p-3 text-[10px] text-left font-mono text-gray-400 border border-gray-100 break-all">
              <span className="block font-bold uppercase tracking-wide text-[8px] text-gray-500 mb-0.5">User Infrastructure Key:</span>
              {user?.id || "OFFLINE_CACHE_NODE"}
            </div>
          </div>

          {/* RIGHT PANEL: Self-Service Config Form */}
          <div className="md:col-span-2 space-y-6">
            
            {/* Identity Update Form */}
            <div className="bg-white rounded-3xl border border-gray-200 shadow-sm p-8">
              <h3 className="text-xs font-black text-gray-400 uppercase tracking-wider mb-6 pb-2 border-b border-gray-50">
                Identity Profile Definitions
              </h3>
              
              <form onSubmit={handleUpdateProfile} className="space-y-5 text-xs">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {/* Field: First Name */}
                  <div className="space-y-1.5">
                    <label className="text-gray-500 font-black uppercase tracking-wide text-[10px]">First Name</label>
                    <input 
                      type="text"
                      value={firstName}
                      onChange={(e) => setFirstName(e.target.value)}
                      placeholder="Enter identity first name"
                      className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition"
                    />
                  </div>

                  {/* Field: Last Name */}
                  <div className="space-y-1.5">
                    <label className="text-gray-500 font-black uppercase tracking-wide text-[10px]">Last Name</label>
                    <input 
                      type="text"
                      value={lastName}
                      onChange={(e) => setLastName(e.target.value)}
                      placeholder="Enter identity last name"
                      className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition"
                    />
                  </div>
                </div>

                <div className="flex justify-end pt-4 border-t border-gray-100">
                  <button
                    type="submit"
                    disabled={submitting}
                    className="px-6 py-3 bg-[#1e3a8a] text-white font-black rounded-xl hover:bg-blue-800 transition shadow-sm disabled:opacity-50"
                  >
                    {submitting ? "Synchronizing Context..." : "Commit Modification"}
                  </button>
                </div>
              </form>
            </div>

            {/* Read-Only Access System Credentials Block */}
            <div className="bg-white rounded-3xl border border-gray-200 shadow-sm p-8 space-y-4">
              <div>
                <h3 className="text-xs font-black text-gray-400 uppercase tracking-wider pb-2 border-b border-gray-50">
                  Security Boundary Guardrails
                </h3>
                <p className="text-[11px] text-gray-400 mt-1">
                  Core parameters managed strictly by corporate institutional identity directories. Edits require security escalation.
                </p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
                <div className="bg-gray-50/70 border border-gray-100 p-3 rounded-xl">
                  <span className="block text-gray-400 font-black uppercase text-[9px] tracking-wide">Primary Email Endpoint</span>
                  <span className="font-medium text-gray-600 font-mono text-[11px] block mt-1">{user?.email || "N/A"}</span>
                </div>
                <div className="bg-gray-50/70 border border-gray-100 p-3 rounded-xl">
                  <span className="block text-gray-400 font-black uppercase text-[9px] tracking-wide">Assigned Scope Role</span>
                  {/* 🔥 ĐÃ SỬA: Đảm bảo nhãn phân quyền hiển thị chuẩn xác */}
                  <span className="font-mono text-gray-600 block mt-1 text-[11px] font-bold">{user?.role || fallbackRole}</span>
                </div>
              </div>
            </div>

          </div>

        </div>

      </div>
    </div>
  );
}