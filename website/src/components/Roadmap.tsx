import React, { useState } from 'react';
import { CheckCircle2, Clock, Calendar, Sparkles } from 'lucide-react';

interface RoadmapItem {
  title: string;
  category: string;
  description: string;
  status: 'available' | 'in-progress' | 'planned' | 'exploring';
}

export const Roadmap: React.FC = () => {
  const [filter, setFilter] = useState<'all' | 'available' | 'in-progress' | 'planned'>('all');

  const roadmapItems: RoadmapItem[] = [
    // Available
    {
      title: "Notification Transaction Ingestion",
      category: "CORE ENGINE",
      description: "On-device parsing of UPI and card debit notifications from user-selected banking apps without reading SMS.",
      status: "available"
    },
    {
      title: "64-bit Integer Paise Room Database",
      category: "DATA INTEGRITY",
      description: "Complete currency arithmetic executed in integer minor units (paise) to eliminate floating-point rounding discrepancies.",
      status: "available"
    },
    {
      title: "Voice-Powered Expense Entry",
      category: "VOICE INTERFACE",
      description: "Natural speech processing into an editable draft confirmation dialog directly from widget or app.",
      status: "available"
    },
    {
      title: "Android 2x2 & 4x2 Home-Screen Widgets",
      category: "WIDGETS",
      description: "Interactive home-screen widgets with live daily outlay, bank balance display, and 1-tap quick expense dialog.",
      status: "available"
    },
    {
      title: "Spendo AI Money Agent (Offline)",
      category: "INTELLIGENCE",
      description: "Deterministic answers for 10 common financial queries computed locally from your SQLite database.",
      status: "available"
    },
    {
      title: "Verification Desk Review Queue",
      category: "ACCURACY",
      description: "Review workflow for inspecting, categorizing, and approving ambiguous notifications and transfers.",
      status: "available"
    },
    // In Progress
    {
      title: "Merchant Auto-Allocation Memory",
      category: "AUTOMATION",
      description: "Remember user-corrected categories for specific merchants and automatically allocate future occurrences.",
      status: "in-progress"
    },
    {
      title: "Recurring Commitment Reminders",
      category: "PLANNING",
      description: "Proactive notifications before scheduled monthly bills, subscriptions, and loan EMIs are due.",
      status: "in-progress"
    },
    // Planned
    {
      title: "Split Expense Tracking",
      category: "BUDGETING",
      description: "Log shared restaurant bills or group trip expenses with friend allocations and settlement tracking.",
      status: "planned"
    },
    {
      title: "PDF Bank Statement Parser",
      category: "DATA IMPORT",
      description: "Client-side PDF parser for importing historical bank statements from supported Indian banks.",
      status: "planned"
    },
    // Exploring
    {
      title: "On-Device Quantized SLM",
      category: "RESEARCH",
      description: "Executing a lightweight 2B parameter financial model directly on mobile NPU via MediaPipe or ONNX Runtime.",
      status: "exploring"
    }
  ];

  const filteredItems = filter === 'all'
    ? roadmapItems
    : roadmapItems.filter(item => item.status === filter);

  const getStatusBadge = (status: RoadmapItem['status']) => {
    switch (status) {
      case 'available':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-mono-tech bg-[#22C55E]/10 text-[#22C55E] border border-[#22C55E]/30">
            <CheckCircle2 className="w-3 h-3" />
            AVAILABLE NOW
          </span>
        );
      case 'in-progress':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-mono-tech bg-[#EAB308]/10 text-[#EAB308] border border-[#EAB308]/30">
            <Clock className="w-3 h-3" />
            IN PROGRESS
          </span>
        );
      case 'planned':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-mono-tech bg-[#3B82F6]/10 text-[#3B82F6] border border-[#3B82F6]/30">
            <Calendar className="w-3 h-3" />
            PLANNED
          </span>
        );
      case 'exploring':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-mono-tech bg-[#A855F7]/10 text-[#A855F7] border border-[#A855F7]/30">
            <Sparkles className="w-3 h-3" />
            EXPLORING
          </span>
        );
    }
  };

  return (
    <section id="roadmap" className="py-24 border-t border-[#27272E] light:border-[#E4E4E7] bg-[#09090B] light:bg-[#FAFAFA]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Header */}
        <div className="max-w-3xl mb-12 space-y-4">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF]">
            <span className="dot-indicator-red" />
            <span className="font-mono-tech text-xs tracking-wider uppercase text-[#A1A1AA] light:text-[#71717A]">
              DEVELOPMENT ROADMAP
            </span>
          </div>
          <h2 className="text-3xl sm:text-5xl font-bold tracking-tight text-[#F4F4F5] light:text-[#09090B]">
            Truthful roadmap. No phantom features.
          </h2>
          <p className="text-base sm:text-lg text-[#A1A1AA] light:text-[#52525B]">
            We clearly distinguish what is already compiled and verified in our repository from what is actively being built or planned for future releases.
          </p>
        </div>

        {/* Filter Tabs */}
        <div className="flex flex-wrap gap-2 mb-10">
          {(['all', 'available', 'in-progress', 'planned'] as const).map((tabKey) => (
            <button
              key={tabKey}
              onClick={() => setFilter(tabKey)}
              className={`px-4 py-2 rounded-xl text-xs font-mono-tech transition-all uppercase ${
                filter === tabKey
                  ? 'bg-[#F4F4F5] light:bg-[#09090B] text-black light:text-white font-bold'
                  : 'bg-[#121215] light:bg-[#FFFFFF] border border-[#27272E] light:border-[#E4E4E7] text-[#A1A1AA] hover:border-[#3F3F4A]'
              }`}
            >
              {tabKey}
            </button>
          ))}
        </div>

        {/* Roadmap Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredItems.map((item, idx) => (
            <div
              key={idx}
              className="p-6 rounded-3xl border border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF] flex flex-col justify-between hover:border-[#3F3F4A] transition-all space-y-4"
            >
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span className="text-[10px] font-mono-tech text-[#71717A] uppercase tracking-wider">
                    {item.category}
                  </span>
                  {getStatusBadge(item.status)}
                </div>
                <h3 className="text-lg font-bold text-[#F4F4F5] light:text-[#09090B] mb-2">
                  {item.title}
                </h3>
                <p className="text-xs sm:text-sm text-[#A1A1AA] light:text-[#52525B] leading-relaxed">
                  {item.description}
                </p>
              </div>
            </div>
          ))}
        </div>

      </div>
    </section>
  );
};
