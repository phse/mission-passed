package app.heckit.ide.missionpassed

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.WindowManager
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import javax.swing.JComponent
import javax.swing.JLayeredPane
import javax.swing.SwingUtilities
import javax.swing.Timer

/** Große Einblendung über dem IDE-Fenster, blendet sich selbst wieder aus. Fängt keine Mausklicks ab. */
object MissionOverlay {
    enum class Kind { PASSED, WASTED }

    private const val FADE_IN = 250
    private const val HOLD = 2200
    private const val FADE_OUT = 600

    private val baseFont: Font by lazy {
        MissionOverlay::class.java.getResourceAsStream("/fonts/Bangers-Regular.ttf").use {
            Font.createFont(Font.TRUETYPE_FONT, it)
        }
    }

    @Volatile
    var lastShown = 0L
        private set

    fun show(project: Project, kind: Kind, subtitle: String) {
        val frame = WindowManager.getInstance().getIdeFrame(project)?.component ?: return
        val root = SwingUtilities.getRootPane(frame) ?: return
        show(root.layeredPane, kind, subtitle)
    }

    /** Für die Vorschau in den Einstellungen: das gerade aktive Fenster. */
    fun showAnywhere(kind: Kind, subtitle: String) {
        val window = IdeFocusManager.getGlobalInstance().lastFocusedFrame?.component ?: return
        val root = SwingUtilities.getRootPane(window) ?: return
        show(root.layeredPane, kind, subtitle)
    }

    private fun show(layered: JLayeredPane, kind: Kind, subtitle: String) {
        lastShown = System.currentTimeMillis()
        layered.components.filterIsInstance<Banner>().forEach { it.dispose() }
        val banner = Banner(kind, subtitle)
        banner.setBounds(0, 0, layered.width, layered.height)
        layered.add(banner, JLayeredPane.POPUP_LAYER as Any)
        layered.repaint()
        banner.start()
    }

    private class Banner(private val kind: Kind, private val subtitle: String) : JComponent() {
        private val started = System.currentTimeMillis()
        private val timer = Timer(16) { tick() }

        init {
            isOpaque = false
        }

        fun start() = timer.start()

        fun dispose() {
            timer.stop()
            val p = parent ?: return
            p.remove(this)
            p.repaint()
        }

        private fun tick() {
            val p = parent
            if (p == null || elapsed() > FADE_IN + HOLD + FADE_OUT) dispose() else {
                setBounds(0, 0, p.width, p.height)
                repaint()
            }
        }

        private fun elapsed() = (System.currentTimeMillis() - started).toInt()

        private fun alpha(): Float {
            val t = elapsed()
            return when {
                t < FADE_IN -> t.toFloat() / FADE_IN
                t < FADE_IN + HOLD -> 1f
                else -> (1f - (t - FADE_IN - HOLD).toFloat() / FADE_OUT).coerceIn(0f, 1f)
            }
        }

        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha())
                if (kind == Kind.PASSED) paintPassed(g2) else paintWasted(g2)
            } finally {
                g2.dispose()
            }
        }

        private fun paintPassed(g: Graphics2D) {
            val size = (height / 9f).coerceIn(48f, 110f)
            val cy = height * 0.42f
            val bandH = size * 2.3f
            val top = cy - bandH / 2
            // Dunkles Band, zu den Rändern weich auslaufend
            g.paint = GradientPaint(0f, top, Color(0, 0, 0, 0), 0f, top + bandH * 0.25f, Color(0, 0, 0, 170))
            g.fillRect(0, top.toInt(), width, (bandH * 0.25f).toInt() + 1)
            g.color = Color(0, 0, 0, 170)
            g.fillRect(0, (top + bandH * 0.25f).toInt(), width, (bandH * 0.5f).toInt() + 1)
            g.paint = GradientPaint(0f, top + bandH * 0.75f, Color(0, 0, 0, 170), 0f, top + bandH, Color(0, 0, 0, 0))
            g.fillRect(0, (top + bandH * 0.75f).toInt(), width, (bandH * 0.25f).toInt() + 1)

            outlinedText(g, "MISSION PASSED!", size, cy + size * 0.1f, Color(0xF5, 0xC3, 0x42))
            outlinedText(g, subtitle.uppercase(), size * 0.42f, cy + size * 0.75f, Color.WHITE)
        }

        private fun paintWasted(g: Graphics2D) {
            g.color = Color(40, 40, 40, 140)
            g.fillRect(0, 0, width, height)
            val size = (height / 6f).coerceIn(64f, 160f)
            val cy = height * 0.45f
            outlinedText(g, "WASTED", size, cy + size * 0.35f, Color(0xD0, 0x24, 0x24))
            outlinedText(g, subtitle.uppercase(), size * 0.25f, cy + size * 0.85f, Color.WHITE)
        }

        private fun outlinedText(g: Graphics2D, text: String, size: Float, baseline: Float, fill: Color) {
            if (text.isBlank()) return
            val layout = TextLayout(text, baseFont.deriveFont(size), g.fontRenderContext)
            val x = (width - layout.advance) / 2f
            val shape = layout.getOutline(AffineTransform.getTranslateInstance(x.toDouble(), baseline.toDouble()))
            g.stroke = BasicStroke(size / 9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g.color = Color.BLACK
            g.draw(shape)
            g.color = fill
            g.fill(shape)
        }
    }
}
