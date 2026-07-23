import AnimateIn from '../../components/AnimateIn';

const featuresList = [
  'structuredData', 'claimTracking', 'feedback', 'aiExtraction', 'vectorSearch', 'realtime'
];

function FeatureCard({ feature, t, index }) {
  const f = t.features[feature];
  const colors = ['from-blue-50 to-indigo-50', 'from-amber-50 to-yellow-50', 'from-emerald-50 to-teal-50', 'from-purple-50 to-pink-50', 'from-cyan-50 to-sky-50', 'from-rose-50 to-orange-50'];
  const dots = ['bg-blue-500', 'bg-amber-500', 'bg-emerald-500', 'bg-purple-500', 'bg-cyan-500', 'bg-rose-500'];
  return (
    <AnimateIn delay={80 * index}>
      <div className={`bg-gradient-to-br ${colors[index % colors.length]} rounded-2xl p-6 border border-gray-100/50 hover:shadow-md transition-all duration-300`}>
        <div className={`w-3 h-3 rounded-full ${dots[index % dots.length]} mb-4`} />
        <h3 className="font-bold text-gray-900 mb-2">{f.title}</h3>
        <p className="text-sm text-gray-600 leading-relaxed">{f.desc}</p>
      </div>
    </AnimateIn>
  );
}

export default function FeaturesSection({ t }) {
  return (
    <section id="features" className="py-20 bg-white">
      <div className="max-w-6xl mx-auto px-6">
        <AnimateIn>
          <h2 className="text-2xl md:text-3xl font-bold text-center text-gray-900 mb-3">{t.features.heading}</h2>
          <p className="text-gray-500 text-center mb-12 max-w-xl mx-auto">{t.features.subheading}</p>
        </AnimateIn>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {featuresList.map((f, i) => <FeatureCard key={f} feature={f} t={t} index={i} />)}
        </div>
      </div>
    </section>
  );
}
