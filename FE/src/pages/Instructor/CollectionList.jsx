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
import { useNavigate, Link } from 'react-router-dom';
import { initialMockData } from '../../mockData.js'; 

export default function CollectionList() {
  const navigate = useNavigate();
  const [collections, setCollections] = useState([]);
  const [projects, setProjects] = useState([]); 
  const [documents, setDocuments] = useState([]); 
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [selectedProjectId, setSelectedProjectId] = useState("");

  // Lấy thông tin giảng viên trực tiếp từ Object userProfile trong mockData.js để hiển thị trên Header
  const instructorName = `${initialMockData.userProfile?.firstName || 'Nguyen'} ${initialMockData.userProfile?.lastName || 'Van A'}`;

  // --- STATES QUẢN LÝ MODAL CHỈNH SỬA (HỖ TRỢ MULTIPLE PDFS) ---
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingCollection, setEditingCollection] = useState(null);
  const [editTitle, setEditTitle] = useState("");
  const [editDescription, setEditDescription] = useState("");
  const [editFiles, setEditFiles] = useState([]); // Chứa danh sách nhiều files PDF mới được chọn thêm
  const [currentAttachedDocs, setCurrentAttachedDocs] = useState([]); // Các file hiện tại đang có của collection này

  // --- 🌟 1. ĐỌC DỮ LIỆU ĐỘNG TỪ LOCALSTORAGE KHI LOAD TRANG ---
  const fetchInitialData = () => {
    setLoading(true);
    setErrorMessage("");
    try {
      // Đọc danh mục dự án (projects)
      const projectList = localStorage.getItem('projects')
        ? JSON.parse(localStorage.getItem('projects'))
        : (initialMockData.projects || []);
      setProjects(projectList);
      
      // Đọc tài liệu đính kèm (documents)
      const docList = localStorage.getItem('referenceDocuments')
        ? JSON.parse(localStorage.getItem('referenceDocuments'))
        : (initialMockData.referenceDocuments || []);
      setDocuments(docList);

      // Đọc danh sách collections
      const localCollections = localStorage.getItem('collections')
        ? JSON.parse(localStorage.getItem('collections'))
        : (initialMockData.collections || []);

      if (projectList.length > 0) {
        const firstId = projectList[0].id;
        setSelectedProjectId(firstId);
        
        const filtered = localCollections.filter(col => col.projectId === firstId);
        setCollections(filtered);
      }
    } catch (error) {
      console.error("Error loading instructor context:", error);
      setErrorMessage("Failed to synchronize collections repository with current active projects.");
    } finally {
      setLoading(false);
    }
  };

  // Thay đổi bộ lọc Category Tab
  const handleProjectFilterChange = (pId) => {
    setSelectedProjectId(pId);
    
    // Đọc danh sách mới nhất từ localStorage trước khi filter
    const localCollections = localStorage.getItem('collections')
      ? JSON.parse(localStorage.getItem('collections'))
      : (initialMockData.collections || []);

    if (!pId) {
      setCollections(localCollections);
      return;
    }
    setLoading(true);
    
    setTimeout(() => {
      const filtered = localCollections.filter(col => col.projectId === pId);
      setCollections(filtered);
      setLoading(false);
    }, 200);
  };

  // --- 🌟 2. XỬ LÝ XÓA BỘ TIÊU CHUẨN (ĐỒNG BỘ LOCALSTORAGE) ---
  const handleDeleteCollection = (id) => {
    if (!window.confirm("Are you sure you want to permanently delete this evidence library specification?")) return;
    
    const localCollections = localStorage.getItem('collections')
      ? JSON.parse(localStorage.getItem('collections'))
      : (initialMockData.collections || []);

    const localDocs = localStorage.getItem('referenceDocuments')
      ? JSON.parse(localStorage.getItem('referenceDocuments'))
      : (initialMockData.referenceDocuments || []);

    // Tiến hành lọc bỏ phần tử bị xóa
    const updatedCollections = localCollections.filter(item => item.id !== id);
    const updatedDocs = localDocs.filter(doc => doc.collectionId !== id);

    // Ghi đè lại vào kho lưu trữ bền vững localStorage
    localStorage.setItem('collections', JSON.stringify(updatedCollections));
    localStorage.setItem('referenceDocuments', JSON.stringify(updatedDocs));

    // Cập nhật State cho giao diện render lại ngay lập tức
    setDocuments(updatedDocs);
    setCollections(updatedCollections.filter(col => col.projectId === selectedProjectId));
  };

  // --- 🌟 3. XÓA FILE ĐÃ CÓ (ĐỒNG BỘ LOCALSTORAGE) ---
  const handleDeleteExistingDoc = (docId) => {
    if (!window.confirm("Remove this reference framework file?")) return;
    
    const localDocs = localStorage.getItem('referenceDocuments')
      ? JSON.parse(localStorage.getItem('referenceDocuments'))
      : (initialMockData.referenceDocuments || []);

    const updatedDocs = localDocs.filter(d => d.id !== docId);
    
    // Lưu vào localStorage
    localStorage.setItem('referenceDocuments', JSON.stringify(updatedDocs));
    
    // Đồng bộ state giao diện ngoài bảng và trong Modal
    setDocuments(updatedDocs);
    setCurrentAttachedDocs(prev => prev.filter(d => d.id !== docId));
  };

  // --- HÀM KÍCH HOẠT MODAL CHỈNH SỬA ---
  const openEditModal = (col) => {
    setEditingCollection(col);
    setEditTitle(col.title);
    setEditDescription(col.description || "");
    setEditFiles([]); 
    
    // Tìm tất cả các tài liệu thuộc về collection này từ danh sách tài liệu hiện tại trong State
    const attachedPdfs = documents.filter(doc => doc.collectionId === col.id);
    setCurrentAttachedDocs(attachedPdfs);
    
    setIsEditModalOpen(true);
  };

  // --- XỬ LÝ KHI CHỌN NHIỀU FILE MỚI HÀNG CHỜ ---
  const handleFileChange = (e) => {
    if (e.target.files) {
      setEditFiles(prev => [...prev, ...Array.from(e.target.files)]);
    }
  };

  // Xóa file trong hàng chờ upload
  const handleRemoveNewFileQueue = (index) => {
    setEditFiles(prev => prev.filter((_, i) => i !== index));
  };

  // --- 🌟 4. HÀM XỬ LÝ LƯU THAY ĐỔI (UPDATE & SAVE ĐỒNG BỘ LOCALSTORAGE) ---
  const handleUpdateCollection = (e) => {
    e.preventDefault();
    if (!editTitle.trim()) return;

    // Lấy mảng dữ liệu hiện hành trong localStorage ra để biến đổi
    let localCollections = localStorage.getItem('collections')
      ? JSON.parse(localStorage.getItem('collections'))
      : (initialMockData.collections || []);

    let localDocs = localStorage.getItem('referenceDocuments')
      ? JSON.parse(localStorage.getItem('referenceDocuments'))
      : (initialMockData.referenceDocuments || []);

    // 4.1 Cập nhật thông tin text (Title, Description)
    localCollections = localCollections.map(item => {
      if (item.id === editingCollection.id) {
        return {
          ...item,
          title: editTitle.trim(),
          description: editDescription.trim()
        };
      }
      return item;
    });

    // 4.2 Xử lý phần add hàng chờ nhiều file mới nếu có
    if (editFiles.length > 0) {
      const newDocsMapped = editFiles.map((file, idx) => ({
        id: `doc_${Date.now()}_${idx}`,
        name: file.name,
        collectionId: editingCollection.id,
        fileUrl: URL.createObjectURL(file),
        uploadedAt: new Date().toISOString().split('T')[0]
      }));

      localDocs = [...newDocsMapped, ...localDocs];
    }

    // 4.3 Tính toán lại số lượng tệp mẫu đính kèm mới nhất
    localCollections = localCollections.map(item => {
      if (item.id === editingCollection.id) {
        const currentCount = localDocs.filter(d => d.collectionId === item.id).length;
        return { ...item, documentCount: currentCount };
      }
      return item;
    });

    // 🌟 BƯỚC QUYẾT ĐỊNH: Lưu cập nhật toàn diện vào localStorage
    localStorage.setItem('collections', JSON.stringify(localCollections));
    localStorage.setItem('referenceDocuments', JSON.stringify(localDocs));

    // 4.4 Cập nhật lại State UI lập tức
    setDocuments(localDocs);
    setCollections(localCollections.filter(col => col.projectId === selectedProjectId));

    // Đóng màn hình modal popup
    setIsEditModalOpen(false);
    alert("Collection updated successfully with multi-spec assets!");
  };

  useEffect(() => {
    fetchInitialData();
  }, []);

  return (
    <div className="min-h-screen bg-[#f8fafc] p-8 text-[#0f172a] font-sans">
      <div className="max-w-7xl mx-auto">
        
        {/* HEADER SECTION */}
        <div className="flex justify-between items-center mb-6 border-b border-gray-200 pb-5">
          <div>
            <h1 className="text-3xl font-extrabold text-[#1e3a8a] tracking-tight">Collections</h1>
            <p className="text-gray-500 text-sm mt-1">
              Configure baseline compliance parameters, multi-document templates, and scope evaluation rules.
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

        {/* UPPER ACTION ROW */}
        <div className="flex justify-between items-center mb-6 gap-4">
          <button
            onClick={() => navigate('/instructor/dashboard')}
            className="px-4 py-2 bg-white text-gray-600 border border-gray-200 font-bold text-xs rounded-xl hover:bg-gray-50 transition shadow-sm flex items-center gap-1.5"
          >
            ← Back to Dashboard
          </button>
          
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

        {/* FILTER TOOLBAR */}
        <div className="bg-white p-4 rounded-2xl border border-gray-200 shadow-sm mb-6 flex items-center gap-4">
          <label className="text-xs font-black text-gray-500 uppercase tracking-wide">Category Tab:</label>
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
                  <th className="px-6 py-4">Reference Files Bound</th>                   
                  <th className="px-6 py-4 text-right">Actions</th>                   
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-xs text-gray-700">
                {loading ? (
                  <tr><td colSpan="5" className="px-6 py-8 text-center text-gray-400 font-medium animate-pulse">Synchronizing metadata data stream...</td></tr>
                ) : collections.length === 0 ? (
                  <tr><td colSpan="5" className="px-6 py-8 text-center text-gray-400 font-medium py-12 text-gray-400">No active collection matrix mapped to this category tab.</td></tr>
                ) : (
                  collections.map((col) => {
                    const attachedPdfs = documents.filter(doc => doc.collectionId === col.id);

                    return (
                      <tr key={col.id} className="hover:bg-gray-50/40 transition">
                        <td className="px-6 py-4 font-mono text-gray-400 text-[11px]">{col.id}</td>
                        <td className="px-6 py-4 font-bold text-gray-900">{col.title}</td>
                        <td className="px-6 py-4 text-gray-500 max-w-xs whitespace-pre-wrap break-words leading-relaxed">{col.description || "No description provided."}</td>
                        
                        <td className="px-6 py-4">
                          {attachedPdfs.length > 0 ? (
                            <div className="flex flex-col gap-1.5">
                              {attachedPdfs.map(pdf => (
                                <div key={pdf.id} className="inline-flex items-center justify-between bg-red-50 border border-red-100 px-2 py-1 rounded-lg text-red-700 font-bold max-w-[220px]">
                                  <span className="truncate max-w-[120px] text-[11px]">📕 {pdf.name}</span>
                                  <a 
                                    href={pdf.fileUrl || "#"} 
                                    target="_blank" 
                                    rel="noopener noreferrer"
                                    className="ml-2 text-[9px] bg-red-600 text-white font-black px-1.5 py-0.5 rounded hover:bg-red-700 transition"
                                  >
                                    View
                                  </a>
                                </div>
                              ))}
                            </div>
                          ) : (
                            <span className="text-gray-400 italic">No document bound</span>
                          )}
                        </td>

                        <td className="px-6 py-4 text-right space-x-3">
                          <button 
                            onClick={() => openEditModal(col)}
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

        {/* MODAL POPUP */}
        {isEditModalOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4 animate-fadeIn">
            <div className="bg-white rounded-3xl border border-gray-200 shadow-2xl max-w-lg w-full p-6 space-y-5 text-xs text-left max-h-[90vh] overflow-y-auto">
              
              <div>
                <h3 className="text-lg font-black text-[#1e3a8a]">Update Collection Specifications</h3>
                <p className="text-[11px] text-gray-400">Modify metadata parameters and manage multiple active reference assets.</p>
              </div>

              <form onSubmit={handleUpdateCollection} className="space-y-4">
                
                <div className="space-y-1">
                  <label className="text-[10px] font-black text-gray-500 uppercase tracking-wide">Collection Schema Title</label>
                  <input 
                    type="text" required value={editTitle}
                    onChange={(e) => setEditTitle(e.target.value)}
                    className="w-full px-3 py-2.5 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition"
                  />
                </div>

                <div className="space-y-1">
                  <label className="text-[10px] font-black text-gray-500 uppercase tracking-wide">Boundary Specification Description</label>
                  <textarea 
                    rows="3" value={editDescription}
                    onChange={(e) => setEditDescription(e.target.value)}
                    className="w-full px-3 py-2.5 bg-gray-50 border border-gray-200 rounded-xl font-medium text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#1e3a8a] focus:bg-white transition"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-[10px] font-black text-gray-500 uppercase tracking-wide block">Current Attached Files ({currentAttachedDocs.length})</label>
                  {currentAttachedDocs.length === 0 ? (
                    <p className="text-gray-400 italic text-[11px]">No documents currently attached.</p>
                  ) : (
                    <div className="space-y-1">
                      {currentAttachedDocs.map(doc => (
                        <div key={doc.id} className="flex justify-between items-center bg-gray-50 px-3 py-2 rounded-xl border border-gray-100">
                          <span className="font-medium text-gray-700 truncate max-w-[280px]">📄 {doc.name}</span>
                          <button 
                            type="button"
                            onClick={() => handleDeleteExistingDoc(doc.id)}
                            className="text-rose-600 font-bold hover:underline text-[10px]"
                          >
                            Remove
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                <div className="space-y-2">
                  <label className="text-[10px] font-black text-gray-500 uppercase tracking-wide block">Upload Additional PDF Assets (Multiple)</label>
                  <div className="border border-dashed border-gray-200 rounded-xl p-4 bg-gray-50/50 flex flex-col items-center justify-center space-y-2">
                    <label className="px-4 py-2 bg-white border border-gray-200 rounded-xl text-gray-700 font-bold shadow-sm cursor-pointer hover:bg-gray-100 text-xs inline-block">
                      📎 Select Files (.pdf)
                      <input 
                        type="file" accept=".pdf" multiple
                        onChange={handleFileChange} 
                        className="hidden" 
                      />
                    </label>
                    <span className="text-[10px] text-gray-400">You can pick one or more template files at once</span>
                  </div>

                  {editFiles.length > 0 && (
                    <div className="mt-2 space-y-1">
                      <span className="text-[10px] font-bold text-blue-600 block">Queue to be added ({editFiles.length}):</span>
                      {editFiles.map((f, index) => (
                        <div key={index} className="flex justify-between items-center bg-blue-50/60 border border-blue-100 px-3 py-1.5 rounded-lg text-blue-800">
                          <span className="font-medium truncate max-w-[280px]">✨ New: {f.name}</span>
                          <button 
                            type="button" 
                            onClick={() => handleRemoveNewFileQueue(index)}
                            className="text-gray-400 hover:text-rose-600 text-xs font-bold"
                          >
                            ✕
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

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