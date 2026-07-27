import { useState, useEffect, useRef } from 'react';
import AnimateIn from '../../components/AnimateIn';

const stepColors = ['#1e3a8a', '#d97706', '#059669', '#7c3aed', '#0284c7'];

const panels = [
  <div className="flex items-center justify-center h-full">
    <div className="w-full max-w-sm bg-white rounded-xl border border-gray-100 shadow-sm p-5">
      <div className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-3">New Project</div>
      <div className="space-y-3">
        <div>
          <div className="text-xs text-gray-400 mb-1">Project Title</div>
          <div className="w-full h-8 bg-gray-50 rounded-lg border border-gray-100 flex items-center px-3 text-sm text-gray-700">Advanced Research 2026</div>
        </div>
        <div>
          <div className="text-xs text-gray-400 mb-1">Description</div>
          <div className="w-full h-16 bg-gray-50 rounded-lg border border-gray-100 flex items-start px-3 pt-2 text-sm text-gray-700">Exploring evidence-based methodologies...</div>
        </div>
        <div className="flex justify-end">
          <div className="w-20 h-7 bg-[#1e3a8a] rounded-lg flex items-center justify-center text-xs text-white font-medium">Create</div>
        </div>
      </div>
    </div>
  </div>,

  <div className="flex items-center justify-center h-full">
    <div className="w-full max-w-sm">
      <div className="flex items-center gap-3 mb-3">
        <div className="flex-1 h-10 bg-white rounded-xl border border-gray-100 shadow-sm flex items-center px-3 gap-2">
          <svg className="w-4 h-4 text-gray-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" d="M12 4v12m-4-4l4 4 4-4M4 16v2a2 2 0 002 2h12a2 2 0 002-2v-2"/></svg>
          <span className="text-xs text-gray-400">Drop files or paste DOI...</span>
        </div>
        <div className="w-10 h-10 bg-indigo-50 rounded-xl flex items-center justify-center">
          <svg className="w-4 h-4 text-[#1e3a8a]" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" d="M12 4v12m-4-4l4 4 4-4"/></svg>
        </div>
      </div>
      <div className="space-y-2">
        <div className="h-10 bg-white rounded-xl border border-gray-100 shadow-sm flex items-center px-3 gap-3">
          <div className="w-5 h-5 bg-amber-100 rounded flex items-center justify-center text-amber-600 text-xs font-bold">PDF</div>
          <span className="text-xs text-gray-600 flex-1">paper_section_analysis.pdf</span>
          <div className="w-16 h-1.5 bg-gray-100 rounded-full overflow-hidden"><div className="w-full h-full bg-emerald-400 rounded-full animate-pulse" /></div>
        </div>
        <div className="h-10 bg-white rounded-xl border border-gray-100 shadow-sm flex items-center px-3 gap-3">
          <div className="w-5 h-5 bg-blue-100 rounded flex items-center justify-center text-blue-600 text-xs font-bold">DOI</div>
          <span className="text-xs text-gray-600 flex-1">10.1234/example.2026.01</span>
          <svg className="w-4 h-4 text-emerald-400" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>
        </div>
      </div>
    </div>
  </div>,

  <div className="flex items-center justify-center h-full">
    <div className="w-full max-w-sm">
      <div className="flex items-center gap-2 mb-3 text-xs text-gray-400">
        <svg className="w-4 h-4 text-emerald-400 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" d="M4 12a8 8 0 1116 0 8 8 0 01-16 0"/></svg>
        AI analyzing document...
      </div>
      <div className="space-y-2">
        <div className="h-12 bg-white rounded-xl border border-gray-100 shadow-sm p-3 flex items-center gap-3 opacity-0 animate-[fadeIn_0.4s_ease_0.1s_forwards]">
          <div className="w-2 h-2 rounded-full bg-[#1e3a8a]" />
          <div>
            <div className="text-xs font-medium text-gray-800">"The system achieves 99.7% uptime..."</div>
            <div className="text-[10px] text-gray-400 mt-0.5">Confidence 94%</div>
          </div>
        </div>
        <div className="h-12 bg-white rounded-xl border border-gray-100 shadow-sm p-3 flex items-center gap-3 opacity-0 animate-[fadeIn_0.4s_ease_0.4s_forwards]">
          <div className="w-2 h-2 rounded-full bg-amber-500" />
          <div>
            <div className="text-xs font-medium text-gray-800">"Evidence-based approach reduces bias..."</div>
            <div className="text-[10px] text-gray-400 mt-0.5">Confidence 87%</div>
          </div>
        </div>
        <div className="h-12 bg-white rounded-xl border border-gray-100 shadow-sm p-3 flex items-center gap-3 opacity-0 animate-[fadeIn_0.4s_ease_0.7s_forwards]">
          <div className="w-2 h-2 rounded-full bg-emerald-500" />
          <div>
            <div className="text-xs font-medium text-gray-800">"Peer review cycle improved by 40%..."</div>
            <div className="text-[10px] text-gray-400 mt-0.5">Confidence 91%</div>
          </div>
        </div>
      </div>
    </div>
  </div>,

  <div className="flex items-center justify-center h-full">
    <div className="w-full max-w-sm">
      <div className="text-xs text-gray-400 mb-3 text-center">Claim — Evidence connections</div>
      <div className="relative flex items-center justify-center">
        <svg viewBox="0 0 240 120" className="w-full max-w-[240px]">
          <circle cx="80" cy="40" r="18" fill="#e0e7ff" stroke="#1e3a8a" strokeWidth="2" />
          <text x="80" y="44" textAnchor="middle" fontSize="7" fill="#1e3a8a" fontWeight="bold">Claim</text>
          <circle cx="160" cy="40" r="18" fill="#fef3c7" stroke="#d97706" strokeWidth="2" />
          <text x="160" y="44" textAnchor="middle" fontSize="7" fill="#d97706" fontWeight="bold">Evid.</text>
          <circle cx="40" cy="90" r="14" fill="#d1fae5" stroke="#059669" strokeWidth="1.5" />
          <text x="40" y="93" textAnchor="middle" fontSize="6" fill="#059669" fontWeight="bold">Src</text>
          <circle cx="120" cy="90" r="14" fill="#dbeafe" stroke="#0284c7" strokeWidth="1.5" />
          <text x="120" y="93" textAnchor="middle" fontSize="6" fill="#0284c7" fontWeight="bold">Src</text>
          <circle cx="200" cy="90" r="14" fill="#f3e8ff" stroke="#7c3aed" strokeWidth="1.5" />
          <text x="200" y="93" textAnchor="middle" fontSize="6" fill="#7c3aed" fontWeight="bold">Src</text>
          <line x1="95" y1="48" x2="145" y2="48" stroke="#d97706" strokeWidth="1.5" strokeDasharray="4 2">
            <animate attributeName="stroke-dashoffset" from="0" to="-24" dur="1s" repeatCount="indefinite" />
          </line>
          <line x1="72" y1="55" x2="48" y2="78" stroke="#059669" strokeWidth="1" strokeDasharray="3 2">
            <animate attributeName="stroke-dashoffset" from="0" to="-20" dur="1.2s" repeatCount="indefinite" />
          </line>
          <line x1="88" y1="57" x2="112" y2="78" stroke="#0284c7" strokeWidth="1" strokeDasharray="3 2">
            <animate attributeName="stroke-dashoffset" from="0" to="-20" dur="0.9s" repeatCount="indefinite" />
          </line>
          <line x1="168" y1="55" x2="192" y2="78" stroke="#7c3aed" strokeWidth="1" strokeDasharray="3 2">
            <animate attributeName="stroke-dashoffset" from="0" to="-20" dur="1.1s" repeatCount="indefinite" />
          </line>
        </svg>
      </div>
      <div className="flex items-center justify-center gap-4 mt-2 text-[10px] text-gray-400">
        <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-amber-400" /> Mapped</span>
        <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-gray-300" /> Pending</span>
      </div>
    </div>
  </div>,

  <div className="flex items-center justify-center h-full">
    <div className="w-full max-w-sm">
      <div className="flex items-center justify-between mb-3">
        <div className="text-xs font-bold text-gray-400">Review Request</div>
        <div className="text-[10px] bg-amber-100 text-amber-700 px-2 py-0.5 rounded-full font-medium">Pending</div>
      </div>
      <div className="bg-white rounded-xl border border-gray-100 shadow-sm p-4">
        <div className="flex items-center gap-3 mb-3">
          <div className="w-7 h-7 bg-indigo-50 rounded-full flex items-center justify-center text-indigo-600 text-xs font-bold">T</div>
          <div>
            <div className="text-xs font-medium text-gray-800">Dr. Tran</div>
            <div className="text-[10px] text-gray-400">Instructor</div>
          </div>
        </div>
        <div className="h-14 bg-gray-50 rounded-lg p-2 text-[10px] text-gray-500 italic leading-relaxed">
          "Good progress. Please strengthen the evidence mapping for claim 3 and add more recent sources."
        </div>
        <div className="flex justify-end mt-3 gap-2">
          <div className="px-3 py-1.5 bg-gray-50 rounded-lg text-[10px] text-gray-500 font-medium">Revise</div>
          <div className="px-3 py-1.5 bg-[#1e3a8a] rounded-lg text-[10px] text-white font-medium">Resubmit</div>
        </div>
      </div>
    </div>
  </div>,
];

