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
      className="group flex min-h-11 items-center gap-0.5 px-1 text-sm text-coffee transition-colors hover:text-espresso"
    >
      <span>{children}</span>
      {external && (
        <FiArrowUpRight className="block transition-transform group-hover:rotate-45" />
      )}
    </Link>
  );
};
