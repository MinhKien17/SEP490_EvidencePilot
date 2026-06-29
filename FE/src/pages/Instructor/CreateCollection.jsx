// import { useState, useEffect } from 'react';
// import { useNavigate } from 'react-router-dom';
// import api from '../../api.js';

// export default function CreateCollection() {
//   const navigate = useNavigate();
  
//   // States cho Form (Khớp chuẩn xác cấu trúc CollectionRequest)
//   const [title, setTitle] = useState("");
//   const [description, setDescription] = useState("");
//   const [projectId, setProjectId] = useState(""); 
  
//   // States quản lý UI
//   const [projects, setProjects] = useState([]);
//   const [loading, setLoading] = useState(false);
//   const [submitting, setSubmitting] = useState(false);
//   const [errorMessage, setErrorMessage] = useState("");

//   // Đọc danh sách các project active để hiển thị lựa chọn liên kết
//   useEffect(() => {
//     const fetchActiveProjects = async () => {
//       setLoading(true);
//       try {
//         const response = await api.get('/api/projects?size=100&active=true');
//         setProjects(response.data.content || []);
//       } catch (error) {
//         console.error("Error loading project mapping context:", error);
//         setErrorMessage("Failed to load project reference keys from infrastructure backend.");
//       } finally {
//         setLoading(false);
//       }
//     };
//     fetchActiveProjects();
//   }, []);

//   // Xử lý gửi API tạo mới
//   const handleSubmit = async (e) => {
//     e.preventDefault();
//     if (!title.trim()) return;

//     setSubmitting(true);
//     setErrorMessage("");

//     try {
//       const payload = {
//         title: title.trim(),
//         description: description.trim() || null,
//         projectId: projectId ? projectId : null // Map giá trị trống thành null để BE xử lý UUID đúng kiểu
//       };

//       await api.post('/api/collections', payload);
//       navigate('/instructor/collections'); // Chuyển trang quay về danh sách sau khi tạo thành công
//     } catch (error) {
//       console.error("Create collection rejected:", error);
//       setErrorMessage(error.response?.data?.message || "Validation error. Please verify input parameters syntax.");
//     } finally {
//       setSubmitting(false);
//     }
//   };

//   return (
//     <div className="min-h-screen bg-[#f8fafc] text-[#0f172a] p-8 font-sans">
//       <div className="max-w-2xl mx-auto">
        
//         {/* Navigation Breadcrumb */}
//         <button 
//           type="button"
//           onClick={() => navigate('/instructor/collections')}
//           className="text-xs font-bold text-gray-400 hover:text-[#1e3a8a] transition flex items-center gap-1 mb-4"
//         >
//           ➔ Back to Library Index
//         </button>

//         {/* Header Block */}
//         <div className="mb-8 border-b border-gray-200 pb-6">
//           <h1 className="text-3xl font-black text-[#1e3a8a] tracking-tight">Build Evidence Collection</h1>
//           <p className="text-xs text-gray-400 mt-1">
//             Initialize a new blueprint master library to group, store, and cross-reference student compliance documents.
//           </p>
//         </div>

//         {errorMessage && (
//           <div className="p-4 mb-6 rounded-2xl bg-rose-50 border border-rose-100 text-rose-700 text-xs font-bold">
//             ⚠️ {errorMessage}
//           </div>
//         )}

//         {/* Form Input Body */}
//         <div className="bg-white rounded-3xl border border-gray-200 shadow-sm p-8">
//           {loading ? (
//             <div className="text-center py-8 text-xs text-gray-400 font-medium animate-pulse">
//               Synchronizing infrastructure mapping configurations...
//             </div>
//           ) : (
//             <form onSubmit={handleSubmit} className="space-y-6 text-xs">
              
//               {/* Field: Title */}
//               <div className="space-y-1.5">
//                 <label className="text-gray-500 font-black uppercase tracking-wide text-[10px]">
//                   Collection Schema Title <span className="text-rose-500">*</span>
//                 </label>
//                 <input 
//                   type="text"
//                   required
//                   value={title}
//                   onChange={(e) => setTitle(e.target.value)}
//                   placeholder="e.g., Autumn 2026 Software Architecture Core Metrics Template"
//                   className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition"
//                 />
//               </div>

