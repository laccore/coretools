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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScriptLauncher {

	/**
	 * @param args
	 */
	public static void main(final String[] args) throws Exception {
		List<String> argsList = new ArrayList<String>(Arrays.asList(args));
		if (argsList.size() == 0) {
			System.err.println("No script specified");
			System.exit(1);
		}

		// ad default script prefix
		String script = argsList.remove(0);
		if (script.indexOf('.') == -1) {
			script = "org.andrill.coretools.scripting." + script;
		}

		// invoke the script
		Class<?> clazz= Class.forName(script);
		Method main = clazz.getMethod("main", new Class[] { String[].class });
		main.invoke(null, new Object[] { argsList.toArray(new String[0]) });
	}
}
