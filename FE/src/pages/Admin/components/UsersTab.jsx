import { useState, useEffect, useCallback } from 'react';
import { driver } from 'driver.js';
import { ErrorBlock } from './shared.jsx';
function UsersSection({ lang, api }) {
  const [users, setUsers] = useState({ content: [], page: 0, totalElements: 0, totalPages: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [pwMsg, setPwMsg] = useState({});
  const [loadingAction, setLoadingAction] = useState({});
  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState({ email: '', firstName: '', lastName: '', password: '', role: 'STUDENT' });
  const [createErr, setCreateErr] = useState('');

  const [q, setQ] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const fetch = useCallback(async (p, signal) => {
    setLoading(true); setError(null);
    try {
      const params = { page: p, size: 20 };
      if (q.trim()) params.q = q.trim();
      if (roleFilter) params.role = roleFilter;
      if (statusFilter) params.status = statusFilter;
      const r = await api.get('/api/admin/users', { params, signal });
      setUsers(r.data);
    } catch (e) {
      if (signal && signal.aborted) return;
      setError(e.message || lang.loadFailed);
    } finally {
      if (!signal || !signal.aborted) setLoading(false);
    }
  }, [api, q, roleFilter, statusFilter, lang.loadFailed]);

  useEffect(() => {
    const ac = new AbortController();
    fetch(page, ac.signal);
    return () => ac.abort();
  }, [fetch, page]);

  useEffect(() => {
    setPage(0);
  }, [q, roleFilter, statusFilter]);

  const startProcessGuide = () => {
    setTimeout(() => {
      const d = driver({
        animate: true, showProgress: true,
        steps: [
          { popover: { title: lang.processGuide, description: lang.guideUsersDesc, side: 'center' } },
          { element: '[data-guide="create-btn"]', popover: { title: lang.createUser, description: lang.guideUsersCreate, side: 'bottom' } },
          { element: '[data-guide="table"]', popover: { title: lang.userAccounts, description: lang.guideUsersTable, side: 'left' } },
          { element: '[data-guide="action-ban"]', popover: { title: lang.actions, description: lang.guideUsersActions, side: 'left' } },
          { popover: { title: lang.done, description: lang.guideUsersDone, side: 'center' } },
        ],
      }).drive();
    }, 300);
  };

  const doToggleRole = async (u) => {
    if (u.role === 'ADMIN') return;
    const newRole = u.role === 'STUDENT' ? 'INSTRUCTOR' : 'STUDENT';
    setLoadingAction(p => ({ ...p, ['role_' + u.id]: true }));
    try {
      await api.patch(`/api/admin/users/${u.id}/role`, { role: newRole });
      setUsers(prev => ({
        ...prev,
        content: prev.content.map(x => x.id === u.id ? { ...x, role: newRole } : x)
      }));
    } catch (e) {
      setError(e.response?.data?.message || e.message);
    } finally {
      setLoadingAction(p => ({ ...p, ['role_' + u.id]: false }));
    }
  };

  const toggleStatus = async (u) => {
    const ns = u.accountStatus === 'ACTIVE' ? 'BANNED' : 'ACTIVE';
    setLoadingAction(p => ({ ...p, [u.id]: true }));
    try {
      await api.patch(`/api/admin/users/${u.id}/status`, { status: ns });
      setUsers(prev => ({ ...prev, content: prev.content.map(x => x.id === u.id ? { ...x, accountStatus: ns } : x) }));
    }
    catch (e) { setError(e.message); }
    finally { setLoadingAction(p => ({ ...p, [u.id]: false })); }
  };

  const doResetPw = async (u) => {
    setLoadingAction(p => ({ ...p, ['pw_' + u.id]: true }));
    try {
      await api.post(`/api/admin/users/${u.id}/password-reset`);
      setPwMsg(p => ({ ...p, [u.id]: { ok: true, msg: lang.resetSent } }));
    }
    catch (e) { setPwMsg(p => ({ ...p, [u.id]: { ok: false, msg: lang.resetFailed } })); }
    finally {
      setLoadingAction(p => ({ ...p, ['pw_' + u.id]: false }));
      setTimeout(() => setPwMsg(p => { const n = { ...p }; delete n[u.id]; return n; }), 3000);
    }
  };

  const doDelete = async (id) => {
    if (!confirm(lang.confirmDelete)) return;
    setLoadingAction(p => ({ ...p, ['del_' + id]: true }));
    try {
      await api.delete(`/api/admin/users/${id}`);
      setUsers(prev => ({ ...prev, content: prev.content.filter(x => x.id !== id) }));
    }
    catch (e) { setError(e.message); }
    finally { setLoadingAction(p => ({ ...p, ['del_' + id]: false })); }
  };

  const doCreate = async (e) => {
    e.preventDefault(); setCreateErr('');
    try {
      await api.post('/api/admin/users', createForm);
      setShowCreate(false);
      setCreateForm({ email: '', firstName: '', lastName: '', password: '', role: 'STUDENT' });
      fetch(0);
    }
    catch (err) { setCreateErr(err.response?.data?.message || err.message); }
  };

  return (
    <div className="p-8 space-y-6 bg-[#f8fafc]">
      {/* Title area */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-gray-200 pb-5">
        <div>
          <h1 className="text-3xl font-extrabold text-[#1e3a8a] tracking-tight">{lang.userAccounts}</h1>
          <p className="text-gray-500 text-xs mt-1">{lang.usersSub}</p>
        </div>
        <div className="flex items-center gap-2.5">
          <button onClick={startProcessGuide} className="flex items-center gap-1.5 px-4 py-2 text-xs font-bold text-gray-600 bg-white border border-gray-200 rounded-xl hover:bg-gray-50 shadow-sm transition">
            <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>{lang.processGuide}</span>
          </button>
          <button data-guide="create-btn" onClick={() => setShowCreate(true)} 
            className="px-4 py-2 text-xs font-bold text-white bg-[#0c162e] hover:bg-[#152447] rounded-xl transition shadow-sm">
            {lang.createUser}
          </button>
        </div>
      </div>

      {/* Search & Filters container */}
      <div className="bg-white rounded-xl border border-gray-200 p-4 shadow-sm flex flex-col sm:flex-row gap-3 items-center">
        {/* Search Input */}
        <div className="w-full sm:flex-1 relative">
          <svg className="w-4 h-4 text-gray-400 absolute left-3 top-2.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <input 
            type="text" 
            placeholder="Search by email or name..." 
            value={q}
            onChange={(e) => { setQ(e.target.value); setPage(0); }}
            className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-blue-500 font-semibold" 
          />
        </div>

        {/* Dropdown 1: Role */}
        <select 
          value={roleFilter} 
          onChange={(e) => { setRoleFilter(e.target.value); setPage(0); }}
          className="w-full sm:w-36 px-3 py-2 bg-white border border-gray-200 rounded-xl text-xs font-semibold text-slate-700 focus:outline-none cursor-pointer"
        >
          <option value="">All Roles</option>
          <option value="STUDENT">Student</option>
          <option value="INSTRUCTOR">Instructor</option>
          <option value="ADMIN">Admin</option>
        </select>

        {/* Dropdown 2: Status */}
        <select 
          value={statusFilter} 
          onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
          className="w-full sm:w-36 px-3 py-2 bg-white border border-gray-200 rounded-xl text-xs font-semibold text-slate-700 focus:outline-none cursor-pointer"
        >
          <option value="">All Statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="BANNED">Banned</option>
        </select>

        {/* Adjustments Filter Button */}
        <button className="p-2 bg-white border border-gray-200 rounded-xl hover:bg-slate-50 transition shadow-sm shrink-0">
          <svg className="w-4 h-4 text-slate-500" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
          </svg>
        </button>
      </div>

      {/* User creation modal */}
      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-xs" onClick={() => setShowCreate(false)}>
          <div className="bg-white rounded-2xl shadow-xl p-6 w-full max-w-md mx-4 transform transition-all" onClick={e => e.stopPropagation()}>
            <div className="flex justify-between items-center mb-4">
              <h3 className="font-bold text-lg text-slate-800">{lang.createUser}</h3>
              <button onClick={() => setShowCreate(false)} className="text-gray-400 hover:text-gray-600 transition">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <form onSubmit={doCreate} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">Email Address</label>
                <input name="email" placeholder="email@example.com" value={createForm.email} onChange={e => setCreateForm(p => ({ ...p, email: e.target.value }))} required className="w-full border border-gray-300 rounded-xl px-3.5 py-2 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none" />
              </div>
              <div className="flex gap-3">
                <div className="flex-1">
                  <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">First Name</label>
                  <input name="firstName" placeholder="First Name" value={createForm.firstName} onChange={e => setCreateForm(p => ({ ...p, firstName: e.target.value }))} required className="w-full border border-gray-300 rounded-xl px-3.5 py-2 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none" />
                </div>
                <div className="flex-1">
                  <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">Last Name</label>
                  <input name="lastName" placeholder="Last Name" value={createForm.lastName} onChange={e => setCreateForm(p => ({ ...p, lastName: e.target.value }))} required className="w-full border border-gray-300 rounded-xl px-3.5 py-2 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none" />
                </div>
              </div>
              <div>
                <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">Password</label>
                <input name="password" type="password" placeholder="••••••••" value={createForm.password} onChange={e => setCreateForm(p => ({ ...p, password: e.target.value }))} required className="w-full border border-gray-300 rounded-xl px-3.5 py-2 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none" />
              </div>
              <div>
                <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">User Role</label>
                <select value={createForm.role} onChange={e => setCreateForm(p => ({ ...p, role: e.target.value }))} className="w-full border border-gray-300 rounded-xl px-3.5 py-2 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none cursor-pointer">
                  <option value="STUDENT">Student</option>
                  <option value="INSTRUCTOR">Instructor</option>
                </select>
              </div>
              {createErr && <div className="text-xs text-rose-600 bg-rose-50 p-2.5 rounded-lg border border-rose-100 font-semibold">{createErr}</div>}
              <div className="flex gap-2.5 justify-end pt-2">
                <button type="button" onClick={() => setShowCreate(false)} className="px-4 py-2 text-xs font-bold text-gray-600 border border-gray-200 rounded-xl hover:bg-gray-50 transition">Cancel</button>
                <button type="submit" className="px-4 py-2 text-xs font-bold bg-[#0c162e] text-white rounded-xl hover:bg-[#152447] transition">{lang.createUser}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {error && <ErrorBlock msg={error} onRetry={() => fetch(page, new AbortController().signal)} />}

      {/* Table Card */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table data-guide="table" className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-50 text-slate-400 font-bold uppercase border-b border-gray-100">
                <th className="px-6 py-3.5 font-bold tracking-wider">Email</th>
                <th className="px-6 py-3.5 font-bold tracking-wider">Full Name</th>
                <th className="px-6 py-3.5 font-bold tracking-wider">Role</th>
                <th className="px-6 py-3.5 font-bold tracking-wider">Status</th>
                <th className="px-6 py-3.5 font-bold tracking-wider text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-slate-700 font-semibold">
              {loading ? Array.from({ length: 5 }).map((_, i) => (
                <tr key={i} className="animate-pulse">{Array.from({ length: 5 }).map((_, j) => (
                  <td key={j} className="px-6 py-5"><div className="h-4 bg-gray-200 rounded w-full" /></td>
                ))}</tr>
              )) : users.content.length === 0 ? (
                <tr><td colSpan={5} className="px-6 py-12 text-center text-gray-400 font-medium">No users found</td></tr>
              ) : users.content.map(u => (
                <tr key={u.id} className="hover:bg-slate-50/50 transition">
                  <td className="px-6 py-4 font-mono text-gray-600 font-medium">{u.email}</td>
                  <td className="px-6 py-4 font-bold text-slate-800">{u.firstName} {u.lastName}</td>
                  <td className="px-6 py-4">
                    <button
                      onClick={() => doToggleRole(u)}
                      disabled={u.role === 'ADMIN' || loadingAction['role_' + u.id]}
                      title={u.role === 'ADMIN' ? 'Admin role cannot be changed' : `Click to change role to ${u.role === 'STUDENT' ? 'INSTRUCTOR' : 'STUDENT'}`}
                      className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold flex items-center gap-1 transition ${
                        u.role === 'ADMIN' ? 'bg-rose-100 text-rose-700 cursor-not-allowed' :
                        u.role === 'INSTRUCTOR' ? 'bg-amber-100 text-amber-700 hover:bg-amber-200 cursor-pointer' :
                        'bg-blue-100 text-blue-700 hover:bg-blue-200 cursor-pointer'
                      }`}
                    >
                      <span>{loadingAction['role_' + u.id] ? '...' : u.role}</span>
                      {u.role !== 'ADMIN' && (
                        <svg className="w-2.5 h-2.5 opacity-60" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
                        </svg>
                      )}
                    </button>
                  </td>
                  <td className="px-6 py-4">
                    <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold ${u.accountStatus === 'ACTIVE' ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'}`}>{u.accountStatus}</span>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center justify-end gap-4">
                      {/* Reset Password Icon */}
                      {pwMsg[u.id] ? (
                        <span className={`inline-block px-2 py-1 text-[10px] font-bold rounded ${pwMsg[u.id].ok ? 'text-emerald-700 bg-emerald-50' : 'text-rose-700 bg-rose-50'}`}>{pwMsg[u.id].msg}</span>
                      ) : (
                        <button onClick={() => doResetPw(u)} disabled={loadingAction['pw_' + u.id]} title="Reset Password"
                          className="p-1.5 rounded-lg hover:bg-slate-100 transition disabled:opacity-50 text-[#1e3a8a] shrink-0">
                          {loadingAction['pw_' + u.id] ? (
                            <span className="text-[10px]">...</span>
                          ) : (
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" viewBox="0 0 24 24">
                              <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" />
                              <path d="M3 3v5h5" />
                              <rect x="9" y="12" width="6" height="5" rx="1" />
                              <path d="M10 12V10a2 2 0 1 1 4 0v2" />
                            </svg>
                          )}
                        </button>
                      )}

                      {/* Ban / Activate Icon */}
                      <button onClick={() => toggleStatus(u)} disabled={loadingAction[u.id]} title={u.accountStatus === 'ACTIVE' ? 'Ban User' : 'Activate User'}
                        className={`p-1.5 rounded-lg hover:bg-slate-100 transition disabled:opacity-50 shrink-0 ${u.accountStatus === 'ACTIVE' ? 'text-amber-600' : 'text-emerald-600'}`}>
                        {loadingAction[u.id] ? (
                          <span className="text-[10px]">...</span>
                        ) : u.accountStatus === 'ACTIVE' ? (
                          <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" viewBox="0 0 24 24">
                            <circle cx="12" cy="12" r="10" />
                            <path d="M4.9 19.1L19.1 4.9" />
                          </svg>
                        ) : (
                          <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" viewBox="0 0 24 24">
                            <circle cx="12" cy="12" r="10" />
                            <path d="M9 12l2 2 4-4" />
                          </svg>
                        )}
                      </button>

                      {/* Delete Icon */}
                      <button onClick={() => doDelete(u.id)} disabled={loadingAction['del_' + u.id]} title="Delete User"
                        className="p-1.5 rounded-lg hover:bg-slate-100 transition disabled:opacity-50 text-rose-600 shrink-0">
                        {loadingAction['del_' + u.id] ? (
                          <span className="text-[10px]">...</span>
                        ) : (
                          <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" viewBox="0 0 24 24">
                            <path d="M3 6h18" />
                            <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6" />
                            <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2" />
                            <line x1="10" x2="10" y1="11" y2="17" />
                            <line x1="14" x2="14" y1="11" y2="17" />
                          </svg>
                        )}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        
        {/* Footer / Pagination */}
        <div className="flex items-center justify-between px-6 py-3.5 border-t border-gray-100 bg-gray-50/50 text-xs font-semibold text-gray-500">
          <span>Showing {users.content.length} of {users.totalElements || users.content.length} users</span>
          {users.totalPages > 1 && (
            <div className="flex items-center gap-1.5">
              <button onClick={() => setPage(page - 1)} disabled={page === 0}
                className="p-1.5 rounded-lg border border-gray-200 text-gray-400 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed transition">
                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
                </svg>
              </button>
              {Array.from({ length: users.totalPages }).map((_, i) => {
                if (i === 0 || i === users.totalPages - 1 || (i >= page - 1 && i <= page + 1)) {
                  const isActive = page === i;
                  return (
                    <button key={i} onClick={() => setPage(i)}
                      className={`w-7 h-7 flex items-center justify-center rounded-lg text-xs font-bold transition ${isActive ? 'bg-[#1e3a8a] text-white shadow-sm' : 'border border-gray-200 text-gray-600 hover:bg-slate-50'}`}>
                      {i + 1}
                    </button>
                  );
                } else if (i === 1 || i === users.totalPages - 2) {
                  return <span key={i} className="text-gray-400 text-xs px-0.5">...</span>;
                }
                return null;
              })}
              <button onClick={() => setPage(page + 1)} disabled={page >= users.totalPages - 1}
                className="p-1.5 rounded-lg border border-gray-200 text-gray-400 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed transition">
                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
                </svg>
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}


export { UsersSection };
