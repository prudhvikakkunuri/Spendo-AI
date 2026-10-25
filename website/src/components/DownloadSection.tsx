import { Download, AlertTriangle, ExternalLink } from 'lucide-react';

export const DownloadSection: React.FC = () => {
  return (
    <section id="download" className="py-24 border-t border-[#27272E] light:border-[#E4E4E7] bg-[#09090B] light:bg-[#FAFAFA] relative overflow-hidden bg-dot-grid">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        
        {/* Main Card */}
        <div className="p-8 sm:p-14 rounded-3xl border-2 border-[#27272E] light:border-[#E4E4E7] bg-[#121215] light:bg-[#FFFFFF] shadow-2xl space-y-10 text-center sm:text-left">
          
          <div className="flex flex-col sm:flex-row items-center justify-between gap-6">
            <div className="space-y-3">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-[#27272E] light:border-[#E4E4E7] bg-[#18181C] light:bg-[#F4F4F5]">
                <span className="dot-indicator-green" />
                <span className="font-mono-tech text-xs tracking-wider uppercase text-[#A1A1AA] light:text-[#71717A]">
                  OFFICIAL APK RELEASE
                </span>
              </div>
              <h2 className="text-3xl sm:text-5xl font-bold tracking-tight text-[#F4F4F5] light:text-[#09090B]">
                Get Spendo for Android
              </h2>
              <p className="text-sm sm:text-base text-[#A1A1AA] light:text-[#52525B] max-w-xl">
                Install directly on your phone. Completely free, no advertisements, and 100% on-device data retention.
              </p>
            </div>

            {/* Release Info Pill */}
            <div className="p-4 rounded-2xl border border-[#27272E] light:border-[#E4E4E7] bg-[#09090B] light:bg-[#F4F4F5] font-mono-tech text-xs text-left min-w-[200px] space-y-1.5">
              <div className="flex justify-between">
                <span className="text-[#71717A]">VERSION:</span>
                <span className="font-bold text-white light:text-black">v0.1.0 dev</span>
              </div>
              <div className="flex justify-between">
                <span className="text-[#71717A]">TARGET:</span>
                <span className="text-white light:text-black">Android 8.0+</span>
              </div>
              <div className="flex justify-between">
                <span className="text-[#71717A]">PACKAGE:</span>
                <span className="text-white light:text-black">spendo-ai.apk</span>
              </div>
              <div className="flex justify-between">
                <span className="text-[#71717A]">SIZE:</span>
                <span className="text-white light:text-black">~17.3 MB</span>
              </div>
            </div>
          </div>

          {/* Action CTAs */}
          <div className="flex flex-col sm:flex-row items-center gap-4 pt-2 border-t border-[#1E1E24] light:border-[#E4E4E7]">
            <a
              href="https://github.com/prudhvikakkunuri/Spendo-AI/releases/download/dev-latest/spendo-ai.apk"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-3 px-8 py-4 rounded-xl font-mono-tech font-bold text-sm bg-[#D71920] hover:bg-[#B8141A] text-white shadow-xl shadow-[#D71920]/25 transition-all active:scale-[0.98]"
            >
              <Download className="w-4 h-4" />
              <span>Download spendo-ai.apk</span>
            </a>

            <a
              href="https://github.com/prudhvikakkunuri/Spendo-AI/releases"
              target="_blank"
              rel="noopener noreferrer"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-4 rounded-xl font-mono-tech text-sm font-semibold border border-[#27272E] light:border-[#E4E4E7] bg-[#18181C] light:bg-[#F4F4F5] text-[#F4F4F5] light:text-[#09090B] hover:border-[#3F3F4A] transition-all"
            >
              <span>Browse All Releases &amp; Notes</span>
              <ExternalLink className="w-3.5 h-3.5 text-[#71717A]" />
            </a>
          </div>

          {/* Android Installation Guidance (Honest Security Context) */}
          <div className="p-5 rounded-2xl border border-[#27272E] light:border-[#E4E4E7] bg-[#09090B] light:bg-[#F4F4F5] space-y-3 text-xs">
            <div className="flex items-center gap-2 font-mono-tech font-bold text-[#F4F4F5] light:text-[#09090B]">
              <AlertTriangle className="w-4 h-4 text-[#EAB308]" />
              <span>Installation Note (Android Sideloading)</span>
            </div>
            <ol className="list-decimal list-inside space-y-1.5 text-[#A1A1AA] light:text-[#52525B] leading-relaxed">
              <li>Download <code>spendo-ai.apk</code> above directly to your Android device.</li>
              <li>When opening the file, Android will prompt: <em>"For your security, your phone is not allowed to install unknown apps from this source."</em></li>
              <li>Tap <strong>Settings</strong> and enable <strong>"Allow from this source"</strong> for your browser or file manager.</li>
              <li>Tap <strong>Install</strong>. Once launched, review the permission guide and select the banking apps you wish to monitor.</li>
            </ol>
          </div>

        </div>

      </div>
    </section>
  );
};
