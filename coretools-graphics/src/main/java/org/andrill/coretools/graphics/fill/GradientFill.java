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
package org.andrill.coretools.graphics.fill;

import java.awt.Color;

/**
 * A gradient fill.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class GradientFill extends Fill {
	private final boolean horizontal;
	private final Color start, end;

	/**
	 * Create a new gradient fill.
	 * 
	 * @param start
	 *            the starting color.
	 * @param end
	 *            the ending color.
	 * @param horizontal
	 *            true if a horizontal gradient, false otherwise.
	 */
	public GradientFill(final Color start, final Color end, final boolean horizontal) {
		super(FillStyle.GRADIENT);
		this.start = start;
		this.end = end;
		this.horizontal = horizontal;
	}

	/**
	 * Get the ending color.
	 * 
	 * @return the ending color.
	 */
	public Color getEnd() {
		return end;
	}

	/**
	 * Get the starting color.
	 * 
	 * @return the starting color.
	 */
	public Color getStart() {
		return start;
	}

	/**
	 * Get the horizontal flag.
	 * 
	 * @return the horizontal flag.
	 */
	public boolean isHorizontal() {
		return horizontal;
	}
}