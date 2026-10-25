package app.reelstack.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PersonTrailTest {
    private val trail = PersonTrail<String>()

    @Test fun backFromAPersonReturnsToTheTitleThatOpenedIt() {
        trail.openedPerson(fromTitle = "A")
        assertEquals(PersonTrail.Step.Title("A"), trail.back(onPerson = true, personId = 9))
        assertNull(trail.back(onPerson = true, personId = 9))
    }

    @Test fun backFromATitleOpenedOnAPersonReturnsToThatPersonThenTheFirstTitle() {
        trail.openedPerson(fromTitle = "A")
        trail.openedTitleFromPerson(9)
        assertEquals(PersonTrail.Step.Person(9), trail.back(onPerson = false, personId = 9))
        trail.openedTitleFromPerson(9)
        assertEquals(PersonTrail.Step.Person(9), trail.back(onPerson = false, personId = 9))
        assertEquals(PersonTrail.Step.Title("A"), trail.back(onPerson = true, personId = 9))
    }

    @Test fun aPersonOpenedFromSearchClosesOnBack() {
        trail.openedPerson(fromTitle = null)
        assertNull(trail.back(onPerson = true, personId = 9))
    }

    @Test fun aTitleOpenedElsewhereDoesNotLeadBackToAnOldPerson() {
        trail.openedPerson(fromTitle = null)
        trail.openedTitleFromPerson(9)
        trail.openedTitleElsewhere()
        assertNull(trail.back(onPerson = false, personId = 9))
    }
}
