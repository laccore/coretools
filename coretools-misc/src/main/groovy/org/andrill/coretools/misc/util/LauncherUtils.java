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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Various utility methods for opening files in their native OS editors/viewers.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class LauncherUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(LauncherUtils.class);
	private static final String[] browsers = new String[] { "firefox", "opera", "konqueror", "epiphany", "seamonkey",
	        "galeon", "kazehakase", "mozilla", "netscape" };

	/**
	 * Attempts to open the specified file in OS native viewer/editor.
	 * 
	 * @param file
	 *            the file to open.
	 */
	public static void open(final File file) {
		String os = System.getProperty("os.name").toLowerCase();
		String cmd;
		if (os.indexOf("win") >= 0) {
			cmd = String.format("cmd /c start %s", file.getAbsolutePath());
		} else if (os.indexOf("mac") >= 0) {
			cmd = String.format("open %s", file.getAbsolutePath());
		} else {
			cmd = String.format("gnome-open %s", file.getAbsolutePath());
		}
		try {
			Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			LOGGER.error("Unable to open file {}: {}", file.getAbsolutePath(), e.getMessage());
		}
	}

	/**
	 * Attempts to open a URL in the user's native browser.
	 * 
	 * Note: This method is adapted from one published by Dem Pilafian at http://www.centerkey.com/java/browser/
	 * 
	 * @param url
	 *            the URL to open.
	 */
	public static void openURL(final String url) {
		String osName = System.getProperty("os.name");
		try {
			if (osName.startsWith("Mac OS")) {
				Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
				Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] { String.class });
				openURL.invoke(null, new Object[] { url });
			} else if (osName.startsWith("Windows")) {
				Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
			} else { // assume Unix or Linux
				boolean found = false;
				for (String browser : browsers) {
					if (!found) {
						found = Runtime.getRuntime().exec(new String[] { "which", browser }).waitFor() == 0;
						if (found) {
							Runtime.getRuntime().exec(new String[] { browser, url });
						}
					}
				}
				if (!found) {
					throw new Exception(Arrays.toString(browsers));
				}
			}
		} catch (Exception e) {
			LOGGER.error("Unable to open url {}: {}", url, e.getMessage());
		}
	}

	private LauncherUtils() {
		// not intended to be instantiated
	}
}
