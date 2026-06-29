// Centralized Mock Data Hub for Student, Instructor, and Admin Workspaces
export const initialMockData = {
  // ==========================================
  // 1. SYSTEM SECURITY & USER PROFILES
  // ==========================================
  userProfile: {
    id: "usr_mock_999",
    firstName: "Nguyen",
    lastName: "Van A",
    email: "vana@fpt.edu.vn",
    role: "INSTRUCTOR" // Can be switched to 'STUDENT' or 'ADMIN' for UI testing
  },

  adminProfile: {
  id: "usr_admin_root",
  firstName: "Root",
  lastName: "Administrator",
  email: "admin.root@fpt.edu.vn",
  role: "ADMIN"
  },
  // ==========================================
  // 2. INFRASTRUCTURE PROJECT REPOSITORIES
  // ==========================================
  projects: [
    { 
      id: "proj_101", 
      title: "Advanced Software Architecture 2026", 
      active: true,
      description: "Core master course focusing on high-availability system designs, load balancing, and microservices mesh patterns."
    },
    { 
      id: "proj_102", 
      title: "Cloud-Native Microservices Lab", 
      active: true,
      description: "Hands-on implementation workspace utilizing Kubernetes clusters, Docker configurations, and automated CI/CD pipelines."
    },
    { 
      id: "proj_103", 
      title: "AI-Driven DevOps Pipeline Blueprint", 
      active: true,
      description: "Experimental research domain exploring AI telemetry agents and structural checking rules inside evaluation matrix layouts."
    }
  ],

  // ==========================================
  // 3. EVIDENCE LIBRARIES & COLLECTIONS
  // ==========================================
  collections: [
    {
      id: "col_881",
      title: "Autumn 2026 Software Architecture Core Metrics Template",
      description: "Baseline checking rules layout context and expected proof documentation structures for system metrics calculation algorithms.",
      documentCount: 5,
      createdAt: "2026-06-15"
    },
    {
      id: "col_882",
      title: "ISO 27001 Security Baseline Verification Library",
      description: "Cryptographic specifications, data protection protocols, and evaluation matrix requirements for production deployment bounds.",
      documentCount: 3,
      createdAt: "2026-06-18"
    },
    {
      id: "col_883",
      title: "Kubernetes Cluster Deployment Manifest Proofs",
      description: "Required deployment structure evidence, network policies, and persistent volume configuration baselines.",
      documentCount: 0,
      createdAt: "2026-06-25"
    }
  ],

  // ==========================================
  // 4. STUDENT VERIFICATION & FEEDBACK QUEUE
  // ==========================================
  feedbackRequests: [
    {
      id: "req_v101",
      projectTitle: "E-Commerce High-Availability Mesh System",
      status: "PENDING",
      submittedBy: "Student Cluster Alpha",
      dateSubmitted: "2026-06-28"
    },
    {
      id: "req_v102",
      projectTitle: "Automated Banking Reconciliation Pipeline",
      status: "REVIEWED",
      submittedBy: "Student Cluster Beta",
      dateSubmitted: "2026-06-20"
    },
    {
      id: "req_v103",
      projectTitle: "Distributed Log Ledger Node Cluster",
      status: "RETURNED",
      submittedBy: "Gamma Workspace Node",
      dateSubmitted: "2026-06-26"
    }
  ],

  // ==========================================
  // 5. DETAILED ASSIGNMENTS & DOCUMENTS (For Student Views)
  // ==========================================
  studentAssignments: [
    {
      id: "asm_001",
      title: "System Topology & Core Infrastructure Blueprint",
      dueDate: "2026-07-10",
      status: "COMPLETED",
      score: "9.5/10"
    },
    {
      id: "asm_002",
      title: "Load Test Assertions & Latency Benchmark Metrics",
      dueDate: "2026-07-24",
      status: "IN_PROGRESS",
      score: "N/A"
    }
  ],

  // ==========================================
  // 🔥 6. ADMIN DASHBOARD METRICS & HEALTH (FIX TRANG TRẮNG)
  // ==========================================
  systemHealth: {
    storageUsed: 42,
    storageTotal: 100,
    activeWorkspaces: 14,
    cpuUsage: "28%"
  },

  auditLogs: [
    {
      id: "log_01",
      timestamp: "2026-06-29 09:45:12",
      username: "vana@fpt.edu.vn",
      role: "INSTRUCTOR",
      action: "Created new evidence collection template [col_881]",
      status: "SUCCESS"
    },
    {
      id: "log_02",
      timestamp: "2026-06-29 08:30:00",
      username: "admin.root@fpt.edu.vn",
      role: "ADMIN",
      action: "Modified security perimeter boundary rules",
      status: "SUCCESS"
    },
    {
      id: "log_03",
      timestamp: "2026-06-28 14:15:22",
      username: "student.alpha@institution.edu",
      role: "STUDENT",
      action: "Attempted unauthorized access to admin endpoint",
      status: "FAILURE"
    }
  ]
};