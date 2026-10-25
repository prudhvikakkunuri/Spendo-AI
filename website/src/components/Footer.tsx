import { Download, ArrowUpRight } from 'lucide-react';

export const Footer: React.FC = () => {
  return (
    <footer className="border-t border-[#27272E] light:border-[#E4E4E7] bg-[#09090B] light:bg-[#FAFAFA] pt-16 pb-12">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Top Ticker Motto */}
        <div className="mb-14 pb-8 border-b border-[#1E1E24] light:border-[#E4E4E7] overflow-hidden whitespace-nowrap">
          <div className="text-4xl sm:text-6xl font-mono-tech font-extrabold text-[#18181C] light:text-[#E4E4E7] tracking-tight uppercase select-none">
            YOUR MONEY. UNDERSTOOD. • 100% ON-DEVICE • NO TRACKING • LOCAL FIRST • 
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-4 gap-10 mb-14">
          
          {/* Brand Col */}
          <div className="space-y-4 md:col-span-1">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-xl bg-[#18181C] light:bg-[#FFFFFF] border border-[#27272E] light:border-[#E4E4E7] flex items-center justify-center p-1">
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
              <span className="font-mono-tech text-base font-bold text-[#F4F4F5] light:text-[#09090B]">
                SPENDO<span className="text-[#D71920]">.</span>
              </span>
            </div>
            <p className="text-xs text-[#A1A1AA] light:text-[#71717A] leading-relaxed">
              A private, Android-first personal finance app and AI money agent. Designed with Nothing OS-inspired minimalism.
            </p>
            <div className="inline-flex items-center gap-2 pt-2 text-xs font-mono-tech text-[#22C55E]">
              <span className="w-2 h-2 rounded-full bg-[#22C55E] animate-pulse" />
              <span>Actively Maintained</span>
            </div>
          </div>

          {/* Navigation Links */}
          <div>
            <h4 className="text-xs font-mono-tech font-bold uppercase tracking-wider text-[#F4F4F5] light:text-[#09090B] mb-4">
              Navigation
            </h4>
            <ul className="space-y-2.5 text-xs font-mono-tech text-[#A1A1AA] light:text-[#71717A]">
              <li><a href="#features" className="hover:text-white transition-colors">Features</a></li>
              <li><a href="#how-it-works" className="hover:text-white transition-colors">How It Works</a></li>
              <li><a href="#widgets" className="hover:text-white transition-colors">Home-Screen Widgets</a></li>
              <li><a href="#privacy" className="hover:text-white transition-colors">Privacy Architecture</a></li>
              <li><a href="#roadmap" className="hover:text-white transition-colors">Product Roadmap</a></li>
              <li><a href="#faq" className="hover:text-white transition-colors">Frequently Asked Questions</a></li>
            </ul>
          </div>

          {/* Development & GitHub */}
          <div>
            <h4 className="text-xs font-mono-tech font-bold uppercase tracking-wider text-[#F4F4F5] light:text-[#09090B] mb-4">
              Open Source
            </h4>
            <ul className="space-y-2.5 text-xs font-mono-tech text-[#A1A1AA] light:text-[#71717A]">
              <li>
                <a
                  href="https://github.com/prudhvikakkunuri/Spendo-AI"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-white transition-colors inline-flex items-center gap-1"
                >
                  GitHub Repository <ArrowUpRight className="w-3 h-3" />
                </a>
              </li>
              <li>
                <a
                  href="https://github.com/prudhvikakkunuri/Spendo-AI/releases"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-white transition-colors inline-flex items-center gap-1"
                >
                  Release History <ArrowUpRight className="w-3 h-3" />
                </a>
              </li>
              <li>
                <a
                  href="https://github.com/prudhvikakkunuri/Spendo-AI/blob/main/CONTRIBUTING.md"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-white transition-colors inline-flex items-center gap-1"
                >
                  Contributing <ArrowUpRight className="w-3 h-3" />
                </a>
              </li>
              <li>
                <a
                  href="https://github.com/prudhvikakkunuri/Spendo-AI/issues"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-white transition-colors inline-flex items-center gap-1"
                >
                  Issue Tracker <ArrowUpRight className="w-3 h-3" />
                </a>
              </li>
            </ul>
          </div>

          {/* Privacy & Download */}
          <div>
            <h4 className="text-xs font-mono-tech font-bold uppercase tracking-wider text-[#F4F4F5] light:text-[#09090B] mb-4">
              Get Started
            </h4>
            <p className="text-xs text-[#A1A1AA] light:text-[#71717A] mb-4 leading-relaxed">
              No account required. Install the APK and start tracking immediately.
            </p>
            <a
              href="https://github.com/prudhvikakkunuri/Spendo-AI/releases/download/dev-latest/spendo-ai.apk"
              className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-mono-tech font-bold bg-[#D71920] hover:bg-[#B8141A] text-white transition-all shadow-md shadow-[#D71920]/20"
            >
              <Download className="w-3.5 h-3.5" />
              <span>Download spendo-ai.apk</span>
            </a>
          </div>

        </div>

        {/* Bottom Bar */}
        <div className="pt-8 border-t border-[#1E1E24] light:border-[#E4E4E7] flex flex-col sm:flex-row items-center justify-between gap-4 text-xs font-mono-tech text-[#71717A]">
          <div>
            © 2026 Spendo · Engineered with Kotlin &amp; Jetpack Compose by Prudhvi Kakkunuri.
          </div>
          <div className="flex items-center gap-4">
            <a
              href="https://github.com/prudhvikakkunuri/Spendo-AI/blob/main/SECURITY.md"
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-white transition-colors"
            >
              Security Policy
            </a>
            <span>·</span>
            <span>100% On-Device Room SQLite</span>
          </div>
        </div>

      </div>
    </footer>
  );
};
