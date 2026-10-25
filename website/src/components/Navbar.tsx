import { useState, useEffect } from 'react';
import { Download, Menu, X, Sun, Moon } from 'lucide-react';
import { GithubIcon } from './Icons';

interface NavbarProps {
  theme: 'dark' | 'light';
  onToggleTheme: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ theme, onToggleTheme }) => {
  const [isScrolled, setIsScrolled] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const navLinks = [
    { label: 'Features', href: '#features' },
    { label: 'How It Works', href: '#how-it-works' },
    { label: 'Widgets', href: '#widgets' },
    { label: 'Privacy', href: '#privacy' },
    { label: 'Roadmap', href: '#roadmap' },
    { label: 'FAQ', href: '#faq' },
  ];

  return (
    <nav
      className={`fixed top-0 inset-x-0 z-50 transition-all duration-300 ${
        isScrolled
          ? 'bg-[#09090B]/90 light:bg-[#FAFAFA]/90 backdrop-blur-md border-b border-[#27272E] light:border-[#E4E4E7] py-3'
          : 'bg-transparent py-5'
      }`}
      aria-label="Main Navigation"
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between">
          {/* Logo */}
          <a
            href="#"
            className="flex items-center gap-3 group focus:outline-none focus-visible:ring-2 focus-visible:ring-[#D71920]"
          >
            <div className="w-9 h-9 rounded-xl bg-[#18181C] light:bg-[#FFFFFF] border border-[#27272E] light:border-[#E4E4E7] flex items-center justify-center p-1.5 transition-transform group-hover:scale-105">
              <svg viewBox="0 0 48 48" className="w-full h-full" fill="none">
                <circle cx="16" cy="14" r="3.5" fill="currentColor" className="text-[#F4F4F5] light:text-[#09090B]" />
                <circle cx="24" cy="14" r="3.5" fill="currentColor" className="text-[#F4F4F5] light:text-[#09090B]" />
                <circle cx="32" cy="14" r="3.5" fill="currentColor" className="text-[#F4F4F5] light:text-[#09090B]" />
                <circle cx="16" cy="22" r="3.5" fill="currentColor" className="text-[#F4F4F5] light:text-[#09090B]" />
                <circle cx="24" cy="24" r="3.5" fill="#D71920" />
                <circle cx="32" cy="26" r="3.5" fill="currentColor" className="text-[#F4F4F5] light:text-[#09090B]" />
                <circle cx="16" cy="34" r="3.5" fill="currentColor" className="text-[#F4F4F5] light:text-[#09090B]" />
                <circle cx="24" cy="34" r="3.5" fill="currentColor" className="text-[#F4F4F5] light:text-[#09090B]" />
                <circle cx="32" cy="34" r="3.5" fill="currentColor" className="text-[#F4F4F5] light:text-[#09090B]" />
              </svg>
            </div>
            <div>
              <span className="font-mono-tech text-base font-bold tracking-tight text-[#F4F4F5] light:text-[#09090B]">
                SPENDO<span className="text-[#D71920]">.</span>
              </span>
              <span className="hidden sm:inline-block ml-2 text-[10px] font-mono-tech px-1.5 py-0.5 rounded border border-[#27272E] light:border-[#E4E4E7] text-[#A1A1AA] light:text-[#71717A]">
                AI MONEY AGENT
              </span>
            </div>
          </a>

          {/* Desktop Navigation Links */}
          <div className="hidden md:flex items-center gap-7">
            {navLinks.map((link) => (
              <a
                key={link.label}
                href={link.href}
                className="text-sm font-medium text-[#A1A1AA] hover:text-[#F4F4F5] light:text-[#71717A] light:hover:text-[#09090B] transition-colors"
              >
                {link.label}
              </a>
            ))}
          </div>

          {/* Action Buttons */}
          <div className="hidden sm:flex items-center gap-3">
            {/* Theme Toggle */}
            <button
              onClick={onToggleTheme}
              aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
              className="p-2 rounded-lg border border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF] text-[#A1A1AA] hover:text-[#F4F4F5] light:text-[#71717A] light:hover:text-[#09090B] transition-colors"
            >
              {theme === 'dark' ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
            </button>

            {/* GitHub Repo */}
            <a
              href="https://github.com/prudhvikakkunuri/Spendo-AI"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 px-3 py-2 text-xs font-mono-tech font-semibold rounded-lg border border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF] text-[#F4F4F5] light:text-[#09090B] hover:border-[#3F3F4A] transition-colors"
            >
              <GithubIcon className="w-3.5 h-3.5" />
              <span>GitHub</span>
            </a>

            {/* Download CTA */}
            <a
              href="https://github.com/prudhvikakkunuri/Spendo-AI/releases/download/dev-latest/spendo-ai.apk"
              className="inline-flex items-center gap-2 px-4 py-2 text-xs font-mono-tech font-bold rounded-lg bg-[#D71920] hover:bg-[#B8141A] text-white shadow-sm transition-all active:scale-[0.98]"
            >
              <Download className="w-3.5 h-3.5" />
              <span>Get APK</span>
            </a>
          </div>

          {/* Mobile Menu Button */}
          <div className="flex sm:hidden items-center gap-2">
            <button
              onClick={onToggleTheme}
              aria-label="Toggle theme"
              className="p-2 rounded-lg border border-[#27272E] text-[#A1A1AA]"
            >
              {theme === 'dark' ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
            </button>
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-label="Toggle Menu"
              className="p-2 rounded-lg border border-[#27272E] text-[#F4F4F5]"
            >
              {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Drawer */}
      {mobileMenuOpen && (
        <div className="sm:hidden border-b border-[#27272E] bg-[#09090B] light:bg-[#FAFAFA] px-4 pt-3 pb-6 space-y-3">
          {navLinks.map((link) => (
            <a
              key={link.label}
              href={link.href}
              onClick={() => setMobileMenuOpen(false)}
              className="block px-3 py-2 text-base font-medium text-[#A1A1AA] hover:text-[#F4F4F5] light:text-[#71717A] light:hover:text-[#09090B]"
            >
              {link.label}
            </a>
          ))}
          <div className="pt-3 border-t border-[#27272E] flex flex-col gap-2">
            <a
              href="https://github.com/prudhvikakkunuri/Spendo-AI"
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center justify-center gap-2 w-full py-2.5 text-sm font-mono-tech rounded-lg border border-[#27272E] text-[#F4F4F5] light:text-[#09090B]"
            >
              <GithubIcon className="w-4 h-4" />
              <span>View Source on GitHub</span>
            </a>
            <a
              href="https://github.com/prudhvikakkunuri/Spendo-AI/releases/download/dev-latest/spendo-ai.apk"
              className="flex items-center justify-center gap-2 w-full py-2.5 text-sm font-mono-tech font-bold rounded-lg bg-[#D71920] text-white"
            >
              <Download className="w-4 h-4" />
              <span>Download Android APK</span>
            </a>
          </div>
        </div>
      )}
    </nav>
  );
};
