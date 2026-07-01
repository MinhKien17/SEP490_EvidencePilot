// import { useState, useEffect } from 'react';
// import api from '../api.js';

// export default function Profile() {
//   // --- 1. STATES MANAGEMENT ---
//   const [user, setUser] = useState(null);
//   const [loading, setLoading] = useState(true);
//   const [submitting, setSubmitting] = useState(false);
  
//   // Form input states
//   const [firstName, setFirstName] = useState("");
//   const [lastName, setLastName] = useState("");
  
//   // Status alerts
//   const [message, setMessage] = useState({ type: "", text: "" });

//   // 🔥 ĐỒNG BỘ NÂNG CAO: Tự động quét URL để định hình Header và Giao diện trước khi dữ liệu API đổ về
//   const pathname = window.location.pathname;
//   const isAdminRoute = pathname.includes('/admin');
//   const fallbackRole = isAdminRoute ? 'ADMIN' : (pathname.includes('/instructor') ? 'INSTRUCTOR' : 'STUDENT');

//   // --- 2. API INTEGRATION ---
//   const fetchUserProfile = async () => {
//     setLoading(true);
//     try {
//       const response = await api.get('/api/users/profile');
//       const data = response.data;
//       setUser(data);
//       setFirstName(data.firstName || "");
//       setLastName(data.lastName || "");
//     } catch (error) {
//       console.error("Failed to query user telemetry profile context:", error);
//       showAlert("error", "Failed to load profile synchronization matrix from security context.");
//     } finally {
//       setLoading(false);
//     }
//   };

//   const handleUpdateProfile = async (e) => {
//     e.preventDefault();
//     if (!firstName.trim() || !lastName.trim()) {
//       showAlert("error", "First name and last name fields cannot be blank execution nodes.");
//       return;
//     }

//     setSubmitting(true);
//     clearAlert();

//     try {
//       const payload = {
//         firstName: firstName.trim(),
//         lastName: lastName.trim()
//       };

//       const response = await api.put('/api/users/profile', payload);
//       setUser(response.data);
//       showAlert("success", "Profile metadata clusters updated successfully on backend core.");
//     } catch (error) {
//       console.error("Profile payload update rejected:", error);
//       showAlert("error", error.response?.data?.message || "Validation error detected during profile synchronization.");
//     } finally {
//       setSubmitting(false);
//     }
//   };

//   useEffect(() => {
//     fetchUserProfile();
//   }, [pathname]); // Tự động load lại nếu có sự chuyển đổi giữa các URL phân hệ

//   const showAlert = (type, text) => setMessage({ type, text });
//   const clearAlert = () => setMessage({ type: "", text: "" });

//   // Xác định role thực tế ưu tiên từ API, nếu chưa có thì dùng phân tích từ URL
//   const currentRole = user?.role || fallbackRole;

//   if (loading) {
//     return (
//       <div className="min-h-screen bg-[#f8fafc] flex items-center justify-center font-sans">
//         <div className="text-xs font-bold text-gray-400 tracking-widest animate-pulse uppercase">
//           Querying secure user cluster metadata...
//         </div>
//       </div>
//     );
//   }

//   return (
//     <div className="min-h-screen bg-[#f8fafc] text-[#0f172a] p-8 font-sans">
//       <div className="max-w-4xl mx-auto">
        
//         {/* Dynamic Header - Đã đồng bộ mượt mà giống mock data */}
//         <div className="mb-8 border-b border-gray-200 pb-6">
//           <h1 className="text-3xl font-black text-[#1e3a8a] tracking-tight">
//             {currentRole === 'ADMIN' ? 'Admin Profile' : 'Instructor Profile'}
//           </h1>
//           <p className="text-xs text-gray-400 mt-1">
//             {currentRole === 'ADMIN' 
//               ? 'Manage root credentials, platform access privileges, and cryptographic keys.'
//               : 'Manage your personal cryptographic identification, profile identities, and institutional platform authority.'}
//           </p>
//         </div>

//         {message.text && (
//           <div className={`p-4 mb-6 rounded-2xl border text-xs font-bold transition animate-fadeIn ${
//             message.type === 'success' 
//               ? 'bg-emerald-50 border-emerald-100 text-emerald-700' 
//               : 'bg-rose-50 border-rose-100 text-rose-700'
//           }`}>
//             {message.type === 'success' ? '✓' : '⚠️'} {message.text}
//           </div>
//         )}

