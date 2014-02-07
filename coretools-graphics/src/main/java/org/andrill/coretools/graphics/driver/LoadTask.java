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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load image from URL task.
 */
public class LoadTask implements Callable<BufferedImage> {
	static class Params {
		String path;
		int level = 0;
		WeakReference<JComponent> component;

		Params(final String path, final int level, final JComponent component) {
			this.path = path;
			this.level = level;
			this.component = new WeakReference<JComponent>(component);
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Params other = (Params) obj;
			if (level != other.level) {
				return false;
			}
			if (path == null) {
				if (other.path != null) {
					return false;
				}
			} else if (!path.equals(other.path)) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + level;
			result = prime * result + ((path == null) ? 0 : path.hashCode());
			return result;
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(LoadTask.class);

	final URL url;
	final int level;
	final JComponent component;

	public LoadTask(final URL url, final int level, final JComponent component) {
		this.url = url;
		this.level = level;
		this.component = component;
	}

	public BufferedImage call() throws Exception {
		ImageInputStream iis = null;
		try {
			LOGGER.debug("Loading {} @ {}", url.toString(), level);
			final ImageReader imgReader = ImageIO.getImageReadersBySuffix(suffix(url.getFile())).next();
			iis = ImageIO.createImageInputStream(url.openStream());
			imgReader.setInput(iis);
			final ImageReadParam readParam = imgReader.getDefaultReadParam();
			if (level > 0) {
				readParam.setSourceSubsampling(level, level, 0, 0);
			}
			BufferedImage img = imgReader.read(0, readParam);
			return img;
		} catch (IOException ioe) {
			LOGGER.error("Unable to load image", ioe);
			return null;
		} finally {
			if (iis != null) {
				try {
					iis.close();
				} catch (IOException e) {
					// ignore
				}
			}
			if (component != null) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						component.invalidate();
						component.validate();
						component.repaint();
					}
				});
			}
		}
	}

	private String suffix(final String str) {
		int index = str.lastIndexOf('.');
		if (index == -1) {
			return str;
		} else {
			return str.substring(index + 1);
		}
	}
}