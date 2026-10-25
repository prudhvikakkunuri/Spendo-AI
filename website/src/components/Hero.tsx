import { useState } from 'react';
import { Download, ArrowUpRight, CheckCircle2, Sparkles } from 'lucide-react';
import { GithubIcon } from './Icons';

export const Hero: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'transactions' | 'ai'>('transactions');

  return (
    <section className="relative min-h-[92vh] flex flex-col justify-center pt-32 pb-20 overflow-hidden bg-dot-grid">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 w-full">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">
          
          {/* Left Column: Product Story & CTAs */}
          <div className="lg:col-span-7 space-y-8 text-center lg:text-left">
            {/* Status Badge */}
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full border border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF] shadow-sm">
              <span className="dot-indicator-red animate-pulse" />
              <span className="font-mono-tech text-xs tracking-wider uppercase text-[#A1A1AA] light:text-[#71717A]">
                SPENDO V0.1.0 // ON-DEVICE AI MONEY AGENT
              </span>
            </div>

            {/* Editorial Headline */}
            <h1 className="text-4xl sm:text-6xl lg:text-7xl font-bold tracking-tight text-[#F4F4F5] light:text-[#09090B] leading-[1.05]">
              Your money.<br />
              <span className="text-[#A1A1AA] light:text-[#71717A] font-normal">
                Understood.
              </span>
            </h1>

            {/* Subtitle */}
            <p className="text-lg sm:text-xl text-[#A1A1AA] light:text-[#52525B] max-w-2xl mx-auto lg:mx-0 leading-relaxed">
              Track expenses in seconds. Notice bank UPI &amp; card spends from notifications right on your phone.
              100% private Room SQLite storage, voice expense logging, and deterministic offline AI.
            </p>

            {/* CTAs */}
            <div className="flex flex-col sm:flex-row items-center justify-center lg:justify-start gap-4 pt-2">
              <a
                href="https://github.com/prudhvikakkunuri/Spendo-AI/releases/download/dev-latest/spendo-ai.apk"
                className="w-full sm:w-auto inline-flex items-center justify-center gap-3 px-8 py-4 rounded-xl font-mono-tech font-bold text-sm bg-[#D71920] hover:bg-[#B8141A] text-white shadow-lg shadow-[#D71920]/20 transition-all active:scale-[0.98]"
              >
                <Download className="w-4 h-4" />
                <span>Download Android APK</span>
              </a>

              <a
                href="https://github.com/prudhvikakkunuri/Spendo-AI"
                target="_blank"
                rel="noopener noreferrer"
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2.5 px-6 py-4 rounded-xl font-mono-tech text-sm font-semibold border border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF] text-[#F4F4F5] light:text-[#09090B] hover:border-[#3F3F4A] transition-all"
              >
                <GithubIcon className="w-4 h-4" />
                <span>Source Code</span>
                <ArrowUpRight className="w-3.5 h-3.5 text-[#71717A]" />
              </a>
            </div>

            {/* Trust Points */}
            <div className="pt-4 flex flex-wrap items-center justify-center lg:justify-start gap-y-3 gap-x-6 text-xs text-[#A1A1AA] light:text-[#71717A] font-mono-tech">
              <div className="flex items-center gap-1.5">
                <CheckCircle2 className="w-3.5 h-3.5 text-[#22C55E]" />
                <span>Zero Cloud Telemetry</span>
              </div>
              <div className="flex items-center gap-1.5">
                <CheckCircle2 className="w-3.5 h-3.5 text-[#22C55E]" />
                <span>No SMS Permissions</span>
              </div>
              <div className="flex items-center gap-1.5">
                <CheckCircle2 className="w-3.5 h-3.5 text-[#22C55E]" />
                <span>Android 8.0+ Native</span>
              </div>
            </div>
          </div>

          {/* Right Column: High-Fidelity Nothing OS Phone Mockup */}
          <div className="lg:col-span-5 flex justify-center">
            <div className="relative w-full max-w-[340px]">
              {/* Outer Phone Shell */}
              <div className="relative rounded-[42px] border-4 border-[#27272E] light:border-[#D4D4D8] bg-[#09090B] light:bg-[#121214] p-3 shadow-2xl shadow-black/60">
                {/* Punch-hole camera */}
                <div className="absolute top-5 left-1/2 -translate-x-1/2 w-4 h-4 bg-black rounded-full z-20 border border-[#27272E]" />

                {/* Inner Screen */}
                <div className="rounded-[32px] bg-[#09090B] border border-[#18181C] p-4 text-[#F4F4F5] overflow-hidden select-none">
                  {/* Android Status Bar */}
                  <div className="flex items-center justify-between text-[11px] font-mono-tech text-[#71717A] pt-1 pb-3 px-1">
                    <span>10:42</span>
                    <div className="flex items-center gap-1.5">
                      <span>5G</span>
                      <span className="w-2.5 h-2 rounded-sm border border-[#71717A] inline-block" />
                    </div>
                  </div>

                  {/* App Header */}
                  <div className="flex items-center justify-between pb-3 border-b border-[#27272E]">
                    <div>
                      <div className="text-[10px] font-mono-tech text-[#71717A] uppercase tracking-wider">
                        SPENDO LEDGER // OCT
                      </div>
                      <div className="text-base font-bold tracking-tight">Main Dashboard</div>
                    </div>
                    <div className="w-2.5 h-2.5 rounded-full bg-[#D71920] shadow-[0_0_8px_#D71920]" />
                  </div>

                  {/* Monthly Outflow Card */}
                  <div className="mt-4 p-3.5 rounded-2xl bg-[#121215] border border-[#27272E]">
                    <div className="flex justify-between items-center text-[10px] font-mono-tech text-[#A1A1AA]">
                      <span>MONTHLY OUTFLOW</span>
                      <span className="text-[#22C55E]">SAFE-TO-SPEND</span>
                    </div>
                    <div className="mt-1 text-2xl font-mono-tech font-bold text-white tracking-tight">
                      ₹24,850<span className="text-sm text-[#71717A]">.00</span>
                    </div>

                    {/* Budget Progress Bar */}
                    <div className="mt-2.5">
                      <div className="flex justify-between text-[10px] font-mono-tech text-[#71717A] mb-1">
                        <span>Limit: ₹45,000</span>
                        <span>55% Used</span>
                      </div>
                      <div className="h-1.5 w-full bg-[#18181C] rounded-full overflow-hidden">
                        <div className="h-full bg-[#F4F4F5] rounded-full w-[55%]" />
                      </div>
                    </div>
                  </div>

                  {/* Quick Action Selector */}
                  <div className="grid grid-cols-2 gap-2 mt-3">
                    <button
                      onClick={() => setActiveTab('transactions')}
                      className={`py-1.5 px-2 rounded-xl text-xs font-mono-tech transition-colors ${
                        activeTab === 'transactions'
                          ? 'bg-[#18181C] text-white border border-[#3F3F4A]'
                          : 'text-[#71717A] hover:text-white'
                      }`}
                    >
                      Recent Spends
                    </button>
                    <button
                      onClick={() => setActiveTab('ai')}
                      className={`py-1.5 px-2 rounded-xl text-xs font-mono-tech flex items-center justify-center gap-1 transition-colors ${
                        activeTab === 'ai'
                          ? 'bg-[#18181C] text-[#D71920] border border-[#3F3F4A]'
                          : 'text-[#71717A] hover:text-white'
                      }`}
                    >
                      <Sparkles className="w-3 h-3" />
                      <span>AI Insights</span>
                    </button>
                  </div>

                  {/* Screen Content Feed */}
                  {activeTab === 'transactions' ? (
                    <div className="mt-3 space-y-2">
                      {/* Item 1 */}
                      <div className="p-2.5 rounded-xl bg-[#121215] border border-[#1E1E24] flex items-center justify-between">
                        <div>
                          <div className="text-xs font-bold text-white">Swiggy</div>
                          <div className="text-[10px] text-[#71717A]">Food &amp; Dining · HDFC Bank</div>
                        </div>
                        <div className="text-xs font-mono-tech font-bold text-[#F4F4F5]">
                          -₹349.00
                        </div>
                      </div>

                      {/* Item 2 */}
                      <div className="p-2.5 rounded-xl bg-[#121215] border border-[#1E1E24] flex items-center justify-between">
                        <div>
                          <div className="text-xs font-bold text-white">Shell Station</div>
                          <div className="text-[10px] text-[#71717A]">Fuel &amp; Travel · Kotak</div>
                        </div>
                        <div className="text-xs font-mono-tech font-bold text-[#F4F4F5]">
                          -₹1,200.00
                        </div>
                      </div>

                      {/* Item 3 (Salary) */}
                      <div className="p-2.5 rounded-xl bg-[#121215] border border-[#1E1E24] flex items-center justify-between">
                        <div>
                          <div className="text-xs font-bold text-white">Client Salary</div>
                          <div className="text-[10px] text-[#71717A]">Income · ICICI Bank</div>
                        </div>
                        <div className="text-xs font-mono-tech font-bold text-[#22C55E]">
                          +₹85,000.00
                        </div>
                      </div>
                    </div>
                  ) : (
                    <div className="mt-3 p-3 rounded-xl bg-[#121215] border border-[#1E1E24] space-y-2 text-xs">
                      <div className="flex items-center gap-1.5 text-[10px] font-mono-tech text-[#D71920]">
                        <Sparkles className="w-3 h-3" />
                        <span>LOCAL FINANCIAL SUMMARY</span>
                      </div>
                      <p className="text-[#A1A1AA] text-[11px] leading-relaxed">
                        "Your top outlay category this month is <strong className="text-white">Food &amp; Dining (₹8,420)</strong>. You have ₹20,150 remaining in your monthly envelope."
                      </p>
                      <div className="text-[10px] font-mono-tech text-[#71717A]">
                        Calculated locally in 4ms via Room SQLite.
                      </div>
                    </div>
                  )}

                  {/* Bottom Navigation Pill */}
                  <div className="mt-4 pt-2 border-t border-[#1E1E24] flex justify-around text-[#71717A] text-[10px] font-mono-tech">
                    <span className="text-[#F4F4F5]">HOME</span>
                    <span>ACTIVITY</span>
                    <span>CALENDAR</span>
                    <span>SPENDO AI</span>
                  </div>
                </div>
              </div>

              {/* Decorative Subtle Background Glow */}
              <div className="absolute -inset-4 bg-gradient-to-r from-[#D71920]/10 to-transparent blur-2xl -z-10 rounded-full" />
            </div>
          </div>

        </div>
      </div>
    </section>
  );
};
