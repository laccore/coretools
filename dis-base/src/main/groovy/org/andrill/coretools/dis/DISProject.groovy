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
package org.andrill.coretools.dis

import groovy.xml.StreamingMarkupBuilder 
import org.andrill.coretools.model.scheme.SchemeEntry;

import java.io.File
import java.net.URLEncoder;
import org.andrill.coretools.dis.models.SectionUnit 
import org.andrill.coretools.dis.models.MunsellColor
import org.andrill.coretools.model.scheme.DefaultScheme 
import org.andrill.coretools.model.scheme.SchemeManager
import org.andrill.coretools.geology.models.Image 
import org.andrill.coretools.geology.models.Length 
import org.andrill.coretools.geology.models.Section 
import org.andrill.coretools.model.AbstractProject 
import org.andrill.coretools.model.Model 
import org.andrill.coretools.model.ModelContainer
import org.andrill.coretools.Platform
import org.andrill.coretools.ResourceLoader

import org.slf4j.Logger 
import org.slf4j.LoggerFactory

/**
 * An implementation of the Project interface for DIS projects.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
class DISProject extends AbstractProject implements ModelContainer.Listener {
	private static final Logger LOGGER = LoggerFactory.getLogger(DISProject.class)
	private Map sections = [:]
	private Map cores = [:]
	private Map status = [:]
	private File disFile = null
	
	/**
	 * Create a new DIS project for the specified path.
	 * 
	 * @param path the URL to the DIS exchange file.
	 */
	DISProject(URL path) {
		this.path = path
		File file = new File(path.toURI())
		if (file.exists()) { disFile = file }
		init()
	}
	
	/**
	 * Create a new DIS project for the specified file.
	 * 
	 * @param file the DIS exchange file.
	 */
	DISProject(File file) {
		this(file.toURI().toURL())
	}
	
	protected void closed(ModelContainer container) {
		container.removeListener(this)
		container.models.each { status.remove(it) }
	}
	
	@Override
	protected ModelContainer create(String name) {
		throw new UnsupportedOperationException('Creating containers is not supported for DIS projects')
	}
	
	@Override
	protected ModelContainer open(String name) {
		ModelContainer container = Platform.getService(ModelContainer.class)
		def section = sections[name]
		def file = asFile(section)
		if (file && file.exists()) {
			try {
				def parsed = new XmlSlurper().parse(file).SITE.HOLE.CORE.SECTION[0]
				parseSection(parsed, container)
			} catch (e) {
				LOGGER.warn("Unable to parse ${file.absolutePath}", e)
				parseSection(section, container)
			}
		} else {
			parseSection(section, container)
		}
		container.addListener(this)
		return container;
	}
	
	protected URL resolve(String path) {
		// try the direct route
		if (disFile) {
			// make a relative path
			String relative = (path - 'FILE://').replace('\\' as char, File.separatorChar)
			int colon = relative.indexOf(':')
			if (colon >= 0) {
				relative = relative[colon+2..-1]
			}
			
			File parent = disFile.parentFile
			while (parent) {
				File file = new File(parent, relative)
				if (file.exists()) {
					return file.toURI().toURL()
				}
				parent = parent.parentFile
			}
		}
		
		// try URL directly
		try { 
			return new URL(path.replace('\\' as char, '/' as char))
		} catch (e) { 
			/* ignore */
		}
		
		// try file
		path -= 'FILE://'
		File file = new File(path)
		if (file.exists()) {
			return file.toURI().toURL()
		}
		
		return null
	}
	
	protected void parseSection(node, container) {
		// create a section
		Section section = new Section()
		section.top = new Length(node.@top_depth.text() as BigDecimal, 'm')
		section.base = section.top + new Length(node.@length.text() as BigDecimal, 'm')
		section.name = node.@name.text()
		section.core = node.parent().@name.text()
		container.add(section)
		
		// add any images
		node.IMAGE.each { imgNode ->
			// figure out any offsets
			String attr = node.parent().parent().parent().parent().@bottomoffset
			def image = new Image()
			image.top = section.top
			image.base = section.base + (attr ? attr as BigDecimal : 0)
			image.path = resolve(imgNode.@url.text())
			container.add(image)
		}
		
		// create any section units
		def intervals = node.SECTIONUNIT.collect { unitNode ->
			// create a new section unit
			def interval = new SectionUnit()
			interval.id = unitNode.@id.text()
			interval.name = unitNode.@name.text()
			
			// populate the rest if not deleted
			def state = unitNode?.@status?.text()
			if (state != 'R') {
				unitNode.attributes().each { key, value ->					
					switch(key) {
						case 'top_interval':	interval.top = (section.top + new Length(value as BigDecimal, 'cm')).cm; break
						case 'bottom_interval':	interval.base = (section.top + new Length(value as BigDecimal, 'cm')).cm; break
						case 'UnitType':		interval.unitType = "dis-units:${value}"; break
						case 'MajorLithology':	interval.lithology = "dis-lithologies:${value}"; break
						case 'ratio':			interval.ratio = (value ?: '100') as Integer; break
						case 'Description':		interval.description = value; break
						case ~/GRAINSIZE.*/:
							interval."grainSize${key.split('_')[1]}" = "dis-grainsize:${value}"
							break
						case ~/SED_STRUCTURE.*/:
							interval."structure${key.split('_')[2]}" = "dis-structures:${value}"
							break
						case ~/TEXTURES.*/:	
							interval."texture${key.split('_')[1]}" = "dis-textures:${value}"
							break
						case ~/COMPONENT.*/:	
							interval."component${key.split('_')[1]}" = "dis-components:${value}"
							break
						case ~/HUE.*/:
							int index = 1
							if (key.contains('_')) { index = key.split('_')[1] as int }
							if (!interval."color${index}") { interval."color${index}" = new MunsellColor() }
							interval."color${index}".hue = value
							break
						case ~/VALUE.*/:
							int index = 1
							if (key.contains('_')) { index = key.split('_')[1] as int }
							if (!interval."color${index}") { interval."color${index}" = new MunsellColor() }
							interval."color${index}".value = value
							break
						case ~/CHROMA.*/:
							int index = 1
							if (key.contains('_')) { index = key.split('_')[1] as int }
							if (!interval."color${index}") { interval."color${index}" = new MunsellColor() }
							interval."color${index}".chroma = value
							break
						case ['id', 'name', 'UnitClass', 'status']: break
						default:
							LOGGER.info('Unhandled DIS attribute: {}={}', key, value)
					}
				}
			}
			
			// set our state
			status[interval] = new Status(section: node.@name.text(), id: interval.id, status: state)
			container.add(interval)
			return interval
		}
	}
	
	private SchemeEntry build(code, props = [:]) {
		if (code.endsWith('clay')) {
			props.color = '204,204,204'
			props.image = getClass().getResource('clay.png').toString()
		} else if (code.endsWith('silt')) {
			props.color = '51,255,0'
			props.image = getClass().getResource('silt.png').toString()
		} else if (code.endsWith('sand')) {
			props.color = '255,255,0'
			props.image = getClass().getResource('sand.png').toString()
		} else if (code.endsWith('gravel') || code.endsWith('cobble')) {
			props.color = '51,102,0'
			props.image = getClass().getResource('gravel.png').toString()
		} else if (code.contains('organic')) {
			props.color = '51,51,51'
			props.image = getClass().getResource('organic.png').toString()
		} else if (code.contains('volcanic') || code.contains('tephra')) {
			props.color = '255,102,0'
			props.image = getClass().getResource('volcanic.png').toString()
		} else {
			LOGGER.debug('No resources for {}', code)
		}
		new SchemeEntry(code, code, props)
	}
	
	@Override
	protected List<String> load() {
		// parse the sections
		def parser = new XmlSlurper()
		def expedition = parser.parse(path.openStream())
		
		expedition.SITE.HOLE.CORE.each { core -> 
			core.SECTION.each { section ->
				sections[section.@name.text()] = section
				cores[section] = core
			}
		}
		name = [expedition.@id.text(), expedition.SITE.@id.text(), expedition.SITE.HOLE.@id.text()].join("_")
		
		// look for any scenes
		String uri = path.toURI().toString()
		uri = uri[0..uri.lastIndexOf('/')]
		File dir = new File(new URI(uri))
		if (dir.exists() && dir.isDirectory()) {
			dir.eachFileMatch(~/.*\.diagram/) { file ->
				this.@scenes.add(file.toURI().toURL())
			}
		}
		
		// add in our template diagram if no default
		if (this.@scenes.size() == 0) {
			def template = Platform.getService(ResourceLoader.class).getResource("rsrc:/org/andrill/coretools/dis/template.diagram")
			if (template) {
				this.@scenes.add(template)
			}
		}
		
		// build our schemes
		SchemeManager schemes = Platform.getService(SchemeManager.class)
		def units = new DefaultScheme('dis-units', 'DIS Unit Types', 'unitType')
		def lithologies = new DefaultScheme('dis-lithologies', 'DIS Lithologies', 'lithology')
		def grainsizes = new DefaultScheme('dis-grainsize', 'DIS Grain Sizes', 'grainSize')
		def structures = new DefaultScheme('dis-structures', 'DIS Structures', 'structures')
		def textures = new DefaultScheme('dis-textures', 'DIS Textures', 'textures')
		def components = new DefaultScheme('dis-components', 'DIS Components', 'components')
		
		expedition.SITE.HOLE.CORE.SECTION.SECTIONUNIT.each { unit ->
			unit.attributes().each { key, value ->
				switch(key) {
					case 'UnitType':		units.addEntry(build(value, [group: unit?.@UnitClass?.text()])); break
					case 'MajorLithology':	lithologies.addEntry(new SchemeEntry(value, value, [:])); break
					case ~/GRAINSIZE.*/:	grainsizes.addEntry(new SchemeEntry(value, value, [:])); break
					case ~/SED_STRUCTURE.*/:structures.addEntry(new SchemeEntry(value, value, [:])); break
					case ~/TEXTURES.*/:		textures.addEntry(new SchemeEntry(value, value, [:])); break
					case ~/COMPONENT.*/:	components.addEntry(new SchemeEntry(value, value, [:])); break
				}
			}
		}
		
		schemes.registerScheme(units);
		schemes.registerScheme(lithologies);
		schemes.registerScheme(grainsizes);
		schemes.registerScheme(structures);
		schemes.registerScheme(textures);
		schemes.registerScheme(components);
		
		// set default configuration
		configuration['units'] = 'cm'
		configuration['format'] = '0.00'
		
		return sections.keySet() as List<String>;
	}
	
	@Override
	protected void save(ModelContainer container) {
		SchemeManager schemes = Platform.getService(SchemeManager.class)
		
		// find our section
		String name = getContainerName(container)
		def section = sections[name]
		def top = container.find { it.modelType == 'Section' }.top
		
		// build our markup
		try {
			def metadata = getMetadata(section)
			def output = new StreamingMarkupBuilder().bind {
				mkp.xmlDeclaration()
				EXPEDITION(metadata['EXPEDITION']) {
					SITE(metadata['SITE']) {
						HOLE(metadata['HOLE']) {
							CORE(metadata['CORE']) {
								SECTION(metadata['SECTION']) {
									mkp.yield section.IMAGE
									
									// output all added and modified section units
									container.models.findAll { it.modelType == 'SectionUnit' }.each { interval ->
										def state = status[interval] ? status[interval].status : 'A'
										def entry = schemes.getEntry(interval?.unitType?.scheme, interval?.unitType?.code)
										def props = ['status': state, Description: 'none', UnitClass: 'UND', UnitType: 'air', MajorLithology: 'air']
										interval.properties.each { k,v ->
											if (v) {
												switch (k) {
													case 'id':			props.id = v; break
													case 'name':		props.name = v; break
													case 'top':			props.top_interval = (interval.top - top).cm.value.toPlainString(); break
													case 'base':		props.bottom_interval = (interval.base - top).cm.value.toPlainString(); break
													case 'unitType':	
														props.UnitClass = entry ? entry.getProperty('group', 'SED') : 'UND'
														props.UnitType = v.code
														break
													case 'lithology':	props.MajorLithology = v.code; break
													case 'ratio':		props.ratio = v; break
													case 'description':	props.Description = v; break
													case ~/grainSize./: props."GRAINSIZE_${k[-1]}" = v.code; break
													case ~/structure./: props."SED_STRUCTURE_${k[-1]}" = v.code; break
													case ~/texture./: 	props."TEXTURES_${k[-1]}" = v.code; break
													case ~/component./:	props."COMPONENT_${k[-1]}" = v.code; break
													case ~/color.*/:
														String i = (k[-1] == '1') ? '' : "_${k[-1]}" 
														props["HUE$i"] = v.hue
														props["VALUE$i"] = v.value
														props["CHROMA$i"] = v.chroma
														break
													case ['modelType', 'modelData', 'container', 'class', 'metaClass', 'indexMin', 'indexMax', 'constraints']: break
													default:
														LOGGER.info("Unhandled PSICAT property {}={}", k, v)
												}
											}
										}
										SECTIONUNIT(props)
									}
									
									// output all deleted section units
									status.values().findAll { it.section == metadata['SECTION'].name && it.status == 'R' }.each { deleted ->
										SECTIONUNIT(id: deleted.id, status: 'R')
									}
								}
							}
						}
					}
				}
			}
			// write out the section file
			asFile(section).withWriter("UTF-8") { it << output }
		} catch (e) {
			LOGGER.error("Unable to save $name to ${asFile(section).absolutePath}", e)
			throw new RuntimeException("Unable to save $name", e)
		}
	}
	
	private def getMetadata(section) {
		def metadata = [:]
		metadata[section.name()] = section.attributes()
		def core = cores[section]
		metadata[core.name()] == core.attributes()
		def parent = core
		while(!metadata[parent.name()]) {
			metadata[parent.name()] = parent[0].attributes()
			parent = parent.parent()
		}
		return metadata
	}
	
	private File asFile(section) {
		def metadata = getMetadata(section)
		def filename = [metadata.EXPEDITION.id, metadata.SITE.id, metadata.HOLE.id, metadata.SECTION.name].join('_')
		new File(new URL(path, URLEncoder.encode(filename + '_psicat.xml')).toURI())
	}
	
	void modelAdded(Model model) {
		def key = status[model]
		if (key && key.status == 'R') { 
			key.status = 'M' 
		}
	}
	
	void modelUpdated(Model model) {
		def key = status[model]
		if (key && key.status != 'A') { 
			key.status = 'M' 
		}
	}
	
	void modelRemoved(Model model) {
		def key = status[model]
		if (key) { 
			key.status = 'R' 
		}
	}
	
	static void main(args) {
		Platform.start()
		DISProject project = new DISProject(new File("/Users/jareed/Desktop/DIS/5011_1_A_dis.xml"))
		def container = project.openContainer('10_3')
		container.models.each { println it }
	}
}

class Status {
	String section
	String id
	String status
}