import { useState, useEffect } from 'react';
import { Navbar } from './components/Navbar';
import { Hero } from './components/Hero';
import { ProductIntro } from './components/ProductIntro';
import { FeatureShowcase } from './components/FeatureShowcase';
import { InteractiveDemo } from './components/InteractiveDemo';
import { HowItWorks } from './components/HowItWorks';
import { WidgetShowcase } from './components/WidgetShowcase';
import { PrivacySecurity } from './components/PrivacySecurity';
import { Roadmap } from './components/Roadmap';
import { DevTransparency } from './components/DevTransparency';
import { DownloadSection } from './components/DownloadSection';
import { FaqSection } from './components/FaqSection';
import { Footer } from './components/Footer';

export function App() {
  const [theme, setTheme] = useState<'dark' | 'light'>('dark');

  useEffect(() => {
    const savedTheme = (localStorage.getItem('spendo-theme') as 'dark' | 'light') || 'dark';
    setTheme(savedTheme);
    if (savedTheme === 'light') {
      document.documentElement.classList.remove('dark');
      document.documentElement.classList.add('light');
    } else {
      document.documentElement.classList.remove('light');
      document.documentElement.classList.add('dark');
    }
  }, []);

  const toggleTheme = () => {
    const newTheme = theme === 'dark' ? 'light' : 'dark';
    setTheme(newTheme);
    localStorage.setItem('spendo-theme', newTheme);
    if (newTheme === 'light') {
      document.documentElement.classList.remove('dark');
      document.documentElement.classList.add('light');
    } else {
      document.documentElement.classList.remove('light');
      document.documentElement.classList.add('dark');
    }
  };

  return (
    <div className="min-h-screen bg-[#09090B] light:bg-[#FAFAFA] text-[#F4F4F5] light:text-[#09090B] selection:bg-[#D71920] selection:text-white transition-colors duration-200">
      {/* Top Navigation */}
      <Navbar theme={theme} onToggleTheme={toggleTheme} />

      {/* Main Content */}
      <main>
        {/* Section 1: Hero */}
        <Hero />

        {/* Section 2: Product Intro & Philosophy */}
        <ProductIntro />

        {/* Section 3: Engineered Features Bento */}
        <FeatureShowcase />

        {/* Section 4: Interactive Expense Simulator */}
        <InteractiveDemo />

        {/* Section 5: How It Works */}
        <HowItWorks />

        {/* Section 6: Android Home-Screen Widgets */}
        <WidgetShowcase />

        {/* Section 7: Verified Privacy & Security */}
        <PrivacySecurity />

        {/* Section 8: Truthful Roadmap */}
        <Roadmap />

        {/* Section 9: Engineering Transparency */}
        <DevTransparency />

        {/* Section 10: Official Android Download */}
        <DownloadSection />

        {/* Section 11: Comprehensive FAQ */}
        <FaqSection />
      </main>

      {/* Section 12: Footer */}
      <Footer />
    </div>
  );
}

export default App;
