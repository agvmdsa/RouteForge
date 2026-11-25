import React from "react";
import { NavLink } from "./NavLink";
import { REPOSITORY_URL } from "../utils/links";

export const NavLinks = () => {
  return (
    <ul className="flex gap-3 text-coffee md:gap-9">
      <li>
        <NavLink href="/#features">Features</NavLink>
      </li>
      <li>
        <NavLink href="/privacy/">Privacy</NavLink>
      </li>
      <li>
        <NavLink href={REPOSITORY_URL} external>
          Source
        </NavLink>
      </li>
    </ul>
  );
};
