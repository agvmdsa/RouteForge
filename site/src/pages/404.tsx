import Head from "next/head";
import { MaxWidthWrapper } from "@/components/utils/MaxWidthWrapper";
import { ButtonLink } from "@/components/buttons/ButtonLink";

export default function NotFound() {
  return (
    <>
      <Head>
        <title>Page not found · RouteForge</title>
      </Head>
      <main>
        <MaxWidthWrapper className="max-w-3xl pb-24 pt-36">
          <h1 className="font-display text-4xl font-semibold tracking-tight text-espresso md:text-5xl">
            This road leads nowhere
          </h1>
          <p className="mb-8 mt-4 max-w-lg text-lg text-coffee">
            There is no page at this address. The home page and the privacy
            policy are the two pages of this site.
          </p>
          <div className="flex flex-col items-start gap-3 sm:flex-row">
            <ButtonLink href="/">Back to the home page</ButtonLink>
            <ButtonLink href="/privacy/" variant="ghost">
              Privacy policy
            </ButtonLink>
          </div>
        </MaxWidthWrapper>
      </main>
    </>
  );
}
