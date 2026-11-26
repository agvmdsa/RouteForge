import React from "react";

/** The fields of the Location that RouteForge hands to Android at every step of a simulation (the values here are only an example). */
const FIELDS = [
  { name: "latitude", value: "52.52000", unit: "" },
  { name: "longitude", value: "13.40500", unit: "" },
  { name: "bearing", value: "64.0", unit: "degrees" },
  { name: "speed", value: "13.9", unit: "m/s" },
  { name: "accuracy", value: "5.0", unit: "m" },
];

export const LocationReadout = () => (
  <div className="mt-6">
    <p className="mb-3 text-sm text-coffee">
      What other apps on the phone receive while a route plays, from the{" "}
      <span className="whitespace-nowrap">gps</span> and{" "}
      <span className="whitespace-nowrap">network</span> providers. Example
      values.
    </p>
    <dl className="grid grid-cols-[max-content_1fr_max-content] gap-x-5 gap-y-1 border-t border-tan pt-3 font-mono text-sm tabular-nums text-ink">
      {FIELDS.map(({ name, value, unit }) => (
        <React.Fragment key={name}>
          <dt className="text-coffee">{name}</dt>
          <dd className="text-right">{value}</dd>
          <dd className="text-coffee">{unit}</dd>
        </React.Fragment>
      ))}
    </dl>
  </div>
);
