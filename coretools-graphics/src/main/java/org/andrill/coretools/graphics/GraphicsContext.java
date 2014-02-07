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
package org.andrill.coretools.graphics;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.andrill.coretools.graphics.driver.Driver;
import org.andrill.coretools.graphics.fill.ColorFill;
import org.andrill.coretools.graphics.fill.Fill;
import org.andrill.coretools.graphics.fill.GradientFill;
import org.andrill.coretools.graphics.fill.MultiFill;
import org.andrill.coretools.graphics.fill.TextureFill;
import org.andrill.coretools.graphics.util.ImageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple 2D graphics API that can be mapped to Java2D, SWT, and other graphics implementations.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class GraphicsContext implements Driver {
	private static final Logger LOGGER = LoggerFactory.getLogger(GraphicsContext.class);
	protected Driver driver = null;

	/**
	 * Create a new GraphicsContext.
	 */
	protected GraphicsContext() {
	}

	/**
	 * Create a new GraphicsContext.
	 * 
	 * @param driver
	 *            the driver.
	 */
	public GraphicsContext(final Driver driver) {
		this.driver = driver;
	}

	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		driver.dispose();
	}

	public void drawArc(final double x, final double y, final double w, final double h, final double start,
	        final double extent, final ArcStyle style) {
		drawArc(new Rectangle2D.Double(x, y, w, h), start, extent, style);
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawArc(final Rectangle2D bounds, final double start, final double extent, final ArcStyle style) {
		driver.drawArc(bounds, start, extent, style);
	}

	public void drawImage(final double x, final double y, final double w, final double h, final URL image) {
		drawImage(new Rectangle2D.Double(x, y, w, h), image);
	}

	public void drawImage(final double x, final double y, final URL image) {
		drawImage(new Point2D.Double(x, y), image);
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawImage(final Point2D point, final URL image) {
		if (image == null) {
			return;
		}
		driver.drawImage(point, image);
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawImage(final Rectangle2D rect, final URL image) {
		if (image == null) {
			return;
		}
		driver.drawImage(rect, image);
	}

	public void drawImageCenter(final double x, final double y, final double w, final double h, final URL image) {
		drawImageCenter(new Rectangle2D.Double(x, y, w, h), image);
	}

	public void drawImageCenter(final Rectangle2D rect, final URL image) {
		if (image == null) {
			return;
		}

		InputStream in = null;
		try {
			in = image.openStream();
			final ImageInfo ii = new ImageInfo();
			ii.setInput(in);
			if (ii.check()) {
				final double iar = (double) ii.getWidth() / (double) ii.getHeight();
				final double rar = rect.getWidth() / rect.getHeight();
				if (rar > iar) {
					drawImage(rect.getCenterX() - ii.getHeight() * iar / 2, rect.getCenterY() - (double) ii.getHeight()
					        / 2, ii.getHeight() * iar, ii.getHeight(), image);
				} else {
					drawImage(rect.getCenterX() - (double) ii.getWidth() / 2, rect.getCenterY() - ii.getWidth() / iar
					        / 2, ii.getWidth(), ii.getWidth() / iar, image);
				}
			}
		} catch (final IOException ioe) {
			LOGGER.warn("Unable to center-align image {}: {}", image, ioe.getMessage());
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (final IOException ignored) {
					// ignore
				}
			}
		}
	}

	public void drawImageLeft(final double x, final double y, final double w, final double h, final URL image) {
		drawImageLeft(new Rectangle2D.Double(x, y, w, h), image);
	}

	public void drawImageLeft(final Rectangle2D rect, final URL image) {
		if (image == null) {
			return;
		}

		InputStream in = null;
		try {
			in = image.openStream();
			final ImageInfo ii = new ImageInfo();
			ii.setInput(in);
			if (ii.check()) {
				final double iar = (double) ii.getWidth() / (double) ii.getHeight();
				final double rar = rect.getWidth() / rect.getHeight();
				if (rar > iar) {
					drawImage(rect.getX(), rect.getY(), ii.getHeight() * iar, ii.getHeight(), image);
				} else {
					drawImage(rect.getX(), rect.getY(), ii.getWidth(), ii.getWidth() / iar, image);
				}
			}
		} catch (final IOException ioe) {
			LOGGER.warn("Unable to left-align image {}: {}", image, ioe.getMessage());
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (final IOException ignored) {
					// ignore
				}
			}
		}
	}

	public void drawImageRight(final double x, final double y, final double w, final double h, final URL image) {
		drawImageRight(new Rectangle2D.Double(x, y, w, h), image);
	}

	public void drawImageRight(final Rectangle2D rect, final URL image) {
		if (image == null) {
			return;
		}

		InputStream in = null;
		try {
			in = image.openStream();
			final ImageInfo ii = new ImageInfo();
			ii.setInput(in);
			if (ii.check()) {
				final double iar = (double) ii.getWidth() / (double) ii.getHeight();
				final double rar = rect.getWidth() / rect.getHeight();
				if (rar > iar) {
					drawImage(rect.getMaxX() - ii.getHeight() * iar, rect.getY(), ii.getHeight() * iar, ii.getHeight(),
					        image);
				} else {
					drawImage(rect.getMaxX() - ii.getWidth(), rect.getY(), ii.getWidth(), ii.getWidth() / iar, image);
				}
			}
		} catch (final IOException ioe) {
			LOGGER.warn("Unable to right-align image {}: {}", image, ioe.getMessage());
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (final IOException ignored) {
					// ignore
				}
			}
		}
	}

	public void drawLine(final double x1, final double y1, final double x2, final double y2) {
		drawLine(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2));
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawLine(final Point2D start, final Point2D end) {
		driver.drawLine(start, end);
	}

	public void drawOval(final double x, final double y, final double w, final double h) {
		drawOval(new Rectangle2D.Double(x, y, w, h));
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawOval(final Rectangle2D bounds) {
		driver.drawOval(bounds);
	}

	public void drawPoint(final double x, final double y) {
		drawPoint(new Point2D.Double(x, y));
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawPoint(final Point2D point) {
		driver.drawPoint(point);
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawPolygon(final List<Point2D> points) {
		driver.drawPolygon(points);
	}

	public void drawRectangle(final double x, final double y, final double w, final double h) {
		drawRectangle(new Rectangle2D.Double(x, y, w, h));
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawRectangle(final Rectangle2D rect) {
		driver.drawRectangle(rect);
	}

	public void drawString(final double x, final double y, final Font font, final String string) {
		drawString(new Point2D.Double(x, y), font, string);
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawString(final Point2D point, final Font font, final String string) {
		driver.drawString(point, font, string);
	}

	public void drawStringCenter(final double x, final double y, final double w, final double h, final Font font,
	        final String string) {
		drawStringCenter(new Rectangle2D.Double(x, y, w, h), font, string);
	}

	public void drawStringCenter(final Rectangle2D bounds, final Font font, final String string) {
		final Rectangle2D sb = getStringBounds(font, string);
		drawString(bounds.getCenterX() - sb.getWidth() / 2, bounds.getCenterY() - (double) font.getSize() / 2, font,
		        string);
	}

	public void drawStringLeft(final double x, final double y, final double w, final double h, final Font font,
	        final String string) {
		drawStringLeft(new Rectangle2D.Double(x, y, w, h), font, string);
	}

	public void drawStringLeft(final Rectangle2D bounds, final Font font, final String string) {
		drawString(bounds.getMinX(), bounds.getCenterY() - (double) font.getSize() / 2, font, string);
	}

	public void drawStringRight(final double x, final double y, final double w, final double h, final Font font,
	        final String string) {
		drawStringRight(new Rectangle2D.Double(x, y, w, h), font, string);
	}

	public void drawStringRight(final Rectangle2D bounds, final Font font, final String string) {
		final Rectangle2D sb = getStringBounds(font, string);
		drawString(bounds.getMaxX() - sb.getWidth(), bounds.getCenterY() - (double) font.getSize() / 2, font, string);
	}

	public void fillArc(final double x, final double y, final double w, final double h, final double start,
	        final double extent, final ArcStyle style) {
		fillArc(new Rectangle2D.Double(x, y, w, h), start, extent, style);
	}

	/**
	 * {@inheritDoc}
	 */
	public void fillArc(final Rectangle2D bounds, final double start, final double extent, final ArcStyle style) {
		driver.fillArc(bounds, start, extent, style);
	}

	public void fillOval(final double x, final double y, final double w, final double h) {
		fillOval(new Rectangle2D.Double(x, y, w, h));
	}

	/**
	 * {@inheritDoc}
	 */
	public void fillOval(final Rectangle2D bounds) {
		driver.fillOval(bounds);
	}

	/**
	 * {@inheritDoc}
	 */
	public void fillPolygon(final List<Point2D> points) {
		driver.fillPolygon(points);
	}

	public void fillRectangle(final double x, final double y, final double w, final double h) {
		fillRectangle(new Rectangle2D.Double(x, y, w, h));
	}

	/**
	 * {@inheritDoc}
	 */
	public void fillRectangle(final Rectangle2D rect) {
		driver.fillRectangle(rect);
	}

	/**
	 * {@inheritDoc}
	 */
	public Rectangle2D getClip() {
		return driver.getClip();
	}

	/**
	 * {@inheritDoc}
	 */
	public Fill getFill() {
		return driver.getFill();
	}

	/**
	 * {@inheritDoc}
	 */
	public Color getLineColor() {
		return driver.getLineColor();
	}

	/**
	 * {@inheritDoc}
	 */
	public LineStyle getLineStyle() {
		return driver.getLineStyle();
	}

	/**
	 * {@inheritDoc}
	 */
	public int getLineThickness() {
		return driver.getLineThickness();
	}

	/**
	 * {@inheritDoc}
	 */
	public Rectangle2D getStringBounds(final Font font, final String string) {
		return driver.getStringBounds(font, string);
	}

	/**
	 * {@inheritDoc}
	 */
	public void popState() {
		driver.popState();
	}

	/**
	 * {@inheritDoc}
	 */
	public void popTransform() {
		driver.popTransform();
	}

	/**
	 * {@inheritDoc}
	 */
	public void pushState() {
		driver.pushState();
	}

	/**
	 * {@inheritDoc}
	 */
	public void pushTransform(final AffineTransform transform) {
		driver.pushTransform(transform);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setClip(final Rectangle2D r) {
		driver.setClip(r);
	}

	/**
	 * Sets the driver for this GraphicsContext.
	 * 
	 * @param driver
	 *            the driver.
	 */
	public void setDriver(final Driver driver) {
		this.driver = driver;
	}

	public void setFill(final Color color) {
		setFill(new ColorFill(color));
	}

	public void setFill(final Color start, final Color end, final boolean horizontal) {
		setFill(new GradientFill(start, end, horizontal));
	}

	public void setFill(final Fill... fills) {
		setFill(new MultiFill(fills));
	}

	/**
	 * {@inheritDoc}
	 */
	public void setFill(final Fill fill) {
		driver.setFill(fill);
	}

	public void setFill(final URL image) {
		setFill(new TextureFill(image));
	}

	/**
	 * {@inheritDoc}
	 */
	public void setLineColor(final Color color) {
		driver.setLineColor(color);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setLineStyle(final LineStyle style) {
		driver.setLineStyle(style);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setLineThickness(final int thickness) {
		driver.setLineThickness(thickness);
	}
}