//         {/* Master Profile Architecture Grid */}
//         <div className="grid grid-cols-1 md:grid-cols-3 gap-8 items-start">
          
//           {/* LEFT PANEL */}
//           <div className="bg-white rounded-3xl border border-gray-200 shadow-sm p-6 flex flex-col items-center text-center space-y-4">
//             <div className={`w-20 h-20 bg-gradient-to-tr rounded-2xl flex items-center justify-center text-white font-black text-2xl shadow-md ${
//               currentRole === 'ADMIN' ? 'from-rose-600 to-orange-500' :
//               currentRole === 'INSTRUCTOR' ? 'from-purple-600 to-indigo-500' : 'from-[#1e3a8a] to-blue-500'
//             }`}>
//               {user?.firstName?.charAt(0) || "U"}{user?.lastName?.charAt(0) || "P"}
//             </div>
            
//             <div className="space-y-1 w-full">
//               <h2 className="font-black text-gray-900 text-base tracking-tight">
//                 {user?.firstName || ""} {user?.lastName || (currentRole === 'ADMIN' ? 'Admin Node' : 'Member')}
//               </h2>
//               <p className="text-xs text-gray-400 font-medium truncate px-2">
//                 {currentRole === 'ADMIN' ? '👑 Administrator' : currentRole === 'INSTRUCTOR' ? '👨‍🏫 Faculty Member' : '🎓 Student Sandbox'}
//               </p>
//             </div>

//             <div className="w-full pt-4 border-t border-gray-100">
//               <span className={`inline-block px-3 py-1 text-[9px] font-black tracking-widest uppercase rounded-lg border ${
//                 currentRole === 'ADMIN' ? 'bg-rose-50 text-rose-700 border-rose-100' :
//                 currentRole === 'INSTRUCTOR' ? 'bg-purple-50 text-purple-700 border-purple-100' :
//                 'bg-blue-50 text-[#1e3a8a] border-blue-100'
//               }`}>
//                 Authority: {currentRole}
//               </span>
//             </div>

//             <div className="w-full bg-gray-50 rounded-xl p-3 text-[10px] text-left font-mono text-gray-400 border border-gray-100 break-all">
//               <span className="block font-bold uppercase tracking-wide text-[8px] text-gray-500 mb-0.5">User Infrastructure Key:</span>
//               {user?.id || "OFFLINE_CACHE_NODE"}
//             </div>
//           </div>

//           {/* RIGHT PANEL: Form */}
//           <div className="md:col-span-2 space-y-6">
            
//             <div className="bg-white rounded-3xl border border-gray-200 shadow-sm p-8">
//               <h3 className="text-xs font-black text-gray-400 uppercase tracking-wider mb-6 pb-2 border-b border-gray-50">
//                 Identity Profile Definitions
//               </h3>
              
//               <form onSubmit={handleUpdateProfile} className="space-y-5 text-xs">
//                 <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
//                   <div className="space-y-1.5">
//                     <label className="text-gray-500 font-black uppercase tracking-wide text-[10px]">First Name</label>
//                     <input 
//                       type="text"
//                       value={firstName}
//                       onChange={(e) => setFirstName(e.target.value)}
//                       placeholder="Enter identity first name"
//                       className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition"
//                     />
//                   </div>

//                   <div className="space-y-1.5">
//                     <label className="text-gray-500 font-black uppercase tracking-wide text-[10px]">Last Name</label>
//                     <input 
//                       type="text"
//                       value={lastName}
//                       onChange={(e) => setLastName(e.target.value)}
//                       placeholder="Enter identity last name"
//                       className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition"
//                     />
//                   </div>
//                 </div>

//                 <div className="flex justify-end pt-4 border-t border-gray-100">
//                   <button
//                     type="submit"
//                     disabled={submitting}
//                     className={`px-6 py-3 text-white font-black rounded-xl transition shadow-sm disabled:opacity-50 ${
//                       currentRole === 'ADMIN' ? 'bg-rose-600 hover:bg-rose-700' :
//                       currentRole === 'INSTRUCTOR' ? 'bg-purple-600 hover:bg-purple-700' : 'bg-[#1e3a8a] hover:bg-blue-800'
//                     }`}
//                   >
//                     {submitting ? "Synchronizing Context..." : "Commit Modification"}
//                   </button>
//                 </div>
//               </form>
//             </div>

