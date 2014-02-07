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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.inject.Singleton;

/**
 * A default implementation of the ResourceLoader interface.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
@Singleton
public class DefaultResourceLoader implements ResourceLoader {
	private static final class Resources extends URLClassLoader {
		public Resources(final ClassLoader parent) {
			super(new URL[0], parent);
		}

		@Override
		public void addURL(final URL url) {
			super.addURL(url);
		}
	}

	private static Logger LOGGER = LoggerFactory.getLogger(DefaultResourceLoader.class);

	@SuppressWarnings("unchecked")
	public static <E> void parseSPI(final URL url, final Set<E> set) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(url.openStream()));
			String line = null;
			while ((line = br.readLine()) != null) {
				final String spi = line.trim();
				if (!spi.startsWith("#")) {
					try {
						final E instance = (E) Class.forName(spi).newInstance();
						set.add(instance);
						LOGGER.debug("Instantiated implementation {} for service {}", instance, url.getFile());
					} catch (final InstantiationException e) {
						LOGGER.error("Unable to instantiate service {} {}", spi, e.getMessage());
						e.printStackTrace();
					} catch (final IllegalAccessException e) {
						LOGGER.error("Unable to instantiate service {} {}", spi, e.getMessage());
						e.printStackTrace();
					} catch (final ClassNotFoundException e) {
						LOGGER.error("Unable to instantiate service {} {}", spi, e.getMessage());
						e.printStackTrace();
					}
				}
			}
		} catch (final IOException e) {
			LOGGER.warn("Unable to read service definition {} {}", url, e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException ioe) {
					// ignore
				}
			}
		}
	}

	private static String strip(final String path) {
		final int idx = path.indexOf(':');
		String stripped = (idx > -1) ? path.substring(idx + 1) : path;
		while (stripped.startsWith("/")) {
			stripped = stripped.substring(1);
		}
		return stripped;
	}

	private Resources resources = new Resources(DefaultResourceLoader.class.getClassLoader());
	private File currentDirectory = new File(".");

	protected final SetMultimap<String, Object> services = LinkedHashMultimap.create();

	/**
	 * Create a new DefaultResourceLoader.
	 */
	public DefaultResourceLoader() {
		LOGGER.debug("initialized");
	}

	/**
	 * {@inheritDoc}
	 */
	public void addResource(final URL resource) {
		resources.addURL(resource);
		LOGGER.debug("Added resource {}", resource);
	}

	private File findFile(final String url) {
		// get our path
		String path = url;
		if (path.startsWith("file:")) {
			path = path.substring(6);
		}

		// find the file
		File file = new File(path);
		if (file.isAbsolute() || file.exists()) {
			currentDirectory = file.getParentFile();
			return file;
		} else {
			return new File(currentDirectory, path);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public URL getResource(final String path) {
		List<URL> list = getResources(path);
		if (list.isEmpty()) {
			return null;
		} else {
			return list.get(0);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public List<URL> getResources(final String path) {
		List<URL> list = new ArrayList<URL>();
		if (path != null) {
			try {
				if (path.startsWith("rsrc:") || path.startsWith("classpath:")) {
					list.addAll(Collections.list(resources.getResources(strip(path))));
				} else if (path.startsWith("file:") || path.startsWith("http:") || path.startsWith("ftp:")
				        || path.startsWith("jar:")) {
					list.add(new URL(path));
				} else {
					try {
						list.add(new URL(path));
					} catch (MalformedURLException mue) {
						list.add(findFile(path).toURI().toURL());
					}
				}
			} catch (MalformedURLException e) {
				LOGGER.warn("Unable to get resources for {}: {}", path, e.getMessage());
			} catch (IOException e) {
				LOGGER.warn("Unable to get resources for {}: {}", path, e.getMessage());
			}
		}
		return list;
	}

	/**
	 * {@inheritDoc}
	 */
	public <E> E getService(final Class<E> service) {
		final Set<E> services = getServices(service);
		if (services.isEmpty()) {
			return null;
		} else {
			return services.iterator().next();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public <E> ImmutableSet<E> getServices(final Class<E> service) {
		final String key = service.getName();
		if (!services.containsKey(key)) {
			for (URL url : getResources("classpath:META-INF/services/" + service.getName())) {
				LOGGER.debug("Discovered service definition resource {}", url);
				parseSPI(url, services.get(key));
			}
		}
		return (ImmutableSet<E>) ImmutableSet.copyOf(services.get(key));
	}
}
