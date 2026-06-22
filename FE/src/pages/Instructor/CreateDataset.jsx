import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api.js';

export default function CreateDataset() {
  const navigate = useNavigate();
  
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [files, setFiles] = useState([]);
  
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');
  
  const fileInputRef = useRef(null);

  const handleFileChange = (e) => {
    const selectedFiles = Array.from(e.target.files);
    const pdfFiles = selectedFiles.filter(file => file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf'));
    
    if (pdfFiles.length !== selectedFiles.length) {
      alert('Some files were ignored. Please upload PDF files only.');
    }
    
    setFiles(prev => [...prev, ...pdfFiles]);
    e.target.value = null;
  };

  const handleRemoveFile = (indexToRemove) => {
    setFiles(prev => prev.filter((_, index) => index !== indexToRemove));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!title.trim()) {
      setError('Dataset Name is required.');
      return;
    }
    if (files.length === 0) {
      setError('Please attach at least one PDF document.');
      return;
    }

    setError('');
    setIsSubmitting(true);

    try {
      // BƯỚC 1: Gọi API tạo Dataset cha (Gửi JSON như bản cũ của bạn)
      const datasetPayload = {
        title: title, 
        description: description
      };
      const datasetResponse = await api.post('/api/datasets', datasetPayload);
      const datasetId = datasetResponse.data.id; 

      // BƯỚC 2: Vòng lặp upload từng file vào Dataset vừa tạo
      if (files.length > 0 && datasetId) {
        for (const file of files) {
          const formData = new FormData();
          formData.append('file', file); // Backend nhận biến tên là 'file'

          await api.post(`/api/datasets/${datasetId}/sources`, formData, {
            headers: {
              'Content-Type': 'multipart/form-data'
            }
          });
        }
      }
      
      // Thành công thì quay về trang quản lý Dataset
      navigate('/instructor/dataset');
    } catch (err) {
      console.error('Failed to create dataset:', err);
      setError(err.response?.data?.message || 'Failed to create dataset. Please check your connection and try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#fcfcfc] font-sans text-[#333333]">
      {/* HEADER */}
      <header className="bg-[#1e3a8a] text-white border-b border-[#152e75] sticky top-0 z-10 shadow-sm">
        <div className="w-full px-8 h-16 flex items-center justify-between">
          <div className="flex items-center space-x-3 cursor-pointer" onClick={() => navigate('/instructor/dashboard')}>
            <span className="font-bold text-xl tracking-wider">Evidence Pilot</span>
          </div>
          <div className="flex items-center space-x-6">
            <button 
              onClick={() => navigate('/instructor/dataset')}
              className="text-sm font-medium text-blue-200 hover:text-white transition"
            >
              Back to Datasets
            </button>
          </div>
        </div>
      </header>

      {/* MAIN CONTENT */}
      <main className="max-w-3xl mx-auto p-6 mt-6">
        
        <div className="text-center mb-8">
          <h2 className="text-3xl font-bold text-[#1e3a8a]">Create New Dataset</h2>
          <p className="text-gray-500 mt-2">Upload your reference documents to build a semantic knowledge baseline.</p>
        </div>

        {error && (
          <div className="mb-6 p-4 bg-red-50 border border-red-200 text-red-700 rounded text-sm font-medium flex items-start">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5 mr-2 mt-0.5 flex-shrink-0">
              <path fillRule="evenodd" d="M9.401 3.003c1.155-2 4.043-2 5.197 0l7.355 12.748c1.154 2-.29 4.5-2.599 4.5H4.645c-2.309 0-3.752-2.5-2.598-4.5L9.4 3.003ZM12 8.25a.75.75 0 0 1 .75.75v3.75a.75.75 0 0 1-1.5 0V9a.75.75 0 0 1 .75-.75Zm0 8.25a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3Z" clipRule="evenodd" />
            </svg>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-8">
          
          <div className="bg-white border border-gray-200 rounded p-6 shadow-sm">
            <h3 className="text-lg font-bold text-gray-800 mb-5 border-b border-gray-100 pb-2">General Information</h3>
            
            <div className="space-y-5">
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-1">
                  Dataset Name <span className="text-red-500">*</span>
                </label>
                <input 
                  type="text" 
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  className="w-full border border-gray-300 rounded p-2.5 text-sm focus:ring-2 focus:ring-[#1e3a8a] focus:border-[#1e3a8a] outline-none transition placeholder-gray-400"
                  placeholder="Enter dataset name (e.g., Biology Research Sources)"
                  disabled={isSubmitting}
                />
              </div>

              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-1">
                  Description
                </label>
                <textarea 
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  rows="4"
                  className="w-full border border-gray-300 rounded p-2.5 text-sm focus:ring-2 focus:ring-[#1e3a8a] focus:border-[#1e3a8a] outline-none transition placeholder-gray-400 resize-y"
                  placeholder="Enter a brief description for this dataset..."
                  disabled={isSubmitting}
                />
              </div>
            </div>
          </div>

          <div className="bg-white border border-gray-200 rounded p-6 shadow-sm">
            <div className="flex justify-between items-center mb-5 border-b border-gray-100 pb-2">
              <h3 className="text-lg font-bold text-gray-800">Source Files (PDF Only)</h3>
              
              <button 
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={isSubmitting}
                className="text-sm px-4 py-2 bg-blue-50 text-[#1e3a8a] font-semibold border border-blue-200 rounded hover:bg-blue-100 transition flex items-center space-x-2"
              >
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-4 h-4">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
                </svg>
                <span>Choose Files</span>
              </button>
              
              <input 
                type="file" 
                multiple 
                accept=".pdf,application/pdf" 
                ref={fileInputRef}
                onChange={handleFileChange}
                className="hidden" 
              />
            </div>

            {files.length === 0 ? (
              <div className="border-2 border-dashed border-gray-300 rounded-lg p-10 text-center bg-gray-50/50">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1} stroke="currentColor" className="w-14 h-14 mx-auto text-gray-400 mb-3">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m3.75 9v6m3-3H9m1.5-12H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z" />
                </svg>
                <p className="text-gray-500 text-sm font-medium">No files attached yet. Only PDF supported.</p>
                <p className="text-gray-400 text-xs mt-1">Click "Choose Files" above to upload.</p>
              </div>
            ) : (
              <ul className="space-y-3">
                {files.map((file, index) => (
                  <li key={index} className="flex justify-between items-center p-3 border border-gray-200 rounded bg-gray-50 shadow-sm hover:border-blue-300 transition">
                    <div className="flex items-center space-x-3 truncate">
                      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-6 h-6 text-red-500 flex-shrink-0">
                        <path fillRule="evenodd" d="M5.625 1.5c-1.036 0-1.875.84-1.875 1.875v17.25c0 1.035.84 1.875 1.875 1.875h12.75c1.035 0 1.875-.84 1.875-1.875V12.75A3.75 3.75 0 0 0 16.5 9h-1.875a1.875 1.875 0 0 1-1.875-1.875V5.25A3.75 3.75 0 0 0 9 1.5H5.625ZM7.5 15a.75.75 0 0 1 .75-.75h7.5a.75.75 0 0 1 0 1.5h-7.5A.75.75 0 0 1 7.5 15Zm.75 2.25a.75.75 0 0 0 0 1.5H12a.75.75 0 0 0 0-1.5H8.25Z" clipRule="evenodd" />
                        <path d="M12.971 1.816A5.23 5.23 0 0 1 14.25 5.25v1.875c0 .207.168.375.375.375H16.5a5.23 5.23 0 0 1 3.434 1.279 9.768 9.768 0 0 0-6.963-6.963Z" />
                      </svg>
                      <div className="truncate">
                        <p className="text-sm font-semibold text-gray-700 truncate">{file.name}</p>
                        <p className="text-xs text-gray-400">{(file.size / 1024 / 1024).toFixed(2)} MB</p>
                      </div>
                    </div>
                    
                    <button 
                      type="button" 
                      onClick={() => handleRemoveFile(index)}
                      disabled={isSubmitting}
                      className="p-1.5 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded transition"
                      title="Remove file"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor" className="w-5 h-5">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M6 18 18 6M6 6l12 12" />
                      </svg>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div className="flex justify-end space-x-4 pt-4">
            <button 
              type="button"
              onClick={() => navigate('/instructor/dataset')}
              disabled={isSubmitting}
              className="px-6 py-2.5 text-sm font-semibold text-gray-600 hover:text-gray-900 transition"
            >
              Cancel
            </button>
            <button 
              type="submit"
              disabled={isSubmitting}
              className="px-6 py-2.5 bg-[#1e3a8a] text-white rounded font-semibold hover:bg-[#152e75] transition shadow-sm disabled:opacity-70 disabled:cursor-not-allowed flex items-center space-x-2 text-sm"
            >
              {isSubmitting ? (
                <>
                  <div className="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full"></div>
                  <span>Saving...</span>
                </>
              ) : (
                <span>Save Dataset</span>
              )}
            </button>
          </div>
          
        </form>
      </main>
    </div>
  );
}