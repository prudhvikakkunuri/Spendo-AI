import React, { useState } from 'react';
import { ChevronDown } from 'lucide-react';

interface FaqItem {
  question: string;
  answer: string;
}

export const FaqSection: React.FC = () => {
  const [openIndex, setOpenIndex] = useState<number | null>(0);

  const faqs: FaqItem[] = [
    {
      question: "What is Spendo?",
      answer: "Spendo is an open-source, on-device personal finance and expense-tracking application for Android. It is designed to notice banking UPI and card debit notifications on-device, calculate safe-to-spend envelopes, and provide clear insights into your money without compromising your privacy."
    },
    {
      question: "Does Spendo read my SMS inbox?",
      answer: "No. Spendo does not declare or request Android's READ_SMS permission. Instead, it utilizes Android's standard NotificationListenerService exclusively for banking and payment applications that you explicitly choose to monitor in Settings. Spendo never accesses your private contacts, OTP messages, or chat histories."
    },
    {
      question: "Does Spendo connect directly to my bank account or UPI apps?",
      answer: "No. Spendo never connects to bank servers, open-banking APIs, or payment gateways, and it cannot initiate or modify financial transactions. It operates purely as an on-device personal spending ledger based on incoming notifications and manual or voice entries."
    },
    {
      question: "How does the voice-powered expense entry work?",
      answer: "When you tap the microphone action in the app or home-screen widget and speak your outlay (e.g. 'Paid ₹350 for dinner at Swiggy'), Spendo uses Android's on-device speech recognizer to parse the merchant, amount, and category into a draft. Spendo follows a strict rule: it never silently commits transactions without your explicit confirmation."
    },
    {
      question: "Does Spendo work without an AI API key?",
      answer: "Yes, 100%. All core personal finance calculations—spending summaries, category breakdowns, safe-to-spend headroom, and the 10 suggested financial queries—are calculated deterministically from your local Room SQLite database without needing any AI API keys or internet connection. If you wish to use advanced cloud LLMs, you can optionally supply your own Gemini, Claude, or OpenAI API key."
    },
    {
      question: "Where is my financial data stored?",
      answer: "All your data is stored strictly in your device's internal SQLite database managed by Android Room. There are no remote sync servers, cloud databases, or background telemetry SDKs. Standard Android cloud backup is disabled to prevent unencrypted database backups from reaching third-party cloud drives."
    },
    {
      question: "Is Spendo free?",
      answer: "Yes. Spendo is a free, open-source personal project. There are no advertisements, paid subscription tiers, or monetization schemes."
    },
    {
      question: "What are the minimum Android compatibility requirements?",
      answer: "Spendo requires Android 8.0 (Oreo / API level 26) or higher. It is compiled against Android 14 (API level 34) and tested across standard Android and Nothing OS launcher environments."
    }
  ];

  const toggleFaq = (idx: number) => {
    setOpenIndex(openIndex === idx ? null : idx);
  };

  return (
    <section id="faq" className="py-24 border-t border-[#27272E] light:border-[#E4E4E7] bg-[#09090B] light:bg-[#FAFAFA]">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Header */}
        <div className="text-center space-y-4 mb-16">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF]">
            <span className="dot-indicator-red" />
            <span className="font-mono-tech text-xs tracking-wider uppercase text-[#A1A1AA] light:text-[#71717A]">
              CLARIFICATIONS
            </span>
          </div>
          <h2 className="text-3xl sm:text-5xl font-bold tracking-tight text-[#F4F4F5] light:text-[#09090B]">
            Frequently Asked Questions
          </h2>
          <p className="text-base sm:text-lg text-[#A1A1AA] light:text-[#52525B]">
            Transparent answers about how Spendo works, how your data is treated, and our engineering decisions.
          </p>
        </div>

        {/* Accordion List */}
        <div className="space-y-4">
          {faqs.map((faq, idx) => {
            const isOpen = openIndex === idx;
            return (
              <div
                key={idx}
                className="rounded-2xl border border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF] overflow-hidden transition-all"
              >
                <button
                  onClick={() => toggleFaq(idx)}
                  className="w-full p-6 text-left flex items-center justify-between gap-4 focus:outline-none focus-visible:ring-2 focus-visible:ring-[#D71920]"
                  aria-expanded={isOpen}
                >
                  <span className="text-base sm:text-lg font-bold text-[#F4F4F5] light:text-[#09090B]">
                    {faq.question}
                  </span>
                  <ChevronDown
                    className={`w-5 h-5 text-[#A1A1AA] shrink-0 transition-transform duration-200 ${
                      isOpen ? 'rotate-180 text-[#D71920]' : ''
                    }`}
                  />
                </button>

                {isOpen && (
                  <div className="px-6 pb-6 pt-1 text-sm text-[#A1A1AA] light:text-[#52525B] leading-relaxed border-t border-[#1E1E24] light:border-[#F4F4F5]">
                    {faq.answer}
                  </div>
                )}
              </div>
            );
          })}
        </div>

      </div>
    </section>
  );
};
