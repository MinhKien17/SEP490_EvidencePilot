// import { useState, useEffect } from 'react';
// import api from '../../api.js';

// export default function ReviewRequests() {
//   const [requests, setRequests] = useState([]);
//   const [loading, setLoading] = useState(false);
//   const [errorMessage, setErrorMessage] = useState("");
  
//   // Trạng thái phục vụ tác vụ ghi log comment (Feedback Content)
//   const [selectedRequest, setSelectedRequest] = useState(null);
//   const [feedbackContent, setFeedbackContent] = useState("");
//   const [submittingFeedback, setSubmittingFeedback] = useState(false);

//   // Lấy toàn bộ danh sách feedback requests gán cho Instructor hiện tại
//   const fetchReviewRequests = async () => {
//     setLoading(true);
//     setErrorMessage("");
//     try {
//       // Gọi endpoint /api/feedback-requests
//       const response = await api.get('/api/feedback-requests');
//       setRequests(response.data);
//     } catch (error) {
//       console.error("Error reading feedback loops:", error);
//       setErrorMessage("Could not load validation assignments assigned to your token profile account.");
//     } finally {
//       setLoading(false);
//     }
//   };

//   // Cập nhật trạng thái vòng lặp chấm điểm (`PATCH /api/feedback-requests/{id}/status`)
//   const handleTransitionStatus = async (requestId, targetStatus) => {
//     setErrorMessage("");
//     try {
//       // Sử dụng cấu trúc @RequestParam cụ thể truyền qua URL query string
//       const response = await api.patch(`/api/feedback-requests/${requestId}/status?status=${targetStatus}`);
      
//       // Đồng bộ trạng thái trực tiếp trên UI table list mà không cần reload trang
//       setRequests(prev => prev.map(req => req.id === requestId ? { ...req, status: response.data.status } : req));
//       if (selectedRequest?.id === requestId) {
//         setSelectedRequest(prev => ({ ...prev, status: response.data.status }));
//       }
//     } catch (error) {
//       setErrorMessage("State transition rejected by backend pipeline engine rules.");
//     }
//   };

//   // Lưu ý ghi nhận xét và đẩy thông báo hệ thống (`POST /api/feedback-requests/{id}/feedback`)
//   const handleSubmitComment = async (e) => {
//     e.preventDefault();
//     if (!feedbackContent.trim() || !selectedRequest) return;

//     setSubmittingFeedback(true);
//     setErrorMessage("");
//     try {
//       // Khớp chính xác InstructorFeedbackRequest DTO record: request.content()
//       await api.post(`/api/feedback-requests/${selectedRequest.id}/feedback`, {
//         content: feedbackContent.trim()
//       });
      
//       alert("Instructor diagnostic feedback dispatched successfully!");
//       setFeedbackContent("");
//       setSelectedRequest(null);
//     } catch (error) {
//       setErrorMessage("Failed to attach textual comment payload onto verification node structure.");
//     } finally {
//       setSubmittingFeedback(false);
//     }
//   };

//   useEffect(() => {
//     fetchReviewRequests();
//   }, []);

//   return (
//     <div className="min-h-screen bg-[#f8fafc] p-8 text-[#0f172a]">
//       <div className="max-w-7xl mx-auto">
        
//         {/* Module Header */}
//         <div className="mb-8 border-b border-gray-200 pb-6">
//           <h1 className="text-3xl font-black text-[#1e3a8a] tracking-tight">Student Review Queue</h1>
//           <p className="text-xs text-gray-400 mt-1">Audit AI evidence assertions, evaluate project models, and emit authoritative grading verdicts.</p>
//         </div>

//         {errorMessage && (
//           <div className="p-4 mb-6 rounded-xl bg-rose-50 border border-rose-100 text-rose-700 text-xs font-bold">
//             ⚠️ {errorMessage}
//           </div>
//         )}

