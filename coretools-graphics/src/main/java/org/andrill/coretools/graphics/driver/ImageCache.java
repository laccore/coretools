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
package org.andrill.coretools.graphics.driver;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

import javax.swing.JComponent;

import org.andrill.coretools.JobService;
import org.andrill.coretools.JobService.Priority;
import org.andrill.coretools.graphics.util.ImageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * A cache for images.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
@Singleton
public class ImageCache {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageCache.class);

	protected final ConcurrentMap<LoadTask.Params, Future<BufferedImage>> levelCache;
	protected final ConcurrentMap<ScaleTask.Params, Future<BufferedImage>> scaleCache;

	@Inject
	ImageCache(final JobService jobs) {
		levelCache = new MapMaker().softValues().makeComputingMap(
		        new Function<LoadTask.Params, Future<BufferedImage>>() {
			        public Future<BufferedImage> apply(final LoadTask.Params key) {
				        try {
					        return jobs.submit(new LoadTask(new URL(key.path), key.level, key.component.get()),
					                Priority.MEDIUM);
				        } catch (MalformedURLException e) {
					        LOGGER.error("Unable to load image", e);
					        throw new AssertionError(); // will never happen
				        }
			        }
		        });
		scaleCache = new MapMaker().softValues().makeComputingMap(
		        new Function<ScaleTask.Params, Future<BufferedImage>>() {
			        public Future<BufferedImage> apply(final ScaleTask.Params key) {
				        // calculate level
				        int level = 0;
				        ImageInfo ii = new ImageInfo();
				        InputStream is = null;
				        try {
					        is = new URL(key.path).openStream();
					        ii.setInput(is);
					        if (ii.check()) {
						        level = Math.min(ii.getWidth() / key.width, ii.getHeight() / key.height);
					        }
				        } catch (IOException e) {
					        LOGGER.error("Unable to load image", e);
				        } finally {
					        if (is != null) {
						        try {
							        is.close();
						        } catch (IOException ioe) {
							        // ignore
						        }
					        }
				        }

				        return jobs.submit(new ScaleTask(levelCache.get(new LoadTask.Params(key.path, level,
				                key.component.get())), new Dimension(key.width, key.height), key.component.get()),
				                Priority.HIGH);
			        }
		        });
	}

	// 5/15/2025 brg: Clear cache to force reload of images
	public void clear() {
		levelCache.clear();
		scaleCache.clear();
	}

	/**
	 * Gets the specified image with the specified dimensions.
	 * 
	 * @param url
	 *            the URL.
	 * @param dim
	 *            the dimension.
	 * @param component
	 *            the component rendering the image or null if headless rendering.
	 * @return the image Future.
	 */
	public Future<BufferedImage> get(final URL url, final Dimension dim, final JComponent component) {
		LOGGER.trace("get: {}, {}, {}", new Object[] { url, dim, component });
		return scaleCache.get(new ScaleTask.Params(url.toExternalForm(), dim.width, dim.height, component));
	}

	/**
	 * Gets the specified image with the specified URL.
	 * 
	 * @param url
	 *            the URL.
	 * @param level
	 *            the decimation level.
	 * @param component
	 *            the component rendering the image or null if headless rendering.
	 * @return the image Future.
	 */
	public Future<BufferedImage> get(final URL url, final int level, final JComponent component) {
		LOGGER.trace("get: {}, {}, {}", new Object[] { url, level, component });
		return levelCache.get(new LoadTask.Params(url.toExternalForm(), level, component));
	}

	/**
	 * Gets the specified image with the specified URL.
	 * 
	 * @param url
	 *            the URL.
	 * @param component
	 *            the component rendering the image or null if headless rendering.
	 * @return the image Future.
	 */
	public Future<BufferedImage> get(final URL url, final JComponent component) {
		LOGGER.trace("get: {}, {}", new Object[] { url, component });
		return get(url, 1, component);
	}

	/**
	 * Gets the closest fully-loaded image with the specified URL.
	 * 
	 * @param url
	 *            the URL.
	 * @param dim
	 *            the dimensions.
	 * @return the image Future or null.
	 */
	public Future<BufferedImage> getPlaceholderImage(final URL url, final Dimension dim) {
		// check our scale cache first
		String path = url.toExternalForm();
		Future<BufferedImage> found = null;
		int fitness = Integer.MAX_VALUE;
		for (Entry<ScaleTask.Params, Future<BufferedImage>> entry : scaleCache.entrySet()) {
			ScaleTask.Params key = entry.getKey();
			Future<BufferedImage> value = entry.getValue();
			if (value.isDone() && !value.isCancelled() && path.equals(key.path)) {
				int diff = Math.max(Math.abs(dim.width - key.width), Math.abs(dim.height - key.height));
				if (diff < fitness) {
					fitness = diff;
					found = value;
				}
			}
		}
		if (found != null) {
			return found;
		}

		// check our level cache next
		for (Entry<LoadTask.Params, Future<BufferedImage>> entry : levelCache.entrySet()) {
			LoadTask.Params key = entry.getKey();
			Future<BufferedImage> value = entry.getValue();
			if (value.isDone() && !value.isCancelled() && path.equals(key.path)) {
				return value;
			}
		}
		return null;
	}
}
