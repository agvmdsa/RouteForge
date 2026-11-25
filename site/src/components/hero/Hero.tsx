import { FiArrowRight, FiGithub } from "react-icons/fi";
import { motion } from "framer-motion";
import { Beams } from "../utils/Beams";
import { GlowingChip } from "../utils/GlowingChip";
import { MaxWidthWrapper } from "../utils/MaxWidthWrapper";
import { ButtonLink } from "../buttons/ButtonLink";
import { REPOSITORY_URL } from "../utils/links";
import { GradientGrid } from "./GradientGrid";

const rise = (delay: number) => ({
  initial: { y: 25, opacity: 0 },
  animate: { y: 0, opacity: 1 },
  transition: { duration: 1.25, delay, ease: "easeInOut" as const },
});

export const Hero = () => {
  return (
    <section className="relative overflow-hidden">
      <MaxWidthWrapper className="relative z-20 flex flex-col items-center justify-center pb-12 pt-24 md:pb-36 md:pt-36">
        <motion.div {...rise(0)} className="relative">
          <GlowingChip>Android · no ads, no trackers</GlowingChip>
        </motion.div>
        <motion.h1
          {...rise(0.25)}
          className="mb-3 text-center text-3xl font-bold leading-tight text-zinc-50 sm:text-4xl sm:leading-tight md:text-5xl md:leading-tight lg:text-7xl lg:leading-tight"
        >
          Simulate your location along real roads
        </motion.h1>
        <motion.p
          {...rise(0.5)}
          className="mb-9 max-w-2xl text-center text-base text-zinc-400 sm:text-lg md:text-xl"
        >
          RouteForge makes your phone report a simulated location that follows
          real streets, with routes computed fully offline, on the device. No
          server, no API key, no account.
        </motion.p>
        <motion.div
          {...rise(0.75)}
          className="flex flex-col items-center gap-4 sm:flex-row"
        >
          <ButtonLink href={REPOSITORY_URL} external>
            <FiGithub />
            View the source
          </ButtonLink>
          <ButtonLink href="/#features" variant="ghost" className="text-zinc-100">
            What it does
            <FiArrowRight />
          </ButtonLink>
        </motion.div>
        <motion.p {...rise(1)} className="mt-6 text-center text-sm text-zinc-500">
          Early stage. Not on Google Play yet.
        </motion.p>
      </MaxWidthWrapper>
      <Beams />
      <GradientGrid />
    </section>
  );
};
