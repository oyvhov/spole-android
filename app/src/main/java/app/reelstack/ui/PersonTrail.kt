package app.reelstack.ui

/**
 * Where Back goes between a title page and a person page opened from each other.
 *
 * Title → person → another title → Back → the person → Back → the first title. One level each way
 * is kept; anything opened from elsewhere starts afresh, so Back never lands on a stale page.
 */
internal class PersonTrail<T> {
    sealed interface Step {
        data class Title<T>(val details: T) : Step
        data class Person(val id: Int) : Step
    }

    private var titleBeforePerson: T? = null
    private var personBeforeTitle: Int? = null

    fun openedPerson(fromTitle: T?) {
        titleBeforePerson = fromTitle
        personBeforeTitle = null
    }

    fun openedTitleFromPerson(personId: Int) {
        personBeforeTitle = personId
    }

    fun openedTitleElsewhere() {
        personBeforeTitle = null
    }

    /** The page Back should restore, or null when Back closes the sheet. */
    fun back(onPerson: Boolean, personId: Int?): Step? {
        if (onPerson) {
            val title = titleBeforePerson ?: return null
            titleBeforePerson = null
            return Step.Title(title)
        }
        val person = personBeforeTitle?.takeIf { it == personId } ?: return null
        personBeforeTitle = null
        return Step.Person(person)
    }

    fun clear() {
        titleBeforePerson = null
        personBeforeTitle = null
    }
}
