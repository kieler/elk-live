/*******************************************************************************
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package de.cau.cs.kieler.elkgraph.web.xtext

import org.eclipse.lsp4j.InitializeParams

class LanguageServerImpl extends org.eclipse.xtext.ide.server.LanguageServerImpl {
	
	override initialize(InitializeParams params) {
		if (params.rootUri === null)
			params.rootUri = 'inmemory:/'
		super.initialize(params)
	}
	
}