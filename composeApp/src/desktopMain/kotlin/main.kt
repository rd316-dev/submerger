import androidx.compose.ui.awt.ComposePanel
import java.awt.BorderLayout
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

fun main() = SwingUtilities.invokeLater {
    val window = JFrame()
    window.setSize(800, 600)
    window.setLocationRelativeTo(null)

    window.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    window.title = "SubMerger"

    val composePanel = ComposePanel()
    composePanel.setContent {
        App(composePanel)
    }

    window.contentPane.add(composePanel, BorderLayout.CENTER)
    window.isVisible = true
}