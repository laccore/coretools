package psicat.dialogs

import groovy.beans.Bindable

class CreateParallelIntervalsModel {
    static List<Class> lastSelectedModels = null
    static boolean showUnmatchedBottomsWarning = true

    def diagram = null // DiagramModel
    List<Class> modelClasses = null
}