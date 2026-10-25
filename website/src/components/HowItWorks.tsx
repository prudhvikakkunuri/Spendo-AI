import React from 'react';
import { Smartphone, CheckCircle, Database, BarChart3, ArrowRight } from 'lucide-react';

export const HowItWorks: React.FC = () => {
  const steps = [
    {
      number: "01",
      title: "Capture the Outlay",
      subtitle: "Instant Sensing",
      description: "When you pay via UPI or card, Spendo's on-device notification listener catches the bank alert. Or simply tap the home-screen widget and speak your expense.",
      icon: <Smartphone className="w-5 h-5 text-[#D71920]" />
    },
    {
      number: "02",
      title: "Inspect the Draft",
      subtitle: "Zero Guesswork",
      description: "Spendo extracts the merchant, amount, category, and source bank. It never silently commits uncertain or ambiguous records behind your back.",
      icon: <CheckCircle className="w-5 h-5 text-[#D71920]" />
    },
    {
      number: "03",
      title: "Save to Local Ledger",
      subtitle: "Atomic Persistence",
      description: "With one tap, the transaction is committed to your local Room SQLite database. Monetary amounts are stored as integer paise to guarantee absolute math precision.",
      icon: <Database className="w-5 h-5 text-[#D71920]" />
    },
    {
      number: "04",
      title: "Understand Your Money",
      subtitle: "Deterministic AI",
      description: "Check your Safe-to-Spend budget, explore month-over-month category breakdowns, and ask Spendo AI questions that are answered directly from verified local records.",
      icon: <BarChart3 className="w-5 h-5 text-[#D71920]" />
    }
  ];

  return (
    <section id="how-it-works" className="py-24 border-t border-[#27272E] light:border-[#E4E4E7] bg-[#09090B] light:bg-[#FAFAFA]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Header */}
        <div className="max-w-3xl mb-16 space-y-4">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF]">
            <span className="dot-indicator-red" />
            <span className="font-mono-tech text-xs tracking-wider uppercase text-[#A1A1AA] light:text-[#71717A]">
              THE WORKFLOW
            </span>
          </div>
          <h2 className="text-3xl sm:text-5xl font-bold tracking-tight text-[#F4F4F5] light:text-[#09090B]">
            From bank notification to budget clarity in 4 steps.
          </h2>
          <p className="text-base sm:text-lg text-[#A1A1AA] light:text-[#52525B]">
            A seamless bridge between automatic payment catching and deliberate personal financial control.
          </p>
        </div>

        {/* Steps Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {steps.map((step, idx) => (
            <div
              key={idx}
              className="p-7 rounded-3xl border border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF] flex flex-col justify-between hover:border-[#3F3F4A] transition-all relative group"
            >
              <div>
                <div className="flex items-center justify-between mb-8">
                  <span className="font-mono-tech text-3xl font-extrabold text-[#27272E] light:text-[#E4E4E7] group-hover:text-[#D71920] transition-colors">
                    {step.number}
                  </span>
                  <div className="w-10 h-10 rounded-xl bg-[#18181C] light:bg-[#F4F4F5] border border-[#27272E] light:border-[#E4E4E7] flex items-center justify-center">
                    {step.icon}
                  </div>
                </div>

                <div className="text-[10px] font-mono-tech text-[#D71920] uppercase tracking-wider mb-1">
                  {step.subtitle}
                </div>
                <h3 className="text-lg font-bold text-[#F4F4F5] light:text-[#09090B] mb-2">
                  {step.title}
                </h3>
                <p className="text-xs sm:text-sm text-[#A1A1AA] light:text-[#52525B] leading-relaxed">
                  {step.description}
                </p>
              </div>

              {idx < steps.length - 1 && (
                <div className="hidden lg:block absolute -right-3 top-1/2 -translate-y-1/2 z-10 text-[#27272E]">
                  <ArrowRight className="w-5 h-5 text-[#3F3F4A]" />
                </div>
              )}
            </div>
          ))}
        </div>

      </div>
    </section>
  );
};
