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
package org.andrill.coretools;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * Starts the platform.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class Platform {
	private static Logger LOGGER = LoggerFactory.getLogger(Platform.class);
	private static boolean started = false;
	private static Injector injector = null;

	/**
	 * Gets a service from the platform.
	 * 
	 * @param <E>
	 *            the service type.
	 * @param clazz
	 *            the service class.
	 * @return the instance.
	 */
	public static <E> E getService(final Class<E> clazz) {
		if (!started) {
			throw new IllegalStateException("The platform has not been started");
		} else {
			return injector.getInstance(clazz);
		}
	}

	/**
	 * Starts the platform.
	 */
	public static void start() {
		if (!started) {
			// discover our modules
			Set<Module> modules = new HashSet<Module>();
			try {
				for (URL url : Collections.list(Platform.class.getClassLoader().getResources(
				        "META-INF/services/com.google.inject.Module"))) {
					DefaultResourceLoader.parseSPI(url, modules);
				}
			} catch (IOException ioe) {
				LOGGER.error("Unable to discover modules", ioe);
			}
			LOGGER.info("Starting modules: {}", modules);
			injector = Guice.createInjector(modules);
			started = true;
		}
	}

	Platform() {
		// not to be instantiated
	}
}
