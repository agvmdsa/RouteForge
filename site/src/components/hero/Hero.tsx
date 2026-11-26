import { FiArrowRight, FiGithub } from "react-icons/fi";
import { motion } from "framer-motion";
import { ContourLines } from "../map/ContourLines";
import { RouteMap } from "../map/RouteMap";
import { MaxWidthWrapper } from "../utils/MaxWidthWrapper";
import { ButtonLink } from "../buttons/ButtonLink";
import { REPOSITORY_URL } from "../utils/links";
import { LocationReadout } from "./LocationReadout";

const rise = (delay: number) => ({
  initial: { y: 22, opacity: 0 },
  animate: { y: 0, opacity: 1 },
  transition: { duration: 0.9, delay, ease: "easeOut" as const },
});

export const Hero = () => {
  return (
    <section className="relative overflow-hidden">
      <ContourLines className="text-tan opacity-60" />
      <MaxWidthWrapper className="relative z-10 grid items-start gap-12 pb-16 pt-28 md:pb-28 md:pt-36 lg:grid-cols-[1fr_1.05fr] lg:gap-14">
        <div>
          <motion.h1
            {...rise(0)}
            className="mb-5 font-display text-4xl font-semibold leading-[1.08] tracking-tight text-espresso sm:text-5xl lg:text-6xl"
          >
            Simulate your location along real roads
          </motion.h1>
          <motion.p {...rise(0.15)} className="mb-8 max-w-xl text-base text-coffee sm:text-lg">
            RouteForge makes your phone report a simulated location that
            follows real streets, with routes computed fully offline, on the
            device. No server, no API key, no account.
          </motion.p>
          <motion.div {...rise(0.3)} className="flex flex-col items-start gap-3 sm:flex-row">
            <ButtonLink href={REPOSITORY_URL} external>
              <FiGithub />
              View the source
            </ButtonLink>
            <ButtonLink href="/#features" variant="ghost">
              How it moves
              <FiArrowRight />
            </ButtonLink>
          </motion.div>
          <motion.p {...rise(0.45)} className="mt-6 text-sm text-coffee">
            Android. Early stage, not on Google Play yet.
          </motion.p>
        </div>
        <motion.div {...rise(0.2)}>
          <RouteMap animated />
          <LocationReadout />
        </motion.div>
      </MaxWidthWrapper>
    </section>
  );
};
