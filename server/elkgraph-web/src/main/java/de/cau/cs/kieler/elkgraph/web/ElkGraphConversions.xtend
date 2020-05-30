/*******************************************************************************
 * Copyright (c) 2020 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package de.cau.cs.kieler.elkgraph.web

import com.google.common.io.CharStreams
import java.io.StringWriter
import java.util.List
import java.util.Optional
import java.util.logging.Level
import java.util.logging.Logger
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.eclipse.elk.graph.ElkNode
import org.eclipse.elk.graph.json.ElkGraphJson
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.URIConverter
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.xtend.lib.annotations.Accessors
import org.eclipse.xtend.lib.annotations.FinalFieldsConstructor
import org.eclipse.xtext.diagnostics.Diagnostic

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import static javax.servlet.http.HttpServletResponse.SC_NOT_ACCEPTABLE
import static javax.servlet.http.HttpServletResponse.SC_OK

class ElkGraphConversions {

    static val LOG = Logger.getLogger(ElkGraphConversions.name)

    @FinalFieldsConstructor
    @Accessors
    static class Result {
        public val int statusCode
        public val String contentType
        public val String content
    }

    static class Error extends Result {
        private new (int statusCode, String content) {
            super(statusCode, "error".contentType, content)
        }
        new(int statusCode, String type, String message) {
            this(statusCode, message.toErrorJson(type))
        }
        new(int statusCode, String type, Exception e) {
            this(statusCode, type, e.message, e)
        }
        new(int statusCode, String type, String message, Exception e) {
            this(statusCode, [|
                if (e instanceof ImportExportException) {
                    message.toErrorJson(type, e.diagnostics.map[it.toJson], null)
                } else {
                    message.toErrorJson(type, null, e.nestedExceptionMessages)
                }
            ].apply())
        }

    }

    @FinalFieldsConstructor
    static class ImportExportException extends Exception {
        val List<Resource.Diagnostic> diagnostics
    }

    public static val KNWON_FORMATS = #{"elkt", "elkg", "json"}

    static val FORMAT_TO_CONTENT_TYPE = #{
        "json" -> "application/json",
        "elkg" -> "application/xml",
        "elkt" -> "text/plain",
        "error" -> "application/json"
    }

    static val FAILURE_REQUEST = "request"
    static val FAILURE_INPUT = "input"
    static val FAILURE_OUTPUT = "output"

    /* - - - - - - - - - - - - - -
     *   HTTP request handling
     * - - - - - - - - - - - - - - */

    static def void handleRequest(HttpServletRequest req, HttpServletResponse resp) {
        val errorOptional = req.checkRequest()
        if (errorOptional.present) {
            resp.sendResult(errorOptional.get)
            return
        }

        val inFormat = req.getParameter("inFormat")
        val outFormat = req.getParameter("outFormat")
        val graph = CharStreams.toString(req.reader)
        val result = ElkGraphConversions.convert(inFormat, outFormat, graph)
        resp.sendResult(result)
    }

    private static def sendResult(HttpServletResponse resp, Result result) {
        resp.status = result.statusCode
        resp.setHeader("Content-Type", result.contentType)
        resp.writer.append(result.content)
    }

    private static def Optional<Error> checkRequest(HttpServletRequest req) {
        val reqContentType = req.getHeader("Content-Type")
        if (reqContentType === null || !FORMAT_TO_CONTENT_TYPE.values.contains(reqContentType)) {
            return Optional.of(
                new Error(SC_NOT_ACCEPTABLE, FAILURE_REQUEST, "Unsupported content type: '" + reqContentType + "'."))
        }

        val inFormat = req.getParameter("inFormat")
        if (inFormat === null) {
            return Optional.of(new Error(SC_BAD_REQUEST, FAILURE_REQUEST, "Missing specification of 'inFormat'."))
        }

        val outFormat = req.getParameter("outFormat")
        if (outFormat === null) {
            return Optional.of(new Error(SC_BAD_REQUEST, FAILURE_REQUEST, "Missing specification of 'outFormat'."))
        }

        for (String format : #[inFormat, outFormat]) {
            if (!KNWON_FORMATS.contains(format)) {
                return Optional.of(new Error(SC_BAD_REQUEST, FAILURE_REQUEST, "Unknown graph format '" + format + "'."))
            }
        }
        // Success
        return Optional.empty
    }

    private static def String toErrorJson(String message, String errorType) {
        return message.toErrorJson(errorType, null, null)
    }

    private static def String toErrorJson(String message, String errorType, List<String> diagnosticJsons, List<String> causes) {
        '''
        {
            "message": "«message»"
            ,"type": "«errorType»"
            «IF !diagnosticJsons.nullOrEmpty»
            ,"diagnostics": [
                «diagnosticJsons.join(",\n")»
            ]
            «ENDIF»
            «IF !causes.nullOrEmpty»
            ,"causes": [
            	«causes.map['"' + it.escape + '"'].join(",\n")»
            ]
            «ENDIF»
        }
        '''.toString
    }

    private static def toJson(Resource.Diagnostic diagnostic) {
        return
        '''
        {
            "message": "«diagnostic.message.escape»"
            ,"startLineNumber": «diagnostic.line»
            ,"startColumn": «diagnostic.column»
            «IF (diagnostic instanceof Diagnostic)»
            ,"endLineNumber": «diagnostic.lineEnd»
            ,"endColumn": «diagnostic.columnEnd»
            «ENDIF»
        }
        '''
    }

    /* - - - - - - - - - - - - - -
     *   Actual conversion logic
     * - - - - - - - - - - - - - - */

    static def Result convert(String inFormat, String outFormat, String graph) {
        if (inFormat == outFormat) {
            return new Result(SC_OK, outFormat.contentType, graph)
        }

        val ElkNode elkNode = try {
                switch (inFormat) {
                    case "json": graph.loadJson
                    case "elkt",
                    case "elkg": graph.loadElkGraph(inFormat)
                }
            } catch (Exception e) {
                LOG.log(Level.INFO, "Failed to load input graph.", e)
                return new Error(SC_BAD_REQUEST, FAILURE_INPUT, "Failed to load input graph.", e)
            }

        val String serializedResult = try {
                switch (outFormat) {
                    case "json": elkNode.toJson
                    case "elkt",
                    case "elkg": elkNode.toElkGraph(outFormat)
                }
            } catch (Exception e) {
                LOG.log(Level.INFO, "Failed to serialize converted graph.", e)
                return new Error(SC_BAD_REQUEST, FAILURE_OUTPUT, "Failed to serialize converted graph.", e)
            }
        return new Result(SC_OK, outFormat.contentType, serializedResult)

    }

    private static def loadElkGraph(String graph, String format) {
        val r = createResource("dummy." + format)
        r.load(new URIConverter.ReadableInputStream(graph, "UTF-8"), null)
        val elkGraph = r.contents.head as ElkNode
        if (!r.errors.empty) {
            throw new ImportExportException(r.errors)
        }
        return elkGraph
    }

    private static def loadJson(String graph) {
        return ElkGraphJson.forGraph(graph).toElk
    }

    private static def toJson(ElkNode graph) {
        return ElkGraphJson.forGraph(graph)
                .prettyPrint(true)
                .omitUnknownLayoutOptions(false)
                .toJson
    }

    private static def toElkGraph(ElkNode graph, String format) {
        val r = createResource("dummy." + format)
        r.contents += graph
        val sw = new StringWriter
        val os = new URIConverter.WriteableOutputStream(sw, "UTF-8")
        r.save(os, null);
        return sw.toString
    }

    private static def createResource(String uri) {
        return new ResourceSetImpl().createResource(URI.createURI(uri))
    }

    private static def contentType(String format) {
        return FORMAT_TO_CONTENT_TYPE.get(format)
    }
    
    private static def List<String> nestedExceptionMessages(Throwable t) {
    	var current = newArrayList(t.class.simpleName + ": " + t.message)
    	if (t.cause !== null) {
    		current += t.cause.nestedExceptionMessages
    	} 
    	return current
    }

	// See https://www.json.org/json-en.html
	static val JSON_ESCAPES = #{
		"\"" -> "\\\"",
		"\\" -> "\\\\",
		"/" -> "\\/",
		"\b" -> "\\b",
		"\f" -> "\\f",
		"\n" -> "\\n",
		"\r" -> "\\r",
		"\t" -> "\\t"
		// no unicode handled here
	}
	
	private static def String escape(String str) {
		IterableExtensions.fold(JSON_ESCAPES.entrySet, str, [ s, pattern |
			s.replace(pattern.key, pattern.value)
		])
	}
}
