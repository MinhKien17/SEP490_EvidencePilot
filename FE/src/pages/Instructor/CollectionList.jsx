// import { useState, useEffect } from 'react';
// import { useNavigate } from 'react-router-dom';
// import api from '../../api.js';

// export default function CollectionList() {
//   const navigate = useNavigate();
//   const [collections, setCollections] = useState([]);
//   const [projects, setProjects] = useState([]); 
//   const [loading, setLoading] = useState(false);
//   const [errorMessage, setErrorMessage] = useState("");
//   const [selectedProjectId, setSelectedProjectId] = useState("");

//   // Tải danh sách project và các collection ban đầu
//   const fetchInitialData = async () => {
//     setLoading(true);
//     setErrorMessage("");
//     try {
//       const projectRes = await api.get('/api/projects?size=100&active=true');
//       const projectList = projectRes.data.content || [];
//       setProjects(projectList);

//       if (projectList.length > 0) {
//         const firstId = projectList[0].id;
//         setSelectedProjectId(firstId);
//         const collectionRes = await api.get(`/api/projects/${firstId}/collections`);
//         setCollections(Array.isArray(collectionRes.data) ? collectionRes.data : collectionRes.data.content || []);
//       }
//     } catch (error) {
//       console.error("Error loading instructor context:", error);
//       setErrorMessage("Failed to synchronize collections repository with current active projects.");
//     } finally {
//       setLoading(false);
//     }
//   };

//   // Thay đổi bộ lọc hiển thị theo từng dự án
//   const handleProjectFilterChange = async (pId) => {
//     setSelectedProjectId(pId);
//     if (!pId) return;
//     setLoading(true);
//     try {
//       const response = await api.get(`/api/projects/${pId}/collections`);
//       setCollections(Array.isArray(response.data) ? response.data : response.data.content || []);
//     } catch (error) {
//       setErrorMessage("Error filtering collections backend index.");
//     } finally {
//       setLoading(false);
//     }
//   };

//   // Soft-delete bộ sưu tập mẫu
//   const handleDeleteCollection = async (id) => {
//     if (!window.confirm("Are you sure you want to archive this evidence library specification?")) return;
//     try {
//       await api.delete(`/api/collections/${id}`);
//       setCollections(prev => prev.filter(item => item.id !== id));
//     } catch (error) {
//       setErrorMessage("Could not delete target collection asset.");
//     }
//   };

//   useEffect(() => {
//     fetchInitialData();
//   }, []);

//   return (
//     <div className="min-h-screen bg-[#f8fafc] p-8 text-[#0f172a]">
//       <div className="max-w-7xl mx-auto">
        
//         {/* Upper Action Section */}
//         <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-8 border-b border-gray-200 pb-6">
//           <div>
//             <h1 className="text-3xl font-black text-[#1e3a8a] tracking-tight">Evidence Libraries</h1>
//             <p className="text-xs text-gray-400 mt-1">Configure baseline compliance parameters, document templates, and scope evaluation rules.</p>
//           </div>
//           {/* Chuyển trang sang file CreateCollection riêng biệt */}
//           <button
//             onClick={() => navigate('/instructor/collections/create')}
//             className="px-5 py-2.5 bg-[#1e3a8a] text-white font-black text-xs rounded-xl hover:bg-blue-800 transition shadow-sm"
//           >
//             + Create Collection Master
//           </button>
//         </div>

//         {errorMessage && (
//           <div className="p-4 mb-6 rounded-xl bg-rose-50 border border-rose-100 text-rose-700 text-xs font-bold">
//             ⚠️ {errorMessage}
//           </div>
//         )}

//         {/* Filter Toolbar Context */}
//         <div className="bg-white p-4 rounded-2xl border border-gray-200 shadow-sm mb-6 flex items-center gap-4">
//           <label className="text-xs font-black text-gray-500 uppercase tracking-wide">Target Project Repository:</label>
//           <select 
//             value={selectedProjectId}
//             onChange={(e) => handleProjectFilterChange(e.target.value)}
//             className="px-3 py-1.5 bg-gray-50 border border-gray-200 text-xs rounded-lg text-gray-800 font-medium focus:outline-none"
//           >
//             {projects.map(p => (
//               <option key={p.id} value={p.id}>{p.title}</option>
//             ))}
//           </select>
//         </div>

