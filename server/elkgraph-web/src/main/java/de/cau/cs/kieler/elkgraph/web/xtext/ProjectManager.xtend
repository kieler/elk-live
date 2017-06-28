package de.cau.cs.kieler.elkgraph.web.xtext

import org.eclipse.xtext.build.IncrementalBuilder
import org.eclipse.xtext.build.IndexState
import org.eclipse.xtext.util.CancelIndicator

class ProjectManager extends org.eclipse.xtext.ide.server.ProjectManager {
	
	override doInitialBuild(CancelIndicator cancelIndicator) {
		new IncrementalBuilder.Result(new IndexState, newArrayList)
	}
	
}