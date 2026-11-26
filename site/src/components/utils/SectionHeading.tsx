import React from "react";

export const SectionHeading = ({
  children,
  persistCenter = false,
}: {
  children: React.ReactNode;
  persistCenter?: boolean;
}) => {
  return (
    <h2
      className={`text-center font-display text-3xl font-semibold leading-[1.1] tracking-tight text-espresso sm:text-4xl md:text-5xl ${persistCenter ? "" : "md:text-start"}`}
    >
      {children}
    </h2>
  );
};
