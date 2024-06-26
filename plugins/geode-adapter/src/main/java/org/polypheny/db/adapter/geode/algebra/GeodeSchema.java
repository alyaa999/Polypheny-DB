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
 *
 * This file incorporates code covered by the following terms:
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.polypheny.db.adapter.geode.algebra;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.polypheny.db.adapter.geode.util.GeodeUtils;
import org.polypheny.db.schema.Table;
import org.polypheny.db.schema.impl.AbstractSchema;


/**
 * Schema mapped onto a Geode Region.
 */
public class GeodeSchema extends AbstractSchema {

    final GemFireCache cache;
    private final List<String> regionNames;
    private ImmutableMap<String, Table> tableMap;


    GeodeSchema( String locatorHost, int locatorPort, Iterable<String> regionNames, String pdxAutoSerializerPackageExp ) {
        this( GeodeUtils.createClientCache( locatorHost, locatorPort, pdxAutoSerializerPackageExp, true ), regionNames );
    }


    GeodeSchema( final GemFireCache cache, final Iterable<String> regionNames ) {
        super();
        this.cache = Objects.requireNonNull( cache, "clientCache" );
        this.regionNames = ImmutableList.copyOf( Objects.requireNonNull( regionNames, "regionNames" ) );
    }


    @Override
    protected Map<String, Table> getTableMap() {
        if ( tableMap == null ) {
            final ImmutableMap.Builder<String, Table> builder = ImmutableMap.builder();
            for ( String regionName : regionNames ) {
                Region region = GeodeUtils.createRegion( cache, regionName );
                Table table = new GeodeTable( region );
                builder.put( regionName, table );
            }
            tableMap = builder.build();
        }
        return tableMap;
    }

}

