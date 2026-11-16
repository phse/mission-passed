package app.heckit.ide.missionpassed.screenshot

import app.heckit.ide.missionpassed.MissionOverlay
import com.intellij.ide.ui.LafManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationsManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.ex.ToolWindowEx
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFrame

/**
 * Erzeugt die Marketplace-Screenshots in der Sandbox (./gradlew runIde -Pscreenshots=<ordner>).
 * Schritte in marketplace/screenshots.txt: `bildname: datei [dark] [passed:text|wasted:text|wanted]` (_ = Leerzeichen).
 */
object ScreenshotMaker {
    private const val SCALE = 2.0
    private val log = logger<ScreenshotMaker>()

    @JvmStatic
    fun start(project: Project, dir: String) {
        val out = File(dir).apply { mkdirs() }
        Thread({
            try {
                run(project, out)
            } catch (e: Throwable) {
                log.error("Screenshots failed", e)
            }
        }, "Mission Passed screenshots").start()
    }

    private fun run(project: Project, out: File) {
        Thread.sleep(8000)
        val frame = edt { WindowManager.getInstance().getFrame(project) } ?: error("no frame")
        val laf = LafManager.getInstance()
        val previousTheme = laf.currentUIThemeLookAndFeel
        val base = edt { project.guessProjectDir() } ?: error("no project dir")
        val steps = File(System.getProperty("missionpassed.screenshotSteps")).readLines()
            .map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        edt {
            frame.extendedState = JFrame.NORMAL
            frame.setBounds(60, 40, 1280, 800)
            (ToolWindowManager.getInstance(project).getToolWindow("Project") as? ToolWindowEx)?.let { it.stretchWidth(240 - it.component.width) }
        }
        for (step in steps) {
            val name = step.substringBefore(':').trim()
            val words = step.substringAfter(':').trim().split(Regex("\\s+"))
            val file = edt { base.findFileByRelativePath(words.first()) } ?: error("${words.first()} missing")
            edt {
                theme(laf, "dark" in words)
                val fem = FileEditorManager.getInstance(project)
                fem.openFiles.forEach { fem.closeFile(it) }
                fem.openFile(file, true)
                NotificationsManager.getNotificationsManager().getNotificationsOfType(Notification::class.java, project).forEach { it.expire() }
            }
            Thread.sleep(if ("wanted" in words) 9000 else 4000)
            val passed = words.firstOrNull { it.startsWith("passed:") }?.removePrefix("passed:")?.replace('_', ' ')
            val wasted = words.firstOrNull { it.startsWith("wasted:") }?.removePrefix("wasted:")?.replace('_', ' ')
            if (passed != null || wasted != null) {
                edt {
                    if (passed != null) MissionOverlay.showNow(project, MissionOverlay.Kind.PASSED, passed)
                    else MissionOverlay.showNow(project, MissionOverlay.Kind.WASTED, wasted!!)
                }
                Thread.sleep(1200) // nach dem Einblenden, vor dem Ausblenden
            }
            shot(frame.rootPane, File(out, "$name.png"))
            Thread.sleep(3500)
        }
        edt { previousTheme?.let { laf.setCurrentLookAndFeel(it, false); laf.updateUI() } }
        log.warn("Mission Passed: screenshots written to $out")
    }

    private fun theme(laf: LafManager, dark: Boolean) {
        if (laf.currentUIThemeLookAndFeel?.isDark == dark) return
        val target = laf.installedThemes.firstOrNull { it.isDark == dark } ?: return
        laf.setCurrentLookAndFeel(target, false)
        laf.updateUI()
    }

    private fun shot(root: javax.swing.JRootPane, file: File) = edt {
        val image = BufferedImage((root.width * SCALE).toInt(), (root.height * SCALE).toInt(), BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.scale(SCALE, SCALE)
        root.paint(g)
        g.dispose()
        ImageIO.write(image, "png", file)
    }

    private fun <T> edt(block: () -> T): T {
        var result: Result<T>? = null
        ApplicationManager.getApplication().invokeAndWait { result = runCatching(block) }
        return result!!.getOrThrow()
    }
}
