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
package org.andrill.coretools.model.io;

import java.io.IOException;
import java.io.InputStream;

import org.andrill.coretools.model.ModelContainer;

/**
 * Reads models from a resource and adds them to a container.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public interface ModelReader {

	/**
	 * Gets the format of this model reader.
	 * 
	 * @return the format.
	 */
	String getFormat();

	/**
	 * Reads models from the specified stream.
	 * 
	 * @param container
	 *            the container.
	 * @param stream
	 *            the stream.
	 * @throws IOException
	 *             thrown if any errors occur.
	 */
	void read(ModelContainer container, InputStream stream) throws IOException;
}
