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

import java.awt.Graphics2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.andrill.coretools.graphics.driver.Java2DDriver;
import org.andrill.coretools.graphics.util.Paper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

/**
 * A PDFGraphics renders graphics to a PDF file.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class PDFGraphics {
	private static final Logger LOGGER = LoggerFactory.getLogger(PDFGraphics.class);
	protected int bottomMargin;
	protected int height;
	protected int leftMargin;
	protected int rightMargin;
	protected int topMargin;
	protected int width;
	protected File file;
	protected Document document;
	protected PdfWriter writer;
	protected PdfContentByte content;

	// state
	protected Graphics2D lastGraphics;
	protected PdfTemplate lastTemplate;
	protected GraphicsContext lastDriver;

	/**
	 * Create a new PDFGraphics.
	 * 
	 * @param file
	 *            the file.
	 * @param width
	 *            the width.
	 * @param height
	 *            the height.
	 * @param margins
	 *            the margins.
	 */
	public PDFGraphics(final File file, final int width, final int height, final int margins) {
		this.width = width;
		this.height = height;
		leftMargin = topMargin = rightMargin = bottomMargin = margins;
		this.file = file;
		init();
	}

	/**
	 * Create a new PDFGraphics.
	 * 
	 * @param file
	 *            the file.
	 * @param paper
	 *            the specified paper.
	 */
	public PDFGraphics(final File file, final Paper paper) {
		width = paper.getWidth();
		height = paper.getHeight();
		leftMargin = paper.getPrintableX();
		topMargin = paper.getPrintableY();
		rightMargin = paper.getWidth() - paper.getPrintableWidth() - paper.getPrintableX();
		bottomMargin = paper.getHeight() - paper.getPrintableHeight() - paper.getPrintableY();
		this.file = file;
		init();
	}

	protected void init() {
		document = new Document(new Rectangle(width, height));
		try {
			writer = PdfWriter.getInstance(document, new FileOutputStream(file));
			document.open();
		} catch (FileNotFoundException e) {
			LOGGER.error("Unable to create PDF document {}: {}", file.getName(), e.getMessage());
			throw new RuntimeException("Unable to create PDF document", e);
		} catch (DocumentException e) {
			LOGGER.error("Unable to create PDF document {}: {}", file.getName(), e.getMessage());
			throw new RuntimeException("Unable to create PDF document", e);
		}
		content = writer.getDirectContent();
	}

	/**
	 * Create a new page.
	 * 
	 * @return the new page.
	 */
	public GraphicsContext newPage() {
		if (lastGraphics != null) {
			lastGraphics.dispose();
		}
		if (lastDriver != null) {
			lastDriver.dispose();
		}
		if (lastTemplate != null) {
			content.addTemplate(lastTemplate, 0, 0);
			document.newPage();
		}
		lastTemplate = content.createTemplate(width, height);
		lastGraphics = lastTemplate.createGraphics(width, height);
		lastGraphics.translate(leftMargin, topMargin);
		lastDriver = new GraphicsContext(new Java2DDriver(lastGraphics));
		return lastDriver;
	}

	/**
	 * Writes the PDF.
	 */
	public void write() {
		if (lastGraphics != null) {
			lastGraphics.dispose();
		}
		if (lastDriver != null) {
			lastDriver.dispose();
		}
		if (lastTemplate != null) {
			content.addTemplate(lastTemplate, 0, 0);
		}
		document.close();
		writer.close();
	}
}
