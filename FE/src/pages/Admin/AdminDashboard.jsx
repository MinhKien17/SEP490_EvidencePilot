// import { useState, useEffect } from 'react';
// import { Link } from 'react-router-dom'; // 🔥 ĐÃ ĐỒNG BỘ: Import Link để điều hướng mượt mà
// import api from '../../api.js';

// export default function AdminDashboard() {
//   const [logs, setLogs] = useState([]);
//   const [systemHealth, setSystemHealth] = useState({
//     storageUsed: 0,
//     storageTotal: 100,
//     activeWorkspaces: 0,
//     cpuUsage: "0%"
//   });

//   useEffect(() => {
//     async function fetchAdminData() {
//       try {
//         const healthRes = await api.get('/api/health');
//         const logsRes = await api.get('/api/user/audit-logs');
//         setSystemHealth(healthRes.data);
//         setLogs(logsRes.data);
//       } catch (error) {
//         console.error("Error fetching admin data from Swagger:", error);
//       }
//     }
//     fetchAdminData();
//   }, []);

//   return (
//     <div className="min-h-screen bg-[#f8fafc] text-[#0f172a] p-8 font-sans">
//       <div className="max-w-7xl mx-auto">
        
//         {/* Header Section */}
//         <div className="flex justify-between items-center mb-8 border-b border-gray-200 pb-5">
//           <div>
//             <h1 className="text-3xl font-extrabold text-[#1e3a8a] tracking-tight">Technical Platform & Security</h1>
//             <p className="text-gray-500 text-sm mt-1">Monitor server resources, audit logs, and platform permissions maintenance.</p>
//           </div>
          
//           {/* 🔥 ĐÃ ĐỒNG BỘ: Khối điều hướng Profile chuẩn mật bộ mã cũ */}
//           <div className="flex items-center space-x-4">
//             <Link 
//               to="/admin/profile" 
//               className="flex items-center space-x-2 bg-white hover:bg-gray-50 border border-gray-200 px-4 py-2 rounded-xl text-xs font-black transition shadow-sm text-gray-700"
//             >
//               <div className="w-5 h-5 rounded-md bg-gradient-to-tr from-rose-600 to-orange-500 flex items-center justify-center text-[10px] text-white font-black">
//                 RA
//               </div>
//               <span>Admin Profile</span>
//             </Link>

//             <span className="bg-emerald-50 border border-emerald-200 text-emerald-700 px-3 py-1 rounded-xl text-xs font-bold uppercase">
//               System Operational
//             </span>
//           </div>
//         </div>

//         {/* Infrastructure & Resources Cards */}
//         <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
//           <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
//             <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block">Storage Capacity Monitoring</span>
//             <div className="text-2xl font-black text-gray-900 mt-2">
//               {systemHealth.storageUsed} GB / {systemHealth.storageTotal} GB
//             </div>
//             <div className="w-full bg-gray-100 h-2 rounded-full mt-3 overflow-hidden">
//               <div className="bg-[#1e3a8a] h-full transition-all duration-300" style={{ width: `${systemHealth.storageTotal > 0 ? (systemHealth.storageUsed / systemHealth.storageTotal) * 100 : 0}%` }}></div>
//             </div>
//           </div>

//           <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col justify-between">
//             <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block">Active Workspace Count</span>
//             <div className="text-3xl font-black text-[#1e3a8a] mt-2">{systemHealth.activeWorkspaces}</div>
//           </div>

//           <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col justify-between">
//             <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block">Service CPU Load</span>
//             <div className="text-3xl font-black text-emerald-600 mt-2">{systemHealth.cpuUsage}</div>
//           </div>
//         </div>

