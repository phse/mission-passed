package app.heckit.ide.missionpassed

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class PreviewPassedAction : DumbAwareAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) { e.presentation.isEnabled = e.project != null }
    override fun actionPerformed(e: AnActionEvent) {
        MissionOverlay.show(e.project ?: return, MissionOverlay.Kind.PASSED, "RESPECT +1")
    }
}

class PreviewWastedAction : DumbAwareAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT
    override fun update(e: AnActionEvent) { e.presentation.isEnabled = e.project != null }
    override fun actionPerformed(e: AnActionEvent) {
        MissionOverlay.show(e.project ?: return, MissionOverlay.Kind.WASTED, "1 test failed")
    }
}
