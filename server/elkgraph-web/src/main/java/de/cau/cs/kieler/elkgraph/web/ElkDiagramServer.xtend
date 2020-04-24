/*******************************************************************************
 * Copyright (c) 2020 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package de.cau.cs.kieler.elkgraph.web

import java.util.function.Consumer
import org.eclipse.sprotty.Action
import org.eclipse.sprotty.IDiagramServer
import org.eclipse.sprotty.SModelRoot
import org.eclipse.sprotty.xtext.LanguageAwareDiagramServer
import org.eclipse.sprotty.xtext.ls.IssueProvider
import org.eclipse.xtend.lib.annotations.Accessors
import org.eclipse.xtend.lib.annotations.EqualsHashCode
import org.eclipse.xtend.lib.annotations.ToString
import org.eclipse.xtext.ide.server.ILanguageServerAccess.Context

class ElkDiagramServer extends LanguageAwareDiagramServer {

    @Accessors
    @EqualsHashCode
    @ToString(skipNulls=true)
    static class ChangeLayoutVersionAction implements Action {
        public static val KIND = 'versionChange'
        String kind = KIND

        String version

        new() { }
        new(Consumer<ChangeLayoutVersionAction> initializer) {
            initializer.accept(this)
        }
    }

    var String currentLayoutVersion = "snapshot"

    override protected handleAction(Action action) {
        if (action.kind == ChangeLayoutVersionAction.KIND) {
            val versionAction = action as ChangeLayoutVersionAction
            currentLayoutVersion = versionAction.version
            diagramLanguageServer.diagramUpdater.updateDiagram(this)
        } else {
            super.handleAction(action)
        }
    }

    override protected createDiagramGeneratorContext(Context context, IDiagramServer server,
        IssueProvider issueProvider) {
        val generatorContext = super.createDiagramGeneratorContext(context, server, issueProvider)
        generatorContext.state.options.put("layoutVersion", currentLayoutVersion)
        return generatorContext
    }
    
    /**
	 * Usually the result of this method is extracted from configuration properties sent with the request.
	 * However, as we (as the server) initiate the model update, we are unaware of what the client desires 
	 * and must set the value ourselves.
	 */
	override protected needsClientLayout(SModelRoot root) {
		return false
	}

	/**
	 * The server-side layout is performed explicitly by the diagram generator, hence 
	 * the "regular" layout mechanism must not be used.
	 * 
	 * @see {@link ElkDiagramServer#needsClientLayout(SModelRoot)}
	 */
	override protected needsServerLayout(SModelRoot root, Action cause) {
		return false
	}
	
}
