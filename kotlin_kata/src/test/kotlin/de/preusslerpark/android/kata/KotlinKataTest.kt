package de.preusslerpark.android.kata

import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class KotlinKataTest {

    @Test
    fun first_stop_is_one() {
        assertEquals(1, makeDriver(1, 2, 3, 4).getCurrentStop())
    }

    @Test
    fun second_stop_is_two() {
        val driver = makeDriver(1, 2, 3, 4)
        driver.driveToNextStop()
        assertEquals(2, driver.getCurrentStop())
    }

    @Test
    fun third_stop() {
        val driver = makeDriver(1, 0, 5, 2)
        driver.driveToNextStop()
        driver.driveToNextStop()
        assertEquals(5, driver.getCurrentStop())
    }

    @Test
    fun starts_from_start_after_last_stop() {
        val driver = makeDriver(1, 2)
        assertEquals(1, driver.getCurrentStop())
        driver.driveToNextStop()
        assertEquals(2, driver.getCurrentStop())
        driver.driveToNextStop()
        assertEquals(1, driver.getCurrentStop())
    }

    @Test
    fun shoud_have_one_gossip_at_start() {
        val driver = makeDriver(1, 2)
        assertEquals(1, driver.getGossipCount())
    }

    @Test
    fun should_exchange_gossip() {
        val driver1 = makeDriver(1, 2)
        val driver2 = makeDriver(1, 2)

        driver1.askForGossips(driver2)
        assertEquals(2, driver1.getGossipCount())
    }

    @Test
    fun can_create_bus_schedules_for_drivers() {
        BusSchedule(makeDriver(1, 2), makeDriver(3, 4))
        BusSchedule(makeDriver(1, 2), makeDriver(3, 4), makeDriver(5, 6))
    }

    @Test
    fun schedule_should_move_drivers() {
        val driver1 = makeDriver(1, 2)
        val driver2 = makeDriver(3, 4)
        val schedule = BusSchedule(driver1, driver2)
        schedule.tickMinute()
        assertEquals(2, driver1.getCurrentStop())
        assertEquals(4, driver2.getCurrentStop())
    }

    @Test
    fun schedule_stops_after_eight_hours() {
        val schedule = BusSchedule(makeDriver(1, 2))
        assertFalse(schedule.isOutOfService())
        for (i in 1..480) {
            schedule.tickMinute()
        }
        assertTrue(schedule.isOutOfService())
    }

    @Test
    fun schedule_lets_drivers_talk() {
        val driver1 = makeDriver(1, 2)
        val driver2 = makeDriver(1, 2)
        val schedule = BusSchedule(driver1, driver2)

        schedule.tickMinute()

        assertEquals(2, driver1.getGossipCount())
        assertEquals(2, driver2.getGossipCount())
    }

    @Test
    fun schedule_not_let_drivers_talk_if_not_met() {
        val driver1 = makeDriver(1, 2)
        val driver2 = makeDriver(4, 5)
        val schedule = BusSchedule(driver1, driver2)

        schedule.tickMinute()
        schedule.tickMinute()

        assertEquals(1, driver1.getGossipCount())
        assertEquals(1, driver2.getGossipCount())
    }

    @Test
    fun schedule_lets_drivers_talk_if_same_station_only() {
        val driver1 = makeDriver(1, 2, 3)
        val driver2 = makeDriver(1, 5, 6)
        val driver3 = makeDriver(7, 5, 3)
        val schedule = BusSchedule(driver1, driver2, driver3)

        schedule.tickMinute()
        schedule.tickMinute()
        schedule.tickMinute()

        assertEquals(3, driver1.getGossipCount())
        assertEquals(3, driver2.getGossipCount())
        assertEquals(3, driver3.getGossipCount())
    }

    @Test
    fun schedule_lets_drivers_talk_on_first_station() {
        val driver1 = makeDriver(1, 2)
        val driver2 = makeDriver(1, 3)
        BusSchedule(driver1, driver2)
        assertEquals(2, driver1.getGossipCount())
        assertEquals(2, driver2.getGossipCount())
    }

    @Test
    fun game_with_one_driver_ends_immediately() {
        val game = GossipGame(makeDriver(1, 2))
        assertEquals("1", game.numberOfStopsNeeded())
    }

    @Test
    fun game_with_two_drivers_ends_after_they_met() {
        val game = GossipGame(makeDriver(1, 2), makeDriver(3, 2))
        assertEquals("2", game.numberOfStopsNeeded())
    }

    @Test
    fun game_with_two_drivers_never_ends_if_not_met() {
        val game = GossipGame(makeDriver(1, 2), makeDriver(3, 4))
        assertEquals("never", game.numberOfStopsNeeded())
    }

    @Test
    fun goal1() {
        val game = GossipGame(makeDriver(2, 1, 2), makeDriver(5, 2, 8))
        assertEquals("never", game.numberOfStopsNeeded())
    }

    @Test
    fun goal2() {
        val game = GossipGame(makeDriver(3, 1, 2, 3), makeDriver(3, 2, 3, 1), makeDriver(4, 2, 3, 4, 5))
        assertEquals("5", game.numberOfStopsNeeded())
    }

    private fun makeDriver(vararg route: Int): Driver {
        val driver: Driver = Driver(route.asList())
        return driver
    }
}

class GossipGame(vararg val drivers: Driver) {
    val schedule = BusSchedule(drivers.toList())

    fun numberOfStopsNeeded(): String {
        var roundsNeeded = 0
        do {
            if (schedule.isOutOfService()) {
                return "never"
            }
            roundsNeeded++
            var done = true
            drivers.forEach {
                done = done and (drivers.size == it.getGossipCount())
            }
            if (done) {
                roundsNeeded.toString()
            }
            schedule.tickMinute()
        } while (!done)

        return roundsNeeded.toString()
    }
}


class BusSchedule(val drivers : List<Driver>) {

    constructor(vararg drivers : Driver) : this(drivers.toList())

    private val MAX_WORKING_MINUTES = 480
    private var minutesWorked : Int = 0

    init {
        letDriversTalk()  // starting point
    }

    fun tickMinute() {
        moveAllDriversToNextStation()
        increaseWorkingTime()
        letDriversTalk()
    }

    private fun letDriversTalk() {
        drivers.forEach {
            val driver = it
            drivers.forEach {
                talkIfSameStationed(driver, it)
            }
        }
    }

    private fun talkIfSameStationed(driver: Driver, it: Driver) {
        if (driver.getCurrentStop() == it.getCurrentStop()) {
            driver.askForGossips(it)
            it.askForGossips(driver)
        }
    }

    private fun increaseWorkingTime() {
        minutesWorked++
    }

    private fun moveAllDriversToNextStation() {
        drivers.forEach { it.driveToNextStop() }
    }

    fun isOutOfService() = minutesWorked >= MAX_WORKING_MINUTES
}
val random = Random()

data class Gossip(val id : Int = random.nextInt()) {

}

class Driver(val route : List<Int>, var currentStopNumber : Int = 0) {

    private var gossips : HashSet<Gossip> = HashSet()
    init {
        gossips.add(Gossip())
    }

    fun getCurrentStop()  = route.get(currentStopNumber)

    fun driveToNextStop() {
        currentStopNumber = if (++currentStopNumber >= route.size) 0 else currentStopNumber
    }

    fun getGossipCount() = gossips.count()

    fun askForGossips(driver: Driver) {
        gossips.addAll(driver.gossips)
    }

}
