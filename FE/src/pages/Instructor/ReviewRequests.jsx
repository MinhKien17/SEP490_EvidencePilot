import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { StatusBadge, LoadingSkeleton, EmptyState, TourLauncher, AppHeader } from '../../components';
import { instructorText, commonText } from '../../locales';
import { useLanguage } from '../../context/LanguageContext';
import api from '../../api.js';

const TOUR_STEPS = [
  { element: '#review-table', popover: { title: 'Review Queue', description: 'View all student review requests. Approve, return for revision, or reject.', side: 'top', align: 'start' } },
];

export default function ReviewRequests() {
  const { language } = useLanguage();
  const t = instructorText[language];
  const ct = commonText[language];

  const [requests, setRequests] = useState([]);
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const fetchReviewRequests = async () => {
    setLoading(true); setErrorMessage('');
    try {
      const [res, proj] = await Promise.all([
        api.get('/api/feedback-requests'),
        api.get('/api/projects?page=0&size=100').catch(() => null),
      ]);
      setRequests(res.data);
      setProjects(proj?.data?.content || []);
    }
    catch { setErrorMessage('Failed to load review requests.'); }
    finally { setLoading(false); }
  };

  useEffect(() => { fetchReviewRequests(); }, []);

  return (
    <div className="min-h-screen bg-[#f8fafc] text-[#0f172a]">
      <AppHeader />
      <div className="max-w-7xl mx-auto p-8">
        <div className="mb-8 border-b border-gray-200 pb-6">
          <div className="mb-2">
            <Link to="/instructor/dashboard" className="text-xs font-bold text-gray-400 hover:text-[#1e3a8a] transition-colors">&larr; {ct.back}</Link>
          </div>
          <h1 className="text-3xl font-black text-[#1e3a8a] tracking-tight">{t.reviewRequests}</h1>
          <p className="text-xs text-gray-400 mt-1">{t.pendingRequests}</p>
        </div>

        {errorMessage && (
          <div className="p-4 mb-6 rounded-xl bg-rose-50 border border-rose-100 text-rose-700 text-xs font-bold">{errorMessage}</div>
        )}

        <div id="review-table" className="bg-white rounded-3xl border border-gray-200 shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 text-gray-400 text-[10px] font-bold uppercase border-b border-gray-100">
                  <th className="px-6 py-4">{t.project}</th>
                  <th className="px-6 py-4">{ct.status}</th>
                  <th className="px-6 py-4">{ct.actions}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-xs text-gray-700">
                {loading ? (
                  <tr><td colSpan="3" className="px-6 py-8"><LoadingSkeleton count={3} height="h-8" /></td></tr>
                ) : requests.length === 0 ? (
                  <tr><td colSpan="3" className="px-6 py-8"><EmptyState title={t.noRequests} /></td></tr>
                ) : requests.map((req) => {
                  const projectTitle = projects.find(p => String(p.id) === String(req.projectId))?.title;
                  return (
                  <tr key={req.id} className="hover:bg-gray-50/40 transition">
                    <td className="px-6 py-4">
                      <Link to={`/instructor/requests/${req.projectId}`}
                        className="font-bold text-gray-900 block text-xs hover:text-[#1e3a8a] transition-colors">
                        {projectTitle || `${t.project} #${req.projectId.slice(0, 8)}`}
                      </Link>
                      <span className="text-[10px] text-gray-400 font-mono block mt-0.5">Request: {req.id}</span>
                    </td>
                    <td className="px-6 py-4"><StatusBadge status={req.status} /></td>
                    <td className="px-6 py-4">
                      <Link to={`/instructor/requests/${req.projectId}`}
                        className="text-xs font-black text-[#1e3a8a] hover:underline">{t.review}</Link>
                    </td>
                  </tr>
                );
                })}
              </tbody>
            </table>
          </div>
        </div>
      </div>
      <TourLauncher steps={TOUR_STEPS} tourKey="instructor-requests" />
    </div>
  );
}
