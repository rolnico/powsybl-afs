/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.afs.storage;

import com.google.common.collect.ImmutableMap;
import com.google.common.testing.EqualsTester;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class NodeGenericMetadataTest {

    @Test
    void test() {
        // check getters
        NodeGenericMetadata metadata = new NodeGenericMetadata()
            .setString("s1", "a")
            .setString("s2", "b")
            .setDouble("d1", 0d)
            .setInt("i1", 1)
            .setBoolean("b1", true);
        assertEquals("a", metadata.getString("s1"));
        assertEquals("b", metadata.getString("s2"));
        assertEquals(ImmutableMap.of("s1", "a", "s2", "b"), metadata.getStrings());
        assertEquals(0d, metadata.getDouble("d1"), 0d);
        assertEquals(ImmutableMap.of("d1", 0d), metadata.getDoubles());
        assertEquals(1, metadata.getInt("i1"));
        assertEquals(ImmutableMap.of("i1", 1), metadata.getInts());
        assertTrue(metadata.getBoolean("b1"));
        assertEquals(ImmutableMap.of("b1", true), metadata.getBooleans());

        // check metadata not found
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> metadata.getString("s3"));
        assertEquals("String metadata 's3' not found", exception.getMessage());

        // check equality
        new EqualsTester()
            .addEqualityGroup(new NodeGenericMetadata().setString("s1", "a"),
                new NodeGenericMetadata().setString("s1", "a"))
            .addEqualityGroup(new NodeGenericMetadata().setDouble("d1", 3d),
                new NodeGenericMetadata().setDouble("d1", 3d))
            .testEquals();
    }
}
