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
package org.andrill.coretools.misc.scheme

import org.andrill.coretools.ResourceLoader
import org.andrill.coretools.model.scheme.Scheme;
import org.andrill.coretools.model.scheme.SchemeEntry;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

/**
 * An implementation of the {@link Scheme} interface.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
public class XMLScheme implements Scheme {
	private static final double THRESHOLD = 0.25
	private final ResourceLoader loader
	
	List<SchemeEntry> entryList = []
	String id = null
	String input = null
	String name = null
	String type = null
	
	@Inject
	XMLScheme(ResourceLoader loader) {
		this.loader = loader
	}

	/**
	 * {@inheritDoc}
	 */
	public SchemeEntry getEntry(String code) {
		def c = code.trim().toLowerCase()
		def match = entryList.find { it.code == c }
		if (match) { return match }

		// fuzzy compare
		int score = THRESHOLD
		entryList.each {
			int s = compare(c, it?.code)
			if (s > score) {
				score = s
				match = it
			}
		}
		return match
	}
	
	public ImmutableSet<SchemeEntry> getEntries() { ImmutableSet.copyOf(entryList) }

	private def compare(str1, str2) {
		def pairs1 = wordLetterPairs(str1)
		def pairs2 = wordLetterPairs(str2)

		int intersection = 0
		int union = pairs1.size() + pairs2.size()
		pairs1.each { pair1 ->
			for(int j = 0; j < pairs2.size(); j++) {
				def pair2 = pairs2[j]
				if (pair1.equals(pair2)) {
					intersection++
					pairs2.remove(j)
					break
				}
			}
		}
		return ((2 * intersection) / union)
	}

	private def letterPairs(String str) {
		int numPairs = str.length() - 1
		def pairs = []
		for (int i = 0; i < numPairs; i++) {
			pairs[i] = str.substring(i, i+2)
		}
		return pairs
	}


	private def wordLetterPairs(String str) {
		def allPairs = []
		str.tokenize(",").each { w ->
			allPairs.addAll(letterPairs(w))
		}
		return allPairs
	}

	/**
	 * {@inheritDoc}
	 */
	public void setInput(String input) {
		this.input = input
		def url = loader.getResource(input)
		if (url) {
			def xml = new XmlSlurper()
			def stream = url.openStream()
			if (stream) {
				def scheme = xml.parse(stream)
				id = scheme?.@id?.text()
				name = scheme?.@name?.text()
				type = scheme?.@type?.text()
				scheme.entry.each { e ->
					String code
					String name
					def props = [:]
					e.property.each { p ->
						if (p?.@name && p?.@value) {
							String key = p.@name.text()
							String value = p.@value.text()
							switch (key) {
								case 'name': name = value; break
								case 'code': code = value.trim().toLowerCase(); break
								default: props[key] = value
							}
						}
					}
					entryList << new SchemeEntry(code, name, props, loader) 
				}
			}
		}
		entryList.each { it.scheme = this }
	}

	String toString() { "Scheme: '$name' [id:$id, type:$type, size:${entryList.size()}]" }
}
