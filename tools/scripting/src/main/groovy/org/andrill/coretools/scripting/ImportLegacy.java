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
import java.util.Arrays;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.andrill.coretools.Platform;
import org.andrill.coretools.model.ModelContainer;

/**
 * A script for reading in a legacy PSICAT data file and writing out in the new Core Tools model container format.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class ImportLegacy {

	public static void main(final String[] args) throws Exception {
		// setup our options
		OptionParser parser = new OptionParser() {
			{
				accepts("in", "the legacy file [R]").withRequiredArg().ofType(File.class);
				accepts("out", "the output file [R]").withRequiredArg().ofType(File.class);
				accepts("resources", "the resources directory [O]").withRequiredArg().ofType(File.class);
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
		if (options.hasArgument("resources")) {
			ScriptUtils.loadResources((File) options.valueOf("resources"));
		}

		// import the legacy data
		ModelContainer container = Platform.getService(ModelContainer.class);
		ScriptUtils.read(container, in, "legacy-psicat");

		// write out the results
		ScriptUtils.write(container, out);
	}
}
