import React, { useState } from "react";
import { MaxWidthWrapper } from "../utils/MaxWidthWrapper";
import { NavLogo } from "./NavLogo";
import { NavLinks } from "./NavLinks";
import { useMotionValueEvent, useScroll, motion } from "framer-motion";

export const NavBar = () => {
  const [scrolled, setScrolled] = useState(false);
  const { scrollY } = useScroll();

  useMotionValueEvent(scrollY, "change", (latest) => {
    setScrolled(latest > 4);
  });

  return (
    <motion.nav
      initial={{ opacity: 0, y: "-100%" }}
      animate={{ opacity: 1, y: "0%" }}
      transition={{ duration: 1.25, ease: "easeInOut" }}
      className={`fixed left-0 right-0 top-0 z-50 border-b py-2 transition-colors duration-200 ${scrolled ? "border-tan/60 bg-cream/95 backdrop-blur" : "border-transparent bg-cream/0"}`}
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
