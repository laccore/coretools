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

import org.andrill.coretools.model.edit.Command;
import org.andrill.coretools.scene.Track;

/**
 * Defines the interface for an EventPolicy. An EventPolicy takes an event and converts it into a {@link Feedback} and a
 * {@link Command}.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public interface EventPolicy {
	public enum Type {
		CREATE, MOVE, RESIZE, KEY
	}

	/**
	 * Gets the {@link Command} for the specified event.
	 * 
	 * @param e
	 *            the event.
	 * @param m
	 *            the target.
	 * @return the command or null.
	 */
	Command getCommand(SceneEvent e, Object m);

	/**
	 * Gets the {@link Feedback} for the specified event.
	 * 
	 * @param e
	 *            the event.
	 * @param m
	 *            the target.
	 * @return the feedback or null.
	 */
	Feedback getFeedback(SceneEvent e, Object m);

	/**
	 * Gets the track associated with this policy.
	 * 
	 * @return the track.
	 */
	Track getTrack();

	/**
	 * Gets the type of this {@link EventPolicy}.
	 * 
	 * @return the type.
	 */
	EventPolicy.Type getType();

	/**
	 * Sets the track associated with this policy.
	 * 
	 * @param track
	 *            the track.
	 */
	void setTrack(Track track);
}
