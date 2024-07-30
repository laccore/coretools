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
package org.andrill.coretools.model.scheme;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.andrill.coretools.Platform;
import org.andrill.coretools.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An entry in a scheme.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class SchemeEntry {
	private static final Logger LOGGER = LoggerFactory.getLogger(SchemeEntry.class);
	protected Map<String, String> properties;
	protected String code;
	protected String name;
	protected Scheme scheme;

	protected final ResourceLoader loader;
	protected Color color = null;
	protected BufferedImage image = null;
	protected Icon icon = null;

	/**
	 * Create a new SchemeEntry.
	 * 
	 * @param code
	 *            the code.
	 * @param name
	 *            the name.
	 * @param properties
	 *            the properties.
	 */
	public SchemeEntry(final String code, final String name, final Map<String, String> properties) {
		this(code, name, properties, Platform.getService(ResourceLoader.class));
	}

	/**
	 * Create a new SchemeEntry.
	 * 
	 * @param code
	 *            the code.
	 * @param name
	 *            the name.
	 * @param properties
	 *            the properties.
	 * @param loader
	 *            the resource loader.
	 */
	public SchemeEntry(final String code, final String name, final Map<String, String> properties, final ResourceLoader loader) {
		this.code = code;
		this.name = name;
		this.properties = properties == null ? new HashMap<String, String>() : properties;
		this.loader = loader;
	}

	/**
	 * Gets the code of this entry.
	 * 
	 * @return the code.
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Gets the color of this entry.
	 * 
	 * @return the icon or null.
	 */
	public Color getColor() {
		if (color == null) {
			String str = properties.get("color");
			if (str != null) {
				String[] split = str.split(",");
				if (split.length == 3) {
					color = new Color(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer
					        .parseInt(split[2]));
				}
			}
		}
		return color;
	}

	/**
	 * Gets the icon of this entry.
	 * 
	 * @return the icon or null.
	 */
	public Icon getIcon() {
		if (icon == null) {
			Color color = getColor();
			BufferedImage pattern = getImage();
			if ((color != null) || (pattern != null)) {
				Rectangle r = new Rectangle(0, 0, 24, 24);
				BufferedImage image = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_RGB);
				Graphics2D g2d = image.createGraphics();

				// fill with color
				g2d.setBackground((color == null) ? Color.white : color);
				g2d.setPaint(g2d.getBackground());
				g2d.fill(r);

				// fill with pattern
				if (pattern != null) {
					int w = Math.min(96, Math.min(pattern.getWidth(), pattern.getHeight()));
					g2d.drawImage(
					        pattern.getSubimage(0, 0, w, w).getScaledInstance(24, 24, Image.SCALE_AREA_AVERAGING), 0,
					        0, null);
				}
				g2d.dispose();
				icon = new ImageIcon(image);
			}
		}
		return icon;
	}

	/**
	 * Gets the image of this entry.
	 * 
	 * @return the image or null.
	 */
	public BufferedImage getImage() {
		if (image == null) {
			URL url = getImageURL();
			if (url != null) {
				try {
					image = ImageIO.read(url);
				} catch (IOException ioe) {
					LOGGER.warn("Unable to load image", ioe);
				}
			}
		}
		return image;
	}

	/**
	 * Gets the image URL of this entry.
	 * 
	 * @return the image URL or null.
	 */
	public URL getImageURL() {
		String path = properties.get("image");
		if ((path == null) || (loader == null)) {
			return null;
		} else {
			return loader.getResource(path);
		}
	}

	/**
	 * Gets the name of this entry.
	 * 
	 * @return the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the properties of this entry.
	 * 
	 * @return the properties.
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * Gets a property of this entry.
	 * 
	 * @param name
	 *            the name.
	 * @param defaultValue
	 *            the default value.
	 * @return the value or default value if the property doesn't exist.
	 */
	public String getProperty(final String name, final String defaultValue) {
		if (properties.containsKey(name)) {
			return properties.get(name);
		} else {
			return defaultValue;
		}
	}

	/**
	 * Gets the scheme of this entry.
	 * 
	 * @return the scheme.
	 */
	public Scheme getScheme() {
		return scheme;
	}

	/**
	 * Sets the code of this entry.
	 * 
	 * @param code
	 *            the code.
	 */
	public void setCode(final String code) {
		this.code = code;
	}

	/**
	 * Sets the name of this entry.
	 * 
	 * @param name
	 *            the name.
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * Sets the properties of this entry.
	 * 
	 * @param properties
	 *            the properties.
	 */
	public void setProperties(final Map<String, String> properties) {
		this.properties = properties == null ? new HashMap<String, String>() : properties;
	}

	/**
	 * Sets a property on this entry.
	 * 
	 * @param name
	 *            the name.
	 * @param value
	 *            the value.
	 */
	public void setProperty(final String name, final String value) {
		properties.put(name, value);
	}

	/**
	 * Sets the scheme of this entry.
	 * 
	 * @param scheme
	 *            the scheme.
	 */
	public void setScheme(final Scheme scheme) {
		this.scheme = scheme;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return name;
	}
}
