import React, { useState, useRef, useEffect } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import FileViewerModal from '../../components/FileViewerModal';
import api from '../../api.js';
import { useLanguage } from '../../context/LanguageContext';
import { UI_TEXT } from '../../constants/uiText';

const DEFAULT_SAMPLE_LATEX = `\\documentclass{article}
\\usepackage[utf-8]{inputenc}
\\usepackage{xcolor}
\\usepackage{soul}

\\title{Evidence Traceability in Modern Agile Environments}
\\author{Minh Nguyen}
\\date{\\today}

\\begin{document}

\\maketitle

\\section{Introduction}

Agile software development depends on fast communication between stakeholders, product owners, and delivery teams. However, project risk increases when feedback loops are informal or delayed.

\\section{Communication Protocols and Risk Reduction}

Clear communication protocols reduce project risk \\hl{because teams can identify blockers earlier and align decisions before sprint goals are missed}.

Risk management in agile projects should combine lightweight documentation, frequent review meetings, and traceable evidence for important project claims.

\\section{Addressing Common Assumptions}

Some teams assume daily meetings alone are enough to control project uncertainty, but this claim still needs stronger evidence from the uploaded sources.`;

const RichTextEditor = React.memo(({ initialHtml, onHtmlChange }) => {
  return (
    <div
      className="flex-1 bg-white text-slate-800 p-8 overflow-y-auto leading-relaxed custom-scrollbar selection:bg-indigo-100 outline-none"
      contentEditable={true}
      suppressContentEditableWarning={true}
      onInput={(e) => onHtmlChange(e.target)}
      dangerouslySetInnerHTML={{ __html: initialHtml }}
    />
  );
}, () => true);

