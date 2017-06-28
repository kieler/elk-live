package de.cau.cs.kieler.elkgraph.web.xtext

import org.eclipse.emf.common.util.URI
import org.eclipse.xtend.lib.annotations.FinalFieldsConstructor
import org.eclipse.xtext.ide.server.IWorkspaceConfigFactory
import org.eclipse.xtext.workspace.IProjectConfig
import org.eclipse.xtext.workspace.ISourceFolder
import org.eclipse.xtext.workspace.IWorkspaceConfig

class InMemoryWorkspaceConfigFactory implements IWorkspaceConfigFactory {
	
	override getWorkspaceConfig(URI workspaceBaseURI) {
		val projectConfig = new InMemoryProjectConfig
		return projectConfig.workspaceConfig
	}
	
}

class InMemoryProjectConfig implements IProjectConfig {
	
	val sourceFolder = new InMemorySourceFolder
	
	override findSourceFolderContaining(URI arg0) {
		sourceFolder
	}
	
	override getName() {
		sourceFolder.name
	}
	
	override getPath() {
		sourceFolder.path
	}
	
	override getSourceFolders() {
		#{sourceFolder}
	}
	
	override getWorkspaceConfig() {
		new InMemoryWorkspaceConfig(this)
	}
	
}

class InMemorySourceFolder implements ISourceFolder {
	
	public static val BASE_URI = 'inmemory:/'
	
	override getName() {
		'inmemory'
	}
	
	override getPath() {
		URI.createURI(BASE_URI)
	}
	
}

@FinalFieldsConstructor
class InMemoryWorkspaceConfig implements IWorkspaceConfig {
	
	val InMemoryProjectConfig projectConfig
	
	override findProjectByName(String name) {
		projectConfig
	}
	
	override findProjectContaining(URI member) {
		projectConfig
	}
	
	override getProjects() {
		#{projectConfig}
	}
	
}