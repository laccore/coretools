package org.andrill.coretools.scene.event;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import org.andrill.coretools.graphics.GraphicsContext;

class ResizeFeedback implements DefaultFeedback.Figure {
	Rectangle2D rect;
	String depth;
	boolean drawTop;
	Font font = new Font("SanSerif", Font.PLAIN, 11);
	
	public ResizeFeedback(Rectangle2D rect, String depth, boolean drawTop) {
		this.rect = rect;
		this.depth = depth;
		this.drawTop = drawTop;
	}
	
	public void render(GraphicsContext g) {
		g.setFill(Feedback.COLOR);
		g.fillRectangle(rect);

		final int str_x = (int)(rect.getX() + rect.getWidth() + 2);
		final int str_y = drawTop ? (int)rect.getY() : (int)(rect.getY() + rect.getHeight());
		FeedbackGraphics.drawDepthString(g, font, str_x, str_y, depth);
	}
}
