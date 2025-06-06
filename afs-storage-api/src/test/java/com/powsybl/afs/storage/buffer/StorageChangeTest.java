/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.storage.buffer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.afs.storage.json.AppStorageJsonModule;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.timeseries.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class StorageChangeTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = JsonUtil.createObjectMapper()
                .registerModule(new AppStorageJsonModule());
    }

    @Test
    void test() throws IOException {
        StorageChangeSet changeSet = new StorageChangeSet();
        changeSet.getChanges().add(new TimeSeriesCreation("id1", new TimeSeriesMetadata("ts1", TimeSeriesDataType.DOUBLE, InfiniteTimeSeriesIndex.INSTANCE)));
        changeSet.getChanges().add(new DoubleTimeSeriesChunksAddition("id1", 1, "ts1", Collections.singletonList(new UncompressedDoubleDataChunk(0, new double[] {1d, 2d}))));
        changeSet.getChanges().add(new StringTimeSeriesChunksAddition("id1", 1, "ts1", Collections.singletonList(new UncompressedStringDataChunk(0, new String[] {"a", "b"}))));

        StorageChangeSet changeSet2 = objectMapper.readValue(objectMapper.writeValueAsString(changeSet), StorageChangeSet.class);
        assertEquals(changeSet, changeSet2);
    }
}
