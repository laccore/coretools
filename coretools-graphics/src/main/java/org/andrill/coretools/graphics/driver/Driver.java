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

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.List;

import org.andrill.coretools.graphics.fill.Fill;

/**
 * The interface for a 2D graphics driver.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public interface Driver {
	/**
	 * Arc styles.
	 */
	public enum ArcStyle {
		CLOSED, OPEN, SECTOR
	}

	/**
	 * Line styles.
	 */
	public enum LineStyle {
		DASH_DOTTED, DASHED, DOTTED, SOLID
	}

	/**
	 * Disposes any resources that need disposing.
	 */
	void dispose();

	/**
	 * Draws an arc.
	 * 
	 * @param bounds
	 *            the bounds of the arc.
	 * @param start
	 *            the starting angle.
	 * @param extent
	 *            the angle extents.
	 * @param style
	 *            the arc style.
	 */
	void drawArc(Rectangle2D bounds, double start, double extent, ArcStyle style);

	/**
	 * Draw the image at the specified point.
	 * 
	 * @param point
	 *            the point.
	 * @param image
	 *            the image.
	 */
	void drawImage(Point2D point, URL image);

	/**
	 * Draws the image in the specified rectangle.
	 * 
	 * @param rect
	 *            the rectangle.
	 * @param image
	 *            the image.
	 */
	void drawImage(Rectangle2D rect, URL image);
	
	/**
	 * For use in non-raster output: embed the full-resolution image, scaled to
	 * fit the specified rectangle.
	 * 
	 * @param rect
	 *            the rectangle.
	 * @param image
	 *            the image.
	 */
	void embedImage(Rectangle2D rect, URL image);

	/**
	 * Draws a line.
	 * 
	 * @param start
	 *            the start.
	 * @param end
	 *            the end.
	 */
	void drawLine(Point2D start, Point2D end);

	/**
	 * Draws an oval.
	 * 
	 * @param bounds
	 *            the bounds of the oval.
	 */
	void drawOval(Rectangle2D bounds);

	/**
	 * Draws a point.
	 * 
	 * @param point
	 *            the point to draw.
	 */
	void drawPoint(Point2D point);

	/**
	 * Draws a polygon.
	 * 
	 * @param points
	 *            the points.
	 */
	void drawPolygon(List<Point2D> points);

	/**
	 * Draws a rectangle.
	 * 
	 * @param rect
	 *            the rectangle.
	 */
	void drawRectangle(Rectangle2D rect);

	/**
	 * Draws a string.
	 * 
	 * @param point
	 *            the point,
	 * @param font
	 *            the font.
	 * @param string
	 *            the string.
	 */
	void drawString(Point2D point, Font font, String string);

	/**
	 * Draws a string, rotated by the specified angle.
	 * 
	 * @param point
	 *            the point,
	 * @param font
	 *            the font.
	 * @param string
	 *            the string.
	 * @param theta
	 *            the angle of rotation, in radians.
	 */
	void drawStringRotated(Point2D point, Font font, String string, double theta);

	/**
	 * Fills an arc.
	 * 
	 * @param bounds
	 *            the bounds of the arc.
	 * @param start
	 *            the starting angle.
	 * @param extent
	 *            the angle extents.
	 * @param style
	 *            the arc style.
	 */
	void fillArc(Rectangle2D bounds, double start, double extent, ArcStyle style);

	/**
	 * Fills an oval.
	 * 
	 * @param bounds
	 *            the bounds of the oval.
	 */
	void fillOval(Rectangle2D bounds);

	/**
	 * Fills a polygon.
	 * 
	 * @param points
	 *            the points.
	 */
	void fillPolygon(List<Point2D> points);

	/**
	 * Fills a rectangle.
	 * 
	 * @param rect
	 *            the rectangle.
	 */
	void fillRectangle(Rectangle2D rect);

	/**
	 * Gets the clip rectangle.
	 * 
	 * @return the clip rectangle.
	 */
	Rectangle2D getClip();

	/**
	 * Gets the fill.
	 * 
	 * @return the fill.
	 */
	Fill getFill();

	/**
	 * Gets the line color.
	 * 
	 * @return the line color.
	 */
	Color getLineColor();

	/**
	 * Gets the line style.
	 * 
	 * @return the line style.
	 */
	LineStyle getLineStyle();

	/**
	 * Gets the line thickness.
	 * 
	 * @return the line thickness.
	 */
	int getLineThickness();

	/**
	 * Gets the bounds of the specified string in the specified font.
	 * 
	 * @param font
	 *            the font.
	 * @param string
	 *            the string.
	 * @return the bounds.
	 */
	Rectangle2D getStringBounds(Font font, String string);

	/**
	 * Pops the state of the driver, including the Fill and the line properties.
	 */
	void popState();

	/**
	 * Pop the top most affine transform from the transform stack.
	 */
	void popTransform();

	/**
	 * Saves the state of the driver, including the Fill and the line properties.
	 */
	void pushState();

	/**
	 * Push a new affine transform onto the transform stack. The stack follows the last-specified-first-applied rule.
	 * Calls to pushTransform() should be accompanied with a corresponding popTransform() call.
	 * 
	 * @param transform
	 *            the new transform.
	 */
	void pushTransform(AffineTransform transform);

	/**
	 * Sets the clip rectangle.
	 * 
	 * @param r
	 *            the clip rectangle.
	 */
	void setClip(Rectangle2D r);

	/**
	 * Sets the fill.
	 * 
	 * @param fill
	 *            the fill.
	 */
	void setFill(Fill fill);

	/**
	 * Sets the line color.
	 * 
	 * @param color
	 *            the line color.
	 */
	void setLineColor(Color color);

	/**
	 * Sets the line style.
	 * 
	 * @param style
	 *            the line style.
	 */
	void setLineStyle(LineStyle style);

	/**
	 * Sets the line thickness.
	 * 
	 * @param thickness
	 *            the thickness.
	 */
	void setLineThickness(int thickness);
}
