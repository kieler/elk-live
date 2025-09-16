/**
 * Copyright (c) 2017 TypeFox GmbH (http://www.typefox.io) and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * 
 * SPDX-License-Identifier: EPL-2.0
 */
package de.cau.cs.kieler.elkgraph.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.elk.core.IGraphLayoutEngine;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.UnsupportedConfigurationException;
import org.eclipse.elk.core.math.KVector;
import org.eclipse.elk.core.math.KVectorChain;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.core.util.LoggedGraph;
import org.eclipse.elk.graph.ElkBendPoint;
import org.eclipse.elk.graph.ElkConnectableShape;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkEdgeSection;
import org.eclipse.elk.graph.ElkGraphElement;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;
import org.eclipse.elk.graph.ElkShape;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.sprotty.BoundsAware;
import org.eclipse.sprotty.Dimension;
import org.eclipse.sprotty.Point;
import org.eclipse.sprotty.SEdge;
import org.eclipse.sprotty.SGraph;
import org.eclipse.sprotty.SLabel;
import org.eclipse.sprotty.SModelElement;
import org.eclipse.sprotty.SModelRoot;
import org.eclipse.sprotty.SNode;
import org.eclipse.sprotty.SPort;
import org.eclipse.sprotty.xtext.IDiagramGenerator;

/**
 * Transforms ELK graphs into sprotty models to be transferred to clients.
 */
@SuppressWarnings("all")
public class ElkGraphDiagramGenerator implements IDiagramGenerator {
  private static final Logger LOG = Logger.getLogger(ElkGraphDiagramGenerator.class.getName());

  private final IGraphLayoutEngine layoutEngine = new RecursiveGraphLayoutEngine();

  private int defaultPortSize = 5;

  private int defaultNodeSize = 30;

  @Override
  public SModelRoot generate(final IDiagramGenerator.Context context) {
    EObject originalGraph = null;
    if (context.getResource().getContents().size() > 0) {
      originalGraph = context.getResource().getContents().get(0);
    } else {
      return null;
    }
    if ((originalGraph instanceof ElkNode)) {
      try {
        final ElkNode elkGraph = EcoreUtil.<ElkNode>copy(((ElkNode)originalGraph));
        this.applyDefaults(elkGraph);
        final String layoutVersion = context.getState().getOptions().get("layoutVersion");
        final ExecutorService executor = Executors.newCachedThreadPool();
        final Callable<Object> layoutTask = new Callable<Object>() {
          @Override
          public Object call() {
            ElkNode laidOutGraph = null;
            if (Objects.equals(layoutVersion, "snapshot")) {
              {
                BasicProgressMonitor _basicProgressMonitor = new BasicProgressMonitor();
                ElkGraphDiagramGenerator.this.layoutEngine.layout(elkGraph, _basicProgressMonitor);
                laidOutGraph = elkGraph;
              }
            } else {
              if (ElkLayoutVersionRegistry.getVersionToWrapper().containsKey(layoutVersion)) {
                {
                  final ElkLayoutVersionWrapper wrapper = ElkLayoutVersionRegistry.getVersionToWrapper().get(layoutVersion);
                  final Optional<ElkNode> result = wrapper.layout(elkGraph);
                  if (!result.isPresent()) {
                    throw new RuntimeException((("Layout failed for version " + layoutVersion) + "."));
                  }
                  laidOutGraph = result.get();
                }
              } else {
                throw new UnsupportedConfigurationException((("Unknown layouter version: " + layoutVersion) + "."));
              }
            }
            return laidOutGraph;
          }
        };
        final int timeoutInSeconds = 5;

        final Future<Object> future = executor.<Object>submit(layoutTask);
        try {
          Object _get = future.get(timeoutInSeconds, TimeUnit.SECONDS);
          final ElkNode laidOutGraph = ((ElkNode) _get);
          final SGraph sgraph = new SGraph();
          sgraph.setType("graph");
          sgraph.setId(this.getId(elkGraph));
          this.processContent(laidOutGraph, sgraph);
          return sgraph;
        } catch (TimeoutException e) {
            final LoggedGraph loggedGraph = new LoggedGraph(
                    EcoreUtil.<ElkNode>copy(elkGraph), "TIMEOUT", LoggedGraph.Type.ELK);
            throw new RuntimeException((("Layout timed out after " + Integer.valueOf(timeoutInSeconds)) + " seconds.\nGraph input: ") + loggedGraph.serialize());
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        } catch (ExecutionException e) {
          throw new RuntimeException(e.getMessage());
        } finally {
          future.cancel(true);
        }
      } catch (RuntimeException e) {
        ElkGraphDiagramGenerator.LOG.log(Level.SEVERE, "Failed to generate ELK graph.", e);
      }
    }
    return null;
  }

