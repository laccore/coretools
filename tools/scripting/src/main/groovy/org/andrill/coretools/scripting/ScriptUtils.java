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
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.andrill.coretools.Platform;
import org.andrill.coretools.ResourceLoader;
import org.andrill.coretools.model.ModelContainer;
import org.andrill.coretools.model.io.ModelFormatManager;
import org.andrill.coretools.model.io.ModelReader;
import org.andrill.coretools.model.io.ModelWriter;

/**
 * Various script-related utility methods.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class ScriptUtils {
	/**
	 * Check the arguments.
	 * 
	 * @param parser
	 *            the parser.
	 * @param required
	 *            the required arguments.
	 * @param args
	 *            the actual args.
	 * @return the OptionSet.
	 * @throws Exception
	 *             thrown if there is a problem parsing the arguments.
	 */
	public static OptionSet checkArgs(final OptionParser parser, final String[] required, final String[] args) throws Exception {
		// parse our options
		OptionSet options = parser.parse(args);

		// check for help
		if (options.has("?")) {
			parser.printHelpOn(System.out);
			System.exit(0);
		}

		// check for required params
		if ((required != null) && (required.length != 0)) {
			for (String arg : required) {
				if (!options.has(arg) || !options.hasArgument(arg)) {
					System.err.println("'" + arg + "' is required");
					parser.printHelpOn(System.err);
					System.exit(1);
				}
			}
		}
		return options;
	}

	private static String getExtension(final File file) {
		String extension = file.getName();
		if (extension.lastIndexOf('.') >= 0) {
			extension = extension.substring(extension.lastIndexOf('.') + 1);
		}
		return extension;
	}

	/**
	 * Loads a directory of resources.
	 * 
	 * @param resources
	 *            the resources.
	 * @throws MalformedURLException
	 *             should never be thrown.
	 */
	public static void loadResources(final File resources) throws MalformedURLException {
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

	/**
	 * Reads in a set of model containers.
	 * 
	 * @param container
	 *            the containers.
	 * @param file
	 *            the file.
	 * @param format
	 *            the file format.
	 * @throws IOException
	 *             thrown if there was a problem reading the files.
	 */
	public static void read(final ModelContainer container, final File file, final String format) throws IOException {
		ModelFormatManager formats = Platform.getService(ModelFormatManager.class);
		ModelReader reader = formats.getReader(format);
		if (reader != null) {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(file);
				reader.read(container, fis);
			} finally {
				fis.close();
			}
		} else {
			System.err.println("Unrecognized container data file format: " + format);
		}
	}

	/**
	 * Reads in a set of model containers.
	 * 
	 * @param container
	 *            the containers.
	 * @param files
	 *            the files.
	 * @throws IOException
	 *             thrown if there was a problem reading the files.
	 */
	public static void read(final ModelContainer container, final List<File> files) throws IOException {
		for (File file : files) {
			read(container, file, getExtension(file));
		}
	}

	/**
	 * Writes a model container to the specified file.
	 * 
	 * @param container
	 *            the model container.
	 * @param out
	 *            the output file.
	 * @throws IOException
	 *             thrown if any problems writing the file.
	 */
	public static void write(final ModelContainer container, final File out) throws IOException {
		write(container, out, getExtension(out));
	}

	/**
	 * Writes a model container to the specified file.
	 * 
	 * @param container
	 *            the model container.
	 * @param out
	 *            the output file.
	 * @param format
	 *            the output format.
	 * @throws IOException
	 *             thrown if any problems writing the file.
	 */
	public static void write(final ModelContainer container, final File out, final String format) throws IOException {
		ModelFormatManager formats = Platform.getService(ModelFormatManager.class);
		ModelWriter writer = formats.getWriter(format);
		if (writer != null) {
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(out);
				writer.write(container, fos);
			} finally {
				if (fos != null) {
					fos.close();
				}
			}
		} else {
			System.err.println("Unrecognized container data file format: " + format);
		}
	}

	private ScriptUtils() {
		// not intended to be instantiated
	}
}
