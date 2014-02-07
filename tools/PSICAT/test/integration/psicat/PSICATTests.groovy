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
package psicat

import griffon.util.IGriffonApplication 

import groovy.util.GroovyTestCase

class PSICATTests extends GroovyTestCase {
	IGriffonApplication app
	def model
	def view
	def controller
	
	void setUp() {
		model = app.models['PSICAT']
		view = app.views['PSICAT']
		controller = app.controllers['PSICAT']
	}
	
	void testStartup() {
		assert model
		assert view
		assert controller
	}
}