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
          <SectionHeading>Built for testing, done properly</SectionHeading>
          <SectionSubheading>
            Most fake GPS apps join waypoints with a straight line and ignore
            basic physics. RouteForge aims to do it right.
          </SectionSubheading>
        </SectionHeadingSpacing>
        <div className="grid gap-9 sm:grid-cols-2 md:gap-12 lg:grid-cols-3">
          {FEATURES.map(({ Icon, title, text }) => (
            <div key={title}>
              <h3 className="mb-1.5 flex items-start text-lg font-medium md:text-xl">
                <Icon className="mr-1.5 mt-1 h-[22px] w-[22px] shrink-0 text-blue-300" />
                {title}
              </h3>
              <p className="text-sm text-zinc-400 md:text-base">{text}</p>
            </div>
          ))}
        </div>
      </MaxWidthWrapper>
    </section>
  );
};