export default function PreviewSection({ t }) {
  const [step, setStep] = useState(0);
  const [paused, setPaused] = useState(false);
  const steps = t.preview.steps;

  useEffect(() => {
    if (paused) return;
    const timer = setInterval(() => setStep(s => (s + 1) % 5), 5500);
    return () => clearInterval(timer);
  }, [paused]);

  return (
    <section className="py-20 bg-white border-t border-gray-100">
      <div className="max-w-6xl mx-auto px-6">
        <AnimateIn>
          <h2 className="text-2xl md:text-3xl font-bold text-center text-gray-900 mb-3">{t.preview.heading}</h2>
          <p className="text-gray-500 text-center mb-12 max-w-lg mx-auto">{t.preview.subheading}</p>
        </AnimateIn>
        <AnimateIn delay={150}>
          <div
            className="relative mx-auto max-w-5xl select-none"
            onMouseEnter={() => setPaused(true)}
            onMouseLeave={() => setPaused(false)}
          >
            <div className="absolute inset-0 bg-gradient-to-br from-indigo-500/10 to-blue-500/10 rounded-3xl blur-2xl" />
            <div className="relative bg-white rounded-2xl shadow-xl border border-gray-200 overflow-hidden">
              <div className="flex items-center gap-1.5 px-4 py-3 bg-gray-50 border-b border-gray-100">
                <div className="w-3 h-3 rounded-full bg-rose-400" />
                <div className="w-3 h-3 rounded-full bg-amber-400" />
                <div className="w-3 h-3 rounded-full bg-emerald-400" />
                <div className="ml-4 text-xs text-gray-400 bg-white px-3 py-1 rounded-md border border-gray-100 flex-1 max-w-[220px] flex items-center gap-2">
                  <svg className="w-3 h-3 text-gray-300 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path strokeLinecap="round" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z"/></svg>
                  <span>{t.preview.url}</span>
                </div>
              </div>

              <div className="px-6 pt-5 pb-2">
                <div className="flex items-center gap-2">
                  {steps.map((s, i) => (
                    <div key={i} className="flex-1">
                      <div className={`h-1 rounded-full transition-all duration-500 ${i <= step ? 'bg-[#1e3a8a]' : 'bg-gray-100'}`} />
                    </div>
                  ))}
                </div>
              </div>

              <div className="px-6 py-2 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-lg flex items-center justify-center text-white text-xs font-bold transition-colors duration-500" style={{ backgroundColor: stepColors[step] }}>
                    {step + 1}
                  </div>
                  <div>
                    <div className="text-sm font-bold text-gray-900 transition-colors duration-500" style={{ color: stepColors[step] }}>
                      {steps[step].title}
                    </div>
                    <div className="text-xs text-gray-400">{steps[step].desc}</div>
                  </div>
                </div>
                <div className="hidden sm:flex items-center gap-1.5">
                  <div className={`w-2 h-2 rounded-full ${paused ? 'bg-amber-400' : 'bg-emerald-400'}`} />
                  <span className="text-[10px] text-gray-400">{paused ? 'Paused' : 'Auto-play'}</span>
                </div>
              </div>

              <div className="px-6 pb-4">
                <div className="h-52 md:h-56 bg-gradient-to-br from-gray-50 to-indigo-50/20 rounded-xl border border-gray-100 overflow-hidden flex items-center justify-center">
                  <div key={step} className="w-full animate-[fadeIn_0.45s_ease-out]">
                    {panels[step]}
                  </div>
                </div>
              </div>

              <div className="px-6 pb-6 flex items-center justify-center gap-2">
                {steps.map((s, i) => (
                  <button
                    key={i}
                    onClick={() => { setStep(i); }}
                    className={`transition-all duration-300 rounded-full ${
                      i === step ? 'w-8 h-2.5 bg-[#1e3a8a]' : 'w-2.5 h-2.5 bg-gray-200 hover:bg-gray-300'
                    }`}
                  >
                    <span className="sr-only">{s.title}</span>
                  </button>
                ))}
              </div>
            </div>
          </div>
        </AnimateIn>
      </div>
    </section>
  );
}
