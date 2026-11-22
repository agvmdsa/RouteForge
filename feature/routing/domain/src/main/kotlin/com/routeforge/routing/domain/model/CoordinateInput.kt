package com.routeforge.routing.domain.model

/** The number typed into a latitude or longitude field. A decimal comma counts as a decimal point, since that is what
 *  the keyboard offers in many languages. */
fun String.toCoordinateOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()
