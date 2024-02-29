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
package org.andrill.coretools.misc.io

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.xml.MarkupBuilder

import org.andrill.coretools.model.ModelContainer;
import org.andrill.coretools.model.ModelManager;
import org.andrill.coretools.model.io.ModelReader;
import org.andrill.coretools.model.io.ModelWriter;

import com.google.inject.Inject

/**
 * An XML reader and writer for containers.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
class XMLReaderWriter implements ModelReader, ModelWriter {
	private static Logger logger = LoggerFactory.getLogger(XMLReaderWriter.class)
	private final ModelManager factory
	
	@Inject
	public XMLReaderWriter(ModelManager factory) {
	    this.factory = factory
    }
	
	String getFormat() { "xml" }
	
	// 1/20/2015 brg: Escape any character that isn't low ASCII as their decimal numeric
	// character reference for XML output. MarkupBuilder only escapes the five standard
	// XML entities. Write the result string using mkp.yieldUnescaped - otherwise, MarkupBuilder
	// will escape the '&' in each decimal numeric character reference e.g. &#8220 -> &amp;#8220,
	// which is read as "&#8220;" instead of yielding the expected character. Thus we also
	// escape XML entities [' " < > &] here.
	def escapeHTML(String s) {
		StringBuilder out = new StringBuilder(Math.max(16, s.length()));
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c > 127 || c == '"' || c == '\'' || c == '<' || c == '>' || c == '&') {
				out.append("&#");
				out.append((int) c);
				out.append(';');
			} else {
				out.append(c);
			}
		}
		return out.toString();
	}
	 
	void read(final ModelContainer container, final InputStream stream) {
		long start = System.currentTimeMillis()
		long count = 0
		def xml = new XmlSlurper()
		def root = xml.parse(stream)
		def data = [:]
		root.model.each { m ->
			def type = m?.@type.text()
			data.clear()
			m.property.each { p ->
				// brg 3/11/2014: This is a sinful violation of Josh's design, but I'm struggling to find
				// an elegant, design-friendly solution to the need for project-relative image URLs
				if (type.equals("Image") && p.@name.text().equals("path")) {
					data["path"] = new URL(container.project.path.toString() + p.text().substring(6)) // strip off "file:/"
				} else {
					data[p?.@name.text()] = p.text()
				}
			}

			// create our model
			def model = factory.build(type, data)
			if (model) { 
				container.add(model) 
				count++
			} else {
				logger.warn("Unable to create model for type {}", type)
			}
		}
		logger.debug("Read {} XML models in {} ms", count, (System.currentTimeMillis() - start))
	}
	
	void write(final ModelContainer container, final OutputStream stream) {
		def xml = new MarkupBuilder(new PrintWriter(stream))
		xml.container {
			container.models.each { m ->
				model(type:m.modelType) {
					m.modelData.each { k,v ->
						if (m.modelType.equals("Image") && k.equals("path")) {
							// brg 3/11/2014: strip down URL to image directory
							def imageFile = v.substring(v.lastIndexOf('/') + 1)
							def localURL = new URL("file:/images/${imageFile}")  
							property(name: k, localURL)
						} else {
							// 2/28/2024 brg: Check 'name' fields in addition to 'description'
							// for characters that must be escaped in XML output. Stopgap fix,
							// need general solution, see issue #8 on https://github.com/laccore/coretools
							if (k.equals("description") || k.equals("name")) {
								property(name: k) {
									xml.mkp.yieldUnescaped(escapeHTML(v))
								}
							} else {
								property(name: k, v)
							}
						}
					}
				}
			}
		}
	}
}
