import React from "react";
import { IconType } from "react-icons";
import {
  FiBell,
  FiDownloadCloud,
  FiEdit3,
  FiMap,
  FiNavigation,
  FiSearch,
} from "react-icons/fi";
import { MaxWidthWrapper } from "../utils/MaxWidthWrapper";
import { SectionHeading } from "../utils/SectionHeading";
import { SectionHeadingSpacing } from "../utils/SectionHeadingSpacing";
import { SectionSubheading } from "../utils/SectionSubheading";

const FEATURES: { Icon: IconType; title: string; text: string }[] = [
  {
    Icon: FiMap,
    title: "Real roads, by choice",
    text: "When a route is ready, RouteForge checks whether every leg can be driven along real streets. You pick Guided (follows the roads) or Free-roam (straight lines, for off-road or unrestricted movement).",
  },
  {
    Icon: FiNavigation,
    title: "Physically realistic movement",
    text: "Speed, bearing and accuracy stay consistent with each other, so the simulated position does not look obviously fake. A joystick is there when you want to steer by hand.",
  },
  {
    Icon: FiEdit3,
    title: "Edit while it plays",
    text: "Reorder, add, move or delete waypoints during playback. Only the part of the route still ahead of you can be changed.",
  },
  {
    Icon: FiBell,
    title: "Control it from the notification",
    text: "Pause, resume or stop a simulation without opening the app, including from the lock screen.",
  },
  {
    Icon: FiSearch,
    title: "Search, share, save",
    text: "Look up a place, or open a map link or a GPX file from another app. Add it to a route, teleport to it, or keep it as a favorite. Favorites, saved routes and recent places come back with one tap.",
  },
  {
    Icon: FiDownloadCloud,
    title: "Offline map data you control",
    text: "Download only the regions you need and set the storage limit yourself. Once a region is downloaded, its routes work without a connection.",
  },
];

export const Features = () => {
  return (
    <section id="features" className="relative overflow-hidden py-20 md:py-28">
      <MaxWidthWrapper className="relative z-10">
        <SectionHeadingSpacing>
          <SectionHeading persistCenter>Built for testing, done properly</SectionHeading>
          <SectionSubheading persistCenter>
            Most fake GPS apps join waypoints with a straight line and ignore
            basic physics. RouteForge aims to do it right.
          </SectionSubheading>
        </SectionHeadingSpacing>
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {FEATURES.map(({ Icon, title, text }) => (
            <div
              key={title}
              className="rounded-2xl border border-tan bg-cream-surface p-6 transition-shadow hover:shadow-[0_18px_40px_-24px_rgba(78,47,27,0.5)]"
            >
              <span className="mb-4 flex h-11 w-11 items-center justify-center rounded-full bg-tan/50 text-route">
                <Icon className="h-5 w-5" />
              </span>
              <h3 className="mb-1.5 text-lg font-semibold text-espresso md:text-xl">{title}</h3>
              <p className="text-sm text-coffee md:text-base">{text}</p>
            </div>
          ))}
        </div>
      </MaxWidthWrapper>
    </section>
  );
};
