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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scale image task.
 */
public class ScaleTask implements Callable<BufferedImage> {
	/**
	 * Scaling parameters.
	 */
	static class Params {
		String path;
		int width;
		int height;
		WeakReference<JComponent> component;

		Params(final String path, final int width, final int height, final JComponent component) {
			this.path = path;
			this.width = width;
			this.height = height;
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
			if (height != other.height) {
				return false;
			}
			if (path == null) {
				if (other.path != null) {
					return false;
				}
			} else if (!path.equals(other.path)) {
				return false;
			}
			if (width != other.width) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + height;
			result = prime * result + ((path == null) ? 0 : path.hashCode());
			result = prime * result + width;
			return result;
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(ScaleTask.class);

	private final Future<BufferedImage> imageLoader;
	private final Dimension2D dimensions;
	private final JComponent component;

	public ScaleTask(final Future<BufferedImage> imageLoader, final Dimension2D dimensions, final JComponent component) {
		this.imageLoader = imageLoader;
		this.dimensions = dimensions;
		this.component = component;
	}

	public BufferedImage call() throws Exception {
		try {
			BufferedImage image = imageLoader.get();
			if (image == null) {
				LOGGER.warn("Unable to scale, image was null");
				return null;
			} else {
				int type = (image.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB
				        : BufferedImage.TYPE_INT_ARGB;
				BufferedImage scaled = new BufferedImage((int) dimensions.getWidth(), (int) dimensions.getHeight(),
				        type);
				Graphics2D g2d = scaled.createGraphics();
				g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g2d.drawImage(image, 0, 0, scaled.getWidth(), scaled.getHeight(), null);
				g2d.dispose();
				return scaled;
			}
		} finally {
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
}