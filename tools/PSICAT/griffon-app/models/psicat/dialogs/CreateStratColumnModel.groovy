package psicat.dialogs

import groovy.beans.Bindable

class CreateStratColumnModel {
   static final String NULL_LOG_FILE = '[no log file selected]'

   def project = null
   def stratMetadata = null
   HashMap<String,String> modelsAndHelpText = null
   @Bindable String stratColumnName = ''
   @Bindable String logFilePath = NULL_LOG_FILE
}