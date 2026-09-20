import { useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import PreviewPane from '../features/PreviewPane';
import DeleteConfirm from '../ui/DeleteConfirm.jsx';
import { StandardConfigEditor } from './sections/StandardConfigModal.jsx';
import { studentDisplayName } from '../../utils/instructor/studentSearch.js';

function moveSection(sections, from, to) {
  if (from === to || from == null || to == null) return sections;
  const next = [...sections];
  const [moved] = next.splice(from, 1);
  next.splice(to, 0, moved);
  return next.map((section, index) => ({ ...section, sectionOrder: index }));
}

export default function EditPaperSectionModal({
  open,
  paper,
  sections,
  serverSections,
  sectionEvals = {},
  projectMembers = [],
  users = [],
  projectReadOnly,
  sectionStructureLocked,
  sectionStructureSaving,
  conflictSectionId,
  onClose,
  onDraftChange,
  onSave,
  onDiscard,
  onAddSection,
  onDeleteSection,
  onStartRename,
  onSaveRename,
  onReloadConflict,
  onSaveStandard,
  onUnassignAll,
  t: labels,
  ct,
}) {
  const { t } = useTranslation();
  const dialogRef = useRef(null);
  const dragIndexRef = useRef(null);
  const [selectedSectionId, setSelectedSectionId] = useState(null);
  const [selectedBulkIds, setSelectedBulkIds] = useState([]);
  const [bulkStudentId, setBulkStudentId] = useState('');
  const [bulkOpen, setBulkOpen] = useState(false);
  const [mode, setMode] = useState('edit');
  const [confirmClose, setConfirmClose] = useState(false);
  const [standardSectionId, setStandardSectionId] = useState(null);
  const [renameSectionId, setRenameSectionId] = useState(null);
  const [renameTitle, setRenameTitle] = useState('');

  const dirty = JSON.stringify(sections) !== JSON.stringify(serverSections);
  const selectedSection = useMemo(
    () => sections.find(section => String(section.id) === String(selectedSectionId)) || sections[0] || null,
    [sections, selectedSectionId],
  );
  const standardSection = sections.find(section => String(section.id) === String(standardSectionId)) || null;
  const studentMembers = useMemo(
    () => projectMembers.filter(member => member.userRole === 'STUDENT'),
    [projectMembers],
  );
  const assignableMembers = useMemo(
    () => studentMembers.filter(member => users.length === 0 || users.some(user => String(user.id) === String(member.userId))),
    [studentMembers, users],
  );
  const selectedStudentId = selectedSection?.assignedUserId || '';

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return undefined;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
    return () => {
      if (dialog.open) dialog.close();
    };
  }, [open]);

  useEffect(() => {
    if (!sections.some(section => String(section.id) === String(selectedSectionId))) {
      setSelectedSectionId(sections[0]?.id || null);
    }
    setSelectedBulkIds(current => current.filter(sectionId => sections.some(section => String(section.id) === String(sectionId))));
  }, [sections, selectedSectionId]);

  useEffect(() => {
    if (!open) {
      setConfirmClose(false);
      setSelectedBulkIds([]);
      setBulkStudentId('');
      setMode('edit');
      setStandardSectionId(null);
      setRenameSectionId(null);
    }
  }, [open]);

  const requestClose = () => {
    if (dirty) {
      setConfirmClose(true);
      return;
    }
    onClose();
  };

  const updateSection = (sectionId, changes) => {
    onDraftChange(sections.map(section => String(section.id) === String(sectionId)
      ? { ...section, ...changes }
      : section));
  };

  const toggleBulkSection = (sectionId) => {
    const id = String(sectionId);
    setSelectedBulkIds(current => current.includes(id)
      ? current.filter(selectedId => selectedId !== id)
      : [...current, id]);
  };

  const applyBulkAssignment = () => {
    if (!bulkStudentId || selectedBulkIds.length === 0) return;
    onDraftChange(sections.map(section => selectedBulkIds.includes(String(section.id)) && section.sectionType !== 'REFERENCE'
      ? { ...section, assignedUserId: bulkStudentId }
      : section));
  };

  const startRename = (section) => {
    setRenameSectionId(section.id);
    setRenameTitle(section.sectionTitle || '');
    onStartRename?.(section);
  };

  const saveRename = (sectionId) => {
    const title = renameTitle.trim();
    if (!title) return;
    onSaveRename?.(sectionId, title);
    updateSection(sectionId, { sectionTitle: title });
    setRenameSectionId(null);
  };

  const handleDrop = (toIndex) => {
    const fromIndex = dragIndexRef.current;
    dragIndexRef.current = null;
    if (sectionStructureLocked || fromIndex == null || fromIndex === toIndex) return;
    onDraftChange(moveSection(sections, fromIndex, toIndex));
  };

  const handleDiscard = () => {
    setConfirmClose(false);
    onDiscard();
  };

  const handleStandardClose = () => setStandardSectionId(null);

  const handleSave = async () => {
    const saved = await onSave();
    if (saved !== false) onClose();
  };

  return (
    <dialog
      ref={dialogRef}
      onCancel={event => { event.preventDefault(); requestClose(); }}
      onClick={event => { if (event.target === dialogRef.current) requestClose(); }}
      aria-label={labels.editPaperSections}
      className="fixed inset-0 m-auto h-[92vh] w-[94vw] max-h-none max-w-none rounded-2xl border-0 bg-transparent p-0 shadow-2xl backdrop:bg-slate-900/60 backdrop:backdrop-blur-sm"
    >
      <div className="flex h-full w-full overflow-hidden rounded-2xl border border-(--border) bg-(--surface)">
        <aside className="hidden w-72 shrink-0 flex-col border-r border-(--border) bg-(--surface-secondary) md:flex" aria-label={labels.pages}>
          <div className="flex items-center justify-between border-b border-(--border) px-4 py-3">
            <h2 className="text-xs font-bold uppercase tracking-wider text-(--text-primary)">{labels.pages} ({sections.length})</h2>
            <button type="button" onClick={requestClose} aria-label={labels.closePages} className="rounded px-2 py-1 text-xs font-bold text-(--text-secondary) hover:bg-(--surface-tertiary)">{t('close')}</button>
          </div>
          <div className="flex-1 space-y-1 overflow-y-auto p-2">
            {sections.map((section, index) => {
              const selected = String(section.id) === String(selectedSection?.id);
              const locked = sectionStructureLocked || sectionStructureSaving;
              const standardConfigured = Boolean(sectionEvals[String(section.id)]?.requirements?.length);
              return (
                <div
                  key={section.id}
                  draggable={!locked}
                  onDragStart={() => { dragIndexRef.current = index; }}
                  onDragOver={event => event.preventDefault()}
                  onDrop={() => handleDrop(index)}
                  className={`rounded-lg border p-2 ${selected ? 'border-indigo-400 bg-(--surface)' : 'border-transparent hover:border-(--border) hover:bg-(--surface-tertiary)'}`}
                  data-testid={String(section.id) === String(conflictSectionId) ? `section-conflict-${section.id}` : undefined}
                >
                  <div className="flex items-start gap-2">
                    <input
                      type="checkbox"
                      aria-label={`${labels.selectSection} ${section.sectionTitle}`}
                      checked={selectedBulkIds.includes(String(section.id))}
                      onChange={() => toggleBulkSection(section.id)}
                      disabled={section.sectionType === 'REFERENCE' || projectReadOnly}
                      className="mt-1 h-3.5 w-3.5 shrink-0"
                    />
                    <button type="button" data-testid={`section-nav-${section.id}`} onClick={() => setSelectedSectionId(section.id)} className="min-w-0 flex-1 text-left">
                      <span className="flex items-center gap-2 text-xs font-semibold text-(--text-primary)">
                        <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded bg-indigo-100 text-[9px] font-bold text-indigo-700">{index + 1}</span>
                        <span className="truncate">{section.sectionTitle || t('untitled')}</span>
                      </span>
                      <span className="mt-1 flex flex-wrap gap-1 pl-7 text-[9px] text-(--text-tertiary)">
                        <span>{section.assignedUserId ? studentDisplayName(projectMembers.find(member => String(member.userId) === String(section.assignedUserId)) || {}) : labels.unassigned}</span>
                        <span>·</span>
                        <span>{standardConfigured ? labels.standardConfigured : labels.standardNotConfigured}</span>
                      </span>
                    </button>
                  </div>
                  <div className="mt-2 flex items-center justify-end gap-1 pl-7">
                    {!locked && (
                      <button type="button" onClick={() => startRename(section)} className="rounded px-1.5 py-1 text-[10px] font-bold text-(--text-tertiary) hover:bg-(--brand-soft) hover:text-(--brand-foreground)">{labels.rename}</button>
                    )}
                    {!locked && (
                      <DeleteConfirm
                        message={labels.deleteSectionConfirm}
                        onConfirm={() => onDeleteSection(section.id)}
                        triggerLabel={labels.deleteSection}
                        confirmLabel={ct.delete}
                        cancelLabel={ct.cancel}
                        disabled={sectionStructureSaving}
                        className="rounded px-1.5 py-1 text-[10px] font-bold text-rose-600 hover:bg-rose-50"
                      >
                        {labels.deleteSection}
                      </DeleteConfirm>
                    )}
                    {String(section.id) === String(conflictSectionId) && (
                      <button type="button" onClick={() => onReloadConflict(section.id)} className="rounded px-1.5 py-1 text-[10px] font-bold text-amber-700 hover:bg-amber-50">{labels.reloadSection}</button>
                    )}
                  </div>
                  {renameSectionId != null && String(renameSectionId) === String(section.id) && (
                    <div className="mt-2 flex gap-1 pl-7">
                      <input autoFocus aria-label={labels.sectionTitle} value={renameTitle} onChange={event => setRenameTitle(event.target.value)} onKeyDown={event => { if (event.key === 'Enter') saveRename(section.id); if (event.key === 'Escape') setRenameSectionId(null); }} className="min-w-0 flex-1 rounded border border-(--border) bg-(--surface) px-2 py-1 text-[10px]" />
                      <button type="button" onClick={() => saveRename(section.id)} className="rounded bg-emerald-600 px-2 py-1 text-[10px] font-bold text-white">{ct.save}</button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
          <div className="border-t border-(--border) p-3">
            <button type="button" onClick={onAddSection} aria-label={labels.addSection} disabled={sectionStructureLocked || sectionStructureSaving || projectReadOnly} className="w-full rounded-lg bg-(--brand) px-3 py-2 text-xs font-bold text-white disabled:cursor-not-allowed disabled:opacity-50">+ {labels.addSection}</button>
            <button type="button" onClick={requestClose} className="mt-2 w-full rounded-lg px-3 py-2 text-xs font-semibold text-(--text-secondary) hover:bg-(--surface-tertiary)">{t('close')}</button>
          </div>
        </aside>

        <section className="flex min-w-0 flex-1 flex-col bg-white" aria-label={labels.paperEditor}>
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-4 py-3 sm:px-6">
            <div className="min-w-0">
              <p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">{paper?.originalFilename || paper?.title || t('paper')}</p>
              <h1 className="truncate text-lg font-bold text-slate-900">{labels.editPaperSections}</h1>
            </div>
            <div className="flex items-center gap-2">
              <div className="flex rounded-lg border border-slate-200 bg-slate-50 p-1">
                <button type="button" onClick={() => setMode('edit')} className={`rounded px-2 py-1 text-[10px] font-bold ${mode === 'edit' ? 'bg-white text-indigo-700 shadow-sm' : 'text-slate-500'}`}>{labels.editMode}</button>
                <button type="button" onClick={() => setMode('preview')} className={`rounded px-2 py-1 text-[10px] font-bold ${mode === 'preview' ? 'bg-white text-indigo-700 shadow-sm' : 'text-slate-500'}`}>{labels.previewMode}</button>
              </div>
              <button type="button" onClick={requestClose} aria-label={labels.closeEditor} className="rounded-lg border border-slate-200 px-2 py-1.5 text-xs font-bold text-slate-600 hover:bg-slate-50">×</button>
            </div>
          </div>

          {mode === 'preview' ? (
            <div className="flex-1 overflow-y-auto p-4 sm:p-8">
              {sections.map(section => (
                <div key={section.id} className="mb-8">
                  <PreviewPane sectionTitle={section.sectionTitle} latex={section.contentTex || ''} mediaAssets={[]} citationNumbers={{}} />
                </div>
              ))}
            </div>
          ) : (
            <div className="flex-1 overflow-y-auto p-4 sm:p-8">
              {!selectedSection ? (
                <p className="py-16 text-center text-sm italic text-slate-400">{labels.noSectionsHelp}</p>
              ) : (
                <div className="mx-auto max-w-4xl space-y-5">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div className="min-w-0 flex-1">
                      <label htmlFor="edit-paper-section-title" className="text-[10px] font-black uppercase tracking-wider text-slate-400">{labels.sectionTitle}</label>
                      <input id="edit-paper-section-title" aria-label={labels.sectionTitle} value={selectedSection.sectionTitle || ''} onChange={event => updateSection(selectedSection.id, { sectionTitle: event.target.value })} readOnly={sectionStructureLocked || projectReadOnly} className="mt-1 w-full border-b-2 border-slate-200 px-0 py-2 text-2xl font-bold text-slate-900 outline-none focus:border-indigo-500 read-only:opacity-60" />
                    </div>
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="rounded-lg bg-slate-100 px-2 py-1 text-[10px] font-bold text-slate-600">{selectedSection.assignedUserId ? studentDisplayName(projectMembers.find(member => String(member.userId) === String(selectedSection.assignedUserId)) || {}) : labels.unassigned}</span>
                      <button type="button" onClick={() => setStandardSectionId(selectedSection.id)} disabled={sectionStructureLocked || projectReadOnly} className="rounded-lg border border-indigo-200 px-2.5 py-1.5 text-[10px] font-bold text-indigo-700 disabled:cursor-not-allowed disabled:opacity-50">{labels.configStandard}</button>
                    </div>
                  </div>

                  {selectedSection.sectionType === 'REFERENCE' ? (
                    <div className="rounded-xl border border-indigo-200 bg-indigo-50 px-3 py-2 text-xs font-semibold text-indigo-800">{labels.referenceSharedEditors}</div>
                  ) : (
                    <label className="block text-xs font-semibold text-slate-600">
                      {labels.assignedStudent}
                      <select aria-label={labels.assignedStudent} value={selectedStudentId} onChange={event => updateSection(selectedSection.id, { assignedUserId: event.target.value || null })} disabled={projectReadOnly || sectionStructureSaving} className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs outline-none focus:border-indigo-500 disabled:bg-slate-100">
                        <option value="">{labels.unassigned}</option>
                        {assignableMembers.map(member => <option key={member.userId} value={member.userId}>{studentDisplayName(member)}</option>)}
                      </select>
                    </label>
                  )}

                  <div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div>
                        <p className="text-xs font-bold text-slate-700">{labels.bulkAssign}</p>
                        <p className="text-[10px] text-slate-500">{labels.bulkAssignHint}</p>
                      </div>
                      <button type="button" onClick={() => setBulkOpen(value => !value)} aria-expanded={bulkOpen} className="rounded-lg border border-slate-200 bg-white px-2.5 py-1.5 text-[10px] font-bold text-slate-600">{labels.bulkAssign}</button>
                    </div>
                    {bulkOpen && (
                      <div className="mt-3 space-y-2 border-t border-slate-200 pt-3">
                        <div className="grid gap-1 sm:grid-cols-2">
                          {sections.map(section => (
                            <label key={section.id} className="flex items-center gap-2 rounded bg-white px-2 py-1.5 text-[10px] text-slate-600">
                              <input type="checkbox" data-testid={`bulk-section-${section.id}`} aria-label={`${labels.selectSection} ${section.sectionTitle}`} checked={selectedBulkIds.includes(String(section.id))} onChange={() => toggleBulkSection(section.id)} disabled={section.sectionType === 'REFERENCE' || projectReadOnly} />
                              <span className="truncate">{section.sectionTitle}</span>
                            </label>
                          ))}
                        </div>
                        <div className="flex flex-wrap items-center gap-2">
                          <select aria-label={labels.bulkAssignmentStudent} value={bulkStudentId} onChange={event => setBulkStudentId(event.target.value)} disabled={projectReadOnly} className="min-w-48 flex-1 rounded-lg border border-slate-200 bg-white px-2 py-1.5 text-xs">
                            <option value="">{labels.selectStudent}</option>
                            {assignableMembers.map(member => <option key={member.userId} value={member.userId}>{studentDisplayName(member)}</option>)}
                          </select>
                          <button type="button" onClick={applyBulkAssignment} disabled={!bulkStudentId || selectedBulkIds.length === 0 || projectReadOnly} className="rounded-lg bg-indigo-600 px-3 py-1.5 text-[10px] font-bold text-white disabled:opacity-50">{labels.applyAssignment}</button>
                        </div>
                      </div>
                    )}
                  </div>

                  {studentMembers.length > 0 && (
                    <div className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-amber-200 bg-amber-50 p-3">
                      <div>
                        <p className="text-xs font-bold text-amber-900">{labels.unassignAll}</p>
                        <p className="text-[10px] text-amber-800">{labels.unassignAllHint}</p>
                      </div>
                      <div className="flex items-center gap-2">
                        <select aria-label={labels.studentFilter} defaultValue={studentMembers[0]?.userId || ''} id="edit-paper-unassign-student" className="rounded-lg border border-amber-200 bg-white px-2 py-1.5 text-xs">
                          {studentMembers.map(member => <option key={member.userId} value={member.userId}>{studentDisplayName(member)}</option>)}
                        </select>
                        <DeleteConfirm
                          message={labels.unassignAllConfirm}
                          onConfirm={() => onUnassignAll(document.getElementById('edit-paper-unassign-student')?.value)}
                          triggerLabel={labels.unassignAll}
                          confirmLabel={labels.unassignAll}
                          cancelLabel={ct.cancel}
                          disabled={projectReadOnly || sectionStructureSaving}
                          className="rounded-lg bg-amber-100 px-2.5 py-1.5 text-[10px] font-bold text-amber-800 hover:bg-amber-200"
                        >
                          {labels.unassignAll}
                        </DeleteConfirm>
                      </div>
                    </div>
                  )}

                  <label htmlFor="edit-paper-section-content" className="block text-[10px] font-black uppercase tracking-wider text-slate-400">{labels.sectionContent}</label>
                  <textarea id="edit-paper-section-content" aria-label={labels.sectionContent} value={selectedSection.contentTex || ''} onChange={event => updateSection(selectedSection.id, { contentTex: event.target.value })} readOnly={projectReadOnly} rows={20} className="w-full resize-y rounded-xl border border-slate-200 bg-white p-4 font-mono text-xs leading-6 text-slate-800 outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 read-only:bg-slate-100 read-only:opacity-70" />

                  {standardSection && (
                    <div className="rounded-xl border border-indigo-100 bg-indigo-50/40 p-4">
                      <div className="mb-3 flex items-center justify-between gap-2">
                        <h3 className="text-xs font-bold text-indigo-900">{labels.configStandard} — {standardSection.sectionTitle}</h3>
                        <button type="button" onClick={handleStandardClose} className="rounded px-2 py-1 text-[10px] font-bold text-indigo-700 hover:bg-white">{t('close')}</button>
                      </div>
                      <StandardConfigEditor
                        open={Boolean(standardSection)}
                        initialRequirements={sectionEvals[String(standardSection.id)]?.requirements || []}
                        isLocked={sectionStructureLocked || projectReadOnly}
                        onSave={async config => {
                          const saved = await onSaveStandard(standardSection.id, config);
                          if (saved) setStandardSectionId(null);
                        }}
                        onClose={handleStandardClose}
                        t={labels}
                        ct={ct}
                      />
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          <div className="flex flex-wrap items-center justify-between gap-2 border-t border-slate-200 bg-white px-4 py-3 sm:px-6">
            <p className="text-[10px] italic text-slate-500">{dirty ? labels.sectionsUnsaved : labels.noUnsavedChanges}</p>
            <div className="flex items-center gap-2">
              <button type="button" onClick={handleDiscard} disabled={!dirty || sectionStructureSaving} className="rounded-lg bg-slate-100 px-3 py-2 text-xs font-semibold text-slate-600 disabled:cursor-not-allowed disabled:opacity-50">{labels.discardChanges}</button>
              <button type="button" onClick={handleSave} disabled={!dirty || sectionStructureSaving || projectReadOnly} className="rounded-lg bg-indigo-600 px-3 py-2 text-xs font-bold text-white hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50">{labels.saveChanges}</button>
            </div>
          </div>
        </section>

        {confirmClose && (
          <div role="alertdialog" aria-label={labels.discardUnsavedChanges} className="fixed inset-0 z-[60] flex items-center justify-center bg-slate-900/30 p-4">
            <div className="w-full max-w-sm rounded-xl border border-(--border) bg-(--surface) p-4 shadow-2xl">
              <p className="text-sm font-bold text-(--text-primary)">{labels.discardUnsavedChanges}</p>
              <p className="mt-1 text-xs text-(--text-secondary)">{labels.discardUnsavedChangesHint}</p>
              <div className="mt-4 flex justify-end gap-2">
                <button type="button" onClick={() => setConfirmClose(false)} className="rounded-lg bg-(--surface-tertiary) px-3 py-2 text-xs font-semibold text-(--text-secondary)">{labels.keepEditing}</button>
                <button type="button" onClick={handleDiscard} className="rounded-lg bg-rose-600 px-3 py-2 text-xs font-bold text-white">{labels.discardChanges}</button>
              </div>
            </div>
          </div>
        )}
      </div>
    </dialog>
  );
}
