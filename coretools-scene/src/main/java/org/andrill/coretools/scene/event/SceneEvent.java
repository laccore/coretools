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

import java.util.HashMap;
import java.util.Map;

import org.andrill.coretools.scene.Scene.ScenePart;

/**
 * A base class for all Scene events.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public abstract class SceneEvent {
	// key/mouse masks
	public static final int ALT_DOWN_MASK = 1 << 9;
	public static final int BUTTON1_DOWN_MASK = 1 << 10;
	public static final int BUTTON2_DOWN_MASK = 1 << 11;
	public static final int BUTTON3_DOWN_MASK = 1 << 12;
	public static final int CTRL_DOWN_MASK = 1 << 7;
	public static final int META_DOWN_MASK = 1 << 8;
	public static final int SHIFT_DOWN_MASK = 1 << 6;

	protected boolean consumed = false;
	protected int modifiers = 0;
	protected ScenePart target;
	protected Object source;
	protected Map<String, String> properties = null;

	/**
	 * Create a new SceneEvent.
	 */
	public SceneEvent() {
	}

	/**
	 * Mark this event as consumed.
	 */
	public void consume() {
		consumed = true;
	}

	/**
	 * Gets the event modifiers.
	 * 
	 * @return the modifiers.
	 */
	public int getModifiers() {
		return modifiers;
	}

	/**
	 * Gets a property of this event.
	 * 
	 * @param name
	 *            the property.
	 * @return the value of the property.
	 */
	public String getProperty(final String name) {
		return (properties == null) ? null : properties.get(name);
	}

	/**
	 * Gets the source of this event.
	 * 
	 * @return the source.
	 */
	public Object getSource() {
		return source;
	}

	/**
	 * Gets the target of this event.
	 * 
	 * @return the target.
	 */
	public ScenePart getTarget() {
		return target;
	}

	/**
	 * Returns whether or not the Alt modifier is down on this event.
	 */
	public boolean isAltDown() {
		return (modifiers & ALT_DOWN_MASK) != 0;
	}

	/**
	 * Checks whether this event has been consumed.
	 * 
	 * @return true if the event is consumed, false otherwise.
	 */
	public boolean isConsumed() {
		return consumed;
	}

	/**
	 * Returns whether or not the Control modifier is down on this event.
	 */
	public boolean isControlDown() {
		return (modifiers & CTRL_DOWN_MASK) != 0;
	}

	/**
	 * Returns whether or not the Meta modifier is down on this event.
	 */
	public boolean isMetaDown() {
		return (modifiers & META_DOWN_MASK) != 0;
	}

	/**
	 * Returns whether or not the Shift modifier is down on this event.
	 */
	public boolean isShiftDown() {
		return (modifiers & SHIFT_DOWN_MASK) != 0;
	}

	/**
	 * Sets the event modifiers.
	 * 
	 * @param modifiers
	 *            the modifiers.
	 */
	public void setModifiers(final int modifiers) {
		this.modifiers = modifiers;
	}

	/**
	 * Sets a property of this event.
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
	 * Sets the source of this event.
	 * 
	 * @param source
	 *            the source.
	 */
	public void setSource(final Object source) {
		this.source = source;
	}

	/**
	 * Sets the target of this event.
	 * 
	 * @param target
	 *            the target.
	 */
	public void setTarget(final ScenePart target) {
		this.target = target;
	}
}
