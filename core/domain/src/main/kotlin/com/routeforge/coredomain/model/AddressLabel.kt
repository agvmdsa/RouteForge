package com.routeforge.coredomain.model

/**
 * A readable description of one position: [full] is shown in a confirmation, [short] is offered as the name of a favorite.
 * Neither is ever blank; an answer with no usable text is "nothing found", not an empty label.
 */
data class AddressLabel(
    val full: String,
    val short: String,
)
