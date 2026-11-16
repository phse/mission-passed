package app.heckit.ide.missionpassed

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel

class MissionConfigurable : BoundConfigurable("Mission Passed") {
    override fun createPanel(): DialogPanel {
        val s = MissionSettings.get().state
        return panel {
            row { checkBox("Show MISSION PASSED when tests pass").bindSelected(s::showPassed) }
            row { checkBox("Show WASTED when tests fail").bindSelected(s::showWasted) }
            row {
                checkBox("Also for other run configurations (exit code 0 or not)").bindSelected(s::includeRuns)
            }
            row {
                comment("The wanted level in the status bar can be hidden like any status bar widget: right-click the status bar.")
            }
            row {
                button("Preview MISSION PASSED") { MissionOverlay.showAnywhere(MissionOverlay.Kind.PASSED, "RESPECT +1") }
                button("Preview WASTED") { MissionOverlay.showAnywhere(MissionOverlay.Kind.WASTED, "1 test failed") }
            }
        }
    }
}
