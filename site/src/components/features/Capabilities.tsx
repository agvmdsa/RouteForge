import React from "react";
import { MaxWidthWrapper } from "../utils/MaxWidthWrapper";

const CAPABILITIES = [
  {
    title: "Edit while it plays",
    text: "Reorder, add, move or delete waypoints during playback. Only the part of the route still ahead of you can be changed.",
  },
  {
    title: "Control it from the notification",
    text: "Pause, resume or stop a simulation without opening the app, including from the lock screen.",
  },
  {
    title: "Search, share, save",
    text: "Look up a place, or open a map link or a GPX file from another app. Add it to a route, teleport to it, or keep it as a favorite.",
  },
  {
    title: "Offline map data you control",
    text: "Download only the regions you need and set the storage limit yourself. Once a region is downloaded, its routes work without a connection.",
  },
];

export const Capabilities = () => {
  return (
    <section className="relative pb-20 md:pb-28">
      <MaxWidthWrapper>
        <h2 className="mb-8 font-display text-2xl font-semibold tracking-tight text-espresso md:text-3xl">
          What else it does
        </h2>
        <dl className="divide-y divide-tan/70 border-y border-tan/70">
          {CAPABILITIES.map(({ title, text }) => (
            <div key={title} className="grid gap-1 py-6 md:grid-cols-[18rem_1fr] md:gap-10">
              <dt className="font-display text-xl font-semibold text-espresso">{title}</dt>
              <dd className="max-w-2xl text-base text-coffee">{text}</dd>
            </div>
          ))}
        </dl>
      </MaxWidthWrapper>
    </section>
  );
};