//         {/* System Activity Log Table */}
//         <div className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden">
//           <div className="px-6 py-4 bg-gray-50/70 border-b border-gray-100">
//             <h2 className="font-bold text-gray-900 text-base">System Audit Logs (User Traceability)</h2>
//           </div>
//           <div className="overflow-x-auto">
//             <table className="w-full text-left border-collapse">
//               <thead>
//                 <tr className="bg-gray-50 text-gray-400 text-xs font-bold uppercase border-b border-gray-100">
//                   <th className="px-6 py-3.5">Timestamp</th>
//                   <th className="px-6 py-3.5">User Account</th>
//                   <th className="px-6 py-3.5">Role</th>
//                   <th className="px-6 py-3.5">Executed Action</th>
//                   <th className="px-6 py-3.5">Status</th>
//                 </tr>
//               </thead>
//               <tbody className="divide-y divide-gray-100 text-sm">
//                 {logs.length === 0 ? (
//                   <tr>
//                     <td colSpan="5" className="px-6 py-10 text-center text-gray-400">Awaiting real-time data from Swagger API controllers...</td>
//                   </tr>
//                 ) : (
//                   logs.map((log) => (
//                     <tr key={log.id || log.timestamp} className="hover:bg-gray-50/50 transition">
//                       <td className="px-6 py-4 text-gray-500 font-mono text-xs">{log.timestamp}</td>
//                       <td className="px-6 py-4 font-semibold text-gray-900">{log.username}</td>
//                       <td className="px-6 py-4">
//                         {/* 🔥 ĐÃ ĐỒNG BỘ: Đổi màu Badge linh hoạt theo Role log thật của Swagger */}
//                         <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${
//                           log.role === 'ADMIN' ? 'bg-rose-50 text-rose-700' : 
//                           log.role === 'INSTRUCTOR' ? 'bg-purple-50 text-purple-700' : 
//                           'bg-blue-50 text-[#1e3a8a]'
//                         }`}>
//                           {log.role}
//                         </span>
//                       </td>
//                       <td className="px-6 py-4 text-gray-600">{log.action}</td>
//                       <td className="px-6 py-4">
//                         <span className={`px-2 py-0.5 rounded text-xs font-bold ${log.status === 'SUCCESS' ? 'text-emerald-700 bg-emerald-50' : 'text-rose-700 bg-rose-50'}`}>{log.status}</span>
//                       </td>
//                     </tr>
//                   ))
//                 )}
//               </tbody>
//             </table>
//           </div>
//         </div>

//       </div>
//     </div>
//   );
// }

import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom'; 
import { initialMockData } from '../../mockData.js'; 

