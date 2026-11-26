import React from "react";
import Link from "next/link";
import { MaxWidthWrapper } from "../utils/MaxWidthWrapper";
import { NavLogo } from "../navbar/NavLogo";
import { CONTACT_EMAIL, ISSUES_URL, LICENSE_URL, REPOSITORY_URL } from "../utils/links";

type FooterLink = { title: string; href: string; external?: boolean };

const LINKS: FooterLink[] = [
  { title: "Features", href: "/#features" },
  { title: "Privacy policy", href: "/privacy/" },
  { title: "Source on GitHub", href: REPOSITORY_URL, external: true },
  { title: "License", href: LICENSE_URL, external: true },
  { title: "Report a problem", href: ISSUES_URL, external: true },
  { title: "Contact", href: `mailto:${CONTACT_EMAIL}`, external: true },
];

export const Footer = () => {
  return (
    <footer className="border-t border-tan/60 bg-cream-deep/50 py-12">
      <MaxWidthWrapper className="flex flex-col items-center gap-6 text-center">
        <NavLogo />
        <nav aria-label="Footer">
          <ul className="flex flex-wrap items-center justify-center gap-x-6 gap-y-2 text-sm">
            {LINKS.map((link) => (
              <li key={link.title}>
                <Link
                  href={link.href}
                  target={link.external ? "_blank" : undefined}
                  rel={link.external ? "noopener noreferrer" : undefined}
                  className="text-coffee transition-colors hover:text-espresso hover:underline"
                >
                  {link.title}
                </Link>
              </li>
            ))}
          </ul>
        </nav>
        <p className="max-w-xl text-xs text-coffee">
          © 2026 Anderson Gabriel. Source available under the PolyForm
          Noncommercial License 1.0.0. Map data © OpenStreetMap contributors.
        </p>
      </MaxWidthWrapper>
    </footer>
  );
};
