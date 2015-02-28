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
package org.andrill.coretools.graphics.driver.pdf;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.imageio.ImageIO;

import org.andrill.coretools.Platform;
import org.andrill.coretools.graphics.GraphicsContext;
import org.andrill.coretools.graphics.RasterGraphics;
import org.andrill.coretools.graphics.driver.Driver;
import org.andrill.coretools.graphics.fill.ColorFill;
import org.andrill.coretools.graphics.fill.Fill;
import org.andrill.coretools.graphics.util.Paper;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.DefaultFontMapper;
import com.lowagie.text.pdf.FontMapper;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

/**
 * A driver implementation for iText.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class PDFDriver implements Driver {
	public class Arc extends Shape {
		Rectangle2D r;
		double start, extent;
		ArcStyle style;

		public Arc(final Rectangle2D r, final double start, final double extent, final ArcStyle style) {
			this.r = r;
			this.start = start;
			this.extent = extent;
			this.style = style;
		}

		@Override
		public void outline(final PdfContentByte content) {
			switch (style) {
				case OPEN:
					outlineOpen(content);
					break;
				case CLOSED:
					outlineClosed(content);
					break;
				case SECTOR:
					outlineSector(content);
					break;
			}
		}

		protected void outlineClosed(final PdfContentByte content) {
			content.arc(x(r.getMinX()), y(r.getMinY()), x(r.getMaxX()), y(r.getMaxY()), (float) start, (float) extent);
			content.closePath();
		}

		protected void outlineOpen(final PdfContentByte content) {
			content.arc(x(r.getMinX()), y(r.getMinY()), x(r.getMaxX()), y(r.getMaxY()), (float) start, (float) extent);
		}

		protected void outlineSector(final PdfContentByte content) {
			content.arc(x(r.getMinX()), y(r.getMinY()), x(r.getMaxX()), y(r.getMaxY()), (float) start, (float) extent);
			content.lineTo(x(r.getCenterX()), y(r.getCenterY()));
			content.closePath();
		}
	}

	public class Line extends Shape {
		Point2D p1, p2;

		public Line(final Point2D p2, final Point2D p1) {
			this.p1 = p1;
			this.p2 = p2;
		}

		@Override
		public void outline(final PdfContentByte content) {
			content.moveTo(x(p1.getX()), y(p1.getY()));
			content.lineTo(x(p2.getX()), y(p2.getY()));
		}
	}

	public class Oval extends Shape {
		Rectangle2D r;

		public Oval(final Rectangle2D rect) {
			r = rect;
		}

		@Override
		public void outline(final PdfContentByte content) {
			content.ellipse(x(r.getMinX()), y(r.getMinY()), x(r.getMaxX()), y(r.getMaxY()));
		}
	}

	public class Polygon extends Shape {
		List<Point2D> points;

		public Polygon(final List<Point2D> points) {
			this.points = points;
		}

		@Override
		public void outline(final PdfContentByte content) {
			if (points.size() > 1) {
				Point2D p = points.get(0);
				content.moveTo(x(p.getX()), y(p.getY()));
				for (int i = 1; i < points.size(); i++) {
					p = points.get(i);
					content.lineTo(x(p.getX()), y(p.getY()));
				}
				content.closePath();
			}
		}
	}

	public class Rectangle extends Shape {
		Rectangle2D r;

		public Rectangle(final Rectangle2D rect) {
			r = rect;
		}

		@Override
		public void outline(final PdfContentByte content) {
			content.rectangle(x(r.getMinX()), y(r.getMaxY()), (float) r.getWidth(), (float) r.getHeight());
		}
	}

	public abstract class Shape {
		public abstract void outline(PdfContentByte content);
	}

	protected static class State {
		Fill fill = null;
		Color lineColor = null;
		LineStyle lineStyle = LineStyle.SOLID;
		int lineThickness = 1;
	}

	protected static final float LINE_DASH[] = { 18, 9 };
	protected static final float LINE_DASH_DOT[] = { 9, 3, 3, 3 };
	protected static final float LINE_DOT[] = { 3, 3 };

	private static void drawTest(final GraphicsContext graphics) throws Exception {
		Platform.start();

		// point, line, rectangle
		graphics.drawPoint(350, 0);
		graphics.drawLine(0, 0, 300, 0);
		graphics.drawRectangle(400, 0, 50, 100);

		graphics.setLineThickness(5);
		graphics.drawPoint(350, 10);
		graphics.drawLine(0, 10, 300, 10);
		graphics.drawRectangle(410, 10, 50, 100);

		graphics.setLineColor(Color.blue);
		graphics.drawPoint(350, 20);
		graphics.drawLine(0, 20, 300, 20);
		graphics.drawRectangle(420, 20, 50, 100);

		graphics.setLineStyle(LineStyle.DASHED);
		graphics.setLineColor(Color.black);
		graphics.setLineThickness(1);
		graphics.drawPoint(350, 30);
		graphics.drawLine(0, 30, 300, 30);
		graphics.drawRectangle(430, 30, 50, 100);

		graphics.setLineStyle(LineStyle.DOTTED);
		graphics.drawPoint(350, 40);
		graphics.drawLine(0, 40, 300, 40);
		graphics.drawRectangle(440, 40, 50, 100);

		graphics.setLineStyle(LineStyle.DASH_DOTTED);
		graphics.drawPoint(350, 50);
		graphics.drawLine(0, 50, 300, 50);
		graphics.drawRectangle(450, 50, 50, 100);

		graphics.setLineStyle(LineStyle.SOLID);
		graphics.drawPoint(350, 60);
		graphics.drawLine(0, 60, 300, 60);
		graphics.drawRectangle(460, 60, 50, 100);

		// arc, circles, ovals
		graphics.drawArc(0, 100, 100, 50, 0, 30, ArcStyle.SECTOR);
		graphics.drawArc(0, 100, 100, 50, 35, 30, ArcStyle.OPEN);
		graphics.drawArc(0, 100, 100, 50, 70, 90, ArcStyle.CLOSED);

		graphics.drawOval(150, 100, 100, 50);

		graphics.setLineStyle(LineStyle.DASHED);
		graphics.setLineColor(Color.blue);
		graphics.setLineThickness(3);
		graphics.drawOval(300, 100, 50, 50);

		// polygon
		graphics.setLineColor(Color.black);
		graphics.setLineStyle(LineStyle.SOLID);
		graphics.setLineThickness(1);
		List<Point2D> points = new ArrayList<Point2D>();
		points.add(new Point2D.Double(0, 200));
		points.add(new Point2D.Double(300, 200));
		points.add(new Point2D.Double(100, 220));
		points.add(new Point2D.Double(100, 230));
		points.add(new Point2D.Double(50, 240));
		points.add(new Point2D.Double(50, 250));
		points.add(new Point2D.Double(0, 250));
		graphics.drawPolygon(points);

		// text
		graphics.drawString(350, 250, new Font("SanSerif", Font.BOLD, 12), "drawString");

		Rectangle2D left = new Rectangle2D.Double(0, 300, 100, 50);
		graphics.drawRectangle(left);
		graphics.drawStringLeft(left, new Font("Serif", Font.ITALIC, 14), "Left");

		Rectangle2D center = new Rectangle2D.Double(150, 300, 100, 50);
		graphics.drawRectangle(center);
		graphics.drawStringCenter(center, new Font("Serif", Font.PLAIN, 14), "Center");

		Rectangle2D right = new Rectangle2D.Double(300, 300, 100, 50);
		graphics.drawRectangle(right);
		graphics.drawStringRight(right, new Font("Dialog", Font.PLAIN, 14), "Right");

		// images
		URL image = new File("icon.png").toURI().toURL();

		graphics.drawImage(new Point2D.Double(0, 400), image);

		Rectangle2D rect = new Rectangle2D.Double(300, 400, 50, 50);
		graphics.drawImage(rect, image);
		graphics.drawRectangle(rect);
	}

	public static void main(final String[] args) throws Exception {
		Paper paper = Paper.getDefault();

		// raster graphics
		RasterGraphics raster = new RasterGraphics(paper.getWidth(), paper.getHeight(), true);
		raster.pushTransform(AffineTransform.getTranslateInstance(36, 36));
		drawTest(raster);
		raster.write(new File("Test.png"));

		// pdf graphics
		Document document = new Document(new com.lowagie.text.Rectangle(paper.getWidth(), paper.getHeight()));
		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream("Test.pdf"));
		document.open();
		drawTest(new GraphicsContext(new PDFDriver(writer.getDirectContent(), paper)));
		document.close();
		writer.close();
	}

	protected FontMapper fontMapper = new DefaultFontMapper();
	protected final PdfContentByte content;
	protected final Paper paper;
	protected Fill fill = new ColorFill(Color.white);
	protected Color lineColor = Color.black;
	protected LineStyle lineStyle = LineStyle.SOLID;
	protected int lineThickness = 1;
	protected Map<String, Image> imageCache = new HashMap<String, Image>();
	protected Stack<AffineTransform> transformStack = new Stack<AffineTransform>();
	protected Stack<State> stateStack = new Stack<State>();

	/**
	 * Create a new PDF driver.
	 * 
	 * @param content
	 *            the content.
	 * @param paper
	 *            the paper.
	 */
	public PDFDriver(final PdfContentByte content, final Paper paper) {
		this.content = content;
		this.paper = paper;
		// AffineTransform tx = AffineTransform.getScaleInstance(1.0, -1.0);
		// tx.translate(paper.getPrintableX(), -paper.getHeight() + paper.getPrintableY());
		// content.transform(tx);
	}

	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		// do nothing
	}

	protected void draw(final Shape shape) {
		content.setLineWidth(lineThickness);
		content.setColorStroke(lineColor);
		switch (lineStyle) {
			case SOLID:
				content.setLineDash(1);
				break;
			case DASHED:
				content.setLineDash(LINE_DASH, 0);
				break;
			case DOTTED:
				content.setLineDash(LINE_DOT, 0);
				break;
			case DASH_DOTTED:
				content.setLineDash(LINE_DASH_DOT, 0);
				break;
		}
		shape.outline(content);
		content.stroke();
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawArc(final Rectangle2D bounds, final double start, final double extent, final ArcStyle style) {
		draw(new Arc(bounds, start, extent, style));
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawImage(final Point2D point, final URL url) {
		Image image = getImage(url);
		try {
			content.addImage(image, image.getWidth(), 0, 0, image.getHeight(), x(point.getX()), y(point.getY()), true);
		} catch (DocumentException e) {
			// TODO log
			e.printStackTrace();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawImage(final Rectangle2D rect, final URL url) {
		Image image = getImage(url);
		try {
			content.addImage(image, (float) rect.getWidth(), 0, 0, (float) rect.getHeight(), x(rect.getX()), y(rect
			        .getY()), true);
		} catch (DocumentException e) {
			// TODO log
			e.printStackTrace();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void embedImage(final Rectangle2D rect, final URL url) {
		// 2/15/2015 brg: to satisfy Driver - if we get PDFDriver up and running for PDF
		// export (we currently use Java2DDriver), we can remove Driver.embedImage(), since
		// this.drawImage() correctly embeds the full-resolution image instead of the scaled
		// image from the cache
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawLine(final Point2D start, final Point2D end) {
		draw(new Line(start, end));
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawOval(final Rectangle2D bounds) {
		draw(new Oval(bounds));
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawPoint(final Point2D point) {
		double offset = lineThickness / 2.0;
		draw(new Line(new Point2D.Double(point.getX() - offset, point.getY()), new Point2D.Double(
		        point.getX() + offset, point.getY())));
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawPolygon(final List<Point2D> points) {
		draw(new Polygon(points));
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawRectangle(final Rectangle2D rect) {
		draw(new Rectangle(rect));
	}

	/**
	 * {@inheritDoc}
	 */
	public void drawString(final Point2D point, final Font font, final String string) {
		content.setFontAndSize(fontMapper.awtToPdf(font), font.getSize2D());
		content.beginText();
		content.showTextAligned(PdfContentByte.ALIGN_LEFT, string, x(point.getX()), y(point.getY() + font.getSize2D()),
		        0);
		content.endText();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void drawStringRotated(final Point2D point, final Font font, final String string, final double theta) {
		// stub for now to satisfy compiler, PDFDriver isn't complete
	}

	protected void fill(final Shape shape) {

	}

	/**
	 * {@inheritDoc}
	 */
	public void fillArc(final Rectangle2D bounds, final double start, final double extent, final ArcStyle style) {
		fill(new Arc(bounds, start, extent, style));
	}

	/**
	 * {@inheritDoc}
	 */
	public void fillOval(final Rectangle2D bounds) {
		fill(new Oval(bounds));
	}

	/**
	 * {@inheritDoc}
	 */
	public void fillPolygon(final List<Point2D> points) {
		fill(new Polygon(points));
	}

	/**
	 * {@inheritDoc}
	 */
	public void fillRectangle(final Rectangle2D rect) {
		fill(new Rectangle(rect));
	}

	/**
	 * {@inheritDoc}
	 */
	public Rectangle2D getClip() {
		// TODO
		return null;
	}

	public Fill getFill() {
		return fill;
	}

	protected Image getImage(final URL url) {
		String key = url.toExternalForm();
		if (!imageCache.containsKey(key)) {
			try {
				// Image image = Image.getInstance(url);
				Image image = Image.getInstance(content, ImageIO.read(url), 0);
				imageCache.put(key, image);
			} catch (BadElementException e) {
				// TODO log
				e.printStackTrace();
			} catch (MalformedURLException e) {
				// TODO log
				e.printStackTrace();
			} catch (IOException e) {
				// TODO log
				e.printStackTrace();
			}
		}
		return imageCache.get(key);
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
		content.setFontAndSize(fontMapper.awtToPdf(font), font.getSize2D());
		float w = content.getEffectiveStringWidth(string, false);
		float h = font.getSize();
		return new Rectangle2D.Double(0, h, w, h);
	}

	/**
	 * {@inheritDoc}
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
		// TODO
	}

	/**
	 * {@inheritDoc}
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
		// TODO
	}

	/**
	 * {@inheritDoc}
	 */
	public void setClip(final Rectangle2D r) {
		// TODO
	}

	/**
	 * {@inheritDoc}
	 */
	public void setFill(final Fill fill) {
		this.fill = fill;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setLineColor(final Color lineColor) {
		this.lineColor = lineColor;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setLineStyle(final LineStyle lineStyle) {
		this.lineStyle = lineStyle;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setLineThickness(final int lineThickness) {
		this.lineThickness = lineThickness;
	}

	protected float x(final double x) {
		return (float) x + paper.getPrintableX();
	}

	protected float y(final double y) {
		return paper.getPrintableHeight() - (float) y + paper.getPrintableY();
	}
}
