import React from "react";
import { FiArrowRight } from "react-icons/fi";
import { ButtonLink } from "../buttons/ButtonLink";
import { ContourLines } from "../map/ContourLines";
import { MaxWidthWrapper } from "../utils/MaxWidthWrapper";
import { REPOSITORY_URL } from "../utils/links";

export const PrivacyBand = () => {
  return (
    <section className="relative overflow-hidden bg-espresso py-20 md:py-24">
      <ContourLines className="text-cream opacity-[0.07]" />
      <MaxWidthWrapper className="relative z-10 flex flex-col items-center text-center">
        <h2 className="mb-4 max-w-2xl font-display text-3xl font-semibold leading-[1.1] tracking-tight text-cream sm:text-4xl md:text-5xl">
          Your real location stays on your phone
        </h2>
        <p className="mb-9 max-w-2xl text-base text-tan md:text-lg">
          No account, no ads, no analytics. The app reads where you are on the
          device only, and the few public map and search services it contacts
          are listed in full in the privacy policy.
        </p>
        <div className="flex flex-col items-center gap-3 sm:flex-row">
          <ButtonLink href="/privacy/" variant="onDark">
            Read the privacy policy
            <FiArrowRight />
          </ButtonLink>
          <ButtonLink
            href={REPOSITORY_URL}
            external
            variant="ghost"
            className="border-tan/50 text-cream hover:bg-cream/10"
          >
            Read the source
          </ButtonLink>
        </div>
      </MaxWidthWrapper>
    </section>
  );
};
