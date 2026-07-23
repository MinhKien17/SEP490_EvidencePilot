import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useLanguage } from '../../context/LanguageContext';
import { driver } from 'driver.js';
import 'driver.js/dist/driver.css';
import api from '../../api.js';

const t = {
  en: {
    dashboard: 'Dashboard', users: 'Users', papers: 'Papers', audit: 'Audit Logs',
    infra: 'Infrastructure', notifications: 'Notifications', settings: 'Settings',
    adminPanel: 'Admin Panel', profile: 'Profile', signOut: 'Sign Out',
    totalUsers: 'Total Users', activeProjects: 'Active Projects', activeDocuments: 'Active Documents',
    students: 'Students', instructors: 'Instructors', admins: 'Admins',
    sourceFiles: 'source files', paperDocs: 'paper docs', categories: 'categories',
    collections: 'collections', userAccounts: 'User Accounts', createUser: 'Create User',
    email: 'Email', fullName: 'Full Name', role: 'Role', status: 'Status', actions: 'Actions',
    active: 'Active', banned: 'Banned', ban: 'Ban', activate: 'Activate',
    resetPassword: 'Reset Password', delete: 'Delete', saving: 'Saving...',
    resetSent: 'Reset email sent', resetFailed: 'Reset failed',
    noUsers: 'No users found', noLogs: 'No audit logs found',
    auditLogs: 'Audit Logs', timestamp: 'Timestamp', actor: 'Actor',
    action: 'Action', entity: 'Entity', details: 'Details',
    papersOverview: 'Papers Overview', drafts: 'Drafts', submitted: 'Submitted',
    inReview: 'In Review', published: 'Published', rejected: 'Rejected',
    systemHealth: 'System Health', storage: 'Storage', uptime: 'Uptime',
    services: 'Services', online: 'Online', offline: 'Offline',
    broadcast: 'Broadcast Notification', message: 'Message', send: 'Send',
    targetRole: 'Target Role', all: 'All', sent: 'Sent',
    settings: 'System Settings', appName: 'Application Name', save: 'Save',
    saved: 'Saved', maintenance: 'Maintenance Mode',
    total: 'total', prev: 'Prev', next: 'Next', page: 'Page',
    loadFailed: 'Failed to load data', retry: 'Retry',
    langSwitch: 'Tiếng Việt', copyright: 'Evidence Pilot © 2026. All rights reserved.',
    tourGuide: 'Guide', guide: 'Tour Guide', processGuide: 'Process Guide',
    createNew: 'Create New', filter: 'Filter', close: 'Close',
    firstName: 'First Name', lastName: 'Last Name', cancel: 'Cancel',
    password: 'Password', confirmDelete: 'Delete this user?',
    done: 'Done',
    pipeline: 'Pipeline', documentCount: 'Document Count',
    guideDashDesc: 'Overview of system KPIs at a glance.',
    guideDashUsers: 'Total registered users broken down by role.',
    guideDashProjects: 'Active projects with categories and collections.',
    guideDashDocuments: 'Total active documents including source files and paper docs.',
    guideDashStatus: 'User status breakdown: active vs banned accounts.',
    guideDashInfra: 'Infrastructure service readiness indicators.',
    guideDashDone: 'Dashboard overview complete.',
    guideUsersDesc: 'User management: create, ban, reset password, or delete accounts.',
    guideUsersCreate: 'Click to open the creation form. Fill email, name, password, and role.',
    guideUsersTable: 'Lists all users with email, name, role, status, and action buttons.',
    guideUsersActions: 'Ban/activate, reset password, or delete a user.',
    guideUsersDone: 'Users walkthrough complete.',
    guidePapersDesc: 'Paper pipeline overview. Each card represents a stage.',
    guidePapersFlow: 'Drafts → Submitted → In Review → Published → Rejected. Shows paper flow through the system.',
    guidePapersCount: 'Total paper documents and source files in the system.',
    guidePapersDone: 'Papers overview complete.',
    guideAuditDesc: 'Audit trail of all system activities.',
    guideAuditFilter: 'Filter logs by entity type: USER, PROJECT, CLAIM, DOCUMENT.',
    guideAuditTable: 'Each row shows when, who, what action, which entity, and changed values.',
    guideAuditDone: 'Audit logs walkthrough complete.',
    guideInfraDesc: 'Infrastructure health monitoring.',
    guideInfraServices: 'Each service shows online/offline status. Red indicates attention needed.',
    guideInfraStorage: 'Storage usage bar. Monitor capacity to avoid service disruption.',
    guideInfraDone: 'Infrastructure overview complete.',
    guideNotifDesc: 'Broadcast notifications to users.',
    guideNotifForm: 'Type your message, select target role, and send. All users receive it in real-time.',
    guideNotifHistory: 'Previously sent notifications appear here with timestamp and target role.',
    guideNotifDone: 'Notifications walkthrough complete.',
    guideSettingsDesc: 'System settings management.',
    guideSettingsForm: 'Configure application name and other system preferences.',
    guideSettingsDone: 'Settings walkthrough complete.',
    collapse: 'Collapse',
    projects: 'Projects', projectTitle: 'Title', projectStatus: 'Status',
    createdAt: 'Created', noProjects: 'No projects found', projectDeleted: 'Project deleted',
    guideProjectsDesc: 'View and manage all projects in the system.',
    guideProjectsTable: 'Each row shows project title, status, and creation date. Admins can delete projects.',
    guideProjectsDone: 'Projects walkthrough complete.',
    sourceCategories: 'Source Categories', categoryName: 'Name', categoryDescription: 'Description',
    addCategory: 'Add Category', editCategory: 'Edit Category',
    noCategories: 'No categories', categorySaved: 'Category saved', categoryDeleted: 'Category deleted',
    guideCategoriesDesc: 'Manage source categories used to classify documents.',
    guideCategoriesList: 'List of all categories. Each shows name, description, and active status.',
    guideCategoriesForm: 'Add or edit a category. Name is required, description is optional.',
    guideCategoriesDone: 'Categories walkthrough complete.',
    systemConfig: 'System Configuration', configKey: 'Setting', configValue: 'Value',
    configNote: 'Read-only. Configured via environment variables.',
    guideConfigDesc: 'View current system configuration values.',
    guideConfigTable: 'Each row shows a setting name and its current value. Loaded at startup.',
    guideConfigDone: 'Configuration walkthrough complete.',
    extractionQueue: 'Extraction Queue', extractionStatus: 'Status', queueSummary: 'Queue Summary',
    noFailedDocuments: 'No failed documents', queueRetry: 'Retry',
    guideQueueDesc: 'Monitor document extraction progress and retry failed jobs.',
    guideQueueCards: 'Summary cards show counts per processing status.',
    guideQueueFailed: 'List of failed documents. Click Retry to re-queue.',
    guideQueueDone: 'Extraction queue walkthrough complete.',
    broadcastHistory: 'Broadcast History', recipients: 'Recipients', noBroadcastHistory: 'No broadcast history',
    guideHistoryDesc: 'View past broadcast notifications sent to users.',
    guideHistoryTable: 'Each entry shows message, target role, recipient count, and sent time.',
    guideHistoryDone: 'Broadcast history walkthrough complete.',
    collections: 'Collections', instructor: 'Instructor', sourceCount: 'Sources',
    noCollections: 'No collections found',
    guideCollectionsDesc: 'Browse all instructor evidence collections.',
    guideCollectionsTable: 'List of collections with instructor email and source count.',
    guideCollectionsDone: 'Collections walkthrough complete.',
  },
  vi: {
    dashboard: 'Bảng điều khiển', users: 'Người dùng', papers: 'Bài báo', audit: 'Nhật ký',
    infra: 'Hạ tầng', notifications: 'Thông báo', settings: 'Cài đặt',
    adminPanel: 'Quản trị hệ thống', profile: 'Hồ sơ', signOut: 'Đăng xuất',
    totalUsers: 'Tổng người dùng', activeProjects: 'Dự án đang hoạt động', activeDocuments: 'Tài liệu đang hoạt động',
    students: 'Sinh viên', instructors: 'Giảng viên', admins: 'Quản trị viên',
    sourceFiles: 'tệp nguồn', paperDocs: 'bài báo', categories: 'danh mục',
    collections: 'bộ sưu tập', userAccounts: 'Tài khoản người dùng', createUser: 'Tạo người dùng',
    email: 'Email', fullName: 'Họ tên', role: 'Vai trò', status: 'Trạng thái', actions: 'Thao tác',
    active: 'Hoạt động', banned: 'Bị khóa', ban: 'Khóa', activate: 'Kích hoạt',
    resetPassword: 'Đặt lại mật khẩu', delete: 'Xóa', saving: 'Đang lưu...',
    resetSent: 'Đã gửi email đặt lại', resetFailed: 'Đặt lại thất bại',
    noUsers: 'Không tìm thấy người dùng', noLogs: 'Không có nhật ký',
    auditLogs: 'Nhật ký hệ thống', timestamp: 'Thời gian', actor: 'Người thực hiện',
    action: 'Hành động', entity: 'Đối tượng', details: 'Chi tiết',
    papersOverview: 'Tổng quan bài báo', drafts: 'Bản nháp', submitted: 'Đã gửi',
    inReview: 'Đang đánh giá', published: 'Đã xuất bản', rejected: 'Từ chối',
    systemHealth: 'Sức khỏe hệ thống', storage: 'Lưu trữ', uptime: 'Thời gian hoạt động',
    services: 'Dịch vụ', online: 'Trực tuyến', offline: 'Ngoại tuyến',
    broadcast: 'Gửi thông báo', message: 'Nội dung', send: 'Gửi',
    targetRole: 'Đối tượng', all: 'Tất cả', sent: 'Đã gửi',
    settings: 'Cài đặt hệ thống', appName: 'Tên ứng dụng', save: 'Lưu',
    saved: 'Đã lưu', maintenance: 'Chế độ bảo trì',
    total: 'tổng', prev: 'Trước', next: 'Sau', page: 'Trang',
    loadFailed: 'Tải dữ liệu thất bại', retry: 'Thử lại',
    langSwitch: 'English', copyright: 'Evidence Pilot © 2026. Bảo lưu mọi quyền.',
    tourGuide: 'Hướng dẫn', guide: 'Hướng dẫn sử dụng', processGuide: 'Quy trình',
    createNew: 'Tạo mới', filter: 'Lọc', close: 'Đóng',
    firstName: 'Tên', lastName: 'Họ', cancel: 'Hủy',
    password: 'Mật khẩu', confirmDelete: 'Xóa người dùng này?',
    done: 'Hoàn tất',
    pipeline: 'Quy trình', documentCount: 'Số lượng tài liệu',
    guideDashDesc: 'Tổng quan các chỉ số KPI của hệ thống.',
    guideDashUsers: 'Tổng số người dùng đã đăng ký, phân loại theo vai trò.',
    guideDashProjects: 'Dự án đang hoạt động với danh mục và bộ sưu tập.',
    guideDashDocuments: 'Tổng số tài liệu đang hoạt động bao gồm tệp nguồn và bài báo.',
    guideDashStatus: 'Phân loại trạng thái người dùng: hoạt động và bị khóa.',
    guideDashInfra: 'Chỉ số sẵn sàng của dịch vụ hạ tầng.',
    guideDashDone: 'Đã hoàn thành tổng quan bảng điều khiển.',
    guideUsersDesc: 'Quản lý người dùng: tạo, khóa, đặt lại mật khẩu hoặc xóa tài khoản.',
    guideUsersCreate: 'Nhấp để mở biểu mẫu tạo. Điền email, tên, mật khẩu và vai trò.',
    guideUsersTable: 'Danh sách tất cả người dùng với email, tên, vai trò, trạng thái và thao tác.',
    guideUsersActions: 'Khóa/kích hoạt, đặt lại mật khẩu hoặc xóa người dùng.',
    guideUsersDone: 'Đã hoàn thành hướng dẫn quản lý người dùng.',
    guidePapersDesc: 'Tổng quan quy trình bài báo. Mỗi thẻ đại diện cho một giai đoạn.',
    guidePapersFlow: 'Bản nháp → Đã gửi → Đang đánh giá → Đã xuất bản → Từ chối.',
    guidePapersCount: 'Tổng số bài báo và tệp nguồn trong hệ thống.',
    guidePapersDone: 'Đã hoàn thành tổng quan bài báo.',
    guideAuditDesc: 'Nhật ký kiểm tra tất cả hoạt động hệ thống.',
    guideAuditFilter: 'Lọc nhật ký theo loại đối tượng: USER, PROJECT, CLAIM, DOCUMENT.',
    guideAuditTable: 'Mỗi dòng hiển thị thời gian, ai thực hiện, hành động gì, đối tượng nào và giá trị thay đổi.',
    guideAuditDone: 'Đã hoàn thành hướng dẫn nhật ký kiểm tra.',
    guideInfraDesc: 'Giám sát sức khỏe hạ tầng.',
    guideInfraServices: 'Mỗi dịch vụ hiển thị trạng thái trực tuyến/ngoại tuyến. Màu đỏ cần chú ý.',
    guideInfraStorage: 'Thanh sử dụng bộ nhớ. Theo dõi dung lượng để tránh gián đoạn dịch vụ.',
    guideInfraDone: 'Đã hoàn thành tổng quan hạ tầng.',
    guideNotifDesc: 'Gửi thông báo đến người dùng.',
    guideNotifForm: 'Nhập nội dung, chọn đối tượng và gửi. Người dùng nhận thông báo theo thời gian thực.',
    guideNotifHistory: 'Các thông báo đã gửi hiển thị tại đây với thời gian và đối tượng nhận.',
    guideNotifDone: 'Đã hoàn thành hướng dẫn thông báo.',
    guideSettingsDesc: 'Quản lý cài đặt hệ thống.',
    guideSettingsForm: 'Cấu hình tên ứng dụng và các tùy chọn hệ thống khác.',
    guideSettingsDone: 'Đã hoàn thành hướng dẫn cài đặt.',
    collapse: 'Đóng tab',
    projects: 'Dự án', projectTitle: 'Tiêu đề', projectStatus: 'Trạng thái',
    createdAt: 'Ngày tạo', noProjects: 'Không có dự án', projectDeleted: 'Đã xóa dự án',
    guideProjectsDesc: 'Xem và quản lý tất cả dự án trong hệ thống.',
    guideProjectsTable: 'Mỗi dòng hiển thị tiêu đề, trạng thái và ngày tạo. Quản trị viên có thể xóa dự án.',
    guideProjectsDone: 'Đã hoàn thành hướng dẫn dự án.',
    sourceCategories: 'Danh mục nguồn', categoryName: 'Tên', categoryDescription: 'Mô tả',
    addCategory: 'Thêm danh mục', editCategory: 'Sửa danh mục',
    noCategories: 'Không có danh mục', categorySaved: 'Đã lưu danh mục', categoryDeleted: 'Đã xóa danh mục',
    guideCategoriesDesc: 'Quản lý danh mục nguồn dùng để phân loại tài liệu.',
    guideCategoriesList: 'Danh sách tất cả danh mục. Mỗi danh mục hiển thị tên, mô tả và trạng thái.',
    guideCategoriesForm: 'Thêm hoặc sửa danh mục. Tên là bắt buộc, mô tả không bắt buộc.',
    guideCategoriesDone: 'Đã hoàn thành hướng dẫn danh mục.',
    systemConfig: 'Cấu hình hệ thống', configKey: 'Cài đặt', configValue: 'Giá trị',
    configNote: 'Chỉ đọc. Cấu hình qua biến môi trường.',
    guideConfigDesc: 'Xem các giá trị cấu hình hệ thống hiện tại.',
    guideConfigTable: 'Mỗi dòng hiển thị tên cài đặt và giá trị hiện tại. Được tải khi khởi động.',
    guideConfigDone: 'Đã hoàn thành hướng dẫn cấu hình.',
    extractionQueue: 'Hàng đợi trích xuất', extractionStatus: 'Trạng thái', queueSummary: 'Tóm tắt hàng đợi',
    noFailedDocuments: 'Không có tài liệu thất bại', queueRetry: 'Thử lại',
    guideQueueDesc: 'Theo dõi tiến trình trích xuất tài liệu và thử lại các tác vụ thất bại.',
    guideQueueCards: 'Thẻ tóm tắt hiển thị số lượng theo từng trạng thái xử lý.',
    guideQueueFailed: 'Danh sách tài liệu thất bại. Nhấp Thử lại để xếp hàng lại.',
    guideQueueDone: 'Đã hoàn thành hướng dẫn hàng đợi trích xuất.',
    broadcastHistory: 'Lịch sử thông báo', recipients: 'Người nhận', noBroadcastHistory: 'Không có lịch sử thông báo',
    guideHistoryDesc: 'Xem các thông báo đã gửi trước đây đến người dùng.',
    guideHistoryTable: 'Mỗi mục hiển thị nội dung, đối tượng nhận, số lượng người nhận và thời gian gửi.',
    guideHistoryDone: 'Đã hoàn thành hướng dẫn lịch sử thông báo.',
    collections: 'Bộ sưu tập', instructor: 'Giảng viên', sourceCount: 'Nguồn',
    noCollections: 'Không tìm thấy bộ sưu tập',
    guideCollectionsDesc: 'Xem tất cả bộ sưu tập bằng chứng của giảng viên.',
    guideCollectionsTable: 'Danh sách bộ sưu tập với email giảng viên và số lượng nguồn.',
    guideCollectionsDone: 'Đã hoàn thành hướng dẫn bộ sưu tập.',
  }
};

