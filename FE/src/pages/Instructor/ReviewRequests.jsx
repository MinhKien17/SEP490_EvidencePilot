import { useState, useEffect } from 'react';
import api from '../../api.js';

export default function ReviewRequests() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // 1. Gọi API lấy danh sách feedback requests thật từ Backend
  useEffect(() => {
    api.get('/api/feedback-requests')
      .then((res) => {
        // Lọc hiển thị những request có status là PENDING trên UI giảng viên
        const pendingRequests = res.data.filter(req => req.status === 'PENDING');
        setRequests(pendingRequests);
      })
      .catch((err) => {
        console.error('Failed to fetch feedback requests from BE:', err);
        setError('Could not connect to the server. Using temporary mock data.');
        
        // MOCK DATA: Chạy dự phòng nếu server ngrok của Kiên chưa bật endpoint này
        setRequests([
          {
            id: 1,
            project: { name: 'Library Management System' },
            student: { firstName: 'John', lastName: 'Doe' },
            requestedAt: '2026-06-18T10:30:00',
            status: 'PENDING'
          },
          {
            id: 2,
            project: { name: 'Fast Food Delivery App' },
            student: { firstName: 'Alice', lastName: 'Smith' },
            requestedAt: '2026-06-19T14:15:00',
            status: 'PENDING'
          }
        ]);
      })
      .finally(() => setLoading(false));
  }, []);

  // 2. Xử lý Approve (Reviewed) hoặc Reject theo đúng endpoint Java của BE
  const handleReview = (id, action) => {
    const endpoint = action === 'APPROVED' 
      ? `/api/feedback-requests/${id}/reviewed` 
      : `/api/feedback-requests/${id}/rejected`;

    api.post(endpoint)
      .then(() => {
        alert(`Request ID ${id} has been successfully ${action.toLowerCase()}!`);
        // Xóa request vừa duyệt khỏi danh sách hiển thị trên UI
        setRequests(requests.filter(req => req.id !== id));
      })
      .catch((err) => {
        console.error(`Error processing ${action}:`, err);
        alert('Action failed. The server endpoint might not be ready yet.');
      });
  };

  if (loading) return <div className="p-10 text-center text-gray-500 font-medium mt-10">Loading requests list...</div>;

  return (
    <div className="max-w-6xl mx-auto p-6 mt-10 bg-white rounded-xl shadow-md border border-gray-100">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h2 className="text-2xl font-bold text-gray-800">Review Student Requests</h2>
          {error && <p className="text-amber-600 text-xs mt-1 font-medium">⚠️ {error}</p>}
        </div>
        <span className="px-3 py-1 bg-yellow-100 text-yellow-800 rounded-full text-sm font-semibold">
          {requests.length} pending
        </span>
      </div>
      
      <div className="overflow-hidden rounded-lg border border-gray-200">
        <table className="w-full text-left border-collapse bg-white">
          <thead className="bg-gray-50 border-b border-gray-200">
            <tr>
              <th className="p-4 font-semibold text-gray-600 text-sm uppercase tracking-wider">Project</th>
              <th className="p-4 font-semibold text-gray-600 text-sm uppercase tracking-wider">Student</th>
              <th className="p-4 font-semibold text-gray-600 text-sm uppercase tracking-wider">Submitted Date</th>
              <th className="p-4 font-semibold text-gray-600 text-sm uppercase tracking-wider text-center">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {requests.length === 0 ? (
              <tr>
                <td colSpan="4" className="p-8 text-center text-gray-500">
                  No pending requests to review.
                </td>
              </tr>
            ) : (
              requests.map((req) => (
                <tr key={req.id} className="hover:bg-blue-50/50 transition duration-150">
                  <td className="p-4">
                    {/* Map đúng cấu trúc object lồng nhau từ Entity Java của BE */}
                    <p className="font-bold text-gray-800">{req.project?.name || 'N/A'}</p>
                  </td>
                  <td className="p-4">
                    <span className="font-medium text-gray-700">
                      {req.student ? `${req.student.firstName} ${req.student.lastName}`.trim() : 'Unknown Student'}
                    </span>
                  </td>
                  <td className="p-4 text-gray-600">
                    {req.requestedAt ? new Date(req.requestedAt).toLocaleDateString('en-US') : 'N/A'}
                    <span className="block text-xs text-gray-400">
                      {req.requestedAt ? new Date(req.requestedAt).toLocaleTimeString('en-US') : ''}
                    </span>
                  </td>
                  <td className="p-4">
                    <div className="flex justify-center gap-2">
                      <button 
                        onClick={() => handleReview(req.id, 'APPROVED')}
                        className="px-4 py-2 bg-green-500 text-white text-sm font-medium rounded-md hover:bg-green-600 transition shadow-sm"
                      >
                        Approve
                      </button>
                      <button 
                        onClick={() => handleReview(req.id, 'REJECTED')}
                        className="px-4 py-2 bg-red-500 text-white text-sm font-medium rounded-md hover:bg-red-600 transition shadow-sm"
                      >
                        Reject
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}