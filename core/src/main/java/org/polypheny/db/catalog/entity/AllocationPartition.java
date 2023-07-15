/*
 * Copyright 2019-2022 The Polypheny Project
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

package org.polypheny.db.catalog.entity;

import com.google.common.collect.ImmutableList;
import io.activej.serializer.annotations.Deserialize;
import io.activej.serializer.annotations.Serialize;
import io.activej.serializer.annotations.SerializeNullable;
import java.io.Serializable;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.jetbrains.annotations.Nullable;


@EqualsAndHashCode
@Value
public class AllocationPartition implements CatalogObject {

    private static final long serialVersionUID = -1124423133579338133L;

    @Serialize
    public long id;

    @Getter
    @Serialize
    @SerializeNullable
    public ImmutableList<String> partitionQualifiers;

    // To be checked if even needed
    @Getter
    @Serialize
    public long partitionGroupId;
    @Serialize
    public long entityId;
    @Serialize
    public long namespaceId;
    @Serialize
    public boolean isUnbound;


    public AllocationPartition(
            @Deserialize("id") final long id,
            @Deserialize("entityId") final long entityId,
            @Deserialize("namespaceId") final long namespaceId,
            @Deserialize("partitionQualifiers") @Nullable final List<String> partitionQualifiers,
            @Deserialize("isUnbound") final boolean isUnbound,
            @Deserialize("partitionGroupId") final long partitionGroupId ) {
        this.id = id;
        this.entityId = entityId;
        this.namespaceId = namespaceId;
        this.partitionQualifiers = partitionQualifiers == null ? null : ImmutableList.copyOf( partitionQualifiers );
        this.isUnbound = isUnbound;
        this.partitionGroupId = partitionGroupId;
    }


    @Override
    public Serializable[] getParameterArray() {
        throw new RuntimeException( "Not implemented" );
    }

}