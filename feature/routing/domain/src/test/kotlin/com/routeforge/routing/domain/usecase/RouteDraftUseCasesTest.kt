package com.routeforge.routing.domain.usecase

import com.routeforge.coredomain.model.RoutePoint
import com.routeforge.routing.domain.model.RouteDraft
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private fun point(
    lat: Double,
    lon: Double,
) = RoutePoint(latitude = lat, longitude = lon)

class RouteDraftUseCasesTest {
    private val add = AddWaypointUseCase()
    private val move = MoveWaypointUseCase()
    private val edit = EditWaypointUseCase()
    private val delete = DeleteWaypointUseCase()
    private val undo = UndoRouteDraftUseCase()

    @Test
    fun `add appends a waypoint`() {
        val draft = add(RouteDraft(), point(1.0, 1.0))

        assertEquals(listOf(point(1.0, 1.0)), draft.points)
    }

    @Test
    fun `move replaces the waypoint's position at its index`() {
        val draft = RouteDraft().add(point(1.0, 1.0)).add(point(2.0, 2.0))

        val moved = move(draft, index = 1, to = point(9.0, 9.0))

        assertEquals(listOf(point(1.0, 1.0), point(9.0, 9.0)), moved.points)
    }

    @Test
    fun `edit replaces the waypoint's coordinates at its index`() {
        val draft = RouteDraft().add(point(1.0, 1.0))

        val edited = edit(draft, index = 0, newValue = point(5.0, 5.0))

        assertEquals(listOf(point(5.0, 5.0)), edited.points)
    }

    @Test
    fun `delete removes the waypoint at its index`() {
        val draft = RouteDraft().add(point(1.0, 1.0)).add(point(2.0, 2.0))

        val deleted = delete(draft, index = 0)

        assertEquals(listOf(point(2.0, 2.0)), deleted.points)
    }

    @Test
    fun `undo reverts the most recent change`() {
        val draft = RouteDraft().add(point(1.0, 1.0))
        val changed = add(draft, point(2.0, 2.0))

        assertEquals(draft.points, undo(changed).points)
    }
}
