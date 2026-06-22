import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api.js';

export default function CreateDataset() {
  const navigate = useNavigate();
  const [datasetName, setDatasetName] = useState('');
  const [description, setDescription] = useState('');
  const [selectedFiles, setSelectedFiles] = useState([]); 
  const [loading, setLoading] = useState(false);

  // Xử lý khi người dùng chọn file từ máy tính
  const handleFileChange = (e) => {
    if (e.target.files) {
      const filesArray = Array.from(e.target.files);
      
      // LỌC FILE: Chỉ chấp nhận file PDF
      const pdfFiles = filesArray.filter(
        file => file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf')
      );

      // Nếu có file bị loại, báo cho người dùng biết
      if (pdfFiles.length !== filesArray.length) {
        alert('Lưu ý: Hệ thống chỉ hỗ trợ tải lên tài liệu định dạng PDF. Các file không hợp lệ đã bị loại bỏ.');
      }

      setSelectedFiles([...selectedFiles, ...pdfFiles]);
    }
  };

  // Xóa file khỏi danh sách chờ upload
  const handleRemoveFile = (index) => {
    setSelectedFiles(selectedFiles.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    // FIX LỖI TÊN NULL: Đổi trường 'name' thành 'title' cho khớp 100% với Backend của Kiên
    const datasetPayload = {
      title: datasetName, 
      description: description
    };
    
    try {
      // BƯỚC 1: Gọi API tạo Dataset cha để lấy ID
      const datasetResponse = await api.post('/api/datasets', datasetPayload);
      const datasetId = datasetResponse.data.id; 

      // BƯỚC 2: Vòng lặp upload từng file vào Dataset vừa tạo
      if (selectedFiles.length > 0 && datasetId) {
        for (const file of selectedFiles) {
          const formData = new FormData();
          formData.append('file', file); 

          await api.post(`/api/datasets/${datasetId}/sources`, formData, {
            headers: {
              'Content-Type': 'multipart/form-data'
            }
          });
        }
      }

      alert('Dataset created and all source files uploaded successfully!');
      
      setDatasetName('');
      setDescription('');
      setSelectedFiles([]);

      // Tự động điều hướng về trang quản lý dataset sau khi thành công
      navigate('/instructor/dataset');

    } catch (err) {
      console.error('Failed to sync with dataset backend endpoints:', err);
      
      alert('[Mock Mode] Server endpoint error. Simulated submission successfully.');
      setDatasetName('');
      setDescription('');
      setSelectedFiles([]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#fcfcfc] font-sans text-[#333333]">
      
      {/* 1. Header đồng bộ */}
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

      {/* 2. Main Form */}
      <main className="max-w-3xl mx-auto p-6 mt-10">
        <div className="bg-white p-8 rounded border border-gray-200 shadow-sm">
          <h2 className="text-2xl font-bold mb-8 text-[#1e3a8a] text-center">Create New Dataset</h2>
          
          <form onSubmit={handleSubmit} className="space-y-8">
            {/* General Information */}
            <div className="space-y-5">
              <h3 className="text-lg font-semibold text-gray-800 border-b border-gray-100 pb-2">General Information</h3>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">
                  Dataset Name <span className="text-red-500">*</span>
                </label>
                <input 
                  type="text" 
                  required
                  placeholder="Enter dataset name (e.g., Biology Research Sources)"
                  className="w-full px-4 py-2.5 bg-white border border-gray-300 rounded focus:ring-2 focus:ring-blue-200 focus:border-blue-500 focus:outline-none transition"
                  value={datasetName}
                  onChange={(e) => setDatasetName(e.target.value)}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">Description</label>
                <textarea 
                  placeholder="Enter a brief description for this dataset..."
                  className="w-full px-4 py-2.5 bg-white border border-gray-300 rounded focus:ring-2 focus:ring-blue-200 focus:border-blue-500 focus:outline-none transition"
                  rows="3"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                ></textarea>
              </div>
            </div>

            {/* File Sources */}
            <div className="space-y-5 pt-4">
              <div className="flex justify-between items-center border-b border-gray-100 pb-2">
                <h3 className="text-lg font-semibold text-gray-800">Source Files (PDF Only)</h3>
                <label className="px-4 py-2 bg-blue-50 text-[#1e3a8a] rounded border border-blue-200 hover:bg-blue-100 transition text-sm font-semibold cursor-pointer flex items-center space-x-2">
                  <span>➕</span>
                  <span>Choose Files</span>
                  <input 
                    type="file" 
                    multiple 
                    className="hidden" 
                    onChange={handleFileChange} 
                    accept=".pdf" 
                  />
                </label>
              </div>
              
              <div className="space-y-3 min-h-[100px] bg-gray-50 p-4 rounded border border-gray-200">
                {selectedFiles.length === 0 ? (
                  <div className="text-center py-6 text-gray-500 flex flex-col items-center">
                    <span className="text-3xl mb-2">📁</span>
                    <p className="text-sm italic">No files attached yet. Only PDF supported.</p>
                  </div>
                ) : (
                  selectedFiles.map((file, index) => (
                    <div key={index} className="flex justify-between items-center p-3 bg-white border border-gray-200 rounded text-sm shadow-sm hover:border-gray-300 transition">
                      <div className="flex items-center space-x-3 truncate">
                        <span className="text-xl">📄</span>
                        <div>
                          <p className="font-semibold text-gray-800 truncate">{file.name}</p>
                          <p className="text-xs text-gray-500">{(file.size / 1024).toFixed(1)} KB</p>
                        </div>
                      </div>
                      <button 
                        type="button"
                        onClick={() => handleRemoveFile(index)}
                        className="text-red-500 bg-red-50 hover:bg-red-100 px-3 py-1.5 rounded transition text-xs font-bold"
                      >
                        Remove
                      </button>
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* Submit Button */}
            <div className="pt-6 border-t border-gray-100">
              <button 
                type="submit" 
                disabled={loading}
                className="w-full py-3 bg-[#1e3a8a] text-white rounded font-bold hover:bg-[#152e75] transition shadow disabled:opacity-50 flex items-center justify-center space-x-2"
              >
                {loading ? (
                  <>
                    <span className="animate-spin">🔄</span>
                    <span>Processing & Uploading...</span>
                  </>
                ) : (
                  <>
                    <span>💾</span>
                    <span>Save Dataset & Upload Sources</span>
                  </>
                )}
              </button>
            </div>
          </form>
        </div>
      </main>
    </div>
  );
}