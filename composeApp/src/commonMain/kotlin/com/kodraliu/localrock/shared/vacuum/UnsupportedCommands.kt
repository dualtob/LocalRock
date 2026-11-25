package com.kodraliu.localrock.shared.vacuum

/**
 * Where a robot's refused commands are kept, so an option it has turned down once stays hidden
 * after the app restarts. A command counts as refused only when the robot answers that it doesn't
 * know it (`unknown_method`); an error about the params may be this app's mistake, not the robot's.
 */
interface UnsupportedCommandStore {
    fun load(): Set<String>
    fun save(commands: Set<String>)

    /** Forgets everything when the app closes; for demo mode and tests. */
    class InMemory : UnsupportedCommandStore {
        private var commands = emptySet<String>()
        override fun load(): Set<String> = commands
        override fun save(commands: Set<String>) { this.commands = commands }
    }
}

/** The key a Beta page option is remembered under, since several share one write command. */
internal fun featureCommandKey(featureId: String): String = "feature:$featureId"
