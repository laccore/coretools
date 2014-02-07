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
package org.andrill.coretools.geology.ui

import java.awt.geom.Rectangle2D

import org.andrill.coretools.data.DataSet
import org.andrill.coretools.graphics.GraphicsContext
import org.andrill.coretools.model.ModelContainer

class DataTrack extends GeologyTrack {
	def datasets = []
	def getHeader() { "Data" }
	def getFooter() { "Data" }
	
	void renderContents(GraphicsContext graphics, Rectangle2D bounds) {
		this.bounds = bounds
		
		// figure out our clip
		def clip = clip(bounds, graphics.clip, 100 / scale)

		// render our series
		def series = getParameter('series', '').split(',')
		series.each { s ->
			def data = datasets.find { it.name == s }
			if (data) {
				renderData(data, graphics, clip)
			}
		}
	}
	
	void renderData(DataSet data, GraphicsContext graphics, Rectangle2D bounds) {
		
	}
	
	void setModels(ModelContainer container) {
		container.findAll { it.modelType == 'CSVDataFile' }.each {
			datasets.addAll(it.datasets)
		}
	}
}