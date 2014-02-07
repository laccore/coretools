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

import java.awt.Rectangle;
import java.util.Map;

import org.andrill.coretools.scene.Scene;
import org.andrill.coretools.scene.Track;

/**
 * A default implementation of the {@link SceneEventHandler} interface.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class DefaultSceneEventHandler implements SceneEventHandler {
	protected final Scene scene;
	protected final Map<Track, Rectangle> layout;
	protected Track track = null;
	protected SceneEventHandler handler = null;

	/**
	 * Create a new DefaultEventHandler.
	 * 
	 * @param scene
	 *            the scene.
	 * @param layout
	 *            the layout.
	 */
	public DefaultSceneEventHandler(final Scene scene, final Map<Track, Rectangle> layout) {
		this.scene = scene;
		this.layout = layout;
	}

	protected SceneEventHandler getHandler(final SceneMouseEvent e) {
		if (e == null) {
			return handler;
		}

		// find our track and adapt it to an event handler
		Track t = null;
		for (Map.Entry<Track, Rectangle> entry : layout.entrySet()) {
			Rectangle r = entry.getValue();
			if ((e.getX() >= r.getMinX()) && (e.getX() <= r.getMaxX())) {
				t = entry.getKey();
			}
		}
		if (t != track) {
			track = t;
			handler = (t == null) ? null : track.getAdapter(SceneEventHandler.class);
		}

		return handler;
	}

	/**
	 * {@inheritDoc}
	 */
	public Feedback keyPressed(final SceneKeyEvent e) {
		SceneEventHandler handler = getHandler(null);
		if (handler != null) {
			return handler.keyPressed(e);
		} else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Feedback keyReleased(final SceneKeyEvent e) {
		SceneEventHandler handler = getHandler(null);
		if (handler != null) {
			return handler.keyReleased(e);
		} else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Feedback keyTyped(final SceneKeyEvent e) {
		SceneEventHandler handler = getHandler(null);
		if (handler != null) {
			return handler.keyTyped(e);
		} else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Feedback mouseClicked(final SceneMouseEvent e) {
		SceneEventHandler handler = getHandler(e);
		if (handler != null) {
			return handler.mouseClicked(e);
		} else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Feedback mouseDragged(final SceneMouseEvent e) {
		SceneEventHandler handler = getHandler(e);
		if (handler != null) {
			return handler.mouseDragged(e);
		} else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Feedback mouseMoved(final SceneMouseEvent e) {
		SceneEventHandler handler = getHandler(e);
		if (handler != null) {
			return handler.mouseMoved(e);
		} else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Feedback mousePressed(final SceneMouseEvent e) {
		SceneEventHandler handler = getHandler(e);
		if (handler != null) {
			return handler.mousePressed(e);
		} else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Feedback mouseReleased(final SceneMouseEvent e) {
		SceneEventHandler handler = getHandler(e);
		if (handler != null) {
			return handler.mouseReleased(e);
		} else {
			return null;
		}
	}
}
