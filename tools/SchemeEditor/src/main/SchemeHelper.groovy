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
import java.awt.Color
import java.awt.Image
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIOimport javax.swing.Icon
import javax.swing.ImageIcon
import groovy.lang.GroovyClassLoaderimport groovy.xml.MarkupBuilder

public class SchemeHelper {
	private GroovyClassLoader classloader		/**	 * Create a new SchemeHelper	 */	public SchemeHelper(classloader) {		this.classloader = classloader	}

	/**
	 * Parse a color from a RGB tuple, e.g. '100,100,100'.
	 */
	Color parseColor(color) {
    	if (color) {
			def rgb = color.split(",")
			return new Color(rgb[0] as int, rgb[1] as int, rgb[2] as int)
		} else {
			return null
		}
    }
	
	/**
	 * Parses an image from a path.
	 */
	Image parseImage(path) {
		def url = resolve(path)
		if (url) {
			url.withInputStream { stream ->
				return ImageIO.read(stream)
			}
		} else {
			return null
		}
	}
		/**	 * Creates an icon for a path.	 */
	Icon iconify(path) {
    	def image = parseImage(path)
		if (image) {
			int w = (int) Math.min(96, image.width)
			int h = (int) Math.min(w, image.height)
			return new ImageIcon(image.getSubimage(0, 0, w, h).getScaledInstance(32, 32, Image.SCALE_AREA_AVERAGING))
		}
    }
    
	/**
	 * Resolve a string into a URL, with special support for rsrc:/ paths.
	 */
    URL resolve(path) {
		if (path) {
    		if (path.startsWith("rsrc:/")) {
    			return getClass()?.getResource(path.substring(5)) ?: classloader?.getResource(path.substring(6))
    		} else if (path.contains(":/")) {
    			return new URL(path)
    		} else {
    			return new File(path).toURL()
    		}
    	} else {
    		return null
    	}
    }		/**	 * Adds a file to the search path	 */	def add(url) {		classloader.addURL(url)	}
	
	/**
	 * Read a scheme a stream.
	 */
	def read(stream) {
		def scheme = [ id:"", name:"", type:"", entries:[] ]
		
		def xml = new XmlSlurper()
		def root = xml.parse(stream)
		scheme.id = root?.@id?.toString()
		scheme.name = root?.@name?.toString()
		scheme.type = root?.@type?.toString()
		root.entry.each { e ->
			def props = [:]
			e.property.each { p ->
				if (p?.@name && p?.@value) {
					props[p.@name.toString()] = p.@value.toString()
				}
			}
			scheme.entries << props
		}
		return scheme
	}
	
	/**
	 * Writes a scheme to a stream
	 */
	def write(scheme, file) {
		// get our package
		def pkg = "${scheme.id.replace('.', '/')}/"

		// create our zip file
		def tmp = new File(file.parentFile, file.name + ".tmp")
		tmp.createNewFile()
		try {
			tmp.withOutputStream() { fos ->
				def zip = new ZipOutputStream(fos)
				
				// handle custom images
				def images = []
				scheme.entries.each { e ->
					if (e?.image) {
						def url = resolve(e.image)
						def f = url.file.substring(url.file.lastIndexOf('/') + 1)
						if (!images.contains(f)) {
							zip.putNextEntry(new ZipEntry(pkg + f))
							url.withInputStream { stream ->
								zip << stream
							}
							zip.closeEntry()
							images << f
						}
						e.image = "rsrc:/${pkg + f}".toString()
					}
				}
				
				// write our scheme xml
				zip.putNextEntry(new ZipEntry("scheme.xml"))
				def writer = new StringWriter()
				def xml = new MarkupBuilder(writer)
			    xml.scheme(id: scheme?.id, name: scheme?.name, type: scheme?.type) {
				    scheme.entries.each() { e ->
				    	entry() {
				    		e.each { k,v ->
				    			if (k != "icon") {
				    				property(name: k, value: v)
				    			}
				    		}
				    	}
				    }
				}
				zip << writer.toString()
				zip.closeEntry()
				zip.close()
			}

			// all's gone well, dump contents of tmp into destination file - File.renameTo()
			// is notoriously unreliable and isn't working on Win7.
			def inStream = new FileInputStream(tmp)
			def outStream = new FileOutputStream(file)
			byte[] buf = new byte[1024]
			int len = 0
			while ((len = inStream.read(buf)) > 0) { outStream.write(buf, 0, len) }
			inStream.close()
			outStream.close()
			tmp.delete()
		} catch (e) {
			e.printStackTrace()
		}
	}
}
