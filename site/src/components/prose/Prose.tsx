import React from "react";

export const Prose = ({ children }: { children: React.ReactNode }) => (
  <div className="space-y-4 leading-relaxed text-ink/90 [&_a]:text-route [&_a]:underline [&_code]:rounded [&_code]:bg-tan/40 [&_code]:px-1 [&_code]:text-sm [&_h2]:mt-10 [&_h2]:font-display [&_h2]:text-2xl [&_h2]:font-semibold [&_h2]:tracking-tight [&_h2]:text-espresso [&_h3]:mt-6 [&_h3]:text-lg [&_h3]:font-medium [&_h3]:text-coffee [&_ul]:list-disc [&_ul]:space-y-2 [&_ul]:pl-6">
    {children}
  </div>
);
