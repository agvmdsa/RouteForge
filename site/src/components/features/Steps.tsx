import React from "react";
import { MaxWidthWrapper } from "../utils/MaxWidthWrapper";
import { SectionHeading } from "../utils/SectionHeading";
import { SectionHeadingSpacing } from "../utils/SectionHeadingSpacing";

const STEPS = [
  {
    title: "Tap the map",
    text: "Add waypoints by tapping, or search for a place, or open a map link or a GPX file from another app.",
  },
  {
    title: "Choose how it moves",
    text: "If the whole route can be driven, pick Guided to follow the roads or Free-roam to go straight. If not, only Free-roam is offered.",
  },
  {
    title: "Press play",
    text: "Set the speed and how often it repeats. Pause, resume or stop from the notification while other apps read the simulated position.",
  },
];

export const Steps = () => {
  return (
    <section className="relative border-y border-tan/60 bg-cream-deep/60 py-20 md:py-24">
      <MaxWidthWrapper>
        <SectionHeadingSpacing>
          <SectionHeading persistCenter>From the first tap to a moving pin</SectionHeading>
        </SectionHeadingSpacing>
        <ol className="relative grid gap-10 md:grid-cols-3 md:gap-8">
          <div
            aria-hidden
            className="absolute left-[16.6%] right-[16.6%] top-5 hidden border-t-2 border-dashed border-route/50 md:block"
          />
          {STEPS.map((step, i) => (
            <li key={step.title} className="relative text-center">
              <span className="relative z-10 mx-auto mb-4 flex h-10 w-10 items-center justify-center rounded-full bg-espresso text-base font-semibold text-cream ring-4 ring-cream-deep">
                {i + 1}
              </span>
              <h3 className="mb-1.5 text-lg font-semibold text-espresso md:text-xl">{step.title}</h3>
              <p className="mx-auto max-w-xs text-sm text-coffee md:text-base">{step.text}</p>
            </li>
          ))}
        </ol>
      </MaxWidthWrapper>
    </section>
  );
};