//         {/* Collections Table List View */}
//         <div className="bg-white rounded-3xl border border-gray-200 shadow-sm overflow-hidden">
//           <div className="overflow-x-auto">
//             <table className="w-full text-left border-collapse">
//               <thead>
//                 <tr className="bg-gray-50 text-gray-400 text-[10px] font-bold uppercase border-b border-gray-100">
//                   <th className="px-6 py-4">Collection UUID</th>
//                   <th className="px-6 py-4">Title Specs</th>
//                   <th className="px-6 py-4">Core Description Label</th>
//                 </tr>
//               </thead>
//               <tbody className="divide-y divide-gray-100 text-xs text-gray-700">
//                 {loading ? (
//                   <tr><td colSpan="4" className="px-6 py-8 text-center text-gray-400 font-medium">Synchronizing metadata data stream...</td></tr>
//                 ) : collections.length === 0 ? (
//                   <tr><td colSpan="4" className="px-6 py-8 text-center text-gray-400 font-medium">No active collection matrix mapped to this project module layout.</td></tr>
//                 ) : (
//                   collections.map((col) => (
//                     <tr key={col.id} className="hover:bg-gray-50/40 transition">
//                       <td className="px-6 py-4 font-mono text-gray-400 text-[11px]">{col.id}</td>
//                       <td className="px-6 py-4 font-bold text-gray-900">{col.title}</td>
//                       <td className="px-6 py-4 text-gray-500 max-w-xs truncate">{col.description || "N/A"}</td>
//                       <td className="px-6 py-4 text-right">
//                         <button 
//                           onClick={() => handleDeleteCollection(col.id)}
//                           className="text-xs font-bold text-rose-600 hover:underline"
//                         >
//                           Soft Archive
//                         </button>
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
import { useNavigate } from 'react-router-dom';
import { initialMockData } from '../../mockData.js'; 

