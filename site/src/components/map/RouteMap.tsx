import React from "react";
import { motion, useReducedMotion } from "framer-motion";

/** The road the Guided route follows: a, b, c and d are the waypoints. */
const GUIDED_ROUTE = "M70 350 C 125 345, 150 270, 205 255 S 290 262, 335 195 S 410 110, 468 78";
const WAYPOINTS = [
  { x: 70, y: 350, label: "A" },
  { x: 205, y: 255, label: "B" },
  { x: 335, y: 195, label: "C" },
  { x: 468, y: 78, label: "D" },
];
const FREE_ROAM_ROUTE = WAYPOINTS.map((p, i) => `${i === 0 ? "M" : "L"}${p.x} ${p.y}`).join(" ");

const ROADS = [
  "M20 300 C 120 300, 180 330, 260 310 S 400 260, 500 285",
  "M130 400 C 150 330, 190 280, 250 200 S 300 90, 330 20",
  "M30 130 C 130 150, 210 120, 290 150 S 420 190, 500 160",
  "M390 410 C 380 340, 400 280, 440 230 S 500 190, 505 150",
];

/** A stylised map with the two ways RouteForge plays a route: along the roads (Guided) or straight through the points (Free-roam). */
export const RouteMap = () => {
  const reduceMotion = useReducedMotion();

  return (
    <div className="relative mx-auto w-full max-w-xl">
      <div className="overflow-hidden rounded-3xl border border-tan bg-cream-surface shadow-[0_30px_60px_-30px_rgba(78,47,27,0.45)]">
        <svg viewBox="0 0 520 420" className="block w-full" role="img" aria-label="A route that follows the roads between four waypoints, next to the straight line a Free-roam route would take">
          <defs>
            <pattern id="graticule" width="40" height="40" patternUnits="userSpaceOnUse">
              <path d="M40 0H0V40" fill="none" stroke="#DCC7A9" strokeOpacity="0.35" strokeWidth="1" />
            </pattern>
            <path id="guided-route" d={GUIDED_ROUTE} />
          </defs>

          <rect width="520" height="420" fill="#FBF5EC" />
          <rect width="520" height="420" fill="url(#graticule)" />
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

          <path d={FREE_ROAM_ROUTE} fill="none" stroke="#6B4226" strokeWidth="2.2" strokeDasharray="2 7" strokeLinecap="round" opacity="0.8" />

          <path
            d={GUIDED_ROUTE}
            pathLength={1}
            fill="none"
            stroke="#C1521C"
            strokeWidth="5"
            strokeLinecap="round"
            className="route-draw"
          />

          {WAYPOINTS.map((p, i) => (
            <motion.g
              key={p.label}
              initial={{ opacity: reduceMotion ? 1 : 0, y: reduceMotion ? 0 : -8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: reduceMotion ? 0 : 0.4 + i * 0.7 }}
            >
              <circle cx={p.x} cy={p.y} r="11" fill="#4E2F1B" stroke="#FBF5EC" strokeWidth="3" />
              <text x={p.x} y={p.y + 4} textAnchor="middle" fontSize="11" fontWeight="700" fill="#F6ECDD">
                {p.label}
              </text>
            </motion.g>
          ))}

          {!reduceMotion && (
            <g>
              <circle r="15" fill="#C1521C" opacity="0.22">
                <animateMotion dur="9s" begin="3.4s" repeatCount="indefinite" rotate="auto">
                  <mpath href="#guided-route" />
                </animateMotion>
              </circle>
              <circle r="7" fill="#C1521C" stroke="#FBF5EC" strokeWidth="3">
                <animateMotion dur="9s" begin="3.4s" repeatCount="indefinite" rotate="auto">
                  <mpath href="#guided-route" />
                </animateMotion>
              </circle>
            </g>
          )}
        </svg>

        <div className="flex flex-wrap items-center gap-x-6 gap-y-2 border-t border-tan bg-cream px-5 py-3 text-xs text-coffee sm:text-sm">
          <span className="flex items-center gap-2">
            <span className="h-1 w-7 rounded-full bg-route" />
            Guided: follows the roads
          </span>
          <span className="flex items-center gap-2">
            <span className="w-7 border-t-2 border-dotted border-coffee" />
            Free-roam: straight through your points
          </span>
        </div>
      </div>
    </div>
  );
};