  /**
   * Transform all content of the given parent node.
   */
  protected void processContent(final ElkNode parent, final SModelElement container) {
    for (final ElkPort elkPort : parent.getPorts()) {
      {
        final SPort sport = new SPort();
        sport.setType("port");
        sport.setId(this.getId(elkPort));
        this.transferBounds(elkPort, sport);
        this.addChild(container, sport);
        this.processLabels(elkPort, sport);
      }
    }
    for (final ElkNode elkNode : parent.getChildren()) {
      {
        final SNode snode = new SNode();
        snode.setType("node");
        snode.setId(this.getId(elkNode));
        this.transferBounds(elkNode, snode);
        this.addChild(container, snode);
        this.processLabels(elkNode, snode);
        this.processContent(elkNode, snode);
      }
    }
    for (final ElkEdge elkEdge : parent.getContainedEdges()) {
      if (((elkEdge.getSources().size() == 1) && (elkEdge.getTargets().size() == 1))) {
        final SEdge sedge = new SEdge();
        sedge.setType("edge");
        sedge.setId(this.getId(elkEdge));
        sedge.setSourceId(this.getId(elkEdge.getSources().get(0)));
        sedge.setTargetId(this.getId(elkEdge.getTargets().get(0)));
        this.transferEdgeLayout(elkEdge, sedge);
        this.addChild(container, sedge);
        this.processLabels(elkEdge, sedge);
      } else {
        for (final ElkConnectableShape source : elkEdge.getSources()) {
          for (final ElkConnectableShape target : elkEdge.getTargets()) {
            {
              final SEdge sedge_1 = new SEdge();
              sedge_1.setType("edge");
              String _id = this.getId(elkEdge);
              String _plus = (_id + "_");
              String _id_1 = this.getId(source);
              String _plus_1 = (_plus + _id_1);
              String _plus_2 = (_plus_1 + "_");
              String _id_2 = this.getId(target);
              String _plus_3 = (_plus_2 + _id_2);
              sedge_1.setId(_plus_3);
              sedge_1.setSourceId(this.getId(source));
              sedge_1.setTargetId(this.getId(target));
              this.transferEdgeLayout(elkEdge, sedge_1);
              this.addChild(container, sedge_1);
              this.processLabels(elkEdge, sedge_1);
            }
          }
        }
      }
    }
  }

  /**
   * Transform all labels of the given graph element.
   */
  protected void processLabels(final ElkGraphElement element, final SModelElement container) {
    EList<ElkLabel> _labels = element.getLabels();
    for (final ElkLabel elkLabel : _labels) {
      {
        final SLabel slabel = new SLabel();
        slabel.setType("label");
        slabel.setId(this.getId(elkLabel));
        slabel.setText(elkLabel.getText());
        this.transferBounds(elkLabel, slabel);
        this.addChild(container, slabel);
        this.processLabels(elkLabel, slabel);
      }
    }
  }

  /**
   * Apply default layout information to all contents of the given parent node.
   */
  private void applyDefaults(final ElkNode parent) {
    for (final ElkPort port : parent.getPorts()) {
      {
        if (port.getWidth() <= 0) {
          port.setWidth(this.defaultPortSize);
        }
        if (port.getHeight() <= 0) {
          port.setHeight(this.defaultPortSize);
        }
        this.computeLabelSizes(port);
      }
    }
    for (final ElkNode node : parent.getChildren()) {
      {
        if (node.getWidth() <= 0) {
          node.setWidth(this.defaultNodeSize);
        }
        if (node.getHeight() <= 0) {
          node.setHeight(this.defaultNodeSize);
        }
        this.computeLabelSizes(node);
        this.applyDefaults(node);
      }
    }
    for (final ElkEdge edge : parent.getContainedEdges()) {
      this.computeLabelSizes(edge);
    }
  }

