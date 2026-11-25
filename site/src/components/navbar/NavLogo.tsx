import Image from "next/image";
import Link from "next/link";
import React from "react";

export const NavLogo = () => {
  return (
    <Link href="/" className="flex items-center gap-2">
      <Image src="/logo.svg" alt="" width={28} height={28} unoptimized />
      <span className="text-lg font-semibold text-zinc-50">RouteForge</span>
    </Link>
  );
};
