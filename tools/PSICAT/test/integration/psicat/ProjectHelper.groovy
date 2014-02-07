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

import org.andrill.coretools.model.DefaultProject
import org.andrill.coretools.model.Project;

class ProjectHelper {
	
	static File createTempDir() {
		File temp = File.createTempFile("PSICAT-tests", "")
		temp.delete()
		temp.mkdir()
		temp.deleteOnExit()
		return temp
	}
	
	static Project createProject(name, origin, containers = []) {
		def project = new DefaultProject(createTempDir())
		project.name = name
		project.origin = origin
		containers.each { project.saveContainer(project.newContainer(it)) }
		project.save()
		return project
	}
}