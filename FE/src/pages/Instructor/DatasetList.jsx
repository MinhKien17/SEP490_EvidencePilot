import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api.js'; 

export default function DatasetList() {
  const navigate = useNavigate();
  const [datasets, setDatasets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  const [expandedDatasetId, setExpandedDatasetId] = useState(null);
  const [datasetSources, setDatasetSources] = useState({});
  const [loadingSources, setLoadingSources] = useState(false);

  // --- STATE PHỤC VỤ TÍNH NĂNG UPDATE ---
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isUpdating, setIsUpdating] = useState(false);
  const [editingDatasetId, setEditingDatasetId] = useState(null);
  const [editTitle, setEditTitle] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [selectedFile, setSelectedFile] = useState(null);

  useEffect(() => {
    fetchDatasets();
  }, []);

  const fetchDatasets = async () => {
    try {
      setLoading(true);
      const response = await api.get('/api/datasets');
      setDatasets(Array.isArray(response.data) ? response.data : []);
      setError(null);
    } catch (err) {
      console.error('Failed to fetch datasets:', err);
      setError('Failed to load datasets. Please check your connection!');
    } finally {
      setLoading(false);
    }
  };

  const fetchSources = async (datasetId) => {
    try {
      setLoadingSources(true);
      const response = await api.get(`/api/datasets/${datasetId}/sources`);
      setDatasetSources(prev => ({
        ...prev,
        [datasetId]: Array.isArray(response.data) ? response.data : []
      }));
    } catch (err) {
      console.error('Failed to fetch dataset sources:', err);
    } finally {
      setLoadingSources(false);
    }
  };

  const handleToggleExpand = async (datasetId) => {
    if (expandedDatasetId === datasetId) {
      setExpandedDatasetId(null);
      return;
    }
    setExpandedDatasetId(datasetId);
    if (!datasetSources[datasetId]) {
      await fetchSources(datasetId);
    }
  };

  const handleDeleteDataset = async (datasetId) => {
    const isConfirmed = window.confirm('Are you sure you want to delete this dataset? This action cannot be undone.');
    if (!isConfirmed) return;

    try {
      await api.delete(`/api/datasets/${datasetId}`);
      setDatasets(prev => prev.filter(d => d.id !== datasetId));
      if (expandedDatasetId === datasetId) {
        setExpandedDatasetId(null);
      }
    } catch (err) {
      console.error('Failed to delete dataset:', err);
      alert('Failed to delete dataset. Please try again.');
    }
  };

  // 🔥 ĐÃ FIX: CHỨC NĂNG XÓA FILE CON THEO ĐƯỜNG DẪN ĐỘC LẬP /api/sources/:id
  const handleDeleteSource = async (e, datasetId, sourceId) => {
    e.stopPropagation(); 
    const isConfirmed = window.confirm('Are you sure you want to remove this source file?');
    if (!isConfirmed) return;

    try {
      // Đổi từ URL lồng nhau sang URL direct thẳng tới sourceId để tránh lỗi 404
      await api.delete(`/api/sources/${sourceId}`);
      
      // Xóa thành công thì cập nhật lại UI state ngay lập tức
      setDatasetSources(prev => ({
        ...prev,
        [datasetId]: prev[datasetId].filter(source => source.id !== sourceId)
      }));
      
      alert('Source file removed successfully!');
    } catch (err) {
      console.error('Failed to delete source file:', err);
      alert(err.response?.data?.message || 'Failed to delete source file. Please make sure the API method is correct.');
    }
  };

  // MỞ MODAL CHỈNH SỬA & ĐỔ DỮ LIỆU CŨ VÀO FORM
  const handleOpenEditModal = (e, dataset) => {
    e.stopPropagation(); 
    setEditingDatasetId(dataset.id);
    setEditTitle(dataset.title || '');
    setEditDescription(dataset.description || '');
    setSelectedFile(null); 
    setIsEditModalOpen(true);
  };

  // GỬI DỮ LIỆU CẬP NHẬT LÊN BACKEND
  const handleUpdateDataset = async (e) => {
    e.preventDefault();
    if (!editTitle.trim()) {
      alert('Dataset name cannot be empty!');
      return;
    }

    try {
      setIsUpdating(true);

      // 1. Cập nhật Tiêu đề & Mô tả
      await api.put(`/api/datasets/${editingDatasetId}`, {
        title: editTitle,
        description: editDescription
      });

      // 2. Nếu chọn đính kèm thêm file mới thì tiến hành upload
      if (selectedFile) {
        const formData = new FormData();
        formData.append('file', selectedFile);

        await api.post(`/api/datasets/${editingDatasetId}/sources`, formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });
        
        await fetchSources(editingDatasetId);
      }

      setDatasets(prev => prev.map(d => 
        d.id === editingDatasetId ? { ...d, title: editTitle, description: editDescription } : d
      ));

      setIsEditModalOpen(false);
      alert('Dataset updated successfully!');
    } catch (err) {
      console.error('Failed to update dataset:', err);
      alert(err.response?.data?.message || 'Failed to update dataset.');
    } finally {
      setIsUpdating(false);
    }
  };

  // XỬ LÝ XEM FILE PDF CÓ ĐÍNH KÈM TOKEN ĐĂNG NHẬP
  const handleViewFile = async (e, datasetId, fileId, fileName) => {
    e.stopPropagation(); 
    try {
      const token = localStorage.getItem('token') || localStorage.getItem('accessToken') || localStorage.getItem('jwt');
      const headers = {};
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      const response = await api.get(`/api/datasets/${datasetId}/sources/${fileId}`, {
        responseType: 'blob',
        headers: headers 
      });

      const fileBlob = new Blob([response.data], { type: 'application/pdf' });
      const fileUrl = URL.createObjectURL(fileBlob);

      const newTab = window.open();
      if (newTab) {
        newTab.location.href = fileUrl;
      } else {
        const link = document.createElement('a');
        link.href = fileUrl;
        link.download = fileName || 'document.pdf';
        link.click();
      }
    } catch (err) {
      console.error('Failed to open PDF file:', err);
      alert('Không thể mở file PDF này. Hãy kiểm tra lại kết nối hoặc quyền truy cập.');
    }
  };

  return (
    <div className="min-h-screen bg-[#fcfcfc] font-sans text-[#333333]">
      
      {/* HEADER SECTION */}
      <header className="bg-[#1e3a8a] text-white border-b border-[#152e75] sticky top-0 z-10 shadow-sm">
        <div className="w-full px-8 h-16 flex items-center justify-between">
          <div className="flex items-center space-x-3 cursor-pointer" onClick={() => navigate('/instructor/dashboard')}>
            <span className="font-bold text-xl tracking-wider">Evidence Pilot</span>
          </div>
          <div className="flex items-center space-x-6">
            <button 
              onClick={() => navigate('/instructor/dashboard')}
              className="text-sm font-medium text-blue-200 hover:text-white transition"
            >
              Back to Dashboard
            </button>
          </div>
        </div>
      </header>

      {/* MAIN CONTAINER */}
      <main className="max-w-5xl mx-auto p-6 mt-6">
        
        {/* TITLE BAR */}
        <div className="flex justify-between items-center mb-8">
          <div>
            <h2 className="text-2xl font-bold text-[#1e3a8a]">Dataset Management</h2>
            <p className="text-sm text-gray-500 mt-1">Manage and view your teaching resource files</p>
          </div>
          <button
            onClick={() => navigate('/instructor/dataset/create')} 
            className="px-5 py-2.5 bg-[#1e3a8a] text-white rounded font-semibold hover:bg-[#152e75] transition shadow flex items-center space-x-2 text-sm"
          >
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-4 h-4">
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
            </svg>
            <span>Create New Dataset</span>
          </button>
        </div>

        {/* DATASET LIST RENDERING */}
        {loading ? (
          <div className="text-center py-12">
            <div className="animate-spin inline-block w-8 h-8 border-[3px] border-current border-t-transparent text-blue-600 rounded-full mb-3"></div>
            <p className="text-gray-500 italic">Loading datasets from server...</p>
          </div>
        ) : error ? (
          <div className="bg-red-50 border border-red-200 text-red-700 p-4 rounded text-center my-6">
            <p className="font-semibold">{error}</p>
            <button onClick={fetchDatasets} className="mt-3 px-4 py-1.5 bg-red-600 text-white rounded text-xs font-bold hover:bg-red-700 transition">Retry</button>
          </div>
        ) : datasets.length === 0 ? (
          <div className="bg-white border border-gray-200 rounded p-12 text-center shadow-sm">
            <h3 className="text-lg font-bold text-gray-700 mb-1">No Datasets Found</h3>
          </div>
        ) : (
          <div className="space-y-4">
            {datasets.map((dataset) => {
              const isExpanded = expandedDatasetId === dataset.id;
              const sources = datasetSources[dataset.id] || [];

              return (
                <div key={dataset.id} className="bg-white border border-gray-200 rounded shadow-sm hover:border-gray-300 transition overflow-hidden group">
                  
                  {/* FOLDER ACCORDION HEADER */}
                  <div 
                    onClick={() => handleToggleExpand(dataset.id)}
                    className="p-5 flex justify-between items-center cursor-pointer select-none bg-white hover:bg-gray-50/80 transition"
                  >
                    <div className="flex items-center space-x-4 flex-1 truncate mr-4">
                      <div className="bg-blue-50/50 p-2.5 rounded text-[#1e3a8a]">
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-8 h-8 opacity-80 group-hover:opacity-100 transition">
                          <path d="M19.5 21a3 3 0 0 0 3-3v-4.5a3 3 0 0 0-3-3h-15a3 3 0 0 0-3 3V18a3 3 0 0 0 3 3h15ZM1.5 10.146V6a3 3 0 0 1 3-3h5.379a2.25 2.25 0 0 1 1.59.659l2.122 2.121c.14.141.331.22.53.22H19.5a3 3 0 0 1 3 3v1.146A4.483 4.483 0 0 0 19.5 9h-15a4.483 4.483 0 0 0-3 1.146Z" />
                        </svg>
                      </div>
                      <div className="truncate">
                        <h4 className="font-bold text-gray-800 text-base truncate group-hover:text-blue-700 transition">
                          {dataset.title || <span className="italic text-gray-400">Untitled Dataset</span>}
                        </h4>
                        <p className="text-sm text-gray-500 truncate mt-0.5">{dataset.description || <span className="italic text-gray-400">No description provided</span>}</p>
                      </div>
                    </div>
                    
                    <div className="flex items-center space-x-3">
                      <span className="text-xs px-2.5 py-1 bg-gray-100 text-gray-500 rounded-full font-medium">ID: {dataset.id}</span>

                      {/* ICON BÚT CHÌ ĐƠN THUẦN CHỈNH SỬA */}
                      <button
                        onClick={(e) => handleOpenEditModal(e, dataset)}
                        className="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-full transition duration-200"
                        title="Update Dataset"
                      >
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                          <path strokeLinecap="round" strokeLinejoin="round" d="m16.862 4.487 1.687-1.688a1.875 1.875 0 1 1 2.652 2.652L6.832 19.82a4.5 4.5 0 0 1-1.897 1.13l-2.685.8.8-2.685a4.5 4.5 0 0 1 1.13-1.897L16.863 4.487Zm0 0L19.5 7.125" />
                        </svg>
                      </button>

                      <button
                        onClick={(e) => { e.stopPropagation(); handleDeleteDataset(dataset.id); }}
                        className="p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-full transition duration-200"
                        title="Delete Dataset"
                      >
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                          <path strokeLinecap="round" strokeLinejoin="round" d="m14.74 9-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 0 1-2.244 2.077H8.084a2.25 2.25 0 0 1-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 0 0-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 0 1 3.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 0 0-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 0 0-7.5 0" />
                        </svg>
                      </button>

                      <div className={`p-1 text-gray-400 transition-transform duration-300 ease-in-out ${isExpanded ? 'rotate-180 text-[#1e3a8a]' : ''}`}>
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-5 h-5">
                          <path strokeLinecap="round" strokeLinejoin="round" d="m19.5 8.25-7.5 7.5-7.5-7.5" />
                        </svg>
                      </div>
                    </div>
                  </div>

                  {/* SUB-FILES DROPDOWN PANEL */}
                  <div className={`grid transition-all duration-300 ease-in-out ${isExpanded ? 'grid-rows-[1fr] opacity-100' : 'grid-rows-[0fr] opacity-0'}`}>
                    <div className="overflow-hidden">
                      <div className="bg-gray-50/50 border-t border-gray-100 p-5 pl-[4.5rem] space-y-3">
                        <h5 className="text-xs font-bold uppercase tracking-wider text-gray-400 mb-2">Attached Source Files ({loadingSources ? '...' : sources.length})</h5>

                        {loadingSources ? (
                          <div className="text-sm text-gray-500 italic py-2 flex items-center space-x-2">
                            <div className="animate-spin w-4 h-4 border-2 border-current border-t-transparent text-gray-400 rounded-full"></div>
                            <span>Fetching files...</span>
                          </div>
                        ) : sources.length === 0 ? (
                          <div className="text-sm text-gray-400 italic py-2">This dataset is empty (No PDF files attached yet).</div>
                        ) : (
                          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                            {sources.map((file) => {
                              const currentFileName = file.fileName || file.name || 'Unknown_File.pdf';
                              
                              return (
                                <div key={file.id} className="flex items-center justify-between p-3 bg-white border border-gray-200 rounded text-sm shadow-sm hover:border-blue-200 transition">
                                  <div className="flex items-center space-x-3 truncate mr-2 flex-1">
                                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-6 h-6 text-gray-400 flex-shrink-0">
                                      <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z" />
                                    </svg>
                                    <div className="truncate flex-1">
                                      <button 
                                        type="button"
                                        onClick={(e) => handleViewFile(e, dataset.id, file.id, currentFileName)}
                                        className="font-semibold text-gray-700 hover:text-blue-600 hover:underline block truncate text-left w-full focus:outline-none"
                                        title="Click to view file"
                                      >
                                        {currentFileName}
                                      </button>
                                      <p className="text-xs text-gray-400">ID: {file.id}</p>
                                    </div>
                                  </div>

                                  {/* NÚT XÓA FILE ĐÃ ĐƯỢC FIX LỖI ROUTER BACKEND */}
                                  <div className="flex items-center space-x-2 flex-shrink-0">
                                    <span className="text-[10px] text-green-700 bg-green-50 px-2 py-0.5 rounded font-bold uppercase tracking-wide">
                                      Active
                                    </span>
                                    <button
                                      type="button"
                                      onClick={(e) => handleDeleteSource(e, dataset.id, file.id)}
                                      className="p-1 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded transition cursor-pointer"
                                      title="Delete this file"
                                    >
                                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-4 h-4">
                                        <path strokeLinecap="round" strokeLinejoin="round" d="M6 18 18 6M6 6l12 12" />
                                      </svg>
                                    </button>
                                  </div>

                                </div>
                              );
                            })}
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </main>

      {/* ================= EDIT DATASET MODAL ================= */}
      {isEditModalOpen && (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
          <div className="bg-white rounded-lg shadow-xl border border-gray-200 max-w-lg w-full overflow-hidden">
            <div className="bg-[#1e3a8a] text-white p-4 flex justify-between items-center">
              <h3 className="font-bold text-lg">Update Dataset (ID: {editingDatasetId})</h3>
              <button onClick={() => setIsEditModalOpen(false)} className="text-white/80 hover:text-white text-xl font-bold focus:outline-none">&times;</button>
            </div>

            <form onSubmit={handleUpdateDataset} className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-bold text-gray-600 uppercase mb-1">Dataset Name *</label>
                <input 
                  type="text" 
                  value={editTitle}
                  onChange={(e) => setEditTitle(e.target.value)}
                  className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-[#1e3a8a] transition"
                  placeholder="Enter dataset name..."
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-600 uppercase mb-1">Description</label>
                <textarea 
                  value={editDescription}
                  onChange={(e) => setEditDescription(e.target.value)}
                  rows={3}
                  className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:border-[#1e3a8a] transition"
                  placeholder="Enter dataset descriptions..."
                />
              </div>

              <div className="border-t border-gray-100 pt-4">
                <label className="block text-xs font-bold text-gray-600 uppercase mb-1">Source Files (PDF only)</label>
                <p className="text-[11px] text-gray-400 mb-2">Choose a file if you want to append/upload more document files to this dataset</p>
                <div className="relative border border-gray-300 rounded px-3 py-2.5 bg-gray-50 flex items-center hover:bg-gray-100 transition cursor-pointer">
                  <input 
                    type="file" 
                    accept="application/pdf"
                    onChange={(e) => setSelectedFile(e.target.files[0])}
                    className="absolute inset-0 opacity-0 w-full h-full cursor-pointer z-10"
                  />
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5 text-gray-500 mr-2 flex-shrink-0">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M12 16.5V9.75m0 0l3 3m-3-3l-3 3M6.75 19.5a4.5 4.5 0 01-1.41-8.775 5.25 5.25 0 0110.233-2.33 3 3 0 013.758 3.848A3.752 3.752 0 0118 19.5H6.75z" />
                  </svg>
                  <span className="text-sm font-medium text-gray-600 truncate">
                    {selectedFile ? selectedFile.name : "Choose new PDF file..."}
                  </span>
                </div>
              </div>

              <div className="flex justify-end items-center space-x-3 pt-4 border-t border-gray-100">
                <button
                  type="button"
                  disabled={isUpdating}
                  onClick={() => setIsEditModalOpen(false)}
                  className="px-4 py-2 text-sm font-semibold text-gray-500 bg-gray-100 rounded hover:bg-gray-200 transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isUpdating}
                  className="px-5 py-2 text-sm font-semibold text-white bg-[#1e3a8a] rounded hover:bg-[#152e75] transition flex items-center space-x-2"
                >
                  {isUpdating ? (
                    <>
                      <div className="animate-spin w-3 h-3 border-2 border-white border-t-transparent rounded-full"></div>
                      <span>Saving Changes...</span>
                    </>
                  ) : (
                    <span>Save Changes</span>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}