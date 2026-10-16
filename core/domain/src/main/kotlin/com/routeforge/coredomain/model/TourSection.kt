package com.routeforge.coredomain.model

/** One section per screen covered by the first-run onboarding tour — see spec
 *  013-first-run-onboarding-tour. Saved, Favorites and Address Search have no value here: Saved is
 *  introduced by the Simulation and Route Creation tours themselves (they say what saving a route
 *  gives you), and the other two are out of scope. */
enum class TourSection { SIMULATION, ROUTE_CREATION }
