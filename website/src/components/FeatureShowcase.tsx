import { 
  BellRing, 
  Mic, 
  Sparkles, 
  PieChart, 
  Building2, 
  CheckSquare2, 
  ShieldCheck, 
  FileSpreadsheet
} from 'lucide-react';

interface FeatureCardProps {
  tag: string;
  title: string;
  description: string;
  icon: React.ReactNode;
  status?: string;
  visualPreview?: React.ReactNode;
  colSpan?: string;
}

const FeatureCard: React.FC<FeatureCardProps> = ({
  tag,
  title,
  description,
  icon,
  status = "AVAILABLE",
  visualPreview,
  colSpan = "col-span-1"
}) => {
  return (
    <div className={`p-7 rounded-3xl border border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF] flex flex-col justify-between hover:border-[#3F3F4A] transition-all group ${colSpan}`}>
      <div>
        {/* Top Header */}
        <div className="flex items-center justify-between mb-5">
          <div className="w-10 h-10 rounded-xl bg-[#18181C] light:bg-[#F4F4F5] border border-[#27272E] light:border-[#E4E4E7] flex items-center justify-center text-[#F4F4F5] light:text-[#09090B] group-hover:text-[#D71920] transition-colors">
            {icon}
          </div>
          <div className="flex items-center gap-2">
            <span className="dot-indicator-green" />
            <span className="font-mono-tech text-[10px] tracking-wider uppercase text-[#71717A]">
              {status}
            </span>
          </div>
        </div>

        {/* Tag & Title */}
        <div className="text-[10px] font-mono-tech text-[#D71920] uppercase tracking-wider mb-1">
          {tag}
        </div>
        <h3 className="text-xl font-bold tracking-tight text-[#F4F4F5] light:text-[#09090B] mb-2">
          {title}
        </h3>
        <p className="text-sm text-[#A1A1AA] light:text-[#52525B] leading-relaxed mb-6">
          {description}
        </p>
      </div>

      {/* Visual Preview slot */}
      {visualPreview && (
        <div className="mt-auto pt-4 border-t border-[#1E1E24] light:border-[#F4F4F5]">
          {visualPreview}
        </div>
      )}
    </div>
  );
};