//               {/* Field: Description */}
//               <div className="space-y-1.5">
//                 <label className="text-gray-500 font-black uppercase tracking-wide text-[10px]">
//                   Boundary Specification & Scope Description
//                 </label>
//                 <textarea 
//                   rows="5"
//                   value={description}
//                   onChange={(e) => setDescription(e.target.value)}
//                   placeholder="Describe the checking rules layout context, expected proof documentation structures, or evaluation matrix requirements..."
//                   className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition"
//                 />
//               </div>

//               {/* Field: Project Relation Selection */}
//               <div className="space-y-1.5">
//                 <label className="text-gray-500 font-black uppercase tracking-wide text-[10px]">
//                   Target Project Association Bound <span className="text-gray-400 font-normal">(Optional)</span>
//                 </label>
//                 <select 
//                   value={projectId}
//                   onChange={(e) => setProjectId(e.target.value)}
//                   className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-700 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition"
//                 >
//                   <option value="">-- Set as Isolated Global Baseline Library --</option>
//                   {projects.map(p => (
//                     <option key={p.id} value={p.id}>{p.title}</option>
//                   ))}
//                 </select>
//                 <p className="text-[10px] text-gray-400 mt-1">
//                   Associating this cluster makes the baseline constraints immediately reviewable inside that student workspace timeline.
//                 </p>
//               </div>

//               {/* Operations Footer */}
//               <div className="flex items-center gap-3 pt-4 border-t border-gray-100 font-bold">
//                 <button 
//                   type="button"
//                   onClick={() => navigate('/instructor/collections')}
//                   className="flex-1 py-3 bg-gray-50 hover:bg-gray-100 text-gray-600 rounded-xl transition text-center border border-gray-200/60"
//                 >
//                   Abort Operation
//                 </button>
//                 <button 
//                   type="submit"
//                   disabled={submitting}
//                   className="flex-1 py-3 bg-[#1e3a8a] text-white rounded-xl hover:bg-blue-800 transition shadow-md disabled:opacity-50 text-center"
//                 >
//                   {submitting ? "Deploying Schema..." : "Assemble Collection Master"}
//                 </button>
//               </div>

//             </form>
//           )}
//         </div>

//       </div>
//     </div>
//   );
// }
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { initialMockData } from '../../mockData.js'; // Đồng bộ với file mock data chung

