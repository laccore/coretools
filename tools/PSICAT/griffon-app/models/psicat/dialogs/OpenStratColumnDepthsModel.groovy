package psicat.dialogs

import groovy.beans.Bindable

class OpenStratColumnDepthsModel {
	@Bindable String metadataPath	
	@Bindable String statusText = ""
	@Bindable String fileTypeText = ""
	def project
	def stratColumnMetadata = null
	def confirmed = false
}