//         {/* Requests Management Table Component Layout */}
//         <div className="bg-white rounded-3xl border border-gray-200 shadow-sm overflow-hidden">
//           <div className="overflow-x-auto">
//             <table className="w-full text-left border-collapse">
//               <thead>
//                 <tr className="bg-gray-50 text-gray-400 text-[10px] font-bold uppercase border-b border-gray-100">
//                   <th className="px-6 py-4">Project Title Context</th>
//                   <th className="px-6 py-4">Submission Status</th>
//                   <th className="px-6 py-4">Resolution Decisions</th>
//                   <th className="px-6 py-4 text-right">Audit Panel</th>
//                 </tr>
//               </thead>
//               <tbody className="divide-y divide-gray-100 text-xs text-gray-700">
//                 {loading ? (
//                   <tr><td colSpan="4" className="px-6 py-8 text-center text-gray-400 font-medium">Interrogating pending assignment queues...</td></tr>
//                 ) : requests.length === 0 ? (
//                   <tr><td colSpan="4" className="px-6 py-8 text-center text-gray-400 font-medium">No pending grading requests assigned to your current instructor view bounds.</td></tr>
//                 ) : (
//                   requests.map((req) => (
//                     <tr key={req.id} className="hover:bg-gray-50/40 transition">
//                       <td className="px-6 py-4">
//                         <span className="font-bold text-gray-900 block text-xs">{req.projectTitle || "Project Evaluation Node"}</span>
//                         <span className="text-[10px] text-gray-400 font-mono block mt-0.5">Request ID: {req.id}</span>
//                       </td>
//                       <td className="px-6 py-4">
//                         <span className={`px-2 py-0.5 inline-block text-[9px] font-black rounded-md uppercase border ${
//                           req.status === 'PENDING' ? 'bg-amber-50 text-amber-700 border-amber-200' :
//                           req.status === 'REVIEWED' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' :
//                           'bg-rose-50 text-rose-700 border-rose-200'
//                         }`}>
//                           {req.status}
//                         </span>
//                       </td>
//                       <td className="px-6 py-4">
//                         <div className="flex gap-2">
//                           <button 
//                             onClick={() => handleTransitionStatus(req.id, "REVIEWED")}
//                             className="px-2 py-1 bg-emerald-600 text-white font-bold text-[10px] rounded hover:bg-emerald-700 transition"
//                           >
//                             Approve
//                           </button>
//                           <button 
//                             onClick={() => handleTransitionStatus(req.id, "RETURNED")}
//                             className="px-2 py-1 bg-amber-500 text-white font-bold text-[10px] rounded hover:bg-amber-600 transition"
//                           >
//                             Return Fix
//                           </button>
//                           <button 
//                             onClick={() => handleTransitionStatus(req.id, "REJECTED")}
//                             className="px-2 py-1 bg-rose-600 text-white font-bold text-[10px] rounded hover:bg-rose-700 transition"
//                           >
//                             Reject
//                           </button>
//                         </div>
//                       </td>
//                       <td className="px-6 py-4 text-right">
//                         <button 
//                           onClick={() => setSelectedRequest(req)}
//                           className="text-xs font-black text-[#1e3a8a] hover:underline"
//                         >
//                           Write Diagnostics
//                         </button>
//                       </td>
//                     </tr>
//                   ))
//                 )}
//               </tbody>
//             </table>
//           </div>
//         </div>

//         {/* Modal Editor: Viết nhận xét đính kèm đồ án */}
//         {selectedRequest && (
//           <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
//             <form onSubmit={handleSubmitComment} className="bg-white rounded-3xl p-6 max-w-lg w-full shadow-2xl border border-gray-100 space-y-4">
//               <div className="border-b border-gray-100 pb-2">
//                 <h3 className="text-sm font-black text-gray-900 uppercase tracking-wide">Append Instructor Evaluation Trail</h3>
//                 <p className="text-[10px] text-gray-400 font-mono mt-0.5">Target Node UUID: {selectedRequest.id}</p>
//               </div>

