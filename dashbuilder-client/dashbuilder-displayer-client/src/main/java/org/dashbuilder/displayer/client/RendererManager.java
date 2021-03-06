/**
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dashbuilder.displayer.client;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.EnumMap;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.dashbuilder.common.client.StringUtils;
import org.dashbuilder.displayer.DisplayerSettings;
import org.dashbuilder.displayer.DisplayerSubType;
import org.dashbuilder.displayer.DisplayerType;
import org.dashbuilder.displayer.client.resources.i18n.CommonConstants;
import org.jboss.errai.ioc.client.container.IOC;
import org.jboss.errai.ioc.client.container.SyncBeanDef;
import org.jboss.errai.ioc.client.container.SyncBeanManager;

/**
 * This class holds a registry of all the RendererLibrary implementations available.
 */
@ApplicationScoped
public class RendererManager {

    private SyncBeanManager beanManager;
    private List<RendererLibrary> renderersList = new ArrayList<RendererLibrary>();
    private Map<DisplayerType, RendererLibrary> renderersDefault = new EnumMap<DisplayerType, RendererLibrary>(DisplayerType.class);
    private Map<DisplayerType, List<RendererLibrary>> renderersByType = new EnumMap<DisplayerType, List<RendererLibrary>>(DisplayerType.class);
    private Map<DisplayerSubType, List<RendererLibrary>> renderersBySubType = new EnumMap<DisplayerSubType, List<RendererLibrary>>(DisplayerSubType.class);

    public RendererManager() {
    }

    @Inject
    public RendererManager(SyncBeanManager beanManager) {
        this.beanManager = beanManager;
    }

    @PostConstruct
    private void init() {
        Collection<SyncBeanDef<RendererLibrary>> beanDefs = beanManager.lookupBeans(RendererLibrary.class);
        for (SyncBeanDef<RendererLibrary> beanDef : beanDefs) {

            RendererLibrary lib = beanDef.getInstance();
            renderersList.add(lib);

            for (DisplayerType displayerType : DisplayerType.values()) {
                if (lib.isDefault(displayerType)) {
                    renderersDefault.put(displayerType, lib);
                }
            }
            List<DisplayerType> types = lib.getSupportedTypes();
            if (types != null && !types.isEmpty()) {

                for (DisplayerType type : types) {
                    List<RendererLibrary> set = renderersByType.get(type);
                    if (set == null) {
                        set = new ArrayList<RendererLibrary>();
                        renderersByType.put(type, set);
                    }
                    set.add(lib);

                    List<DisplayerSubType> subTypes = lib.getSupportedSubtypes(type);
                    if (subTypes != null && !subTypes.isEmpty()) {
                        for (DisplayerSubType subType : subTypes) {
                            List<RendererLibrary> subset = renderersBySubType.get(subType);
                            if (subset == null) {
                                subset = new ArrayList<RendererLibrary>();
                                renderersBySubType.put(subType, subset);
                            }
                            subset.add(lib);
                        }
                    }
                }
            }
        }
    }

    public List<RendererLibrary> getRenderers() {
        return renderersList;
    }

    public RendererLibrary getDefaultRenderer(DisplayerType displayerType) {
        return renderersDefault.get(displayerType);
    }

    public void setDefaultRenderer(DisplayerType displayerType, String rendererName) {
        renderersDefault.put(displayerType, getRendererByUUID(rendererName));
    }

    public List<RendererLibrary> getRenderersForType(DisplayerType displayerType) {
        List<RendererLibrary> result = renderersByType.get(displayerType);
        if (result == null) return new ArrayList<RendererLibrary>();
        return result;
    }

    public List<RendererLibrary> getRenderersForType(DisplayerType type, DisplayerSubType subType) {
        if (type == null) {
            return subType == null ? renderersList : renderersBySubType.get(subType);
        }
        else if (subType == null) {
            return renderersByType.get(type);
        }
        else {
            List<RendererLibrary> types  = renderersByType.get(type);
            List<RendererLibrary> result = new ArrayList<RendererLibrary>(renderersBySubType.get(subType));
            Iterator<RendererLibrary> it = result.iterator();
            while (it.hasNext()) {
                RendererLibrary rl = it.next();
                if (!types.contains(rl)) {
                    it.remove();
                }
            }
            return result;
        }
    }

    public RendererLibrary getRendererByUUID(String renderer) {
        RendererLibrary lib = _getRendererByUUID(renderer);
        if (lib != null) return lib;
        throw new RuntimeException(CommonConstants.INSTANCE.rendererliblocator_renderer_not_found(renderer));
    }

    private RendererLibrary _getRendererByUUID(String renderer) {
        for (RendererLibrary lib : renderersList) {
            if (lib.getUUID().equals(renderer)) {
                return lib;
            }
        }
        return null;
    }

    public RendererLibrary getRendererByName(String renderer) {
        for (RendererLibrary lib : renderersList) {
            if (lib.getName().equals(renderer)) {
                return lib;
            }
        }
        throw new RuntimeException(CommonConstants.INSTANCE.rendererliblocator_renderer_not_found(renderer));
    }

    public RendererLibrary getRendererForType(DisplayerType displayerType) {

        // Return the default
        RendererLibrary defaultRenderer = renderersDefault.get(displayerType);
        if (defaultRenderer != null) return defaultRenderer;

        // Return the first available if no default has been defined
        return renderersByType.get(displayerType).get(0);
    }

    public RendererLibrary getRendererForDisplayer(DisplayerSettings target) {

        // Get the renderer specified
        if (!StringUtils.isBlank(target.getRenderer())) {
            RendererLibrary targetRenderer = _getRendererByUUID(target.getRenderer());
            if (targetRenderer != null) return targetRenderer;
        }

        // Return always the renderer declared as default
        List<RendererLibrary> renderersSupported = getRenderersForType(target.getType(), target.getSubtype());
        RendererLibrary defaultRenderer = getDefaultRenderer(target.getType());
        for (RendererLibrary rendererLibrary : renderersSupported) {
            if (defaultRenderer != null && rendererLibrary.equals(defaultRenderer)) {
                return defaultRenderer;
            }
        }
        // If no default then return the first supported one
        if (!renderersSupported.isEmpty()) return renderersSupported.get(0);
        throw new RuntimeException("No renderer is available for: " + target.getType());
    }
}
