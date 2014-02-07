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
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.andrill.coretools.model.Model;
import org.andrill.coretools.model.edit.Command;
import org.andrill.coretools.model.edit.CommandStack;
import org.andrill.coretools.model.edit.EditableProperty;
import org.andrill.coretools.scene.Selection;
import org.andrill.coretools.scene.Track;
import org.andrill.coretools.scene.Scene.ScenePart;
import org.andrill.coretools.scene.event.EventPolicy.Type;

/**
 * An event handler for tracks.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class DefaultTrackEventHandler implements SceneEventHandler {
	protected Track track = null;
	protected boolean moved = false;
	protected boolean resized = false;
	protected Model target = null;
	protected int handle = -1;
	protected Point point = null;
	protected Map<Type, EventPolicy> policyMap = new HashMap<Type, EventPolicy>();

	/**
	 * Create a new AbstractTrackEventHandler for the specified track.
	 * 
	 * @param track
	 *            the track.
	 */
	public DefaultTrackEventHandler(final Track track, final List<EventPolicy> policies) {
		this.track = track;
		for (EventPolicy p : policies) {
			p.setTrack(track);
			policyMap.put(p.getType(), p);
		}
	}

	/**
	 * Execute the specified command.
	 * 
	 * @param command
	 *            the command.
	 */
	protected void execute(final Command command) {
		if (command != null) {
			CommandStack edit = track.getScene().getCommandStack();
			if ((edit != null) && edit.canExecute() && command.canExecute()) {
				edit.execute(command);
			}
		}
	}

	protected Command getCommand(final Type type, final SceneEvent event, final Object target) {
		EventPolicy policy = policyMap.get(type);
		return (policy == null) ? null : policy.getCommand(event, target);
	}

	protected Feedback getFeedback(final Type type, final SceneEvent event, final Object target) {
		EventPolicy policy = policyMap.get(type);
		return (policy == null) ? null : policy.getFeedback(event, target);
	}

	protected int getHandle(final SceneMouseEvent e, final Model model) {
		if (model == null) {
			return 0;
		}

		EditableProperty[] properties = model.getAdapter(EditableProperty[].class);
		if (properties == null) {
			return 0;
		} else {
			Rectangle2D r = track.getModelBounds(model);
			for (EditableProperty p : properties) {
				String handle = p.getConstraints().get("handle");
				if ((handle == "north") && (Math.abs(r.getMinY() - e.y) <= 5)) {
					return Cursor.N_RESIZE_CURSOR;
				} else if ((handle == "south") && (Math.abs(r.getMaxY() - e.y) <= 5)) {
					return Cursor.S_RESIZE_CURSOR;
				} else if ((handle == "east") && (Math.abs(r.getMaxX() - e.x) <= 5)) {
					return Cursor.E_RESIZE_CURSOR;
				} else if ((handle == "west") && (Math.abs(r.getMinX() - e.x) <= 5)) {
					return Cursor.W_RESIZE_CURSOR;
				}
			}
		}
		return 0;
	}

	/**
	 * Gets the model.
	 * 
	 * @param e
	 *            the model.
	 * @return the model or null.
	 */
	protected Model getModel(final SceneMouseEvent e) {
		Object o = track.findAt(new Point(e.getX(), e.getY()), e.getTarget());
		if (o instanceof Model) {
			return (Model) o;
		} else {
			return null;
		}
	}

	/**
	 * Gets the current selection from the scene.
	 * 
	 * @return the selection.
	 */
	protected Selection getSelection() {
		return track.getScene().getSelection();
	}

	/**
	 * {@inheritDoc}
	 */
	public Feedback keyPressed(final SceneKeyEvent e) {
		return getFeedback(Type.KEY, e, (target == null ? track : target));
	}

	/**
	 * {@inheritDoc}
	 */
	public Feedback keyReleased(final SceneKeyEvent e) {
		execute(getCommand(Type.KEY, e, (target == null ? track : target)));
		return null; // no feedback
	}

	/**
	 * {@inheritDoc}
	 */
	public Feedback keyTyped(final SceneKeyEvent e) {
		return null; // no feedback
	}

	/**
	 * {@inheritDoc}
	 */
	public Feedback mouseClicked(final SceneMouseEvent e) {
		return null; // no feedback
	}

	/**
	 * {@inheritDoc}
	 */
	public Feedback mouseDragged(final SceneMouseEvent e) {
		// save our drag start
		if (point == null) {
			point = new Point(e.getX(), e.getY());
		}

		// set the drag start on our event
		e.dragX = point.x;
		e.dragY = point.y;

		// dispatch the event
		if (target == null) {
			return getFeedback(Type.CREATE, e, track);
		} else {
			if (handle == -1) {
				handle = getHandle(e, target);
			}
			if (handle == 0) {
				moved = true;
				return getFeedback(Type.MOVE, e, target);
			} else {
				resized = true;
				return getFeedback(Type.RESIZE, e, target);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Feedback mouseMoved(final SceneMouseEvent e) {
		if (e.getTarget() == ScenePart.CONTENTS) {
			Model model = getModel(e);
			if (model == null) {
				return getFeedback(Type.CREATE, e, track);
			} else {
				int handle = getHandle(e, model);
				if (handle == 0) {
					return getFeedback(Type.MOVE, e, model);
				} else {
					return getFeedback(Type.RESIZE, e, model);
				}
			}
		} else {
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Feedback mousePressed(final SceneMouseEvent e) {
		if (e.getTarget() == ScenePart.CONTENTS) {
			Model model = getModel(e);
			target = model;
		} else {
			target = null;
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Feedback mouseReleased(final SceneMouseEvent e) {
		// set the drag start on our event
		if (point != null) {
			e.dragX = point.x;
			e.dragY = point.y;
		}

		// dispatch the event
		if (e.getTarget() == ScenePart.CONTENTS) {

			if (target == null) {
				execute(getCommand(Type.CREATE, e, track));
			} else if (moved) {
				execute(getCommand(Type.MOVE, e, target));
			} else if (resized) {
				execute(getCommand(Type.RESIZE, e, target));
			}
		}

		// clear state
		target = null;
		moved = false;
		resized = false;
		handle = -1;
		point = null;

		// return no feedback
		return null;
	}
}
