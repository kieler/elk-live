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

import java.util.Objects;
import java.util.function.Consumer;
import org.eclipse.sprotty.Action;
import org.eclipse.sprotty.IDiagramServer;
import org.eclipse.sprotty.SModelRoot;
import org.eclipse.sprotty.xtext.IDiagramGenerator;
import org.eclipse.sprotty.xtext.LanguageAwareDiagramServer;
import org.eclipse.sprotty.xtext.ls.IssueProvider;
import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtend.lib.annotations.EqualsHashCode;
import org.eclipse.xtend.lib.annotations.ToString;
import org.eclipse.xtext.ide.server.ILanguageServerAccess;
import org.eclipse.xtext.xbase.lib.Pure;
import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;

@SuppressWarnings("all")
public class ElkDiagramServer extends LanguageAwareDiagramServer {
  @Accessors
  @EqualsHashCode
  @ToString(skipNulls = true)
  public static class ChangeLayoutVersionAction implements Action {
    public static final String KIND = "versionChange";

    private String kind = ElkDiagramServer.ChangeLayoutVersionAction.KIND;

    private String version;

    public ChangeLayoutVersionAction() {
    }

    public ChangeLayoutVersionAction(final Consumer<ElkDiagramServer.ChangeLayoutVersionAction> initializer) {
      initializer.accept(this);
    }

    @Pure
    @Override
    public String getKind() {
      return this.kind;
    }

    public void setKind(final String kind) {
      this.kind = kind;
    }

    @Pure
    public String getVersion() {
      return this.version;
    }

    public void setVersion(final String version) {
      this.version = version;
    }

    @Override
    @Pure
    public boolean equals(final Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ElkDiagramServer.ChangeLayoutVersionAction other = (ElkDiagramServer.ChangeLayoutVersionAction) obj;
      if (this.kind == null) {
        if (other.kind != null)
          return false;
      } else if (!this.kind.equals(other.kind))
        return false;
      if (this.version == null) {
        if (other.version != null)
          return false;
      } else if (!this.version.equals(other.version))
        return false;
      return true;
    }

    @Override
    @Pure
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((this.kind== null) ? 0 : this.kind.hashCode());
      return prime * result + ((this.version== null) ? 0 : this.version.hashCode());
    }

    @Override
    @Pure
    public String toString() {
      ToStringBuilder b = new ToStringBuilder(this);
      b.skipNulls();
      b.add("kind", this.kind);
      b.add("version", this.version);
      return b.toString();
    }
  }

  private String currentLayoutVersion = "snapshot";

  @Override
  protected void handleAction(final Action action) {
    String _kind = action.getKind();
    boolean _equals = Objects.equals(_kind, ElkDiagramServer.ChangeLayoutVersionAction.KIND);
    if (_equals) {
      final ElkDiagramServer.ChangeLayoutVersionAction versionAction = ((ElkDiagramServer.ChangeLayoutVersionAction) action);
      this.currentLayoutVersion = versionAction.version;
      this.getDiagramLanguageServer().getDiagramUpdater().updateDiagram(this);
    } else {
      super.handleAction(action);
    }
  }

  @Override
  protected IDiagramGenerator.Context createDiagramGeneratorContext(final ILanguageServerAccess.Context context, final IDiagramServer server, final IssueProvider issueProvider) {
    final IDiagramGenerator.Context generatorContext = super.createDiagramGeneratorContext(context, server, issueProvider);
    generatorContext.getState().getOptions().put("layoutVersion", this.currentLayoutVersion);
    return generatorContext;
  }

  /**
   * Usually the result of this method is extracted from configuration properties sent with the request.
   * However, as we (as the server) initiate the model update, we are unaware of what the client desires
   * and must set the value ourselves.
   */
  @Override
  protected boolean needsClientLayout(final SModelRoot root) {
    return false;
  }

  /**
   * The server-side layout is performed explicitly by the diagram generator, hence
   * the "regular" layout mechanism must not be used.
   * 
   * @see {@link ElkDiagramServer#needsClientLayout(SModelRoot)}
   */
  @Override
  protected boolean needsServerLayout(final SModelRoot root, final Action cause) {
    return false;
  }
}
