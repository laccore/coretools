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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.andrill.coretools.graphics.driver.Java2DDriver;
import org.apache.batik.svggen.CachedImageHandlerBase64Encoder;
import org.apache.batik.svggen.GenericImageHandler;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.util.Base64EncoderStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

/**
 * A SVGGraphics renders graphics to an SVG image.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class SVGGraphics extends GraphicsContext {
	private static final Logger LOGGER = LoggerFactory.getLogger(SVGGraphics.class);
	protected SVGGraphics2D svg2d;

	/**
	 * Create a new SVGGraphics. 
	 */
	public SVGGraphics() {
		// create a DOM document
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder buidler;
		try {
			buidler = factory.newDocumentBuilder();
		} catch (final ParserConfigurationException e) {
			LOGGER.error("Unable to create a DocumentBuilder {}", e);
			throw new RuntimeException("Unable to create a Document Builder");
		}
		final DOMImplementation dom = buidler.getDOMImplementation();

		// Create an instance of org.w3c.dom.Document.
		final String svgNS = "http://www.w3.org/2000/svg";
		final Document document = dom.createDocument(svgNS, "svg", null);

		// Reuse our embedded base64-encoded image data.
		final SVGGeneratorContext context = SVGGeneratorContext.createDefault(document);
		final GenericImageHandler ihandler = new CachedImageHandlerBase64Encoder() {
			@Override
			public void encodeImage(final BufferedImage buf, final OutputStream os) throws IOException {
				final Base64EncoderStream b64Encoder = new Base64EncoderStream(os);
				ImageIO.write(buf, "PNG", b64Encoder);
				b64Encoder.close();

			}
		};
		context.setGenericImageHandler(ihandler);

		// Create an instance of the SVG Generator.
		svg2d = new SVGGraphics2D(context, false);
		driver = new Java2DDriver(svg2d);
	}

	/**
	 * Writes the graphics to the specified SVG image file.
	 * 
	 * @param file
	 *            the file.
	 * @throws IOException
	 *             thrown if any error occurs.
	 */
	public void write(File file) throws IOException {
		OutputStream out = null;
		try {
	        out = new FileOutputStream(file);
	        write(out);
        } finally {
	        if (out != null) {
	        	out.close();
	        }
        }
	}
	
	public void write(OutputStream out) throws IOException {
		try {
			svg2d.stream(new OutputStreamWriter(out), true);
		} finally {
			driver.dispose();
		}
	}
}
