/*
 * Copyright (c) Josh Reed, 2009.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.andrill.coretools.graphics.driver;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import org.andrill.coretools.Platform;
import org.andrill.coretools.ResourceLoader;
import org.andrill.coretools.graphics.fill.ColorFill;
import org.andrill.coretools.graphics.fill.Fill;
import org.andrill.coretools.graphics.fill.GradientFill;
import org.andrill.coretools.graphics.fill.MultiFill;
import org.andrill.coretools.graphics.fill.TextureFill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the Driver interface for Java2D.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class Java2DDriver implements Driver {
	protected static class State {
		Fill fill = null;
		Color lineColor = null;
		LineStyle lineStyle = LineStyle.SOLID;
		int lineThickness = 1;
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(Java2DDriver.class);
	private static final Rectangle ANCHOR = new Rectangle(0, 0, 32, 32);
	protected static final float LINE_DASH[] = { 18, 9 };
	protected static final float LINE_DASH_DOT[] = { 9, 3, 3, 3 };
	protected static final float LINE_DOT[] = { 3, 3 };

	protected Fill background = new ColorFill(Color.white);
	protected Graphics2D g2d = null;
	protected final ImageCache cache;
	protected final ResourceLoader loader;
	protected Color lineColor = Color.black;
	protected LineStyle lineStyle = LineStyle.SOLID;
	protected int lineThickness = 1;
	protected Rectangle2D originalClip = null;
	protected AffineTransform originalTransform = null;
	protected boolean scaleStrokes = false;
	protected BasicStroke stroke = null;
	protected Stack<AffineTransform> transforms = new Stack<AffineTransform>();
	protected Stack<State> stateStack = new Stack<State>();
	protected JComponent interactive = null;
	protected BufferedImage imageError = null;
	protected BufferedImage imageLoading = null;

	/**
	 * Create a new Java2DDriver.
	 * 
	 * @param graphics
	 *            the Java2D graphics object.
	 */
	public Java2DDriver(final Graphics2D graphics) {
		this(graphics, false, null);
	}

	/**
	 * Create a new Java2DDriver.
	 * 
	 * @param graphics
	 *            the Java2D graphics object.
	 * @param scaleStrokes
	 *            true if stroke widths should be scaled, false otherwise.
	 * @param interactive
	 *            the component who the Graphics2D object belongs to, or null.
	 */
	public Java2DDriver(final Graphics2D graphics, final boolean scaleStrokes, final JComponent interactive) {
		g2d = graphics;
		this.scaleStrokes = scaleStrokes;
		originalTransform = g2d.getTransform();
		originalClip = g2d.getClipBounds();
		transforms.push(new AffineTransform());
		this.interactive = interactive;
		loader = Platform.getService(ResourceLoader.class);
		cache = Platform.getService(ImageCache.class);
	}

	private List<Paint> createPaints(final Fill fill, final Shape shape) {
		final List<Paint> paints = new ArrayList<Paint>();

		// if no fill, then just fill with white
		if (fill == null) {
			paints.add(Color.white);
			return paints;
		}

		// create paints based on fill style
		switch (fill.getStyle()) {
			case COLOR:
				final ColorFill c = (ColorFill) fill;
				paints.add(c.getColor());
				break;
			case GRADIENT:
				final GradientFill g = (GradientFill) fill;
				final Rectangle2D r = shape.getBounds2D();
				Point2D p1,
				p2;
				if (g.isHorizontal()) {
					p1 = new Point2D.Double(r.getMinX(), r.getCenterY());
					p2 = new Point2D.Double(r.getMaxX(), r.getCenterY());
				} else {
					p1 = new Point2D.Double(r.getCenterX(), r.getMinY());
					p2 = new Point2D.Double(r.getCenterX(), r.getMaxY());
				}
				paints.add(new GradientPaint(p1, g.getStart(), p2, g.getEnd()));
				break;
			case TEXTURE:
				final TextureFill t = (TextureFill) fill;
				BufferedImage iimage;
				try {
					iimage = cache.get(t.getTexture(), 1, interactive).get();
					if (iimage != null) {
						paints
						        .add(new TexturePaint(iimage,
						                new Rectangle2D.Double(0, 0, iimage.getWidth() / t.getScaling(), iimage.getHeight() / t.getScaling())));
					} else {
						LOGGER.error("Unable to load texture {}", t.getTexture().toExternalForm());
					}
				} catch (InterruptedException e) {
					LOGGER.error("Unable to load texture {}: {}", t.getTexture().toExternalForm(), e.getMessage());
				} catch (ExecutionException e) {
					LOGGER.error("Unable to load texture {}: {}", t.getTexture().toExternalForm(), e.getMessage());
				}
				break;
			case MULTI:
				final MultiFill m = (MultiFill) fill;
				for (final Fill f : m.getFills()) {
					paints.addAll(createPaints(f, shape));
				}
		}
		return paints;
	}

	private Shape createPolygon(final List<Point2D> points) {
		final GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
		boolean first = true;
		for (final Point2D p : points) {
			if (first) {
				path.moveTo((float) p.getX(), (float) p.getY());
			} else {
				path.lineTo((float) p.getX(), (float) p.getY());
			}
			first = false;
		}
		path.closePath();
		return path;
	}

	private void createStroke() {
		switch (lineStyle) {
			case SOLID:
				stroke = new BasicStroke(lineThickness, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, lineThickness,
				        null, 0);
				break;
			case DASHED:
				stroke = new BasicStroke(lineThickness, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, lineThickness,
				        LINE_DASH, 0);
				break;
			case DOTTED:
				stroke = new BasicStroke(lineThickness, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, lineThickness,
				        LINE_DOT, 0);
				break;
			case DASH_DOTTED:
				stroke = new BasicStroke(lineThickness, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, lineThickness,
				        LINE_DASH_DOT, 0);
				break;
			default:
				stroke = new BasicStroke(lineThickness, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, lineThickness,
				        null, 0);
				break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		if (g2d != null) {
			g2d.dispose();
			g2d = null;
		}
		interactive = null;
		stateStack.clear();
		transforms.clear();
		originalClip = null;
		originalTransform = null;
	}

	private void draw(final Shape s) {
		prepareDraw();
		if (scaleStrokes) {
			g2d.draw(s);
		} else {
			g2d.setTransform(originalTransform);
			g2d.draw(transforms.peek().createTransformedShape(s));
			g2d.transform(transforms.peek());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawArc(final Rectangle2D bounds, final double start, final double extent, final ArcStyle style) {
		draw(new Arc2D.Double(bounds, start, extent, getArcType(style)));
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawImage(final Point2D point, final URL image) {
		prepareDraw();
		Future<BufferedImage> future = cache.get(image, 1, interactive);
		if ((interactive == null) || future.isDone()) {
			try {
				BufferedImage bi = future.get();
				if (bi != null) {
					g2d.drawImage(bi, (int) point.getX(), (int) point.getY(), null);
				} else {
					drawImageError(new Rectangle2D.Double(point.getX(), point.getY(), ANCHOR.getWidth(), ANCHOR
					        .getHeight()), image);
				}
			} catch (InterruptedException e) {
				LOGGER.error("drawImage() error", e);
				drawImageError(
				        new Rectangle2D.Double(point.getX(), point.getY(), ANCHOR.getWidth(), ANCHOR.getHeight()),
				        image);
			} catch (ExecutionException e) {
				LOGGER.error("drawImage() error", e);
				drawImageError(
				        new Rectangle2D.Double(point.getX(), point.getY(), ANCHOR.getWidth(), ANCHOR.getHeight()),
				        image);
			}
		} else {
			drawImageLoading(new Rectangle2D.Double(point.getX(), point.getY(), ANCHOR.getWidth(), ANCHOR.getHeight()),
			        image);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawImage(final Rectangle2D rect, final URL image) {
		drawImageScaled(rect, image, true);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void embedImage(final Rectangle2D rect, final URL image) {
		drawImageScaled(rect, image, false);
	}
	
	// Internal draw method to accommodate drawImage and embedImage
	protected void drawImageScaled(final Rectangle2D rect, final URL image, final boolean scaleToRect) {
		prepareDraw();
		Future<BufferedImage> future = null;
		if (scaleToRect) {
			future = cache.get(image, new Dimension((int) rect.getWidth(), (int) rect.getHeight()), interactive);
		} else {
			future = cache.get(image, interactive);
		}
		if ((interactive == null) || future.isDone()) {
			try {
				BufferedImage bi = future.get();
				if (bi != null) {
					g2d.drawImage(bi, (int) rect.getX(), (int) rect.getY(), (int) rect.getWidth(), (int) rect
					        .getHeight(), null);
				} else {
					drawImageError(rect, image);
				}
			} catch (InterruptedException e) {
				LOGGER.error("drawImage() error", e);
				drawImageError(rect, image);
			} catch (ExecutionException e) {
				LOGGER.error("drawImage() error", e);
				drawImageError(rect, image);
			}
		} else {
			drawImageLoading(rect, image);
		}
	}
	

	protected void drawImageError(final Rectangle2D r, final URL orig) {
		if (imageError == null) {
			try {
				imageError = ImageIO.read(loader.getResource("rsrc:org/andrill/coretools/graphics/driver/error.png"));
			} catch (IOException e) {
				LOGGER.error("Unable to load 'error.png'");
			}
		}
		g2d.setPaint(new TexturePaint(imageError, ANCHOR));
		g2d.fill(r);
	}

	protected void drawImageLoading(final Rectangle2D r, final URL url) {
		// try a placeholder image first
		Future<BufferedImage> placeholder = cache.getPlaceholderImage(url, new Dimension((int) r.getWidth(), (int) r
		        .getHeight()));
		if (placeholder == null) {
			internalDrawImageLoading(r);
		} else {
			try {
				BufferedImage image = placeholder.get();
				if (image == null) {
					internalDrawImageLoading(r);
				} else {
					g2d.drawImage(image, (int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight(), null);
				}
			} catch (InterruptedException e) {
				LOGGER.error("drawImageLoading() error", e);
				internalDrawImageLoading(r);
			} catch (ExecutionException e) {
				LOGGER.error("drawImageLoading() error", e);
				internalDrawImageLoading(r);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawLine(final Point2D start, final Point2D end) {
		draw(new Line2D.Double(start, end));
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawOval(final Rectangle2D bounds) {
		draw(new Ellipse2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight()));
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawPoint(final Point2D point) {
		prepareDraw();
		draw(new Line2D.Double(point, point));
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawPolygon(final List<Point2D> points) {
		draw(createPolygon(points));
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawRectangle(final Rectangle2D rect) {
		draw(rect);
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawString(final Point2D point, final Font font, final String string) {
		prepareDraw();
		g2d.setFont(font);
		g2d.drawString(string, (int) point.getX(), (int) point.getY() + font.getSize());
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void drawStringRotated(Point2D point, Font font, String string, double theta) {
		AffineTransform oldTransform = g2d.getTransform();
		g2d.translate(point.getX(), point.getY());
		g2d.rotate(theta);
		drawString(new Point(0, 0), font, string);
		g2d.setTransform(oldTransform);
	}

	private void fill(final Shape s) {
		for (final Paint p : createPaints(background, s)) {
			g2d.setPaint(p);
			g2d.fill(s);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void fillArc(final Rectangle2D bounds, final double start, final double extent, final ArcStyle style) {
		fill(new Arc2D.Double(bounds, start, extent, getArcType(style)));
	}

	/**
	 * {@inheritDoc}
	 */
	public void fillOval(final Rectangle2D bounds) {
		fill(new Ellipse2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight()));
	}

	/**
	 * {@inheritDoc}
	 */
	public void fillPolygon(final List<Point2D> points) {
		fill(createPolygon(points));
	}

	/**
	 * {@inheritDoc}
	 */
	public void fillRectangle(final Rectangle2D rect) {
		fill(rect);
	}

	private int getArcType(final ArcStyle style) {
		switch (style) {
			case OPEN:
				return Arc2D.OPEN;
			case CLOSED:
				return Arc2D.CHORD;
			case SECTOR:
				return Arc2D.PIE;
			default:
				return -1;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Rectangle2D getClip() {
		return g2d.getClipBounds();
	}

	/**
	 * {@inheritDoc}
	 */
	public Fill getFill() {
		return background;
	}

	/**
	 * {@inheritDoc}
	 */
	public Color getLineColor() {
		return lineColor;
	}

	/**
	 * {@inheritDoc}
	 */
	public LineStyle getLineStyle() {
		return lineStyle;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getLineThickness() {
		return lineThickness;
	}

	/**
	 * {@inheritDoc}
	 */
	public Rectangle2D getStringBounds(final Font font, final String string) {
		final FontMetrics metrics = g2d.getFontMetrics(font);
		return metrics.getStringBounds(string, g2d);
	}

	protected void internalDrawImageLoading(final Rectangle2D r) {
		if (imageLoading == null) {
			try {
				imageLoading = ImageIO.read(loader
				        .getResource("rsrc:org/andrill/coretools/graphics/driver/loading.png"));
			} catch (IOException e) {
				LOGGER.error("Unable to load 'loading.png'");
			}
		}
		g2d.setPaint(new TexturePaint(imageLoading, ANCHOR));
		g2d.fill(r);
	}

	/**
	 * Restores the previous state of the context.
	 */
	public void popState() {
		if (!stateStack.isEmpty()) {
			State state = stateStack.pop();
			setFill(state.fill);
			setLineColor(state.lineColor);
			setLineStyle(state.lineStyle);
			setLineThickness(state.lineThickness);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void popTransform() {
		if (!transforms.isEmpty()) {
			transforms.pop();
			g2d.setTransform(originalTransform);
			if (!transforms.isEmpty()) {
				g2d.transform(transforms.peek());
			}
		}
	}

	private void prepareDraw() {
		if (stroke == null) {
			createStroke();
		}
		g2d.setStroke(stroke);
		g2d.setPaint(lineColor);
	}

	/**
	 * Saves the current state of the context.
	 */
	public void pushState() {
		State state = new State();
		state.fill = getFill();
		state.lineColor = getLineColor();
		state.lineStyle = getLineStyle();
		state.lineThickness = getLineThickness();
		stateStack.push(state);
	}

	/**
	 * {@inheritDoc}
	 */
	public void pushTransform(final AffineTransform transform) {
		final AffineTransform tx = new AffineTransform(transforms.peek());
		tx.concatenate(transform);
		transforms.push(tx);
		g2d.transform(transform);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setClip(final Rectangle2D r) {
		if (r == null) {
			g2d.setTransform(originalTransform);
			g2d.setClip(originalClip);
			g2d.transform(transforms.peek());
		} else if (originalClip != null) {
			g2d.setTransform(originalTransform);
			g2d.setClip(transforms.peek().createTransformedShape(r).getBounds2D().createIntersection(originalClip));
			g2d.transform(transforms.peek());
		} else {
			g2d.setClip(r);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setFill(final Fill fill) {
		background = fill;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setLineColor(final Color color) {
		lineColor = color;
		stroke = null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setLineStyle(final LineStyle style) {
		lineStyle = style;
		stroke = null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setLineThickness(final int thickness) {
		lineThickness = thickness;
		stroke = null;
	}
}
