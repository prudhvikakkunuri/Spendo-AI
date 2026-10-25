import { ShieldAlert, Lock } from 'lucide-react';

export const ProductIntro: React.FC = () => {
  return (
    <section className="py-24 border-t border-[#27272E] light:border-[#E4E4E7] bg-[#09090B] light:bg-[#FAFAFA]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header */}
        <div className="max-w-3xl mx-auto text-center space-y-4">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF]">
            <span className="dot-indicator-red" />
            <span className="font-mono-tech text-xs tracking-wider uppercase text-[#A1A1AA] light:text-[#71717A]">
              THE PROBLEM &amp; THE PHILOSOPHY
            </span>
          </div>
          <h2 className="text-3xl sm:text-5xl font-bold tracking-tight text-[#F4F4F5] light:text-[#09090B]">
            Expense tracking shouldn't mean sacrificing your privacy.
          </h2>
          <p className="text-base sm:text-lg text-[#A1A1AA] light:text-[#52525B] leading-relaxed">
            Most financial apps require connecting live bank credentials, read your entire SMS inbox,
            and upload your transaction history to cloud servers for advertising. Spendo changes the paradigm.
          </p>
        </div>

        {/* Contrast Comparison Grid */}
        <div className="mt-16 grid grid-cols-1 md:grid-cols-2 gap-8">
          
          {/* Traditional Expense Trackers */}
          <div className="p-8 rounded-3xl border border-[#27272E] light:border-[#E4E4E7] bg-[#121215]/50 light:bg-[#FFFFFF] space-y-6">
            <div className="flex items-center justify-between border-b border-[#27272E] light:border-[#E4E4E7] pb-4">
              <span className="font-mono-tech text-xs tracking-wider uppercase text-[#71717A]">
                CONVENTIONAL TRACKERS
              </span>
              <ShieldAlert className="w-5 h-5 text-[#71717A]" />
            </div>
            
            <ul className="space-y-4 text-sm text-[#A1A1AA] light:text-[#71717A]">
              <li className="flex items-start gap-3">
                <span className="text-[#D71920] font-bold">✕</span>
                <span><strong>Invasive Cloud Uploads:</strong> Your entire purchase ledger and net worth are stored on third-party servers.</span>
              </li>
              <li className="flex items-start gap-3">
                <span className="text-[#D71920] font-bold">✕</span>
                <span><strong>Overreaching SMS Permissions:</strong> Apps scan your entire personal messaging history under the guise of banking SMS detection.</span>
              </li>
              <li className="flex items-start gap-3">
                <span className="text-[#D71920] font-bold">✕</span>
                <span><strong>Manual Logging Burnout:</strong> Typing every coffee and grocery bill leads to tracker abandonment within weeks.</span>
              </li>
              <li className="flex items-start gap-3">
                <span className="text-[#D71920] font-bold">✕</span>
                <span><strong>Monetized with Ads &amp; Loans:</strong> Clean interfaces are gradually replaced with credit card advertisements and loan promotions.</span>
              </li>
            </ul>
          </div>

          {/* The Spendo Way */}
          <div className="p-8 rounded-3xl border-2 border-[#D71920]/40 bg-[#121215] light:bg-[#FFFFFF] space-y-6 relative overflow-hidden shadow-xl shadow-black/40">
            <div className="flex items-center justify-between border-b border-[#27272E] light:border-[#E4E4E7] pb-4">
              <span className="font-mono-tech text-xs tracking-wider uppercase text-[#D71920] font-bold">
                THE SPENDO ARCHITECTURE
              </span>
              <Lock className="w-5 h-5 text-[#D71920]" />
            </div>

            <ul className="space-y-4 text-sm text-[#F4F4F5] light:text-[#09090B]">
              <li className="flex items-start gap-3">
                <span className="text-[#22C55E] font-bold">✓</span>
                <span><strong>100% On-Device Storage:</strong> All accounts, budgets, and transactions live strictly inside your local Android Room SQLite database.</span>
              </li>
              <li className="flex items-start gap-3">
                <span className="text-[#22C55E] font-bold">✓</span>
                <span><strong>Selective Notification Ingestion:</strong> Spendo uses Android's NotificationListenerService only for apps you explicitly select. No SMS permission required.</span>
              </li>
              <li className="flex items-start gap-3">
                <span className="text-[#22C55E] font-bold">✓</span>
                <span><strong>One-Tap Voice Logging:</strong> Speak outlays in natural language ("₹450 for lunch at Swiggy"); Spendo drafts the entry for confirmation.</span>
              </li>
              <li className="flex items-start gap-3">
                <span className="text-[#22C55E] font-bold">✓</span>
                <span><strong>Deterministic Answering:</strong> Spendo AI queries your verified database deterministically. Never hallucinates numbers.</span>
              </li>
            </ul>

            {/* Subtle Accent Glow */}
            <div className="absolute top-0 right-0 w-32 h-32 bg-[#D71920]/10 rounded-full blur-3xl pointer-events-none" />
          </div>

        </div>

      </div>
    </section>
  );
};