export default function CollectionList() {
  const navigate = useNavigate();
  const [collections, setCollections] = useState([]);
  const [projects, setProjects] = useState([]); 
  const [documents, setDocuments] = useState([]); 
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [selectedProjectId, setSelectedProjectId] = useState("");

  // --- STATES QUẢN LÝ MODAL CHỈNH SỬA ---
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingCollection, setEditingCollection] = useState(null);
  const [editTitle, setEditTitle] = useState("");
  const [editDescription, setEditDescription] = useState("");
  const [editFile, setEditFile] = useState(null);
  const [editFileNameLabel, setEditFileNameLabel] = useState("");

  const fetchInitialData = () => {
    setLoading(true);
    setErrorMessage("");
    try {
      const projectList = initialMockData.projects || [];
      setProjects(projectList);
      
      const docList = initialMockData.referenceDocuments || [];
      setDocuments(docList);

      if (projectList.length > 0) {
        const firstId = projectList[0].id;
        setSelectedProjectId(firstId);
        
        const filtered = (initialMockData.collections || []).filter(col => col.projectId === firstId);
        setCollections(filtered);
      }
    } catch (error) {
      console.error("Error loading instructor context:", error);
      setErrorMessage("Failed to synchronize collections repository with current active projects.");
    } finally {
      setLoading(false);
    }
  };

  const handleProjectFilterChange = (pId) => {
    setSelectedProjectId(pId);
    if (!pId) {
      setCollections(initialMockData.collections || []);
      return;
    }
    setLoading(true);
    
    setTimeout(() => {
      const filtered = (initialMockData.collections || []).filter(col => col.projectId === pId);
      setCollections(filtered);
      setLoading(false);
    }, 200);
  };

  const handleDeleteCollection = (id) => {
    if (!window.confirm("Are you sure you want to permanently delete this evidence library specification?")) return;
    setCollections(prev => prev.filter(item => item.id !== id));
    initialMockData.collections = (initialMockData.collections || []).filter(item => item.id !== id);
  };

  // --- HÀM KÍCH HOẠT MODAL CHỈNH SỬA ---
  const openEditModal = (col, attachedPdf) => {
    setEditingCollection(col);
    setEditTitle(col.title);
    setEditDescription(col.description || "");
    setEditFile(null); // File mới người dùng chọn (nếu có)
    setEditFileNameLabel(attachedPdf ? attachedPdf.name : "No file attached");
    setIsEditModalOpen(true);
  };

  // --- HÀM XỬ LÝ LƯU THAY ĐỔI (UPDATE) ---
  const handleUpdateCollection = (e) => {
    e.preventDefault();
    if (!editTitle.trim()) return;

    // 1. Cập nhật thông tin trong mảng collections của Hub dữ liệu
    initialMockData.collections = initialMockData.collections.map(item => {
      if (item.id === editingCollection.id) {
        return {
          ...item,
          title: editTitle.trim(),
          description: editDescription.trim()
        };
      }
      return item;
    });

    // 2. Xử lý phần File đính kèm (Nếu chọn file mới)
    if (editFile) {
      const generatedBlobUrl = URL.createObjectURL(editFile);
      
      if (!initialMockData.referenceDocuments) {
        initialMockData.referenceDocuments = [];
      }

      // Kiểm tra xem collection này trước đó đã có file chưa
      const existingDocIndex = initialMockData.referenceDocuments.findIndex(doc => doc.collectionId === editingCollection.id);

      if (existingDocIndex > -1) {
        // Nếu đã có file: Cập nhật đè thông tin file mới
        initialMockData.referenceDocuments[existingDocIndex] = {
          ...initialMockData.referenceDocuments[existingDocIndex],
          name: editFile.name,
          fileUrl: generatedBlobUrl,
          uploadedAt: new Date().toISOString().split('T')[0]
        };
      } else {
        // Nếu trước đó chưa có file: Tạo mới bản ghi file nối vào collection này
        const newDoc = {
          id: `doc_${Date.now()}`,
          name: editFile.name,
          collectionId: editingCollection.id,
          fileUrl: generatedBlobUrl,
          uploadedAt: new Date().toISOString().split('T')[0]
        };
        initialMockData.referenceDocuments = [newDoc, ...initialMockData.referenceDocuments];
      }
    }

    // 3. Đồng bộ lại State giao diện hiển thị ngay lập tức
    setDocuments([...initialMockData.referenceDocuments]);
    const filtered = (initialMockData.collections || []).filter(col => col.projectId === selectedProjectId);
    setCollections(filtered);

    // 4. Đóng modal và thông báo thành công
    setIsEditModalOpen(false);
    alert("Collection updated successfully!");
  };

  useEffect(() => {
    fetchInitialData();
  }, []);

  return (
    <div className="min-h-screen bg-[#f8fafc] p-8 text-[#0f172a]">
      <div className="max-w-7xl mx-auto">
        
        {/* Upper Action Section */}
        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-8 border-b border-gray-200 pb-6">
          <div>
            <h1 className="text-3xl font-black text-[#1e3a8a] tracking-tight">Evidence Libraries</h1>
            <p className="text-xs text-gray-400 mt-1">Configure baseline compliance parameters, document templates, and scope evaluation rules.</p>
          </div>
          <button
            onClick={() => navigate('/instructor/collections/create')}
            className="px-5 py-2.5 bg-[#1e3a8a] text-white font-black text-xs rounded-xl hover:bg-blue-800 transition shadow-sm"
          >
            + Create Collection Master
          </button>
        </div>

        {errorMessage && (
          <div className="p-4 mb-6 rounded-xl bg-rose-50 border border-rose-100 text-rose-700 text-xs font-bold">
            ⚠️ {errorMessage}
          </div>
        )}

        {/* Filter Toolbar Context */}
        <div className="bg-white p-4 rounded-2xl border border-gray-200 shadow-sm mb-6 flex items-center gap-4">
          <label className="text-xs font-black text-gray-500 uppercase tracking-wide">Target Project Repository:</label>
          <select 
            value={selectedProjectId}
            onChange={(e) => handleProjectFilterChange(e.target.value)}
            className="px-3 py-1.5 bg-gray-50 border border-gray-200 text-xs rounded-lg text-gray-800 font-medium focus:outline-none focus:ring-2 focus:ring-[#1e3a8a]"
          >
            {projects.map(p => (
              <option key={p.id} value={p.id}>{p.title}</option>
            ))}
          </select>
        </div>

        {/* Collections Table List View */}
        <div className="bg-white rounded-3xl border border-gray-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 text-gray-400 text-[10px] font-bold uppercase border-b border-gray-100">
                  <th className="px-6 py-4">Collection UUID</th>
                  <th className="px-6 py-4">Title Specs</th>
                  <th className="px-6 py-4">Core Description Label</th>
                  <th className="px-6 py-4">Reference File</th>                  
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-xs text-gray-700">
                {loading ? (
                  <tr><td colSpan="5" className="px-6 py-8 text-center text-gray-400 font-medium animate-pulse">Synchronizing metadata data stream...</td></tr>
                ) : collections.length === 0 ? (
                  <tr><td colSpan="5" className="px-6 py-8 text-center text-gray-400 font-medium py-12 text-gray-400">No active collection matrix mapped to this project module layout.</td></tr>
                ) : (
                  collections.map((col) => {
                    const attachedPdf = documents.find(doc => doc.collectionId === col.id);

                    return (
                      <tr key={col.id} className="hover:bg-gray-50/40 transition">
                        <td className="px-6 py-4 font-mono text-gray-400 text-[11px]">{col.id}</td>
                        <td className="px-6 py-4 font-bold text-gray-900">{col.title}</td>
                        <td className="px-6 py-4 text-gray-500 max-w-xs whitespace-pre-wrap break-words leading-relaxed">{col.description || "No description provided."}</td>
                        
                        <td className="px-6 py-4">
                          {attachedPdf ? (
                            <div className="inline-flex items-center gap-2 bg-red-50 border border-red-200 px-3 py-2 rounded-xl text-red-700 font-bold shadow-sm">
                              <span className="text-base shrink-0">📕</span>
                              <span className="text-gray-900 font-bold truncate max-w-[140px] text-xs">{attachedPdf.name}</span>
                              <a 
                                href={attachedPdf.fileUrl || "#"} 
                                target="_blank" 
                                rel="noopener noreferrer"
                                className="ml-2 text-[10px] bg-red-600 text-white font-black px-2 py-1.5 rounded-lg hover:bg-red-700 transition shrink-0 shadow-sm inline-block text-center"
                              >
                                View
                              </a>
                            </div>
                          ) : (
                            <span className="text-gray-400 italic">No document bound</span>
                          )}
                        </td>

                        {/* HÀNH ĐỘNG: CÓ THÊM NÚT EDIT CHỈNH SỬA */}
                        <td className="px-6 py-4 text-right space-x-3">
                          <button 
                            onClick={() => openEditModal(col, attachedPdf)}
                            className="text-xs font-bold text-amber-600 hover:text-amber-800 hover:underline transition"
                          >
                            Edit
                          </button>
                          <button 
                            onClick={() => handleDeleteCollection(col.id)}
                            className="text-xs font-bold text-rose-600 hover:text-rose-900 hover:underline transition"
                          >
                            Delete
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
        {/* 🌟 THÀNH PHẦN MODAL POPUP CẬP NHẬT/CHỈNH SỬA DATA        */}
        {/* ========================================================= */}
        {isEditModalOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4 animate-fadeIn">
            <div className="bg-white rounded-3xl border border-gray-200 shadow-2xl max-w-lg w-full p-6 space-y-5 text-xs text-left">
              
              <div>
                <h3 className="text-lg font-black text-[#1e3a8a]">Update Collection Specifications</h3>
                <p className="text-[11px] text-gray-400">Modify metadata parameters and swap current active reference assets.</p>
              </div>

              <form onSubmit={handleUpdateCollection} className="space-y-4">
                
                {/* Sửa Title */}
                <div className="space-y-1">
                  <label className="text-[10px] font-black text-gray-500 uppercase tracking-wide">Collection Schema Title</label>
                  <input 
                    type="text" required value={editTitle}
                    onChange={(e) => setEditTitle(e.target.value)}
                    className="w-full px-3 py-2.5 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition"
                  />
                </div>

                {/* Sửa Description */}
                <div className="space-y-1">
                  <label className="text-[10px] font-black text-gray-500 uppercase tracking-wide">Boundary Specification Description</label>
                  <textarea 
                    rows="3" value={editDescription}
                    onChange={(e) => setEditDescription(e.target.value)}
                    className="w-full px-3 py-2.5 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition"
                  />
                </div>

                {/* Thay file mới */}
                <div className="space-y-2">
                  <label className="text-[10px] font-black text-gray-500 uppercase tracking-wide block">Reference Document Asset</label>
                  <div className="border border-dashed border-gray-200 rounded-xl p-3 bg-gray-50 flex items-center justify-between">
                    <span className="font-medium text-gray-600 truncate max-w-[240px]">
                      {editFile ? `✨ New: ${editFile.name}` : `🔒 Current: ${editFileNameLabel}`}
                    </span>
                    <label className="px-2.5 py-1.5 bg-white border border-gray-200 rounded-lg text-gray-700 font-bold shadow-xs cursor-pointer hover:bg-gray-100 text-[11px]">
                      Change PDF
                      <input 
                        type="file" accept=".pdf" 
                        onChange={(e) => e.target.files && setEditFile(e.target.files[0])} 
                        className="hidden" 
                      />
                    </label>
                  </div>
                </div>

                {/* Các nút bấm điều khiển */}
                <div className="flex gap-3 pt-3 border-t border-gray-100 font-bold">
                  <button 
                    type="button" 
                    onClick={() => setIsEditModalOpen(false)}
                    className="flex-1 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-600 rounded-xl text-center transition"
                  >
                    Cancel
                  </button>
                  <button 
                    type="submit"
                    className="flex-1 py-2.5 bg-[#1e3a8a] hover:bg-blue-800 text-white rounded-xl text-center transition shadow-md"
                  >
                    Save Changes
                  </button>
                </div>

              </form>
            </div>
          </div>
        )}

      </div>
    </div>
  );
}