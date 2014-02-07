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
package org.andrill.coretools.scene.event;

import java.awt.Color;

import org.andrill.coretools.graphics.GraphicsContext;

/**
 * Defines the interface for feedback.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public interface Feedback {
	public static final String SELECT_TYPE = "Feedback.Select";
	public static final String CREATE_TYPE = "Feedback.Create";
	public static final String MOVE_TYPE = "Feedback.Move";
	public static final String RESIZE_TYPE = "Feedback.Type";
	public static final String DELETE_TYPE = "Feedback.Delete";
	public static final Color COLOR = new Color(0, 0, 0, 192);

	/**
	 * Gets the cursor type of this feedback.
	 * 
	 * @return the cursor type.
	 */
	public abstract int getCursorType();

	/**
	 * Gets a property of this feedback.
	 * 
	 * @param name
	 *            the property.
	 * @return the value of the property.
	 */
	public abstract String getProperty(final String name);

	/**
	 * Gets the target of this feedback.
	 * 
	 * @return the target.
	 */
	public abstract Object getTarget();

	/**
	 * Gets the type of this feedback.
	 * 
	 * @return the request type.
	 */
	public abstract String getType();

	/**
	 * Checks whether this feedback needs rendering.
	 * 
	 * @return true if the feedback needs rendering, false otherwise.
	 */
	public boolean needsRendering();

	/**
	 * Renders feedback.
	 * 
	 * @param graphics
	 *            the graphics object.
	 */
	public abstract void renderFeedback(final GraphicsContext graphics);

}