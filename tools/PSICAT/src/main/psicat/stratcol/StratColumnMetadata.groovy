/*
 * Copyright (c) Brian Grivna, 2016.
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

package psicat.stratcol

interface StratColumnMetadata {
	public int getType() // StratColumnMetadataTypes type
	public String getTypeName() // name of metadata type

	// map metadata sections to those in project, return map with elements of
	// form metadata section:project section
	public mapSections(project)

	// Return list of ModelContainers with models' depths adjusted to reflect top MCD depth of interval.
	public getContainers(project, includeModels)

	public getTop() // get topmost depth in metadata
	public getBase() // get bottommost depth in metadata

	public setLogger(logger)
}
