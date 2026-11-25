import React from "react";

export const GlowingChip = ({ children }: { children: string }) => {
  return (
    <span className="relative z-10 mb-5 inline-block rounded-full border border-tan-deep bg-cream-surface px-3.5 py-1.5 text-xs font-medium text-coffee md:text-sm">
      {children}
    </span>
  );
};
