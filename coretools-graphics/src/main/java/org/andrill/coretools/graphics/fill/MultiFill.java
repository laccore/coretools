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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A multi fill.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class MultiFill extends Fill {
	private final List<Fill> fills = new ArrayList<Fill>();

	/**
	 * Create a new multi fill.
	 * 
	 * @param fills
	 *            the various fills.
	 */
	public MultiFill(final Fill... fills) {
		super(FillStyle.MULTI);
		this.fills.addAll(Arrays.asList(fills));
	}

	/**
	 * Gets the fills.
	 * 
	 * @return the fills.
	 */
	public List<Fill> getFills() {
		return fills;
	}
}