//               <div className="space-y-2 text-xs">
//                 <p className="text-gray-600 font-medium">
//                   You are evaluating the project module, current pipeline state is marked as <span className="font-bold text-blue-700">[{selectedRequest.status}]</span>.
//                 </p>
//                 <div>
//                   <label className="text-gray-400 font-bold block mb-1">Diagnostic Review Comments *</label>
//                   <textarea 
//                     rows="4" required value={feedbackContent} onChange={(e) => setFeedbackContent(e.target.value)}
//                     placeholder="Type detailed compliance flaws, grade breakdowns, or guidance markers here..."
//                     className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] text-gray-800"
//                   />
//                 </div>
//               </div>

//               <div className="flex gap-2 text-xs font-bold pt-2">
//                 <button 
//                   type="button" onClick={() => setSelectedRequest(null)}
//                   className="flex-1 py-2.5 bg-gray-100 text-gray-600 rounded-xl hover:bg-gray-200 transition"
//                 >
//                   Dismiss Panel
//                 </button>
//                 <button 
//                   type="submit" disabled={submittingFeedback}
//                   className="flex-1 py-2.5 bg-[#1e3a8a] text-white rounded-xl hover:bg-blue-800 transition shadow-sm disabled:opacity-50"
//                 >
//                   {submittingFeedback ? "Dispatching..." : "Publish Feedback"}
//                 </button>
//               </div>
//             </form>
//           </div>
//         )}

//       </div>
//     </div>
//   );
// }

import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { initialMockData } from '../../mockData.js'; 

