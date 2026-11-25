import React, { ReactNode } from "react";
import { twMerge } from "tailwind-merge";

type Props = {
  children: ReactNode;
  href: string;
  variant?: "solid" | "ghost" | "onDark";
  external?: boolean;
  className?: string;
};

const VARIANTS = {
  solid:
    "bg-espresso text-cream shadow-sm hover:bg-coffee focus-visible:outline-espresso",
  ghost:
    "border border-tan-deep text-espresso hover:bg-tan/40 focus-visible:outline-espresso",
  onDark:
    "bg-cream text-espresso hover:bg-cream-surface focus-visible:outline-cream",
};

export const ButtonLink = ({
  children,
  href,
  variant = "solid",
  external,
  className,
}: Props) => (
  <a
    href={href}
    target={external ? "_blank" : undefined}
    rel={external ? "noopener noreferrer" : undefined}
    className={twMerge(
      "inline-flex items-center gap-2 rounded-full px-5 py-2.5 text-base font-medium transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2",
      VARIANTS[variant],
      className
    )}
  >
    {children}
  </a>
);
