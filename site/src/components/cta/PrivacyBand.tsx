import React from "react";
import { FiArrowRight } from "react-icons/fi";
import { ButtonLink } from "../buttons/ButtonLink";
import { MaxWidthWrapper } from "../utils/MaxWidthWrapper";
import { REPOSITORY_URL } from "../utils/links";

export const PrivacyBand = () => {
  return (
    <section className="relative overflow-hidden border-t border-zinc-700 bg-gradient-to-b from-zinc-900/30 to-zinc-950 py-20">
      <MaxWidthWrapper className="relative z-20 flex flex-col items-center text-center">
        <h2 className="mb-4 max-w-2xl text-3xl font-bold leading-tight text-zinc-50 sm:text-4xl md:text-5xl">
          Your real location stays on your phone
        </h2>
        <p className="mb-9 max-w-2xl text-base text-zinc-400 md:text-lg">
          No account, no ads, no analytics. The app reads where you are on the
          device only, and the few public map and search services it contacts
          are listed in full in the privacy policy.
        </p>
        <div className="flex flex-col items-center gap-4 sm:flex-row">
          <ButtonLink href="/privacy/">
            Read the privacy policy
            <FiArrowRight />
          </ButtonLink>
          <ButtonLink href={REPOSITORY_URL} external variant="ghost">
            Read the source
          </ButtonLink>
        </div>
      </MaxWidthWrapper>
      <div className="absolute inset-0 z-0 bg-grid-zinc-700/50" />
      <div className="absolute inset-0 bg-gradient-to-b from-zinc-900/0 to-zinc-950" />
    </section>
  );
};
