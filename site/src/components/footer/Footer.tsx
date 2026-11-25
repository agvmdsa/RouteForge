import React from "react";
import Link from "next/link";
import { MaxWidthWrapper } from "../utils/MaxWidthWrapper";
import { NavLogo } from "../navbar/NavLogo";
import { CONTACT_EMAIL, ISSUES_URL, LICENSE_URL, REPOSITORY_URL } from "../utils/links";

type FooterLink = { title: string; href: string; external?: boolean };

export const Footer = () => {
  return (
    <footer className="relative overflow-hidden border-t border-zinc-800 py-12">
      <MaxWidthWrapper className="relative z-20 grid grid-cols-12 gap-x-1.5 gap-y-6">
        <div className="col-span-12 md:col-span-6">
          <NavLogo />
          <p className="mt-3 max-w-sm text-xs text-zinc-400">
            © 2026 Anderson Gabriel. Source available under the PolyForm
            Noncommercial License 1.0.0. Map data © OpenStreetMap contributors.
          </p>
        </div>
        <Column
          title="RouteForge"
          links={[
            { title: "Features", href: "/#features" },
            { title: "Privacy policy", href: "/privacy/" },
          ]}
        />
        <Column
          title="Project"
          links={[
            { title: "Source on GitHub", href: REPOSITORY_URL, external: true },
            { title: "License", href: LICENSE_URL, external: true },
            { title: "Report a problem", href: ISSUES_URL, external: true },
            { title: "Contact", href: `mailto:${CONTACT_EMAIL}`, external: true },
          ]}
        />
      </MaxWidthWrapper>
    </footer>
  );
};

const Column = ({ title, links }: { title: string; links: FooterLink[] }) => (
  <div className="col-span-6 space-y-2 text-sm md:col-span-3">
    <span className="block text-zinc-50">{title}</span>
    {links.map((link) => (
      <Link
        key={link.title}
        href={link.href}
        target={link.external ? "_blank" : undefined}
        rel={link.external ? "noopener noreferrer" : undefined}
        className="block text-zinc-400 transition-colors hover:text-zinc-200 hover:underline"
      >
        {link.title}
      </Link>
    ))}
  </div>
);
