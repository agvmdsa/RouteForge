import React from "react";

const RING_COUNT = 13;

/** A closed, slightly irregular loop around ([cx], [cy]): the same shape a contour line of a hill takes on a topographic map. */
const contour = (cx: number, cy: number, radius: number, phase: number) => {
  const samples = 56;
  const points: string[] = [];
  for (let k = 0; k < samples; k++) {
    const t = (k / samples) * Math.PI * 2;
    const wobble = 1 + 0.12 * Math.sin(3 * t + phase) + 0.07 * Math.sin(5 * t + phase * 1.7);
    const x = cx + radius * wobble * Math.cos(t);
    const y = cy + radius * 0.78 * wobble * Math.sin(t);
    points.push(`${x.toFixed(1)} ${y.toFixed(1)}`);
  }
  return `M${points.join(" L")} Z`;
};

const HILLS = [
  { cx: 1010, cy: 190, phase: 0.6, step: 46 },
  { cx: 170, cy: 640, phase: 2.1, step: 40 },
];

export const ContourLines = ({ className = "" }: { className?: string }) => (
  <svg
    aria-hidden
    viewBox="0 0 1200 800"
    preserveAspectRatio="xMidYMid slice"
    className={`pointer-events-none absolute inset-0 h-full w-full ${className}`}
    fill="none"
  >
    {HILLS.map((hill) =>
      Array.from({ length: RING_COUNT }, (_, i) => (
        <path
          key={`${hill.cx}-${i}`}
          d={contour(hill.cx, hill.cy, 30 + i * hill.step, hill.phase + i * 0.05)}
          stroke="currentColor"
          strokeWidth={i % 4 === 0 ? 1.6 : 0.9}
        />
      ))
    )}
  </svg>
);
