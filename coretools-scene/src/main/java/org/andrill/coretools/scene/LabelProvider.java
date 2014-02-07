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

import org.andrill.coretools.scene.Scene.ScenePart;

/**
 * Defines the interface for a label provider.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public interface LabelProvider {

	/**
	 * Gets the label for the specified screen coordinates.
	 * 
	 * @param screen
	 *            the coordinates.
	 * @param part
	 *            the scene part.
	 * @return the label.
	 */
	String getLabel(Point2D screen, ScenePart part);
}
