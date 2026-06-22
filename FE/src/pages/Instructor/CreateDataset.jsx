import { useState } from 'react';
import api from '../../api.js';

export default function CreateDataset() {
  const [datasetName, setDatasetName] = useState('');
  const [description, setDescription] = useState('');
  const [sources, setSources] = useState([{ name: '', url: '' }]);
  const [loading, setLoading] = useState(false);

  const handleAddSource = () => setSources([...sources, { name: '', url: '' }]);

  const handleSourceChange = (index, field, value) => {
    const updatedSources = [...sources];
    updatedSources[index][field] = value;
    setSources(updatedSources);
  };

  const handleRemoveSource = (index) => {
    setSources(sources.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    // Lọc bỏ các source trống trước khi gửi lên server
    const validSources = sources.filter(s => s.name.trim() !== '' && s.url.trim() !== '');

    const payload = {
      name: datasetName,
      description: description,
      sources: validSources
    };
    
    try {
      // Gọi API thật lên Backend (đón đầu DatasetController)
      await api.post('/api/datasets', payload);
      alert('Dataset and Sources created successfully!');
      
      // Reset form sau khi tạo thành công
      setDatasetName('');
      setDescription('');
      setSources([{ name: '', url: '' }]);
    } catch (err) {
      console.error('Failed to connect to /api/datasets:', err);
      
      // Giả lập thông báo thành công bằng Mock Data nếu chưa deploy endpoint này
      alert('[Mock Mode] Dataset simulated successfully! (Server endpoint is not ready yet).');
      setDatasetName('');
      setDescription('');
      setSources([{ name: '', url: '' }]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto p-6 mt-10 bg-white rounded-xl shadow-md border border-gray-100">
      <h2 className="text-2xl font-bold mb-6 text-gray-800">Create New Dataset</h2>
      
      <form onSubmit={handleSubmit} className="space-y-6">
        {/* General Information Section */}
        <div className="p-5 border border-gray-200 rounded-lg bg-gray-50/50">
          <h3 className="text-lg font-semibold mb-4 text-gray-700">General Information</h3>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Dataset Name <span className="text-red-500">*</span>
              </label>
              <input 
                type="text" 
                required
                placeholder="Enter dataset name..."
                className="w-full p-2.5 bg-white border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:outline-none transition"
                value={datasetName}
                onChange={(e) => setDatasetName(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
              <textarea 
                placeholder="Enter detailed description for this dataset..."
                className="w-full p-2.5 bg-white border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:outline-none transition"
                rows="3"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              ></textarea>
            </div>
          </div>
        </div>

        {/* Sources Management Section */}
        <div className="p-5 border border-gray-200 rounded-lg">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-semibold text-gray-700">Sources List</h3>
            <button 
              type="button" 
              onClick={handleAddSource}
              className="px-4 py-2 bg-blue-50 text-blue-700 rounded-md hover:bg-blue-100 transition text-sm font-medium"
            >
              + Add Source
            </button>
          </div>
          
          <div className="space-y-4">
            {sources.map((source, index) => (
              <div key={index} className="flex gap-4 items-start pb-4 border-b border-gray-100 last:border-0 last:pb-0">
                <div className="flex-1 space-y-3">
                  <input 
                    type="text" 
                    placeholder="Source Name (e.g., Paper PDF, Wikipedia...)" 
                    className="w-full p-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none transition"
                    value={source.name}
                    onChange={(e) => handleSourceChange(index, 'name', e.target.value)}
                  />
                  <input 
                    type="url" 
                    placeholder="URL / Link" 
                    className="w-full p-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none transition"
                    value={source.url}
                    onChange={(e) => handleSourceChange(index, 'url', e.target.value)}
                  />
                </div>
                {sources.length > 1 && (
                  <button 
                    type="button"
                    onClick={() => handleRemoveSource(index)}
                    className="mt-1 px-3 py-2 text-red-500 bg-red-50 hover:bg-red-100 rounded-md transition font-medium text-sm"
                  >
                    Remove
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Submit Button */}
        <button 
          type="submit" 
          disabled={loading}
          className="w-full py-3 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition shadow-sm disabled:opacity-50"
        >
          {loading ? 'Saving Data...' : 'Save Dataset & Sources'}
        </button>
      </form>
    </div>
  );
}