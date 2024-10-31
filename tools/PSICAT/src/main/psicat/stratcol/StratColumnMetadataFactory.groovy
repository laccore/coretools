/*
 * Copyright (c) CSD Facility, 2016.
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

import psicat.stratcol.SectionMetadata
import psicat.stratcol.SpliceIntervalMetadata
import psicat.stratcol.StratColumnMetadataTypes as types
import psicat.stratcol.StratColumnMetadataUtils as utils

class StratColumnMetadataFactory {
	static create(metadataPath) throws Exception {
		if (utils.isValidMetadataFile(metadataPath)) {
			final mdType = utils.identifyMetadataFile(metadataPath)
			if (mdType == types.SectionMetadataFile) {
				return new SectionMetadata(metadataPath)
			} else if (mdType == types.SpliceIntervalFile) {
				return new SpliceIntervalMetadata(metadataPath)
			}
		}
		return null
	}
}