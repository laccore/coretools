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

/**
 * Defines the interface for a scene event handler.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public interface SceneEventHandler {

	/**
	 * Fired when a key is pressed.
	 * 
	 * @param e
	 *            the event.
	 */
	Feedback keyPressed(SceneKeyEvent e);

	/**
	 * Fired when a key is released.
	 * 
	 * @param e
	 *            the event.
	 */
	Feedback keyReleased(SceneKeyEvent e);

	/**
	 * Fired when a key is typed.
	 * 
	 * @param e
	 *            the event.
	 */
	Feedback keyTyped(SceneKeyEvent e);

	/**
	 * Fired when the mouse is clicked.
	 * 
	 * @param e
	 *            the event.
	 */
	Feedback mouseClicked(SceneMouseEvent e);

	/**
	 * Fired when the mouse is dragged.
	 * 
	 * @param e
	 *            the event.
	 */
	Feedback mouseDragged(SceneMouseEvent e);

	/**
	 * Fired when the mouse is moved.
	 * 
	 * @param e
	 *            the event.
	 */
	Feedback mouseMoved(SceneMouseEvent e);

	/**
	 * Fired when the mouse is pressed.
	 * 
	 * @param e
	 *            the event.
	 */
	Feedback mousePressed(SceneMouseEvent e);

	/**
	 * Fired when the mouse is released.
	 * 
	 * @param e
	 *            the event.
	 */
	Feedback mouseReleased(SceneMouseEvent e);
}