export default function AdminDashboard() {
  const [logs, setLogs] = useState([]);
  const [dbCounts, setDbCounts] = useState({ categories: 0, collections: 0, docs: 0 });
  const [accounts, setAccounts] = useState([]);
  const [activeTab, setActiveTab] = useState('STUDENT'); // Bộ lọc chuyển đổi giữa STUDENT và INSTRUCTOR

  // 🌟 LẤY THÔNG TIN ĐỘNG CỦA ADMIN TỪ MOCK DATA ĐỂ ĐỒNG BỘ PROFILE HEADER
  const adminName = `${initialMockData.adminProfile?.firstName || 'Admin'} ${initialMockData.adminProfile?.lastName || 'Root'}`;

  useEffect(() => {
    const timer = setTimeout(() => {
      // --- ĐỒNG BỘ DỮ LIỆU ĐỘNG TỪ LOCALSTORAGE (HOẶC MOCKDATA GỐC) ---
      const localCollections = localStorage.getItem('collections') 
        ? JSON.parse(localStorage.getItem('collections')) 
        : (initialMockData.collections || []);

      const localDocs = localStorage.getItem('referenceDocuments') 
        ? JSON.parse(localStorage.getItem('referenceDocuments')) 
        : (initialMockData.referenceDocuments || []);

      const localProjects = localStorage.getItem('projects') 
        ? JSON.parse(localStorage.getItem('projects')) 
        : (initialMockData.projects || []);

      setLogs(initialMockData.auditLogs || []);

      // Tính toán số lượng file hợp lệ dựa trên dữ liệu mới nhất
      const validCollectionIds = localCollections.map(c => c.id);
      const totalValidDocs = localDocs.filter(
        doc => validCollectionIds.includes(doc.collectionId)
      ).length;

      // Cập nhật số liệu thống kê thời gian thực
      setDbCounts({
        categories: localProjects.length,
        collections: localCollections.length,
        docs: totalValidDocs
      });

      // Tạo danh sách tài khoản sinh viên dựa vào thông tin nộp bài trong feedbackRequests
      const studentAccounts = (initialMockData.feedbackRequests || []).map((req, idx) => ({
        id: `SV_00${idx + 1}`,
        username: req.submittedBy,
        email: `${req.submittedBy.toLowerCase().replace(/\s+/g, '')}@fpt.edu.vn`,
        role: 'STUDENT',
        status: 'ACTIVE'
      }));

      // Lấy tài khoản Giảng viên từ thông tin userProfile có sẵn
      const instructorAccount = {
        id: initialMockData.userProfile?.id || "GV_001",
        username: `${initialMockData.userProfile?.firstName} ${initialMockData.userProfile?.lastName}`,
        email: initialMockData.userProfile?.email || "instructor@fpt.edu.vn",
        role: 'INSTRUCTOR',
        status: 'ACTIVE'
      };

      setAccounts([...studentAccounts, instructorAccount]);
    }, 300);

    return () => clearTimeout(timer);
  }, []);

  // Thay đổi trạng thái tài khoản (Khóa / Mở khóa)
  const toggleAccountStatus = (id) => {
    setAccounts(prev => prev.map(acc => 
      acc.id === id ? { ...acc, status: acc.status === 'ACTIVE' ? 'BANNED' : 'ACTIVE' } : acc
    ));
  };

  // Đặt lại mật khẩu mặc định cho người dùng
  const handleResetPassword = (username) => {
    alert(`[ADMIN] Password has been reset to default for account: ${username}`);
  };

  return (
    <div className="min-h-screen bg-[#f8fafc] text-[#0f172a] p-8 font-sans">
      <div className="max-w-7xl mx-auto">
        
        {/* Header Section */}
        <div className="flex justify-between items-center mb-8 border-b border-gray-200 pb-5">
          <div>
            <h1 className="text-3xl font-extrabold text-[#1e3a8a] tracking-tight">Admin Management Dashboard</h1>
            <p className="text-gray-500 text-sm mt-1">Monitor server resources, system databases, and user account privilege logs.</p>
          </div>
          
          <div className="flex items-center space-x-4">
            <Link 
              to="/admin/profile" 
              className="flex items-center space-x-2 bg-white hover:bg-gray-50 border border-gray-200 px-4 py-2 rounded-xl text-xs font-black transition shadow-sm text-gray-700"
            >
              <div className="w-5 h-5 rounded-md bg-gradient-to-tr from-rose-600 to-orange-500 flex items-center justify-center text-[10px] text-white font-black">
                AD
              </div>
              {/* 🌟 ĐÃ THAY ĐỔI: Thay thế text cứng bằng biến adminName động */}
              <span>{adminName}</span>
            </Link>

            <span className="bg-emerald-50 border border-emerald-200 text-emerald-700 px-3 py-1 rounded-xl text-xs font-bold uppercase">
              System Online
            </span>
          </div>
        </div>

        {/* THÔNG TIN SỐ LIỆU ĐÃ BỎ KHO LƯU TRỮ TRỮ - CHỈ CÒN GRID 3 CỘT ĐỀU NHAU */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          {/* Ô số liệu 1: Category Tabs */}
          <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col justify-between min-h-[110px]">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block">Database: Category Tabs</span>
            <div className="text-3xl font-black text-indigo-600 mt-2">
              {dbCounts.categories} <span className="text-xs text-gray-400 font-normal">{dbCounts.categories === 1 ? 'tab' : 'tabs'}</span>
            </div>
          </div>

          {/* Ô số liệu 2: Standard Collections */}
          <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col justify-between min-h-[110px]">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block">Database: Standard Collections</span>
            <div className="text-3xl font-black text-purple-600 mt-2">
              {dbCounts.collections} <span className="text-xs text-gray-400 font-normal">{dbCounts.collections === 1 ? 'collection' : 'collections'}</span>
            </div>
          </div>

          {/* Ô số liệu 3: Reference PDFs */}
          <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col justify-between min-h-[110px]">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block">Database: Reference File PDFs</span>
            <div className="text-3xl font-black text-amber-600 mt-2">
              {dbCounts.docs} <span className="text-xs text-gray-400 font-normal">{dbCounts.docs === 1 ? 'file' : 'files'}</span>
            </div>
          </div>
        </div>

        {/* QUẢN LÝ ACCOUNT */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden mb-8">
          <div className="px-6 py-4 bg-gray-50/70 border-b border-gray-200 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div>
              <h2 className="font-bold text-gray-900 text-base">User Accounts Access Control</h2>
              <p className="text-xs text-gray-400 mt-0.5">Manage institutional profiles, review status flags, reset credentials or restrict access.</p>
            </div>
            
            <div className="flex bg-gray-100 p-1 rounded-xl">
              <button 
                onClick={() => setActiveTab('STUDENT')}
                className={`px-4 py-1.5 text-xs font-bold rounded-lg transition ${activeTab === 'STUDENT' ? 'bg-white text-[#1e3a8a] shadow-xs' : 'text-gray-500 hover:text-gray-900'}`}
              >
                Student Accounts
              </button>
              <button 
                onClick={() => setActiveTab('INSTRUCTOR')}
                className={`px-4 py-1.5 text-xs font-bold rounded-lg transition ${activeTab === 'INSTRUCTOR' ? 'bg-white text-[#1e3a8a] shadow-xs' : 'text-gray-500 hover:text-gray-900'}`}
              >
                Instructor Accounts
              </button>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 text-gray-400 text-xs font-bold uppercase border-b border-gray-100">
                  <th className="px-6 py-3.5">User ID</th>
                  <th className="px-6 py-3.5">Full Name</th>
                  <th className="px-6 py-3.5">Institutional Email</th>
                  <th className="px-6 py-3.5">Status</th>
                  <th className="px-6 py-3.5 text-right">Administrative Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-xs font-medium">
                {accounts.filter(acc => acc.role === activeTab).map((account) => (
                  <tr key={account.id} className="hover:bg-gray-50/50 transition">
                    <td className="px-6 py-4 text-gray-400 font-mono">{account.id}</td>
                    <td className="px-6 py-4 font-bold text-gray-900">{account.username}</td>
                    <td className="px-6 py-4 text-gray-600 font-mono">{account.email}</td>
                    <td className="px-6 py-4">
                      <span className={`px-2 py-0.5 rounded font-bold text-[10px] ${account.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'}`}>
                        {account.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right space-x-2">
                      <button 
                        onClick={() => handleResetPassword(account.username)}
                        className="px-2.5 py-1 text-gray-600 bg-gray-50 border border-gray-200 rounded-lg hover:bg-gray-100 transition"
                      >
                        Reset Password
                      </button>
                      <button 
                        onClick={() => toggleAccountStatus(account.id)}
                        className={`px-2.5 py-1 rounded-lg border font-bold transition ${account.status === 'ACTIVE' ? 'bg-rose-50 border-rose-200 text-rose-700 hover:bg-rose-100' : 'bg-emerald-50 border-emerald-200 text-emerald-700 hover:bg-emerald-100'}`}
                      >
                        {account.status === 'ACTIVE' ? 'Ban Account' : 'Activate'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Lịch sử nhật ký */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="px-6 py-4 bg-gray-50/70 border-b border-gray-100">
            <h2 className="font-bold text-gray-900 text-base">System Audit Logs</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 text-gray-400 text-xs font-bold uppercase border-b border-gray-100">
                  <th className="px-6 py-3.5">Timestamp</th>
                  <th className="px-6 py-3.5">Account Email</th>
                  <th className="px-6 py-3.5">Role</th>
                  <th className="px-6 py-3.5">Action Executed</th>
                  <th className="px-6 py-3.5">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-sm">
                {logs.length === 0 ? (
                  <tr>
                    <td colSpan="5" className="px-6 py-10 text-center text-gray-400">Loading activity logs...</td>
                  </tr>
                ) : (
                  logs.map((log) => (
                    <tr key={log.id} className="hover:bg-gray-50/50 transition">
                      <td className="px-6 py-4 text-gray-500 font-mono text-xs">{log.timestamp}</td>
                      <td className="px-6 py-4 font-semibold text-gray-900">{log.username}</td>
                      <td className="px-6 py-4">
                        <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                          log.role === 'ADMIN' ? 'bg-rose-50 text-rose-700' : 'bg-purple-50 text-purple-700'
                        }`}>{log.role}</span>
                      </td>
                      <td className="px-6 py-4 text-gray-600">{log.action}</td>
                      <td className="px-6 py-4">
                        <span className={`px-2 py-0.5 rounded text-xs font-bold ${log.status === 'SUCCESS' ? 'text-emerald-700 bg-emerald-50' : 'text-rose-700 bg-rose-50'}`}>{log.status}</span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

      </div>
    </div>
  );
}