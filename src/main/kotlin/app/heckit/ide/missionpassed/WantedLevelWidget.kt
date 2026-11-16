package app.heckit.ide.missionpassed

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.BasicStroke
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Path2D
import javax.swing.JComponent
import kotlin.math.cos
import kotlin.math.sin

class WantedLevelWidgetFactory : StatusBarWidgetFactory {
    override fun getId() = WantedLevelWidget.ID
    override fun getDisplayName() = "Mission Passed: Wanted Level"
    override fun createWidget(project: Project): StatusBarWidget = WantedLevelWidget(project)
}

/** Fahndungslevel: bis zu fünf Sterne für Warnungen (1 Punkt) und Fehler (2 Punkte) in der aktuellen Datei. */
class WantedLevelWidget(private val project: Project) : CustomStatusBarWidget {
    companion object {
        const val ID = "MissionPassed.WantedLevel"

        fun stars(points: Int) = when {
            points <= 0 -> 0
            points == 1 -> 1
            points <= 3 -> 2
            points <= 7 -> 3
            points <= 15 -> 4
            else -> 5
        }
    }

    private val component = Stars()

    override fun ID() = ID

    override fun getComponent(): JComponent = component

    override fun install(statusBar: StatusBar) {
        val bus = project.messageBus.connect(this)
        bus.subscribe(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC, object : DaemonCodeAnalyzer.DaemonListener {
            override fun daemonFinished(fileEditors: Collection<FileEditor>) = update()
        })
        bus.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun selectionChanged(event: FileEditorManagerEvent) = update()
        })
        update()
    }

    override fun dispose() {}

    private fun update() {
        if (project.isDisposed) return
        val editor = project.service<FileEditorManager>().selectedTextEditor
        var warnings = 0
        var errors = 0
        if (editor != null) {
            val markup = DocumentMarkupModel.forDocument(editor.document, project, false)
            markup?.allHighlighters?.forEach { h ->
                val info = HighlightInfo.fromRangeHighlighter(h) ?: return@forEach
                when {
                    info.severity >= HighlightSeverity.ERROR -> errors++
                    info.severity >= HighlightSeverity.WARNING -> warnings++
                }
            }
        }
        component.level = stars(warnings + 2 * errors)
        component.toolTipText = when {
            editor == null -> "Wanted level"
            warnings + errors == 0 -> "Wanted level: clean. Nobody is looking for you."
            else -> "Wanted level ${component.level}: $errors error(s), $warnings warning(s) in this file"
        }
        component.repaint()
    }

    private class Stars : JComponent() {
        var level = 0
        private val size = JBUI.scale(11)
        private val gap = JBUI.scale(2)

        init {
            border = JBUI.Borders.empty(0, 6)
        }

        override fun getPreferredSize(): Dimension {
            val i = insets
            return Dimension(i.left + i.right + 5 * size + 4 * gap, JBUI.scale(16))
        }

        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.stroke = BasicStroke(JBUI.scale(1).toFloat(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                val y = (height - size) / 2.0
                for (i in 0 until 5) {
                    val x = insets.left + i * (size + gap).toDouble()
                    val star = star(x + size / 2.0, y + size / 2.0 + 0.5, size / 2.0)
                    if (i < level) {
                        g2.color = JBColor(0xF5C342, 0xF5C342)
                        g2.fill(star)
                        g2.color = JBColor(0x6B4E00, 0x1E1F22)
                    } else {
                        g2.color = JBColor(0xB0B3BA, 0x5A5D63)
                    }
                    g2.draw(star)
                }
            } finally {
                g2.dispose()
            }
        }

        private fun star(cx: Double, cy: Double, r: Double): Path2D {
            val p = Path2D.Double()
            for (k in 0 until 10) {
                val rad = if (k % 2 == 0) r else r * 0.45
                val a = -Math.PI / 2 + k * Math.PI / 5
                val px = cx + rad * cos(a)
                val py = cy + rad * sin(a)
                if (k == 0) p.moveTo(px, py) else p.lineTo(px, py)
            }
            p.closePath()
            return p
        }
    }
}
