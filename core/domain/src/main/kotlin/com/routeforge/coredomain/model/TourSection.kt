package com.routeforge.coredomain.model

/** One section per screen covered by the first-run onboarding tour — see spec
 *  013-first-run-onboarding-tour. Favorites and Address Search have no value here; they're
 *  explicitly out of scope for that feature. */
enum class TourSection { SIMULATION, ROUTE_CREATION, SAVED }
