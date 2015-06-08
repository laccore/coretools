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
package org.andrill.coretools.scripting;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.andrill.coretools.Platform;
import org.andrill.coretools.ResourceLoader;
import org.andrill.coretools.graphics.util.Paper;
import org.andrill.coretools.misc.util.RenderUtils;
import org.andrill.coretools.model.ModelContainer;
import org.andrill.coretools.scene.DefaultScene;
import org.andrill.coretools.scene.Scene;

/**
 * Renders a diagram from the command line.
 * 
 * @author Josh Reed (jareed@andrill.org)
 * 
 */
public class Render {

	/**
	 * @param args
	 */
	@SuppressWarnings("unchecked")
	public static void main(final String[] args) throws Exception {
		// setup our options
		OptionParser parser = new OptionParser() {
			{
				accepts("diagram", "a scene file [R]").withRequiredArg().ofType(File.class);
				accepts("in", "a container data file [R]").withRequiredArg().ofType(File.class);
				accepts("format", "'pdf', 'svg', 'jpeg', 'png', or 'bmp' [R]").withRequiredArg();
				accepts("out", "the output file [R]").withRequiredArg().ofType(File.class);
				accepts("paper", "'letter', 'a4', etc [O]").withRequiredArg();
				accepts("range", "<top>-<base>[@<pageSize>] [O]").withRequiredArg();
				accepts("no-header", "don't render the header [0]");
				accepts("no-footer", "don't render the footer [0]");
				accepts("resources", "the resources directory [O]").withRequiredArg().ofType(File.class);
				acceptsAll(Arrays.asList("h", "?"), "show help");
			}
		};
		String[] required = { "diagram", "in", "format", "out" };

		// parse our options
		OptionSet options = ScriptUtils.checkArgs(parser, required, args);

		// start the platform
		Platform.start();

		// build and populate our scene
		ModelContainer container = Platform.getService(ModelContainer.class);
		ScriptUtils.read(container, (List<File>) options.valuesOf("in"));

		// create our scene
		//Scene scene = SceneXML.read(options.valueOf("diagram").toString());
		Scene scene = new DefaultScene();
		scene.setModels(container);
		scene.setScalingFactor(100);
		scene.validate();

		// parse our option arguments
		File out = (File) options.valueOf("out");
		Paper paper = Paper.get((String) options.valueOf("paper"));
		double start, end, pageSize;
		if (options.hasArgument("range")) {
			String[] split = ((String) options.valueOf("range")).replaceAll("[-@]", " ").split(" ");
			start = Double.valueOf(split[0]);
			end = Double.valueOf(split[1]);
			if (split.length == 3) {
				pageSize = Double.valueOf(split[2]);
			} else {
				pageSize = end - start;
			}
		} else {
			Rectangle2D contents = scene.getContentSize();
			start = contents.getMinY() / scene.getScalingFactor();
			end = contents.getMaxY() / scene.getScalingFactor();
			pageSize = contents.getHeight() / scene.getScalingFactor();
		}
		boolean renderHeader = !options.has("no-header");
		boolean renderFooter = !options.has("no-footer");
		if (options.hasArgument("resources")) {
			File resources = (File) options.valueOf("resources");
			if (resources.exists() && resources.isDirectory()) {
				File[] files = resources.listFiles(new FilenameFilter() {
					public boolean accept(final File dir, final String name) {
						return name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".zip");
					}
				});
				if (files != null) {
					ResourceLoader loader = Platform.getService(ResourceLoader.class);
					for (File f : files) {
						loader.addResource(f.toURI().toURL());
					}
				}
			}
		}

		// render the format
		String format = options.valueOf("format").toString().toLowerCase();
		if ("pdf".equals(format)) {
			RenderUtils.renderPDF(scene, paper, start, end, pageSize, renderHeader, renderFooter, "", out);
		} else if ("svg".equals(format)) {
			RenderUtils.renderSVG(scene, paper, start, end, pageSize, renderHeader, renderFooter, "", out);
		} else if (Arrays.asList("jpeg", "png", "bmp").contains(format)) {
			RenderUtils.renderRaster(scene, paper, start, end, pageSize, renderHeader, renderFooter, "", out);
		} else {
			System.err.println("Unrecognized output format: " + format);
		}
	}
}
