import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../api.js';

export default function CollectionList() {
  const navigate = useNavigate();
  const [collections, setCollections] = useState([]);
  const [projects, setProjects] = useState([]); 
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [selectedProjectId, setSelectedProjectId] = useState("");

  // Tải danh sách project và các collection ban đầu
  const fetchInitialData = async () => {
    setLoading(true);
    setErrorMessage("");
    try {
      const projectRes = await api.get('/api/projects?size=100&active=true');
      const projectList = projectRes.data.content || [];
      setProjects(projectList);

      if (projectList.length > 0) {
        const firstId = projectList[0].id;
        setSelectedProjectId(firstId);
        const collectionRes = await api.get(`/api/projects/${firstId}/collections`);
        setCollections(Array.isArray(collectionRes.data) ? collectionRes.data : collectionRes.data.content || []);
      }
    } catch (error) {
      console.error("Error loading instructor context:", error);
      setErrorMessage("Failed to synchronize collections repository with current active projects.");
    } finally {
      setLoading(false);
    }
  };

  // Thay đổi bộ lọc hiển thị theo từng dự án
  const handleProjectFilterChange = async (pId) => {
    setSelectedProjectId(pId);
    if (!pId) return;
    setLoading(true);
    try {
      const response = await api.get(`/api/projects/${pId}/collections`);
      setCollections(Array.isArray(response.data) ? response.data : response.data.content || []);
    } catch (error) {
      setErrorMessage("Error filtering collections backend index.");
    } finally {
      setLoading(false);
    }
  };

  // Soft-delete bộ sưu tập mẫu
  const handleDeleteCollection = async (id) => {
    if (!window.confirm("Are you sure you want to archive this evidence library specification?")) return;
    try {
      await api.delete(`/api/collections/${id}`);
      setCollections(prev => prev.filter(item => item.id !== id));
    } catch (error) {
      setErrorMessage("Could not delete target collection asset.");
    }
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
          {/* Chuyển trang sang file CreateCollection riêng biệt */}
          <button
            onClick={() => navigate('/instructor/collections/create')}
            className="px-5 py-2.5 bg-[#1e3a8a] text-white font-black text-xs rounded-xl hover:bg-blue-800 transition shadow-sm"
          >
            + Build Collection Master
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
            className="px-3 py-1.5 bg-gray-50 border border-gray-200 text-xs rounded-lg text-gray-800 font-medium focus:outline-none"
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
                  <th className="px-6 py-4 text-right">Operational Guard</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-xs text-gray-700">
                {loading ? (
                  <tr><td colSpan="4" className="px-6 py-8 text-center text-gray-400 font-medium">Synchronizing metadata data stream...</td></tr>
                ) : collections.length === 0 ? (
                  <tr><td colSpan="4" className="px-6 py-8 text-center text-gray-400 font-medium">No active collection matrix mapped to this project module layout.</td></tr>
                ) : (
                  collections.map((col) => (
                    <tr key={col.id} className="hover:bg-gray-50/40 transition">
                      <td className="px-6 py-4 font-mono text-gray-400 text-[11px]">{col.id}</td>
                      <td className="px-6 py-4 font-bold text-gray-900">{col.title}</td>
                      <td className="px-6 py-4 text-gray-500 max-w-xs truncate">{col.description || "N/A"}</td>
                      <td className="px-6 py-4 text-right">
                        <button 
                          onClick={() => handleDeleteCollection(col.id)}
                          className="text-xs font-bold text-rose-600 hover:underline"
                        >
                          Soft Archive
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

      </div>
    </div>
  );
}