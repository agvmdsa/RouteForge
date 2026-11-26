import React from "react";
import { FiArrowRight } from "react-icons/fi";
import { ButtonLink } from "../buttons/ButtonLink";
import { ContourLines } from "../map/ContourLines";
import { MaxWidthWrapper } from "../utils/MaxWidthWrapper";
import { REPOSITORY_URL } from "../utils/links";

/** Every host the app contacts, as listed in the privacy policy. */
const SERVICES = [
  { host: "tile.openstreetmap.org", use: "Map pictures for the area on screen (Standard style)" },
  { host: "tile.opentopomap.org", use: "Map pictures for the Topographic style" },
  { host: "nominatim.openstreetmap.org", use: "Place search, and the address of a spot you chose" },
  { host: "brouter.de", use: "Routing data of the regions you download" },
  { host: "maps.app.goo.gl", use: "Only when you share a short Google Maps link to the app" },
];

export const PrivacyBand = () => {
  return (
    <section className="relative overflow-hidden bg-espresso py-20 md:py-24">
      <ContourLines className="text-cream opacity-[0.07]" />
      <MaxWidthWrapper className="relative z-10 grid gap-12 lg:grid-cols-[1fr_1.1fr] lg:gap-20">
        <div>
          <h2 className="font-display text-3xl font-semibold leading-[1.1] tracking-tight text-cream sm:text-4xl md:text-5xl">
            Your real location stays on your phone
          </h2>
          <p className="mt-5 max-w-lg text-base text-tan md:text-lg">
            No account, no ads, no analytics. The app reads where you are on
            the device only. These five services are everything it ever talks
            to.
          </p>
          <div className="mt-8 flex flex-col items-start gap-3 sm:flex-row">
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
        </div>
        <dl className="divide-y divide-cream/15 self-center border-y border-cream/15">
          {SERVICES.map(({ host, use }) => (
            <div key={host} className="grid gap-1 py-3.5 sm:grid-cols-[15.5rem_1fr] sm:gap-6">
              <dt className="font-mono text-sm text-cream">{host}</dt>
              <dd className="text-sm text-tan">{use}</dd>
            </div>
          ))}
        </dl>
      </MaxWidthWrapper>
    </section>
  );
};