function PageSkeleton() {
  return (
    <div className="animate-pulse space-y-4 p-6">
      <div className="h-6 bg-gray-200 rounded w-1/3" />
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="h-24 bg-gray-200 rounded-2xl" />
        <div className="h-24 bg-gray-200 rounded-2xl" />
        <div className="h-24 bg-gray-200 rounded-2xl" />
      </div>
      <div className="h-64 bg-gray-200 rounded-2xl" />
    </div>
  );
}

function ErrorBlock({ msg, onRetry }) {
  return (
    <div className="flex items-center justify-between p-4 mx-6 mt-4 bg-rose-50 border border-rose-200 rounded-xl">
      <span className="text-sm font-medium text-rose-700">{msg}</span>
      {onRetry && <button onClick={onRetry} className="text-sm font-bold text-rose-700 underline hover:no-underline">{t.retry}</button>}
    </div>
  );
}

function Pagination({ page, totalPages, totalElements, onPageChange, lang }) {
  if (totalPages <= 1) return null;
  return (
    <div className="flex items-center justify-between px-6 py-3 border-t border-gray-100 bg-gray-50/50">
      <span className="text-xs text-gray-400">{totalElements} {lang.total}</span>
      <div className="flex items-center gap-2">
        <button onClick={() => onPageChange(page - 1)} disabled={page === 0}
          className="px-3 py-1 text-xs font-bold rounded-lg border border-gray-200 bg-white text-gray-600 disabled:opacity-30 disabled:cursor-not-allowed hover:bg-gray-50 transition">{lang.prev}</button>
        <span className="text-xs text-gray-500 font-medium">{lang.page} {page + 1}/{totalPages}</span>
        <button onClick={() => onPageChange(page + 1)} disabled={page >= totalPages - 1}
          className="px-3 py-1 text-xs font-bold rounded-lg border border-gray-200 bg-white text-gray-600 disabled:opacity-30 disabled:cursor-not-allowed hover:bg-gray-50 transition">{lang.next}</button>
      </div>
    </div>
  );
}

function StatCard({ label, value, sub, color }) {
  return (
    <div className="bg-white p-5 rounded-2xl shadow-sm border border-gray-100 flex flex-col justify-between min-h-[100px]">
      <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">{label}</span>
      <div className={`text-3xl font-black mt-1 ${color || 'text-[#1e3a8a]'}`}>{value}</div>
      {sub && <div className="mt-1 flex gap-3 text-[10px] text-gray-500 font-medium">{sub}</div>}
    </div>
  );
}

/* ----- SECTIONS ----- */

