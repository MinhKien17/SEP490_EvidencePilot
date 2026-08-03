import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { StatusBadge, LoadingSkeleton, EmptyState, Modal, TourLauncher, AppHeader } from '../../components';
import { instructorText, commonText } from '../../locales';
import { useLanguage } from '../../context/LanguageContext';
import api from '../../api.js';

export default function ReviewRequests() {
  const { language } = useLanguage();
  const t = instructorText[language];
  const ct = commonText[language];

  const [requests, setRequests] = useState([]);
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [feedbackContent, setFeedbackContent] = useState('');
  const [submittingFeedback, setSubmittingFeedback] = useState(false);
  const tourSteps = [
    { element: '#review-table', popover: { title: t.reviewQueue, description: t.reviewQueueDesc, side: 'top', align: 'start' } },
    { element: '#feedback-modal-btn', popover: { title: t.writeFeedback, description: t.writeFeedbackDesc, side: 'left', align: 'center' } },
  ];

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
    catch { setErrorMessage(t.loadReviewRequestsFailed); }
    finally { setLoading(false); }
  };

  const handleTransitionStatus = async (requestId, targetStatus) => {
    setErrorMessage('');
    try {
      const res = await api.patch(`/api/feedback-requests/${requestId}/status?status=${targetStatus}`);
      setRequests(prev => prev.map(req => req.id === requestId ? { ...req, status: res.data.status } : req));
      if (selectedRequest?.id === requestId) setSelectedRequest(prev => ({ ...prev, status: res.data.status }));
    } catch { setErrorMessage(t.updateStatusFailed); }
  };

  const handleSubmitComment = async (e) => {
    e.preventDefault();
    if (!feedbackContent.trim() || !selectedRequest) return;
    setSubmittingFeedback(true); setErrorMessage('');
    try {
      await api.post(`/api/feedback-requests/${selectedRequest.id}/feedback`, { content: feedbackContent.trim() });
      setFeedbackContent(''); setSelectedRequest(null);
    } catch { setErrorMessage(t.submitFeedbackFailed); }
    finally { setSubmittingFeedback(false); }
  };

  useEffect(() => { fetchReviewRequests(); }, []);

  return (
    <div className="min-h-screen bg-(--page-bg) text-(--text-primary)">
      <AppHeader />
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-8 border-b border-(--border) pb-6">
          <div className="mb-2">
            <Link to="/instructor/dashboard" className="text-xs font-bold text-(--text-tertiary) hover:text-(--brand-foreground) transition-colors">&larr; {ct.back}</Link>
          </div>
          <h1 className="text-3xl font-black text-(--brand-foreground) tracking-tight">{t.reviewRequests}</h1>
          <p className="text-xs text-(--text-tertiary) mt-1">{t.pendingRequests}</p>
        </div>

        {errorMessage && (
          <div className="p-4 mb-6 rounded-xl bg-rose-50 border border-rose-100 text-rose-700 text-xs font-bold">{errorMessage}</div>
        )}

        <div id="review-table" className="bg-(--surface) rounded-2xl border border-(--border) shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-(--surface-secondary) text-(--text-tertiary) text-[10px] font-bold uppercase border-b border-(--border-light)">
                  <th className="px-6 py-4">{t.project}</th>
                  <th className="px-6 py-4">{ct.status}</th>
                  <th className="px-6 py-4">{ct.actions}</th>
                  <th className="px-6 py-4 text-right">{t.feedback}</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-(--border-light) text-xs text-(--text-secondary)">
                {loading ? (
                  <tr><td colSpan="4" className="px-6 py-8"><LoadingSkeleton count={3} height="h-8" /></td></tr>
                ) : requests.length === 0 ? (
                  <tr><td colSpan="4" className="px-6 py-8"><EmptyState title={t.noRequests} /></td></tr>
                ) : requests.map((req) => {
                  const projectTitle = projects.find(p => String(p.id) === String(req.projectId))?.title;
                  return (
                  <tr key={req.id} className="hover:bg-(--surface-secondary) transition-colors">
                    <td className="px-6 py-4">
                      <Link to={`/instructor/requests/${req.projectId}`}
                        className="font-bold text-(--text-primary) block text-xs hover:text-(--brand-foreground) transition-colors">
                        {projectTitle || `${t.project} #${req.projectId.slice(0, 8)}`}
                      </Link>
                      <span className="text-[10px] text-(--text-tertiary) font-mono block mt-0.5">{t.request}: {req.id}</span>
                    </td>
                    <td className="px-6 py-4"><StatusBadge status={req.status} /></td>
                    <td className="px-6 py-4">
                      <Link to={`/instructor/requests/${req.projectId}`}
                        className="text-xs font-black text-(--brand) hover:underline">{t.review}</Link>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <button id="feedback-modal-btn" onClick={() => setSelectedRequest(req)}
                        className="text-xs font-black text-(--brand) hover:underline">{t.provideFeedback}</button>
                    </td>
                  </tr>
                );
                })}
              </tbody>
            </table>
          </div>
        </div>

        <Modal open={!!selectedRequest} onClose={() => setSelectedRequest(null)} title={`${t.feedback} — ID: ${selectedRequest?.id}`} closeLabel={ct.close}>
          <form onSubmit={handleSubmitComment} className="space-y-4">
            <p className="text-xs text-(--text-secondary)">{ct.status}: <StatusBadge status={selectedRequest?.status} /></p>
            <div>
              <label className="text-(--text-secondary) font-bold block mb-1 text-xs">{t.feedback} *</label>
              <textarea rows="4" required value={feedbackContent} onChange={(e) => setFeedbackContent(e.target.value)}
                placeholder={t.feedbackPlaceholder}
                className="w-full px-3 py-2 bg-(--surface-secondary) border border-(--border) rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-(--focus) text-(--text-primary)" />
            </div>
            <div className="flex gap-2 text-xs font-bold pt-2">
              <button type="button" onClick={() => setSelectedRequest(null)}
                className="flex-1 py-2.5 bg-(--surface-secondary) text-(--text-secondary) rounded-xl hover:bg-(--surface-tertiary) transition-colors">{ct.cancel}</button>
              <button type="submit" disabled={submittingFeedback}
                className="flex-1 py-2.5 bg-(--brand) text-(--on-brand) rounded-xl hover:bg-(--brand-hover) transition-colors shadow-sm disabled:opacity-50">
                {submittingFeedback ? ct.saving : ct.submit}
              </button>
            </div>
          </form>
        </Modal>
      </main>
      <TourLauncher steps={tourSteps} tourKey="instructor-requests" />
    </div>
  );
}
