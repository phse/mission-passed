package app.heckit.ide.missionpassed

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

class MissionSettingsState : BaseState() {
    var showPassed by property(true)
    var showWasted by property(true)
    /** Auch normale Run-Konfigurationen (nicht nur Tests) lösen die Einblendung aus. */
    var includeRuns by property(true)
}

@Service(Service.Level.APP)
@State(name = "MissionPassed", storages = [Storage("missionPassed.xml")])
class MissionSettings : SimplePersistentStateComponent<MissionSettingsState>(MissionSettingsState()) {
    companion object {
        fun get(): MissionSettings = service()
    }
}