//             {/* Read-Only Credentials Block */}
//             <div className="bg-white rounded-3xl border border-gray-200 shadow-sm p-8 space-y-4">
//               <div>
//                 <h3 className="text-xs font-black text-gray-400 uppercase tracking-wider pb-2 border-b border-gray-50">
//                   Security Boundary Guardrails
//                 </h3>
//                 <p className="text-[11px] text-gray-400 mt-1">
//                   Core parameters managed strictly by corporate institutional identity directories. Edits require security escalation.
//                 </p>
//               </div>

//               <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
//                 <div className="bg-gray-50/70 border border-gray-100 p-3 rounded-xl">
//                   <span className="block text-gray-400 font-black uppercase text-[9px] tracking-wide">Primary Email Endpoint</span>
//                   <span className="font-medium text-gray-600 font-mono text-[11px] block mt-1">{user?.email || "N/A"}</span>
//                 </div>
//                 <div className="bg-gray-50/70 border border-gray-100 p-3 rounded-xl">
//                   <span className="block text-gray-400 font-black uppercase text-[9px] tracking-wide">Assigned Scope Role</span>
//                   <span className="font-mono text-gray-600 block mt-1 text-[11px] font-bold">{currentRole}</span>
//                 </div>
//               </div>
//             </div>

//           </div>
//         </div>

//       </div>
//     </div>
//   );
// }

import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { initialMockData } from '../mockData.js'; 

