import React from "react";
import { RouteMap, RouteMapMode } from "../map/RouteMap";
import { MaxWidthWrapper } from "../utils/MaxWidthWrapper";

const MODES: { mode: RouteMapMode; name: string; text: string }[] = [
  {
    mode: "guided",
    name: "Guided",
    text: "Follows the roads between your points, so the movement looks like a real drive.",
  },
  {
    mode: "free-roam",
    name: "Free-roam",
    text: "Goes straight through your points: for trails, fields and anywhere a road is not the point.",
  },
];

export const Modes = () => {
  return (
    <section id="features" className="relative scroll-mt-16 py-20 md:py-28">
      <MaxWidthWrapper>
        <div className="mb-12 max-w-2xl md:mb-14">
          <h2 className="font-display text-3xl font-semibold leading-[1.1] tracking-tight text-espresso sm:text-4xl md:text-5xl">
            Follow the streets, or go straight
          </h2>
          <p className="mt-4 max-w-xl text-base text-coffee md:text-lg">
            When a route is ready, RouteForge checks whether every leg can be
            driven along real roads. If it can, you choose how the route plays.
            If it cannot, only Free-roam is offered, never a broken option.
          </p>
        </div>
        <div className="grid gap-10 md:grid-cols-2 md:gap-8">
          {MODES.map(({ mode, name, text }) => (
            <figure key={mode}>
              <RouteMap mode={mode} />
              <figcaption className="mt-4">
                <span className="font-display text-xl font-semibold text-espresso">{name}</span>
                <span className="mt-1 block max-w-md text-sm text-coffee md:text-base">{text}</span>
              </figcaption>
            </figure>
          ))}
        </div>
      </MaxWidthWrapper>
    </section>
  );
};