export const FeatureShowcase: React.FC = () => {
  return (
    <section id="features" className="py-24 border-t border-[#27272E] light:border-[#E4E4E7] bg-[#09090B] light:bg-[#FAFAFA]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header */}
        <div className="max-w-3xl mb-16 space-y-4">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF]">
            <span className="dot-indicator-red" />
            <span className="font-mono-tech text-xs tracking-wider uppercase text-[#A1A1AA] light:text-[#71717A]">
              ENGINEERED CAPABILITIES
            </span>
          </div>
          <h2 className="text-3xl sm:text-5xl font-bold tracking-tight text-[#F4F4F5] light:text-[#09090B]">
            Built with purpose. Nothing superficial.
          </h2>
          <p className="text-base sm:text-lg text-[#A1A1AA] light:text-[#52525B]">
            Every feature in Spendo is designed to minimize the friction of tracking personal outlays while keeping your financial life 100% private.
          </p>
        </div>

        {/* Features Bento Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">

          {/* 1. Notification Ingestion */}
          <FeatureCard
            tag="ON-DEVICE SENSING"
            title="Notification Transaction Catcher"
            description="Notice bank debit and credit transactions straight from user-selected payment apps (HDFC, ICICI, SBI, Kotak, Axis, Google Pay, PhonePe). Uses Android NotificationListenerService on-device."
            icon={<BellRing className="w-5 h-5" />}
            colSpan="md:col-span-2 lg:col-span-2"
            visualPreview={
              <div className="p-3.5 rounded-2xl bg-[#09090B] border border-[#27272E] flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-[#D71920]/10 border border-[#D71920]/30 flex items-center justify-center font-mono-tech font-bold text-xs text-[#D71920]">
                    UPI
                  </div>
                  <div>
                    <div className="text-xs font-bold text-white">Detected: Swiggy · ₹349.00</div>
                    <div className="text-[10px] font-mono-tech text-[#71717A]">Package: com.google.android.apps.nbu.paisa.user</div>
                  </div>
                </div>
                <span className="px-2 py-0.5 rounded text-[10px] font-mono-tech bg-[#22C55E]/10 text-[#22C55E] border border-[#22C55E]/30">
                  AUTO-LOGGED
                </span>
              </div>
            }
          />

          {/* 2. Voice-Powered Entry */}
          <FeatureCard
            tag="VOICE INTERFACE"
            title="Spoken Outlay Parsing"
            description="Say 'Spent ₹420 on fuel at IndianOil' into the widget or app. Spendo extracts the merchant, amount, and category into an editable draft."
            icon={<Mic className="w-5 h-5" />}
            visualPreview={
              <div className="p-3 rounded-2xl bg-[#09090B] border border-[#27272E] text-xs font-mono-tech text-[#A1A1AA]">
                <div className="flex items-center gap-2 text-[#D71920] mb-1">
                  <Mic className="w-3.5 h-3.5" />
                  <span>VOICE DRAFT CONFIRMATION</span>
                </div>
                <div className="text-white font-bold">₹420.00 → Fuel &amp; Travel</div>
              </div>
            }
          />

          {/* 3. Spendo AI Money Agent */}
          <FeatureCard
            tag="DETERMINISTIC INTELLIGENCE"
            title="Spendo AI Money Agent"
            description="Ask questions about your finances: top spending category, monthly projections, or savings suggestions. Answers are computed deterministically from Room SQLite data."
            icon={<Sparkles className="w-5 h-5" />}
            visualPreview={
              <div className="p-3 rounded-2xl bg-[#09090B] border border-[#27272E] text-xs font-mono-tech">
                <span className="text-[#71717A]">Q: "What is my biggest expense this month?"</span>
                <p className="text-[#22C55E] font-bold mt-1">A: ₹14,200.00 (Rent)</p>
              </div>
            }
          />

          {/* 4. Safe-to-Spend Budgets */}
          <FeatureCard
            tag="PAISE ACCURACY"
            title="Safe-to-Spend &amp; Budget Envelopes"
            description="Set daily and monthly spending limits. All currency calculations use 64-bit integer paise to eliminate floating-point rounding errors."
            icon={<PieChart className="w-5 h-5" />}
            visualPreview={
              <div className="space-y-1.5 font-mono-tech text-xs">
                <div className="flex justify-between text-[11px] text-[#A1A1AA]">
                  <span>Daily Cap: ₹1,500</span>
                  <span className="text-[#22C55E]">₹820 Left</span>
                </div>
                <div className="h-1.5 w-full bg-[#18181C] rounded-full overflow-hidden">
                  <div className="h-full bg-[#22C55E] rounded-full w-[45%]" />
                </div>
              </div>
            }
          />

          {/* 5. Verification Desk */}
          <FeatureCard
            tag="HUMAN IN THE LOOP"
            title="Verification Desk"
            description="Ambiguous transaction notifications and unconfirmed merchants land in a review queue. Spendo never silently guesses without your explicit approval."
            icon={<CheckSquare2 className="w-5 h-5" />}
            visualPreview={
              <div className="p-3 rounded-2xl bg-[#09090B] border border-[#27272E] flex items-center justify-between text-xs font-mono-tech">
                <span className="text-[#D71920]">1 Review Pending</span>
                <span className="text-white hover:underline cursor-pointer">Verify Now →</span>
              </div>
            }
          />

          {/* 6. Multi-Bank Accounts */}
          <FeatureCard
            tag="ACCOUNT LEDGER"
            title="Multi-Bank Tracking &amp; Transfers"
            description="Track distinct accounts across banks (HDFC, SBI, Axis, Cash). Transfers between your own accounts are accurately recorded without skewing expense totals."
            icon={<Building2 className="w-5 h-5" />}
          />

          {/* 7. Vault Security */}
          <FeatureCard
            tag="LOCAL ENCRYPTION"
            title="Biometric &amp; PBKDF2 Vault"
            description="Secure your financial records with fingerprint/face unlock and a salted PBKDF2 password. Balance concealment mode hides totals from casual observers."
            icon={<ShieldCheck className="w-5 h-5" />}
          />

          {/* 8. CSV Import / Export */}
          <FeatureCard
            tag="OPEN DATA"
            title="SAF-Compliant CSV Export &amp; Import"
            description="Take full custody of your records anytime. Export clean UTF-8 CSVs or import existing transaction sheets using Android's Storage Access Framework."
            icon={<FileSpreadsheet className="w-5 h-5" />}
          />

        </div>

      </div>
    </section>
  );
};
