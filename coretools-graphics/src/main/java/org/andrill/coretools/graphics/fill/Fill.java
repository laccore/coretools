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

/**
 * The fill class.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public abstract class Fill {

	/**
	 * The fill style.
	 */
	public enum FillStyle {
		COLOR, GRADIENT, MULTI, TEXTURE
	}

	protected final FillStyle style;

	/**
	 * Create a new fill with the specified style.
	 * 
	 * @param style
	 *            the style.
	 */
	public Fill(final FillStyle style) {
		this.style = style;
	}

	/**
	 * Gets the fill style.
	 * 
	 * @return the style.
	 */
	public FillStyle getStyle() {
		return style;
	}
}
