import React from "react";
import { MaxWidthWrapper } from "../utils/MaxWidthWrapper";

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
    <section className="relative border-y border-tan/60 bg-cream-deep/60 py-24 md:py-32">
      <MaxWidthWrapper className="grid gap-10 md:grid-cols-[minmax(0,22rem)_1fr] md:gap-20">
        <h2 className="font-display text-3xl font-semibold leading-[1.1] tracking-tight text-espresso sm:text-4xl md:sticky md:top-28 md:self-start md:text-5xl">
          From the first tap to a moving pin
        </h2>
        <ol className="space-y-10">
          {STEPS.map((step, i) => (
            <li key={step.title} className="grid grid-cols-[3.5rem_1fr] items-baseline gap-x-4">
              <span aria-hidden className="font-display text-5xl font-semibold leading-none text-route">
                {i + 1}
              </span>
              <div>
                <h3 className="font-display text-xl font-semibold text-espresso md:text-2xl">{step.title}</h3>
                <p className="mt-1.5 max-w-xl text-base text-coffee">{step.text}</p>
              </div>
            </li>
          ))}
        </ol>
      </MaxWidthWrapper>
    </section>
  );
};
