import React, { useState } from "react";
import { MaxWidthWrapper } from "../utils/MaxWidthWrapper";
import { NavLogo } from "./NavLogo";
import { NavLinks } from "./NavLinks";
import { useMotionValueEvent, useScroll, motion } from "framer-motion";

export const NavBar = () => {
  const [scrolled, setScrolled] = useState(false);
  const { scrollY } = useScroll();

  useMotionValueEvent(scrollY, "change", (latest) => {
    setScrolled(latest > 150);
  });

  return (
    <motion.nav
      initial={{ opacity: 0, y: "-100%" }}
      animate={{ opacity: 1, y: "0%" }}
      transition={{ duration: 1.25, ease: "easeInOut" }}
      className={`fixed left-0 right-0 top-0 z-50 bg-zinc-950/0 py-3 transition-colors ${scrolled && "bg-zinc-950/80 backdrop-blur"}`}
    >
      <MaxWidthWrapper>
        <div className="flex items-center justify-between">
          <NavLogo />
          <NavLinks />
        </div>
      </MaxWidthWrapper>
    </motion.nav>
  );
};
