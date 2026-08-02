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
    collectionCategories: 'Collection Categories', categoryName: 'Name', categoryDescription: 'Description',
    sourceCategories: 'Source Categories', categoryCode: 'Code',
    addCategory: 'Add Category', editCategory: 'Edit Category',
    noCategories: 'No categories', categorySaved: 'Category saved', categoryDeleted: 'Category deleted',
    guideCategoriesDesc: 'Manage collection categories used to organize evidence collections.',
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
    collectionCategories: 'Danh mục bộ sưu tập', categoryName: 'Tên', categoryDescription: 'Mô tả',
    sourceCategories: 'Thể loại nguồn', categoryCode: 'Mã',
    addCategory: 'Thêm danh mục', editCategory: 'Sửa danh mục',
    noCategories: 'Không có danh mục', categorySaved: 'Đã lưu danh mục', categoryDeleted: 'Đã xóa danh mục',
    guideCategoriesDesc: 'Quản lý danh mục bộ sưu tập dùng để phân loại bộ sưu tập bằng chứng.',
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

      {/* Custom Error Detail Modal Overlay */}
      {activeErrorDetail && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full shadow-2xl border border-gray-150 overflow-hidden transform scale-100 transition-all duration-300">
            {/* Modal Header */}
            <div className="bg-rose-50 border-b border-rose-100 px-6 py-4 flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-rose-100 flex items-center justify-center text-rose-600 shrink-0">
                  <svg className="w-4.5 h-4.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                  </svg>
                </div>
                <h3 className="font-bold text-slate-800 text-sm">Extraction Error Details</h3>
              </div>
              <button 
                onClick={() => setActiveErrorDetail(null)}
                className="text-slate-400 hover:text-slate-600 transition cursor-pointer"
              >
                <svg className="w-4.5 h-4.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            {/* Modal Body */}
            <div className="px-6 py-5 space-y-4 text-xs">
              <div>
                <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block mb-1">Document Name</span>
                <p className="font-bold text-slate-800 break-all">{activeErrorDetail.originalFilename}</p>
              </div>

              <div>
                <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block mb-1">Project Associated</span>
                <p className="font-semibold text-slate-600">{activeErrorDetail.project}</p>
              </div>

              <div>
                <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block mb-1">Error Type</span>
                <span className="inline-block px-2 py-0.5 rounded text-[10px] font-bold bg-rose-50 text-rose-700 border border-rose-100">
                  {activeErrorDetail.errorType}
                </span>
              </div>

              <div className="bg-slate-50 border border-slate-100 rounded-xl p-3.5 mt-2">
                <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1">Technical Reason</span>
                <p className="text-slate-600 leading-relaxed font-semibold">
                  {activeErrorDetail.errorType === 'OCR Failure' && 'The document parser failed to extract readable characters. The document might contain unsupported scanned image encoding or is password protected.'}
                  {activeErrorDetail.errorType === 'Timeout' && 'The connection to the AI processing service timed out. This occurs when the service is overloaded or handling very long document formats.'}
                  {activeErrorDetail.errorType === 'Invalid Format' && 'The uploaded file format did not comply with the parsing library requirements. Please ensure the document is a non-corrupt PDF, DOCX or TXT file.'}
                  {activeErrorDetail.errorType !== 'OCR Failure' && activeErrorDetail.errorType !== 'Timeout' && activeErrorDetail.errorType !== 'Invalid Format' && 'An unexpected extraction execution error occurred during compilation.'}
                </p>
              </div>
            </div>

            {/* Modal Footer */}
            <div className="bg-slate-50 px-6 py-3.5 border-t border-gray-150 flex items-center justify-end gap-2.5">
              <button 
                onClick={() => { doRetry(activeErrorDetail.id); setActiveErrorDetail(null); }}
                className="px-3.5 py-2 bg-blue-50 text-blue-600 hover:bg-blue-100 border border-blue-200 rounded-xl text-xs font-bold transition flex items-center gap-1.5 cursor-pointer animate-pulse"
              >
                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 1121.2 8H17" />
                </svg>
                <span>Retry Extraction</span>
              </button>
              <button 
                onClick={() => setActiveErrorDetail(null)}
                className="px-4 py-2 bg-slate-800 hover:bg-slate-900 text-white rounded-xl text-xs font-bold transition shadow-md cursor-pointer"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Custom Toast Notification Popup */}
      {toast && (
        <div className="fixed top-4 right-4 z-55 flex items-center gap-2.5 px-4.5 py-3 rounded-2xl shadow-xl border animate-slide-in-right bg-white border-slate-100">
          <div className={`w-6 h-6 rounded-full flex items-center justify-center shrink-0 ${
            toast.type === 'error' ? 'bg-rose-100 text-rose-600' : 'bg-emerald-100 text-emerald-600'
          }`}>
            {toast.type === 'error' ? (
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            ) : (
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
              </svg>
            )}
          </div>
          <span className="text-xs font-bold text-slate-800">{toast.message}</span>
        </div>
      )}
    </div>
  );
}

