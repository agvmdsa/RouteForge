import React, { ReactNode } from "react";
import { twMerge } from "tailwind-merge";

type Props = {
  children: ReactNode;
  href: string;
  variant?: "splash" | "ghost";
  external?: boolean;
  className?: string;
};

const VARIANTS = {
  splash:
    "rounded-md bg-gradient-to-br from-blue-400 to-blue-700 px-4 py-2 text-lg text-zinc-50 ring-2 ring-blue-500/50 ring-offset-2 ring-offset-zinc-950 transition-all hover:scale-[1.02] hover:ring-transparent active:scale-[0.98] active:ring-blue-500/70",
  ghost:
    "rounded-md px-4 py-2 text-lg text-zinc-100 transition-all hover:scale-[1.02] hover:bg-zinc-800 hover:text-zinc-50 active:scale-[0.98]",
};

export const ButtonLink = ({
  children,
  href,
  variant = "splash",
  external,
  className,
}: Props) => (
  <a
    href={href}
    target={external ? "_blank" : undefined}
    rel={external ? "noopener noreferrer" : undefined}
    className={twMerge("inline-flex items-center gap-2", VARIANTS[variant], className)}
  >
    {children}
  </a>
);
