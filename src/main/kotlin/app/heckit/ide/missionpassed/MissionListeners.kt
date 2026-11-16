package app.heckit.ide.missionpassed

import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.util.Alarm

/** Andere Run-Konfigurationen: Exit-Code 0 = MISSION PASSED, sonst WASTED. Vom Nutzer gestoppte Läufe zeigen nichts. */
class MissionExecutionListener(private val project: Project) : ExecutionListener {
    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, project)

    override fun processTerminated(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler, exitCode: Int) {
        val settings = MissionSettings.get().state
        if (!settings.includeRuns) return
        if (isTestProfile(env.runProfile)) return
        if (handler.getUserData(ProcessHandler.TERMINATION_REQUESTED) == true) return
        if (exitCode == 130 || exitCode == 137 || exitCode == 143) return // Ctrl+C, kill, SIGTERM
        val name = env.runProfile.name
        // Testläufe, deren Konfiguration das Interface nicht implementiert, melden sich über den Testlistener -
        // kurz warten und nichts zeigen, wenn der schon eingeblendet hat.
        val requested = System.currentTimeMillis()
        alarm.addRequest({
            if (project.isDisposed || MissionOverlay.lastShown >= requested - 2000) return@addRequest
            if (exitCode == 0) {
                if (settings.showPassed) MissionOverlay.show(project, MissionOverlay.Kind.PASSED, name)
            } else if (settings.showWasted) {
                MissionOverlay.show(project, MissionOverlay.Kind.WASTED, "$name · exit code $exitCode")
            }
        }, 500)
    }

    /** Ohne harte Abhängigkeit zum Test-Runner-Plugin (fehlt in 2026.1): Interface per Namen erkennen. */
    private fun isTestProfile(profile: Any): Boolean =
        generateSequence<Class<*>>(profile.javaClass) { it.superclass }
            .flatMap { allInterfaces(it) }
            .any { it.name == "com.intellij.execution.testframework.sm.runner.SMRunnerConsolePropertiesProvider" }

    private fun allInterfaces(c: Class<*>): Sequence<Class<*>> =
        c.interfaces.asSequence().flatMap { sequenceOf(it) + allInterfaces(it) }
}