export default function Workspace() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const { logout, user } = useAuth();
  const { language, toggleLanguage } = useLanguage();
  const [activeTab, setActiveTab] = useState('Source');
  const [editorMode, setEditorMode] = useState('Code');
  const [showHistoryModal, setShowHistoryModal] = useState(false);
  const [showReviseModal, setShowReviseModal] = useState(false);
  const [toastMessage, setToastMessage] = useState('');

  // Backend state
  const [project, setProject] = useState(null);
  const [projects, setProjects] = useState([]);
  const [sources, setSources] = useState([]);
  const [papers, setPapers] = useState([]);
  const [selectedPaper, setSelectedPaper] = useState(null);
  const [claims, setClaims] = useState([]);
  const [feedbacks, setFeedbacks] = useState([]);
  const [graphData, setGraphData] = useState(null);
  const [loadingProject, setLoadingProject] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [viewerFile, setViewerFile] = useState(null);
  const [currentUser, setCurrentUser] = useState(null);

  const [codeContent, setCodeContent] = useState('');

  const displayContent = selectedPaper ? codeContent : DEFAULT_SAMPLE_LATEX;

  // 1. Tải thông tin người dùng hiện tại
  useEffect(() => {
    api.get('/api/users/me')
      .then(res => setCurrentUser(res.data))
      .catch(err => console.error('Failed to fetch user profile', err));
  }, []);

  // 2. Hàm tải lại dữ liệu chi tiết của dự án
  const loadProjectData = async (projId) => {
    if (!projId) return;
    try {
      // Tải chi tiết dự án
      const projRes = await api.get(`/api/projects/${projId}`);
      setProject(projRes.data);

      // Tải nguồn tài liệu (Sources)
      try {
        const srcRes = await api.get(`/api/projects/${projId}/sources`);
        setSources(srcRes.data || []);
      } catch (e) { console.error('Failed to fetch sources', e); }

      // Tải các bản nháp (Papers)
      try {
        const paperRes = await api.get(`/api/papers/by-project/${projId}`);
        const paperList = paperRes.data || [];
        setPapers(paperList);

        // Nếu có paper, chọn paper đầu tiên làm mặc định hiển thị
        if (paperList.length > 0) {
          const defaultPaper = paperList[0];
          setSelectedPaper(defaultPaper);
          setCodeContent(defaultPaper.extractedText || '');
        } else {
          setSelectedPaper(null);
          setCodeContent('');
        }
      } catch (e) { console.error('Failed to fetch papers', e); }

      // Tải luận điểm (Claims)
      try {
        const claimRes = await api.get(`/api/claims/by-project/${projId}`);
        setClaims(claimRes.data || []);
      } catch (e) { console.error('Failed to fetch claims', e); }

      // Tải phản hồi (Feedbacks) - gọi đúng URL /api/feedback-requests
      try {
        const fbRes = await api.get('/api/feedback-requests');
        const allFbs = fbRes.data || [];
        const projectFbs = allFbs.filter(fb => fb.projectId === parseInt(projId));
        setFeedbacks(projectFbs);
      } catch (e) { console.error('Failed to fetch feedback', e); }

      // Tải dữ liệu đồ thị (Graph) - gọi đúng URL /api/projects/{id}/traceability-export
      try {
        const graphRes = await api.get(`/api/projects/${projId}/traceability-export`);
        setGraphData(graphRes.data);
      } catch (e) { console.error('Failed to fetch graph data', e); }

    } catch (err) {
      console.error('Error loading project details:', err);
    }
  };

  // 3. Tải danh sách các dự án và dữ liệu dự án hiện tại
  useEffect(() => {
    async function loadInitialData() {
      try {
        setLoadingProject(true);
        let currentProjectId = projectId;

        // Lấy danh sách dự án
        const listRes = await api.get('/api/projects');
        const activeProjects = listRes.data || [];
        setProjects(activeProjects);

        // Nếu không có projectId trên URL, chọn cái đầu tiên
        if (!currentProjectId && activeProjects.length > 0) {
          currentProjectId = activeProjects[0].id;
          navigate(`/student/projects/${currentProjectId}`, { replace: true });
          return;
        }

        if (currentProjectId) {
          await loadProjectData(currentProjectId);
        }
      } catch (err) {
        console.error('Error loading projects list:', err);
      } finally {
        setLoadingProject(false);
      }
    }

    loadInitialData();
  }, [projectId]);

  // 4. Các hàm xử lý CRUD API

  // Tạo dự án mới
  const handleCreateProject = async () => {
    const title = prompt("Nhập tiêu đề dự án:");
    if (!title) return;
    const description = prompt("Nhập mô tả dự án (tùy chọn):") || "";
    try {
      const res = await api.post('/api/projects', { title, description });
      showToast("Tạo dự án thành công!");

      // Tải lại danh sách và chuyển hướng
      const listRes = await api.get('/api/projects');
      const activeProjects = listRes.data || [];
      setProjects(activeProjects);
      navigate(`/student/projects/${res.data.id}`);
    } catch (err) {
      console.error(err);
      showToast("Tạo dự án thất bại");
    }
  };

  // Xóa dự án hiện tại
  const handleDeleteProject = async () => {
    if (!project) return;
    if (!window.confirm(`Bạn có chắc chắn muốn xóa dự án "${project.title}" không?`)) return;
    try {
      await api.delete(`/api/projects/${project.id}`);
      showToast("Xóa dự án thành công!");

      const listRes = await api.get('/api/projects');
      const activeProjects = listRes.data || [];
      setProjects(activeProjects);

      if (activeProjects.length > 0) {
        navigate(`/student/projects/${activeProjects[0].id}`);
      } else {
        setProject(null);
        setSources([]);
        setPapers([]);
        setClaims([]);
        setFeedbacks([]);
        setGraphData(null);
        setCodeContent('');
        setSelectedPaper(null);
        navigate('/student/projects');
      }
    } catch (err) {
      console.error(err);
      showToast("Xóa dự án thất bại");
    }
  };

  // Tải lên Tài liệu tham khảo (Source)
  const handleUploadSource = async (file) => {
    if (!file || !project) return;
    if (!currentUser) {
      showToast("Không tìm thấy thông tin tài khoản người dùng.");
      return;
    }
    showToast(`Đang tải lên ${file.name}...`);

    const formData = new FormData();
    formData.append('file', file);
    formData.append('projectId', project.id);
    formData.append('uploadedBy', currentUser.id); // Bắt buộc theo API của BE

    try {
      await api.post('/api/sources/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      showToast(`${file.name} đã được tải lên thành công.`);
      // Tải lại nguồn và graph
      const srcRes = await api.get(`/api/projects/${project.id}/sources`);
      setSources(srcRes.data || []);
      const graphRes = await api.get(`/api/projects/${project.id}/traceability-export`);
      setGraphData(graphRes.data);
    } catch (err) {
      console.error('Upload source failed', err);
      showToast(`Tải lên ${file.name} thất bại.`);
    }
  };

  // Xóa tài liệu tham khảo (Source)
  const handleDeleteSource = async (sourceId) => {
    if (!window.confirm("Bạn có chắc chắn muốn xóa tài liệu này?")) return;
    try {
      await api.delete(`/api/sources/${sourceId}`);
      showToast("Xóa tài liệu thành công!");
      // Tải lại nguồn và graph
      const srcRes = await api.get(`/api/projects/${project.id}/sources`);
      setSources(srcRes.data || []);
      const graphRes = await api.get(`/api/projects/${project.id}/traceability-export`);
      setGraphData(graphRes.data);
    } catch (err) {
      console.error(err);
      showToast("Xóa tài liệu thất bại.");
    }
  };

  // Tải lên bản nháp bài báo (Paper)
  const handleUploadPaper = async (file) => {
    if (!file || !project) return;
    showToast(`Đang tải lên bản nháp ${file.name}...`);

    const formData = new FormData();
    formData.append('file', file);
    formData.append('projectId', project.id);

    try {
      const res = await api.post('/api/papers/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      showToast("Tải lên bản nháp bài báo thành công.");

      // Tải lại danh sách Paper
      const paperRes = await api.get(`/api/papers/by-project/${project.id}`);
      const paperList = paperRes.data || [];
      setPapers(paperList);

      // Chọn paper mới tải lên làm hiện tại
      const uploadedPaper = res.data;
      setSelectedPaper(uploadedPaper);
      setCodeContent(uploadedPaper.extractedText || '');
    } catch (err) {
      console.error('Upload paper failed', err);
      showToast("Tải lên bản nháp thất bại.");
    }
  };

  // Xóa bản nháp bài báo (Paper)
  const handleDeletePaper = async (paperId) => {
    if (!window.confirm("Bạn có chắc chắn muốn xóa bản nháp bài viết này?")) return;
    try {
      await api.delete(`/api/papers/${paperId}`);
      showToast("Xóa bản nháp thành công!");

      // Tải lại danh sách Paper
      const paperRes = await api.get(`/api/papers/by-project/${project.id}`);
      const paperList = paperRes.data || [];
      setPapers(paperList);

      if (selectedPaper && selectedPaper.id === paperId) {
        if (paperList.length > 0) {
          setSelectedPaper(paperList[0]);
          setCodeContent(paperList[0].extractedText || '');
        } else {
          setSelectedPaper(null);
          setCodeContent('');
        }
      }
    } catch (err) {
      console.error(err);
      showToast("Xóa bản nháp thất bại.");
    }
  };

  // Tạo luận điểm mới (Claim)
  const handleCreateClaim = async () => {
    if (!newClaimContent.trim() || !project) return;
    try {
      await api.post('/api/claims', {
        content: newClaimContent,
        project: { id: project.id }
      });
      showToast("Đã thêm luận điểm mới.");
      setNewClaimContent('');

      // Tải lại danh sách claims và graph
      const claimRes = await api.get(`/api/claims/by-project/${project.id}`);
      setClaims(claimRes.data || []);
      const graphRes = await api.get(`/api/projects/${project.id}/traceability-export`);
      setGraphData(graphRes.data);
    } catch (err) {
      console.error(err);
      showToast("Thêm luận điểm thất bại.");
    }
  };

  // Cập nhật luận điểm (Claim)
  const handleUpdateClaim = async () => {
    if (!editingClaim || !editClaimContent.trim()) return;
    try {
      await api.put(`/api/claims/${editingClaim.id}`, {
        id: editingClaim.id,
        content: editClaimContent,
        active: true,
        aiConfidenceScore: editingClaim.aiConfidenceScore
      });
      showToast("Cập nhật luận điểm thành công.");
      setEditingClaim(null);
      setEditClaimContent('');

      // Tải lại danh sách claims và graph
      const claimRes = await api.get(`/api/claims/by-project/${project.id}`);
      setClaims(claimRes.data || []);
      const graphRes = await api.get(`/api/projects/${project.id}/traceability-export`);
      setGraphData(graphRes.data);
    } catch (err) {
      console.error(err);
      showToast("Cập nhật luận điểm thất bại.");
    }
  };

  // Xóa luận điểm (Claim)
  const handleDeleteClaim = async (claimId) => {
    if (!window.confirm("Bạn có chắc chắn muốn xóa luận điểm này?")) return;
    try {
      await api.delete(`/api/claims/${claimId}`);
      showToast("Xóa luận điểm thành công!");

      // Tải lại danh sách claims và graph
      const claimRes = await api.get(`/api/claims/by-project/${project.id}`);
      setClaims(claimRes.data || []);
      const graphRes = await api.get(`/api/projects/${project.id}/traceability-export`);
      setGraphData(graphRes.data);

      if (selectedClaim && selectedClaim.id === claimId) {
        setSelectedClaim(null);
        setClaimMatches([]);
      }
    } catch (err) {
      console.error(err);
      showToast("Xóa luận điểm thất bại.");
    }
  };

  // AI phân tích luận điểm tự động (Analyze Claim)
  const handleAnalyzeClaim = async (claimId) => {
    showToast("AI đang tìm chứng cứ và phân tích...");
    try {
      const res = await api.post(`/api/claims/${claimId}/analyze`);
      showToast("AI đã hoàn thành phân tích!");

      // Tải lại danh sách claims và graph
      const claimRes = await api.get(`/api/claims/by-project/${project.id}`);
      setClaims(claimRes.data || []);
      const graphRes = await api.get(`/api/projects/${project.id}/traceability-export`);
      setGraphData(graphRes.data);

      // Nếu đang chọn chính claim này, cập nhật lại selectedClaim
      if (selectedClaim && selectedClaim.id === claimId) {
        setSelectedClaim(res.data);
        handleFetchMatches(claimId);
      }
    } catch (err) {
      console.error(err);
      showToast("Phân tích AI thất bại. Vui lòng kiểm tra dịch vụ AI.");
    }
  };

  // Lấy các nguồn chứng cứ khớp từ AI (Get matches)
  const handleFetchMatches = async (claimId) => {
    setLoadingMatches(true);
    try {
      const res = await api.get(`/api/claims/${claimId}/matches`, {
        params: { topK: 5 }
      });
      setClaimMatches(res.data?.matches || []);
    } catch (err) {
      console.error(err);
      showToast("Lấy chứng cứ khớp thất bại.");
    } finally {
      setLoadingMatches(false);
    }
  };

  // AI đánh giá cấu trúc bài viết (Run AI Review)
  const handleRunAiReview = async () => {
    if (!selectedPaper) {
      showToast("Vui lòng chọn hoặc tải lên một bản nháp bài viết trước.");
      return;
    }
    setLoadingAiReview(true);
    setShowAiReviewModal(true);
    try {
      const res = await api.post(`/api/papers/${selectedPaper.id}/review`);
      setAiReviewResult(res.data);
      showToast("AI Review hoàn thành.");
    } catch (err) {
      console.error(err);
      showToast("AI Review thất bại. Vui lòng kiểm tra dịch vụ AI.");
      setAiReviewResult({
        styleFeedback: "Không thể gọi dịch vụ AI Review. Vui lòng đảm bảo dịch vụ AI (Ollama/Embeddings) đang chạy.",
        structureFeedback: "Lỗi kết nối AI (500/Connection timeout)."
      });
    } finally {
      setLoadingAiReview(false);
    }
  };

  // Gửi bài cho giảng viên duyệt (Submit Review)
  const handleSubmitReview = async () => {
    if (!project) return;
    if (!selectedInstructorId) {
      showToast("Vui lòng chọn một Giảng viên để gửi duyệt.");
      return;
    }
    try {
      await api.post(`/api/projects/${project.id}/submit-review`, {
        instructorId: parseInt(selectedInstructorId)
      });
      showToast("Gửi yêu cầu phê duyệt thành công!");
      setShowSubmitReviewModal(false);

      // Tải lại thông tin dự án (để cập nhật trạng thái IN_REVIEW)
      await loadProjectData(project.id);
    } catch (err) {
      console.error(err);
      showToast("Gửi yêu cầu phê duyệt thất bại.");
    }
  };

  const preRef = useRef(null);

  const handleScroll = (e) => {
    if (preRef.current) {
      preRef.current.scrollTop = e.target.scrollTop;
      preRef.current.scrollLeft = e.target.scrollLeft;
    }
  };

  const showToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(''), 3000);
  };

  const renderPreview = () => {
    const titleMatch = displayContent.match(/\\title\{([^}]+)\}/);
    const authorMatch = displayContent.match(/\\author\{([^}]+)\}/);

    let body = displayContent.replace(/\\documentclass.*?\n/g, '')
      .replace(/\\usepackage.*?\n/g, '')
      .replace(/\\title\{.*?\}/g, '')
      .replace(/\\author\{.*?\}/g, '')
      .replace(/\\date\{.*?\}/g, '')
      .replace(/\\begin\{document\}/g, '')
      .replace(/\\end\{document\}/g, '')
      .replace(/\\maketitle/g, '');

    const sections = body.split(/\\section\{([^}]+)\}/);
    const parsedElements = [];

    const parseText = (text) => {
      const parts = text.split(/\\hl\{([^}]+)\}/g);
      return parts.map((part, index) => {
        if (index % 2 === 1) {
          return <span key={index} className="bg-yellow-200/80 px-1 rounded-sm border-b border-yellow-400">{part}</span>;
        }
        return part;
      });
    };

    if (sections[0] && sections[0].trim()) {
      parsedElements.push(<p key="intro" className="text-[14px] mb-8 leading-[1.8] text-slate-700 font-serif text-justify">{parseText(sections[0].trim())}</p>);
    }

    for (let i = 1; i < sections.length; i += 2) {
      const sectionTitle = sections[i];
      const sectionContent = sections[i + 1] || '';

      parsedElements.push(
        <h2 key={`h2-${i}`} className="font-bold text-lg mb-4 text-slate-800 font-serif">
          {Math.ceil(i / 2)}. {sectionTitle}
        </h2>
      );

      const paragraphs = sectionContent.split('\n\n').filter(p => p.trim());
      paragraphs.forEach((p, pIndex) => {
        parsedElements.push(
          <p key={`p-${i}-${pIndex}`} className="text-[14px] mb-8 leading-[1.8] text-slate-700 font-serif text-justify">
            {parseText(p.trim())}
          </p>
        );
      });
    }

    return (
      <div className="bg-white shadow-xl shadow-slate-200/50 ring-1 ring-slate-200 rounded-md w-full max-w-lg p-12 h-max min-h-[105%] transition-transform transform origin-top hover:scale-[1.01] duration-300">
        {titleMatch && (
          <h1 className="text-2xl font-serif font-bold text-center mb-3 leading-snug text-slate-900">
            {titleMatch[1].split('\\\\').map((line, i) => <React.Fragment key={i}>{line}<br /></React.Fragment>)}
          </h1>
        )}
        {authorMatch && (
          <p className="text-center text-sm mb-10 text-slate-600 font-serif italic">{authorMatch[1]}</p>
        )}
        {parsedElements}
      </div>
    );
  };

  const generateRichTextHtml = (latexCode) => {
    const titleMatch = latexCode.match(/\\title\{([^}]+)\}/);
    const authorMatch = latexCode.match(/\\author\{([^}]+)\}/);

    let body = latexCode.replace(/\\documentclass.*?\n/g, '')
      .replace(/\\usepackage.*?\n/g, '')
      .replace(/\\title\{.*?\}/g, '')
      .replace(/\\author\{.*?\}/g, '')
      .replace(/\\date\{.*?\}/g, '')
      .replace(/\\begin\{document\}/g, '')
      .replace(/\\end\{document\}/g, '')
      .replace(/\\maketitle/g, '');

    const sections = body.split(/\\section\{([^}]+)\}/);
    let html = '';

    if (titleMatch) {
      html += `<h1 class="text-3xl font-bold mb-2 text-slate-900">${titleMatch[1].replace(/\\\\/g, ' ')}</h1>`;
    }
    if (authorMatch) {
      html += `<p class="text-sm text-slate-500 mb-8 italic">By ${authorMatch[1]}</p>`;
    }

    const parseText = (text) => {
      let parsed = text;
      parsed = parsed.replace(/\\hl\{([^}]+)\}/g, '<span class="bg-yellow-200/50 px-1.5 rounded text-slate-800 border-b border-yellow-300">$1</span>');
      return parsed;
    };

    if (sections[0] && sections[0].trim()) {
      html += `<p class="mb-6 text-[15px] text-slate-700">${parseText(sections[0].trim())}</p>`;
    }

    for (let i = 1; i < sections.length; i += 2) {
      const sectionTitle = sections[i];
      const sectionContent = sections[i + 1] || '';

      html += `<h2 class="text-xl font-bold mb-3 text-slate-800">${sectionTitle}</h2>`;

      const paragraphs = sectionContent.split('\n\n').filter(p => p.trim());
      paragraphs.forEach(p => {
        html += `<p class="mb-6 text-[15px] text-slate-700">${parseText(p.trim())}</p>`;
      });
    }

    return html;
  };

  const parseHtmlToLatex = (container) => {
    let newLatex = `\\documentclass{article}\n\\usepackage[utf-8]{inputenc}\n\\usepackage{xcolor}\n\\usepackage{soul}\n\n`;
    Array.from(container.children).forEach(child => {
      if (child.tagName === 'H1') {
        newLatex += `\\title{${child.innerText}}\n`;
      } else if (child.tagName === 'P' && child.innerText.startsWith('By ')) {
        newLatex += `\\author{${child.innerText.substring(3)}}\n\\date{\\today}\n\n\\begin{document}\n\n\\maketitle\n\n`;
      } else if (child.tagName === 'H2') {
        newLatex += `\\section{${child.innerText}}\n\n`;
      } else if (child.tagName === 'P') {
        let text = child.innerHTML.replace(/<span[^>]*>(.*?)<\/span>/g, '\\hl{$1}').replace(/&nbsp;/g, ' ');
        text = text.replace(/<br\s*\/?>/gi, '\n');
        text = text.replace(/<[^>]*>?/gm, '');
        if (text.trim()) newLatex += `${text}\n\n`;
      }
    });
    return newLatex.trim();
  };

  return (
    <div className="h-screen w-full flex flex-col bg-slate-50 overflow-hidden font-sans antialiased text-slate-800">
      {/* Top Navigation Bar */}
      <header className="h-14 border-b border-slate-200 bg-white/80 backdrop-blur-md flex items-center justify-between px-4 shrink-0 shadow-sm z-20">
        <div className="flex items-center gap-4">
          <Link to="/" className="p-1.5 hover:bg-slate-100 rounded-lg text-slate-500 transition-colors">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
          </Link>
          <div className="flex items-center gap-3">
            <div className="w-7 h-7 bg-indigo-600 text-white rounded-md text-xs flex items-center justify-center font-bold shadow-sm shadow-indigo-200">EP</div>
            {projects.length > 0 ? (
              <div className="flex items-center gap-2">
                <select
                  value={project?.id || ''}
                  onChange={(e) => navigate(`/student/projects/${e.target.value}`)}
                  className="bg-white border border-slate-200 rounded-lg px-2 py-1 text-xs font-semibold text-slate-800 outline-none focus:ring-1 focus:ring-indigo-500 max-w-[200px]"
                >
                  {projects.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.title}
                    </option>
                  ))}
                </select>
                <button
                  onClick={handleCreateProject}
                  className="p-1 hover:bg-slate-100 rounded text-slate-500 transition-colors"
                  title={UI_TEXT[language].newProject}
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
                  </svg>
                </button>
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <span className="text-xs font-semibold text-slate-500">{UI_TEXT[language].noProjects}</span>
                <button
                  onClick={handleCreateProject}
                  className="px-2 py-1 bg-indigo-600 hover:bg-indigo-700 text-white text-[11px] font-bold rounded transition-colors"
                >
                  {UI_TEXT[language].createProject}
                </button>
              </div>
            )}
          </div>
        </div>
        <div className="flex items-center gap-3">
          <div className="hidden md:block text-xs text-slate-400 mr-2 font-medium">{UI_TEXT[language].workspaceDescription}</div>
          <div className="flex gap-1.5 bg-rose-50 border border-rose-100 rounded-full px-1 py-1">
            <span className="text-[11px] px-2 py-0.5 text-rose-700 font-semibold rounded-full bg-white shadow-sm">{UI_TEXT[language].returnedWithFeedback}</span>
            <span className="text-[11px] px-2 py-0.5 text-rose-700 font-semibold rounded-full bg-white shadow-sm flex items-center gap-1">
              <div className="w-1.5 h-1.5 rounded-full bg-rose-500 animate-pulse"></div>
              {feedbacks.length} {UI_TEXT[language].feedbacks}
            </span>
          </div>
          <button
            onClick={() => setShowHistoryModal(true)}
            className="flex items-center gap-1.5 text-xs font-semibold text-slate-600 hover:text-slate-900 border border-slate-200 px-3 py-1.5 rounded-lg hover:bg-slate-50 transition-all shadow-sm ml-2">
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            {UI_TEXT[language].history}
          </button>
          <button
            onClick={() => setShowReviseModal(true)}
            className="text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 px-4 py-1.5 rounded-lg flex items-center gap-1.5 shadow-md shadow-indigo-600/20 transition-all hover:shadow-indigo-600/40 transform hover:-translate-y-0.5">
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            {UI_TEXT[language].revise}
          </button>
          <button
            onClick={toggleLanguage}
            className="text-xs font-semibold text-slate-600 border border-slate-200 px-2.5 py-1.5 rounded-lg hover:bg-slate-100 transition-all ml-1"
            title={language === 'vi' ? 'Switch to English' : 'Chuyển sang Tiếng Việt'}
          >
            {language === 'vi' ? 'EN' : 'VI'}
          </button>
          <button
            onClick={() => { logout(); navigate('/'); }}
            className="text-xs font-medium text-slate-500 hover:text-red-600 border border-slate-200 px-3 py-1.5 rounded-lg hover:border-red-200 hover:bg-red-50 transition-all ml-1"
          >
            {UI_TEXT[language].signOut}
          </button>
        </div>
      </header>

      {/* Main Workspace Area */}
      <div className="flex-1 flex overflow-hidden">
        {/* Activity Bar (Branded style) */}
        <div className="w-14 bg-indigo-900 flex flex-col items-center py-4 shrink-0 z-20 border-r border-indigo-950 shadow-[2px_0_8px_-2px_rgba(0,0,0,0.2)]">
          {/* Active Icon (Files) */}
          <div className="w-full flex justify-center relative cursor-pointer mb-6 group">
            <div className="absolute left-0 top-0 bottom-0 w-1 bg-white rounded-r-md shadow-[0_0_8px_rgba(255,255,255,0.8)]"></div>
            <svg className="w-[22px] h-[22px] text-white transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
          </div>
          {/* Inactive Icon (History) */}
          <div onClick={() => showToast('Global History opened')} className="w-full flex justify-center cursor-pointer mb-6 group relative">
            <div className="absolute left-0 top-0 bottom-0 w-1 bg-transparent group-hover:bg-indigo-400 rounded-r-md transition-colors"></div>
            <svg className="w-[22px] h-[22px] text-indigo-300 group-hover:text-white transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
          </div>
          {/* Inactive Icon (Settings) */}
          <div onClick={() => showToast('Settings opened')} className="w-full flex justify-center cursor-pointer group relative">
            <div className="absolute left-0 top-0 bottom-0 w-1 bg-transparent group-hover:bg-indigo-400 rounded-r-md transition-colors"></div>
            <svg className="w-[22px] h-[22px] text-indigo-300 group-hover:text-white transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
          </div>
        </div>

        {/* Left Sidebar: Paper & Source Management */}
        <aside className="w-64 bg-slate-50/50 border-r border-slate-200 flex flex-col shrink-0 z-10 backdrop-blur-sm">
          {/* Section 1: Paper Drafts */}
          <div className="px-4 py-3 border-b border-slate-200 flex justify-between items-center bg-slate-100/40">
            <span className="text-[11px] font-bold text-slate-500 tracking-wider uppercase">{UI_TEXT[language].paperDrafts}</span>
            <label className="text-slate-400 hover:text-indigo-600 transition-colors cursor-pointer" title="Tải lên bản nháp mới">
              <input
                type="file"
                className="hidden"
                accept=".pdf,.docx"
                onChange={(e) => {
                  if (e.target.files && e.target.files.length > 0) {
                    handleUploadPaper(e.target.files[0]);
                  }
                }}
              />
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" /></svg>
            </label>
          </div>

          <div className="p-2 flex-1 max-h-[45%] overflow-y-auto border-b border-slate-200 custom-scrollbar">
            {papers.length === 0 ? (
              <div className="text-xs text-slate-400 italic text-center py-4">Chưa có bản nháp nào được tải lên.</div>
            ) : (
              papers.map(p => (
                <div
                  key={p.id}
                  onClick={() => {
                    setSelectedPaper(p);
                    setCodeContent(p.extractedText || '');
                  }}
                  className={`flex items-center justify-between text-xs font-medium p-2 rounded-md cursor-pointer transition-all mt-1 group ${selectedPaper?.id === p.id ? 'bg-indigo-50 text-indigo-700 border border-indigo-100 shadow-sm' : 'text-slate-600 hover:bg-slate-100'}`}
                >
                  <div className="flex items-center gap-2 truncate">
                    <svg className="w-3.5 h-3.5 shrink-0 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
                    <span className="truncate" title={p.originalFilename}>{p.originalFilename}</span>
                  </div>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDeletePaper(p.id);
                    }}
                    className="opacity-0 group-hover:opacity-100 hover:text-red-600 transition-all p-0.5"
                    title="Xóa bản nháp"
                  >
                    <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
                  </button>
                </div>
              ))
            )}
          </div>

          {/* Section 2: Uploaded Sources */}
          <div className="px-4 py-3 border-b border-slate-200 flex justify-between items-center bg-slate-100/40">
            <span className="text-[11px] font-bold text-slate-500 tracking-wider uppercase">{UI_TEXT[language].sources}</span>
            <label className="text-slate-400 hover:text-indigo-600 transition-colors cursor-pointer" title="Tải lên tài liệu mới">
              <input
                type="file"
                className="hidden"
                accept=".pdf,.docx"
                onChange={(e) => {
                  if (e.target.files && e.target.files.length > 0) {
                    handleUploadSource(e.target.files[0]);
                  }
                }}
              />
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" /></svg>
            </label>
          </div>

          <div className="p-2 flex-1 overflow-y-auto custom-scrollbar">
            {sources.length === 0 ? (
              <div className="text-xs text-slate-400 italic text-center py-4">Chưa có tài liệu tham khảo nào.</div>
            ) : (
              sources.map(src => (
                <div
                  key={src.id}
                  onClick={() => showToast(`Đang xem thông tin tài liệu: ${src.originalFilename}`)}
                  className="flex items-center justify-between text-xs font-medium p-2 rounded-md hover:bg-slate-100 cursor-pointer transition-all mt-1 group text-slate-600"
                >
                  <div className="flex items-center gap-2 truncate">
                    <svg className="w-3.5 h-3.5 shrink-0 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg>
                    <span className="truncate" title={src.originalFilename}>{src.originalFilename}</span>
                  </div>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDeleteSource(src.id);
                    }}
                    className="opacity-0 group-hover:opacity-100 hover:text-red-600 transition-all p-0.5"
                    title="Xóa tài liệu"
                  >
                    <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
                  </button>
                </div>
              ))
            )}
          </div>
        </aside>


        {/* Center Panes: Editor & Preview */}
        <div className="flex-1 flex overflow-hidden bg-slate-200/50 p-3 gap-3">

          {/* Editor Pane */}
          <div className="flex-1 bg-white rounded-xl shadow-sm border border-slate-200 flex flex-col overflow-hidden">
            <div className="h-11 border-b border-slate-100 flex items-center justify-between px-3 bg-white shadow-sm z-10">
              <div className="flex gap-1 border-r border-slate-200 pr-2">
                <button onClick={() => showToast('Bold applied')} className="w-7 h-7 flex items-center justify-center hover:bg-slate-100 rounded-md text-slate-600 transition-colors"><span className="font-bold font-serif">B</span></button>
                <button onClick={() => showToast('Italic applied')} className="w-7 h-7 flex items-center justify-center hover:bg-slate-100 rounded-md text-slate-600 transition-colors"><span className="italic font-serif">I</span></button>
              </div>
              <div className="flex gap-1 border-r border-slate-200 px-2">
                <button onClick={() => showToast('List applied')} className="w-7 h-7 flex items-center justify-center hover:bg-slate-100 rounded-md text-slate-600 transition-colors"><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6h16M4 12h16M4 18h16" /></svg></button>
                <button onClick={() => showToast('List applied')} className="w-7 h-7 flex items-center justify-center hover:bg-slate-100 rounded-md text-slate-600 transition-colors"><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6h16M4 10h16M4 14h16M4 18h16" /></svg></button>
              </div>
              <div className="flex gap-1 px-2 flex-1 items-center">
                <button onClick={() => showToast('Equation editor')} className="w-7 h-7 flex items-center justify-center hover:bg-slate-100 rounded-md text-slate-600 font-serif text-sm transition-colors mr-2">∑</button>
                {selectedPaper ? (
                  <span className="text-[11px] text-slate-400 italic truncate max-w-[200px]">Đang xem: {selectedPaper.originalFilename}</span>
                ) : (
                  <span className="text-[11px] text-amber-600 font-medium">{UI_TEXT[language].sampleDocument}</span>
                )}
              </div>
              <div className="flex items-center gap-3 text-xs font-medium text-slate-500">
                {selectedPaper && (
                  <button
                    onClick={handleRunAiReview}
                    className="bg-indigo-600 hover:bg-indigo-700 text-white px-2.5 py-1 rounded-md text-xs font-bold flex items-center gap-1 shadow-sm transition-colors"
                    title="AI Review cấu trúc và định dạng của paper"
                  >
                    <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 01-2 2h0a2 2 0 01-2-2v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" /></svg>
                    {UI_TEXT[language].aiReview}
                  </button>
                )}
                <div className="flex bg-slate-100 rounded-lg p-0.5 border border-slate-200">
                  <button
                    onClick={() => setEditorMode('Code')}
                    className={`px-3 py-1 rounded-md text-xs font-semibold transition-colors ${editorMode === 'Code' ? 'bg-white shadow-sm text-slate-800' : 'text-slate-500 hover:text-slate-700'}`}>
                    Code
                  </button>
                  <button
                    onClick={() => setEditorMode('Rich Text')}
                    className={`px-3 py-1 rounded-md text-xs font-semibold transition-colors ${editorMode === 'Rich Text' ? 'bg-white shadow-sm text-slate-800' : 'text-slate-500 hover:text-slate-700'}`}>
                    Rich Text
                  </button>
                </div>
              </div>
            </div>
            {/* Editor Area (Mock) */}
            {editorMode === 'Code' ? (
              <div className="relative flex-1 bg-[#0d1117] overflow-hidden group">
                <textarea
                  value={displayContent}
                  onChange={(e) => setCodeContent(e.target.value)}
                  onScroll={(e) => {
                    if (preRef.current) {
                      preRef.current.scrollTop = e.target.scrollTop;
                      preRef.current.scrollLeft = e.target.scrollLeft;
                    }
                  }}
                  spellCheck={false}
                  className="absolute inset-0 w-full h-full bg-transparent text-transparent caret-white resize-none outline-none z-10 m-0 border-0 font-mono text-sm p-5 whitespace-pre-wrap break-words leading-relaxed overflow-auto custom-scrollbar"
                />
                <pre
                  ref={preRef}
                  className="absolute inset-0 w-full h-full pointer-events-none text-slate-300 m-0 border-0 font-mono text-sm p-5 whitespace-pre-wrap break-words leading-relaxed overflow-auto custom-scrollbar"
                  aria-hidden="true"
                >
                  {displayContent.split(/(\\[a-zA-Z]+|\{[^{}]*\})/g).map((part, j) => {
                    if (!part) return null;
                    if (part.startsWith('\\')) return <span key={j} className="text-[#ff7b72]">{part}</span>;
                    if (part.startsWith('{') && part.endsWith('}')) {
                      return <span key={j} className="text-[#a5d6ff]">
                        <span className="text-slate-400">{'{'}</span>
                        {part.slice(1, -1)}
                        <span className="text-slate-400">{'}'}</span>
                      </span>;
                    }
                    return <span key={j} className="text-slate-100">{part}</span>;
                  })}
                </pre>
              </div>
            ) : (
              <RichTextEditor
                initialHtml={generateRichTextHtml(displayContent)}
                onHtmlChange={(target) => {
                  const newCode = parseHtmlToLatex(target);
                  setCodeContent(newCode);
                }}
              />
            )}
          </div>

          {/* PDF Preview Pane */}
          <div className="flex-1 bg-white rounded-xl shadow-sm border border-slate-200 flex flex-col overflow-hidden">
            <div className="h-11 border-b border-slate-100 flex items-center justify-between px-4 bg-white">
              <div className="flex items-center gap-2 text-sm font-bold text-slate-700">
                <svg className="w-4 h-4 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
                PDF Preview
              </div>
              <button onClick={() => showToast('Recompiling PDF...')} className="bg-emerald-500 hover:bg-emerald-600 text-white px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow-sm shadow-emerald-500/20 transition-all hover:-translate-y-0.5">
                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" /></svg>
                Recompile
              </button>
            </div>
            <div className="flex-1 bg-slate-100/50 p-6 overflow-y-auto flex justify-center custom-scrollbar">
              {/* Mock Document */}
              {renderPreview()}
            </div>
          </div>
        </div>
        <aside className="w-[380px] bg-white border-l border-slate-200 flex flex-col shrink-0 shadow-[-4px_0_15px_-3px_rgba(0,0,0,0.05)] z-10">
          {/* Tabs */}
          <div className="flex border-b border-slate-200 bg-white relative shrink-0">
            <button
              onClick={() => setActiveTab('Source')}
              className={`flex-1 py-3 text-[10px] font-bold uppercase tracking-wider flex flex-col justify-center items-center gap-1 transition-all relative ${activeTab === 'Source' ? 'text-indigo-600' : 'text-slate-500 hover:text-slate-800 hover:bg-slate-50'}`}>
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
              {UI_TEXT[language].tabInfo}
              {activeTab === 'Source' && <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-600 shadow-[0_-2px_8px_rgba(79,70,229,0.5)]"></div>}
            </button>
            <button
              onClick={() => setActiveTab('Claims')}
              className={`flex-1 py-3 text-[10px] font-bold uppercase tracking-wider flex flex-col justify-center items-center gap-1 transition-all relative ${activeTab === 'Claims' ? 'text-indigo-600' : 'text-slate-500 hover:text-slate-800 hover:bg-slate-50'}`}>
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" /></svg>
              {UI_TEXT[language].tabClaims}
              {activeTab === 'Claims' && <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-600 shadow-[0_-2px_8px_rgba(79,70,229,0.5)]"></div>}
            </button>
            <button
              onClick={() => setActiveTab('Feedback')}
              className={`flex-1 py-3 text-[10px] font-bold uppercase tracking-wider flex flex-col justify-center items-center gap-1 transition-all relative ${activeTab === 'Feedback' ? 'text-indigo-600' : 'text-slate-500 hover:text-slate-800 hover:bg-slate-50'}`}>
              <div className="relative">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" /></svg>
                {feedbacks.length > 0 && (
                  <span className="absolute -top-1.5 -right-2 bg-rose-500 text-white flex items-center justify-center text-[9px] w-4 h-4 rounded-full font-bold animate-pulse">{feedbacks.length}</span>
                )}
              </div>
              {UI_TEXT[language].tabFeedback}
              {activeTab === 'Feedback' && <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-600 shadow-[0_-2px_8px_rgba(79,70,229,0.5)]"></div>}
            </button>
            <button
              onClick={() => setActiveTab('Graph')}
              className={`flex-1 py-3 text-[10px] font-bold uppercase tracking-wider flex flex-col justify-center items-center gap-1 transition-all relative ${activeTab === 'Graph' ? 'text-indigo-600' : 'text-slate-500 hover:text-slate-800 hover:bg-slate-50'}`}>
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4" /></svg>
              {UI_TEXT[language].tabGraph}
              {activeTab === 'Graph' && <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-indigo-600 shadow-[0_-2px_8px_rgba(79,70,229,0.5)]"></div>}
            </button>
          </div>

          {/* Tab Content */}
          <div className="flex-1 overflow-y-auto bg-slate-50/50 p-4">

            {/* 1. PROJECT INFO TAB */}
            {activeTab === 'Source' && (
              <div className="p-5 flex flex-col gap-6 animate-in fade-in duration-300">
                <label className={`w-full flex justify-center items-center gap-2 border-2 border-dashed rounded-xl p-6 transition-all group mb-6 shadow-sm cursor-pointer ${isUploading ? 'border-indigo-300 bg-indigo-100/50 opacity-60 pointer-events-none' : 'border-indigo-200 hover:border-indigo-400 bg-indigo-50/50 hover:bg-indigo-50'}`}>
                  <input
                    type="file"
                    className="hidden"
                    accept=".pdf,.docx"
                    disabled={isUploading}
                    onChange={async (e) => {
                      if (e.target.files && e.target.files.length > 0) {
                        const file = e.target.files[0];
                        if (!project) {
                          showToast('No project selected to upload to.');
                          return;
                        }
                        setIsUploading(true);
                        const formData = new FormData();
                        formData.append('file', file);
                        try {
                          await api.post(`/api/sources/upload?uploadedBy=${user.id}&projectId=${project.id}`, formData);
                          showToast(`${file.name} uploaded successfully.`);
                          const srcRes = await api.get(`/api/projects/${project.id}/sources`);
                          setSources(srcRes.data);
                        } catch (err) {
                          console.error('Upload failed', err);
                          showToast(`Failed to upload ${file.name}`);
                        } finally {
                          setIsUploading(false);
                        }
                      }
                    }}
                  />
                  <div className="bg-white p-2 rounded-full shadow-sm group-hover:scale-110 transition-transform">
                    {isUploading ? (
                      <div className="animate-spin w-5 h-5 border-2 border-indigo-500 border-t-transparent rounded-full"></div>
                    ) : (
                      <svg className="w-5 h-5 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" /></svg>
                    )}
                  </div>
                  <span className="text-sm font-semibold text-indigo-700">{isUploading ? 'Uploading...' : 'Upload PDF / DOCX'}</span>
                </label>

                <div>
                  <h3 className="text-[11px] font-bold text-slate-400 tracking-widest mb-3 uppercase flex items-center gap-2">
                    <div className="h-px bg-slate-200 flex-1"></div> Shared Resources <div className="h-px bg-slate-200 flex-1"></div>
                  </h3>
                  <div className="bg-white border border-slate-200 rounded-xl shadow-sm mb-3 hover:border-indigo-300 hover:shadow-md transition-all overflow-hidden group">
                    <div className="p-3.5 border-b border-slate-100 flex justify-between items-start bg-slate-50/50">
                      <div>
                        <h4 className="font-bold text-sm text-slate-800 group-hover:text-indigo-700 transition-colors">Agile Risk Evidence Pack</h4>
                        <p className="text-xs text-slate-500 mt-1 line-clamp-2 leading-relaxed">Instructor-curated sources for communication, sprint feedback, and agile risk claims.</p>
                      </div>
                      <span className="bg-indigo-100 text-indigo-700 text-xs font-bold px-2 py-1 rounded-md shadow-sm">4</span>
                    </div>
                    <div className="p-0">
                      <div onClick={() => showToast('Opening resource: instructor-agile-risk-framework.pdf')} className="px-4 py-2.5 border-b border-slate-100 bg-white hover:bg-slate-50 transition-colors cursor-pointer">
                        <p className="text-sm font-semibold text-slate-700 flex items-center gap-2"><svg className="w-3.5 h-3.5 text-red-500" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clipRule="evenodd" /></svg>instructor-agile-risk-framework.pdf</p>
                        <p className="text-[11px] text-slate-400 mt-1 pl-5">Risk control improves when agile teams define escalation paths...</p>
                      </div>
                      <div onClick={() => showToast('Opening resource: feedback-loop-benchmark.docx')} className="px-4 py-2.5 border-b border-slate-100 bg-white hover:bg-slate-50 transition-colors cursor-pointer">
                        <p className="text-sm font-semibold text-slate-700 flex items-center gap-2"><svg className="w-3.5 h-3.5 text-blue-500" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clipRule="evenodd" /></svg>feedback-loop-benchmark.docx</p>
                        <p className="text-[11px] text-slate-400 mt-1 pl-5">Teams with structured sprint feedback loops identify blockers...</p>
                      </div>
                      <div onClick={() => showToast('Loading more sources...')} className="px-4 py-2 bg-slate-50/50 hover:bg-slate-100 text-[11px] text-indigo-600 font-bold text-center uppercase tracking-wider cursor-pointer transition-colors">
                        Show 2 more...
                      </div>
                    </div>
                  </div>
                </div>

                <div>
                  <h3 className="text-[11px] font-bold text-slate-400 tracking-widest mb-3 uppercase flex items-center gap-2">
                    <div className="h-px bg-slate-200 flex-1"></div> Uploaded Sources <div className="h-px bg-slate-200 flex-1"></div>
                  </h3>
                  <div className="flex flex-col gap-3">
                    {sources.length === 0 ? (
                      <div className="text-sm text-slate-500 italic text-center p-4">No uploaded sources yet.</div>
                    ) : (
                      sources.map(src => (
                        <div key={src.id} onClick={() => src.fileUrl ? setViewerFile({ fileUrl: src.fileUrl, fileName: src.originalFilename }) : showToast('File URL not available')} className="bg-white border border-slate-200 rounded-xl p-3.5 hover:shadow-md hover:border-indigo-300 transition-all cursor-pointer transform hover:-translate-y-0.5">
                          <p className="text-sm font-bold text-slate-800 flex items-center gap-2"><svg className="w-4 h-4 text-red-500" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clipRule="evenodd" /></svg>{src.originalFilename}</p>
                          <p className="text-xs text-slate-500 mt-1.5 line-clamp-2 leading-relaxed">Source file uploaded to this project.</p>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              </div>
            )}

            {/* Danh sách Claims */}
            {activeTab === 'Claims' && (
            <div className="space-y-3">
              {claims.length === 0 ? (
                <div className="text-xs text-slate-400 italic text-center py-8">Dự án này chưa có luận điểm nào. Hãy thêm ở trên.</div>
              ) : (
                claims.map(claim => {
                  const isSelected = selectedClaim?.id === claim.id;
                  return (
                    <div
                      key={claim.id}
                      onClick={() => {
                        setSelectedClaim(claim);
                        handleFetchMatches(claim.id);
                      }}
                      className={`bg-white border rounded-xl p-3.5 shadow-sm hover:shadow-md transition-all relative overflow-hidden group cursor-pointer ${isSelected ? 'border-indigo-400 ring-1 ring-indigo-400/20' : 'border-slate-200'}`}
                    >
                      <div className="absolute left-0 top-0 bottom-0 w-1.5 bg-indigo-500"></div>
                      <div className="flex justify-between items-center mb-1.5 pl-1">
                        <span className="text-[9px] font-black text-indigo-700 bg-indigo-50 px-1.5 py-0.5 rounded border border-indigo-100 uppercase tracking-wide">
                          ID: {claim.id}
                        </span>
                        {claim.aiConfidenceScore !== null ? (
                          <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded ${claim.aiConfidenceScore >= 0.7 ? 'bg-emerald-50 text-emerald-700 border border-emerald-100' : claim.aiConfidenceScore >= 0.4 ? 'bg-amber-50 text-amber-700 border-amber-100' : 'bg-rose-50 text-rose-700 border border-rose-100'}`}>
                            Độ tin cậy: {(claim.aiConfidenceScore * 100).toFixed(0)}%
                          </span>
                        ) : (
                          <span className="text-[10px] text-slate-400 italic">Chưa phân tích</span>
                        )}
                      </div>
                      <p className="text-xs font-semibold text-slate-800 pl-1 leading-relaxed">
                        {claim.content}
                      </p>

                      <div className="flex gap-2 mt-3 pt-2.5 border-t border-slate-100 pl-1">
                        <button
                          onClick={(e) => { e.stopPropagation(); handleAnalyzeClaim(claim.id); }}
                          className="text-[10px] font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
                        >
                          <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z" /></svg>
                          AI phân tích
                        </button>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            setEditingClaim(claim);
                            setEditClaimContent(claim.content);
                          }}
                          className="text-[10px] text-slate-500 hover:text-slate-700 flex items-center gap-0.5 ml-auto"
                        >
                          Sửa
                        </button>
                        <button
                          onClick={(e) => { e.stopPropagation(); handleDeleteClaim(claim.id); }}
                          className="text-[10px] text-rose-500 hover:text-rose-700 flex items-center gap-0.5"
                        >
                          Xóa
                        </button>
                      </div>

                      {/* Hiển thị danh sách Matches khi được chọn */}
                      {isSelected && (
                        <div className="mt-3 pt-3 border-t border-dashed border-slate-200 animate-in fade-in slide-in-from-top-1 duration-200">
                          <h4 className="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-2">Chứng cứ khớp nhất (AI gợi ý)</h4>
                          {loadingMatches ? (
                            <div className="text-center py-2 text-[10px] text-slate-400 italic">Đang tìm tài liệu đối chiếu...</div>
                          ) : claimMatches.length === 0 ? (
                            <div className="text-center py-2 text-[10px] text-slate-400 italic">Không tìm thấy tài liệu nào khớp.</div>
                          ) : (
                            <div className="space-y-2">
                              {claimMatches.map((m, idx) => (
                                <div key={idx} className="bg-slate-50 border border-slate-200 rounded p-2 text-[11px] hover:bg-indigo-50/30 transition-colors">
                                  <div className="flex justify-between items-center mb-1 text-[9px] font-medium text-slate-500">
                                    <span className="truncate max-w-[150px] font-bold text-slate-700 flex items-center gap-1"><svg className="w-2.5 h-2.5 text-red-400" fill="currentColor" viewBox="0 0 20 20"><path fillRule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clipRule="evenodd" /></svg>{m.filename}</span>
                                    <span className="text-indigo-600 font-bold bg-indigo-50 px-1 rounded">{(m.score * 100).toFixed(0)}% khớp</span>
                                  </div>
                                  <p className="text-[10px] text-slate-600 line-clamp-3 italic leading-relaxed">"{m.excerpt}"</p>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })
              )}
            </div>
            )}

          {/* 3. FEEDBACK TAB (Nhận xét của Instructor) */}
          {activeTab === 'Feedback' && (
            <div className="flex flex-col gap-4 animate-in fade-in duration-200">
              <div className="flex justify-between items-center mb-1 bg-white border border-slate-200 rounded-xl p-3.5 shadow-sm">
                <div>
                  <p className="text-[10px] text-slate-400 uppercase tracking-wider font-bold">Trạng thái dự án</p>
                  <p className="text-sm font-bold text-slate-800 mt-0.5">
                    {project?.status === 'DRAFT' && 'Bản nháp (Draft)'}
                    {project?.status === 'ACTIVE' && 'Đang hoạt động (Active)'}
                    {project?.status === 'IN_REVIEW' && 'Đang chờ duyệt (In Review)'}
                    {project?.status === 'COMPLETED' && 'Đã hoàn thành (Completed)'}
                    {project?.status === 'ARCHIVED' && 'Đã lưu trữ (Archived)'}
                    {project?.status === 'DELETED' && 'Đã xóa (Deleted)'}
                  </p>
                </div>
                {project?.status === 'ACTIVE' && (
                  <button
                    onClick={() => setShowSubmitReviewModal(true)}
                    className="bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold px-3 py-1.5 rounded-lg shadow-sm transition-all"
                  >
                    Gửi duyệt
                  </button>
                )}
              </div>

              <h3 className="text-[11px] font-bold text-slate-400 tracking-widest uppercase flex items-center gap-2 mt-2">
                <div className="h-px bg-slate-200 flex-1"></div> Lịch sử đánh giá <div className="h-px bg-slate-200 flex-1"></div>
              </h3>

              <div className="space-y-4">
                {feedbacks.length === 0 ? (
                  <div className="text-xs text-slate-400 italic text-center py-8">Chưa có yêu cầu duyệt hoặc nhận xét nào từ giảng viên.</div>
                ) : (
                  feedbacks.map((fb, idx) => (
                    <div key={fb.id || idx} className="bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden">
                      <div className="bg-slate-50 border-b border-slate-100 p-3 flex justify-between items-start">
                        <div className="flex items-center gap-2">
                          <div className="w-7 h-7 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center font-bold text-xs border border-indigo-200">I</div>
                          <div>
                            <p className="text-xs font-bold text-slate-700">Giảng viên (ID: {fb.instructorId})</p>
                            <p className="text-[9px] text-slate-400 font-medium">{fb.requestedAt ? new Date(fb.requestedAt).toLocaleString('vi-VN') : ''}</p>
                          </div>
                        </div>
                        <span className={`text-[9px] px-2 py-0.5 rounded font-black border uppercase ${fb.status === 'PENDING' ? 'bg-amber-50 text-amber-700 border-amber-200' : fb.status === 'RETURNED' ? 'bg-rose-50 text-rose-700 border-rose-200' : fb.status === 'REVIEWED' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-rose-50 text-rose-700'}`}>{fb.status}</span>
                      </div>
                      <div className="p-3 text-xs leading-relaxed text-slate-700">
                        {fb.status === 'PENDING' && (
                          <p className="text-amber-600 font-medium italic">Dự án đã được gửi đi. Đang chờ giảng viên kiểm tra và cho nhận xét.</p>
                        )}
                        {fb.status === 'RETURNED' && (
                          <p className="text-rose-600 font-medium">Giảng viên đã trả lại dự án này. Vui lòng kiểm tra lại sự tương đồng giữa các luận điểm đã viết và tài liệu chứng cứ đính kèm, chỉnh sửa lại rồi gửi lại.</p>
                        )}
                        {fb.status === 'REVIEWED' && (
                          <p className="text-emerald-600 font-medium">Giảng viên đã duyệt thành công dự án của bạn. Điểm số tương thích giữa các lập luận và nguồn đã được xác thực tốt.</p>
                        )}
                        {fb.status === 'REJECTED' && (
                          <p className="text-red-600 font-medium">Yêu cầu duyệt của bạn bị từ chối.</p>
                        )}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {/* 4. GRAPH TAB (Đồ thị đối chiếu thực tế) */}
          {activeTab === 'Graph' && (
            <div className="flex flex-col gap-4 animate-in fade-in duration-200">
              {graphData && graphData.claims && graphData.claims.length > 0 ? (
                <div className="space-y-4">
                  {/* Sơ đồ liên kết Claims -> Verdict -> Source */}
                  <div className="bg-slate-900 rounded-xl p-4 border border-slate-800 text-slate-200">
                    <div className="flex justify-between items-center mb-3">
                      <h4 className="font-bold text-xs text-indigo-400 flex items-center gap-1">
                        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 002-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" /></svg>
                        Mạng lưới đối chiếu nguồn
                      </h4>
                      <span className="text-[9px] text-slate-500">Mô hình liên kết thực tế</span>
                    </div>

                    {/* Biểu diễn trực quan hóa các mối liên kết */}
                    <div className="space-y-3 max-h-72 overflow-y-auto custom-scrollbar pr-1">
                      {graphData.claims.map((c, idx) => {
                        const graphInfo = c.graphData || {};
                        const hasEdge = graphInfo.status !== 'MISSING' && graphInfo.verdict;
                        return (
                          <div key={idx} className="bg-slate-800/80 border border-slate-700/60 rounded-lg p-2.5 text-xs">
                            <div className="font-bold text-slate-300 truncate mb-1">C.{idx + 1}: {c.content}</div>
                            {hasEdge ? (
                              <div className="space-y-1.5 pl-2 border-l border-indigo-500/50 mt-2">
                                <div className="flex justify-between items-center text-[10px]">
                                  <span className="text-slate-400">Verdict:</span>
                                  <span className={`font-black px-1.5 py-0.5 rounded text-[9px] ${graphInfo.verdict === 'SUPPORTED' ? 'bg-emerald-950 text-emerald-400 border border-emerald-800' : graphInfo.verdict === 'REFUTED' ? 'bg-rose-950 text-rose-400 border border-rose-800' : 'bg-amber-950 text-amber-400 border border-amber-800'}`}>{graphInfo.verdict}</span>
                                </div>
                                <div className="flex justify-between text-[10px]">
                                  <span className="text-slate-400">Confidence:</span>
                                  <span className="text-indigo-400 font-bold">{(graphInfo.confidence * 100).toFixed(0)}%</span>
                                </div>
                                <div className="text-[10px] text-slate-400 mt-1">
                                  <span className="block text-slate-500 font-bold">Giải thích:</span>
                                  <p className="italic text-slate-300 pl-1 leading-relaxed">"{graphInfo.explanation}"</p>
                                </div>
                                {graphInfo.missing_evidence && graphInfo.missing_evidence.length > 0 && (
                                  <div className="text-[10px] text-rose-400 mt-1">
                                    <span className="block text-slate-500 font-bold">Chứng cứ thiếu:</span>
                                    <ul className="list-disc pl-3 text-slate-300 space-y-0.5">
                                      {graphInfo.missing_evidence.map((me, i) => <li key={i}>{me}</li>)}
                                    </ul>
                                  </div>
                                )}
                              </div>
                            ) : (
                              <div className="text-[10px] text-slate-500 italic mt-1 pl-2">Chưa chạy AI phân tích chứng cứ cho luận điểm này.</div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  </div>

                  {/* Thông tin thống kê nguồn */}
                  <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-sm">
                    <h4 className="font-bold text-xs text-slate-700 mb-3">Tóm tắt các Nguồn đối chiếu</h4>
                    <div className="space-y-2">
                      {graphData.sources && graphData.sources.map((s, i) => (
                        <div key={i} className="flex justify-between items-center text-xs p-2 bg-slate-50 border border-slate-100 rounded-lg">
                          <span className="truncate max-w-[200px] font-medium text-slate-700 flex items-center gap-1"><svg className="w-3.5 h-3.5 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg>{s.filename}</span>
                          <span className="text-[10px] text-slate-500 bg-slate-200/60 px-2 py-0.5 rounded-full font-bold">{s.referenceCount} Trích dẫn</span>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              ) : (
                <div className="text-xs text-slate-400 italic text-center py-8">Chưa có dữ liệu đồ thị. Vui lòng bấm nút "AI phân tích" ở Tab Luận điểm cho các luận điểm của bạn.</div>
              )}
            </div>
          )}

      </div>
    </aside>
      </div>

    {/* HISTORY MODAL */ }
  {
    showHistoryModal && (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm animate-in fade-in duration-200">
        <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6 transform transition-all">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-lg font-bold text-slate-800">Lịch sử phiên bản</h2>
            <button onClick={() => setShowHistoryModal(false)} className="text-slate-400 hover:text-slate-600 transition-colors">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
            </button>
          </div>
          <div className="space-y-3">
            <div className="flex justify-between items-center p-3 bg-indigo-50 border border-indigo-100 rounded-lg">
              <div>
                <p className="text-sm font-bold text-slate-800">Phiên bản hiện tại</p>
                <p className="text-xs text-slate-500 mt-0.5">Vừa xong</p>
              </div>
              <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-600 bg-indigo-100 px-2 py-1 rounded">Đang hoạt động</span>
            </div>
          </div>
        </div>
      </div>
    )
  }

  {/* REVISE MODAL */ }
  {
    showReviseModal && (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm animate-in fade-in duration-200">
        <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6 transform transition-all">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-lg font-bold text-slate-800">Tự động sửa tài liệu</h2>
            <button onClick={() => setShowReviseModal(false)} className="text-slate-400 hover:text-slate-600 transition-colors">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
            </button>
          </div>
          <p className="text-sm text-slate-600 mb-6">Chọn các phần bạn muốn trợ lý AI hỗ trợ sửa đổi dựa trên nhận xét của giảng viên và các luận điểm đối chiếu.</p>

          <div className="space-y-3 mb-6">
            <label className="flex items-center gap-3 p-3 border border-slate-200 rounded-lg cursor-pointer hover:bg-slate-50 transition-colors">
              <input type="checkbox" className="w-4 h-4 text-indigo-600 rounded border-slate-300 focus:ring-indigo-500" defaultChecked />
              <span className="text-sm font-medium text-slate-700">Sửa đổi các lập luận chưa khớp (Phần 3)</span>
            </label>
          </div>

          <div className="flex justify-end gap-3">
            <button onClick={() => setShowReviseModal(false)} className="px-4 py-2 text-sm font-semibold text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">Hủy</button>
            <button onClick={() => {
              setShowReviseModal(false);
              alert("Yêu cầu sửa đổi đã gửi! AI đang tiến hành xử lý.");
            }} className="px-4 py-2 text-sm font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg shadow-sm shadow-indigo-200 transition-colors">
              Start Revision
            </button>
          </div>
        </div>
      </div>
    )
  }

  {
    viewerFile && (
      <FileViewerModal
        fileUrl={viewerFile.fileUrl}
        fileName={viewerFile.fileName}
        onClose={() => setViewerFile(null)}
      />
    )
  }
    </div >
  );
}
