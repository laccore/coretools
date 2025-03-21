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
application {
	title="PSICAT"
	startupGroups=["PSICAT"]
	autoShutdown=false
}
mvcGroups {
	PSICAT {
		model="psicat.PSICATModel"
		controller="psicat.PSICATController"
		view="psicat.PSICATView"
	}
	SectionCombo {
		model="psicat.components.SectionComboModel"
		controller="psicat.components.SectionComboController"
		view="psicat.components.SectionComboView"
	}
	Project {
		model="psicat.components.ProjectModel"
		controller="psicat.components.ProjectController"
		view="psicat.components.ProjectView"
	}
	Diagram {
		model="psicat.components.DiagramModel"
		controller="psicat.components.DiagramController"
		view="psicat.components.DiagramView"
	}
	NewSectionWizard {
		model="psicat.dialogs.NewSectionWizardModel"
		controller="psicat.dialogs.NewSectionWizardController"
		view="psicat.dialogs.NewSectionWizardView"
	}
	NewProjectWizard {
		model="psicat.dialogs.NewProjectWizardModel"
		controller="psicat.dialogs.NewProjectWizardController"
		view="psicat.dialogs.NewProjectWizardView"
	}
	ImageImport {
		model="psicat.dialogs.ImageImportModel"
		controller="psicat.dialogs.ImageImportController"
		view="psicat.dialogs.ImageImportView"
	}
	ExportDiagramWizard {
		model="psicat.dialogs.ExportDiagramWizardModel"
		controller="psicat.dialogs.ExportDiagramWizardController"
		view="psicat.dialogs.ExportDiagramWizardView"
	}
	ExportTabularWizard {
		model="psicat.dialogs.ExportTabularWizardModel"
		controller="psicat.dialogs.ExportTabularWizardController"
		view="psicat.dialogs.ExportTabularWizardView"
	}
	ExportStrat {
		model="psicat.dialogs.ExportStratModel"
		controller="psicat.dialogs.ExportStratController"
		view="psicat.dialogs.ExportStratView"		
	}
	ImportImageWizard {
		model="psicat.dialogs.ImportImageWizardModel"
		controller="psicat.dialogs.ImportImageWizardController"
		view="psicat.dialogs.ImportImageWizardView"
	}
	ImportTabularWizard {
		model="psicat.dialogs.ImportTabularWizardModel"
		view="psicat.dialogs.ImportTabularWizardView"
		controller="psicat.dialogs.ImportTabularWizardController"
	}
	ImportLegacyWizard {
		model="psicat.dialogs.ImportLegacyWizardModel"
		controller="psicat.dialogs.ImportLegacyWizardController"
		view="psicat.dialogs.ImportLegacyWizardView"
	}
	ChooseSchemesDialog {
		model="psicat.dialogs.ChooseSchemesDialogModel"
		controller="psicat.dialogs.ChooseSchemesDialogController"
		view="psicat.dialogs.ChooseSchemesDialogView"
	}
	ChooseSectionMetadata {
		model="psicat.dialogs.ChooseSectionMetadataModel"
		controller="psicat.dialogs.ChooseSectionMetadataController"
		view="psicat.dialogs.ChooseSectionMetadataView"
	}
	FindReplace {
		model="psicat.dialogs.FindReplaceModel"
		controller="psicat.dialogs.FindReplaceController"
		view="psicat.dialogs.FindReplaceView"
	}
	AuditProject {
		model="psicat.dialogs.AuditProjectModel"
		controller="psicat.dialogs.AuditProjectController"
		view="psicat.dialogs.AuditProjectView"
	}
	OpenStratColumnDepths {
		model="psicat.dialogs.OpenStratColumnDepthsModel"
		controller="psicat.dialogs.OpenStratColumnDepthsController"
		view="psicat.dialogs.OpenStratColumnDepthsView"
	}
	TrackOptions {
		model="psicat.dialogs.TrackOptionsModel"
		controller="psicat.dialogs.TrackOptionsController"
		view="psicat.dialogs.TrackOptionsView"
	}
	DiagramOptions {
		model="psicat.dialogs.DiagramOptionsModel"
		controller="psicat.dialogs.DiagramOptionsController"
		view="psicat.dialogs.DiagramOptionsView"
	}
    CreateStratColumn {
        model = 'psicat.dialogs.CreateStratColumnModel'
        controller = 'psicat.dialogs.CreateStratColumnController'
        view = 'psicat.dialogs.CreateStratColumnView'
	}
    CreateParallelIntervals {
        model = 'psicat.dialogs.CreateParallelIntervalsModel'
        controller = 'psicat.dialogs.CreateParallelIntervalsController'
        view = 'psicat.dialogs.CreateParallelIntervalsView'
    }	
}
