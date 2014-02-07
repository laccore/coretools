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

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.andrill.coretools.Platform;
import org.andrill.coretools.geology.models.Image;
import org.andrill.coretools.geology.models.Length;
import org.andrill.coretools.graphics.util.ImageInfo;
import org.andrill.coretools.model.ModelContainer;

/**
 * A script for import images from a directory and writing out in a Core Tools model container format.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class ImportImages {

	public static void main(final String[] args) throws Exception {
		// our options
		OptionParser parser = new OptionParser() {
			{
				accepts("in", "the directory of images to import [R]").withRequiredArg().ofType(File.class);
				accepts("out", "the output file [R]").withRequiredArg().ofType(File.class);
				accepts("parse-depth", "parse the top depth from filename [O]");
				accepts("depth", "the starting depth [O]").withRequiredArg().ofType(Double.class);
				accepts("parse-base", "parse the base depth from filename [O]");
				accepts("dpi", "the DPI [O]").withRequiredArg().ofType(Integer.class);
				accepts("group", "the image group [O]").withRequiredArg().ofType(String.class);
				acceptsAll(Arrays.asList("h", "?"), "show help");
			}
		};
		String[] required = { "in", "out" };

		// parse our options
		OptionSet options = ScriptUtils.checkArgs(parser, required, args);

		// start the platform
		Platform.start();

		// parse our other options
		File in = (File) options.valueOf("in");
		File out = (File) options.valueOf("out");
		double depth = 0.0;
		if (options.hasArgument("depth")) {
			depth = (Double) options.valueOf("depth");
		} else if (options.has("parse-depth")) {
			depth = -1.0;
		}
		int dpi = 254;
		if (options.hasArgument("dpi")) {
			dpi = (Integer) options.valueOf("dpi");
		} else if (options.has("parse-base")) {
			dpi = -1;
		}
		String group = null;
		if (options.hasArgument("group")) {
			group = (String) options.valueOf("group");
		}

		DecimalFormat dec = new DecimalFormat("0.00");

		// list the images
		File[] images = in.listFiles(new FilenameFilter() {
			public boolean accept(final File dir, final String name) {
				String lower = name.toLowerCase();
				return (lower.endsWith(".bmp") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
						|| lower.endsWith(".png") || lower.endsWith(".tif") || lower.endsWith(".tiff"));
			}
		});
		if ((images == null) || (images.length == 0)) {
			System.err.println("No images found in " + in);
			System.exit(1);
		}

		// import the images
		List<Image> list = new ArrayList<Image>();
		Pattern regex = Pattern.compile("([0-9]*\\.[0-9]+)");
		for (File f : images) {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(f);
				ImageInfo ii = new ImageInfo();
				ii.setInput(fis);
				if (ii.check()) {
					Image image = new Image();
					image.setPath(new URL("file:" + f.getAbsolutePath()));
					image.setGroup(group);

					// parse top
					Matcher match = regex.matcher(f.getName());
					if (depth < 0) {
						if (match.find()) {
							image.setTop(new Length(match.group(0) + " m"));
						} else {
							image.setTop(new Length(new BigDecimal(0), " m"));
						}
					} else {
						image.setTop(new Length(dec.format(depth) + " m"));
					}

					// parse base
					if (dpi < 0) {
						if (match.find()) {
							image.setBase(new Length(match.group(0) + " m"));
						} else {
							Length l = new Length(dec.format(ii.getPhysicalHeightInch()) + " in").to("m");
							image.setBase(new Length(dec.format(l.getValue().add(image.getTop().getValue())) + " m"));
						}
					} else {
						Length l = new Length(dec.format((double) ii.getHeight() / (double) dpi) + " in").to("m");
						image.setBase(new Length(dec.format(l.getValue().add(image.getTop().getValue())) + " m"));
					}
					if (depth >= 0) {
						depth = image.getBase().getValue().doubleValue();
					}
					list.add(image);
				}
			} finally {
				if (fis != null) {
					fis.close();
				}
			}
		}

		// sort and add to the container
		ModelContainer container = Platform.getService(ModelContainer.class);
		for (Image i : list) {
			container.add(i);
		}

		// write out the results
		ScriptUtils.write(container, out);
	}
}
