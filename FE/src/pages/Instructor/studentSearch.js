function normalizeSearch(value) {
  return String(value || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/đ/g, 'd')
    .trim();
}

export function studentDisplayName(student) {
  return [student?.firstName, student?.lastName].filter(Boolean).join(' ')
    || student?.email
    || '';
}

export function getStudentSuggestions(users, projectMembers, query, limit = 8) {
  const memberIds = new Set(projectMembers.map(member => String(member.userId)));
  const terms = normalizeSearch(query).split(/\s+/).filter(Boolean);

  return users
    .filter(student => student.role === 'STUDENT' && !memberIds.has(String(student.id)))
    .filter(student => {
      const searchable = normalizeSearch(`${studentDisplayName(student)} ${student.studentCode || ''}`);
      return terms.every(term => searchable.includes(term));
    })
    .slice(0, limit);
}
