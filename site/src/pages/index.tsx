import Head from "next/head";
import { PrivacyBand } from "@/components/cta/PrivacyBand";
import { Features } from "@/components/features/Features";
import { Hero } from "@/components/hero/Hero";

export default function Home() {
  return (
    <>
      <Head>
        <title>RouteForge: simulate your location along real roads</title>
        <meta
          name="description"
          content="RouteForge is an Android app that simulates the device location along real roads, with routes computed offline on the phone. No account, no ads, no analytics."
        />
      </Head>
      <main>
        <Hero />
        <Features />
        <PrivacyBand />
      </main>
    </>
  );
}
