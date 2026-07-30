package psicat.util

import java.awt.Font
import javax.swing.UIManager

public class FontUtils {
    static Font helpTextFont = null

    static Font getHelpTextFont() {
        if (helpTextFont == null) {
            Font defaultFont = UIManager.getDefaults().getFont("Label.font")
            helpTextFont = defaultFont.deriveFont((float)(defaultFont.getSize() - 2.0))
        }
        return helpTextFont
    }
}