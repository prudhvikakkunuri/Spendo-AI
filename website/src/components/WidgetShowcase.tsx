import { useState } from 'react';
import { Mic, Plus, CheckCircle2 } from 'lucide-react';

export const WidgetShowcase: React.FC = () => {
  const [widgetType, setWidgetType] = useState<'2x2' | '4x2'>('2x2');

  return (
    <section id="widgets" className="py-24 border-t border-[#27272E] light:border-[#E4E4E7] bg-[#09090B] light:bg-[#FAFAFA]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Header */}
        <div className="max-w-3xl mb-16 space-y-4">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF]">
            <span className="dot-indicator-red" />
            <span className="font-mono-tech text-xs tracking-wider uppercase text-[#A1A1AA] light:text-[#71717A]">
              ANDROID HOME-SCREEN INTEGRATION
            </span>
          </div>
          <h2 className="text-3xl sm:text-5xl font-bold tracking-tight text-[#F4F4F5] light:text-[#09090B]">
            Never open an app just to log a chai.
          </h2>
          <p className="text-base sm:text-lg text-[#A1A1AA] light:text-[#52525B]">
            Spendo's home-screen widgets bring real-time balance totals, safe-to-spend headroom, and 1-tap voice entry straight to your Android launcher.
          </p>
        </div>

        {/* Interactive Widget Explorer */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
          
          {/* Controls & Copy */}
          <div className="lg:col-span-6 space-y-6">
            <div className="flex gap-3">
              <button
                onClick={() => setWidgetType('2x2')}
                className={`px-4 py-2.5 rounded-xl font-mono-tech text-xs font-bold transition-all ${
                  widgetType === '2x2'
                    ? 'bg-[#D71920] text-white'
                    : 'bg-[#121215] light:bg-[#FFFFFF] border border-[#27272E] light:border-[#E4E4E7] text-[#A1A1AA]'
                }`}
              >
                2x2 Compact Widget
              </button>
              <button
                onClick={() => setWidgetType('4x2')}
                className={`px-4 py-2.5 rounded-xl font-mono-tech text-xs font-bold transition-all ${
                  widgetType === '4x2'
                    ? 'bg-[#D71920] text-white'
                    : 'bg-[#121215] light:bg-[#FFFFFF] border border-[#27272E] light:border-[#E4E4E7] text-[#A1A1AA]'
                }`}
              >
                4x2 Deep Summary Widget
              </button>
            </div>

            <div className="space-y-4 text-sm text-[#A1A1AA] light:text-[#52525B]">
              {widgetType === '2x2' ? (
                <>
                  <h3 className="text-xl font-bold text-[#F4F4F5] light:text-[#09090B]">
                    Instant 1-Tap Entry &amp; Daily Spend
                  </h3>
                  <p>
                    Fits seamlessly alongside your home-screen app icons. Shows exactly what you've spent today and your combined bank balance.
                  </p>
                  <ul className="space-y-2.5 font-mono-tech text-xs text-[#F4F4F5] light:text-[#09090B]">
                    <li className="flex items-center gap-2">
                      <CheckCircle2 className="w-4 h-4 text-[#22C55E]" />
                      <span>Transparent floating dialog popup without launching the full dashboard</span>
                    </li>
                    <li className="flex items-center gap-2">
                      <CheckCircle2 className="w-4 h-4 text-[#22C55E]" />
                      <span>Direct voice microphone action for spoken entries</span>
                    </li>
                    <li className="flex items-center gap-2">
                      <CheckCircle2 className="w-4 h-4 text-[#22C55E]" />
                      <span>Real-time Room SQLite updates when any transaction is committed</span>
                    </li>
                  </ul>
                </>
              ) : (
                <>
                  <h3 className="text-xl font-bold text-[#F4F4F5] light:text-[#09090B]">
                    Comprehensive Ledger &amp; Budget Headroom
                  </h3>
                  <p>
                    A wider dashboard surface displaying monthly spend envelopes, dynamic safe-to-spend progress, and quick expense entry.
                  </p>
                  <ul className="space-y-2.5 font-mono-tech text-xs text-[#F4F4F5] light:text-[#09090B]">
                    <li className="flex items-center gap-2">
                      <CheckCircle2 className="w-4 h-4 text-[#22C55E]" />
                      <span>Dual metric: Today's outlay alongside current monthly total</span>
                    </li>
                    <li className="flex items-center gap-2">
                      <CheckCircle2 className="w-4 h-4 text-[#22C55E]" />
                      <span>Visual budget gauge showing percentage of allocated envelope used</span>
                    </li>
                    <li className="flex items-center gap-2">
                      <CheckCircle2 className="w-4 h-4 text-[#22C55E]" />
                      <span>Direct deep-links to Review Desk, Add Expense, and Analytics</span>
                    </li>
                  </ul>
                </>
              )}
            </div>
          </div>

          {/* Visual Widget Render Container */}
          <div className="lg:col-span-6 flex justify-center">
            {widgetType === '2x2' ? (
              /* 2x2 Widget Mock */
              <div className="w-64 h-64 rounded-3xl p-5 bg-[#121215] border-2 border-[#27272E] shadow-2xl flex flex-col justify-between select-none relative group hover:border-[#D71920]/60 transition-all">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="w-2 h-2 rounded-full bg-[#D71920]" />
                    <span className="font-mono-tech text-[10px] text-[#A1A1AA] uppercase tracking-wider">
                      TODAY SPENT
                    </span>
                  </div>
                  <span className="font-mono-tech text-[10px] text-[#71717A]">
                    SPENDO
                  </span>
                </div>

                <div>
                  <div className="text-3xl font-mono-tech font-bold text-white tracking-tight">
                    ₹420<span className="text-lg text-[#71717A]">.00</span>
                  </div>
                  <div className="text-[11px] font-mono-tech text-[#22C55E] mt-1">
                    Balance: ₹1,48,250.00
                  </div>
                </div>

                {/* Widget Buttons */}
                <div className="grid grid-cols-2 gap-2 pt-2 border-t border-[#1E1E24]">
                  <div className="py-2 px-2.5 rounded-xl bg-[#18181C] border border-[#27272E] text-white flex items-center justify-center gap-1.5 text-xs font-mono-tech font-bold">
                    <Plus className="w-3.5 h-3.5 text-[#D71920]" />
                    <span>Expense</span>
                  </div>
                  <div className="py-2 px-2 rounded-xl bg-[#18181C] border border-[#27272E] text-white flex items-center justify-center gap-1.5 text-xs font-mono-tech">
                    <Mic className="w-3.5 h-3.5 text-[#22C55E]" />
                    <span>Voice</span>
                  </div>
                </div>
              </div>
            ) : (
              /* 4x2 Widget Mock */
              <div className="w-full max-w-md rounded-3xl p-6 bg-[#121215] border-2 border-[#27272E] shadow-2xl space-y-4 select-none relative hover:border-[#D71920]/60 transition-all">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="w-2 h-2 rounded-full bg-[#D71920]" />
                    <span className="font-mono-tech text-[11px] text-[#A1A1AA] uppercase tracking-wider">
                      SPENDO // MONTHLY SAFE-TO-SPEND
                    </span>
                  </div>
                  <span className="font-mono-tech text-[10px] text-[#22C55E]">
                    LIVE SYNC
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <span className="text-[10px] font-mono-tech text-[#71717A] block">TODAY'S OUTFLOW</span>
                    <span className="text-xl font-mono-tech font-bold text-white">₹420.00</span>
                  </div>
                  <div>
                    <span className="text-[10px] font-mono-tech text-[#71717A] block">MONTH TOTAL</span>
                    <span className="text-xl font-mono-tech font-bold text-white">₹24,850.00</span>
                  </div>
                </div>

                {/* Progress */}
                <div>
                  <div className="flex justify-between text-[10px] font-mono-tech text-[#71717A] mb-1">
                    <span>Envelope: ₹45,000.00</span>
                    <span className="text-white">55.2% Used</span>
                  </div>
                  <div className="h-2 w-full bg-[#18181C] rounded-full overflow-hidden">
                    <div className="h-full bg-[#F4F4F5] rounded-full w-[55.2%]" />
                  </div>
                </div>

                {/* Actions */}
                <div className="flex items-center justify-between pt-2 border-t border-[#1E1E24]">
                  <span className="text-xs font-mono-tech text-[#A1A1AA]">
                    Net Balance: ₹1,48,250
                  </span>
                  <div className="flex gap-2">
                    <span className="px-3 py-1.5 rounded-lg bg-[#18181C] border border-[#27272E] text-xs font-mono-tech text-white flex items-center gap-1">
                      <Mic className="w-3 h-3 text-[#D71920]" />
                      Voice
                    </span>
                    <span className="px-3 py-1.5 rounded-lg bg-[#D71920] text-xs font-mono-tech font-bold text-white flex items-center gap-1">
                      <Plus className="w-3 h-3" />
                      Add
                    </span>
                  </div>
                </div>
              </div>
            )}
          </div>

        </div>

      </div>
    </section>
  );
};
