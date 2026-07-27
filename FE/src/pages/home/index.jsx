import { useState, useCallback } from 'react';
import { useLanguage } from '../../context/LanguageContext';
import { homeText } from '../../locales/home';
import LoadingScreen from '../../components/LoadingScreen';
import Navbar from './Navbar';
import HeroSection from './HeroSection';
import RolesSection from './RolesSection';
import WorkflowSection from './WorkflowSection';
import FeaturesSection from './FeaturesSection';
import StatsSection from './StatsSection';
import TestimonialsSection from './TestimonialsSection';
import PreviewSection from './PreviewSection';
import CtaSection from './CtaSection';
import FooterSection from './FooterSection';

export default function Home() {
  const { language } = useLanguage();
  const t = homeText[language];
  const [showSplash, setShowSplash] = useState(() => !sessionStorage.getItem('splashSeen'));

  const handleSplashFinish = useCallback(() => {
    sessionStorage.setItem('splashSeen', '1');
    setShowSplash(false);
  }, []);

  if (showSplash) {
    return <LoadingScreen onFinish={handleSplashFinish} />;
  }

  return (
    <div className="min-h-screen bg-[#fcfcfc] text-[#333] font-sans">
      <Navbar t={t} />
      <HeroSection t={t} />
      <StatsSection t={t} />
      <RolesSection t={t} />
      <WorkflowSection t={t} />
      <FeaturesSection t={t} />
      <TestimonialsSection t={t} />
      <PreviewSection t={t} />
      <CtaSection t={t} />
      <FooterSection t={t} />
    </div>
  );
}
