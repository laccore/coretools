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
package org.andrill.coretools.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.andrill.coretools.Platform;
import org.andrill.coretools.AlphanumComparator;
import org.andrill.coretools.model.io.ModelFormatManager;
import org.andrill.coretools.model.io.ModelReader;
import org.andrill.coretools.model.io.ModelWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.MapMaker;

/**
 * Represents a project on disk.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class DefaultProject extends AbstractProject {
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultProject.class);
	private static final String FONTSIZE_PROP = "font-size";
	private static final String ORIGIN_PROP = "origin";
	private static final String NAME_PROP = "name";
	private static final String DEFAULT_DATA_FORMAT = "xml";
	private static final String DEFAULT_DATA_DIR = "data";
	private static final String DEFAULT_SCENE_DIR = "diagrams";
	private static final String DATA_FORMAT = "data-format";
	private static final String DATA_DIR = "data-dir";
	private static final String SCENE_DIR = "scene-dir";
	private static final String CONFIG_FILE = "project.properties";
	protected final File directory;
	protected final ModelFormatManager formats;
	protected final ConcurrentMap<String, File> files;

	/**
	 * Create a new DefaultProject.
	 * 
	 * @param directory
	 *            the directory.
	 */
	public DefaultProject(final File directory) {
		this(directory, Platform.getService(ModelFormatManager.class));
	}

	/**
	 * Create a new DefaultProject.
	 * 
	 * @param directory
	 *            the directory.
	 * @param formats
	 *            the {@link ModelFormatManager}
	 */
	public DefaultProject(final File directory, final ModelFormatManager formats) {
		this.directory = directory;
		try {
	        this.path = directory.toURI().toURL();
        } catch (MalformedURLException e) {
	        throw new AssertionError();
        }
		this.formats = formats;
		files = new MapMaker().makeMap();
		init();
	}

	@Override
	protected ModelContainer create(final String name) {
		File dataDir = getDataDir();
		dataDir.mkdirs();
		files.put(name, new File(dataDir, name + "." + getDataFormat()));
		return Platform.getService(ModelContainer.class);
	}
	
	@Override
	protected void delete(final String name) {
		File dataDir = getDataDir();
		files.remove(name);
		File fileToDelete = new File(dataDir, name + '.' + getDataFormat());
		try {
			fileToDelete.delete();
		} catch (SecurityException e) {
			LOGGER.error(e.getMessage());
		}
	}

	protected String fileName(final File f) {
		String name = f.getName();
		int i = name.indexOf('.');
		if (i != -1) {
			name = name.substring(0, i);
		}
		return name;
	}

	protected File getDataDir() {
		return new File(directory, getProperty(DATA_DIR, DEFAULT_DATA_DIR));
	}

	protected String getDataFormat() {
		return getProperty(DATA_FORMAT, DEFAULT_DATA_FORMAT);
	}

	protected String getExtension(final File f) {
		String extension = f.getName();
		int i = extension.lastIndexOf('.');
		if (i != -1) {
			extension = extension.substring(i + 1);
		}
		return extension;
	}
	
	protected String removeExtension(final File f) {
		return f.getName().replaceFirst("[.][^.]+$", "");
	}

	public String getFontSize() {
		return getProperty(FONTSIZE_PROP, "11");
	}
	
	@Override
	public String getName() {
		return getProperty(NAME_PROP, fileName(directory));
	}

	public String getOrigin() {
		return getProperty(ORIGIN_PROP, "top").trim().toLowerCase();
	}

	protected String getProperty(final String key, final String defaultValue) {
		String value = configuration.get(key);
		return value == null ? defaultValue : value;
	}

	protected File getSceneDir() {
		return new File(directory, getProperty(SCENE_DIR, DEFAULT_SCENE_DIR));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<URL> getScenes() {
		return scenes;
	}

	/**
	 * Checks whether this project is origin top.
	 * 
	 * @return true if origin top, false otherwise.
	 */
	public boolean isOriginTop() {
		return getProperty(ORIGIN_PROP, "top").equals("top");
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<String> load() {
		// create the project directory if it doesn't exist
		if (!directory.exists()) {
			directory.mkdirs();
		}

		// load our configuration
		File configFile = new File(directory, CONFIG_FILE);
		if (configFile.exists()) {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(configFile);
				Properties properties = new Properties();
				properties.load(fis);
				configuration.putAll((Map) properties);
			} catch (FileNotFoundException e) {
				// should never happen
				LOGGER.error("Unable to load project configuration", e);
			} catch (IOException e) {
				LOGGER.error("Invalid project configuration", e);
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException ignored) {
						// ignored
					}
				}
			}
		}

		// parse any scene files
		File sceneDir = getSceneDir();
		if (sceneDir.exists() && sceneDir.isDirectory()) {
			for (File file : sceneDir.listFiles()) {
				try {
					scenes.add(file.toURI().toURL());
				} catch (MalformedURLException e) {
					// should never happen
					throw new AssertionError();
				}
			}
		}

		// parse our containers
		ArrayList<String> sortedContainers = new ArrayList<String>();
		File dataDir = getDataDir();
		if (dataDir.exists() && dataDir.isDirectory()) {
			for (File file : dataDir.listFiles()) {
				if (formats.getReader(getExtension(file)) != null) {
					files.put(removeExtension(file), file);
					sortedContainers.add(removeExtension(file));
				}
			}
		}
		Collections.sort(sortedContainers, new AlphanumComparator.StringAlphanumComparator());
		return sortedContainers;
	}
	
	@Override
	protected ModelContainer open(final String name) {
		ModelContainer container = Platform.getService(ModelContainer.class);
		container.setProject(this);
		File file = files.get(name);
		if (file.exists()) {
			// get our reader
			ModelReader reader = formats.getReader(getExtension(file));
			if (reader == null) {
				reader = formats.getReader(getDataFormat());
			}
			if (reader == null) {
				LOGGER.error("Unable to find a ModelReader for format '{}' or '{}'", getExtension(file),
				        getDataFormat());
				throw new RuntimeException("Unable to open container: no ModelReader found");
			}

			// read in our data
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(file);
				reader.read(container, fis);
			} catch (FileNotFoundException e) {
				// should never happen
				LOGGER.error("Unable to open " + file.getAbsolutePath(), e);
			} catch (IOException e) {
				LOGGER.error("Unable to open " + file.getAbsolutePath(), e);
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException ignored) {
						// ignored
					}
				}
			}
		}
		return container;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void save() {
		super.save();
		saveConfiguration();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void save(final ModelContainer container) {
		// find our name
		String name = getContainerName(container);
		if (name != null) {
			save(name, container);
		}
	}

	protected void save(final String name, final ModelContainer container) {
		File file = files.get(name);

		// make our directory structure
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}

		// get our writer
		ModelWriter writer = formats.getWriter(getExtension(file));
		if (writer == null) {
			writer = formats.getWriter(getDataFormat());
		}
		if (writer == null) {
			LOGGER.error("Unable to find a ModelWriter for format '{}' or '{}'", getExtension(file), getDataFormat());
			throw new RuntimeException("Unable to save container: no ModelWriter found");
		}

		// write out the file
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			writer.write(container, fos);
		} catch (FileNotFoundException e) {
			// should never happen
			LOGGER.error("Unable to save " + file.getAbsolutePath(), e);
		} catch (IOException e) {
			LOGGER.error("Unable to save " + file.getAbsolutePath(), e);
		}
	}

	public void saveConfiguration() {
		// save the configuration
		Properties properties = new Properties();
		properties.putAll(configuration);
		
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(new File(directory, CONFIG_FILE));
			properties.store(fos, null);
		} catch (FileNotFoundException e) {
			// should never happen
			LOGGER.error("Unable to save configuration", e);
		} catch (IOException e) {
			LOGGER.error("Unable to save configuration", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setName(final String name) {
		configuration.put(NAME_PROP, name);
	}
	
	public void setFontSize(final String fontsize){
		configuration.put(FONTSIZE_PROP, fontsize);
	}

	/**
	 * Sets the origin of this project.
	 * 
	 * @param origin
	 *            the origin.
	 */
	public void setOrigin(final String origin) {
		configuration.put(ORIGIN_PROP, origin.trim().toLowerCase());
	}

}
