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
package org.andrill.coretools.scene;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.Class;
import java.util.List;

import org.andrill.coretools.Adaptable;
import org.andrill.coretools.graphics.GraphicsContext;
import org.andrill.coretools.model.Model;
import org.andrill.coretools.model.ModelContainer;
import org.andrill.coretools.scene.Scene.ScenePart;

import com.google.common.collect.ImmutableMap;

/**
 * Defines the interface of a track. A Track is responsible for visualizing Models. For consistency, Tracks are
 * considered to be oriented vertically when calculating sizes and rendering.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public interface Track extends Adaptable {
	/**
	 * Find the track element at the specified point in screen coordinates.
	 * 
	 * @param screen
	 *            the screen coordinates.
	 * @param part
	 *            the scene part.
	 * @return the track element or null.
	 */
	Object findAt(Point2D screen, ScenePart part);

	/**
	 * Gets the size of this track's contents.
	 * 
	 * @return the contents size.
	 */
	Rectangle2D getContentSize();

	/**
	 * Gets the bounds for the specified model.
	 * 
	 * @param model
	 *            the model.
	 * @return the bounds or null if the model is not in this track.
	 */
	Rectangle2D getModelBounds(Model model);

	/**
	 * Gets the Model class(es) this track creates.
	 * 
	 * @return list of Classes.
	 */
	List<Class> getCreatedClasses();

	/**
	 * Gets a configuration parameter for this track.
	 * 
	 * @param name
	 *            the name.
	 * @param defaultValue
	 *            the default value.
	 * @return the configured parameter or the default value if not set.
	 */
	String getParameter(String name, String defaultValue);

	/**
	 * Gets the parameters of this track.
	 * 
	 * @return the parameters.
	 */
	ImmutableMap<String, String> getParameters();

	/**
	 * Gets the scene this track exists in.
	 * 
	 * @return the scene.
	 */
	Scene getScene();

	/**
	 * Render the contents of this track.
	 * 
	 * @param graphics
	 *            the graphics.
	 * @param bounds
	 *            the bounds.
	 */
	void renderContents(GraphicsContext graphics, Rectangle2D bounds);

	/**
	 * Render the footer of this track.
	 * 
	 * @param graphics
	 *            the graphics.
	 * @param bounds
	 *            the bounds.
	 */
	void renderFooter(GraphicsContext graphics, Rectangle2D bounds);

	/**
	 * Render the header of this track.
	 * 
	 * @param graphics
	 *            the graphics.
	 * @param bounds
	 *            the bounds.
	 */
	void renderHeader(GraphicsContext graphics, Rectangle2D bounds);

	/**
	 * Sets the models of this track.
	 * 
	 * @param container
	 *            the models.
	 */
	void setModels(ModelContainer container);

	/**
	 * Sets a configuration parameter for this track.
	 * 
	 * @param name
	 *            the name.
	 * @param value
	 *            the value.
	 */
	void setParameter(String name, String value);

	/**
	 * Sets the scene this track exists in.
	 * 
	 * @param scene
	 *            the scene.
	 */
	void setScene(Scene scene);
}
