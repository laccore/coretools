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
package org.andrill.coretools.misc;

import org.andrill.coretools.misc.io.ExcelReaderWriter;
import org.andrill.coretools.misc.io.LegacyReader;
import org.andrill.coretools.misc.io.XMLReaderWriter;
import org.andrill.coretools.misc.scheme.XMLSchemeFactory;
import org.andrill.coretools.model.io.ModelReader;
import org.andrill.coretools.model.io.ModelWriter;
import org.andrill.coretools.model.scheme.SchemeManager;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class MiscModule extends AbstractModule {
	@Override
	protected void configure() {
	    // configure our XML Scheme Factory
		Multibinder.newSetBinder(binder(), SchemeManager.Factory.class).addBinding().to(XMLSchemeFactory.class);
		
		// configure our ModelReaders
		Multibinder<ModelReader> readers = Multibinder.newSetBinder(binder(), ModelReader.class);
		readers.addBinding().to(LegacyReader.class);
		readers.addBinding().to(XMLReaderWriter.class);
		readers.addBinding().to(ExcelReaderWriter.class);
		
		// configure our ModelWriters
		Multibinder<ModelWriter> writers = Multibinder.newSetBinder(binder(), ModelWriter.class);
		writers.addBinding().to(XMLReaderWriter.class);
		writers.addBinding().to(ExcelReaderWriter.class);
	}
}
