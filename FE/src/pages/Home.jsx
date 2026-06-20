import { Link } from 'react-router-dom';

export default function Home() {
  return (
    <div className="min-h-screen bg-[#fcfcfc] text-[#333333] font-sans flex flex-col">
      {/* 1. Header / Navigation Bar */}
      <header className="bg-[#1e3a8a] text-white px-8 py-3 flex justify-between items-center shadow-sm">
        <div className="flex items-center space-x-3">
          <span className="text-xl font-bold tracking-wider">Evidence Pilot 🚀</span>
        </div>
        <nav className="space-x-6 flex items-center">
          <a href="#features" className="text-blue-200 hover:text-white font-medium text-sm transition">Features</a>
          <a href="#about" className="text-blue-200 hover:text-white font-medium text-sm transition">About Us</a>
          <Link to="/login" className="bg-[#2563eb] hover:bg-[#1d4ed8] text-white px-4 py-1.5 rounded text-sm font-medium transition shadow-sm">
            Log In
          </Link>
          <Link to="/register" className="bg-transparent hover:bg-white/10 text-white border border-white/50 px-4 py-1.5 rounded text-sm font-medium transition">
            Register
          </Link>
        </nav>
      </header>

      {/* 2. Hero Section (Clean & Centered Layout without JSON block) */}
      <main className="max-w-4xl w-full mx-auto px-8 py-24 text-center flex flex-col justify-center items-center flex-1">
        <div className="space-y-6">
          <h1 className="text-4xl md:text-6xl font-normal text-[#1e3a8a] leading-tight">
            The easy to use, <br />
            <span className="font-bold">Collaborative Research</span> Platform
          </h1>
          <p className="text-lg text-gray-600 leading-relaxed max-w-2xl mx-auto">
            Evidence Pilot is a comprehensive platform for scientific research management, evaluating projects, sources, and claims tailored for both students and instructors.
          </p>
          <div className="pt-4">
            <Link to="/register" className="bg-[#1e3a8a] hover:bg-[#1e40af] text-white px-8 py-3.5 rounded text-base font-semibold transition shadow-md inline-block">
              Create a New Account
            </Link>
          </div>
        </div>
      </main>

      {/* 3. Features Grid Section */}
      <section id="features" className="bg-[#f0f4f8] py-16 border-t border-gray-200">
        <div className="max-w-6xl w-full mx-auto px-8">
          <h2 className="text-2xl font-bold text-center text-[#1e3a8a] mb-12">Why use Evidence Pilot?</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div className="bg-white p-6 rounded border border-gray-200 shadow-sm text-left">
              <div className="text-xl font-semibold text-[#1e3a8a] mb-2">Structured Dataset</div>
              <p className="text-sm text-gray-600 leading-relaxed">
                Easily create and structure your datasets with verified external sources, links, and cross-references.
              </p>
            </div>
            <div className="bg-white p-6 rounded border border-gray-200 shadow-sm text-left">
              <div className="text-xl font-semibold text-[#1e3a8a] mb-2">Claim Tracking</div>
              <p className="text-sm text-gray-600 leading-relaxed">
                Keep track of student project claims, source verifications, and structural research graphs effortlessly.
              </p>
            </div>
            <div className="bg-white p-6 rounded border border-gray-200 shadow-sm text-left">
              <div className="text-xl font-semibold text-[#1e3a8a] mb-2">Direct Feedback</div>
              <p className="text-sm text-gray-600 leading-relaxed">
                Seamless collaboration through interactive review requests, comments, and direct feedback from instructors.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* 4. Footer */}
      <footer className="bg-[#0f172a] text-gray-300 py-6 text-center text-xs border-t border-gray-900 mt-auto">
        © 2026 Evidence Pilot Project. 
      </footer>
    </div>
  );
}
