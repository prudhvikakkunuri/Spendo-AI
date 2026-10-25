import { Bug, GitPullRequest, ArrowUpRight, Terminal } from 'lucide-react';
import { GithubIcon } from './Icons';

export const DevTransparency: React.FC = () => {
  const metrics = [
    { label: "AUTOMATED TESTS", value: "140 Passing", detail: "Financial audit & parsing suites" },
    { label: "TARGET RUNTIME", value: "Android 8.0+", detail: "API 26 up to Android 14 (API 34)" },
    { label: "UI TOOLKIT", value: "Jetpack Compose", detail: "Material 3 · 100% Kotlin" },
    { label: "LOCAL STORAGE", value: "Room SQLite", detail: "Paise integer precision (Long)" },
  ];

  return (
    <section className="py-24 border-t border-[#27272E] light:border-[#E4E4E7] bg-[#09090B] light:bg-[#FAFAFA]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Header */}
        <div className="max-w-3xl mb-16 space-y-4">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF]">
            <span className="dot-indicator-red" />
            <span className="font-mono-tech text-xs tracking-wider uppercase text-[#A1A1AA] light:text-[#71717A]">
              ENGINEERING INTEGRITY
            </span>
          </div>
          <h2 className="text-3xl sm:text-5xl font-bold tracking-tight text-[#F4F4F5] light:text-[#09090B]">
            Development transparency.
          </h2>
          <p className="text-base sm:text-lg text-[#A1A1AA] light:text-[#52525B]">
            Spendo is crafted with careful attention to Android platform best practices. No fabricated metrics, no private tracking scripts.
          </p>
        </div>

        {/* Metrics Grid */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-6 mb-12">
          {metrics.map((m, idx) => (
            <div
              key={idx}
              className="p-6 rounded-2xl border border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF] space-y-2"
            >
              <span className="text-[10px] font-mono-tech text-[#71717A] uppercase tracking-wider block">
                {m.label}
              </span>
              <div className="text-2xl font-mono-tech font-bold text-white light:text-black">
                {m.value}
              </div>
              <p className="text-xs text-[#A1A1AA] light:text-[#71717A]">
                {m.detail}
              </p>
            </div>
          ))}
        </div>

        {/* GitHub Deep Links Bento */}
        <div className="p-8 rounded-3xl border border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF] space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-[#27272E] light:border-[#E4E4E7] pb-6">
            <div className="flex items-center gap-3">
              <GithubIcon className="w-6 h-6 text-[#F4F4F5] light:text-[#09090B]" />
              <div>
                <h3 className="text-lg font-bold text-[#F4F4F5] light:text-[#09090B]">
                  prudhvikakkunuri / Spendo-AI
                </h3>
                <p className="text-xs font-mono-tech text-[#71717A]">
                  Public Android repository on GitHub
                </p>
              </div>
            </div>
            <a
              href="https://github.com/prudhvikakkunuri/Spendo-AI"
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-mono-tech font-bold bg-[#F4F4F5] light:bg-[#09090B] text-black light:text-white hover:opacity-90 transition-opacity"
            >
              <span>Explore Codebase</span>
              <ArrowUpRight className="w-3.5 h-3.5" />
            </a>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs font-mono-tech">
            <a
              href="https://github.com/prudhvikakkunuri/Spendo-AI/issues/new?template=bug_report.yml"
              target="_blank"
              rel="noopener noreferrer"
              className="p-4 rounded-xl border border-[#27272E] light:border-[#E4E4E7] hover:border-[#D71920] transition-colors flex items-center justify-between group"
            >
              <div className="flex items-center gap-2.5">
                <Bug className="w-4 h-4 text-[#D71920]" />
                <span className="text-[#F4F4F5] light:text-[#09090B]">Report a Bug</span>
              </div>
              <ArrowUpRight className="w-3.5 h-3.5 text-[#71717A] group-hover:text-white" />
            </a>

            <a
              href="https://github.com/prudhvikakkunuri/Spendo-AI/actions/workflows/android-ci.yml"
              target="_blank"
              rel="noopener noreferrer"
              className="p-4 rounded-xl border border-[#27272E] light:border-[#E4E4E7] hover:border-[#22C55E] transition-colors flex items-center justify-between group"
            >
              <div className="flex items-center gap-2.5">
                <Terminal className="w-4 h-4 text-[#22C55E]" />
                <span className="text-[#F4F4F5] light:text-[#09090B]">Android CI Workflows</span>
              </div>
              <ArrowUpRight className="w-3.5 h-3.5 text-[#71717A] group-hover:text-white" />
            </a>

            <a
              href="https://github.com/prudhvikakkunuri/Spendo-AI/blob/main/CONTRIBUTING.md"
              target="_blank"
              rel="noopener noreferrer"
              className="p-4 rounded-xl border border-[#27272E] light:border-[#E4E4E7] hover:border-[#3F3F4A] transition-colors flex items-center justify-between group"
            >
              <div className="flex items-center gap-2.5">
                <GitPullRequest className="w-4 h-4 text-[#3B82F6]" />
                <span className="text-[#F4F4F5] light:text-[#09090B]">Contributing Guide</span>
              </div>
              <ArrowUpRight className="w-3.5 h-3.5 text-[#71717A] group-hover:text-white" />
            </a>
          </div>

        </div>

      </div>
    </section>
  );
};
