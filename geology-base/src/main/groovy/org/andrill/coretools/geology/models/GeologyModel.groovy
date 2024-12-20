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
package org.andrill.coretools.geology.models

import java.util.Locale
import java.text.NumberFormat

import org.andrill.coretools.AdapterManager
import org.andrill.coretools.Platform
import org.andrill.coretools.geology.GProperty
import org.andrill.coretools.model.Model
import org.andrill.coretools.model.ModelContainer
import org.andrill.coretools.model.edit.EditableProperty
import org.andrill.coretools.geology.models.util.GeologyFormatter

/**
 * An implementation of the Model interface for geology-related models.
 * 
 * @author Josh Reed (jareed@andrill.org)
 */
abstract class GeologyModel implements Model {
	static {
		ExpandoMetaClass.enableGlobally()
		Number.metaClass.getProperty { String unit -> new Length(delegate, unit) }
	}
	protected static final List SKIP = ['class', 'metaClass', 'modelType', 'modelData', 'container', 'constraints', 'indexMin', 'indexMax']
	protected List propertyList
	ModelContainer container
	
	public GeologyModel() {}
	public GeologyModel(Map map) {
		map.each { k,v ->
		 	if (properties.containsKey(k)) {
		 		obj."$k" = v
		 	}
		}
	}

	// confirm obj.top <= obj.base
	static boolean validInterval(propname, value, obj) {
		def valid = true
		def bdval
		try {
			bdval = new BigDecimal(value)
		} catch (e) {
			return false
		}
		if (propname.equals("top")) {
			final baseval = obj.base.value
			valid = bdval <= baseval
		} else if (propname.equals("base")) {
			final topval = obj.top.value
			valid = bdval >= topval
		}
		// 7/23/2024: All interval models should use the propnames 'top' and 'base'.
		// If not, return true. Otherwise focus will be forever trapped in the input Widget.

		// if (!valid) { println "invalid interval" }
		return valid
	}

	Object getAdapter(Class adapter) {
		if (adapter == EditableProperty[].class) {
			if (propertyList == null && this?.@constraints) {
				propertyList = []
				this?.@constraints.each { k, v ->
					propertyList << buildProperty(k, v)
				}
			}
			return (propertyList as EditableProperty[])
		}
		return Platform.getService(AdapterManager.class).getAdapter(this, adapter);
	}
	
	protected def buildProperty(name, args) {
		def map = [:]
		map.source = this
		map.name = name
		map.widgetType = args.widgetType ?: lookupWidgetType(name)
		map.widgetProperties = args.widgetProperties ?: [:]
		map.validators = buildValidator(name, args)
		map.constraints = [:]
		args.each { k,v -> map.constraints[k] = v?.toString() }
		return new GProperty(map)
	}
	
	protected def lookupWidgetType(name) {
		if (this.metaClass.getMetaProperty(name).type == SchemeRef.class) return "SchemeEntry"
		else return "TextField"
	}
	
	protected def buildValidator(name, args) {
		def list = []
		if (!args?.nullable)	list << { value, obj -> value != null }
		if (!args?.blank)		list << { value, obj -> value == null || value.trim() != "" }
		if (this.metaClass.getMetaProperty(name).type == Number.class)		list << { value, obj -> value == null || new BigDecimal(value) }
		if (this.metaClass.getMetaProperty(name).type == Length.class) {
			list << { value, obj -> value == null || Length.validLength(value) }
			if (this.constraints[name].containsKey('widgetType') && this.constraints[name].get('widgetType').equals("LengthWidget")) {
				list << { value, obj -> validInterval(name, value, obj) }
			}
		}
		if (this.metaClass.getMetaProperty(name).type == SchemeRef.class)	list << { value, obj -> value == null || value?.indexOf(':') > -1 }
		return list
	}
	
	/**
	 * Gets the model data.
	 */
	Map<String,String> getModelData() {
		def data = [:]
		metaClass.properties.each { p ->
			if (!SKIP.contains(p.name)) {
				def v = p.getProperty(this)
				if (v != null) {
					data[p.name] = handle(v)
				}
			}
		}
		return data
	}
	
	/**
	 * Gets the model type.
	 */
	String getModelType() { 
		return getClass().simpleName 
	}
	
	/**
	 * Notify our container that this model has been updated.
	 */
	void updated() { 
		if (container != null) {
			container.update(this)
		}
	}
	 
	def handle(v) {
		if (v instanceof List) {
			return v.collect { handle(it) }.join("|")
		} else if (v instanceof Length) {
			String units = container?.project?.configuration?.units ?: 'm'
			NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH)
			format.applyPattern('0.00#####')
			return "${format.format(v.to(units).value)} ${units}"
		} else {
			return v?.toString()
		}
	}
	
	/**
	 * Some helpers.
	 */
	void setProperty(String name, value) {
		def metaProperty = metaClass.getMetaProperty(name)
		if (metaProperty) {
			if (value != null && metaProperty.type == Length.class && !(value instanceof Length)) {
				def v = value.toString()
				metaProperty.setProperty(this, new Length(v))
	        } else if (value != null && metaProperty.type == SchemeRef.class && !(value instanceof SchemeRef)) {
	 			String[] split = value.toString().split(':')
	 			if (split.length > 1) {	
	 			    metaProperty.setProperty(this, new SchemeRef(scheme:split[0].trim(), code:split[1].trim()))
	 			}
	        } else if (value != null && metaProperty.type == URL.class && !(value instanceof URL)) {
	        	metaProperty.setProperty(this, new URL(value))
	 		} else if (value != null && metaProperty.type == BigDecimal.class && !(value instanceof BigDecimal)) {
			 	metaProperty.setProperty(this, new BigDecimal(value))
	 		} else {
	 			metaProperty.setProperty(this, value)
	 		}
		}
	}

	public double getIndexMin() { (double) Math.min(top?.to("m")?.value ?: 0, base?.to("m")?.value ?: 0) }
	public double getIndexMax() { (double) Math.max(top?.to("m")?.value ?: 0, base?.to("m")?.value ?: 0) }
	
	protected format(value) {
		return GeologyFormatter.format(value)
	}

	// https://stackoverflow.com/questions/2559759/how-do-i-convert-camelcase-into-human-readable-names-in-java
	private String splitCamelCase(String s) {
		return s.replaceAll(
			String.format("%s|%s|%s",
				"(?<=[A-Z])(?=[A-Z][a-z])",
				"(?<=[^A-Z])(?=[A-Z])",
				"(?<=[A-Za-z])(?=[^A-Za-z])"
			),
			" "
		);
	}

	String toString() {
		final spacedClassName = splitCamelCase(this.class.simpleName)
		if (top == base) {
			"$spacedClassName ${format(top)}"	
		} else {
			"$spacedClassName ${format(top.value)}-${format(base)}"
		}
	}

	String toStringInUnits(units) {
		final spacedClassName = splitCamelCase(this.class.simpleName)
		if (top == base) {
			"$spacedClassName ${format(top.to(units))}"	
		} else {
			"$spacedClassName ${format(top.to(units).value)}-${format(base.to(units))}"
		}
	}
}