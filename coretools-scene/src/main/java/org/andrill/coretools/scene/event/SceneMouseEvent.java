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

import java.awt.event.MouseEvent;

import org.andrill.coretools.scene.Scene.ScenePart;

/**
 * A scene mouse event.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class SceneMouseEvent extends SceneEvent {
	public enum Button {
		NO_BUTTON, BUTTON1, BUTTON2, BUTTON3
	}

	protected Button button;
	protected int clickCount;
	protected int x, y;
	protected int dragX = -1, dragY = -1;

	/**
	 * Create a new SceneMouseEvent.
	 */
	public SceneMouseEvent() {
	}

	/**
	 * Create a new SceneMouseEvent.
	 * 
	 * @param source
	 *            the source.
	 * @param target
	 *            the target.
	 * @param modifiers
	 *            the modifiers.
	 * @param x
	 *            the x coordinate in diagram space.
	 * @param y
	 *            the y coordinate in diagram space.
	 * @param button
	 *            the button.
	 * @param clickCount
	 *            the click count.
	 */
	public SceneMouseEvent(final Object source, final ScenePart target, final int modifiers, final int x, final int y,
	        final Button button, final int clickCount) {
		this.target = target;
		this.source = source;
		this.modifiers = modifiers;
		this.x = x;
		this.y = y;
		this.button = button;
		this.clickCount = clickCount;
	}

	/**
	 * Create a new SceneMouseEvent from the specified AWT MouseEvent.
	 * 
	 * @param source
	 *            the source.
	 * @param target
	 *            the target.
	 * @param event
	 *            the mouse event.
	 */
	public SceneMouseEvent(final Object source, final ScenePart target, final MouseEvent event) {
		this.source = source;
		this.target = target;
		modifiers = event.getModifiersEx();
		x = event.getX();
		y = event.getY();
		clickCount = event.getClickCount();
		switch (event.getButton()) {
			case MouseEvent.BUTTON1:
				button = Button.BUTTON1;
				break;
			case MouseEvent.BUTTON2:
				button = Button.BUTTON2;
				break;
			case MouseEvent.BUTTON3:
				button = Button.BUTTON3;
				break;
			default:
				button = Button.NO_BUTTON;
		}
	}

	/**
	 * Gets the button.
	 * 
	 * @return the button.
	 */
	public Button getButton() {
		return button;
	}

	/**
	 * Gets the click count.
	 * 
	 * @return the click count.
	 */
	public int getClickCount() {
		return clickCount;
	}

	/**
	 * Gets the x coordinate in diagram space.
	 * 
	 * @return the x coordinate.
	 */
	public int getX() {
		return x;
	}

	/**
	 * Gets the y coordinate in diagram space.
	 * 
	 * @return the y coordinate.
	 */
	public int getY() {
		return y;
	}

	/**
	 * Set the button.
	 * 
	 * @param button
	 *            the button.
	 */
	public void setButton(final Button button) {
		this.button = button;
	}

	/**
	 * Sets teh click count.
	 * 
	 * @param clickCount
	 *            the click count.
	 */
	public void setClickCount(final int clickCount) {
		this.clickCount = clickCount;
	}

	/**
	 * Sets the x coordinate in diagram space.
	 * 
	 * @param x
	 *            the x coordinate.
	 */
	public void setX(final int x) {
		this.x = x;
	}

	/**
	 * ets the y coordinate in diagram space.
	 * 
	 * @param y
	 *            the y coordinate.
	 */
	public void setY(final int y) {
		this.y = y;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("SceneMouseEvent [");
		sb.append("Source: " + getSource() + ", ");
		sb.append("Target: " + getTarget() + ", ");
		sb.append("Modifiers: " + getModifiers() + ", ");
		sb.append("Alt: " + isAltDown() + ", ");
		sb.append("Ctrl: " + isControlDown() + ", ");
		sb.append("Meta: " + isMetaDown() + ", ");
		sb.append("Shift: " + isShiftDown() + ", ");
		sb.append("Button: " + getButton() + ", ");
		sb.append("Point: (" + getX() + ", " + getY() + "), ");
		sb.append("Click Count: " + getClickCount());
		sb.append("]");
		return sb.toString();
	}
}
