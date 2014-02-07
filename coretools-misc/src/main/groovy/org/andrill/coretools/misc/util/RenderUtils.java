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
package org.andrill.coretools.misc.util;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;

import org.andrill.coretools.graphics.GraphicsContext;
import org.andrill.coretools.graphics.PDFGraphics;
import org.andrill.coretools.graphics.RasterGraphics;
import org.andrill.coretools.graphics.SVGGraphics;
import org.andrill.coretools.graphics.util.Paper;
import org.andrill.coretools.scene.PageableScene;
import org.andrill.coretools.scene.Scene;

/**
 * Various rendering-related utility methods.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class RenderUtils {
	/**
	 * Render the scene as a multi-page PDF.
	 * 
	 * @param scene
	 *            the scene.
	 * @param paper
	 *            the paper.
	 * @param start
	 *            the start depth/height.
	 * @param end
	 *            the end depth/height.
	 * @param pageSize
	 *            the page size.
	 * @param renderHeader
	 *            the render header flag.
	 * @param renderFooter
	 *            the render footer flag.
	 * @param file
	 *            the file.
	 */
	public static void renderPDF(final Scene scene, final Paper paper, final double start, final double end,
	        final double pageSize, final boolean renderHeader, final boolean renderFooter, final File file) {
		// wrap the scene to make it pageable
		PageableScene pageable = new PageableScene(scene, paper, start, pageSize, renderHeader, renderFooter);
		pageable.validate();

		// render our PDF
		PDFGraphics pdf = new PDFGraphics(file, paper);
		int pages = (int) Math.max(Math.ceil((end - start) / pageSize), 1);
		for (int i = 1; i <= pages; i++) {
			GraphicsContext page = pdf.newPage();

			// render header
			if (renderHeader) {
				pageable.renderHeader(i, page);
				page.pushTransform(AffineTransform.getTranslateInstance(0, pageable.getHeaderSize().getHeight() - 1));
			}

			// render contents
			pageable.renderContents(i, page);
			page.pushTransform(AffineTransform.getTranslateInstance(0, pageable.getContentSize(i).getHeight() - 1));

			// render footer
			if (renderFooter) {
				pageable.renderFooter(i, page);
			}
		}
		pdf.write();
	}

	/**
	 * Render the scene as a series of raster images.
	 * 
	 * @param scene
	 *            the scene.
	 * @param paper
	 *            the paper.
	 * @param start
	 *            the start depth/height.
	 * @param end
	 *            the end depth/height.
	 * @param pageSize
	 *            the page size.
	 * @param renderHeader
	 *            the render header flag.
	 * @param renderFooter
	 *            the render footer flag.
	 * @param file
	 *            the file.
	 */
	public static void renderRaster(final Scene scene, final Paper paper, final double start, final double end,
	        final double pageSize, final boolean renderHeader, final boolean renderFooter, final File file)
	        throws IOException {
		// wrap the scene to make it pageable
		PageableScene pageable = new PageableScene(scene, paper, start, pageSize, renderHeader, renderFooter);
		pageable.validate();

		// figure out the raster type and render each page as a separate raster
		String extension = file.getName().substring(file.getName().lastIndexOf('.'));
		int pages = (int) Math.max(Math.ceil((end - start) / pageSize), 1);
		for (int i = 1; i <= pages; i++) {
			RasterGraphics page = new RasterGraphics(paper.getPrintableWidth() + 1, paper.getPrintableHeight() - 1, true);

			// render header
			if (renderHeader) {
				pageable.renderHeader(i, page);
				page.pushTransform(AffineTransform.getTranslateInstance(0, pageable.getHeaderSize().getHeight() - 1));
			}

			// render contents
			pageable.renderContents(i, page);
			page.pushTransform(AffineTransform.getTranslateInstance(0, pageable.getContentSize(i).getHeight() - 1));

			// render footer
			if (renderFooter) {
				pageable.renderFooter(i, page);
			}

			page.write(new File(file.getParentFile(), file.getName().replace(extension, "-" + i + extension)));
		}
	}

	/**
	 * Render the scene as a series of SVG images.
	 * 
	 * @param scene
	 *            the scene.
	 * @param paper
	 *            the paper.
	 * @param start
	 *            the start depth/height.
	 * @param end
	 *            the end depth/height.
	 * @param pageSize
	 *            the page size.
	 * @param renderHeader
	 *            the render header flag.
	 * @param renderFooter
	 *            the render footer flag.
	 * @param file
	 *            the file.
	 */
	public static void renderSVG(final Scene scene, final Paper paper, final double start, final double end,
	        final double pageSize, final boolean renderHeader, final boolean renderFooter, final File file)
	        throws IOException {
		PageableScene pageable = new PageableScene(scene, paper, start, pageSize, renderHeader, renderFooter);
		pageable.validate();

		// render each page as a separate SVG file
		int pages = (int) Math.max(Math.ceil((end - start) / pageSize), 1);
		for (int i = 1; i <= pages; i++) {
			SVGGraphics page = new SVGGraphics();

			// render header
			if (renderHeader) {
				pageable.renderHeader(i, page);
				page.pushTransform(AffineTransform.getTranslateInstance(0, pageable.getHeaderSize().getHeight() - 1));
			}

			// render contents
			pageable.renderContents(i, page);
			page.pushTransform(AffineTransform.getTranslateInstance(0, pageable.getContentSize(i).getHeight() - 1));

			// render footer
			if (renderFooter) {
				pageable.renderFooter(i, page);
			}

			page.write(new File(file.getParentFile(), file.getName().replace(".svg", "-" + i + ".svg")));
		}
	}

	private RenderUtils() {
		// not to be instantiated
	}
}
