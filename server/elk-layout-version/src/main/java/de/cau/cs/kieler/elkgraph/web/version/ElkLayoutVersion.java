//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package de.cau.cs.kieler.elkgraph.web.version;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.elk.core.IGraphLayoutEngine;
import org.eclipse.elk.core.RecursiveGraphLayoutEngine;
import org.eclipse.elk.core.data.ILayoutMetaDataProvider;
import org.eclipse.elk.core.data.LayoutAlgorithmData;
import org.eclipse.elk.core.data.LayoutMetaDataService;
import org.eclipse.elk.core.options.CoreOptions;
import org.eclipse.elk.core.util.BasicProgressMonitor;
import org.eclipse.elk.core.util.persistence.ElkGraphResourceFactory;
import org.eclipse.elk.graph.ElkGraphPackage;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.Resource.Factory.Registry;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

public class ElkLayoutVersion implements IElkLayoutVersion {
    private static final Logger LOG = Logger.getLogger(ElkLayoutVersion.class.getName());
    private final IGraphLayoutEngine layoutEngine = new RecursiveGraphLayoutEngine();

    public ElkLayoutVersion() {
        try {
            Map<String, Object> _extensionToFactoryMap = Registry.INSTANCE.getExtensionToFactoryMap();
            ElkGraphResourceFactory _elkGraphResourceFactory = new ElkGraphResourceFactory();
            _extensionToFactoryMap.put("elkg", _elkGraphResourceFactory);
            ElkGraphPackage.eINSTANCE.eClass();

            try {
                LayoutMetaDataService.getInstance();
            } catch (Throwable var8) {
                if (!(var8 instanceof ServiceConfigurationError)) {
                    throw Exceptions.sneakyThrow(var8);
                }

                Constructor<LayoutMetaDataService> ctor = LayoutMetaDataService.class.getDeclaredConstructor();
                ctor.setAccessible(true);
                Field instanceField = LayoutMetaDataService.class.getDeclaredField("instance");
                instanceField.setAccessible(true);
                instanceField.set((Object)null, ctor.newInstance());
                LayoutMetaDataService _instance = LayoutMetaDataService.getInstance();
                CoreOptions _coreOptions = new CoreOptions();
                _instance.registerLayoutMetaDataProviders(new ILayoutMetaDataProvider[]{_coreOptions});
            }

            Functions.Function1<LayoutAlgorithmData, Boolean> _function = (it) -> {
                return it.getId().contains("layered");
            };
            LayoutAlgorithmData _findFirst = (LayoutAlgorithmData)IterableExtensions.findFirst(LayoutMetaDataService.getInstance().getAlgorithmData(), _function);
            boolean manualRegistrationRequired = _findFirst == null;
            if (manualRegistrationRequired) {
                LOG.info("Manually registering layout providers (required prior to 0.7.0).");
                Consumer<String> _function_1 = (providerClassName) -> {
                    try {
                        Class<?> clazz = Class.forName(providerClassName);
                        Object _newInstance = clazz.newInstance();
                        LayoutMetaDataService.getInstance().registerLayoutMetaDataProviders(new ILayoutMetaDataProvider[]{(ILayoutMetaDataProvider)_newInstance});
                    } catch (Throwable var3) {
                        if (!(var3 instanceof Throwable)) {
                            throw Exceptions.sneakyThrow(var3);
                        }

                        LOG.info(providerClassName + " could not be registered (this is not necessarily an error)");
                    }

                };
                Collections.unmodifiableList(CollectionLiterals.newArrayList(new String[]{"org.eclipse.elk.alg.layered.options.LayeredMetaDataProvider", "org.eclipse.elk.alg.force.options.ForceMetaDataProvider", "org.eclipse.elk.alg.force.options.StressMetaDataProvider", "org.eclipse.elk.alg.mrtree.options.MrTreeMetaDataProvider", "org.eclipse.elk.alg.radial.options.RadialMetaDataProvider", "org.eclipse.elk.alg.common.compaction.options.PolyominoOptions", "org.eclipse.elk.alg.disco.options.DisCoMetaDataProvider", "org.eclipse.elk.alg.spore.options.SporeMetaDataProvider", "org.eclipse.elk.alg.rectpacking.options.RectPackingMetaDataProvider"})).forEach(_function_1);
            }

        } catch (Throwable var9) {
            throw Exceptions.sneakyThrow(var9);
        }
    }

    public String layout(String serializedGraph) {
        Function<ElkNode, ElkNode> _function = (it) -> {
            return this.layout(it);
        };
        Function<ElkNode, String> _function_1 = (it) -> {
            return (String)this.serialize(it).orElse(null);
        };
        return (String)this.deserialize(serializedGraph).map(_function).map(_function_1).orElse(null);
    }

    protected ElkNode layout(ElkNode elkGraph) {
        BasicProgressMonitor _basicProgressMonitor = new BasicProgressMonitor();
        this.layoutEngine.layout(elkGraph, _basicProgressMonitor);
        return elkGraph;
    }

    protected Optional<ElkNode> deserialize(String serializedGraph) {
        ResourceSetImpl rs = new ResourceSetImpl();
        Resource r = rs.createResource(URI.createURI("dummy.elkg"));

        try {
            URIConverter.ReadableInputStream _readableInputStream = new URIConverter.ReadableInputStream(serializedGraph);
            r.load(_readableInputStream, (Map)null);
            boolean _isEmpty = r.getContents().isEmpty();
            boolean _not = !_isEmpty;
            if (_not) {
                EObject _head = (EObject)IterableExtensions.head(r.getContents());
                return Optional.of((ElkNode)_head);
            }
        } catch (Throwable var8) {
            if (!(var8 instanceof IOException)) {
                throw Exceptions.sneakyThrow(var8);
            }

            IOException e = (IOException)var8;
            LOG.log(Level.WARNING, "elkg deserialization failed (for concrete layout version).", e);
        }

        return Optional.empty();
    }

    protected Optional<String> serialize(ElkNode laidOutGraph) {
        Resource r = laidOutGraph.eResource();
        StringWriter sw = new StringWriter();
        URIConverter.WriteableOutputStream os = new URIConverter.WriteableOutputStream(sw, "UTF-8");

        try {
            r.save(os, (Map)null);
            return Optional.of(sw.toString());
        } catch (Throwable var7) {
            if (var7 instanceof IOException e) {
                LOG.log(Level.WARNING, "elkg serialization failed (for concrete layout version).", e);
                return Optional.empty();
            } else {
                throw Exceptions.sneakyThrow(var7);
            }
        }
    }
}
