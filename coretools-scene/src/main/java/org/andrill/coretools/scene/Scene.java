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

import java.awt.Dimension;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Map;

import org.andrill.coretools.Adaptable;
import org.andrill.coretools.graphics.GraphicsContext;
import org.andrill.coretools.model.ModelContainer;
import org.andrill.coretools.model.edit.CommandStack;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Renders models as a series of tracks arranged visually.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public interface Scene extends Adaptable {

	/**
	 * Listens to changes in the scene.
	 */
	public interface ChangeListener {

		/**
		 * Called when the scene changes.
		 */
		void sceneChanged();
	}

	/**
	 * Scene origin.
	 */
	public enum Origin {
		TOP, BASE
	}

	/**
	 * Parts of the scene.
	 */
	public enum ScenePart {
		HEADER, CONTENTS, FOOTER
	}

	/**
	 * Listens for selection changes in the scene.
	 */
	public interface SelectionListener {

		/**
		 * Called when the selection changes.
		 * 
		 * @param selection
		 *            the new selection.
		 */
		void selectionChanged(Selection selection);
	}

	public static final Dimension ZERO = new Dimension(0, 0);
	public static final int DEFAULT_HEADER_HEIGHT = 36;

	/**
	 * Adds a change listener.
	 * 
	 * @param l
	 *            the listener.
	 */
	void addChangeListener(ChangeListener l);

	/**
	 * Adds a selection listener.
	 * 
	 * @param l
	 *            the listener.
	 */
	void addSelectionListener(SelectionListener l);

	/**
	 * Add the specified track to the scene.
	 * 
	 * @param track
	 *            the track.
	 * @param constraints
	 *            the layout constraints.
	 */
	void addTrack(Track track, String constraints);

	/**
	 * Find the scene element at the specified point in screen coordinates.
	 * 
	 * @param screen
	 *            the screen coordinates.
	 * @param part
	 *            the scene part.
	 * @return the scene element or null if no match.
	 */
	Object findAt(Point2D screen, ScenePart part);

	/**
	 * Gets the command stack for the scene.
	 * 
	 * @return the edit support.
	 */
	CommandStack getCommandStack();

	/**
	 * Gets the size of the content.
	 * 
	 * @return the contents size.
	 */
	Rectangle2D getContentSize();

	/**
	 * Gets the size of the footer.
	 * 
	 * @return the footer size.
	 */
	Rectangle2D getFooterSize();

	/**
	 * Gets the size of the header.
	 * 
	 * @return the header size.
	 */
	Rectangle2D getHeaderSize();

	/**
	 * Gets the models for this scene.
	 * 
	 * @return the models.
	 */
	ModelContainer getModels();

	/**
	 * Gets the origin of this scene.
	 * 
	 * @return the origin.
	 */
	Origin getOrigin();

	/**
	 * Gets a configuration parameter for this scene.
	 * 
	 * @param name
	 *            the name.
	 * @param defaultValue
	 *            the default value.
	 * @return the configured parameter or the default value if not set.
	 */
	String getParameter(String name, String defaultValue);

	/**
	 * Gets the parameters of this scene.
	 * 
	 * @return the parameters.
	 */
	ImmutableMap<String, String> getParameters();

	/**
	 * Gets the preferred width of this scene in screen units.
	 * 
	 * @return the preferred width.
	 */
	double getPreferredWidth();

	/**
	 * Gets a rendering hint.
	 * 
	 * @param name
	 *            the name of the hint.
	 * @return the value or null if not set.
	 */
	String getRenderHint(String name);

	/**
	 * Gets the render hints.
	 * 
	 * @return the render hints.
	 */
	public Map<String, String> getRenderHints();

	/**
	 * Gets the scaling factor.
	 * 
	 * @return the scaling factor.
	 */
	double getScalingFactor();

	/**
	 * Gets the selection.
	 * 
	 * @return the selection.
	 */
	Selection getSelection();

	/**
	 * Gets the layout constraints set for the specified track.
	 * 
	 * @param track
	 *            the track.
	 * @return the layout constraints or null if none set.
	 */
	String getTrackConstraints(Track track);

	/**
	 * Gets the tracks in this scene.
	 * 
	 * @return the tracks.
	 */
	ImmutableList<Track> getTracks();

	/**
	 * Invalidate the scene.
	 */
	void invalidate();

	/**
	 * Removes a change listener.
	 * 
	 * @param l
	 *            the listener.
	 */
	void removeChangeListener(ChangeListener l);

	/**
	 * Removes a selection listener.
	 * 
	 * @param l
	 *            the listener.
	 */
	void removeSelectionListener(SelectionListener l);

	/**
	 * Render the contents of this scene.
	 * 
	 * @param graphics
	 *            the graphics.
	 * @param clip
	 *            the clip to render relative to the rectangle returned by {@link Scene#getContentSize(Dimension2D)}.
	 */
	void renderContents(GraphicsContext graphics, Rectangle2D clip);

	/**
	 * Render the footer of this scene.
	 * 
	 * @param graphics
	 *            the graphics.
	 */
	void renderFooter(GraphicsContext graphics);

	/**
	 * Render the header of this scene.
	 * 
	 * @param graphics
	 *            the graphics.
	 */
	void renderHeader(GraphicsContext graphics);

	/**
	 * Sets the command stack.
	 * 
	 * @param commandStack
	 *            the edit support object.
	 */
	void setCommandStack(CommandStack commandStack);

	/**
	 * Sets the models in this scene.
	 * 
	 * @param container
	 *            the models.
	 */
	void setModels(ModelContainer container);

	/**
	 * Sets a configuration parameter for this scene.
	 * 
	 * @param name
	 *            the name.
	 * @param value
	 *            the value.
	 */
	void setParameter(String name, String value);

	/**
	 * Sets the preferred width.
	 * 
	 * @param width
	 *            the preferred width.
	 */
	void setPreferredWidth(double width);

	/**
	 * Sets a rendering hint.
	 * 
	 * @param name
	 *            the name of the hint.
	 * @param value
	 *            the value of the hint.
	 */
	void setRenderHint(String name, String value);

	/**
	 * Sets the scaling factor.
	 * 
	 * @param scale
	 *            the scaling factor.
	 */
	void setScalingFactor(double scale);

	/**
	 * Sets the selection.
	 * 
	 * @param selection
	 *            the selection.
	 */
	void setSelection(Selection selection);

	/**
	 * Validate the scene.
	 */
	void validate();
}
