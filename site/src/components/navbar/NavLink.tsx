import Link from "next/link";
import React from "react";
import { FiArrowUpRight } from "react-icons/fi";

export const NavLink = ({
  children,
  external,
  href = "#",
}: {
  children: string;
  external?: boolean;
  href?: string;
}) => {
  return (
    <Link
      href={href}
      target={external ? "_blank" : undefined}
      rel={external ? "noopener noreferrer" : undefined}
      className="group flex items-center gap-0.5 text-xs text-coffee transition-colors hover:text-espresso sm:text-sm"
    >
      <span>{children}</span>
      {external && (
        <FiArrowUpRight className="block transition-transform group-hover:rotate-45" />
      )}
    </Link>
  );
};
