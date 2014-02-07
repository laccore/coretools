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

import groovy.util.Eval;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.andrill.coretools.Platform;
import org.andrill.coretools.model.Model;
import org.andrill.coretools.model.ModelContainer;

/**
 * A script for searching and filter model containers.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class Containers {

	@SuppressWarnings("unchecked")
	public static void main(final String[] args) throws Exception {
		// our options
		OptionParser parser = new OptionParser() {
			{
				accepts("in", "the directory of images to import [R]").withRequiredArg().ofType(File.class);
				accepts("out", "the output file [O]").withOptionalArg().ofType(File.class);
				accepts("grep", "include all models that match the expression [O]").withOptionalArg();
				accepts("filter", "remove all models that match the expression [O]").withOptionalArg();
				accepts("verbose", "verbose output [O]");
				acceptsAll(Arrays.asList("h", "?"), "show help");
			}
		};

		// parse our options
		OptionSet options = ScriptUtils.checkArgs(parser, new String[] { "in" }, args);
		boolean verbose = options.has("verbose");

		// start the platform
		Platform.start();

		// get our input file
		ModelContainer container = Platform.getService(ModelContainer.class);
		ScriptUtils.read(container, (List<File>) options.valuesOf("in"));

		// for collecting results
		ModelContainer result = Platform.getService(ModelContainer.class);

		// handle grep and filter
		if (options.has("filter") || options.has("grep")) {
			String filter = (String) options.valueOf("filter");
			String grep = (String) options.valueOf("grep");
			for (Model model : container.getModels()) {
				if (grep != null) {
					if (Eval.me("model", model, grep) == Boolean.TRUE) {
						result.add(model);
						if (verbose) {
							System.out.println(model.getModelType() + ":" + model.getModelData());
						}
					}
				}
				if (filter != null) {
					if (Eval.me("model", model, filter) == Boolean.FALSE) {
						result.add(model);
						if (verbose) {
							System.out.println(model.getModelType() + ":" + model.getModelData());
						}
					}
				}
			}
		} else {
			for (Model model : container.getModels()) {
				result.add(model);
			}
		}

		// write out the results
		if (options.hasArgument("out")) {
			ScriptUtils.write(result, (File) options.valueOf("out"));
		}
	}
}
