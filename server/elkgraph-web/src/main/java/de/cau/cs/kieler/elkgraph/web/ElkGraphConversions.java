/**
 * Copyright (c) 2020 Kiel University and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * 
 * SPDX-License-Identifier: EPL-2.0
 */
package de.cau.cs.kieler.elkgraph.web;

import com.google.common.collect.Iterables;
import com.google.common.io.CharStreams;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.json.ElkGraphJson;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtend.lib.annotations.FinalFieldsConstructor;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.diagnostics.Diagnostic;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.Functions.Function2;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;
import org.eclipse.xtext.xbase.lib.Pair;
import org.eclipse.xtext.xbase.lib.Pure;

@SuppressWarnings("all")
public class ElkGraphConversions {
  @FinalFieldsConstructor
  @Accessors
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

    @Pure
    public int getStatusCode() {
      return this.statusCode;
    }

    @Pure
    public String getContentType() {
      return this.contentType;
    }

    @Pure
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
        String _xifexpression = null;
        if ((e instanceof ElkGraphConversions.ImportExportException)) {
          final Function1<Resource.Diagnostic, String> _function = (Resource.Diagnostic it) -> {
            return ElkGraphConversions.toJson(it);
          };
          _xifexpression = ElkGraphConversions.toErrorJson(message, type, ListExtensions.<Resource.Diagnostic, String>map(((ElkGraphConversions.ImportExportException)e).diagnostics, _function), null);
        } else {
          _xifexpression = ElkGraphConversions.toErrorJson(message, type, null, ElkGraphConversions.nestedExceptionMessages(e));
        }
        return _xifexpression;
      }).apply());
    }
  }

  @FinalFieldsConstructor
  public static class ImportExportException extends Exception {
    private final List<Resource.Diagnostic> diagnostics;

    public ImportExportException(final List<Resource.Diagnostic> diagnostics) {
      super();
      this.diagnostics = diagnostics;
    }
  }

  private static final Logger LOG = Logger.getLogger(ElkGraphConversions.class.getName());

  public static final Set<String> KNWON_FORMATS = Collections.<String>unmodifiableSet(CollectionLiterals.<String>newHashSet("elkt", "elkg", "json"));

  private static final Map<String, String> FORMAT_TO_CONTENT_TYPE = Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(Pair.<String, String>of("json", "application/json"), Pair.<String, String>of("elkg", "application/xml"), Pair.<String, String>of("elkt", "text/plain"), Pair.<String, String>of("error", "application/json")));

  private static final String FAILURE_REQUEST = "request";

  private static final String FAILURE_INPUT = "input";

  private static final String FAILURE_OUTPUT = "output";

  /**
   * - - - - - - - - - - - - - -
   *   HTTP request handling
   * - - - - - - - - - - - - - -
   */
  public static void handleRequest(final HttpServletRequest req, final HttpServletResponse resp) {
    try {
      final Optional<ElkGraphConversions.Error> errorOptional = ElkGraphConversions.checkRequest(req);
      boolean _isPresent = errorOptional.isPresent();
      if (_isPresent) {
        ElkGraphConversions.sendResult(resp, errorOptional.get());
        return;
      }
      final String inFormat = req.getParameter("inFormat");
      final String outFormat = req.getParameter("outFormat");
      final String graph = CharStreams.toString(req.getReader());
      final ElkGraphConversions.Result result = ElkGraphConversions.convert(inFormat, outFormat, graph);
      ElkGraphConversions.sendResult(resp, result);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  private static PrintWriter sendResult(final HttpServletResponse resp, final ElkGraphConversions.Result result) {
    try {
      PrintWriter _xblockexpression = null;
      {
        resp.setStatus(result.statusCode);
        resp.setHeader("Content-Type", result.contentType);
        _xblockexpression = resp.getWriter().append(result.content);
      }
      return _xblockexpression;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  private static Optional<ElkGraphConversions.Error> checkRequest(final HttpServletRequest req) {
    final String reqContentType = req.getHeader("Content-Type");
    if (((reqContentType == null) || (!ElkGraphConversions.FORMAT_TO_CONTENT_TYPE.values().contains(reqContentType)))) {
      ElkGraphConversions.Error _error = new ElkGraphConversions.Error(HttpServletResponse.SC_NOT_ACCEPTABLE, ElkGraphConversions.FAILURE_REQUEST, (("Unsupported content type: \'" + reqContentType) + "\'."));
      return Optional.<ElkGraphConversions.Error>of(_error);
    }
    final String inFormat = req.getParameter("inFormat");
    if ((inFormat == null)) {
      ElkGraphConversions.Error _error_1 = new ElkGraphConversions.Error(HttpServletResponse.SC_BAD_REQUEST, ElkGraphConversions.FAILURE_REQUEST, "Missing specification of \'inFormat\'.");
      return Optional.<ElkGraphConversions.Error>of(_error_1);
    }
    final String outFormat = req.getParameter("outFormat");
    if ((outFormat == null)) {
      ElkGraphConversions.Error _error_2 = new ElkGraphConversions.Error(HttpServletResponse.SC_BAD_REQUEST, ElkGraphConversions.FAILURE_REQUEST, "Missing specification of \'outFormat\'.");
      return Optional.<ElkGraphConversions.Error>of(_error_2);
    }
    for (final String format : new String[] { inFormat, outFormat }) {
      boolean _contains = ElkGraphConversions.KNWON_FORMATS.contains(format);
      boolean _not = (!_contains);
      if (_not) {
        ElkGraphConversions.Error _error_3 = new ElkGraphConversions.Error(HttpServletResponse.SC_BAD_REQUEST, ElkGraphConversions.FAILURE_REQUEST, (("Unknown graph format \'" + format) + "\'."));
        return Optional.<ElkGraphConversions.Error>of(_error_3);
      }
    }
    return Optional.<ElkGraphConversions.Error>empty();
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
      boolean _isNullOrEmpty = IterableExtensions.isNullOrEmpty(diagnosticJsons);
      boolean _not = (!_isNullOrEmpty);
      if (_not) {
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
    {
      boolean _isNullOrEmpty_1 = IterableExtensions.isNullOrEmpty(causes);
      boolean _not_1 = (!_isNullOrEmpty_1);
      if (_not_1) {
        _builder.append("    ");
        _builder.append(",\"causes\": [");
        _builder.newLine();
        _builder.append("    ");
        _builder.append("\t");
        final Function1<String, String> _function = (String it) -> {
          String _escape = ElkGraphConversions.escape(it);
          String _plus = ("\"" + _escape);
          return (_plus + "\"");
        };
        String _join_1 = IterableExtensions.join(ListExtensions.<String, String>map(causes, _function), ",\n");
        _builder.append(_join_1, "    \t");
        _builder.newLineIfNotEmpty();
        _builder.append("    ");
        _builder.append("]");
        _builder.newLine();
      }
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
    boolean _equals = Objects.equals(inFormat, outFormat);
    if (_equals) {
      String _contentType = ElkGraphConversions.contentType(outFormat);
      return new ElkGraphConversions.Result(HttpServletResponse.SC_OK, _contentType, graph);
    }
    ElkNode _xtrycatchfinallyexpression = null;
    try {
      ElkNode _switchResult = null;
      if (inFormat != null) {
        switch (inFormat) {
          case "json":
            _switchResult = ElkGraphConversions.loadJson(graph);
            break;
          case "elkt":
          case "elkg":
            _switchResult = ElkGraphConversions.loadElkGraph(graph, inFormat);
            break;
        }
      }
      _xtrycatchfinallyexpression = _switchResult;
    } catch (final Throwable _t) {
      if (_t instanceof Exception) {
        final Exception e = (Exception)_t;
        ElkGraphConversions.LOG.log(Level.INFO, "Failed to load input graph.", e);
        return new ElkGraphConversions.Error(HttpServletResponse.SC_BAD_REQUEST, ElkGraphConversions.FAILURE_INPUT, "Failed to load input graph.", e);
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
    final ElkNode elkNode = _xtrycatchfinallyexpression;
    String _xtrycatchfinallyexpression_1 = null;
    try {
      String _switchResult = null;
      if (outFormat != null) {
        switch (outFormat) {
          case "json":
            _switchResult = ElkGraphConversions.toJson(elkNode);
            break;
          case "elkt":
          case "elkg":
            _switchResult = ElkGraphConversions.toElkGraph(elkNode, outFormat);
            break;
        }
      }
      _xtrycatchfinallyexpression_1 = _switchResult;
    } catch (final Throwable _t) {
      if (_t instanceof Exception) {
        final Exception e = (Exception)_t;
        ElkGraphConversions.LOG.log(Level.INFO, "Failed to serialize converted graph.", e);
        return new ElkGraphConversions.Error(HttpServletResponse.SC_BAD_REQUEST, ElkGraphConversions.FAILURE_OUTPUT, "Failed to serialize converted graph.", e);
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
    final String serializedResult = _xtrycatchfinallyexpression_1;
    String _contentType_1 = ElkGraphConversions.contentType(outFormat);
    return new ElkGraphConversions.Result(HttpServletResponse.SC_OK, _contentType_1, serializedResult);
  }

  private static ElkNode loadElkGraph(final String graph, final String format) {
    try {
      final Resource r = ElkGraphConversions.createResource(("dummy." + format));
      URIConverter.ReadableInputStream _readableInputStream = new URIConverter.ReadableInputStream(graph, "UTF-8");
      r.load(_readableInputStream, null);
      EObject _head = IterableExtensions.<EObject>head(r.getContents());
      final ElkNode elkGraph = ((ElkNode) _head);
      boolean _isEmpty = r.getErrors().isEmpty();
      boolean _not = (!_isEmpty);
      if (_not) {
        EList<Resource.Diagnostic> _errors = r.getErrors();
        throw new ElkGraphConversions.ImportExportException(_errors);
      }
      return elkGraph;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  private static ElkNode loadJson(final String graph) {
    return ElkGraphJson.forGraph(graph).toElk();
  }

  private static String toJson(final ElkNode graph) {
    return ElkGraphJson.forGraph(graph).prettyPrint(true).omitUnknownLayoutOptions(false).toJson();
  }

  private static String toElkGraph(final ElkNode graph, final String format) {
    try {
      final Resource r = ElkGraphConversions.createResource(("dummy." + format));
      EList<EObject> _contents = r.getContents();
      _contents.add(graph);
      final StringWriter sw = new StringWriter();
      final URIConverter.WriteableOutputStream os = new URIConverter.WriteableOutputStream(sw, "UTF-8");
      r.save(os, null);
      return sw.toString();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }

  private static Resource createResource(final String uri) {
    return new ResourceSetImpl().createResource(URI.createURI(uri));
  }

  private static String contentType(final String format) {
    return ElkGraphConversions.FORMAT_TO_CONTENT_TYPE.get(format);
  }

  private static List<String> nestedExceptionMessages(final Throwable t) {
    String _simpleName = t.getClass().getSimpleName();
    String _plus = (_simpleName + ": ");
    String _message = t.getMessage();
    String _plus_1 = (_plus + _message);
    ArrayList<String> current = CollectionLiterals.<String>newArrayList(_plus_1);
    Throwable _cause = t.getCause();
    boolean _tripleNotEquals = (_cause != null);
    if (_tripleNotEquals) {
      List<String> _nestedExceptionMessages = ElkGraphConversions.nestedExceptionMessages(t.getCause());
      Iterables.<String>addAll(current, _nestedExceptionMessages);
    }
    return current;
  }

  private static final Map<String, String> JSON_ESCAPES = Collections.<String, String>unmodifiableMap(CollectionLiterals.<String, String>newHashMap(Pair.<String, String>of("\"", "\\\""), Pair.<String, String>of("\\", "\\\\"), Pair.<String, String>of("/", "\\/"), Pair.<String, String>of("\b", "\\b"), Pair.<String, String>of("\f", "\\f"), Pair.<String, String>of("\n", "\\n"), Pair.<String, String>of("\r", "\\r"), Pair.<String, String>of("\t", "\\t")));

  private static String escape(final String str) {
    final Function2<String, Map.Entry<String, String>, String> _function = (String s, Map.Entry<String, String> pattern) -> {
      return s.replace(pattern.getKey(), pattern.getValue());
    };
    return IterableExtensions.<Map.Entry<String, String>, String>fold(ElkGraphConversions.JSON_ESCAPES.entrySet(), str, _function);
  }
}
