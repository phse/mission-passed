package app.heckit.ide.missionpassed

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project

/** Testläufe: grün = MISSION PASSED, rot = WASTED. Abgebrochene oder leere Läufe zeigen nichts. */
class MissionTestListener(private val project: Project) : SMTRunnerEventsAdapter() {
    override fun onTestingFinished(testsRoot: SMTestProxy.SMRootTestProxy) {
        if (testsRoot.wasTerminated() || testsRoot.isEmptySuite) return
        val leaves = testsRoot.allTests.filter { it.isLeaf && it !== testsRoot }
        val failed = leaves.count { it.isDefect && !it.isIgnored }
        val settings = MissionSettings.get().state
        ApplicationManager.getApplication().invokeLater({
            if (project.isDisposed) return@invokeLater
            if (failed > 0 || !testsRoot.isPassed) {
                if (settings.showWasted) {
                    val text = if (failed == 1) "1 test failed" else if (failed > 1) "$failed tests failed" else "tests failed"
                    MissionOverlay.show(project, MissionOverlay.Kind.WASTED, text)
                }
            } else if (settings.showPassed) {
                MissionOverlay.show(project, MissionOverlay.Kind.PASSED, "RESPECT +${leaves.size.coerceAtLeast(1)}")
            }
        }, ModalityState.any())
    }
}
