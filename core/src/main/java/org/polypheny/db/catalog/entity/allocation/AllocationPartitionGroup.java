/*
 * Copyright 2019-2023 The Polypheny Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.polypheny.db.catalog.entity.allocation;

import com.google.common.collect.ImmutableList;
import io.activej.serializer.annotations.Deserialize;
import io.activej.serializer.annotations.Serialize;
import io.activej.serializer.annotations.SerializeNullable;
import java.io.Serializable;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.Nullable;
import org.polypheny.db.catalog.entity.CatalogObject;


@EqualsAndHashCode
@Value
public class AllocationPartitionGroup implements CatalogObject {

    private static final long serialVersionUID = 6229244317971622972L;

    @Serialize
    public long id;
    @Serialize
    public String name;
    @Serialize
    public long entityId;
    @Serialize
    public long namespaceId;
    @Serialize
    @SerializeNullable
    public ImmutableList<String> partitionQualifiers;
    @Serialize
    public boolean isUnbound;
    @Serialize
    public long partitionKey;


    public AllocationPartitionGroup(
            @Deserialize("id") final long id,
            @Deserialize("name") final String name,
            @Deserialize("entityId") final long entityId,
            @Deserialize("namespaceId") final long namespaceId,
            @Deserialize("partitionKey") final long partitionKey,
            @Deserialize("partitionQualifiers") @Nullable final List<String> partitionQualifiers,
            @Deserialize("isUnbound") final boolean isUnbound ) {
        this.id = id;
        this.name = name;
        this.entityId = entityId;
        this.namespaceId = namespaceId;
        this.partitionKey = partitionKey;
        // TODO @HENNLO Although the qualifiers are now part of CatalogPartitions, it might be a good improvement to
        //  accumulate all qualifiers of all internal partitions here to speed up query time.
        this.partitionQualifiers = partitionQualifiers == null ? null : ImmutableList.copyOf( partitionQualifiers );
        this.isUnbound = isUnbound;
    }


    @Override
    public Serializable[] getParameterArray() {
        throw new RuntimeException( "Not implemented" );
    }

}