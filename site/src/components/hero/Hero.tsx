import { FiArrowRight, FiGithub } from "react-icons/fi";
import { motion } from "framer-motion";
import { ContourLines } from "../map/ContourLines";
import { RouteMap } from "../map/RouteMap";
import { GlowingChip } from "../utils/GlowingChip";
import { MaxWidthWrapper } from "../utils/MaxWidthWrapper";
import { ButtonLink } from "../buttons/ButtonLink";
import { REPOSITORY_URL } from "../utils/links";

const rise = (delay: number) => ({
  initial: { y: 22, opacity: 0 },
  animate: { y: 0, opacity: 1 },
  transition: { duration: 0.9, delay, ease: "easeOut" as const },
});

export const Hero = () => {
  return (
    <section className="relative overflow-hidden">
      <ContourLines className="text-tan opacity-60" />
      <MaxWidthWrapper className="relative z-10 grid items-center gap-12 pb-16 pt-28 md:pb-28 md:pt-36 lg:grid-cols-2 lg:gap-8">
        <div className="text-center lg:text-start">
          <motion.div {...rise(0)}>
            <GlowingChip>Android · no ads, no trackers</GlowingChip>
          </motion.div>
          <motion.h1
            {...rise(0.15)}
            className="mb-4 text-4xl font-bold leading-tight text-espresso sm:text-5xl sm:leading-tight lg:text-6xl lg:leading-tight"
          >
            Simulate your location along real roads
          </motion.h1>
          <motion.p
            {...rise(0.3)}
            className="mx-auto mb-8 max-w-xl text-base text-coffee sm:text-lg lg:mx-0"
          >
            RouteForge makes your phone report a simulated location that
            follows real streets, with routes computed fully offline, on the
            device. No server, no API key, no account.
          </motion.p>
          <motion.div
            {...rise(0.45)}
            className="flex flex-col items-center gap-3 sm:flex-row lg:justify-start sm:justify-center"
          >
            <ButtonLink href={REPOSITORY_URL} external>
              <FiGithub />
              View the source
            </ButtonLink>
            <ButtonLink href="/#features" variant="ghost">
              What it does
              <FiArrowRight />
            </ButtonLink>
          </motion.div>
          <motion.p {...rise(0.6)} className="mt-6 text-sm text-coffee/70">
            Early stage. Not on Google Play yet.
          </motion.p>
        </div>
        <motion.div {...rise(0.3)}>
          <RouteMap />
        </motion.div>
      </MaxWidthWrapper>
    </section>
  );
};
