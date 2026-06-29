// import { useState, useEffect } from 'react';
// import api from '../../api.js'; // Đường dẫn chuẩn từ pages/Admin/ đi ra src/api.js

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
//           <span className="bg-emerald-50 border border-emerald-200 text-emerald-700 px-3 py-1 rounded-xl text-xs font-bold uppercase">
//             System Operational
//           </span>
//         </div>

//         {/* Infrastructure & Resources Cards (HealthController Swagger) */}
//         <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
//           <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
//             <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block">Storage Capacity Monitoring</span>
//             <div className="text-2xl font-black text-gray-900 mt-2">
//               {systemHealth.storageUsed} GB / {systemHealth.storageTotal} GB
//             </div>
//             <div className="w-full bg-gray-100 h-2 rounded-full mt-3 overflow-hidden">
//               <div className="bg-[#1e3a8a] h-full transition-all duration-300" style={{ width: `${(systemHealth.storageUsed / systemHealth.storageTotal) * 100}%` }}></div>
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

//         {/* System Activity Log Table (UserController / SystemNotificationController Swagger) */}
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
//                     <tr key={log.id} className="hover:bg-gray-50/50 transition">
//                       <td className="px-6 py-4 text-gray-500 font-mono text-xs">{log.timestamp}</td>
//                       <td className="px-6 py-4 font-semibold text-gray-900">{log.username}</td>
//                       <td className="px-6 py-4"><span className="px-2 py-0.5 rounded-full bg-blue-50 text-blue-700 text-xs font-medium">{log.role}</span></td>
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
import { initialMockData } from '../../mockData.js'; // Nhập mock dữ liệu chung

export default function AdminDashboard() {
  const [logs, setLogs] = useState([]);
  const [systemHealth, setSystemHealth] = useState({
    storageUsed: 0,
    storageTotal: 100,
    activeWorkspaces: 0,
    cpuUsage: "0%"
  });

  useEffect(() => {
    // Giả lập độ trễ mạng khi tải dữ liệu từ Mock Data Module
    const timer = setTimeout(() => {
      setSystemHealth(initialMockData.systemHealth);
      setLogs(initialMockData.auditLogs);
    }, 300); // 300ms delay cho giống thật

    return () => clearTimeout(timer);
  }, []);

  return (
    <div className="min-h-screen bg-[#f8fafc] text-[#0f172a] p-8 font-sans">
      <div className="max-w-7xl mx-auto">
        
        {/* Header Section */}
        <div className="flex justify-between items-center mb-8 border-b border-gray-200 pb-5">
          <div>
            <h1 className="text-3xl font-extrabold text-[#1e3a8a] tracking-tight">Technical Platform & Security</h1>
            <p className="text-gray-500 text-sm mt-1">Monitor server resources, audit logs, and platform permissions maintenance.</p>
          </div>
          <span className="bg-emerald-50 border border-emerald-200 text-emerald-700 px-3 py-1 rounded-xl text-xs font-bold uppercase">
            System Operational (MOCK)
          </span>
        </div>

        {/* Infrastructure & Resources Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block">Storage Capacity Monitoring</span>
            <div className="text-2xl font-black text-gray-900 mt-2">
              {systemHealth.storageUsed} GB / {systemHealth.storageTotal} GB
            </div>
            <div className="w-full bg-gray-100 h-2 rounded-full mt-3 overflow-hidden">
              <div className="bg-[#1e3a8a] h-full transition-all duration-300" style={{ width: `${(systemHealth.storageUsed / systemHealth.storageTotal) * 100}%` }}></div>
            </div>
          </div>

          <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col justify-between">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block">Active Workspace Count</span>
            <div className="text-3xl font-black text-[#1e3a8a] mt-2">{systemHealth.activeWorkspaces}</div>
          </div>

          <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col justify-between">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block">Service CPU Load</span>
            <div className="text-3xl font-black text-emerald-600 mt-2">{systemHealth.cpuUsage}</div>
          </div>
        </div>

        {/* System Activity Log Table */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="px-6 py-4 bg-gray-50/70 border-b border-gray-100">
            <h2 className="font-bold text-gray-900 text-base">System Audit Logs (User Traceability)</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 text-gray-400 text-xs font-bold uppercase border-b border-gray-100">
                  <th className="px-6 py-3.5">Timestamp</th>
                  <th className="px-6 py-3.5">User Account</th>
                  <th className="px-6 py-3.5">Role</th>
                  <th className="px-6 py-3.5">Executed Action</th>
                  <th className="px-6 py-3.5">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-sm">
                {logs.length === 0 ? (
                  <tr>
                    <td colSpan="5" className="px-6 py-10 text-center text-gray-400">Loading offline mockup dataset logs...</td>
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