export default function CreateCollection() {
  const navigate = useNavigate();
  
  // States cho Form (Khớp chuẩn xác cấu trúc CollectionRequest)
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [projectId, setProjectId] = useState(""); 
  
  // States quản lý UI
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  // Đọc danh sách các project active từ mockData để hiển thị lựa chọn liên kết
  useEffect(() => {
    const fetchActiveProjects = () => {
      setLoading(true);
      try {
        setProjects(initialMockData.projects || []);
      } catch (error) {
        console.error("Error loading project mapping context:", error);
        setErrorMessage("Failed to load project reference keys from infrastructure backend.");
      } finally {
        setLoading(false);
      }
    };
    fetchActiveProjects();
  }, []);

  // Xử lý gửi dữ liệu giả lập tạo mới
  const handleSubmit = (e) => {
    e.preventDefault();
    if (!title.trim()) return;

    setSubmitting(true);
    setErrorMessage("");

    // Giả lập độ trễ mạng khi lưu dữ liệu
    setTimeout(() => {
      try {
        const newCollection = {
          id: `col_mock_${Math.floor(Math.random() * 1000)}`, // Tạo chuỗi UUID ngẫu nhiên
          title: title.trim(),
          description: description.trim() || "No description provided.",
          documentCount: 0,
          createdAt: new Date().toISOString().split('T')[0] // Lấy ngày hiện tại dạng YYYY-MM-DD
        };

        // Đẩy tạm vào bộ nhớ của mockData runtime để trang danh sách có thể hiển thị luôn
        initialMockData.collections = [newCollection, ...initialMockData.collections];

        setSubmitting(false);
        navigate('/instructor/collections'); // Chuyển trang quay về danh sách sau khi tạo thành công
      } catch (error) {
        setErrorMessage("Validation error. Please verify input parameters syntax.");
        setSubmitting(false);
      }
    }, 600); // Delay 600ms giả lập tạo tiến trình nén schema dữ liệu
  };

  return (
    <div className="min-h-screen bg-[#f8fafc] text-[#0f172a] p-8 font-sans">
      <div className="max-w-2xl mx-auto">
        
        {/* Navigation Breadcrumb */}
        <button 
          type="button"
          onClick={() => navigate('/instructor/collections')}
          className="text-xs font-bold text-gray-400 hover:text-[#1e3a8a] transition flex items-center gap-1 mb-4"
        >
          ➔ Back to Library Index
        </button>

        {/* Header Block */}
        <div className="mb-8 border-b border-gray-200 pb-6">
          <h1 className="text-3xl font-black text-[#1e3a8a] tracking-tight">Build Evidence Collection</h1>
          <p className="text-xs text-gray-400 mt-1">
            Initialize a new blueprint master library to group, store, and cross-reference student compliance documents. (MOCK SIMULATION)
          </p>
        </div>

        {errorMessage && (
          <div className="p-4 mb-6 rounded-2xl bg-rose-50 border border-rose-100 text-rose-700 text-xs font-bold">
            ⚠️ {errorMessage}
          </div>
        )}

        {/* Form Input Body */}
        <div className="bg-white rounded-3xl border border-gray-200 shadow-sm p-8">
          {loading ? (
            <div className="text-center py-8 text-xs text-gray-400 font-medium animate-pulse">
              Synchronizing infrastructure mapping configurations...
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-6 text-xs">
              
              {/* Field: Title */}
              <div className="space-y-1.5">
                <label className="text-gray-500 font-black uppercase tracking-wide text-[10px]">
                  Collection Schema Title <span className="text-rose-500">*</span>
                </label>
                <input 
                  type="text"
                  required
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="e.g., Autumn 2026 Software Architecture Core Metrics Template"
                  className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition"
                />
              </div>

              {/* Field: Description */}
              <div className="space-y-1.5">
                <label className="text-gray-500 font-black uppercase tracking-wide text-[10px]">
                  Boundary Specification & Scope Description
                </label>
                <textarea 
                  rows="5"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Describe the checking rules layout context, expected proof documentation structures, or evaluation matrix requirements..."
                  className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition"
                />
              </div>

              {/* Field: Project Relation Selection */}
              <div className="space-y-1.5">
                <label className="text-gray-500 font-black uppercase tracking-wide text-[10px]">
                  Target Project Association Bound <span className="text-gray-400 font-normal">(Optional)</span>
                </label>
                <select 
                  value={projectId}
                  onChange={(e) => setProjectId(e.target.value)}
                  className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-700 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition"
                >
                  <option value="">-- Set as Isolated Global Baseline Library --</option>
                  {projects.map(p => (
                    <option key={p.id} value={p.id}>{p.title}</option>
                  ))}
                </select>
                <p className="text-[10px] text-gray-400 mt-1">
                  Associating this cluster makes the baseline constraints immediately reviewable inside that student workspace timeline.
                </p>
              </div>

              {/* Operations Footer */}
              <div className="flex items-center gap-3 pt-4 border-t border-gray-100 font-bold">
                <button 
                  type="button"
                  onClick={() => navigate('/instructor/collections')}
                  className="flex-1 py-3 bg-gray-50 hover:bg-gray-100 text-gray-600 rounded-xl transition text-center border border-gray-200/60"
                >
                  Abort Operation
                </button>
                <button 
                  type="submit"
                  disabled={submitting}
                  className="flex-1 py-3 bg-[#1e3a8a] text-white rounded-xl hover:bg-blue-800 transition shadow-md disabled:opacity-50 text-center"
                >
                  {submitting ? "Deploying Schema..." : "Assemble Collection Master"}
                </button>
              </div>

            </form>
          )}
        </div>

      </div>
    </div>
  );
}