  /**
   * Compute sizes for all labels of an element. <em>Note:</em> Sizes are hard-coded here, so don't expect
   * the result to be rendered properly on all clients!
   */
  private void computeLabelSizes(final ElkGraphElement element) {
    for (final ElkLabel label : element.getLabels()) {
      if (label.getText() != null && !label.getText().isEmpty()) {
        if (label.getWidth() <= 0) {
          label.setWidth(label.getText().length() * 9);
        }
        if (label.getHeight() <= 0) {
          label.setHeight(16);
        }
      }
    }
  }

  /**
   * Add a child element to the sprotty model.
   */
  private void addChild(final SModelElement container, final SModelElement child) {
    if (container.getChildren() == null) {
      container.setChildren(new ArrayList<SModelElement>());
    }
    container.getChildren().add(child);
  }

  /**
   * Transfer bounds to a sprotty model element.
   */
  private void transferBounds(final ElkShape shape, final BoundsAware bounds) {
    bounds.setPosition(new Point(shape.getX(), shape.getY()));
    if (((shape.getWidth() > 0) || (shape.getHeight() > 0))) {
      bounds.setSize(new Dimension(shape.getWidth(), shape.getHeight()));
    }
  }

  /**
   * Transfer an edge layout to a sprotty edge.
   */
  private void transferEdgeLayout(final ElkEdge elkEdge, final SEdge sEdge) {
    sEdge.setRoutingPoints(new ArrayList<Point>());
    for (final ElkEdgeSection section : elkEdge.getSections()) {
        sEdge.getRoutingPoints().add(new Point(section.getStartX(), section.getStartY()));
        for (final ElkBendPoint bendPoint : section.getBendPoints()) {
          sEdge.getRoutingPoints().add(new Point(bendPoint.getX(), bendPoint.getY()));
        }
      sEdge.getRoutingPoints().add(new Point(section.getEndX(), section.getEndY()));
    }
    final KVectorChain junctionPoints = elkEdge.<KVectorChain>getProperty(CoreOptions.JUNCTION_POINTS);
    int index = 0;
    for (KVector point : junctionPoints) {
      final SNode sJunction = new SNode();
      sJunction.setType("junction");
      sJunction.setId(this.getId(elkEdge) + "_j" + index);
      sJunction.setPosition(new Point(point.x, point.y));
      this.addChild(sEdge, sJunction);
      index++;
    }
  }

  /**
   * Compute a unique identifier for the given element.
   */
  private String getId(final ElkGraphElement element) {
    final EObject container = element.eContainer();
    if ((container instanceof ElkGraphElement)) {
      String identifier = element.getIdentifier();
      if ((identifier == null)) {
        final EStructuralFeature feature = element.eContainingFeature();
        final List<? extends ElkGraphElement> list = (List<? extends ElkGraphElement>) container.eGet(feature);
        identifier = feature.getName() + "#" + list.indexOf(element);
      }
      return this.getId((ElkGraphElement) container) + "." + identifier;
    } else {
      return element.getIdentifier() != null ? element.getIdentifier() : "graph";
    }
  }

  private SGraph showError(final Throwable throwable) {
    final SGraph sgraph = new SGraph();
    sgraph.setType("graph");
    final SLabel label = new SLabel();
    label.setType("label");
    label.setId("error");
    label.setText(throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
    label.setPosition(new Point(20, 20));
    this.addChild(sgraph, label);
    return sgraph;
  }

  public void setDefaultPortSize(final int defaultPortSize) {
    this.defaultPortSize = defaultPortSize;
  }

  public void setDefaultNodeSize(final int defaultNodeSize) {
    this.defaultNodeSize = defaultNodeSize;
  }
}
