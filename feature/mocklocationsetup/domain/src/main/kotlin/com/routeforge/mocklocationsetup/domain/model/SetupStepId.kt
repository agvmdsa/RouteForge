package com.routeforge.mocklocationsetup.domain.model

enum class SetupStepId {
    ENABLE_DEVELOPER_OPTIONS,
    SELECT_MOCK_LOCATION_APP,
    ;

    companion object {
        /** Total number of setup steps that exist today — co-located here, rather than a derived
         *  constant in presentation, so it's discoverable from the one place a new step is added. */
        val Count: Int = entries.size
    }
}
