package org.andrill.coretools.scene.event;

import java.awt.*;
import java.awt.geom.Rectangle2D;

import org.andrill.coretools.graphics.GraphicsContext;

class MoveFeedback implements DefaultFeedback.Figure {
	Rectangle2D rect;
	String topDepth, baseDepth;
	Font font = new Font("SanSerif", Font.PLAIN, 11);
	
	public MoveFeedback(Rectangle2D rect, String topDepth, String baseDepth) {
		this.rect = rect;
		this.topDepth = topDepth;
		this.baseDepth = baseDepth;
	}
	
	public void render(GraphicsContext g) {
		g.setFill(Feedback.COLOR);
		g.fillRectangle(rect);

		final int str_x = (int)(rect.getX() + rect.getWidth() + 5);
		if (topDepth.equals(baseDepth)) {
			FeedbackGraphics.drawDepthString(g, font, str_x, (int)rect.getY(), topDepth);
		} else {
			FeedbackGraphics.drawDepthString(g, font, str_x, (int)rect.getY(), topDepth);
			FeedbackGraphics.drawDepthString(g, font, str_x, (int)(rect.getY() + rect.getHeight()), baseDepth);
		}
	}
}


