import { useState } from 'react';
import { Check, ArrowRight, RotateCcw } from 'lucide-react';

interface ParsedTransaction {
  amount: number;
  merchant: string;
  category: string;
  bank: string;
  rawText?: string;
}

export const InteractiveDemo: React.FC = () => {
  const samplePrompts = [
    { text: "Paid ₹280 to Starbucks for Coffee via HDFC", rawText: "Paid ₹280 to Starbucks for Coffee via HDFC", amount: 280, merchant: "Starbucks", category: "Food & Dining", bank: "HDFC Bank" },
    { text: "Spent ₹1450 on Groceries at Nature's Basket using Kotak", rawText: "Spent ₹1450 on Groceries at Nature's Basket using Kotak", amount: 1450, merchant: "Nature's Basket", category: "Groceries", bank: "Kotak Mahindra" },
    { text: "Paid ₹45 for Masala Chai at Tea Stall via UPI", rawText: "Paid ₹45 for Masala Chai at Tea Stall via UPI", amount: 45, merchant: "Tea Stall", category: "Food & Dining", bank: "UPI / Google Pay" },
    { text: "Uber cab fare ₹420 from Airport paid with ICICI", rawText: "Uber cab fare ₹420 from Airport paid with ICICI", amount: 420, merchant: "Uber", category: "Travel & Fuel", bank: "ICICI Bank" }
  ];

  const [inputText, setInputText] = useState(samplePrompts[0].text);
  const [parsed, setParsed] = useState<ParsedTransaction>(samplePrompts[0]);
  const [isConfirmed, setIsConfirmed] = useState(false);
  const [ledgerCount, setLedgerCount] = useState(3);
  const [spentTotal, setSpentTotal] = useState(24850);

  // Simple client-side parser simulation
  const handleTextChange = (text: string) => {
    setInputText(text);
    setIsConfirmed(false);

    // Heuristic extraction
    const amountMatch = text.match(/₹?\s?(\d+(?:,\d+)*(?:\.\d{1,2})?)/);
    const amountVal = amountMatch ? parseFloat(amountMatch[1].replace(/,/g, '')) : 250;

    let category = "General Outlay";
    const lower = text.toLowerCase();
    if (lower.includes('coffee') || lower.includes('lunch') || lower.includes('dinner') || lower.includes('starbucks') || lower.includes('chai') || lower.includes('food')) {
      category = "Food & Dining";
    } else if (lower.includes('groceries') || lower.includes('supermarket') || lower.includes('basket')) {
      category = "Groceries";
    } else if (lower.includes('uber') || lower.includes('ola') || lower.includes('fuel') || lower.includes('petrol') || lower.includes('cab')) {
      category = "Travel & Fuel";
    }

    let bank = "Primary Bank (HDFC)";
    if (lower.includes('kotak')) bank = "Kotak Mahindra";
    else if (lower.includes('icici')) bank = "ICICI Bank";
    else if (lower.includes('sbi')) bank = "State Bank of India";
    else if (lower.includes('upi') || lower.includes('gpay') || lower.includes('paytm')) bank = "UPI Direct";

    // Extract a plausible merchant
    let merchant = "Merchant";
    if (lower.includes('starbucks')) merchant = "Starbucks";
    else if (lower.includes("nature's basket")) merchant = "Nature's Basket";
    else if (lower.includes("uber")) merchant = "Uber";
    else if (lower.includes("tea stall")) merchant = "Tea Stall";
    else {
      const parts = text.split(/\s+/);
      merchant = parts.length > 2 ? parts[2] : "Local Vendor";
    }

    setParsed({
      amount: isNaN(amountVal) ? 250 : amountVal,
      merchant: merchant,
      category: category,
      bank: bank,
      rawText: text
    });
  };

  const handleConfirm = () => {
    setIsConfirmed(true);
    setSpentTotal((prev) => prev + parsed.amount);
    setLedgerCount((prev) => prev + 1);
  };

  const handleReset = () => {
    setIsConfirmed(false);
    setInputText(samplePrompts[0].text);
    setParsed(samplePrompts[0]);
    setSpentTotal(24850);
    setLedgerCount(3);
  };

  return (
    <section className="py-24 border-t border-[#27272E] light:border-[#E4E4E7] bg-[#09090B] light:bg-[#FAFAFA]">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Header */}
        <div className="text-center space-y-4 mb-14">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF]">
            <span className="dot-indicator-red" />
            <span className="font-mono-tech text-xs tracking-wider uppercase text-[#A1A1AA] light:text-[#71717A]">
              TRY SPENDO IN YOUR BROWSER
            </span>
          </div>
          <h2 className="text-3xl sm:text-5xl font-bold tracking-tight text-[#F4F4F5] light:text-[#09090B]">
            Interactive Expense Simulator
          </h2>
          <p className="text-base sm:text-lg text-[#A1A1AA] light:text-[#52525B] max-w-2xl mx-auto">
            Experience how Spendo catches verbal or typed outlays, categorizes them instantly, and requires your explicit confirmation before updating the ledger.
          </p>
        </div>

        {/* Demo Box */}
        <div className="p-6 sm:p-10 rounded-3xl border border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF] shadow-2xl space-y-8">
          
          {/* Preset Buttons */}
          <div>
            <label className="block text-xs font-mono-tech text-[#71717A] mb-2 uppercase">
              Select or type a sample outlay:
            </label>
            <div className="flex flex-wrap gap-2">
              {samplePrompts.map((p, idx) => (
                <button
                  key={idx}
                  onClick={() => {
                    setInputText(p.text);
                    setParsed(p);
                    setIsConfirmed(false);
                  }}
                  className={`text-xs font-mono-tech px-3 py-2 rounded-xl border transition-all text-left ${
                    inputText === p.text
                      ? 'border-[#D71920] bg-[#D71920]/10 text-white'
                      : 'border-[#27272E] light:border-[#E4E4E7] text-[#A1A1AA] hover:border-[#3F3F4A]'
                  }`}
                >
                  "{p.merchant} · ₹{p.amount}"
                </button>
              ))}
            </div>
          </div>

          {/* Input Field */}
          <div className="relative">
            <input
              type="text"
              value={inputText}
              onChange={(e) => handleTextChange(e.target.value)}
              placeholder="e.g. Paid ₹350 for lunch at Swiggy via HDFC"
              className="w-full px-4 py-3.5 rounded-xl border border-[#27272E] light:border-[#E4E4E7] bg-[#09090B] light:bg-[#F4F4F5] font-mono-tech text-sm text-[#F4F4F5] light:text-[#09090B] placeholder:text-[#71717A] focus:outline-none focus:border-[#D71920]"
            />
            <span className="absolute right-3.5 top-3.5 text-xs font-mono-tech text-[#71717A]">
              Live Parser
            </span>
          </div>

          {/* Verification Desk Draft Card */}
          <div className="p-6 rounded-2xl bg-[#09090B] light:bg-[#F4F4F5] border border-[#27272E] light:border-[#E4E4E7] space-y-4">
            <div className="flex items-center justify-between border-b border-[#27272E] light:border-[#E4E4E7] pb-3">
              <div className="flex items-center gap-2">
                <span className="dot-indicator-red" />
                <span className="text-xs font-mono-tech font-bold text-[#F4F4F5] light:text-[#09090B]">
                  VERIFICATION DESK // DRAFT DETECTED
                </span>
              </div>
              <span className={`text-[10px] font-mono-tech px-2 py-0.5 rounded ${
                isConfirmed ? 'bg-[#22C55E]/15 text-[#22C55E]' : 'bg-[#D71920]/15 text-[#D71920]'
              }`}>
                {isConfirmed ? 'CONFIRMED & COMMITTED' : 'AWAITING APPROVAL'}
              </span>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs font-mono-tech">
              <div>
                <span className="text-[#71717A] block">PARSED AMOUNT</span>
                <span className="text-lg font-bold text-white light:text-black">
                  ₹{parsed.amount.toFixed(2)}
                </span>
              </div>
              <div>
                <span className="text-[#71717A] block">MERCHANT</span>
                <span className="text-sm font-semibold text-white light:text-black">
                  {parsed.merchant}
                </span>
              </div>
              <div>
                <span className="text-[#71717A] block">CATEGORY</span>
                <span className="text-sm font-semibold text-[#22C55E]">
                  {parsed.category}
                </span>
              </div>
              <div>
                <span className="text-[#71717A] block">ACCOUNT</span>
                <span className="text-sm font-semibold text-white light:text-black">
                  {parsed.bank}
                </span>
              </div>
            </div>

            {/* Action Bar */}
            <div className="pt-2 flex flex-col sm:flex-row items-center justify-between gap-3">
              <span className="text-[11px] font-mono-tech text-[#71717A]">
                Rule 9: Never silently commit ambiguous transactions.
              </span>
              <div className="flex items-center gap-3 w-full sm:w-auto">
                <button
                  onClick={handleReset}
                  className="px-4 py-2.5 rounded-xl text-xs font-mono-tech border border-[#27272E] text-[#A1A1AA] hover:text-white flex items-center justify-center gap-1.5"
                >
                  <RotateCcw className="w-3.5 h-3.5" />
                  <span>Reset</span>
                </button>
                <button
                  onClick={handleConfirm}
                  disabled={isConfirmed}
                  className={`flex-1 sm:flex-none px-6 py-2.5 rounded-xl text-xs font-mono-tech font-bold flex items-center justify-center gap-2 transition-all ${
                    isConfirmed
                      ? 'bg-[#22C55E] text-black cursor-default'
                      : 'bg-[#D71920] hover:bg-[#B8141A] text-white active:scale-95'
                  }`}
                >
                  {isConfirmed ? (
                    <>
                      <Check className="w-4 h-4" />
                      <span>Saved to Local Room DB!</span>
                    </>
                  ) : (
                    <>
                      <span>Confirm &amp; Commit Expense</span>
                      <ArrowRight className="w-4 h-4" />
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>

          {/* Simulated Ledger & Budget Impact Bar */}
          <div className="p-4 rounded-xl border border-[#27272E] light:border-[#E4E4E7] bg-[#09090B] light:bg-[#FAFAFA] flex flex-col sm:flex-row items-center justify-between gap-4 font-mono-tech text-xs">
            <div>
              <span className="text-[#71717A] block">UPDATED MONTHLY OUTFLOW</span>
              <span className="text-base font-bold text-white light:text-black">
                ₹{spentTotal.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              </span>
            </div>
            <div className="w-full sm:w-64">
              <div className="flex justify-between text-[10px] text-[#71717A] mb-1">
                <span>Safe-to-Spend Cap: ₹45,000</span>
                <span>{((spentTotal / 45000) * 100).toFixed(1)}%</span>
              </div>
              <div className="h-2 w-full bg-[#18181C] rounded-full overflow-hidden">
                <div
                  className="h-full bg-[#D71920] transition-all duration-500 rounded-full"
                  style={{ width: `${Math.min(100, (spentTotal / 45000) * 100)}%` }}
                />
              </div>
            </div>
            <div className="text-right">
              <span className="text-[#71717A] block">TOTAL TRANSACTIONS</span>
              <span className="font-bold text-white light:text-black">{ledgerCount} items logged</span>
            </div>
          </div>

          {/* Sandbox Disclaimer */}
          <p className="text-center text-xs text-[#71717A] font-mono-tech">
            ✦ Client-side interactive sandbox demonstration. No financial data is ever collected or sent to a server.
          </p>

        </div>

      </div>
    </section>
  );
};
