package com.routeforge.routing.domain.model

import com.routeforge.coredomain.model.RoutePoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

private fun point(
    lat: Double,
    lon: Double,
) = RoutePoint(latitude = lat, longitude = lon)

class RouteDraftTest {
    @Test
    fun `starts empty`() {
        assertEquals(emptyList<RoutePoint>(), RouteDraft().points)
    }

    @Test
    fun `add appends a waypoint in order`() {
        val draft = RouteDraft().add(point(1.0, 1.0)).add(point(2.0, 2.0))

        assertEquals(listOf(point(1.0, 1.0), point(2.0, 2.0)), draft.points)
    }

    @Test
    fun `update replaces the waypoint at the given index`() {
        val draft = RouteDraft().add(point(1.0, 1.0)).add(point(2.0, 2.0))

        val updated = draft.update(index = 0, newValue = point(9.0, 9.0))

        assertEquals(listOf(point(9.0, 9.0), point(2.0, 2.0)), updated.points)
    }

    @Test
    fun `delete removes the waypoint at the given index`() {
        val draft = RouteDraft().add(point(1.0, 1.0)).add(point(2.0, 2.0)).add(point(3.0, 3.0))

        val deleted = draft.delete(index = 1)

        assertEquals(listOf(point(1.0, 1.0), point(3.0, 3.0)), deleted.points)
    }

    @Test
    fun `undo reverts the most recent change`() {
        val original = RouteDraft().add(point(1.0, 1.0))
        val changed = original.add(point(2.0, 2.0))

        val reverted = changed.undo()

        assertEquals(original.points, reverted.points)
    }

    @Test
    fun `repeated undo steps back through prior states in order`() {
        val step1 = RouteDraft().add(point(1.0, 1.0))
        val step2 = step1.add(point(2.0, 2.0))
        val step3 = step2.add(point(3.0, 3.0))

        val backOne = step3.undo()
        val backTwo = backOne.undo()

        assertEquals(step2.points, backOne.points)
        assertEquals(step1.points, backTwo.points)
    }

    @Test
    fun `undo with no prior state is a no-op`() {
        val draft = RouteDraft()

        val undone = draft.undo()

        assertEquals(draft.points, undone.points)
    }

    @Test
    fun `undo with no prior state returns the same instance`() {
        val draft = RouteDraft().add(point(1.0, 1.0)).undo()

        assertSame(draft, draft.undo())
    }

    @Test
    fun `bit-for-bit identical consecutive waypoints are both kept, not merged`() {
        val samePoint = point(1.0, 1.0)
        val draft = RouteDraft().add(samePoint).add(samePoint)

        assertEquals(listOf(samePoint, samePoint), draft.points)
    }

    @Test
    fun `waypoints differing by any amount are treated as distinct`() {
        val draft = RouteDraft().add(point(1.0, 1.0)).add(point(1.0000000001, 1.0))

        assertEquals(2, draft.points.size)
        assertEquals(1.0, draft.points[0].latitude)
        assertEquals(1.0000000001, draft.points[1].latitude)
    }
}
