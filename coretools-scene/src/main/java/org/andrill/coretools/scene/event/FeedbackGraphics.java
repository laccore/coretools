package org.andrill.coretools.scene.event;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import org.andrill.coretools.graphics.GraphicsContext;

public class FeedbackGraphics {
    private static Color LIGHT_GRAY_BG = new Color(240,240,240);

    public static void drawDepthString(GraphicsContext g, Font font, int x, int y, String depth) {
        final int PADDING = 4;
        final Rectangle2D bounds = g.getStringBounds(font, depth);
        final int rectHeight = (int)(bounds.getHeight() + PADDING);
        final Rectangle2D bgRect = new Rectangle(x, y - (rectHeight/2), (int)(bounds.getWidth() + PADDING), rectHeight);
        g.setFill(LIGHT_GRAY_BG);
        g.fillRectangle(bgRect);
        g.setLineColor(Color.BLACK);
        g.drawRectangle(bgRect);
        g.drawStringCenter(bgRect, font, depth);
    }  
}
