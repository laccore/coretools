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
package org.andrill.coretools.misc.util;

import org.andrill.coretools.Platform;
import org.andrill.coretools.scene.Scene
import org.andrill.coretools.scene.Track;
import groovy.xml.MarkupBuilder;

import org.andrill.coretools.scene.DefaultScene

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Utility methods for reading and writing scenes.
 */
class SceneUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(SceneUtils.class)
	private SceneUtils() {}

	/**
	 * Load a scene from the XML format.
	 */
	static Scene fromXML(URL url) {
		def xml = new XmlSlurper()
		def root = xml.parse(url.openStream())
		
		// create and configure the scene
		Scene scene = ((root?.@class) ? instance(root.@class) : new DefaultScene())
		if (!scene) { return null }
		configure(scene, root)
		
		// create and configure the tracks
		root.track.each { t ->
			Track track = instance(t.@class)
			if (track) {
				configure(track, t)
				scene.addTrack(track, t?.@constraints.text())
			}
		}
		return scene
	}
	
	static void toXML(Scene scene, Writer writer) {
		def xml = new MarkupBuilder(writer)
		xml.scene('class':scene.class.name) {
			scene.parameters.each { k, v -> param(name: k, value: v) }
			scene.tracks.each { t ->
				track('class':t.class.name, 'constraints':scene.getTrackConstraints(t)) {
					track.parameters.each { k, v -> param(name: k, value: v) }
				}
			}
		}
	}
	
	private static void configure(obj, node) {
		node.param.each { p ->
			String name = p?.@name?.text()
			String value = p?.@value?.text()
			if (name) {
				obj.setParameter(name, value)
			}
		}
	}
	
	private static def instance(clazz) {
		if (clazz) {
			try {
				LOGGER.debug("Creating instance of {}", clazz.toString())
				return Platform.getService(clazz.toString() as Class)
			} catch (e) {
				LOGGER.error("Unable to instantiate class {} {}", clazz, e.message)
			}
		}
	}
}