export default function Profile() {
  const navigate = useNavigate();

  // --- 1. STATES MANAGEMENT ---
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState(""); 
  const [message, setMessage] = useState({ type: "", text: "" });

  const pathname = window.location.pathname;
  const fallbackRole = pathname.includes('/admin') ? 'ADMIN' : 'INSTRUCTOR';

  // --- 2. ĐỌC DỮ LIỆU TỪ MOCK DATA CHUNG ---
  const fetchUserProfile = () => {
    setLoading(true);
    try {
      const data = fallbackRole === 'ADMIN' 
        ? initialMockData.adminProfile 
        : initialMockData.userProfile;
      
      if (data) {
        setUser(data);
        setFirstName(data.firstName || "");
        setLastName(data.lastName || "");
        setEmail(data.email || ""); 
      }
    } catch (error) {
      console.error("Lỗi truy xuất dữ liệu profile:", error);
    } finally {
      setLoading(false);
    }
  };

  // --- 3. CẬP NHẬT NGƯỢC LẠI VÀO MOCK DATA CHUNG ---
  const handleUpdateProfile = (e) => {
    e.preventDefault();
    if (!firstName.trim() || !lastName.trim() || !email.trim()) {
      setMessage({ type: "error", text: "First name, last name, and email fields cannot be blank." });
      return;
    }

    setSubmitting(true);
    setMessage({ type: "", text: "" });

    setTimeout(() => {
      const updatedUser = {
        ...user,
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim() 
      };

      if (fallbackRole === 'ADMIN') {
        initialMockData.adminProfile = updatedUser;
      } else {
        initialMockData.userProfile = updatedUser;
      }

      setUser(updatedUser);
      setMessage({ type: "success", text: "Profile updated successfully" });
      setSubmitting(false);
    }, 400);
  };

  useEffect(() => {
    fetchUserProfile();
  }, [pathname]);

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
    <div className="min-h-screen bg-[#f8fafc] text-[#0f172a] p-8 font-sans flex flex-col justify-between">
      <div className="max-w-4xl w-full mx-auto flex-1">
        
        {/* Dynamic Header */}
        <div className="mb-8 border-b border-gray-200 pb-6">
          <h1 className="text-3xl font-black text-[#1e3a8a] tracking-tight">
            {fallbackRole === 'ADMIN' ? 'Admin Profile' : 'Instructor Profile'}
          </h1>
          <p className="text-xs text-gray-400 mt-1">
            {fallbackRole === 'ADMIN' 
              ? 'Manage root credentials, platform access privileges, and cryptographic keys.'
              : 'Manage your personal cryptographic identification, profile identities, and institutional platform authority.'}
          </p>
        </div>

        {message.text && (
          <div className={`p-4 mb-6 rounded-2xl border text-xs font-bold transition ${
            message.type === 'success' ? 'bg-emerald-50 border-emerald-100 text-emerald-700' : 'bg-rose-50 border-rose-100 text-rose-700'
          }`}>
            {message.type === 'success' ? '✓' : '⚠️'} {message.text}
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 items-start">
          
          {/* LEFT PANEL */}
          <div className="bg-white rounded-3xl border border-gray-200 shadow-sm p-6 flex flex-col items-center text-center space-y-4">
            <div className={`w-20 h-20 bg-gradient-to-tr rounded-2xl flex items-center justify-center text-white font-black text-2xl shadow-md ${
              fallbackRole === 'ADMIN' ? 'from-rose-600 to-orange-500' : 'from-purple-600 to-indigo-500'
            }`}>
              {user?.firstName?.charAt(0) || "A"}{user?.lastName?.charAt(0) || "P"}
            </div>
            
            <div className="space-y-1 w-full">
              <h2 className="font-black text-gray-900 text-base tracking-tight">
                {user?.firstName} {user?.lastName}
              </h2>
              <p className="text-xs text-gray-400 font-medium truncate px-2">
                {fallbackRole === 'ADMIN' ? '👑 Administrator' : '👨‍🏫 Faculty Member'}
              </p>
            </div>

            <div className="w-full pt-4 border-t border-gray-100">
              <span className={`inline-block px-3 py-1 text-[9px] font-black tracking-widest uppercase rounded-lg border ${
                fallbackRole === 'ADMIN' ? 'bg-rose-50 text-rose-700 border-rose-100' : 'bg-purple-50 text-purple-700 border-purple-100'
              }`}>
                Authority: {fallbackRole}
              </span>
            </div>

            <div className="w-full bg-gray-50 rounded-xl p-3 text-[10px] text-left font-mono text-gray-400 border border-gray-100 break-all">
              <span className="block font-bold uppercase tracking-wide text-[8px] text-gray-500 mb-0.5">User Infrastructure Key:</span>
              {user?.id}
            </div>
          </div>

          {/* RIGHT PANEL: Form */}
          <div className="md:col-span-2 space-y-6">
            <div className="bg-white rounded-3xl border border-gray-200 shadow-sm p-8">
              <h3 className="text-xs font-black text-gray-400 uppercase tracking-wider mb-6 pb-2 border-b border-gray-50">
                Identity Profile Definitions
              </h3>
              
              <form onSubmit={handleUpdateProfile} className="space-y-5 text-xs">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-1.5">
                    <label className="text-gray-500 font-black uppercase tracking-wide text-[10px]">First Name</label>
                    <input 
                      type="text" value={firstName} onChange={(e) => setFirstName(e.target.value)}
                      className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition"
                    />
                  </div>
                  <div className="space-y-1.5">
                    <label className="text-gray-500 font-black uppercase tracking-wide text-[10px]">Last Name</label>
                    <input 
                      type="text" value={lastName} onChange={(e) => setLastName(e.target.value)}
                      className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition"
                    />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label className="text-gray-500 font-black uppercase tracking-wide text-[10px]">Email</label>
                  <input 
                    type="email" value={email} onChange={(e) => setEmail(e.target.value)}
                    className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition"
                  />
                </div>

                <div className="flex justify-end pt-4 border-t border-gray-100">
                  <button
                    type="submit" disabled={submitting}
                    className={`px-6 py-2.5 text-white font-black rounded-xl transition shadow-sm disabled:opacity-50 ${
                      fallbackRole === 'ADMIN' ? 'bg-rose-600 hover:bg-rose-700' : 'bg-purple-600 hover:bg-purple-700'
                    }`}
                  >
                    {submitting ? "Synchronizing..." : "Update"}
                  </button>
                </div>
              </form>
            </div>

            <div className="bg-white rounded-3xl border border-gray-200 shadow-sm p-5">
              <div className="text-xs">
                <div className="bg-gray-50/70 border border-gray-100 p-3 rounded-xl inline-block min-w-[200px]">
                  <span className="block text-gray-400 font-black uppercase text-[9px] tracking-wide">Assigned Scope Role</span>
                  <span className="font-mono text-gray-600 block mt-1 text-[11px] font-bold">{fallbackRole}</span>
                </div>
              </div>
            </div>
          </div>

        </div>
      </div>

      {/* 🌟 NÚT BACK NẰM RIÊNG BIỆT HẲN RA NGOÀI, DƯỚI CÙNG GÓC TRÁI MÀN HÌNH */}
      <div className="max-w-4xl w-full mx-auto pt-6 text-left">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="px-6 py-2.5 bg-white border border-gray-200 text-gray-600 font-bold text-xs rounded-xl hover:bg-gray-100 transition shadow-sm"
        >
          ←Back
        </button>
      </div>

    </div>
  );
}