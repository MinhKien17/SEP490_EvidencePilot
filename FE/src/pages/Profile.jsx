import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api.js';
import { AppHeader, LoadingSkeleton } from '../components';
import { useAuth } from '../context/AuthContext.jsx';
import { useLanguage } from '../context/LanguageContext.jsx';
import { commonText } from '../locales';

export default function Profile() {
  const navigate = useNavigate();
  const { user: authUser, verifySession } = useAuth();
  const { language } = useLanguage();
  const t = commonText[language];
  const [user, setUser] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [message, setMessage] = useState({ type: '', text: '' });

  useEffect(() => {
    if (!authUser) return;
    setUser(authUser);
    setFirstName(authUser.firstName || '');
    setLastName(authUser.lastName || '');
  }, [authUser]);

  const handleUpdateProfile = async (event) => {
    event.preventDefault();
    if (!firstName.trim() || !lastName.trim()) {
      setMessage({ type: 'error', text: t.nameRequired });
      return;
    }

    setSubmitting(true);
    setMessage({ type: '', text: '' });
    try {
      const { data } = await api.put('/api/users/profile', {
        firstName: firstName.trim(),
        lastName: lastName.trim(),
      });
      setUser(data);
      setMessage({ type: 'success', text: t.profileUpdated });
      verifySession().catch(() => {});
    } catch (error) {
      setMessage({ type: 'error', text: error.response?.data?.message || t.profileUpdateFailed });
    } finally {
      setSubmitting(false);
    }
  };

  if (!user) {
    return (
      <div className="min-h-screen bg-[var(--page-bg)]">
        <AppHeader />
        <div className="mx-auto max-w-4xl p-4 sm:p-6 lg:p-8"><LoadingSkeleton count={4} /></div>
      </div>
    );
  }

  const roleLabel = {
    ADMIN: t.roleAdmin,
    INSTRUCTOR: t.roleInstructor,
    STUDENT: t.roleStudent,
  }[user.role] || user.role;
  const initials = `${user.firstName?.[0] || ''}${user.lastName?.[0] || ''}` || 'U';

  return (
    <div className="min-h-screen overflow-x-hidden bg-[var(--page-bg)] text-[var(--text-primary)]">
      <AppHeader />
      <main className="mx-auto max-w-4xl p-4 sm:p-6 lg:p-8">
        <header className="mb-6 border-b border-[var(--border)] pb-5">
          <h1 className="text-2xl font-black text-[var(--brand-foreground)] sm:text-3xl">
            {t.profileTitle.replace('{{role}}', roleLabel)}
          </h1>
          <p className="mt-1 text-sm text-[var(--text-secondary)]">{t.profileDescription}</p>
        </header>

        {message.text && (
          <div role={message.type === 'error' ? 'alert' : 'status'} className={`mb-6 flex items-center gap-2 rounded-xl border p-3 text-xs font-bold ${message.type === 'success' ? 'border-emerald-200 bg-emerald-50 text-emerald-800' : 'border-rose-200 bg-rose-50 text-rose-800'}`}>
            {message.type === 'success'
              ? <svg aria-hidden="true" viewBox="0 0 24 24" className="h-4 w-4 shrink-0 fill-none stroke-current" strokeWidth="2"><path d="m5 12 4 4L19 6" /></svg>
              : <svg aria-hidden="true" viewBox="0 0 24 24" className="h-4 w-4 shrink-0 fill-none stroke-current" strokeWidth="2"><path d="M12 3 2 21h20L12 3Z" /><path d="M12 9v5M12 18h.01" /></svg>}
            {message.text}
          </div>
        )}

        <div className="grid items-start gap-6 md:grid-cols-3">
          <aside className="rounded-2xl border border-[var(--border)] bg-[var(--surface)] p-5 text-center shadow-sm">
            <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-2xl bg-[var(--brand)] text-2xl font-black text-white shadow-sm">{initials.toUpperCase()}</div>
            <h2 className="mt-4 break-words text-base font-black">{user.firstName} {user.lastName}</h2>
            <p className="mt-1 text-xs font-bold text-[var(--brand-foreground)]">{roleLabel}</p>
            <div className="mt-5 border-t border-[var(--border-light)] pt-4 text-left">
              <p className="text-[10px] font-bold uppercase tracking-wider text-[var(--text-tertiary)]">{t.userId}</p>
              <p className="mt-1 break-all font-mono text-[10px] text-[var(--text-secondary)]">{user.id}</p>
            </div>
          </aside>

          <section className="rounded-2xl border border-[var(--border)] bg-[var(--surface)] p-5 shadow-sm sm:p-6 md:col-span-2">
            <h2 className="mb-5 text-sm font-bold text-[var(--brand-foreground)]">{t.accountDetails}</h2>
            <form onSubmit={handleUpdateProfile} className="space-y-5">
              <div className="grid gap-4 sm:grid-cols-2">
                <label className="space-y-1.5 text-xs font-bold text-[var(--text-secondary)]">
                  <span>{t.firstName}</span>
                  <input type="text" value={firstName} onChange={(event) => setFirstName(event.target.value)} required maxLength={100} className="w-full rounded-xl border border-[var(--border)] bg-[var(--surface-secondary)] px-4 py-3 text-sm font-medium text-[var(--text-primary)] outline-none" />
                </label>
                <label className="space-y-1.5 text-xs font-bold text-[var(--text-secondary)]">
                  <span>{t.lastName}</span>
                  <input type="text" value={lastName} onChange={(event) => setLastName(event.target.value)} required maxLength={100} className="w-full rounded-xl border border-[var(--border)] bg-[var(--surface-secondary)] px-4 py-3 text-sm font-medium text-[var(--text-primary)] outline-none" />
                </label>
              </div>

              <label className="space-y-1.5 text-xs font-bold text-[var(--text-secondary)]">
                <span>{t.email}</span>
                <input type="email" value={user.email || ''} readOnly className="w-full cursor-not-allowed rounded-xl border border-[var(--border)] bg-[var(--surface-tertiary)] px-4 py-3 text-sm text-[var(--text-secondary)]" />
              </label>

              <div className="rounded-xl bg-[var(--surface-secondary)] p-3">
                <p className="text-[10px] font-bold uppercase tracking-wider text-[var(--text-tertiary)]">{t.assignedRole}</p>
                <p className="mt-1 text-xs font-bold text-[var(--text-secondary)]">{roleLabel}</p>
              </div>

              <div className="flex flex-wrap justify-between gap-3 border-t border-[var(--border-light)] pt-5">
                <button type="button" onClick={() => navigate(-1)} className="rounded-xl border border-[var(--border)] bg-[var(--surface)] px-5 py-2.5 text-xs font-bold text-[var(--text-secondary)] hover:bg-[var(--surface-secondary)]">&larr; {t.back}</button>
                <button type="submit" disabled={submitting} className="rounded-xl bg-[var(--brand)] px-5 py-2.5 text-xs font-black text-white shadow-sm hover:bg-[var(--brand-hover)] disabled:opacity-50">
                  {submitting ? t.updatingProfile : t.updateProfile}
                </button>
              </div>
            </form>
          </section>
        </div>
      </main>
    </div>
  );
}
