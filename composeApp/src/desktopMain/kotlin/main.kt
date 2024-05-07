import androidx.compose.ui.awt.ComposePanel
import java.awt.BorderLayout
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

fun main() = SwingUtilities.invokeLater {
    val window = JFrame()
    val composePanel = ComposePanel()

    window.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    window.title = "SubMerger"
    window.contentPane.add(composePanel, BorderLayout.CENTER)

    composePanel.setContent {
        App(composePanel)
    }

    window.setSize(800, 600)
    window.setLocationRelativeTo(null)
    window.isVisible = true
}