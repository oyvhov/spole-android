package app.reelstack

import android.app.Application

/**
 * The application for JVM UI tests: Spole's own, minus the startup it does not need here.
 *
 * [ReelstackApplication.onCreate] schedules a background refresh, which reaches WorkManager, which
 * the manifest deliberately does not auto-initialise. In a test that is about a dialog window that
 * would only ever be noise — and it fails asynchronously, so it surfaces as an unrelated error in
 * whatever test happens to be running when it lands.
 */
class SheetTestApplication : Application()