export default function ReviewRequests() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  
  // Trạng thái phục vụ tác vụ kiểm tra chi tiết và nhặt file (Pick Files Workspace)
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [studentFiles, setStudentFiles] = useState([]); // Chứa danh sách (VD: 20 file) sinh viên nộp
  const [pickedFileIds, setPickedFileIds] = useState([]); // Lưu mảng ID của các file được GV "pick" (VD: lấy 3 cái)
  const [feedbackContent, setFeedbackContent] = useState("");
  const [submittingFeedback, setSubmittingFeedback] = useState(false);

  // Lấy thông tin giảng viên lên Header
  const instructorName = `${initialMockData.userProfile?.firstName || 'Nguyen'} ${initialMockData.userProfile?.lastName || 'Van A'}`;

  const fetchReviewRequests = () => {
    setLoading(true);
    setErrorMessage("");
    try {
      // Đảm bảo dữ liệu mẫu có cấu trúc file nộp của SV để demo tính năng chọn file
      if (initialMockData.feedbackRequests && initialMockData.feedbackRequests.length > 0) {
        initialMockData.feedbackRequests = initialMockData.feedbackRequests.map(req => ({
          ...req,
          // Giả lập mỗi request sinh viên nộp lên một cụm nhiều file (Ví dụ mảng các file tài liệu)
          submittedFiles: req.submittedFiles || [
            { id: `st_doc_${req.id}_1`, name: "01_System_Architecture_Draft.pdf", isPicked: false },
            { id: `st_doc_${req.id}_2`, name: "02_Database_Schema_v2.pdf", isPicked: false },
            { id: `st_doc_${req.id}_3`, name: "03_API_Specification_Final.pdf", isPicked: false },
            { id: `st_doc_${req.id}_4`, name: "04_UI_UX_Wireframe_Figma_Export.pdf", isPicked: false },
            { id: `st_doc_${req.id}_5`, name: "05_Deployment_Script_Docker.pdf", isPicked: false },
          ]
        }));
      }
      setRequests(initialMockData.feedbackRequests || []);
    } catch (error) {
      console.error("Error reading feedback loops:", error);
      setErrorMessage("Could not load validation assignments.");
    } finally {
      setLoading(false);
    }
  };

  const handleTransitionStatus = (requestId, targetStatus) => {
    setRequests(prev => prev.map(req => req.id === requestId ? { ...req, status: targetStatus } : req));
    initialMockData.feedbackRequests = initialMockData.feedbackRequests.map(req => 
      req.id === requestId ? { ...req, status: targetStatus } : req
    );
  };

  // --- KÍCH HOẠT WORKSPACE ĐỂ CHECK VÀ NHẶT FILE MINH CHỨNG ---
  const openAuditWorkspace = (req) => {
    setSelectedRequest(req);
    setFeedbackContent(req.feedback || "");
    
    // Nạp danh sách file sinh viên nộp của project này vào workspace
    const files = req.submittedFiles || [];
    setStudentFiles(files);

    // Lọc ra sẵn những file đã được chọn từ trước (nếu có) để đưa vào hàng tích chọn
    const alreadyPicked = files.filter(f => f.isPicked).map(f => f.id);
    setPickedFileIds(alreadyPicked);
  };

  // Xử lý Checkbox Toggle: Cho phép chọn/bỏ chọn từng file riêng lẻ
  const handleTogglePickFile = (fileId) => {
    setPickedFileIds(prev => 
      prev.includes(fileId) ? prev.filter(id => id !== fileId) : [...prev, fileId]
    );
  };

  // --- LƯU KẾT QUẢ PHÊ DUYỆT + DANH SÁCH FILE ĐƯỢC GIẢNG VIÊN CHỌN ---
  const handlePublishAudit = (e) => {
    e.preventDefault();
    if (!selectedRequest) return;

    setSubmittingFeedback(true);
    
    setTimeout(() => {
      try {
        // Cập nhật lại cấu trúc file trong Mock Data lõi (Đánh dấu file nào được duyệt, file nào không)
        const updatedFiles = studentFiles.map(file => ({
          ...file,
          isPicked: pickedFileIds.includes(file.id)
        }));

        initialMockData.feedbackRequests = initialMockData.feedbackRequests.map(req => {
          if (req.id === selectedRequest.id) {
            return {
              ...req,
              feedback: feedbackContent.trim(),
              submittedFiles: updatedFiles,
              // Tự động chuyển trạng thái sang REVIEWED vì giáo viên đã vào chấm điểm và nhặt file
              status: "REVIEWED" 
            };
          }
          return req;
        });

        // Đồng bộ lại danh sách ngoài bảng Table
        setRequests([...initialMockData.feedbackRequests]);
        
        alert(`Đã lưu phê duyệt thành công! Nhặt được ${pickedFileIds.length}/${studentFiles.length} file tài liệu đạt chuẩn làm minh chứng bộ sưu tập.`);
        setSelectedRequest(null);
      } catch (error) {
        setErrorMessage("Failed to save picked asset constraints.");
      } finally {
        setSubmittingFeedback(false);
      }
    }, 300);
  };

  useEffect(() => {
    fetchReviewRequests();
  }, []);

  return (
    <div className="min-h-screen bg-[#f8fafc] p-8 text-[#0f172a] font-sans">
      <div className="max-w-7xl mx-auto">
        
        {/* 🌟 HEADER SECTION ĐỒNG BỘ CÓ PROFILE + NÚT BACK TO DASHBOARD */}
        <div className="flex justify-between items-center mb-6 border-b border-gray-200 pb-5">
          <div className="space-y-2">
            {/* NÚT BACK TO DASHBOARD MỚI ĐƯỢC THÊM VÀO ĐÂY */}
            <Link 
              to="/instructor/dashboard" 
              className="inline-flex items-center space-x-1.5 text-xs text-gray-500 hover:text-[#1e3a8a] font-bold transition-colors group"
            >
              <span className="transform group-hover:-translate-x-0.5 transition-transform">←</span>
              <span>Back to Dashboard</span>
            </Link>
            
            <h1 className="text-3xl font-extrabold text-[#1e3a8a] tracking-tight">Student Review Queue</h1>
            <p className="text-gray-500 text-sm mt-1">
              Audit student project repositories, pick valid framework deliverables, and append authoritative grading verdicts.
            </p>
          </div>
          
          <div className="flex items-center space-x-4">
            <Link 
              to="/instructor/profile" 
              className="flex items-center space-x-2 bg-white hover:bg-gray-50 border border-gray-200 px-4 py-2 rounded-xl text-xs font-black transition shadow-sm text-gray-700"
            >
              <div className="w-5 h-5 rounded-md bg-gradient-to-tr from-blue-600 to-indigo-500 flex items-center justify-center text-[10px] text-white font-black">
                INS
              </div>
              <span>{instructorName}</span>
            </Link>
            <span className="bg-blue-50 border border-blue-200 text-blue-700 px-3 py-1 rounded-xl text-xs font-bold uppercase">
              Instructor Mode
            </span>
          </div>
        </div>

        {errorMessage && (
          <div className="p-4 mb-6 rounded-xl bg-rose-50 border border-rose-100 text-rose-700 text-xs font-bold">⚠️ {errorMessage}</div>
        )}

        {/* Requests Management Table Layout */}
        <div className="bg-white rounded-3xl border border-gray-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 text-gray-400 text-[10px] font-bold uppercase border-b border-gray-100">
                  <th className="px-6 py-4">Project Title Context</th>
                  <th className="px-6 py-4">Submission Status</th>
                  <th className="px-6 py-4">Picked Evidences</th>
                  <th className="px-6 py-4">Resolution Decisions</th>
                  <th className="px-6 py-4 text-right">Audit Workspace</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-xs text-gray-700">
                {loading ? (
                  <tr><td colSpan="5" className="px-6 py-8 text-center text-gray-400 font-medium animate-pulse">Interrogating pending assignment queues...</td></tr>
                ) : requests.length === 0 ? (
                  <tr><td colSpan="5" className="px-6 py-8 text-center text-gray-400 font-medium">No pending grading requests assigned.</td></tr>
                ) : (
                  requests.map((req) => {
                    const pickedCount = (req.submittedFiles || []).filter(f => f.isPicked).length;
                    const totalCount = (req.submittedFiles || []).length;

                    return (
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
                        
                        {/* CỘT HIỂN THỊ SỐ LƯỢNG FILE ĐÃ ĐƯỢC GV NHẶT */}
                        <td className="px-6 py-4">
                          <span className={`font-mono text-xs px-2 py-1 rounded-lg ${pickedCount > 0 ? 'bg-blue-50 text-blue-700 font-bold' : 'bg-gray-100 text-gray-400'}`}>
                            {pickedCount} / {totalCount} Files Picked
                          </span>
                        </td>

                        <td className="px-6 py-4">
                          <div className="flex gap-1.5">
                            <button onClick={() => handleTransitionStatus(req.id, "REVIEWED")} className="px-2 py-1 bg-emerald-600 text-white font-bold text-[10px] rounded-lg hover:bg-emerald-700 transition">Approve</button>
                            <button onClick={() => handleTransitionStatus(req.id, "RETURNED")} className="px-2 py-1 bg-amber-500 text-white font-bold text-[10px] rounded-lg hover:bg-amber-600 transition">Return Fix</button>
                          </div>
                        </td>
                        
                        <td className="px-6 py-4 text-right">
                          <button 
                            onClick={() => openAuditWorkspace(req)}
                            className="text-xs font-black bg-blue-50 text-[#1e3a8a] border border-blue-100 px-3 py-1.5 rounded-xl hover:bg-[#1e3a8a] hover:text-white transition shadow-sm"
                          >
                            Enter Audit & Pick →
                          </button>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* ========================================================= */}
        {/* AUDIT WORKSPACE MODAL: NƠI GV XEM 20 FILE VÀ CHỌN 3 FILE */}
        {/* ========================================================= */}
        {selectedRequest && (
          <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-fadeIn">
            <form onSubmit={handlePublishAudit} className="bg-white rounded-3xl p-6 max-w-xl w-full shadow-2xl border border-gray-200 space-y-4 max-h-[90vh] overflow-y-auto text-left">
              
              <div className="border-b border-gray-100 pb-3">
                <span className="text-[10px] bg-indigo-50 border border-indigo-100 text-indigo-700 font-black uppercase px-2 py-0.5 rounded-md">Evaluation Node</span>
                <h3 className="text-base font-black text-gray-900 mt-1">{selectedRequest.projectTitle}</h3>
                <p className="text-[10px] text-gray-400 font-mono mt-0.5">Student Repository Bundle ID: {selectedRequest.id}</p>
              </div>

              {/* KHÔNG GIAN HIỂN THỊ FILE VÀ PICK TÍCH CHỌN */}
              <div className="space-y-2">
                <div className="flex justify-between items-center">
                  <label className="text-gray-500 font-black uppercase tracking-wide text-[10px]">
                    Student Deliverables Matrix ({studentFiles.length} items submitted)
                  </label>
                  <span className="text-[11px] font-bold text-blue-600">
                    Selected: {pickedFileIds.length} compliance assets
                  </span>
                </div>
                
                <p className="text-[11px] text-gray-400 bg-gray-50 p-2.5 rounded-xl border border-gray-100 leading-relaxed">
                  💡 <strong>Quyền Giảng viên:</strong> Tích chọn các file đạt chuẩn chất lượng từ bộ sưu tập của sinh viên ở dưới để lưu giữ lại làm bằng chứng lưu kho hệ thống.
                </p>

                {/* Khối danh sách các file có checkbox để Pick */}
                <div className="border border-gray-200 rounded-2xl overflow-hidden divide-y divide-gray-100 max-h-48 overflow-y-auto bg-gray-50/40">
                  {studentFiles.map((file) => {
                    const isPicked = pickedFileIds.includes(file.id);
                    return (
                      <div 
                        key={file.id} 
                        onClick={() => handleTogglePickFile(file.id)}
                        className={`flex items-center justify-between p-3 cursor-pointer transition ${isPicked ? 'bg-blue-50/70' : 'hover:bg-gray-50'}`}
                      >
                        <div className="flex items-center space-x-3 max-w-[80%]">
                          <input 
                            type="checkbox"
                            checked={isPicked}
                            onChange={() => {}} // Đã xử lý qua thẻ bọc onClick của row
                            className="w-4 h-4 text-[#1e3a8a] border-gray-300 rounded focus:ring-[#1e3a8a]"
                          />
                          <span className={`text-xs font-medium truncate ${isPicked ? 'text-blue-900 font-bold' : 'text-gray-700'}`}>
                            📄 {file.name}
                          </span>
                        </div>
                        
                        <span className={`text-[9px] font-black uppercase px-2 py-0.5 rounded ${isPicked ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-400'}`}>
                          {isPicked ? "Picked" : "Skip"}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Ô Nhập Nhận Xét Góp Ý Như Cũ */}
              <div className="space-y-1 text-xs">
                <label className="text-gray-500 font-black uppercase tracking-wide text-[10px]">Diagnostic Evaluation Report</label>
                <textarea 
                  rows="3" required value={feedbackContent} onChange={(e) => setFeedbackContent(e.target.value)}
                  placeholder="Type detailed review notes or feedback instructions for this repository submission..."
                  className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white text-gray-800 transition"
                />
              </div>

              {/* Footer điều khiển của Modal */}
              <div className="flex gap-2 text-xs font-bold pt-2 border-t border-gray-100">
                <button 
                  type="button" onClick={() => setSelectedRequest(null)}
                  className="flex-1 py-3 bg-gray-100 text-gray-600 rounded-xl hover:bg-gray-200 transition text-center"
                >
                  Dismiss Workspace
                </button>
                <button 
                  type="submit" disabled={submittingFeedback}
                  className="flex-1 py-3 bg-[#1e3a8a] text-white rounded-xl hover:bg-blue-800 transition shadow-md disabled:opacity-50 text-center"
                >
                  {submittingFeedback ? "Saving Matrix..." : "Publish & Save Selection"}
                </button>
              </div>

            </form>
          </div>
        )}

      </div>
    </div>
  );
}