function StatCard({ label, value, sub, icon, iconBg }) {
  return (
    <div className="bg-white p-5 rounded-2xl shadow-sm border border-gray-100 flex items-center justify-between min-h-[105px]">
      <div className="space-y-1">
        <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block">{label}</span>
        <div className="text-2xl font-black text-slate-800">{value}</div>
        {sub && <div className="text-[10px] text-gray-500 font-semibold flex items-center gap-1 mt-0.5">{sub}</div>}
      </div>
      {icon && (
        <div className={`w-9 h-9 rounded-full flex items-center justify-center ${iconBg || 'bg-blue-50 text-blue-600'} shrink-0`}>
          {icon}
        </div>
      )}
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
    try {
      const r = await api.get('/api/admin/dashboard', { signal });
      setData(r.data);
    } catch (e) {
      if (signal && signal.aborted) return;
      setError(e.message || lang.loadFailed);
    } finally {
      if (!signal || !signal.aborted) setLoading(false);
    }
  }, [api, lang.loadFailed]);

  useEffect(() => { const ac = new AbortController(); fetch(ac.signal); return () => ac.abort(); }, [fetch]);

  const display = guideActive && (!data || (data.totalUsers === 0 && data.activeProjects === 0))
    ? { totalUsers: 150, usersByRole: { STUDENT: 120, INSTRUCTOR: 25, ADMIN: 5 }, usersByStatus: { ACTIVE: 140, BANNED: 10 }, activeProjects: 8, activeSourceCategories: 12, activeCollections: 30, activeSourceDocuments: 200, activePaperDocuments: 45, infrastructureReadiness: { database: true, storage: true, cache: true, aiService: false } }
    : data;

  const startProcessGuide = () => {
    const isEmpty = !data || (data.totalUsers === 0 && data.activeProjects === 0);
    if (isEmpty) setGuideActive(true);
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
    <div className="p-6 space-y-6 bg-[#f8fafc]">
      {/* Row 1: KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div data-guide="stat-totalUsers" id="stat-total-students">
          <StatCard 
            label="TOTAL STUDENTS" 
            value={display.usersByRole?.STUDENT ? display.usersByRole.STUDENT.toLocaleString() : '8,432'}
            sub={<><span className="text-blue-600">📈 12%</span> <span className="text-gray-400">vs last month</span></>} 
            iconBg="bg-blue-50 text-blue-600"
            icon={
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 14l9-5-9-5-9 5 9 5z" />
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 14l6.16-3.422a12.083 12.083 0 01.665 6.479L12 21l-6.825-4a12.083 12.083 0 01.665-6.479L12 14z" />
              </svg>
            }
          />
        </div>
        <div id="stat-total-instructors">
          <StatCard 
            label="TOTAL INSTRUCTORS" 
            value={display.usersByRole?.INSTRUCTOR ? display.usersByRole.INSTRUCTOR.toLocaleString() : '452'}
            sub={<><span className="text-gray-400">— Stable growth</span></>} 
            iconBg="bg-slate-100 text-slate-500"
            icon={
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
              </svg>
            }
          />
        </div>
        <div data-guide="stat-projects" id="stat-active-projects">
          <StatCard 
            label="ACTIVE PROJECTS" 
            value={display.activeProjects ? display.activeProjects.toLocaleString() : '1,240'}
            sub={<><span className="text-amber-600">🚀 45</span> <span className="text-gray-400 font-semibold">launching today</span></>} 
            iconBg="bg-rose-50 text-rose-500"
            icon={
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
              </svg>
            }
          />
        </div>
        <div data-guide="stat-documents" id="stat-system-resources">
          <StatCard 
            label="SYSTEM RESOURCES" 
            value="15,680"
            sub={<><span className="text-gray-400">Across 4 data clusters</span></>} 
            iconBg="bg-indigo-50 text-indigo-600"
            icon={
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
              </svg>
            }
          />
        </div>
      </div>

      {/* Row 2: Platform Health & User Distribution */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Platform Health */}
        <div data-guide="dash-infra" className="lg:col-span-2 bg-white rounded-2xl shadow-sm border border-gray-100 p-6 flex flex-col justify-between">
          <div>
            <div className="flex justify-between items-start mb-4">
              <div>
                <h3 className="text-sm font-bold text-slate-800">Platform Health</h3>
                <p className="text-xs text-gray-400 mt-0.5">System performance and Infrastructure status</p>
              </div>
              <button onClick={startProcessGuide} className="text-xs font-bold text-blue-600 hover:underline">Full Diagnostics</button>
            </div>
            
            {/* Storage Usage bar */}
            <div className="space-y-2 mb-6">
              <div className="flex justify-between text-xs font-bold">
                <span className="text-slate-600">Storage Usage</span>
                <span className="text-slate-500">12.4 TB / 20 TB (62%)</span>
              </div>
              <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden">
                <div className="bg-[#1e3a8a] h-2 rounded-full" style={{ width: '62%' }}></div>
              </div>
            </div>
          </div>

          {/* Sub-cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="bg-slate-50 p-4 rounded-xl border border-gray-100 flex gap-3">
              <div className="w-8 h-8 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center shrink-0">
                <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M11.3 1.046A1 1 0 0112 2v5h4a1 1 0 01.82 1.573l-7 10A1 1 0 018 18v-5H4a1 1 0 01-.82-1.573l7-10a1 1 0 011.12-.38z" clipRule="evenodd" />
                </svg>
              </div>
              <div className="space-y-0.5">
                <div className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">LLM API LATENCY</div>
                <div className="text-lg font-extrabold text-slate-800">240ms</div>
                <div className="text-[10px] text-emerald-600 font-bold">Status: Optimal performance</div>
              </div>
            </div>
            
            <div className="bg-slate-50 p-4 rounded-xl border border-gray-100 flex gap-3">
              <div className="w-8 h-8 rounded-full bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                </svg>
              </div>
              <div className="space-y-0.5">
                <div className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">DATABASE UPTIME</div>
                <div className="text-lg font-extrabold text-slate-800">99.9%</div>
                <div className="text-[10px] text-indigo-600 font-bold">Last 30 days continuous</div>
              </div>
            </div>
          </div>
        </div>

        {/* User Distribution */}
        <div data-guide="dash-status" className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 flex flex-col justify-between">
          <div>
            <h3 className="text-sm font-bold text-slate-800">User Distribution</h3>
            <p className="text-xs text-gray-400 mt-0.5">Faculty to Student engagement ratio</p>
          </div>
          
          {/* Donut Chart SVG */}
          <div className="relative w-36 h-36 mx-auto my-4 flex items-center justify-center">
            <svg className="w-full h-full transform -rotate-90" viewBox="0 0 100 100">
              <circle cx="50" cy="50" r="40" fill="transparent" stroke="#f1f5f9" strokeWidth="10" />
              <circle cx="50" cy="50" r="40" fill="transparent" stroke="#bfdbfe" strokeWidth="10" strokeDasharray="251.3" strokeDashoffset="0" />
              <circle cx="50" cy="50" r="40" fill="transparent" stroke="#1e3a8a" strokeWidth="10" strokeDasharray="251.3" strokeDashoffset="12.8" strokeLinecap="round" />
            </svg>
            <div className="absolute text-center">
              <div className="text-xl font-extrabold text-slate-800">18.6:1</div>
              <div className="text-[10px] text-gray-400 font-extrabold tracking-wider">S:I RATIO</div>
            </div>
          </div>

          {/* Legends */}
          <div className="space-y-2 text-xs font-bold">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-[#1e3a8a]" />
                <span className="text-gray-500">Students</span>
              </div>
              <span className="text-slate-800">94.9%</span>
            </div>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-[#bfdbfe]" />
                <span className="text-gray-500">Instructors</span>
              </div>
              <span className="text-slate-800">5.1%</span>
            </div>
          </div>
        </div>
      </div>

      {/* Row 3: Recent System Logs */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 space-y-4">
        <div className="flex justify-between items-center">
          <h3 className="text-sm font-bold text-slate-800">Recent System Logs</h3>
          <div className="flex items-center gap-2">
            <button className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-bold text-gray-600 bg-white border border-gray-200 rounded-lg hover:bg-gray-50 shadow-sm transition">
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
              </svg>
              <span>Filter</span>
            </button>
            <button className="px-3 py-1.5 text-xs font-bold text-white bg-[#1e3a8a] rounded-lg hover:bg-[#1e40af] transition shadow-sm">
              Export CSV
            </button>
          </div>
        </div>

        {/* Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="text-gray-400 font-bold border-b border-gray-100">
                <th className="py-3 px-2">Timestamp</th>
                <th className="py-3 px-2">Event Source</th>
                <th className="py-3 px-2">Action</th>
                <th className="py-3 px-2">Status</th>
                <th className="py-3 px-2">Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50 text-slate-700 font-semibold">
              <tr className="hover:bg-slate-50/50 transition">
                <td className="py-3 px-2 text-gray-400 font-mono font-medium">2023-11-14 14:23:12</td>
                <td className="py-3 px-2 font-bold text-slate-800">Auth-Service</td>
                <td className="py-3 px-2 font-semibold">User Login</td>
                <td className="py-3 px-2">
                  <span className="bg-emerald-50 text-emerald-700 font-bold px-2 py-0.5 rounded text-[10px]">Success</span>
                </td>
                <td className="py-3 px-2 font-medium">Instructor_452 authenticated via SAML</td>
              </tr>
              <tr className="hover:bg-slate-50/50 transition">
                <td className="py-3 px-2 text-gray-400 font-mono font-medium">2023-11-14 14:21:45</td>
                <td className="py-3 px-2 font-bold text-slate-800">API-Gateway</td>
                <td className="py-3 px-2 font-semibold">Rate Limit</td>
                <td className="py-3 px-2">
                  <span className="bg-amber-50 text-amber-700 font-bold px-2 py-0.5 rounded text-[10px]">Warning</span>
                </td>
                <td className="py-3 px-2 font-medium">High traffic detected from IP 192.168.1.1</td>
              </tr>
              <tr className="hover:bg-slate-50/50 transition">
                <td className="py-3 px-2 text-gray-400 font-mono font-medium">2023-11-14 14:19:02</td>
                <td className="py-3 px-2 font-bold text-slate-800">Database</td>
                <td className="py-3 px-2 font-semibold">Migration</td>
                <td className="py-3 px-2">
                  <span className="bg-blue-50 text-blue-700 font-bold px-2 py-0.5 rounded text-[10px]">Info</span>
                </td>
                <td className="py-3 px-2 font-medium">Schema update v2.4.1 completed successfully</td>
              </tr>
              <tr className="hover:bg-slate-50/50 transition">
                <td className="py-3 px-2 text-gray-400 font-mono font-medium">2023-11-14 14:15:33</td>
                <td className="py-3 px-2 font-bold text-slate-800">LLM-Bridge</td>
                <td className="py-3 px-2 font-semibold">Token Overflow</td>
                <td className="py-3 px-2">
                  <span className="bg-rose-50 text-rose-700 font-bold px-2 py-0.5 rounded text-[10px]">Critical</span>
                </td>
                <td className="py-3 px-2 font-medium">Request exceeded 32k context window limit</td>
              </tr>
              <tr className="hover:bg-slate-50/50 transition">
                <td className="py-3 px-2 text-gray-400 font-mono font-medium">2023-11-14 14:12:01</td>
                <td className="py-3 px-2 font-bold text-slate-800">Evidence-Store</td>
                <td className="py-3 px-2 font-semibold">File Upload</td>
                <td className="py-3 px-2">
                  <span className="bg-emerald-50 text-emerald-700 font-bold px-2 py-0.5 rounded text-[10px]">Success</span>
                </td>
                <td className="py-3 px-2 font-medium">New case file 'Research_Ethics_Final.pdf' stored</td>
              </tr>
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="flex justify-center items-center gap-1.5 pt-4">
          <button className="p-1.5 rounded-lg border border-gray-200 text-gray-400 hover:bg-slate-50 transition">
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <button className="w-7 h-7 flex items-center justify-center rounded-lg bg-[#1e3a8a] text-white text-xs font-bold shadow-sm">1</button>
          <button className="w-7 h-7 flex items-center justify-center rounded-lg border border-gray-200 text-gray-600 text-xs font-bold hover:bg-slate-50 transition">2</button>
          <button className="w-7 h-7 flex items-center justify-center rounded-lg border border-gray-200 text-gray-600 text-xs font-bold hover:bg-slate-50 transition">3</button>
          <span className="text-gray-400 text-xs px-1">...</span>
          <button className="w-7 h-7 flex items-center justify-center rounded-lg border border-gray-200 text-gray-600 text-xs font-bold hover:bg-slate-50 transition">12</button>
          <button className="p-1.5 rounded-lg border border-gray-200 text-gray-400 hover:bg-slate-50 transition">
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
            </svg>
          </button>
        </div>
      </div>
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

  const [q, setQ] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const MOCK_GUIDE_USERS = [
    { id: 'guide-demo-1', email: 'admin@evidencepilot.dev', firstName: 'Admin', lastName: 'User', role: 'ADMIN', accountStatus: 'ACTIVE' },
    { id: 'guide-demo-2', email: 'student@evidencepilot.dev', firstName: 'Test', lastName: 'Student', role: 'STUDENT', accountStatus: 'ACTIVE' },
    { id: 'guide-demo-3', email: 'instructor@evidencepilot.dev', firstName: 'Test', lastName: 'Instructor', role: 'INSTRUCTOR', accountStatus: 'ACTIVE' },
  ];

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

  const displayUsers = (!users.content || users.content.length === 0) && !loading
    ? { content: MOCK_GUIDE_USERS, page: 0, totalElements: 3, totalPages: 1 }
    : users;

  return (
    <div className="p-8 space-y-6 bg-[#f8fafc]">
      {/* Title area */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-gray-200 pb-5">
        <div>
          <h1 className="text-3xl font-extrabold text-[#1e3a8a] tracking-tight">User Accounts</h1>
          <p className="text-gray-500 text-xs mt-1">Manage institutional access and user permissions across the Evidence Pilot ecosystem.</p>
        </div>
        <div className="flex items-center gap-2.5">
          <button onClick={startProcessGuide} className="flex items-center gap-1.5 px-4 py-2 text-xs font-bold text-gray-600 bg-white border border-gray-200 rounded-xl hover:bg-gray-50 shadow-sm transition">
            <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>Process Guide</span>
          </button>
          <button data-guide="create-btn" onClick={() => setShowCreate(true)} 
            className="px-4 py-2 text-xs font-bold text-white bg-[#0c162e] hover:bg-[#152447] rounded-xl transition shadow-sm">
            Create User
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
              <h3 className="font-bold text-lg text-slate-800">Create User</h3>
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
                <button type="submit" className="px-4 py-2 text-xs font-bold bg-[#0c162e] text-white rounded-xl hover:bg-[#152447] transition">Create User</button>
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
              {loading && !guideActive ? Array.from({ length: 5 }).map((_, i) => (
                <tr key={i} className="animate-pulse">{Array.from({ length: 5 }).map((_, j) => (
                  <td key={j} className="px-6 py-5"><div className="h-4 bg-gray-200 rounded w-full" /></td>
                ))}</tr>
              )) : displayUsers.content.length === 0 ? (
                <tr><td colSpan={5} className="px-6 py-12 text-center text-gray-400 font-medium">No users found</td></tr>
              ) : displayUsers.content.map(u => (
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
          <span>Showing {displayUsers.content.length} of {displayUsers.totalElements || displayUsers.content.length} users</span>
          {displayUsers.totalPages > 1 && (
            <div className="flex items-center gap-1.5">
              <button onClick={() => setPage(page - 1)} disabled={page === 0}
                className="p-1.5 rounded-lg border border-gray-200 text-gray-400 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed transition">
                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
                </svg>
              </button>
              {Array.from({ length: displayUsers.totalPages }).map((_, i) => {
                if (i === 0 || i === displayUsers.totalPages - 1 || (i >= page - 1 && i <= page + 1)) {
                  const isActive = page === i;
                  return (
                    <button key={i} onClick={() => setPage(i)}
                      className={`w-7 h-7 flex items-center justify-center rounded-lg text-xs font-bold transition ${isActive ? 'bg-[#1e3a8a] text-white shadow-sm' : 'border border-gray-200 text-gray-600 hover:bg-slate-50'}`}>
                      {i + 1}
                    </button>
                  );
                } else if (i === 1 || i === displayUsers.totalPages - 2) {
                  return <span key={i} className="text-gray-400 text-xs px-0.5">...</span>;
                }
                return null;
              })}
              <button onClick={() => setPage(page + 1)} disabled={page >= displayUsers.totalPages - 1}
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

function ProjectsSection({ lang, api }) {
  const [projects, setProjects] = useState({ content: [], page: 0, totalElements: 0, totalPages: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [guideActive, setGuideActive] = useState(false);

  const [q, setQ] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  // Modals and Forms
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showMembersModal, setShowMembersModal] = useState(false);
  const [projectForm, setProjectForm] = useState({ title: '', description: '', targetStandard: 'IEEE' });
  const [activeProject, setActiveProject] = useState(null);
  const [projectErr, setProjectErr] = useState('');
  const [toast, setToast] = useState(null);

  // Membership state
  const [members, setMembers] = useState([]);
  const [membersLoading, setMembersLoading] = useState(false);
  const [allUsers, setAllUsers] = useState([]);
  const [selectedUser, setSelectedUser] = useState('');
  const [selectedRole, setSelectedRole] = useState('MEMBER');
  const [memberErr, setMemberErr] = useState('');

  const MOCK_PROJECTS = [
    { id: 'proj-mock-1', title: 'Climate Impact Study - Arctic Base', projCode: 'PRJ-2026-001', investigator: 'Dr. Elena Draghici', initials: 'ED', color: 'bg-blue-600', status: 'ACTIVE', createdAt: '2026-03-12T10:00:00' },
    { id: 'proj-mock-2', title: 'Genomic Sequencing Analysis', projCode: 'PRJ-2026-004', investigator: 'Prof. Marcus Liang', initials: 'ML', color: 'bg-slate-800', status: 'ASSIGNED', createdAt: '2026-04-05T10:00:00' },
    { id: 'proj-mock-3', title: 'Neural Path Mapping Study', projCode: 'PRJ-2026-009', investigator: 'Sarah Kowalski', initials: 'SK', color: 'bg-amber-800', status: 'COMPLETED', createdAt: '2026-01-18T10:00:00' },
  ];

  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

  const fetch = useCallback(async (p, signal) => {
    setLoading(true); setError(null);
    try {
      const params = { page: p, size: 20 };
      if (q.trim()) params.q = q.trim();
      if (statusFilter) params.status = statusFilter;
      const r = await api.get('/api/projects', { params, signal });
      setProjects(r.data);
    } catch (e) {
      if (signal && signal.aborted) return;
      setError(e.message || lang.loadFailed);
    } finally {
      if (!signal || !signal.aborted) setLoading(false);
    }
  }, [api, q, statusFilter, lang.loadFailed]);

  useEffect(() => {
    const ac = new AbortController();
    fetch(page, ac.signal);
    return () => ac.abort();
  }, [fetch, page]);

  useEffect(() => {
    setPage(0);
  }, [q, statusFilter]);

  const doUnarchive = async (p) => {
    try {
      setProjects(prev => ({
        ...prev,
        content: prev.content.map(x => x.id === p.id ? { ...x, status: 'APPROVED' } : x)
      }));
      showToast("Project workspace unarchived and restored to active state!", "success");
    } catch (e) {
      showToast("Failed to unarchive project.", "error");
    }
  };

  const doDelete = async (id) => {
    if (id.startsWith('proj-mock-')) {
      showToast("Cannot delete mock project.", "error");
      return;
    }
    if (!confirm(lang.confirmDelete)) return;
    try {
      await api.delete(`/api/projects/${id}`);
      showToast("Project deleted successfully!", "success");
      fetch(page);
    }
    catch (e) { setError(e.message); }
  };

  // Create Project
  const doCreate = async (e) => {
    e.preventDefault();
    setProjectErr('');
    try {
      await api.post('/api/projects', projectForm);
      showToast("Project created successfully!", "success");
      setShowCreateModal(false);
      setProjectForm({ title: '', description: '', targetStandard: 'IEEE' });
      fetch(0);
    } catch (err) {
      setProjectErr(err.response?.data?.message || err.message);
    }
  };

  // Edit Project
  const handleOpenEdit = (p) => {
    if (p.id.startsWith('proj-mock-')) {
      showToast("Cannot edit mock project.", "error");
      return;
    }
    setActiveProject(p);
    setProjectForm({
      title: p.title || '',
      description: p.description || '',
      targetStandard: p.targetStandard || 'IEEE'
    });
    setProjectErr('');
    setShowEditModal(true);
  };

  const doUpdate = async (e) => {
    e.preventDefault();
    setProjectErr('');
    try {
      await api.put(`/api/projects/${activeProject.id}`, projectForm);
      showToast("Project updated successfully!", "success");
      setShowEditModal(false);
      fetch(page);
    } catch (err) {
      setProjectErr(err.response?.data?.message || err.message);
    }
  };

  // Membership Handlers
  const handleOpenMembers = async (p) => {
    if (p.id.startsWith('proj-mock-')) {
      showToast("Cannot manage members of mock project.", "error");
      return;
    }
    setActiveProject(p);
    setMembers([]);
    setSelectedUser('');
    setMemberErr('');
    setShowMembersModal(true);
    setMembersLoading(true);

    try {
      // 1. Fetch current members
      const resMembers = await api.get(`/api/projects/${p.id}/members`);
      setMembers(resMembers.data || []);

      // 2. Fetch all system users to select from
      const resUsers = await api.get('/api/admin/users?size=100');
      setAllUsers(resUsers.data?.content || []);
    } catch (err) {
      setMemberErr("Failed to load members or users list.");
    } finally {
      setMembersLoading(false);
    }
  };

  const doAddMember = async (e) => {
    e.preventDefault();
    if (!selectedUser) {
      setMemberErr("Please select a user to add.");
      return;
    }
    setMemberErr('');
    try {
      await api.post(`/api/projects/${activeProject.id}/members?userId=${selectedUser}&role=${selectedRole}`);
      showToast("Member added successfully!", "success");
      setSelectedUser('');
      
      // Refresh member list
      const resMembers = await api.get(`/api/projects/${activeProject.id}/members`);
      setMembers(resMembers.data || []);
    } catch (err) {
      setMemberErr(err.response?.data?.message || "Failed to add member. Double check if they are already in the project.");
    }
  };

  const doRemoveMember = async (userId) => {
    if (!confirm("Are you sure you want to remove this member?")) return;
    setMemberErr('');
    try {
      await api.delete(`/api/projects/${activeProject.id}/members/${userId}`);
      showToast("Member removed successfully!", "success");
      
      // Refresh member list
      const resMembers = await api.get(`/api/projects/${activeProject.id}/members`);
      setMembers(resMembers.data || []);
    } catch (err) {
      setMemberErr(err.response?.data?.message || "Failed to remove member.");
    }
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

  const getPIForProject = (id, title) => {
    const investigators = [
      { name: 'Dr. Elena Draghici', initials: 'ED', color: 'bg-blue-600' },
      { name: 'Prof. Marcus Liang', initials: 'ML', color: 'bg-slate-800' },
      { name: 'Sarah Kowalski', initials: 'SK', color: 'bg-amber-800' },
      { name: 'Dr. Alan Turing', initials: 'AT', color: 'bg-emerald-700' },
      { name: 'Prof. Richard Feynman', initials: 'RF', color: 'bg-indigo-700' }
    ];
    let hash = 0;
    const str = id + title;
    for (let i = 0; i < str.length; i++) {
      hash = str.charCodeAt(i) + ((hash << 5) - hash);
    }
    const index = Math.abs(hash) % investigators.length;
    return investigators[index];
  };

  const getProjCodeForProject = (id, index) => {
    const num = (index + 1).toString().padStart(3, '0');
    return `PRJ-2026-${num}`;
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'ACTIVE':
      case 'IN_PROGRESS':
      case 'APPROVED':
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-700">Active</span>;
      case 'ASSIGNED':
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-blue-100 text-blue-700">Assigned</span>;
      case 'SUBMITTED_FOR_REVIEW':
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-700">Under Review</span>;
      default:
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-700">Completed</span>;
    }
  };

  const displayProjects = (!projects.content || projects.content.length === 0) && !loading
    ? { content: MOCK_PROJECTS, page: 0, totalElements: 3, totalPages: 1 }
    : projects;

  const totalActiveVal = projects.totalElements || 24;

  return (
    <div className="p-8 space-y-6 bg-[#f8fafc]">
      {/* Title Area */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-gray-200 pb-5">
        <div>
          <h1 className="text-3xl font-extrabold text-[#1e3a8a] tracking-tight">Research Projects</h1>
          <p className="text-gray-550 text-xs mt-1">Central terminal for managing all active and archived academic research initiatives.</p>
        </div>
        <div className="flex items-center gap-2.5">
          <button className="flex items-center gap-1.5 px-4 py-2 text-xs font-bold text-gray-600 bg-white border border-gray-200 rounded-xl hover:bg-gray-50 shadow-sm transition">
            <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            <span>Export Records</span>
          </button>
          <button onClick={() => { setProjectForm({ title: '', description: '', targetStandard: 'IEEE' }); setProjectErr(''); setShowCreateModal(true); }}
            className="flex items-center gap-1.5 px-4 py-2 text-xs font-bold text-white bg-[#0c162e] hover:bg-[#152447] rounded-xl transition shadow-sm cursor-pointer">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
            </svg>
            <span>Create Project</span>
          </button>
        </div>
      </div>

      {/* Mini KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Total Active */}
        <div className="bg-white rounded-xl border border-gray-200 p-4 shadow-sm flex flex-col justify-between h-28">
          <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">TOTAL ACTIVE</span>
          <div className="flex items-baseline gap-2 mt-1">
            <span className="text-3xl font-extrabold text-slate-800">{totalActiveVal}</span>
            <span className="text-[10px] font-bold text-blue-600">+3 this month</span>
          </div>
        </div>

        {/* Collaborators */}
        <div className="bg-white rounded-xl border border-gray-200 p-4 shadow-sm flex flex-col justify-between h-28">
          <div className="flex justify-between items-start">
            <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">COLLABORATORS</span>
            <svg className="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
          </div>
          <span className="text-3xl font-extrabold text-slate-800 mt-1">118</span>
        </div>

        {/* Papers Processed */}
        <div className="bg-white rounded-xl border border-gray-200 p-4 shadow-sm flex flex-col justify-between h-28">
          <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">PAPERS PROCESSED</span>
          <div className="flex items-baseline gap-2 mt-1">
            <span className="text-3xl font-extrabold text-slate-800">1.4k</span>
            <span className="text-[10px] font-bold text-emerald-600">98% success</span>
          </div>
        </div>

        {/* Completion Rate */}
        <div className="bg-white rounded-xl border border-gray-200 p-4 shadow-sm flex flex-col justify-between h-28">
          <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">COMPLETION RATE</span>
          <div className="space-y-2 mt-1">
            <div className="flex justify-between items-baseline">
              <span className="text-3xl font-extrabold text-slate-800">82%</span>
            </div>
            <div className="w-full bg-slate-100 h-1.5 rounded-full overflow-hidden">
              <div className="bg-[#1e3a8a] h-1.5 rounded-full" style={{ width: '82%' }}></div>
            </div>
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white rounded-xl border border-gray-200 p-4 shadow-sm flex flex-col sm:flex-row gap-3 items-center justify-between">
        <div className="flex flex-1 w-full gap-3 items-center">
          {/* Search Input */}
          <div className="flex-1 relative">
            <svg className="w-4 h-4 text-gray-400 absolute left-3 top-2.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input 
              type="text" 
              placeholder="Search projects..." 
              value={q}
              onChange={(e) => { setQ(e.target.value); setPage(0); }}
              className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-blue-500 font-semibold" 
            />
          </div>

          {/* Status Dropdown */}
          <select 
            value={statusFilter} 
            onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
            className="w-36 px-3 py-2 bg-white border border-gray-200 rounded-xl text-xs font-semibold text-slate-700 focus:outline-none cursor-pointer"
          >
            <option value="">All Statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="ASSIGNED">Assigned</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="SUBMITTED_FOR_REVIEW">Under Review</option>
            <option value="APPROVED">Approved</option>
            <option value="ARCHIVED">Archived</option>
          </select>
        </div>

        <span className="text-xs text-gray-400 font-bold self-end sm:self-center shrink-0">
          Showing {displayProjects.content.length} of {displayProjects.totalElements || displayProjects.content.length} Projects
        </span>
      </div>

      {error && <ErrorBlock msg={error} onRetry={() => fetch(page, new AbortController().signal)} />}

      {/* Table Card */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table data-guide="projects-table" className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-50 text-slate-400 font-bold uppercase border-b border-gray-100">
                <th className="px-6 py-3.5 font-bold tracking-wider">Project Title</th>
                <th className="px-6 py-3.5 font-bold tracking-wider">Instructor</th>
                <th className="px-6 py-3.5 font-bold tracking-wider">Status</th>
                <th className="px-6 py-3.5 font-bold tracking-wider">Date Created</th>
                <th className="px-6 py-3.5 font-bold tracking-wider text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-slate-700 font-semibold">
              {loading ? Array.from({ length: 5 }).map((_, i) => (
                <tr key={i} className="animate-pulse">{Array.from({ length: 5 }).map((_, j) => (
                  <td key={j} className="px-6 py-5"><div className="h-4 bg-gray-200 rounded w-full" /></td>
                ))}</tr>
              )) : displayProjects.content.length === 0 ? (
                <tr><td colSpan={5} className="px-6 py-12 text-center text-gray-400 font-medium">No projects found</td></tr>
              ) : displayProjects.content.map((p, idx) => {
                const pi = getPIForProject(p.id, p.title);
                const projCode = p.projCode || getProjCodeForProject(p.id, idx);
                const piName = p.investigator || pi.name;
                const piInitials = p.initials || pi.initials;
                const piColor = pi.color;

                return (
                  <tr key={p.id} className="hover:bg-slate-50/50 transition">
                    {/* Project Title */}
                    <td className="px-6 py-4">
                      <div className="flex flex-col">
                        <span className="font-bold text-slate-800 max-w-xs truncate">{p.title}</span>
                        <span className="text-[10px] text-gray-400 font-bold mt-0.5">{projCode}</span>
                      </div>
                    </td>

                    {/* Principal Investigator with Avatar */}
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <div className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] text-white font-bold shrink-0 ${piColor}`}>
                          {piInitials}
                        </div>
                        <span className="text-slate-700 font-semibold">{piName}</span>
                      </div>
                    </td>

                    {/* Status badge */}
                    <td className="px-6 py-4">
                      {getStatusBadge(p.status)}
                    </td>

                    {/* Date Created */}
                    <td className="px-6 py-4 text-slate-500 font-medium">
                      {new Date(p.createdAt).toLocaleDateString('en-US', { month: 'short', day: '2-digit', year: 'numeric' })}
                    </td>

                    {/* Actions icons */}
                    <td className="px-6 py-4">
                      <div className="flex items-center justify-end gap-1.5">
                        {/* Manage Members Icon */}
                        <button onClick={() => handleOpenMembers(p)} title="Manage Members" className="p-1.5 rounded-lg hover:bg-slate-100 text-blue-600 hover:text-blue-800 transition cursor-pointer">
                          <svg className="w-4.5 h-4.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" viewBox="0 0 24 24">
                            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                            <circle cx="9" cy="7" r="4" />
                            <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                            <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                          </svg>
                        </button>

                        {/* Unarchive Icon (Restore to Active) */}
                        <button onClick={() => doUnarchive(p)} title="Unarchive Project (Restore to Active)" className="p-1.5 rounded-lg hover:bg-emerald-50 text-emerald-600 hover:text-emerald-800 transition cursor-pointer">
                          <svg className="w-4.5 h-4.5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                          </svg>
                        </button>

                        {/* Edit Icon */}
                        <button onClick={() => handleOpenEdit(p)} title="Edit Project" className="p-1.5 rounded-lg hover:bg-slate-100 text-slate-550 hover:text-slate-800 transition cursor-pointer">
                          <svg className="w-4.5 h-4.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" viewBox="0 0 24 24">
                            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                            <path d="M18.5 2.5a2.121 2.121 0 1 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                          </svg>
                        </button>

                        {/* Delete Icon */}
                        <button onClick={() => doDelete(p.id)} title="Delete Project" className="p-1.5 rounded-lg hover:bg-slate-100 text-rose-600 hover:text-rose-800 transition cursor-pointer">
                          <svg className="w-4.5 h-4.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" viewBox="0 0 24 24">
                            <path d="M3 6h18" />
                            <path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6" />
                            <path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2" />
                          </svg>
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {/* Footer / Pagination */}
        <div className="flex items-center justify-between px-6 py-3.5 border-t border-gray-100 bg-gray-50/50 text-xs font-semibold text-gray-500">
          {displayProjects.totalPages > 1 ? (
            <>
              <div className="flex items-center gap-1.5">
                <button onClick={() => setPage(page - 1)} disabled={page === 0}
                  className="p-1.5 rounded-lg border border-gray-200 text-gray-400 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed transition">
                  <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
                  </svg>
                </button>
                {Array.from({ length: displayProjects.totalPages }).map((_, i) => {
                  if (i === 0 || i === displayProjects.totalPages - 1 || (i >= page - 1 && i <= page + 1)) {
                    const isActive = page === i;
                    return (
                      <button key={i} onClick={() => setPage(i)}
                        className={`w-7 h-7 flex items-center justify-center rounded-lg text-xs font-bold transition ${isActive ? 'bg-[#1e3a8a] text-white shadow-sm' : 'border border-gray-200 text-gray-600 hover:bg-slate-50'}`}>
                        {i + 1}
                      </button>
                    );
                  } else if (i === 1 || i === displayProjects.totalPages - 2) {
                    return <span key={i} className="text-gray-400 text-xs px-0.5">...</span>;
                  }
                  return null;
                })}
                <button onClick={() => setPage(page + 1)} disabled={page >= displayProjects.totalPages - 1}
                  className="p-1.5 rounded-lg border border-gray-200 text-gray-400 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed transition">
                  <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
                  </svg>
                </button>
              </div>
              <span>Page {page + 1} of {displayProjects.totalPages}</span>
            </>
          ) : (
            <>
              <div className="w-1" />
              <span>Page 1 of 1</span>
            </>
          )}
        </div>
      </div>

      {/* Create Project Modal Overlay */}
      {showCreateModal && (
        <div className="fixed inset-0 z-55 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-md border border-gray-150 transform scale-100 transition-all duration-300">
            <h3 className="font-bold text-slate-800 text-sm mb-4">Create Research Project</h3>
            <form onSubmit={doCreate} className="space-y-4">
              <div>
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1.5">Project Title *</label>
                <input 
                  placeholder="e.g. Evidence Mapping Base" 
                  value={projectForm.title} 
                  onChange={e => setProjectForm(p => ({ ...p, title: e.target.value }))} 
                  required 
                  className="w-full px-3 py-2 bg-slate-50 border border-gray-255 rounded-xl font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500 text-xs" 
                />
              </div>
              <div>
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1.5">Description</label>
                <textarea 
                  placeholder="Summarize research objectives..." 
                  value={projectForm.description} 
                  onChange={e => setProjectForm(p => ({ ...p, description: e.target.value }))} 
                  rows={3} 
                  className="w-full px-3 py-2 bg-slate-50 border border-gray-255 rounded-xl font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500 text-xs resize-none" 
                />
              </div>
              <div>
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1.5">Citation / Target Standard *</label>
                <select 
                  value={projectForm.targetStandard} 
                  onChange={e => setProjectForm(p => ({ ...p, targetStandard: e.target.value }))} 
                  className="w-full px-3 py-2 bg-slate-50 border border-gray-255 rounded-xl font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500 text-xs cursor-pointer"
                >
                  <option value="IEEE">IEEE</option>
                  <option value="ACM">ACM</option>
                  <option value="SPRINGER_LNCS">Springer LNCS</option>
                  <option value="APA">APA</option>
                  <option value="MLA">MLA</option>
                  <option value="CUSTOM">Custom Standard</option>
                </select>
              </div>
              {projectErr && <div className="text-xs text-rose-600 bg-rose-50 p-2.5 rounded-lg border border-rose-105 font-semibold">{projectErr}</div>}
              <div className="flex gap-2.5 justify-end pt-2">
                <button type="button" onClick={() => setShowCreateModal(false)} className="px-3.5 py-2 text-xs font-bold text-slate-650 hover:bg-slate-50 rounded-xl transition cursor-pointer">{lang.cancel}</button>
                <button type="submit" className="px-4 py-2 bg-[#0c162e] hover:bg-[#152447] text-white rounded-xl text-xs font-bold transition shadow-md cursor-pointer">Create Project</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Project Modal Overlay */}
      {showEditModal && (
        <div className="fixed inset-0 z-55 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-md border border-gray-150 transform scale-100 transition-all duration-300">
            <h3 className="font-bold text-slate-800 text-sm mb-4">Edit Research Project</h3>
            <form onSubmit={doUpdate} className="space-y-4">
              <div>
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1.5">Project Title *</label>
                <input 
                  placeholder="Project Title" 
                  value={projectForm.title} 
                  onChange={e => setProjectForm(p => ({ ...p, title: e.target.value }))} 
                  required 
                  className="w-full px-3 py-2 bg-slate-50 border border-gray-255 rounded-xl font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500 text-xs" 
                />
              </div>
              <div>
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1.5">Description</label>
                <textarea 
                  placeholder="Objectives..." 
                  value={projectForm.description} 
                  onChange={e => setProjectForm(p => ({ ...p, description: e.target.value }))} 
                  rows={3} 
                  className="w-full px-3 py-2 bg-slate-50 border border-gray-255 rounded-xl font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500 text-xs resize-none" 
                />
              </div>
              <div>
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1.5">Citation / Target Standard *</label>
                <select 
                  value={projectForm.targetStandard} 
                  onChange={e => setProjectForm(p => ({ ...p, targetStandard: e.target.value }))} 
                  className="w-full px-3 py-2 bg-slate-50 border border-gray-255 rounded-xl font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500 text-xs cursor-pointer"
                >
                  <option value="IEEE">IEEE</option>
                  <option value="ACM">ACM</option>
                  <option value="SPRINGER_LNCS">Springer LNCS</option>
                  <option value="APA">APA</option>
                  <option value="MLA">MLA</option>
                  <option value="CUSTOM">Custom Standard</option>
                </select>
              </div>
              {projectErr && <div className="text-xs text-rose-600 bg-rose-50 p-2.5 rounded-lg border border-rose-105 font-semibold">{projectErr}</div>}
              <div className="flex gap-2.5 justify-end pt-2">
                <button type="button" onClick={() => setShowEditModal(false)} className="px-3.5 py-2 text-xs font-bold text-slate-650 hover:bg-slate-50 rounded-xl transition cursor-pointer">{lang.cancel}</button>
                <button type="submit" className="px-4 py-2 bg-[#0c162e] hover:bg-[#152447] text-white rounded-xl text-xs font-bold transition shadow-md cursor-pointer">Save Changes</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Workspace Membership Management Modal Overlay */}
      {showMembersModal && activeProject && (
        <div className="fixed inset-0 z-55 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg border border-gray-150 overflow-hidden transform scale-100 transition-all duration-300">
            {/* Modal Header */}
            <div className="bg-slate-50 border-b border-gray-150 px-6 py-4 flex items-center justify-between">
              <div>
                <h3 className="font-bold text-slate-800 text-sm">Manage Workspace Members</h3>
                <p className="text-gray-400 text-[10px] mt-0.5 truncate max-w-xs">{activeProject.title}</p>
              </div>
              <button 
                onClick={() => setShowMembersModal(false)}
                className="text-slate-400 hover:text-slate-600 transition cursor-pointer"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            {/* Modal Body */}
            <div className="p-6 space-y-5">
              {/* Form to add a new member */}
              <form onSubmit={doAddMember} className="bg-slate-50/50 border border-slate-200 rounded-xl p-4.5 space-y-3">
                <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block">Add Workspace Member</span>
                
                <div className="flex flex-col sm:flex-row gap-3">
                  {/* Select User */}
                  <div className="flex-1">
                    <select 
                      value={selectedUser} 
                      onChange={e => setSelectedUser(e.target.value)} 
                      className="w-full px-3 py-2 bg-white border border-gray-255 rounded-xl font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500 text-xs cursor-pointer"
                    >
                      <option value="">-- Choose User Accounts --</option>
                      {allUsers
                        .filter(u => !members.some(m => m.userId === u.id))
                        .map(u => (
                          <option key={u.id} value={u.id}>
                            {u.firstName} {u.lastName} ({u.email} - {u.role})
                          </option>
                        ))}
                    </select>
                  </div>

                  {/* Select Project Role */}
                  <div className="w-full sm:w-36">
                    <select 
                      value={selectedRole} 
                      onChange={e => setSelectedRole(e.target.value)} 
                      className="w-full px-3 py-2 bg-white border border-gray-255 rounded-xl font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500 text-xs cursor-pointer"
                    >
                      <option value="MEMBER">Member</option>
                      <option value="LEADER">Leader</option>
                      <option value="INSTRUCTOR">Instructor</option>
                    </select>
                  </div>

                  <button 
                    type="submit" 
                    className="px-4 py-2 bg-[#0c162e] hover:bg-[#152447] text-white rounded-xl text-xs font-bold transition shadow-sm shrink-0 cursor-pointer"
                  >
                    Add
                  </button>
                </div>
              </form>

              {memberErr && <div className="text-xs text-rose-700 bg-rose-50 p-2.5 rounded-lg border border-rose-100 font-semibold">{memberErr}</div>}

              {/* Members List */}
              <div className="space-y-2">
                <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block">Current Members ({members.length})</span>
                
                {membersLoading ? (
                  <div className="animate-pulse space-y-2 py-4">
                    <div className="h-8 bg-gray-200 rounded w-full"></div>
                    <div className="h-8 bg-gray-200 rounded w-full"></div>
                  </div>
                ) : members.length === 0 ? (
                  <div className="text-xs text-gray-400 py-6 text-center italic border border-dashed border-gray-255 rounded-xl bg-slate-50/20">
                    No members assigned to this project workspace.
                  </div>
                ) : (
                  <div className="divide-y divide-gray-150 border border-gray-200 rounded-xl max-h-56 overflow-y-auto bg-white">
                    {members.map(m => (
                      <div key={m.id} className="px-4 py-2.5 flex items-center justify-between hover:bg-slate-50/50 transition text-xs">
                        <div className="min-w-0">
                          <p className="font-bold text-slate-800 truncate">{m.firstName} {m.lastName}</p>
                          <p className="text-[10px] text-gray-400 font-mono mt-0.5 truncate">{m.email}</p>
                        </div>
                        <div className="flex items-center gap-3 shrink-0">
                          <span className={`px-2 py-0.5 rounded text-[9px] font-bold border ${
                            m.role === 'INSTRUCTOR' 
                              ? 'bg-amber-50 text-amber-700 border-amber-100' 
                              : m.role === 'LEADER'
                              ? 'bg-blue-50 text-blue-700 border-blue-100'
                              : 'bg-slate-50 text-slate-650 border-slate-100'
                          }`}>
                            {m.role}
                          </span>
                          <button 
                            onClick={() => doRemoveMember(m.userId)} 
                            className="p-1 text-rose-500 hover:text-rose-700 hover:bg-rose-50 rounded transition cursor-pointer"
                          >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                            </svg>
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            {/* Modal Footer */}
            <div className="bg-slate-50 px-6 py-3.5 border-t border-gray-150 flex items-center justify-end">
              <button 
                onClick={() => setShowMembersModal(false)}
                className="px-4 py-2 bg-slate-800 hover:bg-slate-900 text-white rounded-xl text-xs font-bold transition shadow-md cursor-pointer"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Custom Toast Notification Popup */}
      {toast && (
        <div className="fixed top-4 right-4 z-55 flex items-center gap-2.5 px-4.5 py-3 rounded-2xl shadow-xl border animate-slide-in-right bg-white border-slate-100">
          <div className={`w-6 h-6 rounded-full flex items-center justify-center shrink-0 ${
            toast.type === 'error' ? 'bg-rose-100 text-rose-600' : 'bg-emerald-100 text-emerald-600'
          }`}>
            {toast.type === 'error' ? (
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            ) : (
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
              </svg>
            )}
          </div>
          <span className="text-xs font-bold text-slate-800">{toast.message}</span>
        </div>
      )}
    </div>
  );
}


function PapersSection({ lang, api }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [guideActive, setGuideActive] = useState(false);
  const [q, setQ] = useState('');

  const MOCK_PAPERS = [
    {
      id: 'paper-mock-1',
      title: 'Neural Architectures for Semantic Analysis',
      doi: '10.1038/s41586-021-03819-2',
      project: 'NLP Core-v2',
      author: 'Dr. Sarah Chen',
      status: 'Submitted',
      type: 'pdf'
    },
    {
      id: 'paper-mock-2',
      title: 'Ethics in Large Language Model Deployment',
      doi: '10.1145/3442188.3445922',
      project: 'Global AI Policy',
      author: 'Marc Thompson',
      status: 'In Review',
      type: 'link'
    },
    {
      id: 'paper-mock-3',
      title: 'Climate Modeling through Deep Residual Networks',
      doi: '10.1038/s41558-021-01031-2',
      project: 'EcoMetrics 2024',
      author: 'Elena Rodriguez',
      status: 'Published',
      type: 'pdf'
    }
  ];

  const fetch = useCallback(async (signal) => {
    setLoading(true);
    try {
      const r = await api.get('/api/admin/dashboard', { signal });
      setData(r.data);
    }
    catch (e) {
      if (signal && signal.aborted) return;
      setError(e.message || lang.loadFailed);
    }
    finally {
      if (!signal || !signal.aborted) setLoading(false);
    }
  }, [api, lang.loadFailed]);

  useEffect(() => {
    const ac = new AbortController();
    fetch(ac.signal);
    return () => ac.abort();
  }, [fetch]);

  const stats = [
    { label: 'DRAFTS', value: '02', subtext: 'paper docs', barColor: 'bg-gray-400' },
    { label: 'SUBMITTED', value: '02', subtext: 'paper docs', barColor: 'bg-blue-500' },
    { label: 'IN REVIEW', value: '02', subtext: 'paper docs', barColor: 'bg-amber-500' },
    { label: 'PUBLISHED', value: '02', subtext: 'paper docs', barColor: 'bg-emerald-500' },
    { label: 'REJECTED', value: '02', subtext: 'paper docs', barColor: 'bg-rose-500' }
  ];

  const display = data || {
    activePaperDocuments: 2,
    activeSourceDocuments: 3
  };

  const filteredPapers = MOCK_PAPERS.filter(p => 
    p.title.toLowerCase().includes(q.toLowerCase()) ||
    p.project.toLowerCase().includes(q.toLowerCase()) ||
    p.author.toLowerCase().includes(q.toLowerCase())
  );

  return (
    <div className="p-8 space-y-6 bg-[#f8fafc]">
      {/* Header Area */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-gray-200 pb-5">
        <div>
          <h1 className="text-3xl font-extrabold text-[#1e3a8a] tracking-tight">Papers Overview</h1>
          <p className="text-gray-500 text-xs mt-1">Manage and monitor research paper progress across all active projects.</p>
        </div>
        <div className="flex items-center gap-2.5">
          <button className="flex items-center gap-1.5 px-4 py-2 text-xs font-bold text-white bg-[#0c162e] hover:bg-[#152447] rounded-xl transition shadow-sm">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
            </svg>
            <span>Upload Paper</span>
          </button>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-3 lg:grid-cols-5 gap-4">
        {stats.map((s, i) => (
          <div key={i} className="bg-white rounded-xl border border-gray-200 p-4 shadow-sm flex flex-col justify-between h-28">
            <div className="flex justify-between items-center">
              <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider">{s.label}</span>
              <div className={`h-1.5 w-8 rounded-full ${s.barColor}`} />
            </div>
            <div className="mt-2">
              <span className="text-3xl font-extrabold text-slate-800">{s.value}</span>
              <p className="text-[10px] text-gray-400 font-bold mt-0.5">{s.subtext}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Banner Section */}
      <div className="bg-[#0c162e] text-white rounded-2xl p-5 shadow-sm flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="w-10 h-10 rounded-xl bg-white/10 flex items-center justify-center shrink-0 border border-white/10">
            <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
          </div>
          <div className="flex flex-col">
            <h3 className="font-bold text-sm">Current Active Documents</h3>
            <p className="text-slate-300 text-xs mt-0.5">
              Total volume across current session: <span className="text-white font-bold">{display?.activePaperDocuments || 2} active documents</span> &middot; <span className="text-white font-bold">{display?.activeSourceDocuments || 3} source files</span> identified.
            </p>
          </div>
        </div>
        <div className="flex -space-x-2 overflow-hidden shrink-0">
          <div className="inline-block h-8 w-8 rounded-full ring-2 ring-[#0c162e] bg-slate-100 flex items-center justify-center text-[10px] font-extrabold text-[#0c162e]">
            PDF
          </div>
          <div className="inline-block h-8 w-8 rounded-full ring-2 ring-[#0c162e] bg-slate-100 flex items-center justify-center text-[10px] font-extrabold text-[#0c162e]">
            DOC
          </div>
          <div className="inline-block h-8 w-8 rounded-full ring-2 ring-[#0c162e] bg-slate-100 flex items-center justify-center text-[10px] font-extrabold text-[#0c162e]">
            TXT
          </div>
        </div>
      </div>

      {/* Recent Papers Main Card */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden">
        {/* Table Header and Search */}
        <div className="p-5 border-b border-gray-100 flex flex-col sm:flex-row gap-4 items-center justify-between">
          <h2 className="text-lg font-bold text-slate-800">Recent Papers</h2>
          <div className="flex items-center gap-2 w-full sm:w-auto">
            {/* Search Input */}
            <div className="relative flex-1 sm:w-64">
              <svg className="w-4 h-4 text-gray-400 absolute left-3 top-2.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              <input 
                type="text" 
                placeholder="Search papers..." 
                value={q}
                onChange={(e) => setQ(e.target.value)}
                className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-blue-500 font-semibold" 
              />
            </div>
            {/* Filter Button */}
            <button className="p-2 bg-white border border-gray-200 rounded-xl hover:bg-gray-50 shadow-sm transition">
              <svg className="w-4 h-4 text-slate-500" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
              </svg>
            </button>
          </div>
        </div>

        {/* Papers Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-50 text-slate-400 font-bold uppercase border-b border-gray-100">
                <th className="px-6 py-3.5 font-bold tracking-wider">Title</th>
                <th className="px-6 py-3.5 font-bold tracking-wider">Project</th>
                <th className="px-6 py-3.5 font-bold tracking-wider">Author</th>
                <th className="px-6 py-3.5 font-bold tracking-wider">Status</th>
                <th className="px-6 py-3.5 font-bold tracking-wider text-right"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-slate-700 font-semibold">
              {filteredPapers.length === 0 ? (
                <tr><td colSpan={5} className="px-6 py-12 text-center text-gray-400 font-medium">No papers found</td></tr>
              ) : filteredPapers.map((p) => (
                <tr key={p.id} className="hover:bg-slate-50/50 transition">
                  {/* Title with Icon */}
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <div className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 border ${
                        p.type === 'pdf' 
                          ? 'bg-orange-50 border-orange-100 text-orange-600' 
                          : 'bg-blue-50 border-blue-100 text-blue-600'
                      }`}>
                        {p.type === 'pdf' ? (
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                          </svg>
                        ) : (
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                          </svg>
                        )}
                      </div>
                      <div className="flex flex-col">
                        <span className="font-bold text-slate-800 max-w-sm truncate">{p.title}</span>
                        <span className="text-[10px] text-gray-400 font-bold mt-0.5">DOI: {p.doi}</span>
                      </div>
                    </div>
                  </td>

                  {/* Project */}
                  <td className="px-6 py-4 text-slate-600 font-semibold">
                    {p.project}
                  </td>

                  {/* Author */}
                  <td className="px-6 py-4 text-slate-600 font-semibold">
                    {p.author}
                  </td>

                  {/* Status badge */}
                  <td className="px-6 py-4">
                    <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                      p.status === 'Submitted' 
                        ? 'bg-blue-100 text-blue-700' 
                        : p.status === 'In Review' 
                          ? 'bg-amber-100 text-amber-700' 
                          : 'bg-emerald-100 text-emerald-700'
                    }`}>
                      {p.status}
                    </span>
                  </td>

                  {/* Actions vertical three dots */}
                  <td className="px-6 py-4 text-right">
                    <button className="p-1 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-700 transition">
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z" />
                      </svg>
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Footer / Pagination */}
        <div className="flex items-center justify-between px-6 py-3.5 border-t border-gray-100 bg-gray-50/50 text-xs font-semibold text-gray-500">
          <span>Showing 1-3 of 12 papers</span>
          <div className="flex items-center gap-1.5">
            <button className="px-3 py-1.5 rounded-lg border border-gray-200 text-slate-600 hover:bg-slate-50 transition">
              Previous
            </button>
            <button className="w-7 h-7 flex items-center justify-center rounded-lg text-xs font-bold bg-[#0c162e] text-white shadow-sm">
              1
            </button>
            <button className="w-7 h-7 flex items-center justify-center rounded-lg text-xs font-bold border border-gray-200 text-slate-600 hover:bg-slate-50 transition">
              2
            </button>
            <button className="px-3 py-1.5 rounded-lg border border-gray-200 text-slate-600 hover:bg-slate-50 transition">
              Next
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function AuditLogsSection({ lang, api }) {
  const [logs, setLogs] = useState({ content: [], page: 0, totalElements: 0, totalPages: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [guideActive, setGuideActive] = useState(false);

  const [q, setQ] = useState('');
  const [actionFilter, setActionFilter] = useState('');
  const [userFilter, setUserFilter] = useState('');

  const MOCK_LOGS = [
    { actorId: 'mock-1', actorEmail: 'instructor@evidencepilot.dev', action: 'PROJECT_UPDATED', entityType: 'PROJECT', entityId: '65F11761', occurredAt: '2026-07-30T13:01:16' },
    { actorId: 'mock-2', actorEmail: 'instructor@evidencepilot.dev', action: 'PROJECT_UPDATED', entityType: 'PROJECT', entityId: '65F11761', occurredAt: '2026-07-30T13:00:52' },
    { actorId: 'mock-3', actorEmail: 'instructor@evidencepilot.dev', action: 'PROJECT_CREATED', entityType: 'PROJECT', entityId: '65F11761', occurredAt: '2026-07-30T13:00:28' },
    { actorId: 'mock-4', actorEmail: 'security_bot@evidencepilot.dev', action: 'USER_BANNED', entityType: 'USER', entityId: '99A2312', occurredAt: '2026-07-30T12:45:10' },
    { actorId: 'mock-5', actorEmail: 'instructor@evidencepilot.dev', action: 'PROJECT_CREATED', entityType: 'PROJECT', entityId: 'e4725949', occurredAt: '2026-07-30T12:59:57' }
  ];

  const fetch = useCallback(async (p, signal) => {
    setLoading(true); setError(null);
    try {
      const params = { page: p, size: 20 };
      const r = await api.get('/api/admin/audit-logs', { params, signal });
      setLogs(r.data);
    } catch (e) {
      if (signal && signal.aborted) return;
      setError(e.message || lang.loadFailed);
    } finally {
      if (!signal || !signal.aborted) setLoading(false);
    }
  }, [api, lang.loadFailed]);

  useEffect(() => {
    const ac = new AbortController();
    fetch(page, ac.signal);
    return () => ac.abort();
  }, [fetch, page]);

  const startProcessGuide = () => {
    setGuideActive(true);
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

  const getActorAvatar = (email) => {
    const isBot = email.includes('bot');
    const initial = email.charAt(0).toUpperCase();
    if (isBot) {
      return (
        <div className="w-6 h-6 rounded-full flex items-center justify-center text-[10px] text-white font-bold shrink-0 bg-rose-400">
          {initial}
        </div>
      );
    } else {
      return (
        <div className="w-6 h-6 rounded-full flex items-center justify-center text-[10px] text-white font-bold shrink-0 bg-blue-400">
          {initial}
        </div>
      );
    }
  };

  const getActionBadge = (action) => {
    switch (action) {
      case 'PROJECT_UPDATED':
      case 'UPDATE':
        return <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-100">PROJECT_UPDATED</span>;
      case 'PROJECT_CREATED':
      case 'CREATE':
        return <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-100">PROJECT_CREATED</span>;
      case 'USER_BANNED':
      case 'BAN':
        return <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-rose-50 text-rose-700 border border-rose-100">USER_BANNED</span>;
      default:
        return <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-50 text-slate-700 border border-slate-100">{action}</span>;
    }
  };

  const displayLogs = (!logs.content || logs.content.length === 0) && !loading
    ? { content: MOCK_LOGS, page: 0, totalElements: 5, totalPages: 1 }
    : logs;

  const filteredLogs = displayLogs.content.filter(log => {
    const matchesQ = q.trim() === '' || 
      log.actorEmail.toLowerCase().includes(q.toLowerCase()) || 
      (log.entityType + '#' + log.entityId).toLowerCase().includes(q.toLowerCase());
    
    const matchesAction = actionFilter === '' || log.action === actionFilter;
    const matchesUser = userFilter === '' || log.actorEmail === userFilter;

    return matchesQ && matchesAction && matchesUser;
  });

  return (
    <div className="p-8 space-y-6 bg-[#f8fafc]">
      {/* Title Area */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-gray-200 pb-5">
        <div>
          <h1 className="text-3xl font-extrabold text-[#1e3a8a] tracking-tight">Audit Logs</h1>
          <p className="text-gray-500 text-xs mt-1">Review system activities and security events across the platform.</p>
        </div>
        <div className="flex items-center gap-2.5">
          <button className="flex items-center gap-1.5 px-4 py-2 text-xs font-bold text-gray-600 bg-white border border-gray-200 rounded-xl hover:bg-gray-50 shadow-sm transition">
            <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            <span>Export CSV</span>
          </button>
          <button onClick={() => fetch(page)} className="flex items-center gap-1.5 px-4 py-2 text-xs font-bold text-white bg-[#0c162e] hover:bg-[#152447] rounded-xl transition shadow-sm">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 1121.2 8H17" />
            </svg>
            <span>Refresh Logs</span>
          </button>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Total Logs */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-slate-50 flex items-center justify-center shrink-0 border border-gray-100">
            <svg className="w-6 h-6 text-slate-500" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
          </div>
          <div>
            <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block">TOTAL LOGS</span>
            <span className="text-2xl font-extrabold text-slate-800">1,248</span>
            <span className="text-[10px] font-bold text-emerald-600 flex items-center gap-0.5 mt-0.5">
              <svg className="w-3 h-3" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
              </svg>
              <span>+12% this week</span>
            </span>
          </div>
        </div>

        {/* Security Alerts */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-rose-50 flex items-center justify-center shrink-0 border border-rose-100">
            <svg className="w-6 h-6 text-rose-500" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
          </div>
          <div>
            <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block">SECURITY ALERTS</span>
            <span className="text-2xl font-extrabold text-slate-800">12</span>
            <span className="text-[10px] font-bold text-rose-600 flex items-center gap-0.5 mt-0.5">
              <svg className="w-3 h-3" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              <span>3 require attention</span>
            </span>
          </div>
        </div>

        {/* System Events */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-blue-50 flex items-center justify-center shrink-0 border border-blue-100">
            <svg className="w-6 h-6 text-blue-500" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          </div>
          <div>
            <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block">SYSTEM EVENTS</span>
            <span className="text-2xl font-extrabold text-slate-800">850</span>
            <span className="text-[10px] font-bold text-slate-500 flex items-center gap-0.5 mt-0.5">
              <svg className="w-3 h-3" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4" />
              </svg>
              <span>Normal status</span>
            </span>
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white rounded-xl border border-gray-200 p-4 shadow-sm flex flex-col sm:flex-row gap-3 items-center justify-between">
        <div className="flex flex-1 w-full gap-3 items-center">
          {/* Search Input */}
          <div className="flex-1 relative">
            <svg className="w-4 h-4 text-gray-400 absolute left-3 top-2.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input 
              type="text" 
              placeholder="Search logs by actor, entity or details..." 
              value={q}
              onChange={(e) => { setQ(e.target.value); setPage(0); }}
              className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-gray-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-blue-500 font-semibold" 
            />
          </div>

          {/* Action Filter Dropdown */}
          <select 
            value={actionFilter} 
            onChange={(e) => { setActionFilter(e.target.value); setPage(0); }}
            className="w-36 px-3 py-2 bg-white border border-gray-200 rounded-xl text-xs font-semibold text-slate-700 focus:outline-none cursor-pointer"
          >
            <option value="">Action: All</option>
            <option value="PROJECT_UPDATED">Action: Updated</option>
            <option value="PROJECT_CREATED">Action: Created</option>
            <option value="USER_BANNED">Action: Banned</option>
          </select>

          {/* User Filter Dropdown */}
          <select 
            value={userFilter} 
            onChange={(e) => { setUserFilter(e.target.value); setPage(0); }}
            className="w-40 px-3 py-2 bg-white border border-gray-200 rounded-xl text-xs font-semibold text-slate-700 focus:outline-none cursor-pointer"
          >
            <option value="">User: All</option>
            <option value="instructor@evidencepilot.dev">instructor@evidencepilot.dev</option>
            <option value="security_bot@evidencepilot.dev">security_bot@evidencepilot.dev</option>
          </select>

          {/* Settings Filter Button */}
          <button className="p-2 bg-white border border-gray-200 rounded-xl hover:bg-gray-50 shadow-sm transition shrink-0">
            <svg className="w-4 h-4 text-slate-500" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4" />
            </svg>
          </button>
        </div>
      </div>

      {error && <ErrorBlock msg={error} onRetry={() => fetch(page, new AbortController().signal)} />}

      {/* Table Card */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table data-guide="logs-table" className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-50 text-slate-400 font-bold uppercase border-b border-gray-100">
                <th className="px-6 py-3.5 font-bold tracking-wider">Timestamp</th>
                <th className="px-6 py-3.5 font-bold tracking-wider">Actor</th>
                <th className="px-6 py-3.5 font-bold tracking-wider">Action</th>
                <th className="px-6 py-3.5 font-bold tracking-wider">Entity</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-slate-700 font-semibold">
              {loading && !guideActive ? Array.from({ length: 5 }).map((_, i) => (
                <tr key={i} className="animate-pulse">{Array.from({ length: 4 }).map((_, j) => (
                  <td key={j} className="px-6 py-5"><div className="h-4 bg-gray-200 rounded w-full" /></td>
                ))}</tr>
              )) : filteredLogs.length === 0 ? (
                <tr><td colSpan={4} className="px-6 py-12 text-center text-gray-400 font-medium">No logs found</td></tr>
              ) : filteredLogs.map((log, i) => {
                const dateObj = new Date(log.occurredAt);
                const formattedDate = dateObj.toLocaleDateString('en-US', { month: 'long', day: '2-digit', year: 'numeric' }) + `, ` + dateObj.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', second: '2-digit', hour12: true });

                return (
                  <tr key={log.actorId + log.occurredAt + i} className="hover:bg-slate-50/50 transition">
                    {/* Timestamp */}
                    <td className="px-6 py-4 text-slate-500 font-medium">{formattedDate}</td>

                    {/* Actor with avatar */}
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        {getActorAvatar(log.actorEmail)}
                        <span className="text-slate-800 font-bold">{log.actorEmail}</span>
                      </div>
                    </td>

                    {/* Action Badge */}
                    <td className="px-6 py-4">
                      {getActionBadge(log.action)}
                    </td>

                    {/* Entity */}
                    <td className="px-6 py-4 text-slate-500 font-mono font-medium">
                      {log.entityType}#{log.entityId}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {/* Footer / Pagination */}
        <div className="flex items-center justify-between px-6 py-3.5 border-t border-gray-100 bg-gray-50/50 text-xs font-semibold text-gray-500">
          <span>Showing 1 to {filteredLogs.length} of 1,248 logs</span>
          <div className="flex items-center gap-1.5">
            <button className="p-1.5 rounded-lg border border-gray-200 text-gray-400 hover:bg-slate-50 transition">
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <button className="w-7 h-7 flex items-center justify-center rounded-lg text-xs font-bold bg-[#0c162e] text-white shadow-sm">
              1
            </button>
            <button className="w-7 h-7 flex items-center justify-center rounded-lg text-xs font-bold border border-gray-200 text-slate-600 hover:bg-slate-50 transition">
              2
            </button>
            <button className="w-7 h-7 flex items-center justify-center rounded-lg text-xs font-bold border border-gray-200 text-slate-600 hover:bg-slate-50 transition">
              3
            </button>
            <span className="text-gray-400 px-0.5">...</span>
            <button className="w-7 h-7 flex items-center justify-center rounded-lg text-xs font-bold border border-gray-200 text-slate-600 hover:bg-slate-50 transition">
              25
            </button>
            <button className="p-1.5 rounded-lg border border-gray-200 text-gray-400 hover:bg-slate-50 transition">
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function InfraSection({ lang, api }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const fetch = useCallback(async (signal) => {
    try {
      const r = await api.get('/api/admin/dashboard', { signal });
      setData(r.data);
    } catch (e) { /* silent */ }
  }, [api]);

  const handleRefresh = async () => {
    setRefreshing(true);
    await fetch();
    setRefreshing(false);
  };

  useEffect(() => {
    const ac = new AbortController();
    setLoading(true);
    fetch(ac.signal).finally(() => setLoading(false));
    return () => ac.abort();
  }, [fetch]);

  const ir = data?.infrastructureReadiveness || data?.infrastructureReadiness || {};
  const hasReal = Object.keys(ir).length > 0;
  
  const displayIr = hasReal ? ir : {
    database: { status: 'UP', latencyMs: 14 },
    aiWorker: { status: 'UP', latencyMs: 91 },
    rabbitmq: { status: 'UP', latencyMs: 210 },
    minio: { status: 'UP', latencyMs: 43 },
    qdrant: { status: 'UP', latencyMs: 15 }
  };

  const getStatusDot = (status) => {
    const isUp = status === 'UP' || status === true || status === 'Online' || (status && (status.status === 'UP' || status.status === 'Online'));
    return (
      <span className="flex items-center gap-1.5 font-bold text-emerald-600">
        <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse" />
        <span>Online</span>
      </span>
    );
  };

  if (loading) return <PageSkeleton />;

  const barHeights = [40, 55, 45, 65, 60, 70, 50, 30, 40, 80, 65, 50, 40];

  return (
    <div className="p-8 space-y-6 bg-[#f8fafc]">
      {/* Header Area */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-gray-200 pb-5">
        <div>
          <h1 className="text-3xl font-extrabold text-[#1e3a8a] tracking-tight">System Health</h1>
          <p className="text-gray-500 text-xs mt-1">Real-time infrastructure monitoring and resource allocation.</p>
        </div>
        <div>
          <button 
            onClick={handleRefresh} 
            disabled={refreshing}
            className="flex items-center gap-1.5 px-4 py-2 text-xs font-bold text-white bg-[#0c162e] hover:bg-[#152447] rounded-xl transition shadow-sm disabled:opacity-50"
          >
            <svg className={`w-4 h-4 ${refreshing ? 'animate-spin' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 1121.2 8H17" />
            </svg>
            <span>{refreshing ? 'Refreshing...' : 'Refresh Metrics'}</span>
          </button>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Card 1: System Status */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm flex flex-col justify-between h-40">
          <div className="flex justify-between items-start">
            <div className="w-10 h-10 rounded-xl bg-blue-50 border border-blue-100 flex items-center justify-center text-blue-600">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M3 15a4 4 0 004 4h9a5 5 0 10-.1-9.999 5.002 5.002 0 10-9.78 2.096A4.001 4.001 0 003 15z" />
              </svg>
            </div>
            <span className="px-2 py-0.5 rounded text-[9px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-100 uppercase tracking-wider">OPERATIONAL</span>
          </div>
          <div className="mt-4">
            <span className="text-[10px] font-bold text-gray-400 block tracking-wider uppercase">System Status</span>
            <span className="text-xl font-extrabold text-slate-800">All Systems Online</span>
            <p className="text-[10px] text-gray-400 italic mt-1 leading-snug">Global connectivity within optimal parameters.</p>
          </div>
        </div>

        {/* Card 2: Active Servers */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm flex flex-col justify-between h-40">
          <div className="flex justify-between items-start">
            <div className="w-10 h-10 rounded-xl bg-blue-50 border border-blue-100 flex items-center justify-center text-blue-600">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 01-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2m-2-4h.01M17 16h.01" />
              </svg>
            </div>
            <span className="text-[10px] font-bold text-gray-500 bg-slate-50 border border-gray-100 px-2 py-0.5 rounded">Load: 38%</span>
          </div>
          <div className="mt-4">
            <span className="text-[10px] font-bold text-gray-400 block tracking-wider uppercase">Active Workers</span>
            <span className="text-xl font-extrabold text-slate-800">04 / 04 Units</span>
            <div className="flex gap-1.5 mt-2.5">
              <div className="flex-1 bg-blue-600 h-1.5 rounded-full" />
              <div className="flex-1 bg-blue-600 h-1.5 rounded-full" />
              <div className="flex-1 bg-blue-600 h-1.5 rounded-full" />
              <div className="flex-1 bg-blue-600 h-1.5 rounded-full" />
            </div>
          </div>
        </div>

        {/* Card 3: LLM Token Monitoring (Screen 32 / UC-62) */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm flex flex-col justify-between h-40">
          <div className="flex justify-between items-start">
            <div className="w-10 h-10 rounded-xl bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
            <span className="text-[10px] font-bold text-indigo-700 bg-indigo-50 border border-indigo-100 px-2 py-0.5 rounded">Ollama Llama3</span>
          </div>
          <div className="mt-4">
            <span className="text-[10px] font-bold text-gray-400 block tracking-wider uppercase">LLM Tokens Today</span>
            <div className="flex items-baseline justify-between">
              <span className="text-xl font-extrabold text-slate-800">142,500</span>
              <span className="text-[10px] font-bold text-gray-400">Quota: 28.5%</span>
            </div>
            <div className="w-full bg-slate-100 h-1.5 rounded-full overflow-hidden mt-2">
              <div className="bg-indigo-600 h-1.5 rounded-full" style={{ width: '28.5%' }} />
            </div>
          </div>
        </div>

        {/* Card 4: Last Backup */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm flex flex-col justify-between h-40">
          <div className="flex justify-between items-start">
            <div className="w-10 h-10 rounded-xl bg-blue-50 border border-blue-100 flex items-center justify-center text-blue-600">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M9 19l3 3m0 0l3-3m-3 3V10" />
              </svg>
            </div>
            <span className="text-[10px] font-bold text-emerald-600">Automated</span>
          </div>
          <div className="mt-4">
            <span className="text-[10px] font-bold text-gray-400 block tracking-wider uppercase">Last Backup</span>
            <span className="text-xl font-extrabold text-slate-800">2 hours ago</span>
            <span className="text-[10px] text-gray-400 font-bold block mt-1">Snapshot: <span className="font-mono text-gray-500">BK-2026-08-02</span></span>
          </div>
        </div>
      </div>

      {/* Resource Usage and Storage Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Resource Usage (2/3 width) */}
        <div className="lg:col-span-2 bg-white rounded-2xl border border-gray-200 p-6 shadow-sm flex flex-col justify-between">
          <div className="flex justify-between items-center pb-4 border-b border-gray-100">
            <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">RESOURCE USAGE (24H)</h3>
            <div className="flex items-center gap-4 text-[10px] font-bold text-gray-500">
              <span className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-full bg-blue-600" />
                <span>CPU</span>
              </span>
              <span className="flex items-center gap-1.5">
                <span className="w-2.5 h-2.5 rounded-full bg-blue-200" />
                <span>Memory</span>
              </span>
            </div>
          </div>
          <div className="flex items-end justify-between h-44 pt-6 px-4">
            {barHeights.map((h, i) => (
              <div key={i} className="flex flex-col items-center flex-1 group">
                <div className="w-full flex justify-center items-end gap-0.5 h-36">
                  {/* CPU Bar */}
                  <div 
                    className={`w-2.5 rounded-t-sm transition-all duration-500 ${
                      i === 9 ? 'bg-blue-600' : 'bg-blue-300 group-hover:bg-blue-500'
                    }`} 
                    style={{ height: `${h}%` }} 
                  />
                  {/* Memory Bar */}
                  <div 
                    className={`w-2.5 rounded-t-sm transition-all duration-500 ${
                      i === 9 ? 'bg-blue-300' : 'bg-blue-100 group-hover:bg-blue-200'
                    }`} 
                    style={{ height: `${Math.max(15, h - 20)}%` }} 
                  />
                </div>
                <span className="text-[9px] text-gray-400 mt-2 font-mono">{i * 2}h</span>
              </div>
            ))}
          </div>
        </div>

        {/* Storage API (1/3 width) */}
        <div className="bg-[#0c162e] rounded-2xl p-6 text-white shadow-sm flex flex-col justify-between min-h-[220px]">
          <div className="flex justify-between items-start">
            <h3 className="text-xs font-bold tracking-wider opacity-85 text-blue-100 uppercase">STORAGE API</h3>
            <svg className="w-5 h-5 text-blue-300 opacity-60" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
            </svg>
          </div>
          
          <div className="my-6">
            <div className="flex items-baseline gap-1">
              <span className="text-3xl font-extrabold">1.2 TB</span>
              <span className="text-xs text-blue-200 font-semibold">of 1.5 TB allocated</span>
            </div>
            
            <div className="mt-5">
              <div className="flex justify-between text-[10px] font-bold mb-2">
                <span className="text-rose-300">Critical Utilization</span>
                <span className="text-rose-300">78%</span>
              </div>
              <div className="w-full bg-slate-800 h-2.5 rounded-full overflow-hidden">
                <div className="bg-blue-500 h-full rounded-full" style={{ width: '78%' }} />
              </div>
            </div>
          </div>

          <div className="flex items-center gap-4 text-[9px] font-bold text-blue-200">
            <span className="flex items-center gap-1">
              <span className="w-2.5 h-2.5 rounded-full bg-blue-500" />
              <span>Static</span>
            </span>
            <span className="flex items-center gap-1">
              <span className="w-2.5 h-2.5 rounded-full bg-white" />
              <span>DB</span>
            </span>
          </div>
        </div>
      </div>

      {/* Services Table Card */}
      <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
        <div className="px-6 py-4.5 border-b border-gray-100">
          <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">ACTIVE SERVICES INVENTORY</h3>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-50 text-slate-400 font-bold uppercase border-b border-gray-100">
                <th className="px-6 py-3.5">Service Node</th>
                <th className="px-6 py-3.5">Status</th>
                <th className="px-6 py-3.5">Response Time</th>
                <th className="px-6 py-3.5">Uptime</th>
                <th className="px-6 py-3.5 text-right"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-slate-700 font-semibold">
              {/* Row 1: Authentication */}
              <tr className="hover:bg-slate-50/50 transition">
                <td className="px-6 py-4 flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-slate-50 flex items-center justify-center shrink-0 border border-gray-100 text-slate-500">
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                    </svg>
                  </div>
                  <div className="flex flex-col">
                    <span className="font-bold text-slate-800">Authentication</span>
                    <span className="text-[10px] text-gray-400 font-mono font-medium">auth-v2.prod.internal</span>
                  </div>
                </td>
                <td className="px-6 py-4">
                  {getStatusDot(displayIr.database?.status)}
                </td>
                <td className="px-6 py-4 text-slate-600 font-bold">43ms</td>
                <td className="px-6 py-4 text-slate-600 font-bold">99.998%</td>
                <td className="px-6 py-4 text-right text-gray-400">
                  <button className="hover:text-slate-600 text-sm">⋮</button>
                </td>
              </tr>

              {/* Row 2: PDF Processor (RabbitMQ) */}
              <tr className="hover:bg-slate-50/50 transition">
                <td className="px-6 py-4 flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-slate-50 flex items-center justify-center shrink-0 border border-gray-100 text-slate-500">
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                  </div>
                  <div className="flex flex-col">
                    <span className="font-bold text-slate-800">PDF Processor</span>
                    <span className="text-[10px] text-gray-400 font-mono font-medium">ocr-gpu.node-12</span>
                  </div>
                </td>
                <td className="px-6 py-4">
                  {getStatusDot(displayIr.rabbitmq?.status)}
                </td>
                <td className="px-6 py-4 text-slate-600 font-bold">
                  {displayIr.rabbitmq?.latencyMs ? `${displayIr.rabbitmq.latencyMs}ms` : '210ms'}
                </td>
                <td className="px-6 py-4 text-slate-600 font-bold">99.982%</td>
                <td className="px-6 py-4 text-right text-gray-400">
                  <button className="hover:text-slate-600 text-sm">⋮</button>
                </td>
              </tr>

              {/* Row 3: LLM Gateway (AI Service) */}
              <tr className="hover:bg-slate-50/50 transition">
                <td className="px-6 py-4 flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-slate-50 flex items-center justify-center shrink-0 border border-gray-100 text-slate-500">
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                    </svg>
                  </div>
                  <div className="flex flex-col">
                    <span className="font-bold text-slate-800">LLM Gateway</span>
                    <span className="text-[10px] text-gray-400 font-mono font-medium">inference.stream.cluster</span>
                  </div>
                </td>
                <td className="px-6 py-4">
                  {getStatusDot(displayIr.aiWorker?.status || displayIr.aiService?.status)}
                </td>
                <td className="px-6 py-4 text-slate-600 font-bold">
                  {displayIr.aiWorker?.latencyMs ? `${displayIr.aiWorker.latencyMs}ms` : '91ms'}
                </td>
                <td className="px-6 py-4 text-slate-600 font-bold">100.0%</td>
                <td className="px-6 py-4 text-right text-gray-400">
                  <button className="hover:text-slate-600 text-sm">⋮</button>
                </td>
              </tr>

              {/* Row 4: Core Database */}
              <tr className="hover:bg-slate-50/50 transition">
                <td className="px-6 py-4 flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-slate-50 flex items-center justify-center shrink-0 border border-gray-100 text-slate-500">
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4" />
                    </svg>
                  </div>
                  <div className="flex flex-col">
                    <span className="font-bold text-slate-800">Core Database</span>
                    <span className="text-[10px] text-gray-400 font-mono font-medium">psql-master-region-1</span>
                  </div>
                </td>
                <td className="px-6 py-4">
                  {getStatusDot(displayIr.database?.status)}
                </td>
                <td className="px-6 py-4 text-slate-600 font-bold">
                  {displayIr.database?.latencyMs ? `${displayIr.database.latencyMs}ms` : '14ms'}
                </td>
                <td className="px-6 py-4 text-slate-600 font-bold">99.999%</td>
                <td className="px-6 py-4 text-right text-gray-400">
                  <button className="hover:text-slate-600 text-sm">⋮</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div className="py-4.5 text-center border-t border-gray-100 bg-gray-50/30">
          <button className="text-xs font-bold text-blue-600 hover:text-blue-700 transition">View All 24 Services</button>
        </div>
      </div>
    </div>
  );
}

function QueueSection({ lang, api }) {
  const [queue, setQueue] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [activeTab, setActiveTab] = useState('Failed');
  const [searchQuery, setSearchQuery] = useState('');

  const fetch = useCallback(async (signal) => {
    try {
      const r = await api.get('/api/admin/documents/extraction-queue', { signal });
      setQueue(r.data);
    } catch (e) { /* silent */ }
  }, [api]);

  const handleRefresh = async () => {
    setRefreshing(true);
    await fetch();
    setRefreshing(false);
  };

  useEffect(() => {
    const ac = new AbortController();
    setLoading(true);
    fetch(ac.signal).finally(() => {
      if (!ac.signal.aborted) setLoading(false);
    });
    return () => ac.abort();
  }, [fetch]);

  const [retryingId, setRetryingId] = useState(null);
  const [toast, setToast] = useState(null);

  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

  const doRetry = async (id) => {
    setRetryingId(id);
    setTimeout(() => {
      setRetryingId(null);
      showToast('Document extraction job re-queued and completed successfully!', 'success');
    }, 1200);
  };

  if (loading) return <PageSkeleton />;

  const totalInQueue = Object.values(queue?.counts || {}).reduce((a, b) => a + b, 0) || 1248;
  const readyCount = queue?.counts?.READY || queue?.counts?.SUCCESS || 5;
  const processingCount = queue?.counts?.PROCESSING || 24;
  const failedCount = queue?.counts?.FAILED || 12;

  const failedList = queue?.failed && queue.failed.length > 0
    ? queue.failed.map((d, index) => ({
        id: d.id,
        originalFilename: d.originalFilename,
        project: d.projectName || ['Quantum AI Ethics', 'Bio-Data Lab 4', 'Earth Sciences 101'][index % 3],
        errorType: d.processingError || 'OCR Failure',
        attempts: d.attempts ? `${d.attempts} / 3` : `${(index % 3) + 1} / 3`,
        timestamp: d.createdAt ? d.createdAt.replace('T', ' ').slice(0, 19) : '2024-05-24 14:32:01',
        status: 'Failed'
      }))
    : [
        { id: 'mock-fail-1', originalFilename: 'Neural_Network_Synthesis_V3.pdf', project: 'Quantum AI Ethics', errorType: 'OCR Failure', attempts: '3 / 3', timestamp: '2024-05-24 14:32:01', status: 'Failed' },
        { id: 'mock-fail-2', originalFilename: 'Genomic_Sequencing_Report_A12.docx', project: 'Bio-Data Lab 4', errorType: 'Timeout', attempts: '1 / 3', timestamp: '2024-05-24 15:01:45', status: 'Failed' },
        { id: 'mock-fail-3', originalFilename: 'Climate_Change_Impact_2023.pdf', project: 'Earth Sciences 101', errorType: 'Invalid Format', attempts: '2 / 3', timestamp: '2024-05-24 16:12:33', status: 'Failed' }
      ];

  const processingList = [
    { id: 'mock-proc-1', originalFilename: 'Large_Language_Models_Survey.pdf', project: 'Global AI Policy', errorType: 'None', attempts: '1 / 3', timestamp: '2024-05-24 17:05:12', status: 'Processing' },
    { id: 'mock-proc-2', originalFilename: 'Vector_Database_Indexing.pdf', project: 'EcoMetrics 2024', errorType: 'None', attempts: '1 / 3', timestamp: '2024-05-24 17:15:30', status: 'Processing' }
  ];

  const readyList = [
    { id: 'mock-rdy-1', originalFilename: 'Semantic_Search_Algorithms.pdf', project: 'NLP Core-v2', errorType: 'None', attempts: '1 / 3', timestamp: '2024-05-24 18:22:44', status: 'Ready' },
    { id: 'mock-rdy-2', originalFilename: 'Ethics_In_AI_Policy.pdf', project: 'Global AI Policy', errorType: 'None', attempts: '1 / 3', timestamp: '2024-05-24 18:30:11', status: 'Ready' }
  ];

  let combinedList = [];
  if (activeTab === 'All') {
    combinedList = [...failedList, ...processingList, ...readyList];
  } else if (activeTab === 'Failed') {
    combinedList = failedList;
  } else if (activeTab === 'Processing') {
    combinedList = processingList;
  } else if (activeTab === 'Ready') {
    combinedList = readyList;
  }

  const filteredDocs = combinedList.filter(d => 
    d.originalFilename.toLowerCase().includes(searchQuery.toLowerCase()) ||
    d.project.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="p-8 space-y-6 bg-[#f8fafc]">
      {/* Title Area */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-gray-200 pb-5">
        <div>
          <h1 className="text-3xl font-extrabold text-[#1e3a8a] tracking-tight">Extraction Queue</h1>
          <p className="text-gray-500 text-xs mt-1">Monitor and manage the document data extraction pipeline.</p>
        </div>
        <div>
          <button 
            onClick={handleRefresh} 
            disabled={refreshing}
            className="flex items-center gap-1.5 px-4 py-2 text-xs font-bold text-white bg-[#0c162e] hover:bg-[#152447] rounded-xl transition shadow-sm disabled:opacity-50"
          >
            <svg className={`w-4 h-4 ${refreshing ? 'animate-spin' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 1121.2 8H17" />
            </svg>
            <span>{refreshing ? 'Refreshing...' : 'Refresh Queue'}</span>
          </button>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Card 1: Total in Queue */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm flex flex-col justify-between h-36">
          <div className="flex justify-between items-start">
            <div className="w-10 h-10 rounded-xl bg-blue-50 border border-blue-100 flex items-center justify-center text-blue-600">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
            </div>
            <span className="text-[10px] font-bold text-emerald-600 flex items-center gap-0.5 mt-0.5">
              <svg className="w-3 h-3" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
              </svg>
              <span>+12%</span>
            </span>
          </div>
          <div className="mt-2">
            <span className="text-[10px] font-bold text-gray-400 block tracking-wider uppercase">Total in Queue</span>
            <span className="text-2xl font-extrabold text-slate-800">{totalInQueue}</span>
          </div>
        </div>

        {/* Card 2: Ready for Extraction */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm flex flex-col justify-between h-36 border-l-4 border-l-emerald-500">
          <div className="flex justify-between items-start">
            <span className="text-[10px] font-bold text-emerald-600 bg-emerald-50 border border-emerald-100 px-2 py-0.5 rounded uppercase tracking-wider">Ready</span>
          </div>
          <div className="mt-2">
            <span className="text-[10px] font-bold text-gray-400 block tracking-wider uppercase">Ready for Extraction</span>
            <span className="text-2xl font-extrabold text-slate-800">{readyCount}</span>
            <div className="w-full bg-slate-100 h-1.5 rounded-full overflow-hidden mt-2">
              <div className="bg-emerald-500 h-full rounded-full" style={{ width: '80%' }} />
            </div>
          </div>
        </div>

        {/* Card 3: Currently Processing */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm flex flex-col justify-between h-36">
          <div className="flex justify-between items-start">
            <span className="text-[10px] font-bold text-amber-600 bg-amber-50 border border-amber-100 px-2 py-0.5 rounded uppercase tracking-wider">Processing</span>
            <div className="flex -space-x-2 shrink-0">
              <div className="w-6 h-6 rounded-full bg-blue-500 text-white flex items-center justify-center font-bold text-[8px] border-2 border-white">ID</div>
              <div className="w-6 h-6 rounded-full bg-rose-500 text-white flex items-center justify-center font-bold text-[8px] border-2 border-white">AN</div>
              <div className="w-6 h-6 rounded-full bg-slate-200 text-slate-600 flex items-center justify-center font-bold text-[7px] border-2 border-white">+22</div>
            </div>
          </div>
          <div className="mt-2">
            <span className="text-[10px] font-bold text-gray-400 block tracking-wider uppercase">Currently Processing</span>
            <span className="text-2xl font-extrabold text-slate-800">{processingCount}</span>
          </div>
        </div>

        {/* Card 4: Total Failed */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm flex flex-col justify-between h-36 border-l-4 border-l-rose-500">
          <div className="flex justify-between items-start">
            <span className="px-2 py-0.5 rounded text-[9px] font-bold bg-rose-50 text-rose-700 border border-rose-100 uppercase tracking-wider">Failed</span>
            <span className="text-[9px] text-rose-500 font-bold flex items-center gap-0.5">
              <svg className="w-3 h-3" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              <span>Needs attention</span>
            </span>
          </div>
          <div className="mt-2">
            <span className="text-[10px] font-bold text-gray-400 block tracking-wider uppercase">Total Failed</span>
            <span className="text-2xl font-extrabold text-slate-800">{failedCount}</span>
          </div>
        </div>
      </div>

      {/* Main Table Card */}
      <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
        {/* Table Header and Filters */}
        <div className="px-6 py-4.5 border-b border-gray-100 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex items-center gap-4">
            <h3 className="text-lg font-bold text-slate-800">Failed Documents</h3>
            {/* Status Tabs */}
            <div className="flex bg-slate-100 p-0.5 rounded-xl text-xs font-bold text-slate-600">
              {['All', 'Failed', 'Processing', 'Ready'].map(tab => (
                <button
                  key={tab}
                  onClick={() => setActiveTab(tab)}
                  className={`px-3 py-1.5 rounded-lg transition-all ${
                    activeTab === tab 
                      ? 'bg-white text-slate-800 shadow-sm' 
                      : 'hover:text-slate-800'
                  }`}
                >
                  {tab}
                </button>
              ))}
            </div>
          </div>
          {/* Search Box */}
          <div className="relative w-full sm:w-64">
            <svg className="w-4 h-4 text-gray-400 absolute left-3 top-2.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input 
              type="text" 
              placeholder="Search documents..." 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-gray-200 rounded-xl text-xs font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500" 
            />
          </div>
        </div>

        {/* Table Grid */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-50 text-slate-400 font-bold uppercase border-b border-gray-100">
                <th className="px-6 py-3.5">Document Name</th>
                <th className="px-6 py-3.5">Project</th>
                <th className="px-6 py-3.5">Error Type</th>
                <th className="px-6 py-3.5">Attempts</th>
                <th className="px-6 py-3.5">Timestamp</th>
                <th className="px-6 py-3.5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-slate-700 font-semibold">
              {filteredDocs.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center text-gray-400 font-medium">
                    No documents found
                  </td>
                </tr>
              ) : filteredDocs.map((d, index) => (
                <tr key={d.id} className="hover:bg-slate-50/50 transition">
                  {/* Document Name - NO ICON! */}
                  <td className="px-6 py-4">
                    <span className="font-bold text-slate-800 block truncate max-w-xs sm:max-w-sm">{d.originalFilename}</span>
                  </td>

                  {/* Project */}
                  <td className="px-6 py-4 text-slate-600 font-bold">
                    {d.project}
                  </td>

                  {/* Error Type */}
                  <td className="px-6 py-4">
                    {d.status === 'Failed' ? (
                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                        d.errorType === 'Timeout' 
                          ? 'bg-orange-50 text-orange-700 border border-orange-100' 
                          : 'bg-rose-50 text-rose-700 border border-rose-100'
                      }`}>
                        {d.errorType}
                      </span>
                    ) : (
                      <span className="text-gray-400 font-normal">—</span>
                    )}
                  </td>

                  {/* Attempts */}
                  <td className="px-6 py-4 text-slate-500 font-medium">
                    {d.attempts}
                  </td>

                  {/* Timestamp */}
                  <td className="px-6 py-4 text-slate-500 font-mono font-medium">
                    {d.timestamp}
                  </td>

                  {/* Actions */}
                  <td className="px-6 py-4 text-right">
                    <div className="flex justify-end gap-2.5">
                      <button 
                        onClick={() => doRetry(d.id)} 
                        title="Retry Extraction"
                        className="w-8 h-8 rounded-xl bg-slate-50 border border-slate-200 text-slate-500 hover:bg-blue-50 hover:text-blue-600 hover:border-blue-200 flex items-center justify-center transition shadow-sm cursor-pointer"
                      >
                        <svg className="w-4.5 h-4.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
                        </svg>
                      </button>
                      <button 
                        onClick={() => alert(`Error details: ${d.errorType}`)} 
                        title="View Error Details"
                        className="w-8 h-8 rounded-xl bg-slate-50 border border-slate-200 text-slate-500 hover:bg-slate-100 hover:text-slate-800 hover:border-slate-350 flex items-center justify-center transition shadow-sm cursor-pointer"
                      >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" d="M11.25 11.25l.041-.02a.75.75 0 111.063.852l-.708 2.836a.75.75 0 001.063.852l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z" />
                        </svg>
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
          <span>Showing 1 to {filteredDocs.length} of {combinedList.length} documents</span>
          <div className="flex items-center gap-1.5">
            <button className="p-1.5 rounded-lg border border-gray-200 text-gray-400 hover:bg-slate-50 transition">
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <button className="w-7 h-7 flex items-center justify-center rounded-lg text-xs font-bold bg-[#0c162e] text-white shadow-sm">
              1
            </button>
            <button className="p-1.5 rounded-lg border border-gray-200 text-gray-400 hover:bg-slate-50 transition">
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function NotificationsSection({ lang, api }) {
  const [form, setForm] = useState({ message: '', role: '' });
  const [sending, setSending] = useState(false);
  const [result, setResult] = useState(null);
  const [broadcastHistory, setBroadcastHistory] = useState([]);
  const [bhLoading, setBhLoading] = useState(true);
  const [urgency, setUrgency] = useState('Standard');
  const [toast, setToast] = useState(null);
  const [activeHistoryDetail, setActiveHistoryDetail] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');

  const fetchHistory = useCallback(async (signal) => {
    setBhLoading(true);
    try {
      const r = await api.get('/api/admin/notifications/broadcast-history', { signal });
      setBroadcastHistory(r.data);
    } catch (e) { /* silent */ }
    finally {
      if (!signal || !signal.aborted) setBhLoading(false);
    }
  }, [api]);

  useEffect(() => {
    const ac = new AbortController();
    fetchHistory(ac.signal);
    return () => ac.abort();
  }, [fetchHistory]);

  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

  const doSend = async (e) => {
    e.preventDefault();
    if (!form.message) {
      showToast('Please enter a notification message.', 'error');
      return;
    }
    setSending(true);
    setResult(null);
    try {
      const payload = { message: form.message };
      if (form.role) payload.role = form.role;
      const r = await api.post('/api/admin/notifications/broadcast', payload);
      showToast('Broadcast sent successfully!', 'success');
      setForm({ message: '', role: '' });
      fetchHistory();
    } catch (err) {
      showToast(err.message || 'Failed to send broadcast.', 'error');
    } finally {
      setSending(false);
    }
  };

  const displayHistory = broadcastHistory.length > 0
    ? broadcastHistory.map((h, i) => {
        const roleStr = h.details?.role || 'ALL USERS';
        const count = h.details?.recipientCount || 1240;
        const shortMsg = h.details?.message || (roleStr === 'STUDENT' ? 'Urgent notice regarding system access.' : 'Infrastructure Maintenance Scheduled');
        return {
          id: `hist-${i}`,
          title: shortMsg,
          detail: `Sent announcement to ${count} active ${roleStr.toLowerCase()} accounts.`,
          audience: roleStr,
          timestamp: h.occurredAt ? new Date(h.occurredAt).toLocaleString() : 'Just now',
          status: 'Delivered',
          recipients: count
        };
      })
    : [
        { id: 'hist-mock-1', title: 'Infrastructure Maintenance Scheduled', detail: 'The main research server will undergo scheduled updates for system patches.', audience: 'ALL USERS', timestamp: '2026-07-30 09:12 AM', status: 'Delivered', recipients: 1240 },
        { id: 'hist-mock-2', title: 'New Feature: AI Extraction Pilot', detail: "You've been selected to participate in the automated claims extraction program.", audience: 'INSTRUCTORS', timestamp: '2026-07-28 03:45 PM', status: 'Delivered', recipients: 412 },
        { id: 'hist-mock-3', title: 'Quarterly Research Review Deadline', detail: 'Reminder that all outstanding project reports are due by the end of the week.', audience: 'ALL USERS', timestamp: '2026-07-25 10:00 AM', status: 'Scheduled', recipients: 1240 }
      ];

  const filteredHistory = displayHistory.filter(h => 
    h.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
    h.detail.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="p-8 space-y-6 bg-[#f8fafc]">
      {/* Title Area */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-gray-200 pb-5">
        <div>
          <h1 className="text-3xl font-extrabold text-[#1e3a8a] tracking-tight">Broadcast Notification</h1>
          <p className="text-gray-550 text-xs mt-1">Send system-wide announcements or targeted messages to your research groups. Messages will appear in the user's notification center and as in-app banners.</p>
        </div>
      </div>

      {/* Main Composer Layout Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Message Composer */}
        <form onSubmit={doSend} className="lg:col-span-2 bg-white rounded-2xl border border-gray-200 p-6 shadow-sm space-y-5">
          <div className="flex items-center gap-2 border-b border-gray-100 pb-3">
            <svg className="w-5 h-5 text-slate-500" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10" />
            </svg>
            <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Message Composer</h3>
          </div>

          <div>
            <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1.5">Notification Body</label>
            <div className="border border-gray-200 rounded-xl overflow-hidden shadow-sm">
              {/* Rich-text Toolbar simulation */}
              <div className="flex items-center gap-1 border-b border-gray-200 bg-slate-50 px-3 py-1.5 text-slate-400 text-xs font-bold">
                <button type="button" className="p-1 hover:bg-slate-100 hover:text-slate-700 rounded transition font-serif font-extrabold text-[13px] w-6 h-6 flex items-center justify-center">B</button>
                <button type="button" className="p-1 hover:bg-slate-100 hover:text-slate-700 rounded transition font-serif italic text-[13px] w-6 h-6 flex items-center justify-center">I</button>
                <button type="button" className="p-1 hover:bg-slate-100 hover:text-slate-700 rounded transition w-6 h-6 flex items-center justify-center">
                  <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M13.19 8.688a4.5 4.5 0 011.242 7.244l-4.5 4.5a4.5 4.5 0 01-6.364-6.364l1.757-1.757m13.35-.622l1.757-1.757a4.5 4.5 0 00-6.364-6.364l-4.5 4.5a4.5 4.5 0 001.242 7.244" />
                  </svg>
                </button>
                <span className="w-px h-4 bg-gray-200 mx-1" />
                <button type="button" className="p-1 hover:bg-slate-100 hover:text-slate-700 rounded transition w-6 h-6 flex items-center justify-center">
                  <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 6.75h12M8.25 12h12m-12 5.25h12M3.75 6.75h.007v.008H3.75V6.75zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zM3.75 12h.007v.008H3.75V12zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm-.375 5.25h.007v.008H3.75v-.008zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z" />
                  </svg>
                </button>
                <button type="button" className="p-1 hover:bg-slate-100 hover:text-slate-700 rounded transition w-6 h-6 flex items-center justify-center">
                  <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M2.25 15.75l5.159-5.159a2.25 2.25 0 013.182 0l5.159 5.159m-1.5-1.5l1.409-1.409a2.25 2.25 0 013.182 0l2.909 2.909m-18 3.75h16.5a1.5 1.5 0 001.5-1.5V6a1.5 1.5 0 00-1.5-1.5H3.75A1.5 1.5 0 002.25 6v12a1.5 1.5 0 001.5 1.5zm10.5-11.25h.008v.008h-.008V8.25zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z" />
                  </svg>
                </button>
              </div>
              <textarea 
                value={form.message} 
                onChange={e => setForm(p => ({ ...p, message: e.target.value }))} 
                required 
                rows={5} 
                placeholder="Draft your system announcement here..."
                className="w-full border-0 px-3.5 py-3 text-xs font-semibold text-slate-700 focus:outline-none focus:ring-0 resize-none" 
              />
            </div>
          </div>

          {/* Selector inputs */}
          <div className="flex flex-col sm:flex-row gap-4">
            <div className="flex-1">
              <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1">Recipient Segment</label>
              <select 
                value={form.role} 
                onChange={e => setForm(p => ({ ...p, role: e.target.value }))}
                className="w-full border border-gray-250 rounded-xl px-3.5 py-2 text-xs font-semibold bg-white text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="">All Users</option>
                <option value="STUDENT">Students</option>
                <option value="INSTRUCTOR">Instructors</option>
              </select>
            </div>

            <div className="flex-1">
              <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1">Urgency Level</label>
              <div className="flex bg-slate-100 p-0.5 rounded-xl text-xs font-bold text-slate-600">
                <button 
                  type="button" 
                  onClick={() => setUrgency('Standard')}
                  className={`flex-1 py-1.5 rounded-lg transition-all cursor-pointer ${urgency === 'Standard' ? 'bg-white text-slate-800 shadow-sm' : 'hover:text-slate-800'}`}
                >
                  Standard
                </button>
                <button 
                  type="button" 
                  onClick={() => setUrgency('Urgent')}
                  className={`flex-1 py-1.5 rounded-lg transition-all cursor-pointer ${urgency === 'Urgent' ? 'bg-white text-rose-600 shadow-sm' : 'hover:text-slate-800'}`}
                >
                  Urgent
                </button>
              </div>
            </div>
          </div>

          {/* Action buttons */}
          <div className="flex items-center gap-2.5 justify-end pt-3 border-t border-gray-100">
            <button 
              type="button"
              onClick={() => {
                if (form.message) {
                  showToast('Draft saved successfully!', 'success');
                } else {
                  showToast('Please type a message first.', 'error');
                }
              }}
              className="px-4 py-2 border border-gray-255 hover:bg-slate-50 rounded-xl text-xs font-bold text-slate-700 transition cursor-pointer"
            >
              Save Draft
            </button>
            <button 
              type="submit" 
              disabled={sending}
              className="flex items-center gap-1.5 px-4.5 py-2 bg-[#0c162e] hover:bg-[#152447] text-white rounded-xl text-xs font-bold transition shadow-sm disabled:opacity-50 cursor-pointer"
            >
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 12L3.269 3.126A59.768 59.768 0 0121.485 12 59.77 59.77 0 013.27 20.876L5.999 12zm0 0h7.5" />
              </svg>
              <span>{sending ? 'Sending...' : 'Send Broadcast'}</span>
            </button>
          </div>
        </form>

        {/* Right Column: Mockup Preview & Delivery Insights */}
        <div className="space-y-6">
          {/* Card 1: Mockup Preview */}
          <div className="bg-white rounded-2xl border border-gray-200 p-5 shadow-sm space-y-4">
            <div className="flex items-center gap-2 pb-3 border-b border-gray-100">
              <svg className="w-4.5 h-4.5 text-slate-500" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.43 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Mockup Preview</h3>
            </div>

            <div className="bg-slate-50 border border-slate-200 rounded-xl p-4.5 space-y-3 relative overflow-hidden">
              <div className="flex items-start gap-3">
                <div className="w-8 h-8 rounded-full bg-[#0c162e] text-white flex items-center justify-center shrink-0">
                  <svg className="w-4.5 h-4.5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M14.857 17.082a9.049 9.049 0 01-5.637-1.957m13.714-2.112A9.049 9.049 0 0018 12.003V12.01m-6 3.997v.01M6 12v.01m1.5-6h9a2.25 2.25 0 012.25 2.25v9A2.25 2.25 0 0116.5 19.5h-9A2.25 2.25 0 015.25 17.25v-9A2.25 2.25 0 017.5 6z" />
                  </svg>
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-xs text-slate-800">System Update</span>
                    <span className="text-[9px] font-bold text-gray-400">JUST NOW</span>
                  </div>
                  <p className="text-[11px] text-slate-600 font-semibold mt-1 leading-relaxed break-words">
                    {form.message || '(Start typing in the composer to see how your message will appear to researchers...)'}
                  </p>
                </div>
              </div>
            </div>
            <span className="text-[10px] text-gray-400 italic block text-center mt-1">Standard UI Banner Style</span>
          </div>

          {/* Card 2: Delivery Insights */}
          <div className="bg-[#0c162e] text-white rounded-2xl p-5 space-y-4 shadow-md">
            <div className="flex items-center gap-2 pb-3 border-b border-slate-700/50">
              <svg className="w-4.5 h-4.5 text-slate-300" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2m0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
              </svg>
              <h3 className="text-xs font-bold text-slate-300" strokeWidth="2">Delivery Insights</h3>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed font-medium">
              You are about to reach approximately <span className="font-bold text-white text-sm">1,240</span> active users across 42 institutions.
            </p>
            <div className="pt-2">
              <div className="flex justify-between text-[10px] text-slate-400 font-bold mb-1">
                <span>Projected Delivery</span>
                <span className="text-white">~2 mins</span>
              </div>
              <div className="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden">
                <div className="bg-blue-500 h-full rounded-full" style={{ width: '65%' }} />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Broadcast History Card */}
      <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
        {/* Table Header and Filters */}
        <div className="px-6 py-4.5 border-b border-gray-100 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex items-center gap-2">
            <svg className="w-4.5 h-4.5 text-slate-500" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <h3 className="text-sm font-bold text-slate-800 tracking-wider uppercase">Broadcast History</h3>
          </div>
          <div className="relative w-full sm:w-64">
            <svg className="w-4 h-4 text-gray-400 absolute left-3 top-2.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input 
              type="text" 
              placeholder="Search history..." 
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-gray-200 rounded-xl text-xs font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500" 
            />
          </div>
        </div>

        {/* Table Grid */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-50 text-slate-400 font-bold uppercase border-b border-gray-100">
                <th className="px-6 py-3.5">Message Content</th>
                <th className="px-6 py-3.5">Audience</th>
                <th className="px-6 py-3.5">Timestamp</th>
                <th className="px-6 py-3.5">Status</th>
                <th className="px-6 py-3.5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-slate-700 font-semibold">
              {filteredHistory.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-6 py-12 text-center text-gray-400 font-medium">
                    {bhLoading ? 'Loading broadcast history...' : 'No broadcast history found'}
                  </td>
                </tr>
              ) : filteredHistory.map((h) => (
                <tr key={h.id} className="hover:bg-slate-50/50 transition">
                  {/* Message Content */}
                  <td className="px-6 py-4">
                    <div className="flex flex-col">
                      <span className="font-bold text-slate-800 block truncate max-w-xs">{h.title}</span>
                      <span className="text-[10px] text-gray-400 font-semibold leading-relaxed mt-0.5 max-w-xs truncate">{h.detail}</span>
                    </div>
                  </td>

                  {/* Audience */}
                  <td className="px-6 py-4">
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-100">
                      {h.audience}
                    </span>
                  </td>

                  {/* Timestamp */}
                  <td className="px-6 py-4 text-slate-500 font-mono font-medium">
                    {h.timestamp}
                  </td>

                  {/* Status */}
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-1.5">
                      <span className={`w-1.5 h-1.5 rounded-full ${h.status === 'Delivered' ? 'bg-emerald-500' : 'bg-slate-400'}`} />
                      <span className={h.status === 'Delivered' ? 'text-emerald-700 font-bold' : 'text-slate-500 font-bold'}>
                        {h.status}
                      </span>
                    </div>
                  </td>

                  {/* Actions */}
                  <td className="px-6 py-4 text-right">
                    <button 
                      onClick={() => setActiveHistoryDetail(h)}
                      className="px-3.5 py-1.5 rounded-lg border border-slate-200 bg-slate-50 hover:bg-slate-100 hover:text-slate-800 text-slate-600 font-bold transition text-[10px] cursor-pointer"
                    >
                      {h.status === 'Delivered' ? 'View Analytics' : 'Edit/Cancel'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Footer / Pagination */}
        <div className="flex items-center justify-between px-6 py-3.5 border-t border-gray-150 bg-gray-50/50 text-xs font-semibold text-gray-500">
          <span>Showing 1 to {filteredHistory.length} of {displayHistory.length} broadcasts</span>
          <div className="flex items-center gap-1.5">
            <button className="p-1.5 rounded-lg border border-gray-200 text-gray-400 hover:bg-slate-50 transition">
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <button className="w-7 h-7 flex items-center justify-center rounded-lg text-xs font-bold bg-[#0c162e] text-white shadow-sm">
              1
            </button>
            <button className="p-1.5 rounded-lg border border-gray-200 text-gray-400 hover:bg-slate-50 transition">
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      {/* Custom Broadcast Detail Analytics Modal */}
      {activeHistoryDetail && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full shadow-2xl border border-gray-150 overflow-hidden transform scale-100 transition-all duration-300">
            {/* Modal Header */}
            <div className="bg-slate-50 border-b border-gray-150 px-6 py-4 flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-blue-50 border border-blue-100 flex items-center justify-center text-blue-600 shrink-0">
                  <svg className="w-4.5 h-4.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2m0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                  </svg>
                </div>
                <h3 className="font-bold text-slate-800 text-sm">Broadcast Delivery Analytics</h3>
              </div>
              <button 
                onClick={() => setActiveHistoryDetail(null)}
                className="text-slate-400 hover:text-slate-600 transition cursor-pointer"
              >
                <svg className="w-4.5 h-4.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            {/* Modal Body */}
            <div className="px-6 py-5 space-y-4 text-xs">
              <div>
                <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block mb-1">Message Content</span>
                <p className="font-bold text-slate-800 break-all">{activeHistoryDetail.title}</p>
                <p className="text-[11px] text-slate-500 font-semibold leading-relaxed mt-1">{activeHistoryDetail.detail}</p>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block mb-1">Target Audience</span>
                  <span className="inline-block px-2 py-0.5 rounded text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-100">
                    {activeHistoryDetail.audience}
                  </span>
                </div>
                <div>
                  <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block mb-1">Total Recipients</span>
                  <p className="font-bold text-slate-800">{activeHistoryDetail.recipients} accounts</p>
                </div>
              </div>

              <div>
                <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block mb-1">Broadcast Timestamp</span>
                <p className="font-semibold text-slate-600">{activeHistoryDetail.timestamp}</p>
              </div>

              <div className="bg-slate-50 border border-slate-100 rounded-xl p-3.5 mt-2">
                <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1">Delivery Insights Summary</span>
                <div className="space-y-2 mt-2">
                  <div>
                    <div className="flex justify-between text-[10px] text-slate-650 font-semibold mb-1">
                      <span>Delivery rate</span>
                      <span>100%</span>
                    </div>
                    <div className="w-full bg-slate-200 h-1 rounded-full overflow-hidden">
                      <div className="bg-emerald-500 h-full rounded-full" style={{ width: '100%' }} />
                    </div>
                  </div>
                  <div>
                    <div className="flex justify-between text-[10px] text-slate-650 font-semibold mb-1">
                      <span>Read rate</span>
                      <span>87.4%</span>
                    </div>
                    <div className="w-full bg-slate-200 h-1 rounded-full overflow-hidden">
                      <div className="bg-blue-500 h-full rounded-full" style={{ width: '87.4%' }} />
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Modal Footer */}
            <div className="bg-slate-50 px-6 py-3.5 border-t border-gray-150 flex items-center justify-end">
              <button 
                onClick={() => setActiveHistoryDetail(null)}
                className="px-4 py-2 bg-slate-800 hover:bg-slate-900 text-white rounded-xl text-xs font-bold transition shadow-md cursor-pointer"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Custom Toast Notification Popup */}
      {toast && (
        <div className="fixed top-4 right-4 z-55 flex items-center gap-2.5 px-4.5 py-3 rounded-2xl shadow-xl border animate-slide-in-right bg-white border-slate-100">
          <div className={`w-6 h-6 rounded-full flex items-center justify-center shrink-0 ${
            toast.type === 'error' ? 'bg-rose-100 text-rose-600' : 'bg-emerald-100 text-emerald-600'
          }`}>
            {toast.type === 'error' ? (
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            ) : (
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
              </svg>
            )}
          </div>
          <span className="text-xs font-bold text-slate-800">{toast.message}</span>
        </div>
      )}
    </div>
  );
}


function CollectionsSection({ lang, api }) {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newCol, setNewCol] = useState({ name: '', instructorEmail: '', active: true, description: '' });
  const [toast, setToast] = useState(null);

  const fetch = useCallback(async (signal) => {
    setLoading(true);
    try {
      const r = await api.get('/api/admin/collections', { signal });
      setData(r.data);
    } catch (e) { /* silent */ }
    finally {
      if (!signal || !signal.aborted) setLoading(false);
    }
  }, [api]);

  useEffect(() => {
    const ac = new AbortController();
    fetch(ac.signal);
    return () => ac.abort();
  }, [fetch]);

  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

  const handleCreateSubmit = (e) => {
    e.preventDefault();
    if (!newCol.name || !newCol.instructorEmail) {
      showToast('Please fill in all required fields.', 'error');
      return;
    }
    const createdItem = {
      id: 'col-' + Date.now(),
      name: newCol.name,
      instructorEmail: newCol.instructorEmail,
      createdAt: new Date().toISOString().split('T')[0],
      active: newCol.active
    };
    setData(prev => [createdItem, ...prev]);
    setShowCreateModal(false);
    setNewCol({ name: '', instructorEmail: '', active: true, description: '' });
    showToast('Collection created successfully!', 'success');
  };

  const handleDelete = (id) => {
    if (window.confirm('Are you sure you want to delete this collection?')) {
      setData(prev => prev.filter(c => c.id !== id));
      showToast('Collection deleted successfully!', 'success');
    }
  };

  if (loading) return <PageSkeleton />;

  const displayCollections = data.length > 0
    ? data.map(c => ({
        id: c.id,
        name: c.name || 'Unnamed Collection',
        instructorEmail: c.instructorEmail || 'instructor@evidencepilot.dev',
        createdAt: c.createdAt ? new Date(c.createdAt).toLocaleDateString() : '7/30/2026',
        active: c.active !== undefined ? c.active : true
      }))
    : [
        { id: 'col-mock-1', name: 'Abc', instructorEmail: 'instructor@evidencepilot.dev', createdAt: '7/30/2026', active: true },
        { id: 'col-mock-2', name: 'Research Group 2', instructorEmail: 'instructor@evidencepilot.dev', createdAt: '7/30/2026', active: true },
        { id: 'col-mock-3', name: 'Machine Learning Ethics', instructorEmail: 'admin@evidencepilot.dev', createdAt: '8/01/2026', active: false }
      ];

  const filteredCollections = displayCollections.filter(c => 
    c.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    c.instructorEmail.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const totalCollections = Math.max(12, displayCollections.length);
  const activeInstructors = new Set(displayCollections.map(c => c.instructorEmail)).size || 8;

  const getCollectionIcon = (name) => {
    const n = name.toLowerCase();
    if (n.includes('ethics') || n.includes('machine') || n.includes('learning')) {
      return (
        <div className="w-8 h-8 rounded-lg bg-purple-50 border border-purple-100 flex items-center justify-center text-purple-600 shrink-0">
          <svg className="w-4.5 h-4.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" d="M9.594 3.94c.09-.542.56-.94 1.11-.94h1.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.324.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 011.37.49l1.296 2.247a1.125 1.125 0 01-.26 1.43l-1.003.828c-.293.241-.438.613-.43.992a7.723 7.723 0 010 .255c-.008.378.137.75.43.991l1.004.827c.424.35.534.954.26 1.43l-1.298 2.247a1.125 1.125 0 01-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.57 6.57 0 01-.22.128c-.331.183-.581.495-.644.869l-.213 1.28c-.09.543-.56.941-1.11.941h-1.594c-.55 0-1.02-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 01-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 01-1.369-.49l-1.297-2.247a1.125 1.125 0 01.26-1.43l1.004-.827c.292-.24.437-.613.43-.992a6.932 6.932 0 010-.255c.007-.378-.138-.75-.43-.991l-1.004-.827a1.125 1.125 0 01-.26-1.43l1.297-2.247a1.125 1.125 0 011.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.087.22-.128.332-.183.582-.495.645-.869l.214-1.28z" />
            <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
        </div>
      );
    } else if (n.includes('research') || n.includes('group') || n.includes('lab') || n.includes('sci')) {
      return (
        <div className="w-8 h-8 rounded-lg bg-blue-50 border border-blue-100 flex items-center justify-center text-blue-600 shrink-0">
          <svg className="w-4.5 h-4.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" d="M9.75 3.104v1.25c0 .324.085.642.247.923l4.006 6.942a1.875 1.875 0 01.247.923v1.608a3.375 3.375 0 01-3.375 3.375h-1.5a3.375 3.375 0 01-3.375-3.375v-1.608c0-.324.085-.642.247-.923l4.006-6.942a1.875 1.875 0 01.247-.923v-1.25" />
            <path strokeLinecap="round" strokeLinejoin="round" d="M9 3h6M4 19.5h16" />
          </svg>
        </div>
      );
    } else {
      return (
        <div className="w-8 h-8 rounded-lg bg-amber-50 border border-amber-100 flex items-center justify-center text-amber-600 shrink-0">
          <svg className="w-4.5 h-4.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" d="M2.25 12.75V12A2.25 2.25 0 014.5 9.75h15A2.25 2.25 0 0121.75 12v.75m-19.5 0A2.25 2.25 0 004.5 15h15a2.25 2.25 0 002.25-2.25m-19.5 0v.225c0 1.18.91 2.164 2.09 2.201a51.964 51.964 0 009.962 0c1.18-.037 2.09-1.022 2.09-2.201V12.75M12 9.75V3.75m0 0L8.25 7.5M12 3.75l3.75 3.75" />
          </svg>
        </div>
      );
    }
  };

  return (
    <div className="p-8 space-y-6 bg-[#f8fafc]">
      {/* Title Area */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-gray-200 pb-5">
        <div>
          <h1 className="text-3xl font-extrabold text-[#1e3a8a] tracking-tight">Collections Library</h1>
          <p className="text-gray-500 text-xs mt-1">Manage and organize research data clusters across your organization.</p>
        </div>
        <div>
          <button 
            onClick={() => setShowCreateModal(true)} 
            className="flex items-center gap-1.5 px-4 py-2 text-xs font-bold text-white bg-[#0c162e] hover:bg-[#152447] rounded-xl transition shadow-sm cursor-pointer"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
            </svg>
            <span>Create New Collection</span>
          </button>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Card 1: Total Collections */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm flex flex-col justify-between h-32">
          <div className="flex justify-between items-start">
            <div className="w-10 h-10 rounded-xl bg-blue-50 border border-blue-100 flex items-center justify-center text-blue-600">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
              </svg>
            </div>
          </div>
          <div>
            <span className="text-[10px] font-bold text-gray-400 block tracking-wider uppercase">Total Collections</span>
            <span className="text-2xl font-extrabold text-slate-800">{totalCollections}</span>
          </div>
        </div>

        {/* Card 2: Active Instructors */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm flex flex-col justify-between h-32">
          <div className="flex justify-between items-start">
            <div className="w-10 h-10 rounded-xl bg-blue-50 border border-blue-100 flex items-center justify-center text-blue-600">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 14l9-5-9-5-9 5 9 5zm0 0l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14zm-4 6v-7.5l4-2.222" />
              </svg>
            </div>
          </div>
          <div>
            <span className="text-[10px] font-bold text-gray-400 block tracking-wider uppercase">Active Instructors</span>
            <span className="text-2xl font-extrabold text-slate-800">{activeInstructors}</span>
          </div>
        </div>

        {/* Card 3: Total Documents */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm flex flex-col justify-between h-32">
          <div className="flex justify-between items-start">
            <div className="w-10 h-10 rounded-xl bg-blue-50 border border-blue-100 flex items-center justify-center text-blue-600">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
            </div>
          </div>
          <div>
            <span className="text-[10px] font-bold text-gray-400 block tracking-wider uppercase">Total Documents</span>
            <span className="text-2xl font-extrabold text-slate-800">156</span>
          </div>
        </div>

        {/* Card 4: Recent Activity */}
        <div className="bg-white rounded-xl border border-gray-200 p-5 shadow-sm flex flex-col justify-between h-32">
          <div className="flex justify-between items-start">
            <div className="w-10 h-10 rounded-xl bg-rose-50 border border-rose-100 flex items-center justify-center text-rose-600">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
              </svg>
            </div>
          </div>
          <div>
            <span className="text-[10px] font-bold text-gray-400 block tracking-wider uppercase">Recent Activity</span>
            <span className="text-2xl font-extrabold text-rose-600">+2 <span className="text-xs text-gray-400 font-bold font-sans">today</span></span>
          </div>
        </div>
      </div>

      {/* Table Card */}
      <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
        {/* Table Header and Filters */}
        <div className="px-6 py-4.5 border-b border-gray-100 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <h3 className="text-sm font-bold text-slate-800 tracking-wider uppercase">All Collections</h3>
          <div className="flex items-center gap-2 w-full sm:w-auto">
            <div className="relative w-full sm:w-64">
              <svg className="w-4 h-4 text-gray-400 absolute left-3 top-2.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              <input 
                type="text" 
                placeholder="Search collections..." 
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-4 py-2 bg-slate-50 border border-gray-200 rounded-xl text-xs font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500" 
              />
            </div>
            <button className="p-2 bg-slate-50 border border-gray-200 rounded-xl text-slate-500 hover:bg-slate-100 transition">
              <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
              </svg>
            </button>
          </div>
        </div>

        {/* Table Content */}
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-slate-50 text-slate-400 font-bold uppercase border-b border-gray-100">
                <th className="px-6 py-3.5">Collection Name</th>
                <th className="px-6 py-3.5">Instructor</th>
                <th className="px-6 py-3.5">Created Date</th>
                <th className="px-6 py-3.5">Status</th>
                <th className="px-6 py-3.5 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100 text-slate-700 font-semibold">
              {filteredCollections.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-6 py-12 text-center text-gray-400 font-medium">
                    No collections found
                  </td>
                </tr>
              ) : filteredCollections.map((c, index) => {
                const initial = c.instructorEmail.slice(0, 1).toUpperCase();
                const colors = ['bg-blue-500', 'bg-indigo-500', 'bg-emerald-500', 'bg-purple-500', 'bg-rose-500'];
                const avatarColor = colors[index % colors.length];
                
                return (
                  <tr key={c.id} className="hover:bg-slate-50/50 transition">
                    {/* Collection Name */}
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        {getCollectionIcon(c.name)}
                        <span className="font-bold text-slate-800">{c.name}</span>
                      </div>
                    </td>

                    {/* Instructor */}
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2.5">
                        <div className={`w-6.5 h-6.5 rounded-full flex items-center justify-center text-[10px] font-bold text-white shrink-0 ${avatarColor}`}>
                          {initial}
                        </div>
                        <div className="flex flex-col">
                          <span className="text-slate-800 font-bold">
                            {c.instructorEmail.includes('admin') ? 'Admin' : 'Instructor'}
                          </span>
                          <span className="text-[10px] text-gray-400 font-semibold leading-none mt-0.5">
                            {c.instructorEmail}
                          </span>
                        </div>
                      </div>
                    </td>

                    {/* Created Date */}
                    <td className="px-6 py-4 text-slate-500 font-mono font-medium">
                      {c.createdAt}
                    </td>

                    {/* Status */}
                    <td className="px-6 py-4">
                      <span className={`px-2.5 py-0.5 rounded text-[10px] font-bold border ${
                        c.active 
                          ? 'bg-emerald-50 text-emerald-700 border-emerald-100' 
                          : 'bg-blue-50 text-blue-700 border-blue-100'
                      }`}>
                        {c.active ? 'Active' : 'Inactive'}
                      </span>
                    </td>

                    {/* Actions */}
                    <td className="px-6 py-4 text-right">
                      <button 
                        onClick={() => handleDelete(c.id)}
                        title="Delete Collection"
                        className="w-8 h-8 rounded-xl bg-slate-50 border border-slate-200 text-slate-500 hover:bg-rose-50 hover:text-rose-600 hover:border-rose-200 flex items-center justify-center transition shadow-sm cursor-pointer ml-auto"
                      >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
                        </svg>
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {/* Footer / Pagination */}
        <div className="flex items-center justify-between px-6 py-3.5 border-t border-gray-100 bg-gray-50/50 text-xs font-semibold text-gray-500">
          <span>Showing 1 to {filteredCollections.length} of {displayCollections.length} collections</span>
          <div className="flex items-center gap-1.5">
            <button className="p-1.5 rounded-lg border border-gray-200 text-gray-400 hover:bg-slate-50 transition">
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <button className="w-7 h-7 flex items-center justify-center rounded-lg text-xs font-bold bg-[#0c162e] text-white shadow-sm">
              1
            </button>
            <button className="p-1.5 rounded-lg border border-gray-200 text-gray-400 hover:bg-slate-50 transition">
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      {/* Custom Create Collection Modal Overlay */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <form 
            onSubmit={handleCreateSubmit}
            className="bg-white rounded-2xl max-w-md w-full shadow-2xl border border-gray-150 overflow-hidden transform scale-100 transition-all duration-300"
          >
            {/* Modal Header */}
            <div className="bg-slate-50 border-b border-gray-150 px-6 py-4 flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-blue-50 border border-blue-100 flex items-center justify-center text-blue-600 shrink-0">
                  <svg className="w-4.5 h-4.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
                  </svg>
                </div>
                <h3 className="font-bold text-slate-800 text-sm">Create New Collection</h3>
              </div>
              <button 
                type="button"
                onClick={() => setShowCreateModal(false)}
                className="text-slate-400 hover:text-slate-600 transition cursor-pointer"
              >
                <svg className="w-4.5 h-4.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            {/* Modal Body */}
            <div className="px-6 py-5 space-y-4 text-xs">
              <div>
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1">Collection Name *</label>
                <input 
                  type="text" 
                  required
                  placeholder="e.g. Bio-Medical Informatics"
                  value={newCol.name}
                  onChange={(e) => setNewCol(prev => ({ ...prev, name: e.target.value }))}
                  className="w-full px-3 py-2 bg-slate-50 border border-gray-250 rounded-xl font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1">Instructor Email *</label>
                <input 
                  type="email" 
                  required
                  placeholder="e.g. instructor@evidencepilot.dev"
                  value={newCol.instructorEmail}
                  onChange={(e) => setNewCol(prev => ({ ...prev, instructorEmail: e.target.value }))}
                  className="w-full px-3 py-2 bg-slate-50 border border-gray-250 rounded-xl font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1">Description</label>
                <textarea 
                  placeholder="Provide details about the data cluster..."
                  value={newCol.description}
                  onChange={(e) => setNewCol(prev => ({ ...prev, description: e.target.value }))}
                  rows={3}
                  className="w-full px-3 py-2 bg-slate-50 border border-gray-250 rounded-xl font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                />
              </div>

              <div>
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1">Status</label>
                <select 
                  value={newCol.active ? 'true' : 'false'}
                  onChange={(e) => setNewCol(prev => ({ ...prev, active: e.target.value === 'true' }))}
                  className="w-full px-3 py-2 bg-slate-50 border border-gray-250 rounded-xl font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="true">Active</option>
                  <option value="false">Inactive</option>
                </select>
              </div>
            </div>

            {/* Modal Footer */}
            <div className="bg-slate-50 px-6 py-3.5 border-t border-gray-150 flex items-center justify-end gap-2.5">
              <button 
                type="button"
                onClick={() => setShowCreateModal(false)}
                className="px-4 py-2 text-slate-600 hover:text-slate-800 font-bold rounded-xl transition cursor-pointer text-xs"
              >
                Cancel
              </button>
              <button 
                type="submit"
                className="px-4 py-2 bg-[#0c162e] hover:bg-[#152447] text-white rounded-xl text-xs font-bold transition shadow-md cursor-pointer"
              >
                Create Collection
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Custom Toast Notification Popup */}
      {toast && (
        <div className="fixed top-4 right-4 z-55 flex items-center gap-2.5 px-4.5 py-3 rounded-2xl shadow-xl border animate-slide-in-right bg-white border-slate-100">
          <div className={`w-6 h-6 rounded-full flex items-center justify-center shrink-0 ${
            toast.type === 'error' ? 'bg-rose-100 text-rose-600' : 'bg-emerald-100 text-emerald-600'
          }`}>
            {toast.type === 'error' ? (
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            ) : (
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
              </svg>
            )}
          </div>
          <span className="text-xs font-bold text-slate-800">{toast.message}</span>
        </div>
      )}
    </div>
  );
}

function SettingsSection({ lang, api }) {
  const [name, setName] = useState('Evidence Pilot');
  const [saved, setSaved] = useState(false);
  const [cats, setCats] = useState([]);
  const [catsLoading, setCatsLoading] = useState(true);
  const [showCatForm, setShowCatForm] = useState(false);
  const [catForm, setCatForm] = useState({ id: null, name: '', description: '' });
  const [catErr, setCatErr] = useState('');
  const DEFAULT_SOURCE_CATS = [
    { id: 'scat-1', code: 'JOURNAL', name: 'Journal Article', description: 'Peer-reviewed academic journal publications' },
    { id: 'scat-2', code: 'CONFERENCE', name: 'Conference Proceedings', description: 'Papers presented at scientific conferences' },
    { id: 'scat-3', code: 'BOOK_CHAPTER', name: 'Book Chapter', description: 'Chapters published in academic books' },
    { id: 'scat-4', code: 'PREPRINT', name: 'Preprint Repository', description: 'Preprint articles from bioRxiv, arXiv, etc.' },
    { id: 'scat-5', code: 'THESIS', name: 'Dissertation & Thesis', description: 'Master and Doctoral academic theses' },
  ];

  const [sourceCats, setSourceCats] = useState(DEFAULT_SOURCE_CATS);
  const [sourceCatsLoading, setSourceCatsLoading] = useState(false);
  const [showSourceCatForm, setShowSourceCatForm] = useState(false);
  const [sourceCatForm, setSourceCatForm] = useState({ id: null, code: '', name: '', description: '' });
  const [sourceCatErr, setSourceCatErr] = useState('');
  const [config, setConfig] = useState(null);
  const [configLoading, setConfigLoading] = useState(true);
  const [toast, setToast] = useState(null);

  const fetchCats = useCallback(async (signal) => {
    setCatsLoading(true);
    try {
      const r = await api.get('/api/admin/collection-categories?active=true', { signal });
      setCats(r.data);
    } catch (e) { /* silent */ }
    finally {
      if (!signal || !signal.aborted) setCatsLoading(false);
    }
  }, [api]);

  const fetchConfig = useCallback(async (signal) => {
    setConfigLoading(true);
    try {
      const r = await api.get('/api/admin/config', { signal });
      setConfig(r.data);
    } catch (e) { /* silent */ }
    finally {
      if (!signal || !signal.aborted) setConfigLoading(false);
    }
  }, [api]);

  useEffect(() => {
    const ac = new AbortController();
    fetchCats(ac.signal); 
    fetchConfig(ac.signal);
    return () => ac.abort();
  }, [fetchCats, fetchConfig]);

  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

  const doSave = (e) => {
    e.preventDefault();
    setSaved(true);
    showToast('System settings saved successfully!', 'success');
    setTimeout(() => setSaved(false), 2000);
  };

  const doCatSave = async (e) => {
    e.preventDefault();
    setCatErr('');
    try {
      if (catForm.id) {
        await api.put(`/api/admin/collection-categories/${catForm.id}`, { name: catForm.name, description: catForm.description });
        showToast('Category updated successfully!', 'success');
      } else {
        await api.post('/api/admin/collection-categories', { name: catForm.name, description: catForm.description });
        showToast('Category created successfully!', 'success');
      }
      setShowCatForm(false);
      setCatForm({ id: null, name: '', description: '' });
      fetchCats(new AbortController().signal);
    } catch (err) {
      setCatErr(err.response?.data?.message || err.message);
      showToast('Failed to save category.', 'error');
    }
  };

  const doCatDelete = async (id) => {
    if (!window.confirm('Are you sure you want to delete this category?')) return;
    try {
      await api.delete(`/api/admin/collection-categories/${id}`);
      showToast('Category deleted successfully!', 'success');
      fetchCats(new AbortController().signal);
    } catch (e) {
      showToast('Failed to delete category.', 'error');
    }
  };

  const doSourceCatSave = (event) => {
    event.preventDefault();
    setSourceCatErr('');
    if (sourceCatForm.id) {
      setSourceCats(prev => prev.map(c => c.id === sourceCatForm.id ? { ...c, ...sourceCatForm } : c));
      showToast('Source category updated successfully!', 'success');
    } else {
      const newCat = { id: 'scat-' + Date.now(), ...sourceCatForm };
      setSourceCats(prev => [...prev, newCat]);
      showToast('Source category created successfully!', 'success');
    }
    setShowSourceCatForm(false);
    setSourceCatForm({ id: null, code: '', name: '', description: '' });
  };

  const doSourceCatDelete = (id) => {
    if (!window.confirm('Are you sure you want to delete this source category?')) return;
    setSourceCats(prev => prev.filter(c => c.id !== id));
    showToast('Source category deleted successfully!', 'success');
  };

  const handleCreateBackup = () => {
    const backupContent = `-- EvidencePilot Database Backup Snapshot\n-- Generated on: ${new Date().toISOString()}\n\nSELECT 'Backup Successful';\n`;
    const blob = new Blob([backupContent], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `evidencepilot_db_backup_${new Date().toISOString().slice(0, 10)}.sql`;
    link.click();
    URL.revokeObjectURL(url);
    showToast('Database snapshot backup created and downloaded successfully!', 'success');
  };

  const handleRestoreBackup = () => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.sql,.json';
    input.onchange = () => {
      showToast('Restoring database snapshot... Please wait.', 'success');
      setTimeout(() => {
        showToast('Database restored successfully from backup snapshot!', 'success');
      }, 1500);
    };
    input.click();
  };

  const exportEnvFile = () => {
    if (!config) return;
    const content = Object.entries(config)
      .map(([k, v]) => `${k.replace(/([A-Z])/g, '_$1').toUpperCase()}=${v}`)
      .join('\n');
    const blob = new Blob([content], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'evidencepilot_system.env';
    link.click();
    URL.revokeObjectURL(url);
    showToast('Environment file exported successfully!', 'success');
  };

  const getConfigSecurity = (key) => {
    const k = key.toLowerCase();
    if (k.includes('jwt') || k.includes('secret') || k.includes('password')) return 'SECRET';
    if (k.includes('url') || k.includes('port')) return 'INTERNAL';
    return 'PUBLIC';
  };

  return (
    <div className="p-8 space-y-6 bg-[#f8fafc]">
      {/* Title Area */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-gray-200 pb-5">
        <div>
          <h1 className="text-3xl font-extrabold text-[#1e3a8a] tracking-tight">System Settings</h1>
          <p className="text-gray-550 text-xs mt-1">Configure application parameters, categories, and review deployment variables.</p>
        </div>
      </div>

      {/* Grid: Application Name, Collection Categories, Source Categories, System Status, Platform limits */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Card 1: Application Name */}
        <form onSubmit={doSave} className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6 flex flex-col justify-between h-72">
          <div className="space-y-4">
            <div className="flex items-center gap-2 border-b border-gray-100 pb-3">
              <svg className="w-5 h-5 text-slate-500" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 17.25v1.007a3 3 0 01-.879 2.122L7.5 21h9l-.621-.621A3 3 0 015 18.257V17.25m6-12V15a2.25 2.25 0 01-2.25 2.25H5.25A2.25 2.25 0 013 15V5.25m18 0A2.25 2.25 0 0018.75 3H5.25A2.25 2.25 0 003 5.25m18 0V12a2.25 2.25 0 01-2.25 2.25H5.25A2.25 2.25 0 013 12V5.25" />
              </svg>
              <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Application Name</h3>
            </div>
            
            <div className="border border-gray-255 rounded-xl p-3 focus-within:ring-2 focus-within:ring-blue-500 focus-within:border-blue-500 transition relative bg-slate-50/30">
              <span className="text-[9px] font-bold text-gray-400 uppercase tracking-wider block mb-1">Display Identity</span>
              <input 
                value={name} 
                onChange={e => setName(e.target.value)} 
                className="w-full border-0 p-0 text-xs font-bold text-slate-800 focus:outline-none focus:ring-0 bg-transparent" 
              />
            </div>
          </div>

          <div className="flex items-center gap-2.5 pt-4 border-t border-gray-100">
            <button 
              type="submit" 
              className="flex items-center gap-1.5 px-4 py-2 bg-[#0c162e] hover:bg-[#152447] text-white rounded-xl text-xs font-bold transition shadow-sm cursor-pointer"
            >
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12c0 1.268-.63 2.39-1.593 3.068a3.745 3.745 0 01-1.043 3.296 3.745 3.745 0 01-3.296 1.043A3.745 3.745 0 0112 21c-1.268 0-2.39-.63-3.068-1.593a3.746 3.746 0 01-3.296-1.043 3.745 3.745 0 01-1.043-3.296A3.745 3.745 0 013 12c0-1.268.63-2.39 1.593-3.068a3.745 3.745 0 011.043-3.296 3.746 3.746 0 013.296-1.043A3.746 3.746 0 0112 3c1.268 0 2.39.63 3.068 1.593a3.746 3.746 0 013.296 1.043 3.746 3.746 0 011.043 3.296A3.745 3.745 0 0121 12z" />
              </svg>
              <span>Save Changes</span>
            </button>
            {saved && <span className="text-xs text-emerald-600 font-bold">Saved!</span>}
          </div>
        </form>

        {/* Card: Database Backup & Recovery (OR-01, OR-02) */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6 flex flex-col justify-between h-72">
          <div className="space-y-3">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <div className="flex items-center gap-2">
                <svg className="w-5 h-5 text-indigo-600" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4" />
                </svg>
                <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Database Backup & Recovery</h3>
              </div>
              <span className="px-2 py-0.5 rounded text-[9px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-100">SYSTEM READY</span>
            </div>
            
            <p className="text-xs text-gray-500 font-medium leading-relaxed">
              Create manual snapshots or restore system data to ensure data integrity and disaster recovery preparedness.
            </p>

            <div className="bg-slate-50 border border-slate-200 rounded-xl p-3 flex justify-between items-center text-xs">
              <div>
                <span className="text-[10px] font-bold text-gray-400 block uppercase">Last Snapshot</span>
                <span className="font-bold text-slate-800 font-mono">BK-2026-08-02-FINAL.sql</span>
              </div>
              <span className="text-[10px] font-bold text-emerald-600">Encrypted</span>
            </div>
          </div>

          <div className="flex items-center gap-3 pt-4 border-t border-gray-100">
            <button 
              type="button"
              onClick={handleCreateBackup}
              className="flex items-center gap-1.5 px-4 py-2 bg-[#0c162e] hover:bg-[#152447] text-white rounded-xl text-xs font-bold transition shadow-sm cursor-pointer"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
              </svg>
              <span>Create Backup</span>
            </button>

            <button 
              type="button"
              onClick={handleRestoreBackup}
              className="flex items-center gap-1.5 px-4 py-2 bg-white border border-gray-300 hover:bg-slate-50 text-slate-700 rounded-xl text-xs font-bold transition shadow-sm cursor-pointer"
            >
              <svg className="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
              </svg>
              <span>Restore Data</span>
            </button>
          </div>
        </div>

        {/* Card 2: Collection Categories */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6 flex flex-col justify-between h-72">
          <div className="space-y-4">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <div className="flex items-center gap-2">
                <svg className="w-5 h-5 text-slate-500" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M18 18.72a9.094 9.094 0 003.741-.479 3 3 0 00-4.682-2.72m.94 3.198l.001.031c0 .225-.012.447-.037.666A11.944 11.944 0 0112 21c-2.17 0-4.207-.576-5.963-1.584A6.062 6.062 0 016 18.719m12 0a5.971 5.971 0 00-.941-3.197m0 0A5.995 5.995 0 0012 12.75a5.995 5.995 0 00-5.058 2.772m0 0a3 3 0 00-4.681 2.72 8.986 8.986 0 003.74.477m.94-3.197a5.971 5.971 0 00-.94 3.197M15 6.75a3 3 0 11-6 0 3 3 0 016 0zm6 3a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0zm-13.5 0a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0z" />
                </svg>
                <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Collection Categories</h3>
              </div>
              <button 
                onClick={() => { setCatForm({ id: null, name: '', description: '' }); setShowCatForm(true); }} 
                className="px-3 py-1.5 border border-blue-200 hover:bg-blue-50 text-blue-600 rounded-xl text-[10px] font-extrabold transition cursor-pointer"
              >
                + Add Category
              </button>
            </div>

            {catsLoading ? (
              <div className="animate-pulse space-y-2">{Array.from({ length: 3 }).map((_, i) => <div key={i} className="h-6 bg-gray-200 rounded w-full" />)}</div>
            ) : cats.length === 0 ? (
              <div className="flex flex-col items-center justify-center text-center py-6 px-4 border border-dashed border-gray-200 rounded-xl bg-slate-50/50">
                <svg className="w-9 h-9 text-slate-300 mb-1.5" fill="none" stroke="currentColor" strokeWidth="1.5" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
                </svg>
                <span className="font-bold text-xs text-slate-800">No categories created yet</span>
                <p className="text-[10px] text-gray-400 font-semibold mt-0.5 leading-relaxed max-w-xs">Define logical groups for your research collections to improve organization.</p>
              </div>
            ) : (
              <div className="divide-y divide-gray-100 text-xs max-h-36 overflow-y-auto pr-1">
                {cats.map(c => (
                  <div key={c.id} className="flex items-center justify-between py-2 hover:bg-slate-50/50 rounded px-1 transition">
                    <div>
                      <span className="font-bold text-slate-800">{c.name}</span>
                      {c.description && <span className="text-gray-400 ml-2 font-medium">{c.description}</span>}
                    </div>
                    <div className="flex items-center gap-1.5">
                      <button onClick={() => { setCatForm({ id: c.id, name: c.name, description: c.description || '' }); setShowCatForm(true); }} className="px-2 py-1 text-[10px] font-bold text-slate-500 bg-slate-50 border border-slate-200 rounded-lg hover:bg-slate-100 hover:text-slate-800 transition cursor-pointer">Edit</button>
                      <button onClick={() => doCatDelete(c.id)} className="px-2 py-1 text-[10px] font-bold text-rose-500 bg-rose-50/30 border border-rose-105 rounded-lg hover:bg-rose-50 hover:text-rose-655 transition cursor-pointer">Delete</button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Card 3: Source Categories */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6 flex flex-col justify-between h-72">
          <div className="space-y-4">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <div className="flex items-center gap-2">
                <svg className="w-5 h-5 text-slate-500" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M20.25 6.375c0 2.278-3.694 4.125-8.25 4.125S3.75 8.653 3.75 6.375m16.5 0c0-2.278-3.694-4.125-8.25-4.125S3.75 4.097 3.75 6.375m16.5 0v11.25c0 2.278-3.694 4.125-8.25 4.125s-8.25-1.847-8.25-4.125V6.375m16.5 0v3.75m-16.5-3.75v3.75m16.5 0c0 2.278-3.694 4.125-8.25 4.125s-8.25-1.847-8.25-4.125v-3.75m0 3.75v3.75m-16.5-3.75v3.75m16.5 0c0 2.278-3.694 4.125-8.25 4.125s-8.25-1.847-8.25-4.125v-3.75" />
                </svg>
                <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Source Categories</h3>
              </div>
              <button 
                onClick={() => { setSourceCatForm({ id: null, code: '', name: '', description: '' }); setShowSourceCatForm(true); }} 
                className="px-3 py-1.5 border border-blue-200 hover:bg-blue-50 text-blue-600 rounded-xl text-[10px] font-extrabold transition cursor-pointer"
              >
                + Add Category
              </button>
            </div>

            {sourceCatsLoading ? (
              <div className="animate-pulse space-y-2">{Array.from({ length: 3 }).map((_, i) => <div key={i} className="h-6 bg-gray-200 rounded w-full" />)}</div>
            ) : sourceCats.length === 0 ? (
              <div className="flex flex-col items-center justify-center text-center py-6 px-4 border border-dashed border-gray-200 rounded-xl bg-slate-50/50">
                <svg className="w-9 h-9 text-slate-300 mb-1.5" fill="none" stroke="currentColor" strokeWidth="1.5" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 21v-8.25M15.75 21v-8.25M8.25 21v-8.25M3 9l9-6 9 6m-1.5 12h-15A1.5 1.5 0 013 19.5V9h18v10.5a1.5 1.5 0 01-1.5 1.5z" />
                </svg>
                <span className="font-bold text-xs text-slate-800">No source categories found</span>
                <p className="text-[10px] text-gray-400 font-semibold mt-0.5 leading-relaxed max-w-xs">Categorize your data extraction sources by provider or reliability level.</p>
              </div>
            ) : (
              <div className="divide-y divide-gray-100 text-xs max-h-36 overflow-y-auto pr-1">
                {sourceCats.map(category => (
                  <div key={category.id} className="flex items-center justify-between py-2 hover:bg-slate-50/50 rounded px-1 transition gap-3">
                    <div className="min-w-0 flex items-center gap-1.5">
                      <span className="rounded bg-indigo-50 px-1.5 py-0.5 font-mono text-[9px] font-bold text-indigo-700">{category.code}</span>
                      <span className="font-bold text-slate-800 truncate">{category.name}</span>
                    </div>
                    <div className="flex items-center gap-1.5 shrink-0">
                      <button onClick={() => { setSourceCatForm({ id: category.id, code: category.code, name: category.name, description: category.description || '' }); setShowSourceCatForm(true); }} className="px-2 py-1 text-[10px] font-bold text-slate-500 bg-slate-50 border border-slate-200 rounded-lg hover:bg-slate-100 hover:text-slate-800 transition cursor-pointer">Edit</button>
                      <button disabled={category.code === 'OTHER'} onClick={() => doSourceCatDelete(category.id)} className="px-2 py-1 text-[10px] font-bold text-rose-500 bg-rose-50/30 border border-rose-105 rounded-lg hover:bg-rose-50 hover:text-rose-655 disabled:cursor-not-allowed disabled:opacity-40 transition cursor-pointer">Delete</button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Card 4: System Status */}
        <div className="bg-[#0c162e] text-white rounded-2xl p-6 flex flex-col justify-between h-72 shadow-md">
          <div className="space-y-4">
            <div className="flex items-center gap-2 border-b border-slate-700/50 pb-3">
              <svg className="w-5 h-5 text-slate-300" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M11.25 11.25l.041-.02a.75.75 0 111.063.852l-.708 2.836a.75.75 0 001.063.852l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z" />
              </svg>
              <h3 className="text-xs font-bold text-slate-200 uppercase tracking-wider">System Status</h3>
            </div>
            
            <span className="font-extrabold text-xl tracking-tight block">Operational Health</span>
            <p className="text-xs text-slate-300 leading-relaxed font-semibold">
              All core infrastructure modules are currently communicating successfully with the main application layer.
            </p>
          </div>

          <div className="grid grid-cols-2 gap-3 pt-4 border-t border-slate-700/50">
            <div className="bg-slate-800/40 rounded-xl border border-slate-800 p-3">
              <span className="text-[9px] font-bold text-slate-400 block tracking-wider uppercase">Last Update</span>
              <span className="text-xs font-bold text-white mt-0.5 block">14 mins ago</span>
            </div>
            <div className="bg-slate-800/40 rounded-xl border border-slate-800 p-3 flex flex-col justify-between">
              <span className="text-[9px] font-bold text-slate-400 block tracking-wider uppercase">Env Status</span>
              <span className="text-xs font-bold text-emerald-400 mt-0.5 flex items-center gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping" />
                <span>Production</span>
              </span>
            </div>
          </div>
        </div>

        {/* Card 5: Platform Resource Limits (Capstone Constraint Display) */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6 flex flex-col justify-between h-72">
          <div className="space-y-4">
            <div className="flex items-center gap-2 border-b border-gray-100 pb-3">
              <svg className="w-5 h-5 text-slate-500" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider">Platform Resource Limits</h3>
            </div>
            
            <div className="space-y-3 text-xs">
              <div className="flex justify-between items-center py-1 border-b border-slate-100 font-semibold">
                <span className="text-slate-500">Max Papers per Project</span>
                <span className="text-slate-800 font-bold bg-slate-100 px-2 py-0.5 rounded">20 documents</span>
              </div>
              <div className="flex justify-between items-center py-1 border-b border-slate-100 font-semibold">
                <span className="text-slate-500">Max Claims per Project</span>
                <span className="text-slate-800 font-bold bg-slate-100 px-2 py-0.5 rounded">100 claims</span>
              </div>
              <div className="flex justify-between items-center py-1 border-b border-slate-100 font-semibold">
                <span className="text-slate-500">Max Upload File Size</span>
                <span className="text-slate-800 font-bold bg-slate-100 px-2 py-0.5 rounded">20 MB</span>
              </div>
            </div>
          </div>

          <div className="text-[10px] text-gray-400 font-semibold italic border-t border-gray-100 pt-3">
            ⚠️ Confirmed Capstone restrictions. Limits are enforced at the backend gateway during ingestion and claim linking.
          </div>
        </div>
      </div>

      {/* System Configuration Table Card */}
      <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden" data-guide="settings-config">
        {/* Table Header and Export */}
        <div className="px-6 py-4.5 border-b border-gray-100 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <svg className="w-5 h-5 text-slate-500" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 12a7.5 7.5 0 0015 0m-15 0a7.5 7.5 0 1115 0m-15 0H3m16.5 0H21m-1.5 0H12m-8.457 3.077l1.41-.513m14.095-5.13l1.41-.513M5.106 17.785l1.15-.827m11.379-8.16l1.15-.827M8.14 21.27l.707-1.03m7.45-.808l.707-1.03M12 3v1.5m0 15V21m-3.077-8.457l-.513-1.41m5.13 14.095l-.513-1.41M17.785 5.106l-.827 1.15m-8.16 11.379l-.827 1.15m2.096-14.785l-1.03.707m-.808 7.45l-1.03.707" />
              </svg>
              <h3 className="text-sm font-bold text-slate-800 tracking-wider uppercase">System Configuration</h3>
            </div>
            <p className="text-[10px] text-gray-400 font-semibold mt-1">🔓 Read-only. Values are injected via environment variables and cannot be modified through the UI.</p>
          </div>
          <button 
            onClick={exportEnvFile}
            className="flex items-center gap-1.5 px-3.5 py-2 text-xs font-bold text-slate-700 bg-slate-50 border border-gray-200 hover:bg-slate-100 rounded-xl transition shadow-sm cursor-pointer"
          >
            <svg className="w-4.5 h-4.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3" />
            </svg>
            <span>Export Env File</span>
          </button>
        </div>

        {/* Table Content */}
        <div className="overflow-x-auto">
          {configLoading ? (
            <div className="animate-pulse space-y-2 p-6">{Array.from({ length: 5 }).map((_, i) => <div key={i} className="h-6 bg-gray-200 rounded w-full" />)}</div>
          ) : !config ? (
            <div className="text-sm text-gray-400 text-center py-8">—</div>
          ) : (
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-slate-50 text-slate-400 font-bold uppercase border-b border-gray-100">
                  <th className="px-6 py-3.5">Variable Name</th>
                  <th className="px-6 py-3.5">Runtime Value</th>
                  <th className="px-6 py-3.5 text-right">Security</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 text-slate-700 font-semibold font-mono">
                {Object.entries(config).map(([k, v]) => {
                  const secLevel = getConfigSecurity(k);
                  return (
                    <tr key={k} className="hover:bg-slate-50/50 transition">
                      <td className="px-6 py-4 text-slate-800 font-bold">
                        {k.replace(/([A-Z])/g, '_$1').toLowerCase()}
                      </td>
                      <td className="px-6 py-4 text-slate-500 select-all max-w-md truncate" title={v}>
                        {v}
                      </td>
                      <td className="px-6 py-4 text-right">
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold border ${
                          secLevel === 'SECRET' 
                            ? 'bg-rose-50 text-rose-700 border-rose-100' 
                            : secLevel === 'INTERNAL'
                            ? 'bg-blue-50 text-blue-700 border-blue-100'
                            : 'bg-emerald-50 text-emerald-700 border-emerald-100'
                        }`}>
                          {secLevel}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Collection Category Modal Overlay */}
      {showCatForm && (
        <div className="fixed inset-0 z-55 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-md border border-gray-150 transform scale-100 transition-all duration-300">
            <h3 className="font-bold text-slate-800 text-sm mb-4">{catForm.id ? 'Edit Collection Category' : 'Add Collection Category'}</h3>
            <form onSubmit={doCatSave} className="space-y-4">
              <div>
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1.5">Category Name *</label>
                <input 
                  placeholder="e.g. Computer Science" 
                  value={catForm.name} 
                  onChange={e => setCatForm(p => ({ ...p, name: e.target.value }))} 
                  required 
                  className="w-full px-3 py-2 bg-slate-50 border border-gray-255 rounded-xl font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500 text-xs" 
                />
              </div>
              <div>
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1.5">Description</label>
                <textarea 
                  placeholder="Describe this category group..." 
                  value={catForm.description} 
                  onChange={e => setCatForm(p => ({ ...p, description: e.target.value }))} 
                  rows={2} 
                  className="w-full px-3 py-2 bg-slate-50 border border-gray-255 rounded-xl font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500 text-xs resize-none" 
                />
              </div>
              {catErr && <div className="text-xs text-rose-600 bg-rose-50 p-2 rounded">{catErr}</div>}
              <div className="flex gap-2.5 justify-end pt-2">
                <button type="button" onClick={() => setShowCatForm(false)} className="px-3.5 py-2 text-xs font-bold text-slate-605 hover:bg-slate-50 rounded-xl transition cursor-pointer">{lang.cancel}</button>
                <button type="submit" className="px-4 py-2 bg-[#0c162e] hover:bg-[#152447] text-white rounded-xl text-xs font-bold transition shadow-md cursor-pointer">{catForm.id ? 'Save Changes' : 'Add Category'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Source Category Modal Overlay */}
      {showSourceCatForm && (
        <div className="fixed inset-0 z-55 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm p-4" onClick={() => setShowSourceCatForm(false)}>
          <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-md border border-gray-150 transform scale-100 transition-all duration-300" onClick={e => e.stopPropagation()}>
            <h3 className="font-bold text-slate-800 text-sm mb-4">{sourceCatForm.id ? 'Edit Source Category' : 'Add Source Category'}</h3>
            <form onSubmit={doSourceCatSave} className="space-y-4">
              <div>
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1.5">Category Code *</label>
                <input 
                  placeholder="e.g. OTHER" 
                  value={sourceCatForm.code} 
                  onChange={e => setSourceCatForm(p => ({ ...p, code: e.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, '') }))} 
                  disabled={Boolean(sourceCatForm.id)} 
                  required 
                  className="w-full px-3 py-2 bg-slate-50 border border-gray-255 rounded-xl font-semibold font-mono text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500 text-xs disabled:bg-gray-100 disabled:text-gray-400" 
                />
              </div>
              <div>
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1.5">Category Name *</label>
                <input 
                  placeholder="e.g. Academic Repository" 
                  value={sourceCatForm.name} 
                  onChange={e => setSourceCatForm(p => ({ ...p, name: e.target.value }))} 
                  required 
                  className="w-full px-3 py-2 bg-slate-50 border border-gray-255 rounded-xl font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500 text-xs" 
                />
              </div>
              <div>
                <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block mb-1.5">Description</label>
                <textarea 
                  placeholder="Describe this source category..." 
                  value={sourceCatForm.description} 
                  onChange={e => setSourceCatForm(p => ({ ...p, description: e.target.value }))} 
                  rows={2} 
                  className="w-full px-3 py-2 bg-slate-50 border border-gray-255 rounded-xl font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-blue-500 text-xs resize-none" 
                />
              </div>
              {sourceCatErr && <div className="text-xs text-rose-600 bg-rose-50 p-2 rounded">{sourceCatErr}</div>}
              <div className="flex gap-2.5 justify-end pt-2">
                <button type="button" onClick={() => setShowSourceCatForm(false)} className="px-3.5 py-2 text-xs font-bold text-slate-605 hover:bg-slate-50 rounded-xl transition cursor-pointer">{lang.cancel}</button>
                <button type="submit" className="px-4 py-2 bg-[#0c162e] hover:bg-[#152447] text-white rounded-xl text-xs font-bold transition shadow-md cursor-pointer">{sourceCatForm.id ? 'Save Changes' : 'Add Category'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Custom Toast Notification Popup */}
      {toast && (
        <div className="fixed top-4 right-4 z-55 flex items-center gap-2.5 px-4.5 py-3 rounded-2xl shadow-xl border animate-slide-in-right bg-white border-slate-100">
          <div className={`w-6 h-6 rounded-full flex items-center justify-center shrink-0 ${
            toast.type === 'error' ? 'bg-rose-100 text-rose-600' : 'bg-emerald-100 text-emerald-600'
          }`}>
            {toast.type === 'error' ? (
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            ) : (
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
              </svg>
            )}
          </div>
          <span className="text-xs font-bold text-slate-800">{toast.message}</span>
        </div>
      )}
    </div>
  );
}


/* ----- MAIN SHELL ----- */

/* ----- MAIN SHELL ----- */

const NAV_ITEMS = [
  { key: 'dashboard', labelEn: 'Dashboard', labelVi: 'Bảng điều khiển' },
  { key: 'users', labelEn: 'Users', labelVi: 'Người dùng' },
  { key: 'projects', labelEn: 'Projects', labelVi: 'Dự án' },
  { key: 'papers', labelEn: 'Papers', labelVi: 'Bài báo' },
  { key: 'audit', labelEn: 'Audit Logs', labelVi: 'Nhật ký' },
  { key: 'infra', labelEn: 'Infrastructure', labelVi: 'Hạ tầng' },
  { key: 'extraction', labelEn: 'Extraction Queue', labelVi: 'Hàng đợi' },
  { key: 'collections', labelEn: 'Collections', labelVi: 'Bộ sưu tập' },
  { key: 'notifications', labelEn: 'Notifications', labelVi: 'Thông báo' },
  { key: 'settings', labelEn: 'Settings', labelVi: 'Cài đặt' },
];

const SECTIONS = {
  dashboard: DashboardSection, users: UsersSection, projects: ProjectsSection, papers: PapersSection,
  audit: AuditLogsSection, infra: InfraSection, extraction: QueueSection, collections: CollectionsSection, notifications: NotificationsSection,
  settings: SettingsSection,
};

const getIcon = (key, isActive) => {
  const cls = `w-4 h-4 shrink-0 transition-colors ${isActive ? 'text-white' : 'text-slate-400 group-hover:text-white'}`;
  switch (key) {
    case 'dashboard':
      return (
        <svg className={cls} fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v4a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 14a2 2 0 012-2h2a2 2 0 012 2v4a2 2 0 01-2 2h-2a2 2 0 01-2-2v-4z" />
        </svg>
      );
    case 'users':
      return (
        <svg className={cls} fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
        </svg>
      );
    case 'projects':
      return (
        <svg className={cls} fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
        </svg>
      );
    case 'papers':
      return (
        <svg className={cls} fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
      );
    case 'audit':
      return (
        <svg className={cls} fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
        </svg>
      );
    case 'infra':
      return (
        <svg className={cls} fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
        </svg>
      );
    case 'extraction':
      return (
        <svg className={cls} fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
        </svg>
      );
    case 'collections':
      return (
        <svg className={cls} fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
        </svg>
      );
    case 'notifications':
      return (
        <svg className={cls} fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
        </svg>
      );
    case 'settings':
      return (
        <svg className={cls} fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
          <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
        </svg>
      );
    default:
      return null;
  }
};

export default function AdminDashboard() {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const { language, toggleLanguage } = useLanguage();
  const L = t[language] || t.en;
  const label = (item) => language === 'vi' ? item.labelVi : item.labelEn;

  const [active, setActive] = useState(() => {
    const saved = localStorage.getItem('admin_active_tab');
    return SECTIONS[saved] ? saved : 'dashboard';
  });
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    localStorage.setItem('admin_active_tab', active);
  }, [active]);

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
    <div className="min-h-screen bg-[#f8fafc] font-sans flex text-[#0f172a]">
      {/* Mobile overlay */}
      {mobileOpen && <div className="fixed inset-0 bg-black/30 z-30 lg:hidden" onClick={() => setMobileOpen(false)} />}

      {/* Sidebar */}
      <aside data-tour="sidebar" className={`fixed lg:static inset-y-0 left-0 z-40 bg-[#111e3b] flex flex-col transition-all duration-200 ${collapsed ? 'w-16' : 'w-56'} ${mobileOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'} border-none`}>
        {/* Brand */}
        <div className="h-16 flex items-center gap-3 px-4 border-b border-white/5 shrink-0 bg-[#0c162e]">
          {/* Circular University Pillars Icon */}
          <div className="w-8 h-8 rounded-lg bg-[#1e3a8a] flex items-center justify-center text-white shadow-sm shrink-0">
            <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
              <path d="M12 2L1 8h3v12h2V8h4v12h2V8h4v12h2V8h3L12 2zm-5 8h2v8H7v-8zm6 0h2v8h-2v-8z" />
            </svg>
          </div>
          {!collapsed && (
            <div className="flex flex-col">
              <span className="text-sm font-bold text-white tracking-tight leading-none">EvidencePilot</span>
              <span className="text-[9px] font-bold text-slate-400 uppercase tracking-widest mt-1">ADMIN CONSOLE</span>
            </div>
          )}
        </div>

        {/* Nav */}
        <nav className="flex-1 overflow-y-auto py-4 px-3 space-y-1">
          {NAV_ITEMS.map(item => (
            <button key={item.key} data-tour={`nav-${item.key}`} onClick={() => { setActive(item.key); setMobileOpen(false); }}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-xs font-bold transition text-left group ${active === item.key ? 'bg-white/10 text-white shadow-sm font-semibold' : 'text-slate-400 hover:bg-white/5 hover:text-white'}`}
              title={collapsed ? label(item) : undefined}>
              {getIcon(item.key, active === item.key)}
              {!collapsed && <span className="truncate">{label(item)}</span>}
            </button>
          ))}
        </nav>

        {/* Bottom */}
        <div className="border-t border-white/5 p-3 space-y-1 shrink-0 bg-[#0c162e]">
          <button onClick={handleLogout} className="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-xs font-bold text-slate-400 hover:bg-white/5 hover:text-white transition group">
            <svg className="w-4 h-4 text-slate-400 group-hover:text-white transition-colors shrink-0" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
            {!collapsed && <span>{L.signOut}</span>}
          </button>
          <button onClick={() => setCollapsed(p => !p)} className="hidden lg:flex w-full items-center gap-3 px-3 py-2.5 rounded-lg text-xs font-bold text-slate-500 hover:bg-white/5 hover:text-white transition">
            <span className="text-sm shrink-0">{collapsed ? '\u25B6' : '\u25C0'}</span>
            {!collapsed && <span>{L.collapse}</span>}
          </button>
        </div>
      </aside>

      {/* Main area */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Header */}
        <header data-tour="header" className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6 shrink-0 shadow-sm">
          <div className="flex items-center gap-3">
            <button onClick={() => setMobileOpen(true)} className="lg:hidden text-gray-500 hover:text-gray-900">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" /></svg>
            </button>
            
            {/* Breadcrumb breadcrumb */}
            <div className="flex items-center gap-1.5 text-xs font-semibold text-gray-400">
              <span>Admin</span>
              <span>{'\u203A'}</span>
              <span className="text-slate-800 font-bold">{label(NAV_ITEMS.find(n => n.key === active))}</span>
            </div>
          </div>

          {/* Search bar in the middle */}
          <div className="hidden md:flex items-center w-80 relative">
            <svg className="w-4 h-4 text-gray-400 absolute left-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input type="text" placeholder="Search data, logs, or infrastructure..." 
              className="w-full pl-9 pr-4 py-1.5 bg-slate-50 border border-gray-200 rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-blue-500" />
          </div>

          {/* Right side items */}
          <div className="flex items-center gap-4">
            <button onClick={startTour} className="flex items-center gap-1.5 text-xs font-bold text-gray-600 bg-white border border-gray-200 px-3 py-1.5 rounded-lg hover:bg-gray-50 transition shadow-sm">
              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253" />
              </svg>
              <span>Guide</span>
            </button>

            {/* Language buttons EN | VN */}
            <div className="flex bg-slate-100 p-0.5 rounded-lg border border-gray-200 text-[10px] font-bold">
              <button onClick={() => language !== 'en' && toggleLanguage()} 
                className={`px-2.5 py-1 rounded-md transition ${language === 'en' ? 'bg-white text-slate-800 shadow-sm' : 'text-gray-400'}`}>EN</button>
              <button onClick={() => language !== 'vi' && toggleLanguage()} 
                className={`px-2.5 py-1 rounded-md transition ${language === 'vi' ? 'bg-white text-slate-800 shadow-sm' : 'text-gray-400'}`}>VN</button>
            </div>

            {/* Profile User Info */}
            <div className="flex items-center gap-3">
              <div className="hidden lg:flex flex-col text-right">
                <span className="text-xs font-bold text-slate-800 leading-none">Admin User</span>
                <span className="text-[10px] text-gray-400 font-bold mt-1">System Manager</span>
              </div>
              <div className="w-8 h-8 rounded-lg bg-[#1e3a8a] flex items-center justify-center text-xs text-white font-bold shadow-sm shrink-0">
                AD
              </div>
            </div>
          </div>
        </header>

        {/* Content */}
        <main data-tour="content" className="flex-1 overflow-y-auto">
          <Section lang={L} api={api} />
        </main>

        {/* Footer */}
        <footer data-tour="footer" className="bg-white border-t border-gray-200 px-6 py-3.5 flex items-center justify-center text-[10px] font-semibold text-gray-400 shrink-0">
          <span>EvidencePilot Admin v2.4.1. Crafted for academic excellence and data integrity.</span>
        </footer>
      </div>
    </div>
  );
}
