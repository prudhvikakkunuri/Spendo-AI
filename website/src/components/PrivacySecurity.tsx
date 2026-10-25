import React from 'react';
import { Shield, Lock, EyeOff, ServerOff, KeyRound, HardDriveDownload } from 'lucide-react';

export const PrivacySecurity: React.FC = () => {
  const securityPillars = [
    {
      title: "100% On-Device Room Database",
      description: "Every account, category, budget, and transaction record is stored directly on your phone in an encrypted local Room SQLite database. We do not operate a backend server, user database, or central ledger.",
      icon: <ServerOff className="w-5 h-5 text-[#22C55E]" />
    },
    {
      title: "Zero Inbound SMS Permissions",
      description: "Unlike traditional Indian expense apps, Spendo never requests READ_SMS permission. It utilizes Android's NotificationListenerService exclusively for banking apps you manually select in settings.",
      icon: <EyeOff className="w-5 h-5 text-[#22C55E]" />
    },
    {
      title: "No Account, Login, or KYC Needed",
      description: "You do not need to register with your phone number, email, or Google account. Spendo works the moment you install it. Your financial life belongs to you.",
      icon: <Lock className="w-5 h-5 text-[#22C55E]" />
    },
    {
      title: "Bring-Your-Own-Key (BYOK) AI",
      description: "Local financial questions are answered deterministically offline. For cloud LLM exploration (Gemini, Claude, OpenAI), your API keys are stored securely using Android EncryptedSharedPreferences.",
      icon: <KeyRound className="w-5 h-5 text-[#22C55E]" />
    },
    {
      title: "Android Cloud Backup Disabled",
      description: "Spendo explicitly disables standard Android cloud backup (android:allowBackup=false) to ensure unencrypted transaction dumps are never uploaded to third-party cloud drives without your knowledge.",
      icon: <Shield className="w-5 h-5 text-[#22C55E]" />
    },
    {
      title: "Full Data Custody & Export",
      description: "Export all your transactions anytime to clean, standard UTF-8 CSV files via Android's Storage Access Framework. You can purge or backup your ledger in a single tap.",
      icon: <HardDriveDownload className="w-5 h-5 text-[#22C55E]" />
    }
  ];

  return (
    <section id="privacy" className="py-24 border-t border-[#27272E] light:border-[#E4E4E7] bg-[#09090B] light:bg-[#FAFAFA]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Header */}
        <div className="max-w-3xl mb-16 space-y-4">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF]">
            <span className="dot-indicator-red" />
            <span className="font-mono-tech text-xs tracking-wider uppercase text-[#A1A1AA] light:text-[#71717A]">
              VERIFIED PRIVACY ARCHITECTURE
            </span>
          </div>
          <h2 className="text-3xl sm:text-5xl font-bold tracking-tight text-[#F4F4F5] light:text-[#09090B]">
            Your data never leaves your device.
          </h2>
          <p className="text-base sm:text-lg text-[#A1A1AA] light:text-[#52525B]">
            We believe personal finance software should be a local vault, not a cloud surveillance engine. Here is our exact technical commitment.
          </p>
        </div>

        {/* Pillars Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {securityPillars.map((pillar, idx) => (
            <div
              key={idx}
              className="p-7 rounded-3xl border border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF] space-y-4 hover:border-[#3F3F4A] transition-all"
            >
              <div className="w-10 h-10 rounded-xl bg-[#18181C] light:bg-[#F4F4F5] border border-[#27272E] light:border-[#E4E4E7] flex items-center justify-center">
                {pillar.icon}
              </div>
              <h3 className="text-lg font-bold text-[#F4F4F5] light:text-[#09090B]">
                {pillar.title}
              </h3>
              <p className="text-xs sm:text-sm text-[#A1A1AA] light:text-[#52525B] leading-relaxed">
                {pillar.description}
              </p>
            </div>
          ))}
        </div>

        {/* Technical Callout */}
        <div className="mt-12 p-6 rounded-2xl border border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF] flex flex-col sm:flex-row items-center justify-between gap-6">
          <div className="space-y-1">
            <div className="font-mono-tech text-xs font-bold text-[#F4F4F5] light:text-[#09090B]">
              OPEN-SOURCE &amp; REPRODUCIBLE BUILDS
            </div>
            <p className="text-xs text-[#A1A1AA] light:text-[#52525B]">
              Every line of Kotlin, Jetpack Compose UI, and Room DAO logic is auditable on GitHub.
            </p>
          </div>
          <a
            href="https://github.com/prudhvikakkunuri/Spendo-AI/blob/main/SECURITY.md"
            target="_blank"
            rel="noopener noreferrer"
            className="px-5 py-2.5 rounded-xl text-xs font-mono-tech font-bold border border-[#27272E] light:border-[#E4E4E7] hover:border-[#3F3F4A] text-white light:text-black whitespace-nowrap transition-colors"
          >
            Review Security Policy (SECURITY.md) →
          </a>
        </div>

      </div>
    </section>
  );
};
