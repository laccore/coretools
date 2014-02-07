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

import java.awt.Cursor;
import java.util.HashMap;
import java.util.Map;

import org.andrill.coretools.graphics.GraphicsContext;

/**
 * A Feedback object is created in response to a SceneEvent.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class DefaultFeedback implements Feedback {
	public interface Figure {
		void render(GraphicsContext g);
	}

	protected String type = null;
	protected Map<String, String> properties = null;
	protected int cursorType = Cursor.DEFAULT_CURSOR;
	protected Object target = null;
	protected Figure figure = null;

	/**
	 * Create a new DefaultFeedback.
	 */
	public DefaultFeedback() {
	}

	/**
	 * Create a new DefaultFeedback of the specified type.
	 * 
	 * @param type
	 *            the type.
	 */
	public DefaultFeedback(final String type) {
		this.type = type;
	}

	/**
	 * Create a new DefaultFeedback.
	 * 
	 * @param type
	 *            the type.
	 * @param target
	 *            the target.
	 * @param cursorType
	 *            the cursor type.
	 * @param properties
	 *            the properties.
	 * @param figure
	 *            the figure;
	 */
	public DefaultFeedback(final String type, final Object target, final int cursorType,
	        final Map<String, String> properties, final Figure figure) {
		this.type = type;
		this.target = target;
		this.cursorType = cursorType;
		if ((properties != null) && !properties.isEmpty()) {
			this.properties = new HashMap<String, String>();
			this.properties.putAll(properties);
		}
		this.figure = figure;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getCursorType() {
		return cursorType;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getProperty(final String name) {
		return (properties == null) ? null : properties.get(name);
	}

	/**
	 * {@inheritDoc}
	 */
	public Object getTarget() {
		return target;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getType() {
		return type;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean needsRendering() {
		return figure != null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void renderFeedback(final GraphicsContext graphics) {
		if (figure != null) {
			figure.render(graphics);
		}
	}

	/**
	 * Sets the cursor type of this feedback.
	 * 
	 * @param cursorType
	 *            the cursor type.
	 */
	public void setCursorType(final int cursorType) {
		this.cursorType = cursorType;
	}

	/**
	 * Sets the figure for this feedback.
	 * 
	 * @param figure
	 *            the figure.
	 */
	public void setFigure(final Figure figure) {
		this.figure = figure;
	}

	/**
	 * Sets a property of this feedback.
	 * 
	 * @param name
	 *            the name.
	 * @param value
	 *            the value.
	 */
	public void setProperty(final String name, final String value) {
		if (properties == null) {
			properties = new HashMap<String, String>();
		}
		properties.put(name, value);
	}

	/**
	 * Sets the target of this feedback.
	 * 
	 * @param target
	 *            the target.
	 */
	public void setTarget(final Object target) {
		this.target = target;
	}

	/**
	 * Sets the type of this feedback.
	 * 
	 * @param type
	 *            the type.
	 */
	public void setType(final String type) {
		this.type = type;
	}
}
