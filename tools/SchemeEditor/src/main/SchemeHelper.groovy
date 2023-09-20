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
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.TexturePaint
import java.util.jar.JarFile
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import javax.imageio.ImageIO
import javax.swing.Icon
import javax.swing.ImageIcon

import groovy.xml.MarkupBuilder

import com.lowagie.text.Document
import com.lowagie.text.DocumentException
import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.PdfContentByte
import com.lowagie.text.pdf.PdfWriter

public class SchemeHelper {
	public IMAGE_EXTENSIONS = ['.bmp', '.gif', '.jpeg', '.jpg', '.png', '.tif', '.tiff']
	
	/**
	 * Create a new SchemeHelper
	 */
	public SchemeHelper() { }

	/**
	 * Parse a color from a RGB tuple, e.g. '100,100,100'.
	 */
	Color parseColor(color) {
    	if (color) {
			def rgb = color.split(",")
			return new Color(rgb[0] as int, rgb[1] as int, rgb[2] as int)
		} else {
			return Color.WHITE // default to white for null Color as we do in PSICAT
		}
    }
	
	// todo: make all SchemeHelper methods static
	static parseColorString(color) {
		if (color) {
			def rgb = color.split(",")
			return new Color(rgb[0] as int, rgb[1] as int, rgb[2] as int)
		} else {
			return Color.WHITE // default to white for null Color as we do in PSICAT
		}
	}
	
	static codeFromName(name, rc=",") {
		def code = name.toLowerCase()
		code = code.replace(" ", "$rc")
		code = code.replace("$rc$rc", "$rc")
		return code
	}
	
	static String createUniqueCode(name, schemeEntries) {
		def result = null
		def baseCode = codeFromName(name)
		int suffixNum = 1
		def suffix = ""
		while (true) {
			def code = baseCode + suffix
			if (schemeEntries.find { it.code?.equals(code) } != null) {
				suffix = "$suffixNum"
				suffixNum++
			} else {
				result = code
				break
			}
		}
		return result
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
	
	/**
	 * Creates an icon for a path.
	 */
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
		def url = null
		if (path) {
    		if (path.startsWith("rsrc:/")) {
				url =  getClass()?.getResource(path.substring(5))
				if (url == null) {
					// 10/20/2021 brg: Ensure cacheDir isn't null, otherwise we run into
					// an ambiguous overload error in the File c'tor (is the first arg a
					// String or File?).
					def safeCacheDir = cacheDir ?: new File(".")
					File cacheFile = new File(safeCacheDir, path.substring(path.lastIndexOf('/') + 1))
					if (cacheFile.exists()) {
						url = cacheFile.toURL()
					}
				}
    		} else if (path.contains(":/")) {
				url =  new URL(path)
    		} else {
				url = new File(path).toURL()
    		}
    	}
		return url
    }

	// 12/17/2014 brg: Abandoning use of adding scheme files to classloader as a 
	// means of caching images. Saving a scheme already in the classloader caused two
	// problems: 1) certain images suddenly stopped loading and 2) (sometimes) crashes
	// on OSX. We now cache images as files in a temp directory, which has proven more
	// reliable so far (despite my crude implementation).
    private File cacheDir = null
	
	/**
	 * Adds a file to the cache
	 */
	def add(url) {
		initCacheDir()
		ZipFile jar = new ZipFile(url.getPath())
		jar.entries().each {
			def dotIndex = it.name.lastIndexOf('.')
			if (dotIndex != -1) {
				def ext = it.name.substring(dotIndex).toLowerCase()
				if (IMAGE_EXTENSIONS.contains(ext)) {
					def fname = it.name.substring(it.name.lastIndexOf("/") + 1)
					File outFile = new File(cacheDir, fname)
					InputStream fis = jar.getInputStream(jar.getEntry(it.name))
					FileOutputStream fos = new FileOutputStream(outFile)
					while (fis.available() > 0) { fos.write(fis.read()); }
					fis.close()
					fos.close()
				}
			}
		}
	}
	
	/**
	 *  Adds a file stream to the cache
	 */
	def addToCache(instream, name) {
		initCacheDir()
		File outFile = new File(cacheDir, name)
		if (outFile.exists()) return
		FileOutputStream fos = new FileOutputStream(outFile)
		while (instream.available() > 0) { fos.write(instream.read()) }
		instream.close()
		fos.close()
	}
	
