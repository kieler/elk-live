package de.cau.cs.kieler.elkgraph.web.xtext

import org.eclipse.emf.common.util.URI
import org.eclipse.xtend.lib.annotations.Accessors
import org.eclipse.xtend.lib.annotations.FinalFieldsConstructor
import org.eclipse.xtext.ide.server.IWorkspaceConfigFactory
import org.eclipse.xtext.workspace.IProjectConfig
import org.eclipse.xtext.workspace.ISourceFolder
import org.eclipse.xtext.workspace.IWorkspaceConfig

import static extension org.eclipse.xtext.util.UriUtil.*

class InMemoryWorkspaceConfigFactory implements IWorkspaceConfigFactory {
	
	override getWorkspaceConfig(URI workspaceBaseURI) {
		val projectConfig = new InMemoryProjectConfig
		return projectConfig.workspaceConfig
	}
	
}

class InMemoryProjectConfig implements IProjectConfig {
	
	@Accessors
	val URI path
	
	new() {
		this(URI.createURI('inmemory:/'))
	}
	
	new(URI path) {
		this.path = path
	}
	
	override getName() {
		'inmemory'
	}
	
	override findSourceFolderContaining(URI member) {
		new InMemorySourceFolder(member.trimFragment.trimSegments(1))
	}
	
	override getSourceFolders() {
		emptySet
	}
	
	override getWorkspaceConfig() {
		new InMemoryWorkspaceConfig(this)
	}
	
}

@FinalFieldsConstructor
class InMemorySourceFolder implements ISourceFolder {
	
	@Accessors
	val URI path
	
	override getName() {
		'inmemory'
	}
	
}

@FinalFieldsConstructor
class InMemoryWorkspaceConfig implements IWorkspaceConfig {
	
	val IProjectConfig projectConfig
	
	override findProjectByName(String name) {
		if (projectConfig.name == name)
			return projectConfig
	}
	
	override findProjectContaining(URI member) {
		if (projectConfig.path.isPrefixOf(member))
			return projectConfig
		else
			new InMemoryProjectConfig(member.trimFragment.trimSegments(1))
	}
	
	override getProjects() {
		#{projectConfig}
	}
	
}