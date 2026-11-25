import React from "react";

export const SectionSubheading = ({
  children,
  persistCenter = false,
}: {
  children: React.ReactNode;
  persistCenter?: boolean;
}) => {
  return (
    <p
      className={`text-center text-base text-coffee md:text-lg ${persistCenter ? "" : "mx-auto max-w-xl md:mx-0 md:text-start"}`}
    >
      {children}
    </p>
  );
};