function DashboardSection({ lang, api }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [guideActive, setGuideActive] = useState(false);

  const fetch = useCallback(async (signal) => {
    setLoading(true); setError(null);
    try { const r = await api.get('/api/admin/dashboard', { signal }); setData(r.data); }
    catch (e) { if (e.name !== 'CanceledError') setError(e.message || lang.loadFailed); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { const ac = new AbortController(); fetch(ac.signal); return () => ac.abort(); }, [fetch]);

  const display = guideActive && (!data || (data.totalUsers === 0 && data.activeProjects === 0))
    ? { totalUsers: 150, usersByRole: { STUDENT: 120, INSTRUCTOR: 25, ADMIN: 5 }, usersByStatus: { ACTIVE: 140, BANNED: 10 }, activeProjects: 8, activeSourceCategories: 12, activeCollections: 30, activeSourceDocuments: 200, activePaperDocuments: 45, infrastructureReadiness: { database: true, storage: true, cache: true, aiService: false } }
    : data;

  const startProcessGuide = () => {
    const isEmpty = !data || (data.totalUsers === 0 && data.activeProjects === 0);
    if (isEmpty) setGuideActive(true);
    let si = -1;
    setTimeout(() => {
      const d = driver({
        animate: true, showProgress: true,
        steps: [
          { popover: { title: lang.processGuide, description: lang.guideDashDesc, side: 'center' } },
          { element: '[data-guide="stat-totalUsers"]', popover: { title: lang.totalUsers, description: lang.guideDashUsers, side: 'bottom' } },
          { element: '[data-guide="stat-projects"]', popover: { title: lang.activeProjects, description: lang.guideDashProjects, side: 'bottom' } },
          { element: '[data-guide="stat-documents"]', popover: { title: lang.activeDocuments, description: lang.guideDashDocuments, side: 'bottom' } },
          { element: '[data-guide="dash-status"]', popover: { title: lang.status, description: lang.guideDashStatus, side: 'top' } },
          { element: '[data-guide="dash-infra"]', popover: { title: lang.systemHealth, description: lang.guideDashInfra, side: 'top' } },
          { popover: { title: lang.done, description: lang.guideDashDone, side: 'center' } },
        ],
        onDestroy: () => setGuideActive(false),
      });
      d.drive();
    }, 300);
  };

  if (loading) return <PageSkeleton />;
  if (error && !guideActive) return <ErrorBlock msg={error} onRetry={() => fetch(new AbortController().signal)} />;
  if (!display) return <div className="p-6 text-gray-400 text-center">{lang.loadFailed}</div>;

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between mb-2">
        <h2 className="text-lg font-bold text-gray-900">{lang.dashboard}</h2>
        <button onClick={startProcessGuide} className="px-2.5 py-1.5 text-xs font-bold text-gray-500 bg-gray-50 border border-gray-200 rounded-lg hover:bg-gray-100 transition">
          {'\u2753'} {lang.processGuide}
        </button>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div data-guide="stat-totalUsers"><StatCard label={lang.totalUsers} value={display.totalUsers}
          sub={<>{display.usersByRole?.STUDENT || 0} {lang.students} &middot; {display.usersByRole?.INSTRUCTOR || 0} {lang.instructors} &middot; {display.usersByRole?.ADMIN || 0} {lang.admins}</>} /></div>
        <div data-guide="stat-projects"><StatCard label={lang.activeProjects} value={display.activeProjects} color="text-indigo-600"
          sub={<>{display.activeSourceCategories} {lang.categories} &middot; {display.activeCollections} {lang.collections}</>} /></div>
        <div data-guide="stat-documents"><StatCard label={lang.activeDocuments} value={(display.activeSourceDocuments || 0) + (display.activePaperDocuments || 0)} color="text-amber-600"
          sub={<>{display.activeSourceDocuments || 0} {lang.sourceFiles} &middot; {display.activePaperDocuments || 0} {lang.paperDocs}</>} /></div>
      </div>
      {display.usersByStatus && (
        <div data-guide="dash-status" className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5">
          <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block mb-3">{lang.status}</span>
          <div className="flex gap-6">
            <span className="text-sm"><span className="font-bold text-emerald-600">{display.usersByStatus.ACTIVE || 0}</span> <span className="text-gray-400">{lang.active}</span></span>
            <span className="text-sm"><span className="font-bold text-rose-600">{display.usersByStatus.BANNED || 0}</span> <span className="text-gray-400">{lang.banned}</span></span>
          </div>
        </div>
      )}
      {display.infrastructureReadiness && Object.keys(display.infrastructureReadiness).length > 0 && (
        <div data-guide="dash-infra" className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5">
          <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block mb-3">{lang.systemHealth}</span>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            {Object.entries(display.infrastructureReadiness).map(([k, v]) => (
              <div key={k} className="flex items-center gap-2 text-sm">
                <span className={`w-2 h-2 rounded-full ${v ? 'bg-emerald-400' : 'bg-rose-400'}`} />
                <span className="text-gray-600 capitalize">{k.replace(/([A-Z])/g, ' $1')}</span>
                <span className={`font-bold ${v ? 'text-emerald-600' : 'text-rose-600'}`}>{v ? lang.online : lang.offline}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

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
  const [guideActive, setGuideActive] = useState(false);

  const MOCK_GUIDE_USERS = [
    { id: 'guide-demo-1', email: 'nguyen.van.a@demo.edu.vn', firstName: 'Nguyen', lastName: 'Van A', role: 'STUDENT', accountStatus: 'ACTIVE' },
    { id: 'guide-demo-2', email: 'le.thi.c@demo.edu.vn', firstName: 'Le', lastName: 'Thi C', role: 'INSTRUCTOR', accountStatus: 'ACTIVE' },
    { id: 'guide-demo-3', email: 'tran.van.b@demo.edu.vn', firstName: 'Tran', lastName: 'Van B', role: 'STUDENT', accountStatus: 'BANNED' },
  ];

  const displayUsers = guideActive
    ? { ...users, content: [...MOCK_GUIDE_USERS, ...users.content] }
    : users;

  const fetch = useCallback(async (p, signal) => {
    setLoading(true); setError(null);
    try { const r = await api.get('/api/admin/users', { params: { page: p, size: 20 }, signal }); setUsers(r.data); }
    catch (e) { if (e.name !== 'CanceledError') setError(e.message || lang.loadFailed); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { const ac = new AbortController(); fetch(page, ac.signal); return () => ac.abort(); }, [fetch, page]);

  const startProcessGuide = () => {
    const hasData = users.content.length > 0;
    if (!hasData) setGuideActive(true);
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
        onDestroy: () => { setGuideActive(false); },
      });
      d.drive();
    }, 300);
  };

  const toggleStatus = async (u) => {
    const ns = u.accountStatus === 'ACTIVE' ? 'BANNED' : 'ACTIVE';
    setLoadingAction(p => ({ ...p, [u.id]: true }));
    try { await api.patch(`/api/admin/users/${u.id}/status`, { status: ns }); setUsers(prev => ({ ...prev, content: prev.content.map(x => x.id === u.id ? { ...x, accountStatus: ns } : x) })); }
    catch (e) { setError(e.message); }
    finally { setLoadingAction(p => ({ ...p, [u.id]: false })); }
  };

  const doResetPw = async (u) => {
    setLoadingAction(p => ({ ...p, ['pw_' + u.id]: true }));
    try { await api.post(`/api/admin/users/${u.id}/password-reset`); setPwMsg(p => ({ ...p, [u.id]: { ok: true, msg: lang.resetSent } })); }
    catch (e) { setPwMsg(p => ({ ...p, [u.id]: { ok: false, msg: lang.resetFailed } })); }
    finally { setLoadingAction(p => ({ ...p, ['pw_' + u.id]: false })); setTimeout(() => setPwMsg(p => { const n = { ...p }; delete n[u.id]; return n; }), 3000); }
  };

  const doDelete = async (id) => {
    if (!confirm(lang.confirmDelete)) return;
    setLoadingAction(p => ({ ...p, ['del_' + id]: true }));
    try { await api.delete(`/api/admin/users/${id}`); setUsers(prev => ({ ...prev, content: prev.content.filter(x => x.id !== id) })); }
    catch (e) { setError(e.message); }
    finally { setLoadingAction(p => ({ ...p, ['del_' + id]: false })); }
  };

  const doCreate = async (e) => {
    e.preventDefault(); setCreateErr('');
    try { await api.post('/api/admin/users', createForm); setShowCreate(false); setCreateForm({ email: '', firstName: '', lastName: '', password: '', role: 'STUDENT' }); fetch(0); }
    catch (err) { setCreateErr(err.response?.data?.message || err.message); }
  };

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-bold text-gray-900">{lang.userAccounts}</h2>
        <div className="flex items-center gap-2">
          <button onClick={startProcessGuide} className="px-2.5 py-1.5 text-xs font-bold text-gray-500 bg-gray-50 border border-gray-200 rounded-lg hover:bg-gray-100 transition">
            {'\u2753'} {lang.processGuide}
          </button>
          <button data-guide="create-btn" onClick={() => setShowCreate(true)} className="px-3 py-1.5 text-xs font-bold bg-[#1e3a8a] text-white rounded-lg hover:bg-[#1e40af] transition">{lang.createUser}</button>
        </div>
      </div>
      {guideActive && (
        <div className="mb-3 px-3 py-2 bg-amber-50 border border-amber-200 rounded-lg text-xs text-amber-700 font-medium">
          {'\uD83D\uDCD7'} {lang.processGuide}: {lang.users?.toLowerCase?.() || 'users'} {lang.guide?.toLowerCase?.() || 'guide'}
        </div>
      )}
      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30" onClick={() => setShowCreate(false)}>
          <div className="bg-white rounded-2xl shadow-xl p-6 w-full max-w-md mx-4" onClick={e => e.stopPropagation()}>
            <h3 className="font-bold text-gray-900 mb-4">{lang.createUser}</h3>
            <form onSubmit={doCreate} className="space-y-3">
              <input name="email" placeholder={lang.email} value={createForm.email} onChange={e => setCreateForm(p => ({ ...p, email: e.target.value }))} required className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm" />
              <div className="flex gap-2">
                <input name="firstName" placeholder={lang.firstName} value={createForm.firstName} onChange={e => setCreateForm(p => ({ ...p, firstName: e.target.value }))} required className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm" />
                <input name="lastName" placeholder={lang.lastName} value={createForm.lastName} onChange={e => setCreateForm(p => ({ ...p, lastName: e.target.value }))} required className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm" />
              </div>
              <input name="password" type="password" placeholder={lang.password} value={createForm.password} onChange={e => setCreateForm(p => ({ ...p, password: e.target.value }))} required className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm" />
              <select value={createForm.role} onChange={e => setCreateForm(p => ({ ...p, role: e.target.value }))} className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm">
                <option value="STUDENT">{lang.students}</option>
                <option value="INSTRUCTOR">{lang.instructors}</option>
              </select>
              {createErr && <div className="text-xs text-rose-600 bg-rose-50 p-2 rounded">{createErr}</div>}
              <div className="flex gap-2 justify-end">
                <button type="button" onClick={() => setShowCreate(false)} className="px-3 py-1.5 text-xs font-bold text-gray-600 border border-gray-200 rounded-lg hover:bg-gray-50">{lang.cancel}</button>
                <button type="submit" className="px-3 py-1.5 text-xs font-bold bg-[#1e3a8a] text-white rounded-lg hover:bg-[#1e40af]">{lang.createUser}</button>
              </div>
            </form>
          </div>
        </div>
      )}
      {error && <ErrorBlock msg={error} onRetry={() => fetch(page, new AbortController().signal)} />}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table data-guide="table" className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50 text-gray-400 text-xs font-bold uppercase border-b border-gray-100">
                <th className="px-5 py-3">{lang.email}</th>
                <th className="px-5 py-3">{lang.fullName}</th>
                <th className="px-5 py-3">{lang.role}</th>
                <th className="px-5 py-3">{lang.status}</th>
                <th className="px-5 py-3 text-right">{lang.actions}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-xs">
              {loading && !guideActive ? Array.from({ length: 5 }).map((_, i) => (
                <tr key={i} className="animate-pulse">{Array.from({ length: 5 }).map((_, j) => (
                  <td key={j} className="px-5 py-4"><div className="h-4 bg-gray-200 rounded w-full" /></td>
                ))}</tr>
              )) : displayUsers.content.length === 0 ? (
                <tr><td colSpan={5} className="px-5 py-10 text-center text-gray-400">{lang.noUsers}</td></tr>
              ) : displayUsers.content.map(u => (
                <tr key={u.id} className={`hover:bg-gray-50/50 transition ${u.id.startsWith('guide-') ? 'bg-amber-50/50' : ''}`}>
                  <td className="px-5 py-3 font-mono text-gray-600">{u.email}</td>
                  <td className="px-5 py-3 font-semibold text-gray-900">{u.firstName} {u.lastName}</td>
                  <td className="px-5 py-3">
                    <span className={`px-2 py-0.5 rounded-full text-[10px] font-medium ${u.role === 'ADMIN' ? 'bg-rose-50 text-rose-700' : u.role === 'INSTRUCTOR' ? 'bg-purple-50 text-purple-700' : 'bg-blue-50 text-[#1e3a8a]'}`}>{u.role}</span>
                  </td>
                  <td className="px-5 py-3">
                    <span className={`px-2 py-0.5 rounded font-bold text-[10px] ${u.accountStatus === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'}`}>{u.accountStatus}</span>
                  </td>
                  <td className="px-5 py-3 text-right space-x-1.5">
                    {pwMsg[u.id] ? (
                      <span className={`inline-block px-2 py-1 text-[10px] font-bold rounded ${pwMsg[u.id].ok ? 'text-emerald-700 bg-emerald-50' : 'text-rose-700 bg-rose-50'}`}>{pwMsg[u.id].msg}</span>
                    ) : (
                      <button data-guide="action-reset" onClick={() => doResetPw(u)} disabled={loadingAction['pw_' + u.id]} className="px-2 py-1 text-gray-600 bg-gray-50 border border-gray-200 rounded-lg hover:bg-gray-100 transition disabled:opacity-50">{loadingAction['pw_' + u.id] ? '...' : lang.resetPassword}</button>
                    )}
                    <button data-guide="action-ban" onClick={() => toggleStatus(u)} disabled={loadingAction[u.id]}
                      className={`px-2 py-1 rounded-lg border font-bold transition disabled:opacity-50 ${u.accountStatus === 'ACTIVE' ? 'bg-rose-50 border-rose-200 text-rose-700 hover:bg-rose-100' : 'bg-emerald-50 border-emerald-200 text-emerald-700 hover:bg-emerald-100'}`}>
                      {loadingAction[u.id] ? '...' : u.accountStatus === 'ACTIVE' ? lang.ban : lang.activate}
                    </button>
                    <button data-guide="action-delete" onClick={() => doDelete(u.id)} disabled={loadingAction['del_' + u.id]}
                      className="px-2 py-1 text-gray-400 bg-gray-50 border border-gray-200 rounded-lg hover:bg-gray-100 hover:text-rose-600 transition disabled:opacity-50">
                      {loadingAction['del_' + u.id] ? '...' : lang.delete}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {!loading && !guideActive && <Pagination page={users.page} totalPages={users.totalPages} totalElements={users.totalElements} onPageChange={setPage} lang={lang} />}
      </div>
    </div>
  );
}

function ProjectsSection({ lang, api }) {
  const [projects, setProjects] = useState({ content: [], page: 0, totalElements: 0, totalPages: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [filterStatus, setFilterStatus] = useState('');
  const [guideActive, setGuideActive] = useState(false);

  const fetch = useCallback(async (p, signal) => {
    setLoading(true); setError(null);
    try {
      const params = { page: p, size: 20 };
      if (filterStatus) params.status = filterStatus;
      const r = await api.get('/api/projects', { params, signal }); setProjects(r.data);
    } catch (e) { if (e.name !== 'CanceledError') setError(e.message || lang.loadFailed); }
    finally { setLoading(false); }
  }, [filterStatus]);

  useEffect(() => { const ac = new AbortController(); fetch(page, ac.signal); return () => ac.abort(); }, [fetch, page]);

  const doDelete = async (id) => {
    if (!confirm(lang.confirmDelete)) return;
    try { await api.delete(`/api/projects/${id}`); setProjects(prev => ({ ...prev, content: prev.content.filter(x => x.id !== id), totalElements: prev.totalElements - 1 })); }
    catch (e) { setError(e.message); }
  };

  const startProcessGuide = () => {
    setGuideActive(true);
    setTimeout(() => {
      const d = driver({
        animate: true, showProgress: true,
        steps: [
          { popover: { title: lang.processGuide, description: lang.guideProjectsDesc, side: 'center' } },
          { element: '[data-guide="projects-table"]', popover: { title: lang.projects, description: lang.guideProjectsTable, side: 'left' } },
          { popover: { title: lang.done, description: lang.guideProjectsDone, side: 'center' } },
        ],
        onDestroy: () => setGuideActive(false),
      });
      d.drive();
    }, 300);
  };

  const statusColors = { ACTIVE: 'bg-emerald-50 text-emerald-700', IN_REVIEW: 'bg-amber-50 text-amber-700', DELETED: 'bg-gray-100 text-gray-500' };

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-bold text-gray-900">{lang.projects}</h2>
        <div className="flex items-center gap-2">
          <button onClick={startProcessGuide} className="px-2.5 py-1.5 text-xs font-bold text-gray-500 bg-gray-50 border border-gray-200 rounded-lg hover:bg-gray-100 transition">
            {'\u2753'} {lang.processGuide}
          </button>
          <select value={filterStatus} onChange={e => { setFilterStatus(e.target.value); setPage(0); }}
            className="text-xs border border-gray-200 rounded-lg px-2 py-1.5 bg-white text-gray-600">
            <option value="">{lang.all}</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="IN_REVIEW">IN_REVIEW</option>
            <option value="DELETED">DELETED</option>
          </select>
        </div>
      </div>
      {error && <ErrorBlock msg={error} onRetry={() => fetch(page, new AbortController().signal)} />}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table data-guide="projects-table" className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50 text-gray-400 text-xs font-bold uppercase border-b border-gray-100">
                <th className="px-5 py-3">{lang.projectTitle}</th>
                <th className="px-5 py-3">{lang.projectStatus}</th>
                <th className="px-5 py-3">{lang.createdAt}</th>
                <th className="px-5 py-3 text-right">{lang.actions}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-xs">
              {loading ? Array.from({ length: 5 }).map((_, i) => (
                <tr key={i} className="animate-pulse">{Array.from({ length: 4 }).map((_, j) => (
                  <td key={j} className="px-5 py-4"><div className="h-4 bg-gray-200 rounded w-full" /></td>
                ))}</tr>
              )) : projects.content.length === 0 ? (
                <tr><td colSpan={4} className="px-5 py-10 text-center text-gray-400">{lang.noProjects}</td></tr>
              ) : projects.content.map(p => (
                <tr key={p.id} className="hover:bg-gray-50/50 transition">
                  <td className="px-5 py-3 font-semibold text-gray-900">{p.title}</td>
                  <td className="px-5 py-3"><span className={`px-2 py-0.5 rounded-full text-[10px] font-medium ${statusColors[p.status] || 'bg-gray-50 text-gray-600'}`}>{p.status}</span></td>
                  <td className="px-5 py-3 text-gray-500">{new Date(p.createdAt).toLocaleDateString()}</td>
                  <td className="px-5 py-3 text-right">
                    <button onClick={() => doDelete(p.id)} className="px-2 py-1 text-gray-400 bg-gray-50 border border-gray-200 rounded-lg hover:bg-rose-50 hover:text-rose-600 transition">{lang.delete}</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {!loading && <Pagination page={projects.page} totalPages={projects.totalPages} totalElements={projects.totalElements} onPageChange={setPage} lang={lang} />}
      </div>
    </div>
  );
}

function PapersSection({ lang, api }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [guideActive, setGuideActive] = useState(false);

  const fetch = useCallback(async (signal) => {
    setLoading(true);
    try { const r = await api.get('/api/admin/dashboard', { signal }); setData(r.data); }
    catch (e) { /* silent */ }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { const ac = new AbortController(); fetch(ac.signal); return () => ac.abort(); }, [fetch]);

  const hasPapers = data && (data.activePaperDocuments > 0 || data.activeSourceDocuments > 0);
  const mockPapers = { activePaperDocuments: 45, activeSourceDocuments: 200 };
  const display = guideActive && !hasPapers ? mockPapers : data;

  const startProcessGuide = () => {
    if (!hasPapers) setGuideActive(true);
    setTimeout(() => {
      const d = driver({
        animate: true, showProgress: true,
        steps: [
          { popover: { title: lang.processGuide, description: lang.guidePapersDesc, side: 'center' } },
          { popover: { title: lang.pipeline, description: lang.guidePapersFlow, side: 'center' } },
          { popover: { title: lang.documentCount, description: lang.guidePapersCount, side: 'center' } },
          { popover: { title: lang.done, description: lang.guidePapersDone, side: 'center' } },
        ],
        onDestroy: () => setGuideActive(false),
      });
      d.drive();
    }, 300);
  };

  if (loading) return <PageSkeleton />;
  return (
    <div className="p-6 space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-gray-900">{lang.papersOverview}</h2>
        <button onClick={startProcessGuide} className="px-2.5 py-1.5 text-xs font-bold text-gray-500 bg-gray-50 border border-gray-200 rounded-lg hover:bg-gray-100 transition">
          {'\u2753'} {lang.processGuide}
        </button>
      </div>
      <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
        <StatCard label={lang.drafts} value={display && guideActive ? '12' : '\u2014'} sub={<>{display?.activePaperDocuments || 0} {lang.paperDocs}</>} color="text-gray-500" />
        <StatCard label={lang.submitted} value={display && guideActive ? '8' : '\u2014'} sub={<>{display?.activePaperDocuments || 0} {lang.paperDocs}</>} color="text-blue-600" />
        <StatCard label={lang.inReview} value={display && guideActive ? '15' : '\u2014'} sub={<>{display?.activePaperDocuments || 0} {lang.paperDocs}</>} color="text-amber-600" />
        <StatCard label={lang.published} value={display && guideActive ? '7' : '\u2014'} sub={<>{display?.activePaperDocuments || 0} {lang.paperDocs}</>} color="text-emerald-600" />
        <StatCard label={lang.rejected} value={display && guideActive ? '3' : '\u2014'} sub={<>{display?.activePaperDocuments || 0} {lang.paperDocs}</>} color="text-rose-600" />
      </div>
      <div data-guide="papers-count" className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5 text-sm text-gray-500">
        {lang.activeDocuments}: <span className="font-bold text-gray-900">{display ? (display.activePaperDocuments || 0) : '\u2014'}</span>
        &nbsp;&middot;&nbsp;{lang.sourceFiles}: <span className="font-bold text-gray-900">{display ? (display.activeSourceDocuments || 0) : '\u2014'}</span>
      </div>
    </div>
  );
}

function AuditLogsSection({ lang, api }) {
  const [logs, setLogs] = useState({ content: [], page: 0, totalElements: 0, totalPages: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [filterEntity, setFilterEntity] = useState('');
  const [guideActive, setGuideActive] = useState(false);

  const MOCK_LOGS = [
    { actorId: 'mock-1', actorEmail: 'admin@fpt.edu.vn', action: 'LOGIN', entityType: 'USER', entityId: 'usr-001', oldValue: null, newValue: 'Session started', occurredAt: new Date().toISOString() },
    { actorId: 'mock-2', actorEmail: 'instructor@fpt.edu.vn', action: 'UPDATE', entityType: 'PROJECT', entityId: 'proj-101', oldValue: 'DRAFT', newValue: 'SUBMITTED', occurredAt: new Date(Date.now() - 3600000).toISOString() },
  ];

  const displayLogs = guideActive && logs.content.length === 0
    ? { ...logs, content: MOCK_LOGS, totalElements: 2, totalPages: 1 }
    : logs;

  const fetch = useCallback(async (p, signal) => {
    setLoading(true); setError(null);
    try {
      const params = { page: p, size: 20 };
      if (filterEntity) params.entityType = filterEntity;
      const r = await api.get('/api/admin/audit-logs', { params, signal }); setLogs(r.data);
    } catch (e) { if (e.name !== 'CanceledError') setError(e.message || lang.loadFailed); }
    finally { setLoading(false); }
  }, [filterEntity]);

  useEffect(() => { const ac = new AbortController(); fetch(page, ac.signal); return () => ac.abort(); }, [fetch, page]);

  const startProcessGuide = () => {
    if (logs.content.length === 0) setGuideActive(true);
    setTimeout(() => {
      const d = driver({
        animate: true, showProgress: true,
        steps: [
          { popover: { title: lang.processGuide, description: lang.guideAuditDesc, side: 'center' } },
          { element: '[data-guide="logs-filter"]', popover: { title: lang.filter, description: lang.guideAuditFilter, side: 'bottom' } },
          { element: '[data-guide="logs-table"]', popover: { title: lang.auditLogs, description: lang.guideAuditTable, side: 'left' } },
          { popover: { title: lang.done, description: lang.guideAuditDone, side: 'center' } },
        ],
        onDestroy: () => setGuideActive(false),
      });
      d.drive();
    }, 300);
  };

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-bold text-gray-900">{lang.auditLogs}</h2>
        <div className="flex items-center gap-2">
          <button onClick={startProcessGuide} className="px-2.5 py-1.5 text-xs font-bold text-gray-500 bg-gray-50 border border-gray-200 rounded-lg hover:bg-gray-100 transition">
            {'\u2753'} {lang.processGuide}
          </button>
          <select data-guide="logs-filter" value={filterEntity} onChange={e => { setFilterEntity(e.target.value); setPage(0); }}
            className="text-xs border border-gray-200 rounded-lg px-2 py-1.5 bg-white text-gray-600">
            <option value="">{lang.all}</option>
            <option value="USER">USER</option>
            <option value="PROJECT">PROJECT</option>
            <option value="CLAIM">CLAIM</option>
            <option value="DOCUMENT">DOCUMENT</option>
          </select>
        </div>
      </div>
      {error && <ErrorBlock msg={error} onRetry={() => fetch(page, new AbortController().signal)} />}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table data-guide="logs-table" className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50 text-gray-400 text-xs font-bold uppercase border-b border-gray-100">
                <th className="px-5 py-3">{lang.timestamp}</th>
                <th className="px-5 py-3">{lang.actor}</th>
                <th className="px-5 py-3">{lang.action}</th>
                <th className="px-5 py-3">{lang.entity}</th>
                <th className="px-5 py-3">{lang.details}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-xs">
              {loading && !guideActive ? Array.from({ length: 5 }).map((_, i) => (
                <tr key={i} className="animate-pulse">{Array.from({ length: 5 }).map((_, j) => (
                  <td key={j} className="px-5 py-4"><div className="h-4 bg-gray-200 rounded w-full" /></td>
                ))}</tr>
              )) : displayLogs.content.length === 0 ? (
                <tr><td colSpan={5} className="px-5 py-10 text-center text-gray-400">{lang.noLogs}</td></tr>
              ) : displayLogs.content.map((log, i) => (
                <tr key={log.actorId + log.occurredAt + i} className="hover:bg-gray-50/50 transition">
                  <td className="px-5 py-3 text-gray-500 font-mono">{new Date(log.occurredAt).toLocaleString()}</td>
                  <td className="px-5 py-3 font-semibold text-gray-900">{log.actorEmail}</td>
                  <td className="px-5 py-3"><span className="px-2 py-0.5 rounded-full text-[10px] font-medium bg-purple-50 text-purple-700">{log.action}</span></td>
                  <td className="px-5 py-3 text-gray-600 font-mono">{log.entityType}#{log.entityId?.slice(0, 8)}</td>
                  <td className="px-5 py-3 text-gray-500 max-w-[180px] truncate" title={log.oldValue || log.newValue}>{log.oldValue ? `${log.oldValue} \u2192 ` : ''}{log.newValue}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {!loading && !guideActive && <Pagination page={logs.page} totalPages={logs.totalPages} totalElements={logs.totalElements} onPageChange={setPage} lang={lang} />}
      </div>
    </div>
  );
}

function InfraSection({ lang, api }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [guideActive, setGuideActive] = useState(false);

  const fetch = useCallback(async (signal) => {
    setLoading(true);
    try { const r = await api.get('/api/admin/dashboard', { signal }); setData(r.data); }
    catch (e) { /* silent */ }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { const ac = new AbortController(); fetch(ac.signal); return () => ac.abort(); }, [fetch]);

  const ir = data?.infrastructureReadiveness || data?.infrastructureReadiness || {};
  const hasReal = Object.keys(ir).length > 0;
  const mockIr = { database: true, storage: true, cache: true, queue: false, aiService: true };
  const displayIr = guideActive && !hasReal ? mockIr : ir;

  const startProcessGuide = () => {
    if (!hasReal) setGuideActive(true);
    setTimeout(() => {
      const d = driver({
        animate: true, showProgress: true,
        steps: [
          { popover: { title: lang.processGuide, description: lang.guideInfraDesc, side: 'center' } },
          { element: '[data-guide="infra-services"]', popover: { title: lang.services, description: lang.guideInfraServices, side: 'right' } },
          { element: '[data-guide="infra-storage"]', popover: { title: lang.storage, description: lang.guideInfraStorage, side: 'left' } },
          { popover: { title: lang.done, description: lang.guideInfraDone, side: 'center' } },
        ],
        onDestroy: () => setGuideActive(false),
      });
      d.drive();
    }, 300);
  };

  if (loading) return <PageSkeleton />;
  return (
    <div className="p-6 space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-gray-900">{lang.systemHealth}</h2>
        <button onClick={startProcessGuide} className="px-2.5 py-1.5 text-xs font-bold text-gray-500 bg-gray-50 border border-gray-200 rounded-lg hover:bg-gray-100 transition">
          {'\u2753'} {lang.processGuide}
        </button>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div data-guide="infra-services" className="bg-white p-5 rounded-2xl shadow-sm border border-gray-100">
          <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block mb-3">{lang.services}</span>
          <div className="space-y-2">
            {Object.keys(displayIr).length === 0 ? (
              <span className="text-sm text-gray-400">—</span>
            ) : Object.entries(displayIr).map(([k, v]) => (
              <div key={k} className="flex items-center justify-between text-sm">
                <span className="text-gray-600 capitalize">{k.replace(/([A-Z])/g, ' $1')}</span>
                <span className={`flex items-center gap-1.5 font-bold ${v ? 'text-emerald-600' : 'text-rose-600'}`}>
                  <span className={`w-2 h-2 rounded-full ${v ? 'bg-emerald-400' : 'bg-rose-400'}`} />
                  {v ? lang.online : lang.offline}
                </span>
              </div>
            ))}
          </div>
        </div>
        <div data-guide="infra-storage" className="bg-white p-5 rounded-2xl shadow-sm border border-gray-100">
          <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block mb-3">{lang.storage}</span>
          <div className="text-sm text-gray-500">{lang.storage} <span className="font-bold text-gray-900">—</span></div>
          <div className="mt-2 w-full bg-gray-100 h-2 rounded-full overflow-hidden">
            <div className="bg-[#1e3a8a] h-full rounded-full" style={{ width: guideActive ? '42%' : '0%' }} />
          </div>
        </div>
      </div>
    </div>
  );
}

function QueueSection({ lang, api }) {
  const [queue, setQueue] = useState(null);
  const [loading, setLoading] = useState(true);
  const [guideActive, setGuideActive] = useState(false);

  const fetch = useCallback(async (signal) => {
    setLoading(true);
    try { const r = await api.get('/api/admin/documents/extraction-queue', { signal }); setQueue(r.data); }
    catch (e) { /* silent */ }
    finally { setLoading(false); }
  }, [api]);

  useEffect(() => { const ac = new AbortController(); fetch(ac.signal); return () => ac.abort(); }, [fetch]);

  const doRetry = async (id) => {
    try { await api.post(`/api/documents/${id}/file`, new FormData()); fetch(new AbortController().signal); }
    catch (e) { /* silent */ }
  };

  const startProcessGuide = () => {
    setGuideActive(true);
    setTimeout(() => {
      const d = driver({
        animate: true, showProgress: true,
        steps: [
          { popover: { title: lang.processGuide, description: lang.guideQueueDesc, side: 'center' } },
          { element: '[data-guide="queue-cards"]', popover: { title: lang.queueSummary, description: lang.guideQueueCards, side: 'bottom' } },
          { element: '[data-guide="queue-failed"]', popover: { title: lang.extractionQueue, description: lang.guideQueueFailed, side: 'left' } },
          { popover: { title: lang.done, description: lang.guideQueueDone, side: 'center' } },
        ],
        onDestroy: () => setGuideActive(false),
      });
      d.drive();
    }, 300);
  };

  const statusColors = { FAILED: 'bg-rose-100 text-rose-700', PROCESSING: 'bg-amber-100 text-amber-700', QUEUED: 'bg-blue-100 text-blue-700', READY: 'bg-emerald-100 text-emerald-700' };

  if (loading) return <PageSkeleton />;
  return (
    <div className="p-6 space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-gray-900">{lang.extractionQueue}</h2>
        <button onClick={startProcessGuide} className="px-2.5 py-1.5 text-xs font-bold text-gray-500 bg-gray-50 border border-gray-200 rounded-lg hover:bg-gray-100 transition">
          {'\u2753'} {lang.processGuide}
        </button>
      </div>
      <div data-guide="queue-cards" className="grid grid-cols-3 md:grid-cols-5 gap-3">
        {queue?.counts && Object.entries(queue.counts).filter(([, v]) => v > 0).map(([k, v]) => (
          <StatCard key={k} label={k} value={v} color={statusColors[k]?.split(' ')[1] || 'text-gray-500'} sub={<span className={`px-2 py-0.5 rounded-full text-[10px] font-medium ${statusColors[k] || 'bg-gray-50 text-gray-500'}`}>{k}</span>} />
        ))}
      </div>
      <div data-guide="queue-failed" className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="px-5 py-3 border-b border-gray-100">
          <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">{lang.extractionStatus}: FAILED</span>
        </div>
        {!queue?.failed || queue.failed.length === 0 ? (
          <div className="text-sm text-gray-400 text-center py-8">{lang.noFailedDocuments}</div>
        ) : (
          <div className="divide-y divide-gray-100 text-xs">
            {queue.failed.map(d => (
              <div key={d.id} className="flex items-center justify-between px-5 py-3">
                <div className="truncate max-w-[300px]">
                  <span className="font-semibold text-gray-900">{d.originalFilename || '—'}</span>
                  {d.processingError && <span className="text-gray-400 ml-2" title={d.processingError}>— {d.processingError}</span>}
                </div>
                <button onClick={() => doRetry(d.id)} className="px-2 py-1 text-xs font-bold text-rose-600 bg-rose-50 border border-rose-200 rounded-lg hover:bg-rose-100 transition shrink-0">{lang.queueRetry}</button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function NotificationsSection({ lang, api }) {
  const [form, setForm] = useState({ message: '', role: '' });
  const [sending, setSending] = useState(false);
  const [result, setResult] = useState(null);
  const [history, setHistory] = useState([]);
  const [guideActive, setGuideActive] = useState(false);
  const [broadcastHistory, setBroadcastHistory] = useState([]);
  const [bhLoading, setBhLoading] = useState(true);

  const fetchHistory = useCallback(async (signal) => {
    setBhLoading(true);
    try { const r = await api.get('/api/admin/notifications/broadcast-history', { signal }); setBroadcastHistory(r.data); }
    catch (e) { /* silent */ }
    finally { setBhLoading(false); }
  }, [api]);

  useEffect(() => { const ac = new AbortController(); fetchHistory(ac.signal); return () => ac.abort(); }, [fetchHistory]);

  const doSend = async (e) => {
    e.preventDefault(); setSending(true); setResult(null);
    try {
      const payload = { message: form.message };
      if (form.role) payload.role = form.role;
      const r = await api.post('/api/admin/notifications/broadcast', payload);
      setResult({ ok: true, msg: `${lang.sent} (${r.data?.recipientCount || '—'} ${lang.users?.toLowerCase() || 'users'})` });
      setHistory(p => [{ message: form.message, role: form.role || lang.all, sentAt: new Date().toISOString() }, ...p]);
      setForm({ message: '', role: '' });
    } catch (err) { setResult({ ok: false, msg: err.message }); }
    finally { setSending(false); }
  };

  const startProcessGuide = () => {
    setGuideActive(true);
    setTimeout(() => {
      const d = driver({
        animate: true, showProgress: true,
        steps: [
          { popover: { title: lang.processGuide, description: lang.guideNotifDesc, side: 'center' } },
          { element: '[data-guide="notif-form"]', popover: { title: lang.broadcast, description: lang.guideNotifForm, side: 'top' } },
          { element: '[data-guide="notif-history"]', popover: { title: lang.sent, description: lang.guideNotifHistory, side: 'bottom' } },
          { element: '[data-guide="notif-broadcast-history"]', popover: { title: lang.broadcastHistory, description: lang.guideHistoryTable, side: 'bottom' } },
          { popover: { title: lang.done, description: lang.guideNotifDone, side: 'center' } },
        ],
        onDestroy: () => setGuideActive(false),
      });
      d.drive();
    }, 300);
  };

  return (
    <div className="p-6 space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-gray-900">{lang.broadcast}</h2>
        <button onClick={startProcessGuide} className="px-2.5 py-1.5 text-xs font-bold text-gray-500 bg-gray-50 border border-gray-200 rounded-lg hover:bg-gray-100 transition">
          {'\u2753'} {lang.processGuide}
        </button>
      </div>
      <form data-guide="notif-form" onSubmit={doSend} className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5 space-y-3">
        <textarea value={form.message} onChange={e => setForm(p => ({ ...p, message: e.target.value }))} required rows={3} placeholder={lang.message}
          className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm resize-none" />
        <div className="flex items-center gap-3">
          <select value={form.role} onChange={e => setForm(p => ({ ...p, role: e.target.value }))} className="border border-gray-300 rounded-lg px-3 py-1.5 text-sm bg-white">
            <option value="">{lang.all} {lang.users?.toLowerCase() || ''}</option>
            <option value="STUDENT">{lang.students}</option>
            <option value="INSTRUCTOR">{lang.instructors}</option>
          </select>
          <button type="submit" disabled={sending}
            className="px-4 py-1.5 text-xs font-bold bg-[#1e3a8a] text-white rounded-lg hover:bg-[#1e40af] transition disabled:opacity-50">
            {sending ? lang.saving : lang.send}
          </button>
        </div>
        {result && (
          <div className={`text-xs p-2 rounded ${result.ok ? 'text-emerald-700 bg-emerald-50' : 'text-rose-700 bg-rose-50'}`}>{result.msg}</div>
        )}
      </form>
      {history.length > 0 && (
        <div data-guide="notif-history" className="bg-white rounded-2xl shadow-sm border border-gray-100">
          <div className="px-5 py-3 border-b border-gray-100">
            <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">{lang.sent}</span>
          </div>
          <div className="divide-y divide-gray-100 text-xs max-h-48 overflow-y-auto">
            {history.map((h, i) => (
              <div key={i} className="px-5 py-2.5 flex justify-between">
                <span className="text-gray-700 truncate max-w-[300px]">{h.message}</span>
                <span className="text-gray-400 shrink-0 ml-3">{h.role} &middot; {new Date(h.sentAt).toLocaleTimeString()}</span>
              </div>
            ))}
          </div>
        </div>
      )}
      <div data-guide="notif-broadcast-history" className="bg-white rounded-2xl shadow-sm border border-gray-100">
        <div className="px-5 py-3 border-b border-gray-100">
          <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">{lang.broadcastHistory}</span>
        </div>
        {bhLoading ? (
          <div className="animate-pulse space-y-2 p-5">{Array.from({ length: 3 }).map((_, i) => <div key={i} className="h-6 bg-gray-200 rounded w-full" />)}</div>
        ) : broadcastHistory.length === 0 ? (
          <div className="text-sm text-gray-400 text-center py-6">{lang.noBroadcastHistory}</div>
        ) : (
          <div className="divide-y divide-gray-100 text-xs max-h-48 overflow-y-auto">
            {broadcastHistory.map((h, i) => (
              <div key={i} className="px-5 py-2.5 flex justify-between">
                <span className="text-gray-700 truncate max-w-[250px]">{h.actorEmail}</span>
                <span className="text-gray-400 shrink-0 ml-3">{h.details?.recipientCount || '—'} {lang.recipients} &middot; {h.occurredAt ? new Date(h.occurredAt).toLocaleString() : ''}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function CollectionsSection({ lang, api }) {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [guideActive, setGuideActive] = useState(false);

  const fetch = useCallback(async (signal) => {
    setLoading(true);
    try { const r = await api.get('/api/admin/collections', { signal }); setData(r.data); }
    catch (e) { /* silent */ }
    finally { setLoading(false); }
  }, [api]);

  useEffect(() => { const ac = new AbortController(); fetch(ac.signal); return () => ac.abort(); }, [fetch]);

  const startProcessGuide = () => {
    setGuideActive(true);
    setTimeout(() => {
      const d = driver({
        animate: true, showProgress: true,
        steps: [
          { popover: { title: lang.processGuide, description: lang.guideCollectionsDesc, side: 'center' } },
          { element: '[data-guide="collections-table"]', popover: { title: lang.collections, description: lang.guideCollectionsTable, side: 'left' } },
          { popover: { title: lang.done, description: lang.guideCollectionsDone, side: 'center' } },
        ],
        onDestroy: () => setGuideActive(false),
      });
      d.drive();
    }, 300);
  };

  if (loading) return <PageSkeleton />;
  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-bold text-gray-900">{lang.collections}</h2>
        <button onClick={startProcessGuide} className="px-2.5 py-1.5 text-xs font-bold text-gray-500 bg-gray-50 border border-gray-200 rounded-lg hover:bg-gray-100 transition">
          {'\u2753'} {lang.processGuide}
        </button>
      </div>
      <div className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table data-guide="collections-table" className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-gray-50 text-gray-400 text-xs font-bold uppercase border-b border-gray-100">
                <th className="px-5 py-3">{lang.collectionName}</th>
                <th className="px-5 py-3">{lang.instructor}</th>
                <th className="px-5 py-3">{lang.createdAt}</th>
                <th className="px-5 py-3">{lang.status}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-xs">
              {data.length === 0 ? (
                <tr><td colSpan={4} className="px-5 py-10 text-center text-gray-400">{lang.noCollections}</td></tr>
              ) : data.map(c => (
                <tr key={c.id} className="hover:bg-gray-50/50 transition">
                  <td className="px-5 py-3 font-semibold text-gray-900">{c.name}</td>
                  <td className="px-5 py-3 text-gray-600">{c.instructorEmail}</td>
                  <td className="px-5 py-3 text-gray-500">{c.createdAt ? new Date(c.createdAt).toLocaleDateString() : '—'}</td>
                  <td className="px-5 py-3"><span className={`px-2 py-0.5 rounded text-[10px] font-bold ${c.active ? 'bg-emerald-50 text-emerald-700' : 'bg-gray-100 text-gray-500'}`}>{c.active ? lang.active : lang.banned}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function SettingsSection({ lang, api }) {
  const [name, setName] = useState('Evidence Pilot');
  const [saved, setSaved] = useState(false);
  const [guideActive, setGuideActive] = useState(false);
  const [cats, setCats] = useState([]);
  const [catsLoading, setCatsLoading] = useState(true);
  const [showCatForm, setShowCatForm] = useState(false);
  const [catForm, setCatForm] = useState({ id: null, name: '', description: '' });
  const [catErr, setCatErr] = useState('');
  const [config, setConfig] = useState(null);
  const [configLoading, setConfigLoading] = useState(true);

  const fetchCats = useCallback(async (signal) => {
    setCatsLoading(true);
    try { const r = await api.get('/api/admin/source-categories?active=true', { signal }); setCats(r.data); }
    catch (e) { /* silent */ }
    finally { setCatsLoading(false); }
  }, [api]);

  const fetchConfig = useCallback(async (signal) => {
    setConfigLoading(true);
    try { const r = await api.get('/api/admin/config', { signal }); setConfig(r.data); }
    catch (e) { /* silent */ }
    finally { setConfigLoading(false); }
  }, [api]);

  useEffect(() => {
    const ac = new AbortController();
    fetchCats(ac.signal); fetchConfig(ac.signal);
    return () => ac.abort();
  }, [fetchCats, fetchConfig]);

  const doSave = (e) => { e.preventDefault(); setSaved(true); setTimeout(() => setSaved(false), 2000); };

  const doCatSave = async (e) => {
    e.preventDefault(); setCatErr('');
    try {
      if (catForm.id) { await api.put(`/api/admin/source-categories/${catForm.id}`, { name: catForm.name, description: catForm.description }); }
      else { await api.post('/api/admin/source-categories', { name: catForm.name, description: catForm.description }); }
      setShowCatForm(false); setCatForm({ id: null, name: '', description: '' }); fetchCats(new AbortController().signal);
    } catch (err) { setCatErr(err.response?.data?.message || err.message); }
  };

  const doCatDelete = async (id) => {
    if (!confirm(lang.confirmDelete)) return;
    try { await api.delete(`/api/admin/source-categories/${id}`); fetchCats(new AbortController().signal); }
    catch (e) { /* silent */ }
  };

  const startProcessGuide = () => {
    setGuideActive(true);
    setTimeout(() => {
      const d = driver({
        animate: true, showProgress: true,
        steps: [
          { popover: { title: lang.processGuide, description: lang.guideSettingsDesc, side: 'center' } },
          { element: '[data-guide="settings-form"]', popover: { title: lang.settings, description: lang.guideSettingsForm, side: 'bottom' } },
          { element: '[data-guide="settings-categories"]', popover: { title: lang.sourceCategories, description: lang.guideCategoriesDesc, side: 'top' } },
          { element: '[data-guide="settings-config"]', popover: { title: lang.systemConfig, description: lang.guideConfigDesc, side: 'top' } },
          { popover: { title: lang.done, description: lang.guideSettingsDone, side: 'center' } },
        ],
        onDestroy: () => setGuideActive(false),
      });
      d.drive();
    }, 300);
  };

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold text-gray-900">{lang.settings}</h2>
        <button onClick={startProcessGuide} className="px-2.5 py-1.5 text-xs font-bold text-gray-500 bg-gray-50 border border-gray-200 rounded-lg hover:bg-gray-100 transition">
          {'\u2753'} {lang.processGuide}
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <form data-guide="settings-form" onSubmit={doSave} className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5 space-y-4">
          <div>
            <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-1.5">{lang.appName}</label>
            <input value={name} onChange={e => setName(e.target.value)} className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm" />
          </div>
          <div className="flex items-center gap-2">
            <button type="submit" className="px-4 py-1.5 text-xs font-bold bg-[#1e3a8a] text-white rounded-lg hover:bg-[#1e40af] transition">{lang.save}</button>
            {saved && <span className="text-xs text-emerald-600 font-medium">{lang.saved}</span>}
          </div>
        </form>

        <div data-guide="settings-categories" className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5">
        <div className="flex items-center justify-between mb-4">
          <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">{lang.sourceCategories}</span>
          <button onClick={() => { setCatForm({ id: null, name: '', description: '' }); setShowCatForm(true); }} className="px-2.5 py-1 text-xs font-bold bg-[#1e3a8a] text-white rounded-lg hover:bg-[#1e40af] transition">{lang.addCategory}</button>
        </div>
        {catsLoading ? (
          <div className="animate-pulse space-y-2">{Array.from({ length: 3 }).map((_, i) => <div key={i} className="h-8 bg-gray-200 rounded w-full" />)}</div>
        ) : cats.length === 0 ? (
          <div className="text-sm text-gray-400 text-center py-4">{lang.noCategories}</div>
        ) : (
          <div className="divide-y divide-gray-100 text-sm max-h-64 overflow-y-auto">
            {cats.map(c => (
              <div key={c.id} className="flex items-center justify-between py-2.5">
                <div>
                  <span className="font-semibold text-gray-900">{c.name}</span>
                  {c.description && <span className="text-gray-400 ml-2 text-xs">{c.description}</span>}
                </div>
                <div className="flex items-center gap-2">
                  <button onClick={() => { setCatForm({ id: c.id, name: c.name, description: c.description || '' }); setShowCatForm(true); }} className="px-2 py-1 text-xs text-gray-600 bg-gray-50 border border-gray-200 rounded-lg hover:bg-gray-100 transition">{lang.editCategory}</button>
                  <button onClick={() => doCatDelete(c.id)} className="px-2 py-1 text-xs text-gray-400 border border-gray-200 rounded-lg hover:border-rose-200 hover:text-rose-600 transition">{lang.delete}</button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
      </div>

      <div data-guide="settings-config" className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5">
        <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block mb-1">{lang.systemConfig}</span>
        <p className="text-xs text-gray-400 mb-3">{lang.configNote}</p>
        {configLoading ? (
          <div className="animate-pulse space-y-2">{Array.from({ length: 5 }).map((_, i) => <div key={i} className="h-6 bg-gray-200 rounded w-full" />)}</div>
        ) : !config ? (
          <div className="text-sm text-gray-400 text-center py-4">—</div>
        ) : (
          <div className="divide-y divide-gray-100 text-xs">
            {Object.entries(config).map(([k, v]) => (
              <div key={k} className="flex items-center justify-between py-2">
                <span className="font-mono font-medium text-gray-700">{k.replace(/([A-Z])/g, ' $1').trim()}</span>
                <span className="font-mono text-gray-500 max-w-[200px] truncate" title={v}>{v}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      {showCatForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30" onClick={() => setShowCatForm(false)}>
          <div className="bg-white rounded-2xl shadow-xl p-6 w-full max-w-md mx-4" onClick={e => e.stopPropagation()}>
            <h3 className="font-bold text-gray-900 mb-4">{catForm.id ? lang.editCategory : lang.addCategory}</h3>
            <form onSubmit={doCatSave} className="space-y-3">
              <input placeholder={lang.categoryName} value={catForm.name} onChange={e => setCatForm(p => ({ ...p, name: e.target.value }))} required className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm" />
              <textarea placeholder={lang.categoryDescription} value={catForm.description} onChange={e => setCatForm(p => ({ ...p, description: e.target.value }))} rows={2} className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm resize-none" />
              {catErr && <div className="text-xs text-rose-600 bg-rose-50 p-2 rounded">{catErr}</div>}
              <div className="flex gap-2 justify-end">
                <button type="button" onClick={() => setShowCatForm(false)} className="px-3 py-1.5 text-xs font-bold text-gray-600 border border-gray-200 rounded-lg hover:bg-gray-50">{lang.cancel}</button>
                <button type="submit" className="px-3 py-1.5 text-xs font-bold bg-[#1e3a8a] text-white rounded-lg hover:bg-[#1e40af]">{catForm.id ? lang.save : lang.addCategory}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

/* ----- MAIN SHELL ----- */

const NAV_ITEMS = [
  { key: 'dashboard', icon: '\uD83D\uDCCA', labelEn: 'Dashboard', labelVi: 'B\u1EA3ng \u0111i\u1EC1u khi\u1EC3n' },
  { key: 'users', icon: '\uD83D\uDC65', labelEn: 'Users', labelVi: 'Ng\u01B0\u1EDDi d\u00F9ng' },
  { key: 'projects', icon: '\uD83D\uDCC1', labelEn: 'Projects', labelVi: 'D\u1EF1 \u00E1n' },
  { key: 'papers', icon: '\uD83D\uDCC4', labelEn: 'Papers', labelVi: 'B\u00E0i b\u00E1o' },
  { key: 'audit', icon: '\uD83D\uDCCB', labelEn: 'Audit Logs', labelVi: 'Nh\u1EADt k\u00FD' },
  { key: 'infra', icon: '\uD83D\uDDA5\uFE0F', labelEn: 'Infrastructure', labelVi: 'H\u1EA1 t\u1EA7ng' },
  { key: 'extraction', icon: '\u2699\uFE0F', labelEn: 'Extraction Queue', labelVi: 'H\u00E0ng \u0111\u1EE3i' },
  { key: 'collections', icon: '\uD83D\uDCDA', labelEn: 'Collections', labelVi: 'B\u1ED9 s\u01B0u t\u1EADp' },
  { key: 'notifications', icon: '\uD83D\uDD14', labelEn: 'Notifications', labelVi: 'Th\u00F4ng b\u00E1o' },
  { key: 'settings', icon: '\u2699\uFE0F', labelEn: 'Settings', labelVi: 'C\u00E0i \u0111\u1EB7t' },
];

const SECTIONS = {
  dashboard: DashboardSection, users: UsersSection, projects: ProjectsSection, papers: PapersSection,
  audit: AuditLogsSection, infra: InfraSection, extraction: QueueSection, collections: CollectionsSection, notifications: NotificationsSection,
  settings: SettingsSection,
};

export default function AdminDashboard() {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const { language, toggleLanguage } = useLanguage();
  const L = t[language] || t.en;
  const label = (item) => language === 'vi' ? item.labelVi : item.labelEn;

  const [active, setActive] = useState('dashboard');
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  const Section = SECTIONS[active];

  const handleLogout = () => { logout(); navigate('/'); };

  const startTour = useCallback(() => {
    const navItems = NAV_ITEMS.map(item => ({
      element: `[data-tour="nav-${item.key}"]`,
      popover: {
        title: label(item),
        description: language === 'vi'
          ? `Nhấp để xem ${item.labelVi.toLowerCase()}. Tại đây bạn có thể quản lý và theo dõi các hoạt động liên quan.`
          : `Click to view ${item.labelEn.toLowerCase()}. Here you can manage and monitor related activities.`,
        side: 'right',
        align: 'start',
      }
    }));

    const driverObj = driver({
      animate: true,
      showProgress: true,
      showButtons: ['next', 'previous', 'close'],
      steps: [
        {
          popover: {
            title: language === 'vi' ? 'Chào mừng đến với Trang Quản trị' : 'Welcome to Admin Panel',
            description: language === 'vi'
              ? 'Hướng dẫn này sẽ giới thiệu các chức năng chính. Nhấp "Tiếp theo" để bắt đầu.'
              : 'This guide will introduce the main features. Click "Next" to start.',
            side: 'center',
          }
        },
        {
          element: '[data-tour="sidebar"]',
          popover: {
            title: language === 'vi' ? 'Thanh điều hướng' : 'Sidebar Navigation',
            description: language === 'vi'
              ? 'Sử dụng thanh bên để chuyển đổi giữa các chức năng quản trị.'
              : 'Use the sidebar to switch between admin functions.',
            side: 'right',
          }
        },
        ...navItems,
        {
          element: '[data-tour="header"]',
          popover: {
            title: language === 'vi' ? 'Thanh tiêu đề' : 'Header Bar',
            description: language === 'vi'
              ? 'Chứa nút chuyển ngôn ngữ, hướng dẫn và thông tin quản trị viên.'
              : 'Contains language toggle, guide, and admin profile info.',
            side: 'bottom',
          }
        },
        {
          element: '[data-tour="content"]',
          popover: {
            title: language === 'vi' ? 'Khu vực nội dung' : 'Content Area',
            description: language === 'vi'
              ? 'Nội dung của chức năng đang chọn sẽ hiển thị tại đây.'
              : 'Content for the selected function is displayed here.',
            side: 'left',
          }
        },
        {
          element: '[data-tour="footer"]',
          popover: {
            title: language === 'vi' ? 'Chân trang' : 'Footer',
            description: language === 'vi'
              ? 'Chuyển đổi ngôn ngữ giữa Tiếng Việt và English tại đây.'
              : 'Switch language between Vietnamese and English here.',
            side: 'top',
          }
        },
        {
          popover: {
            title: language === 'vi' ? 'Bắt đầu sử dụng' : 'Ready to Go',
            description: language === 'vi'
              ? 'Bạn đã sẵn sàng! Nhấp "Kết thúc" để bắt đầu quản trị hệ thống.'
              : "You're all set! Click 'Finish' to start managing the system.",
          }
        },
      ],
      onDestroy: () => localStorage.setItem('admin_tour_done', 'true'),
    });

    driverObj.drive();
  }, [language]);

  return (
    <div className="min-h-screen bg-[#f8fafc] font-sans flex">
      {/* Mobile overlay */}
      {mobileOpen && <div className="fixed inset-0 bg-black/30 z-30 lg:hidden" onClick={() => setMobileOpen(false)} />}

      {/* Sidebar */}
      <aside data-tour="sidebar" className={`fixed lg:static inset-y-0 left-0 z-40 bg-white border-r border-gray-200 flex flex-col transition-all duration-200 ${collapsed ? 'w-16' : 'w-56'} ${mobileOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}`}>
        {/* Brand */}
        <div className="h-14 flex items-center px-4 border-b border-gray-100 shrink-0">
          {collapsed ? (
            <span className="text-lg font-black text-[#1e3a8a] mx-auto">EP</span>
          ) : (
            <span className="text-base font-bold text-[#1e3a8a] tracking-tight">Evidence Pilot</span>
          )}
        </div>

        {/* Nav */}
        <nav className="flex-1 overflow-y-auto py-3 px-2 space-y-0.5">
          {NAV_ITEMS.map(item => (
            <button key={item.key} data-tour={`nav-${item.key}`} onClick={() => { setActive(item.key); setMobileOpen(false); }}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-xs font-bold transition text-left ${active === item.key ? 'bg-[#1e3a8a]/10 text-[#1e3a8a] border-l-2 border-[#1e3a8a] rounded-l-none' : 'text-gray-500 hover:bg-gray-50 hover:text-gray-900'}`}
              title={collapsed ? label(item) : undefined}>
              <span className="text-base shrink-0">{item.icon}</span>
              {!collapsed && <span className="truncate">{label(item)}</span>}
            </button>
          ))}
        </nav>

        {/* Bottom */}
        <div className="border-t border-gray-100 p-2 space-y-0.5 shrink-0">
          <button onClick={handleLogout} className="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-xs font-bold text-gray-500 hover:bg-gray-50 hover:text-gray-900 transition">
            <span className="text-base shrink-0">{'\uD83D\uDEAA'}</span>
            {!collapsed && <span>{L.signOut}</span>}
          </button>
          <button onClick={() => setCollapsed(p => !p)} className="hidden lg:flex w-full items-center gap-3 px-3 py-2 rounded-lg text-xs font-bold text-gray-400 hover:bg-gray-50 transition">
            <span className="text-sm shrink-0">{collapsed ? '\u25B6' : '\u25C0'}</span>
            {!collapsed && <span>{L.collapse}</span>}
          </button>
        </div>
      </aside>

      {/* Main area */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Header */}
        <header data-tour="header" className="h-14 bg-white border-b border-gray-200 flex items-center justify-between px-4 lg:px-6 shrink-0">
          <div className="flex items-center gap-3">
            <button onClick={() => setMobileOpen(true)} className="lg:hidden text-gray-500 hover:text-gray-900">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" /></svg>
            </button>
            <h1 className="text-sm font-bold text-gray-900 hidden sm:block">{L.adminPanel}</h1>
            <span className="text-xs text-gray-400">/ {label(NAV_ITEMS.find(n => n.key === active))}</span>
          </div>
          <div className="flex items-center gap-2">
            <button onClick={startTour} className="text-xs font-bold text-gray-500 bg-gray-50 border border-gray-200 px-2.5 py-1.5 rounded-lg hover:bg-gray-100 transition">
              {'\u2753'} {L.tourGuide}
            </button>
            <button onClick={toggleLanguage} className="text-xs font-bold text-gray-500 bg-gray-50 border border-gray-200 px-2.5 py-1.5 rounded-lg hover:bg-gray-100 transition">
              {L.langSwitch}
            </button>
            <div className="w-7 h-7 rounded-md bg-gradient-to-tr from-rose-600 to-orange-500 flex items-center justify-center text-[11px] text-white font-black">
              AD
            </div>
          </div>
        </header>

        {/* Content */}
        <main data-tour="content" className="flex-1 overflow-y-auto">
          <Section lang={L} api={api} />
        </main>

        {/* Footer */}
        <footer data-tour="footer" className="bg-white border-t border-gray-200 px-6 py-3 flex items-center justify-between text-[11px] text-gray-400 shrink-0">
          <span>{L.copyright}</span>
          <button onClick={toggleLanguage} className="font-bold text-gray-500 hover:text-[#1e3a8a] transition">
            {L.langSwitch}
          </button>
        </footer>
      </div>
    </div>
  );
}
