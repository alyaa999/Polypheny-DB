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

package org.polypheny.db.catalog.impl.logical;

import io.activej.serializer.BinarySerializer;
import io.activej.serializer.annotations.Deserialize;
import io.activej.serializer.annotations.Serialize;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;
import org.polypheny.db.catalog.IdBuilder;
import org.polypheny.db.catalog.catalogs.LogicalCatalog;
import org.polypheny.db.catalog.catalogs.LogicalDocumentCatalog;
import org.polypheny.db.catalog.entity.logical.LogicalCollection;
import org.polypheny.db.catalog.entity.logical.LogicalNamespace;
import org.polypheny.db.catalog.logistic.EntityType;
import org.polypheny.db.type.PolySerializable;

@Value
@SuperBuilder(toBuilder = true)
public class DocumentCatalog implements PolySerializable, LogicalDocumentCatalog {

    @Getter
    public BinarySerializer<DocumentCatalog> serializer = PolySerializable.builder.get().build( DocumentCatalog.class );

    IdBuilder idBuilder = IdBuilder.getInstance();
    @Serialize
    @Getter
    public Map<Long, LogicalCollection> collections;
    @Getter
    @Serialize
    public LogicalNamespace logicalNamespace;


    public DocumentCatalog( LogicalNamespace logicalNamespace ) {
        this( logicalNamespace, new ConcurrentHashMap<>() );
    }


    public DocumentCatalog(
            @Deserialize("logicalNamespace") LogicalNamespace logicalNamespace,
            @Deserialize("collections") Map<Long, LogicalCollection> collections ) {
        this.logicalNamespace = logicalNamespace;
        this.collections = new ConcurrentHashMap<>( collections );

    }


    @NonFinal
    @Builder.Default
    boolean openChanges = false;


    @Override
    public PolySerializable copy() {
        return PolySerializable.deserialize( serialize(), DocumentCatalog.class );
    }


    @Override
    public LogicalCollection addCollection( String name, EntityType entity, boolean modifiable ) {
        long id = idBuilder.getNewLogicalId();
        LogicalCollection collection = new LogicalCollection( id, name, logicalNamespace.id, entity, modifiable );
        collections.put( id, collection );
        return collection;
    }


    @Override
    public void deleteCollection( long id ) {
        collections.remove( id );
    }


    @Override
    public LogicalCatalog withLogicalNamespace( LogicalNamespace namespace ) {
        return toBuilder().logicalNamespace( namespace ).build();
    }

}