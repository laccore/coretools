package org.andrill.coretools.scene.event;

import java.awt.geom.Rectangle2D;

import org.andrill.coretools.graphics.GraphicsContext;

class RectangleFeedback implements DefaultFeedback.Figure {
	Rectangle2D rect;
	
	public RectangleFeedback(Rectangle2D rect) { this.rect = rect; }
	
	public void render(GraphicsContext g) {
		g.setFill(Feedback.COLOR);
		g.fillRectangle(rect);
	}
}
