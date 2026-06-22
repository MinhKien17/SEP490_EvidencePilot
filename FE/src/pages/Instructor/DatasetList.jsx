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

  const handleToggleExpand = async (datasetId) => {
    if (expandedDatasetId === datasetId) {
      setExpandedDatasetId(null);
      return;
    }

    setExpandedDatasetId(datasetId);

    if (!datasetSources[datasetId]) {
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

  // 🔥 HÀM XỬ LÝ MỞ FILE PDF CHUẨN CÓ ĐÍNH KÈM TOKEN ĐĂNG NHẬP
  const handleViewFile = async (e, datasetId, fileId, fileName) => {
    e.stopPropagation(); // Ngăn chặn sự kiện đóng/mở folder cha khi click vào nút xem file
    
    try {
      // Lấy token đăng nhập từ localStorage (đề phòng các trường hợp key đặt tên là 'token' hoặc 'accessToken')
      const token = localStorage.getItem('token') || localStorage.getItem('accessToken') || localStorage.getItem('jwt');
      
      const headers = {};
      if (token) {
        // Đính kèm token Bearer theo đúng chuẩn cấu hình bảo mật (Authorize) 
        headers['Authorization'] = `Bearer ${token}`;
      }

      // Gọi API lấy file dưới dạng blob (binary data)
      // Nếu đường dẫn endpoint download file thực tế khác đoạn này, bạn chỉ cần sửa lại URL trong `api.get`
      const response = await api.get(`/api/datasets/${datasetId}/sources/${fileId}`, {
        responseType: 'blob',
        headers: headers 
      });

      // Tạo một URL tạm thời đại diện cho dữ liệu nhị phân của file PDF nhận được từ server
      const fileBlob = new Blob([response.data], { type: 'application/pdf' });
      const fileUrl = URL.createObjectURL(fileBlob);

      // Mở file ra một tab mới hoàn toàn sạch sẽ
      const newTab = window.open();
      if (newTab) {
        newTab.location.href = fileUrl;
      } else {
        // Dự phòng trường hợp trình duyệt chặn popup block không cho mở tab mới tự động
        const link = document.createElement('a');
        link.href = fileUrl;
        link.download = fileName || 'document.pdf';
        link.click();
      }
    } catch (err) {
      console.error('Failed to open PDF file:', err);
      const status = err.response?.status;
      if (status === 401 || status === 403) {
        alert('Phiên đăng nhập đã hết hạn hoặc bạn không có quyền xem file này (Lỗi Authen Token).');
      } else if (status === 404) {
        alert('Không tìm thấy file trên hệ thống (Lỗi 404 - Hãy kiểm tra lại ID Dataset/Source có trùng DB Backend không).');
      } else {
        alert(`Không thể mở file PDF này. Mã lỗi hệ thống: ${status || 'Unknown error'}`);
      }
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

        {/* LOADING & ERROR STATES */}
        {loading ? (
          <div className="text-center py-12">
            <div className="animate-spin inline-block w-8 h-8 border-[3px] border-current border-t-transparent text-blue-600 rounded-full mb-3" role="status" aria-label="loading"></div>
            <p className="text-gray-500 italic">Loading datasets from server...</p>
          </div>
        ) : error ? (
          <div className="bg-red-50 border border-red-200 text-red-700 p-4 rounded text-center my-6">
            <p className="font-semibold">{error}</p>
            <button 
              onClick={fetchDatasets}
              className="mt-3 px-4 py-1.5 bg-red-600 text-white rounded text-xs font-bold hover:bg-red-700 transition"
            >
              Retry
            </button>
          </div>
        ) : datasets.length === 0 ? (
          <div className="bg-white border border-gray-200 rounded p-12 text-center shadow-sm">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1} stroke="currentColor" className="w-16 h-16 mx-auto text-gray-300 mb-4">
              <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 9.776c.112-.017.227-.026.344-.026h15.812c.117 0 .232.009.344.026m-16.5 0a2.25 2.25 0 0 0-1.883 2.542l.857 6a2.25 2.25 0 0 0 2.227 1.932H19.05a2.25 2.25 0 0 0 2.227-1.932l.857-6a2.25 2.25 0 0 0-1.883-2.542m-16.5 0V6A2.25 2.25 0 0 1 6 3.75h3.879a1.5 1.5 0 0 1 1.06.44l2.122 2.12a1.5 1.5 0 0 0 1.06.44H18A2.25 2.25 0 0 1 20.25 9v.776" />
            </svg>
            <h3 className="text-lg font-bold text-gray-700 mb-1">No Datasets Found</h3>
            <p className="text-gray-500 text-sm max-w-sm mx-auto mb-6">You haven't created any datasets yet.</p>
          </div>
        ) : (
          /* DATASET LIST RENDERING */
          <div className="space-y-4">
            {datasets.map((dataset) => {
              const isExpanded = expandedDatasetId === dataset.id;
              const sources = datasetSources[dataset.id] || [];

              return (
                <div 
                  key={dataset.id} 
                  className="bg-white border border-gray-200 rounded shadow-sm hover:border-gray-300 transition overflow-hidden group"
                >
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
                        <p className="text-sm text-gray-500 truncate mt-0.5">
                          {dataset.description || <span className="italic text-gray-400">No description provided</span>}
                        </p>
                      </div>
                    </div>
                    
                    <div className="flex items-center space-x-5">
                      <span className="text-xs px-2.5 py-1 bg-gray-100 text-gray-500 rounded-full font-medium">
                        ID: {dataset.id}
                      </span>

                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDeleteDataset(dataset.id);
                        }}
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
                        <h5 className="text-xs font-bold uppercase tracking-wider text-gray-400 mb-2">
                          Attached Source Files ({loadingSources ? '...' : sources.length})
                        </h5>

                        {loadingSources ? (
                          <div className="text-sm text-gray-500 italic py-2 flex items-center space-x-2">
                            <div className="animate-spin w-4 h-4 border-2 border-current border-t-transparent text-gray-400 rounded-full"></div>
                            <span>Fetching files...</span>
                          </div>
                        ) : sources.length === 0 ? (
                          <div className="text-sm text-gray-400 italic py-2 flex items-center">
                            This dataset is empty (No PDF files attached yet).
                          </div>
                        ) : (
                          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                            {sources.map((file) => {
                              const currentFileName = file.fileName || file.name || 'Unknown_File.pdf';
                              
                              return (
                                <div 
                                  key={file.id} 
                                  className="flex items-center justify-between p-3 bg-white border border-gray-200 rounded text-sm shadow-sm hover:border-blue-200 transition"
                                >
                                  <div className="flex items-center space-x-3 truncate mr-2 flex-1">
                                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-6 h-6 text-gray-400 flex-shrink-0">
                                      <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z" />
                                    </svg>
                                    <div className="truncate flex-1">
                                      {/* TRIGGER BUTTON: Gọi hàm handleViewFile có gửi kèm Token */}
                                      <button 
                                        type="button"
                                        onClick={(e) => handleViewFile(e, dataset.id, file.id, currentFileName)}
                                        className="font-semibold text-gray-700 hover:text-blue-600 hover:underline block truncate text-left w-full cursor-pointer focus:outline-none"
                                        title="Click to view/download file"
                                      >
                                        {currentFileName}
                                      </button>
                                      <p className="text-xs text-gray-400">ID: {file.id}</p>
                                    </div>
                                  </div>
                                  <span className="text-[11px] text-[#1e3a8a] bg-blue-50 px-2.5 py-1 rounded-full font-bold uppercase tracking-wide flex-shrink-0">
                                    Active
                                  </span>
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
    </div>
  );
}