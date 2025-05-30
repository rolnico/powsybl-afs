/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.ext.base;

import com.google.common.io.CharStreams;
import com.powsybl.afs.*;
import com.powsybl.afs.storage.events.AppStorageListener;
import com.powsybl.afs.storage.events.NodeDataUpdated;
import com.powsybl.afs.storage.events.NodeEvent;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public abstract class AbstractScript<T extends AbstractScript> extends ProjectFile implements StorableScript {

    private static final String INCLUDED_SCRIPTS_DEPENDENCY_NAME = "scriptIncludes";
    private static final String DEFAULT_SCRIPTS_DELIMITER = "\n\n";
    protected final OrderedDependencyManager orderedDependencyManager = new OrderedDependencyManager(this);
    private final String scriptContentName;
    private final List<ScriptListener> listeners = new ArrayList<>();
    private final AppStorageListener l = eventList -> processEvents(eventList.getEvents(), info.getId(), listeners);

    public AbstractScript(ProjectFileCreationContext context, int codeVersion, String scriptContentName) {
        super(context, codeVersion);
        this.scriptContentName = Objects.requireNonNull(scriptContentName);
        storage.getEventsBus().addListener(l);
    }

    private void processEvents(List<NodeEvent> events, String nodeId, List<ScriptListener> listeners) {
        for (NodeEvent event : events) {
            if (NodeDataUpdated.TYPENAME.equals(event.getType())) {
                NodeDataUpdated dataUpdated = (NodeDataUpdated) event;
                if (dataUpdated.getId().equals(nodeId) && scriptContentName.equals(dataUpdated.getDataName())) {
                    for (ScriptListener listener : listeners) {
                        listener.scriptUpdated();
                    }
                }
            }
        }
    }

    public List<AbstractScript> getIncludedScripts() {
        return orderedDependencyManager.getDependencies(INCLUDED_SCRIPTS_DEPENDENCY_NAME, AbstractScript.class);
    }

    public void addGenericScript(GenericScript genericScript) {
        if (getId().equals(genericScript.getId()) || genericScript.hasDeepDependency(this)) {
            throw new AfsCircularDependencyException();
        }
        orderedDependencyManager.appendDependencies(INCLUDED_SCRIPTS_DEPENDENCY_NAME, Collections.singletonList(genericScript));
        invalidate();
    }

    public void addScript(T includeScript) {
        if (getId().equals(includeScript.getId()) || includeScript.hasDeepDependency(this)) {
            throw new AfsCircularDependencyException();
        }
        orderedDependencyManager.appendDependencies(INCLUDED_SCRIPTS_DEPENDENCY_NAME, Collections.singletonList(includeScript));
        invalidate();
    }

    public void removeScript(String scriptNodeId) {
        orderedDependencyManager.removeDependencies(INCLUDED_SCRIPTS_DEPENDENCY_NAME, Collections.singletonList(scriptNodeId));
        invalidate();
    }

    public void switchIncludedDependencies(int dependencyIndex1, int dependencyIndex2) {
        List<AbstractScript> includedScripts = getIncludedScripts();
        if (dependencyIndex1 < 0 || dependencyIndex1 >= includedScripts.size() || dependencyIndex2 < 0 || dependencyIndex2 >= includedScripts.size()) {
            throw new AfsException("One or both indexes values are out of bounds");
        }
        List<AbstractScript> reOrderedIncludedScripts = new ArrayList<>(includedScripts);
        reOrderedIncludedScripts.set(dependencyIndex1, includedScripts.get(dependencyIndex2));
        reOrderedIncludedScripts.set(dependencyIndex2, includedScripts.get(dependencyIndex1));
        orderedDependencyManager.setDependencies(INCLUDED_SCRIPTS_DEPENDENCY_NAME, Collections.unmodifiableList(reOrderedIncludedScripts));
    }

    @Override
    public String readScript(boolean withIncludes) {
        String ownContent = readScript();
        if (withIncludes) {
            String includesScript = orderedDependencyManager
                .getDependencies(INCLUDED_SCRIPTS_DEPENDENCY_NAME, AbstractScript.class)
                .stream()
                .map(script -> script.readScript(true))
                .collect(Collectors.joining(DEFAULT_SCRIPTS_DELIMITER));
            if (StringUtils.isNotBlank(includesScript)) {
                includesScript += DEFAULT_SCRIPTS_DELIMITER;
            }
            return includesScript + ownContent;
        }
        return ownContent;
    }

    @Override
    public String readScript() {
        try {
            return CharStreams.toString(new InputStreamReader(storage.readBinaryData(info.getId(), scriptContentName)
                .orElseThrow(() -> new AfsException("Unable to read data from the node " + info.getId())), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void writeScript(String content) {
        try (Reader reader = new StringReader(content);
            Writer writer = new OutputStreamWriter(storage.writeBinaryData(info.getId(), scriptContentName), StandardCharsets.UTF_8)) {
            CharStreams.copy(reader, writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        storage.updateModificationTime(info.getId());
        storage.flush();

        // invalidate backward dependencies
        invalidate();
    }

    public void clearDependenciesCache() {
        orderedDependencyManager.clearCache();
    }

    @Override
    public void addListener(ScriptListener listener) {
        Objects.requireNonNull(listener);
        listeners.add(listener);
    }

    @Override
    public void removeListener(ScriptListener listener) {
        Objects.requireNonNull(listener);
        listeners.remove(listener);
    }
}
