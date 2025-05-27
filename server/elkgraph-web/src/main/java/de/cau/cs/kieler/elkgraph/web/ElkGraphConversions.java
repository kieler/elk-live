/**
 * Copyright (c) 2025 Kiel University and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package de.cau.cs.kieler.elkgraph.web;

import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.json.ElkGraphJson;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.diagnostics.Diagnostic;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;

import static java.util.Map.entry;

public class ElkGraphConversions {
  public static class Result {
    public final int statusCode;

    public final String contentType;

    public final String content;

    public Result(final int statusCode, final String contentType, final String content) {
      super();
      this.statusCode = statusCode;
      this.contentType = contentType;
      this.content = content;
    }

    public int getStatusCode() {
      return this.statusCode;
    }

    public String getContentType() {
      return this.contentType;
    }

    public String getContent() {
      return this.content;
    }
  }

  public static class Error extends ElkGraphConversions.Result {
    private Error(final int statusCode, final String content) {
      super(statusCode, ElkGraphConversions.contentType("error"), content);
    }

    public Error(final int statusCode, final String type, final String message) {
      this(statusCode, ElkGraphConversions.toErrorJson(message, type));
    }

    public Error(final int statusCode, final String type, final Exception e) {
      this(statusCode, type, e.getMessage(), e);
    }

    public Error(final int statusCode, final String type, final String message, final Exception e) {
      this(statusCode, ((Function0<String>) () -> {
        if (e instanceof ImportExportException) {
          return ElkGraphConversions.toErrorJson(message, type,
                  ((ImportExportException) e).diagnostics.stream().map(
                          (Resource.Diagnostic it) -> {
                            return ElkGraphConversions.toJson(it);
                          }).toList(), null);
        } else {
          return ElkGraphConversions.toErrorJson(message, type, null, ElkGraphConversions.nestedExceptionMessages(e));
        }
      }).apply());
    }
  }

  public static class ImportExportException extends Exception {
    private final List<Resource.Diagnostic> diagnostics;

    public ImportExportException(final List<Resource.Diagnostic> diagnostics) {
      super();
      this.diagnostics = diagnostics;
    }
  }

  private static final Logger LOG = Logger.getLogger(ElkGraphConversions.class.getName());

  public static final Set<String> KNWON_FORMATS = new HashSet<>(Arrays.asList("elkt", "elkg", "json"));

  private static final Map<String, String> FORMAT_TO_CONTENT_TYPE = Map.of(
          "json", "application/json",
          "elkg", "application/xml",
          "elkt", "text/plain",
          "error", "application/json");

  private static final String FAILURE_REQUEST = "request";

  private static final String FAILURE_INPUT = "input";

  private static final String FAILURE_OUTPUT = "output";

  /**
   * - - - - - - - - - - - - - -
   *   HTTP request handling
   * - - - - - - - - - - - - - -
   */
  public static void handleRequest(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
    final Optional<ElkGraphConversions.Error> errorOptional = ElkGraphConversions.checkRequest(req);
    if (errorOptional.isPresent()) {
      ElkGraphConversions.sendResult(resp, errorOptional.get());
      return;
    }
    final String inFormat = req.getParameter("inFormat");
    final String outFormat = req.getParameter("outFormat");
    final String graph = CharStreams.toString(req.getReader());
    final ElkGraphConversions.Result result = ElkGraphConversions.convert(inFormat, outFormat, graph);
    ElkGraphConversions.sendResult(resp, result);
  }

  private static PrintWriter sendResult(final HttpServletResponse resp, final ElkGraphConversions.Result result) throws IOException {
      resp.setStatus(result.statusCode);
      resp.setHeader("Content-Type", result.contentType);
      return resp.getWriter().append(result.content);
  }

  private static Optional<ElkGraphConversions.Error> checkRequest(final HttpServletRequest req) {
    final String reqContentType = req.getHeader("Content-Type");
    if (reqContentType == null || !ElkGraphConversions.FORMAT_TO_CONTENT_TYPE.values().contains(reqContentType)) {
      return Optional.of(new ElkGraphConversions.Error(
              HttpServletResponse.SC_NOT_ACCEPTABLE,
              ElkGraphConversions.FAILURE_REQUEST,
              "Unsupported content type: \'" + reqContentType + "\'."));
    }

    final String inFormat = req.getParameter("inFormat");
    if (inFormat == null) {
      return Optional.of(new ElkGraphConversions.Error(
              HttpServletResponse.SC_BAD_REQUEST,
              ElkGraphConversions.FAILURE_REQUEST,
              "Missing specification of \'inFormat\'."));
    }

    final String outFormat = req.getParameter("outFormat");
    if ((outFormat == null)) {
      return Optional.of(new ElkGraphConversions.Error(
              HttpServletResponse.SC_BAD_REQUEST,
              ElkGraphConversions.FAILURE_REQUEST,
              "Missing specification of \'outFormat\'."));
    }

    for (final String format : new String[] { inFormat, outFormat }) {
      if (!ElkGraphConversions.KNWON_FORMATS.contains(format)) {
        return Optional.of(new ElkGraphConversions.Error(
                HttpServletResponse.SC_BAD_REQUEST,
                ElkGraphConversions.FAILURE_REQUEST,
                "Unknown graph format \'" + format + "\'."));
      }
    }
    // Success
    return Optional.empty();
  }

  private static String toErrorJson(final String message, final String errorType) {
    return ElkGraphConversions.toErrorJson(message, errorType, null, null);
  }

  private static String toErrorJson(final String message, final String errorType, final List<String> diagnosticJsons, final List<String> causes) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("{");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("\"message\": \"");
    _builder.append(message, "    ");
    _builder.append("\"");
    _builder.newLineIfNotEmpty();
    _builder.append("    ");
    _builder.append(",\"type\": \"");
    _builder.append(errorType, "    ");
    _builder.append("\"");
    _builder.newLineIfNotEmpty();
    {
      if (diagnosticJsons != null && !diagnosticJsons.isEmpty()) {
        _builder.append("    ");
        _builder.append(",\"diagnostics\": [");
        _builder.newLine();
        _builder.append("    ");
        _builder.append("    ");
        String _join = IterableExtensions.join(diagnosticJsons, ",\n");
        _builder.append(_join, "        ");
        _builder.newLineIfNotEmpty();
        _builder.append("    ");
        _builder.append("]");
        _builder.newLine();
      }
    }
    if (causes != null && !causes.isEmpty()) {
      _builder.append("    ");
      _builder.append(",\"causes\": [");
      _builder.newLine();
      _builder.append("    ");
      _builder.append("\t");
      _builder.append(IterableExtensions.join(ListExtensions.<String, String>map(causes, (String it) -> {
        return ("\"" + ElkGraphConversions.escape(it) + "\"");
      }), ",\n"));
      _builder.append("    \t");
      _builder.newLineIfNotEmpty();
      _builder.append("    ");
      _builder.append("]");
      _builder.newLine();
    }

    _builder.append("}");
    _builder.newLine();
    return _builder.toString();
  }

  private static String toJson(final Resource.Diagnostic diagnostic) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("{");
    _builder.newLine();
    _builder.append("    ");
    _builder.append("\"message\": \"");
    String _escape = ElkGraphConversions.escape(diagnostic.getMessage());
    _builder.append(_escape, "    ");
    _builder.append("\"");
    _builder.newLineIfNotEmpty();
    _builder.append("    ");
    _builder.append(",\"startLineNumber\": ");
    int _line = diagnostic.getLine();
    _builder.append(_line, "    ");
    _builder.newLineIfNotEmpty();
    _builder.append("    ");
    _builder.append(",\"startColumn\": ");
    int _column = diagnostic.getColumn();
    _builder.append(_column, "    ");
    _builder.newLineIfNotEmpty();
    {
      if ((diagnostic instanceof Diagnostic)) {
        _builder.append("    ");
        _builder.append(",\"endLineNumber\": ");
        int _lineEnd = ((Diagnostic)diagnostic).getLineEnd();
        _builder.append(_lineEnd, "    ");
        _builder.newLineIfNotEmpty();
        _builder.append("    ");
        _builder.append(",\"endColumn\": ");
        int _columnEnd = ((Diagnostic)diagnostic).getColumnEnd();
        _builder.append(_columnEnd, "    ");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("}");
    _builder.newLine();
    return _builder.toString();
  }

  /**
   * - - - - - - - - - - - - - -
   *   Actual conversion logic
   * - - - - - - - - - - - - - -
   */
  public static ElkGraphConversions.Result convert(final String inFormat, final String outFormat, final String graph) {
    if (Objects.equals(inFormat, outFormat)) {
      return new ElkGraphConversions.Result(
              HttpServletResponse.SC_OK,
              ElkGraphConversions.contentType(outFormat), graph);
    }
    ElkNode elkNode = null;
    try {
      switch (inFormat) {
        case "json":
          elkNode = ElkGraphConversions.loadJson(graph);
          break;
        case "elkt":
        case "elkg":
          elkNode = ElkGraphConversions.loadElkGraph(graph, inFormat);
          break;
      }
    } catch (Exception e) {
      ElkGraphConversions.LOG.log(Level.INFO, "Failed to load input graph.", e);
      return new ElkGraphConversions.Error(
              HttpServletResponse.SC_BAD_REQUEST,
              ElkGraphConversions.FAILURE_INPUT,
              "Failed to load input graph.", e);
    }
    String serializedResult = null;
    try {
      if (outFormat != null) {
        switch (outFormat) {
          case "json":
            serializedResult = ElkGraphConversions.toJson(elkNode);
            break;
          case "elkt":
          case "elkg":
            serializedResult = ElkGraphConversions.toElkGraph(elkNode, outFormat);
            break;
        }
      }
    } catch (Exception e) {
      ElkGraphConversions.LOG.log(Level.INFO, "Failed to serialize converted graph.", e);
      return new ElkGraphConversions.Error(
              HttpServletResponse.SC_BAD_REQUEST,
              ElkGraphConversions.FAILURE_OUTPUT,
              "Failed to serialize converted graph.", e);
    }
    return new ElkGraphConversions.Result(
            HttpServletResponse.SC_OK,
            ElkGraphConversions.contentType(outFormat),
            serializedResult);
  }

  private static ElkNode loadElkGraph(final String graph, final String format) throws IOException, ImportExportException {
    final Resource r = ElkGraphConversions.createResource(("dummy." + format));
    r.load(new URIConverter.ReadableInputStream(graph, "UTF-8"), null);
    final ElkNode elkGraph = ((ElkNode) r.getContents().get(0));
    if (!r.getErrors().isEmpty()) {
      EList<Resource.Diagnostic> _errors = r.getErrors();
      throw new ElkGraphConversions.ImportExportException(_errors);
    }
    return elkGraph;
  }

  private static ElkNode loadJson(final String graph) {
    return ElkGraphJson.forGraph(graph).toElk();
  }

  private static String toJson(final ElkNode graph) {
    return ElkGraphJson.forGraph(graph).prettyPrint(true).omitUnknownLayoutOptions(false).toJson();
  }

  private static String toElkGraph(final ElkNode graph, final String format) throws IOException {
    final Resource r = ElkGraphConversions.createResource(("dummy." + format));
    r.getContents().add(graph);
    final StringWriter sw = new StringWriter();
    final URIConverter.WriteableOutputStream os = new URIConverter.WriteableOutputStream(sw, "UTF-8");
    r.save(os, null);
    return sw.toString();
  }

  private static Resource createResource(final String uri) {
    return new ResourceSetImpl().createResource(URI.createURI(uri));
  }

  private static String contentType(final String format) {
    return ElkGraphConversions.FORMAT_TO_CONTENT_TYPE.get(format);
  }

  private static List<String> nestedExceptionMessages(final Throwable t) {
    ArrayList<String> current = new ArrayList<>();
    current.add(t.getClass().getSimpleName() + ": " + t.getMessage());
    if (t.getCause() != null) {
      current.addAll(ElkGraphConversions.nestedExceptionMessages(t.getCause()));
    }
    return current;
  }

  private static final Map<String, String> JSON_ESCAPES = Map.ofEntries(
          entry("\"", "\\\""),
          entry("\\", "\\\\"),
          entry("/", "\\/"),
          entry("\b", "\\b"),
          entry("\f", "\\f"),
          entry("\n", "\\n"),
          entry("\r", "\\r"),
          entry("\t", "\\t"));

  private static String escape(final String str) {
    return IterableExtensions.fold(ElkGraphConversions.JSON_ESCAPES.entrySet(), str, (String s, Map.Entry<String, String> pattern) -> {
      return s.replace(pattern.getKey(), pattern.getValue());
    });
  }
}