	def isCached(name) {
		initCacheDir()
		File f = new File(cacheDir, name)
		return f.exists()
	}
	
	def initCacheDir() {
		if (!cacheDir) {
			try {
				def baseDir = new File(System.getProperty("java.io.tmpdir"))
				String baseName = "schemeEditor-" + System.currentTimeMillis()
				cacheDir = new File(baseDir, baseName)
				cacheDir.mkdir()
			} catch (IllegalStateException e) {
				System.out.println("failed to create temp directory")
			}
		}
	}
	
	/**
	 * Read a scheme from a stream.
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
							
							if (!isCached(f))
								url.withInputStream { stream -> addToCache(stream, f) }
							
							zip.closeEntry()
							images << f
						}
						e.image = "rsrc:/${pkg + f}".toString()
					}
				}
				
				// write our scheme xml
				zip.putNextEntry(new ZipEntry("scheme.xml"))
				def writer = new StringWriter()

				// 7/22/2023 brg: Tried using MarkupBuilder to write scheme XML,
				// but couldn't prevent escaping of ampersand (&) in UTF-8 encoding
				// in a value attribute to "&amp;", which hoses parsing of the UTF-8 char. Modern
				// Groovy has the setting we need to fix this (escapeAttributes), but our ancient Groovy
				// does not. Alas.
				// Thankfully, the scheme XML structure is very simple; write it manually.
				writer.write("<?xml version='1.0' encoding='UTF-8'?>")
				def schemeXml = "<scheme id='${scheme?.id}' name='${scheme?.name}' type='${scheme?.type}'>\n"
				writer.write(schemeXml)

				scheme.entries.each() { e ->
					writer.write("  <entry>\n")
					e.each() { k,v ->
						writer.write("    <property ")
						if (k != "icon") {
							def xmlValue = (k == "name" ? escapeHTML(v) : v);
							writer.write("name='${k}' value='${xmlValue}' ")
							def entryXml
						}
						writer.write("/>\n")
					}
					writer.write("  </entry>\n")
				}

				writer.write("</scheme>")

				zip << writer.toString()
				zip.closeEntry()
				zip.close()
			}
			
			// all's gone well, dump contents of tmp into destination file - File.renameTo()
			// is notoriously unreliable and isn't working on Win7.
			def inStream = new FileInputStream(tmp)
			
			// add .jar extension if necessary
			def outFile = file
			if (!file.name.endsWith(".jar")) {
				outFile = new File(file.parentFile, file.name + ".jar")
			}
			
			def outStream = new FileOutputStream(outFile)
			byte[] buf = new byte[1024]
			int len = 0
			while ((len = inStream.read(buf)) > 0) { outStream.write(buf, 0, len) }
			inStream.close()
			outStream.close()
			tmp.delete()
		} catch (e) {
			e.printStackTrace()
			throw e // rethrow for client to handle
		}
	}
	
	def wrap(text, fontMetrics, maxWidth) {
		def lines = []
		def width = 0
		def curLine = ""
		text.split(" ").each { word ->
			def wordWidth = fontMetrics.stringWidth(word + " ") 
			if (width + wordWidth > maxWidth) {
				lines << curLine
				curLine = word + " "
				width = wordWidth
			} else {
				curLine += (word + " ")
				width += wordWidth
			}
		}
		lines << curLine
	}

	// 9/20/2023 brg: copy/paste from coretools-misc XMLReaderWriter.groovy
	private escapeHTML(String s) {
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
	
	// assuming 8.5 x 11" for pagination
	def exportCatalog(paginate, destFile, schemeEntries, isLithology, schemeName, schemeId) {
		final int TITLE_HEIGHT = 20
		final int MARGIN = 36 // 1/2"
		final int SYMBOL_LARGE = 32
		final int SYMBOL_SMALL = 16
		final int INTERSYMBOL_PADDING = 8
		final int SYMBOL_IMAGES_WIDTH = SYMBOL_LARGE + INTERSYMBOL_PADDING + SYMBOL_SMALL

		final int entryPadding = 5
		final int entryWidth = isLithology ? 130 : 260
		final int entryHeight = isLithology ? 170 : 40
		final int textWidth = isLithology ? entryWidth : (entryWidth - SYMBOL_IMAGES_WIDTH) // symbol: subtract large and small image width plus 5pix padding 

		final int width = 612 // 8.5" wide
		final int entriesPerRow = (width / entryWidth)
		final int height = paginate ? 792 : (schemeEntries.size() / entriesPerRow + 1) * (entryHeight) + TITLE_HEIGHT // 11" high if paginated
    	
        Document document = new Document(new Rectangle(width, height))
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(destFile))
        document.open();
        PdfContentByte content = writer.getDirectContent();
		
		def g2 = null
		def template = null
		def lastPage = false
		
		int startIdx = 0
		int entriesPerPage = paginate ? (((height - MARGIN * 2) / entryHeight) * entriesPerRow).intValue() : schemeEntries.size()
		
		while (!lastPage) {
			// start a page
			if (g2 != null) g2.dispose()
			if (template != null) {
				content.addTemplate(template, 0, 0)
				document.newPage()
			}
			template = content.createTemplate(width, height)
			g2 = content.createGraphics(width, height)
			g2.translate(MARGIN, MARGIN)
			
			// draw title
			g2.setFont(new Font("SansSerif", Font.PLAIN, 14))
			def dateStr = new Date().toString()
			def titleStr = "$schemeName ($schemeId)"
			g2.drawString(titleStr, 0, 0)
	
			// draw tiles
			g2.setFont(new Font("SansSerif", Font.PLAIN, 10))
			def fontMetrics = g2.fontMetrics
			def letterHeight = fontMetrics.height
			def row = 0, col = 0

			int endIdx = startIdx + entriesPerPage - 1
			if (endIdx >= schemeEntries.size() - 1) {
				endIdx = schemeEntries.size() - 1
				lastPage = true
			}
			
			def entries = schemeEntries[startIdx..endIdx]
			startIdx += entriesPerPage
			
			entries.eachWithIndex { entry, index ->
				def x = col * (entryWidth + entryPadding)
				def y = TITLE_HEIGHT + row * (entryHeight)
	
				def color = entry.color ?: Color.white
				
				if (isLithology) {
					g2.setPaint(parseColor(entry.color))
					g2.fillRect(x.intValue(), y.intValue(), entryWidth, entryWidth)
				}
				
				if (entry.image) {
					def image = null
					try {
						image = ImageIO.read(resolve(entry.image))
					} catch (IOException e) {
						println "Couldn't load image ${entry.image}"
					}

					if (image) {
						if (isLithology) {
							g2.setPaint(new TexturePaint(image, new java.awt.Rectangle(x, y, image.width, image.height)))
							g2.fillRect(x, y, entryWidth, entryWidth)
						} else {
							// PSICAT diagram output-sized image
							g2.setPaint(new TexturePaint(image, new java.awt.Rectangle(x, y, image.width, image.height)))
							g2.fillRect(x, y, SYMBOL_LARGE, SYMBOL_LARGE)
							
							def space = SYMBOL_LARGE + INTERSYMBOL_PADDING
							
							// in-PSICAT-sized image (smaller)
							g2.setPaint(new TexturePaint(image, new java.awt.Rectangle(x + space, y, (image.width / 2).intValue(), (image.height / 2).intValue())))
							g2.fillRect(x + space, y, SYMBOL_SMALL, SYMBOL_SMALL)
						}
					} else {
						println "no image found for ${entry.name}"
					}
				}
				
				// draw entry name, wrapped if needed			
				g2.setPaint(Color.BLACK)
				def nameLines = wrap(entry.name, fontMetrics, textWidth)
				nameLines.eachWithIndex { line, curLine ->
					if (isLithology)
						g2.drawString(line, x, y + entryWidth + letterHeight * (1 + curLine))
					else
						g2.drawString(line, x + 16 + 40 + entryPadding, y + letterHeight * (1 + curLine))
				}
	
				// advance column and row
				col++
				if (col >= entriesPerRow) {
					row++
					col = 0
				}
			}
		}
		
		if (g2 != null) {
			g2.dispose();
			content.addTemplate(template, 0, 0)	
		}
        document.close();
	}
}
