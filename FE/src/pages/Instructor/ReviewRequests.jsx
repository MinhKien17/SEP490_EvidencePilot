import { useState, useEffect } from 'react';
import api from '../../api.js';

export default function ReviewRequests() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  
  // Trạng thái phục vụ tác vụ ghi log comment (Feedback Content)
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [feedbackContent, setFeedbackContent] = useState("");
  const [submittingFeedback, setSubmittingFeedback] = useState(false);

  // Lấy toàn bộ danh sách feedback requests gán cho Instructor hiện tại
  const fetchReviewRequests = async () => {
    setLoading(true);
    setErrorMessage("");
    try {
      // Gọi endpoint /api/feedback-requests
      const response = await api.get('/api/feedback-requests');
      setRequests(response.data);
    } catch (error) {
      console.error("Error reading feedback loops:", error);
      setErrorMessage("Could not load validation assignments assigned to your token profile account.");
    } finally {
      setLoading(false);
    }
  };

  // Cập nhật trạng thái vòng lặp chấm điểm (`PATCH /api/feedback-requests/{id}/status`)
  const handleTransitionStatus = async (requestId, targetStatus) => {
    setErrorMessage("");
    try {
      // Sử dụng cấu trúc @RequestParam cụ thể truyền qua URL query string
      const response = await api.patch(`/api/feedback-requests/${requestId}/status?status=${targetStatus}`);
      
      // Đồng bộ trạng thái trực tiếp trên UI table list mà không cần reload trang
      setRequests(prev => prev.map(req => req.id === requestId ? { ...req, status: response.data.status } : req));
      if (selectedRequest?.id === requestId) {
        setSelectedRequest(prev => ({ ...prev, status: response.data.status }));
      }
    } catch (error) {
      setErrorMessage("State transition rejected by backend pipeline engine rules.");
    }
  };

  // Lưu ý ghi nhận xét và đẩy thông báo hệ thống (`POST /api/feedback-requests/{id}/feedback`)
  const handleSubmitComment = async (e) => {
    e.preventDefault();
    if (!feedbackContent.trim() || !selectedRequest) return;

    setSubmittingFeedback(true);
    setErrorMessage("");
    try {
      // Khớp chính xác InstructorFeedbackRequest DTO record: request.content()
      await api.post(`/api/feedback-requests/${selectedRequest.id}/feedback`, {
        content: feedbackContent.trim()
      });
      
      alert("Instructor diagnostic feedback dispatched successfully!");
      setFeedbackContent("");
      setSelectedRequest(null);
    } catch (error) {
      setErrorMessage("Failed to attach textual comment payload onto verification node structure.");
    } finally {
      setSubmittingFeedback(false);
    }
  };

  useEffect(() => {
    fetchReviewRequests();
  }, []);

  return (
    <div className="min-h-screen bg-[#f8fafc] p-8 text-[#0f172a]">
      <div className="max-w-7xl mx-auto">
        
        {/* Module Header */}
        <div className="mb-8 border-b border-gray-200 pb-6">
          <h1 className="text-3xl font-black text-[#1e3a8a] tracking-tight">Student Review Queue</h1>
          <p className="text-xs text-gray-400 mt-1">Audit AI evidence assertions, evaluate project models, and emit authoritative grading verdicts.</p>
        </div>

        {errorMessage && (
          <div className="p-4 mb-6 rounded-xl bg-rose-50 border border-rose-100 text-rose-700 text-xs font-bold">
            ⚠️ {errorMessage}
          </div>
        )}

        {/* Requests Management Table Component Layout */}
        <div className="bg-white rounded-3xl border border-gray-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 text-gray-400 text-[10px] font-bold uppercase border-b border-gray-100">
                  <th className="px-6 py-4">Project Title Context</th>
                  <th className="px-6 py-4">Submission Status</th>
                  <th className="px-6 py-4">Resolution Decisions</th>
                  <th className="px-6 py-4 text-right">Audit Panel</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-xs text-gray-700">
                {loading ? (
                  <tr><td colSpan="4" className="px-6 py-8 text-center text-gray-400 font-medium">Interrogating pending assignment queues...</td></tr>
                ) : requests.length === 0 ? (
                  <tr><td colSpan="4" className="px-6 py-8 text-center text-gray-400 font-medium">No pending grading requests assigned to your current instructor view bounds.</td></tr>
                ) : (
                  requests.map((req) => (
                    <tr key={req.id} className="hover:bg-gray-50/40 transition">
                      <td className="px-6 py-4">
                        <span className="font-bold text-gray-900 block text-xs">{req.projectTitle || "Project Evaluation Node"}</span>
                        <span className="text-[10px] text-gray-400 font-mono block mt-0.5">Request ID: {req.id}</span>
                      </td>
                      <td className="px-6 py-4">
                        <span className={`px-2 py-0.5 inline-block text-[9px] font-black rounded-md uppercase border ${
                          req.status === 'PENDING' ? 'bg-amber-50 text-amber-700 border-amber-200' :
                          req.status === 'REVIEWED' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' :
                          'bg-rose-50 text-rose-700 border-rose-200'
                        }`}>
                          {req.status}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex gap-2">
                          <button 
                            onClick={() => handleTransitionStatus(req.id, "REVIEWED")}
                            className="px-2 py-1 bg-emerald-600 text-white font-bold text-[10px] rounded hover:bg-emerald-700 transition"
                          >
                            Approve
                          </button>
                          <button 
                            onClick={() => handleTransitionStatus(req.id, "RETURNED")}
                            className="px-2 py-1 bg-amber-500 text-white font-bold text-[10px] rounded hover:bg-amber-600 transition"
                          >
                            Return Fix
                          </button>
                          <button 
                            onClick={() => handleTransitionStatus(req.id, "REJECTED")}
                            className="px-2 py-1 bg-rose-600 text-white font-bold text-[10px] rounded hover:bg-rose-700 transition"
                          >
                            Reject
                          </button>
                        </div>
                      </td>
                      <td className="px-6 py-4 text-right">
                        <button 
                          onClick={() => setSelectedRequest(req)}
                          className="text-xs font-black text-[#1e3a8a] hover:underline"
                        >
                          Write Diagnostics
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Modal Editor: Viết nhận xét đính kèm đồ án */}
        {selectedRequest && (
          <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
            <form onSubmit={handleSubmitComment} className="bg-white rounded-3xl p-6 max-w-lg w-full shadow-2xl border border-gray-100 space-y-4">
              <div className="border-b border-gray-100 pb-2">
                <h3 className="text-sm font-black text-gray-900 uppercase tracking-wide">Append Instructor Evaluation Trail</h3>
                <p className="text-[10px] text-gray-400 font-mono mt-0.5">Target Node UUID: {selectedRequest.id}</p>
              </div>

              <div className="space-y-2 text-xs">
                <p className="text-gray-600 font-medium">
                  You are evaluating the project module, current pipeline state is marked as <span className="font-bold text-blue-700">[{selectedRequest.status}]</span>.
                </p>
                <div>
                  <label className="text-gray-400 font-bold block mb-1">Diagnostic Review Comments *</label>
                  <textarea 
                    rows="4" required value={feedbackContent} onChange={(e) => setFeedbackContent(e.target.value)}
                    placeholder="Type detailed compliance flaws, grade breakdowns, or guidance markers here..."
                    className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] text-gray-800"
                  />
                </div>
              </div>

              <div className="flex gap-2 text-xs font-bold pt-2">
                <button 
                  type="button" onClick={() => setSelectedRequest(null)}
                  className="flex-1 py-2.5 bg-gray-100 text-gray-600 rounded-xl hover:bg-gray-200 transition"
                >
                  Dismiss Panel
                </button>
                <button 
                  type="submit" disabled={submittingFeedback}
                  className="flex-1 py-2.5 bg-[#1e3a8a] text-white rounded-xl hover:bg-blue-800 transition shadow-sm disabled:opacity-50"
                >
                  {submittingFeedback ? "Dispatching..." : "Publish Feedback"}
                </button>
              </div>
            </form>
          </div>
        )}

      </div>
    </div>
  );
}