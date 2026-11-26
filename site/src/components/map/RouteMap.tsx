import React, { useId } from "react";
import { motion, useReducedMotion } from "framer-motion";

const WAYPOINTS = [
  { x: 70, y: 350, label: "A" },
  { x: 190, y: 238, label: "B" },
  { x: 340, y: 212, label: "C" },
  { x: 468, y: 78, label: "D" },
];

const FREE_ROAM_ROUTE = WAYPOINTS.map((p, i) => `${i === 0 ? "M" : "L"}${p.x} ${p.y}`).join(" ");

/**
 * The road the Guided route follows: it leaves each waypoint and comes back to the next one by a winding way, bulging to one
 * side and then the other (the sine terms are zero at both waypoints, so the route still passes through every one of them).
 * Sampled at fixed steps with rounded numbers, so the server and the browser draw the same path.
 */
const windingRoute = (points: { x: number; y: number }[]) => {
  const steps = 28;
  const coordinates: string[] = [];
  points.slice(0, -1).forEach((from, i) => {
    const to = points[i + 1];
    const dx = to.x - from.x;
    const dy = to.y - from.y;
    const length = Math.hypot(dx, dy);
    const normal = { x: -dy / length, y: dx / length };
    const side = i % 2 === 0 ? 1 : -1;
    for (let k = i === 0 ? 0 : 1; k <= steps; k++) {
      const u = k / steps;
      const bulge = side * length * 0.3 * (Math.sin(Math.PI * u) + 0.4 * Math.sin(3 * Math.PI * u));
      coordinates.push(`${(from.x + dx * u + normal.x * bulge).toFixed(1)} ${(from.y + dy * u + normal.y * bulge).toFixed(1)}`);
    }
  });
  return `M${coordinates.join(" L")}`;
};

const GUIDED_ROUTE = windingRoute(WAYPOINTS);

const ROADS = [
  "M20 300 C 120 300, 180 330, 260 310 S 400 260, 500 285",
  "M130 400 C 150 330, 190 280, 250 200 S 300 90, 330 20",
  "M30 130 C 130 150, 210 120, 290 150 S 420 190, 500 160",
  "M390 410 C 380 340, 400 280, 440 230 S 500 190, 505 150",
];

export type RouteMapMode = "guided" | "free-roam";

const DESCRIPTIONS: Record<RouteMapMode, string> = {
  guided: "A route that follows the roads between four waypoints",
  "free-roam": "A route that goes in straight lines between the same four waypoints, across the river and the park",
};

const MapCanvas = ({ mode, animated }: { mode: RouteMapMode; animated: boolean }) => {
  const reduceMotion = useReducedMotion();
  const moving = animated && !reduceMotion;
  const uid = useId().replace(/:/g, "");
  const routeId = `route-${uid}`;
  const gridId = `grid-${uid}`;
  const route = mode === "guided" ? GUIDED_ROUTE : FREE_ROAM_ROUTE;

  return (
    <svg viewBox="0 0 520 420" className="block w-full" role="img" aria-label={DESCRIPTIONS[mode]}>
      <defs>
        <pattern id={gridId} width="40" height="40" patternUnits="userSpaceOnUse">
          <path d="M40 0H0V40" fill="none" stroke="#DCC7A9" strokeOpacity="0.35" strokeWidth="1" />
        </pattern>
        <path id={routeId} d={route} />
      </defs>

      <rect width="520" height="420" fill="#FBF5EC" />
      <rect width="520" height="420" fill={`url(#${gridId})`} />
      <path d="M-10 60 C 90 20, 130 120, 70 190 C 20 250, 60 300, -10 330 L-10 60 Z" fill="#E5E7CF" />
      <path d="M360 440 C 380 380, 450 380, 530 350 L530 440 Z" fill="#E5E7CF" />
      <path d="M0 215 C 90 230, 140 190, 220 215 S 330 300, 400 270 S 480 240, 530 250" fill="none" stroke="#CFDAD2" strokeWidth="16" strokeLinecap="round" />

      {ROADS.map((d) => (
        <g key={d} fill="none" strokeLinecap="round">
          <path d={d} stroke="#F6ECDD" strokeWidth="11" />
          <path d={d} stroke="#E8D8BC" strokeWidth="7" />
        </g>
      ))}
      <g fill="none" strokeLinecap="round">
        <path d={GUIDED_ROUTE} stroke="#F6ECDD" strokeWidth="13" />
        <path d={GUIDED_ROUTE} stroke="#E8D8BC" strokeWidth="9" />
      </g>

      <path
        d={route}
        pathLength={1}
        fill="none"
        stroke="#C1521C"
        strokeWidth="5"
        strokeLinecap="round"
        strokeLinejoin="round"
        className={moving ? "route-draw" : undefined}
      />

      {WAYPOINTS.map((p, i) => {
        const pin = (
          <>
            <circle cx={p.x} cy={p.y} r="11" fill="#4E2F1B" stroke="#FBF5EC" strokeWidth="3" />
            <text x={p.x} y={p.y + 4} textAnchor="middle" fontSize="11" fontWeight="700" fill="#F6ECDD">
              {p.label}
            </text>
          </>
        );
        return moving ? (
          <motion.g
            key={p.label}
            initial={{ opacity: 0, y: -8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.4 + i * 0.7 }}
          >
            {pin}
          </motion.g>
        ) : (
          <g key={p.label}>{pin}</g>
        );
      })}

      {moving && (
        <g>
          <circle r="15" fill="#C1521C" opacity="0.22">
            <animateMotion dur="9s" begin="3.4s" repeatCount="2" fill="freeze" rotate="auto">
              <mpath href={`#${routeId}`} />
            </animateMotion>
          </circle>
          <circle r="7" fill="#C1521C" stroke="#FBF5EC" strokeWidth="3">
            <animateMotion dur="9s" begin="3.4s" repeatCount="2" fill="freeze" rotate="auto">
              <mpath href={`#${routeId}`} />
            </animateMotion>
          </circle>
        </g>
      )}
    </svg>
  );
};

/** A stylised map of one route. The animated one plays once or twice and then rests; the static one shows the finished route. */
export const RouteMap = ({
  mode = "guided",
  animated = false,
  className = "",
}: {
  mode?: RouteMapMode;
  animated?: boolean;
  className?: string;
}) => (
  <div
    className={`overflow-hidden rounded-3xl border border-tan bg-cream-surface shadow-[0_30px_60px_-30px_rgba(78,47,27,0.45)] ${className}`}
  >
    <MapCanvas mode={mode} animated={animated} />
  </div>
);
