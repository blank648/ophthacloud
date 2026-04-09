import React from 'react';

interface OphthaLogoProps {
  size?: number;
  showText?: boolean;
  className?: string;
}

const OphthaLogo: React.FC<OphthaLogoProps> = ({ size = 32, showText = true, className = '' }) => (
  <div className={`flex items-center gap-2 ${className}`}>
    <svg width={size} height={size} viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
      {/* Eye iris shape merged with cloud */}
      <ellipse cx="32" cy="32" rx="28" ry="20" stroke="hsl(197, 60%, 69%)" strokeWidth="2" fill="none" opacity="0.4" />
      <ellipse cx="32" cy="32" rx="20" ry="14" stroke="hsl(197, 60%, 69%)" strokeWidth="2" fill="none" opacity="0.6" />
      <ellipse cx="32" cy="32" rx="12" ry="9" stroke="hsl(197, 60%, 69%)" strokeWidth="2.5" fill="hsl(197, 73%, 34%)" fillOpacity="0.2" />
      <circle cx="32" cy="32" r="5" fill="hsl(197, 60%, 69%)" />
      <circle cx="32" cy="30" r="1.5" fill="white" opacity="0.8" />
      {/* Cloud accent */}
      <path d="M18 38c-2 0-4 1.5-4 3.5S16 45 18 45h28c2.5 0 4.5-2 4.5-4.5 0-2-1.5-3.8-3.5-4.2" stroke="hsl(197, 60%, 69%)" strokeWidth="1.5" fill="none" opacity="0.3" />
    </svg>
    {showText && (
      <span className="text-primary-300 font-semibold text-[15px] tracking-tight">
        OphthaCloud
      </span>
    )}
  </div>
);

export default OphthaLogo;
