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
package org.andrill.coretools.misc.scheme;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.andrill.coretools.ResourceLoader;
import org.andrill.coretools.model.scheme.Scheme;
import org.andrill.coretools.model.scheme.SchemeManager.Factory;

import com.google.inject.Inject;

/**
 * A factory for creating {@link XMLScheme} services.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class XMLSchemeFactory implements Factory {
	private final ResourceLoader loader;
	
	@Inject
	XMLSchemeFactory(ResourceLoader loader) {
	    this.loader = loader;
    }

	public Set<Scheme> getSchemes() {
		Set<Scheme> discovered = new HashSet<Scheme>();
		for (URL url : loader.getResources("classpath:scheme.xml")) {
			XMLScheme scheme = new XMLScheme(loader);
			scheme.setInput(url.toExternalForm());
			discovered.add(scheme);
		}
		return discovered;
    }
}
