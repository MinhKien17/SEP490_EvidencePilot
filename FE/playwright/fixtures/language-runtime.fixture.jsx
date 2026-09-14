import React from 'react';
import { createRoot } from 'react-dom/client';
import { LanguageProvider, useLanguage } from '../../src/context/LanguageContext.jsx';
import i18n from '../../src/i18n.js';
import { homeText } from '../../src/locales/home.js';

function LanguageProbe() {
  const { language, setLanguage, toggleLanguage } = useLanguage();

  window.changeI18nextLanguage = (next) => i18n.changeLanguage(next);

  return (
    <main>
      <output data-testid="context-language">{language}</output>
      <output data-testid="i18next-language">{i18n.resolvedLanguage || i18n.language}</output>
      <output data-testid="legacy-copy">{homeText[language]?.nav.home || 'missing'}</output>
      <button type="button" onClick={() => setLanguage('vi')}>Set Vietnamese</button>
      <button type="button" onClick={toggleLanguage}>Toggle language</button>
    </main>
  );
}

createRoot(document.getElementById('root')).render(
  <LanguageProvider>
    <LanguageProbe />
  </LanguageProvider>,
);
