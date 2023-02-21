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

package org.polypheny.db.adapter.druid;


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.polypheny.db.schema.Entity;
import org.polypheny.db.schema.Namespace.Schema;
import org.polypheny.db.schema.impl.AbstractNamespace;
import org.polypheny.db.type.PolyType;


/**
 * Schema mapped onto a Druid instance.
 */
public class DruidSchema extends AbstractNamespace implements Schema {

    final String url;
    final String coordinatorUrl;
    private final boolean discoverTables;
    private Map<String, Entity> tableMap = null;


    /**
     * Creates a Druid schema.
     *
     * @param url URL of query REST service, e.g. "http://localhost:8082"
     * @param coordinatorUrl URL of coordinator REST service, e.g. "http://localhost:8081"
     * @param discoverTables If true, ask Druid what tables exist; if false, only create tables explicitly in the model
     */
    public DruidSchema( long id, String url, String coordinatorUrl, boolean discoverTables ) {
        super( id );
        this.url = Objects.requireNonNull( url );
        this.coordinatorUrl = Objects.requireNonNull( coordinatorUrl );
        this.discoverTables = discoverTables;
    }


    @Override
    protected Map<String, Entity> getTableMap() {
        if ( !discoverTables ) {
            return ImmutableMap.of();
        }

        if ( tableMap == null ) {
            final DruidConnectionImpl connection = new DruidConnectionImpl( url, coordinatorUrl );
            Set<String> tableNames = connection.tableNames();

            tableMap = Maps.asMap( ImmutableSet.copyOf( tableNames ), CacheBuilder.newBuilder().build( CacheLoader.from( name -> table( name, connection ) ) ) );
        }

        return tableMap;
    }


    private Entity table( String tableName, DruidConnectionImpl connection ) {
        final Map<String, PolyType> fieldMap = new LinkedHashMap<>();
        final Set<String> metricNameSet = new LinkedHashSet<>();
        final Map<String, List<ComplexMetric>> complexMetrics = new HashMap<>();

        connection.metadata( tableName, DruidEntity.DEFAULT_TIMESTAMP_COLUMN, null, fieldMap, metricNameSet, complexMetrics );

        return DruidEntity.create( DruidSchema.this, tableName, null, fieldMap, metricNameSet, DruidEntity.DEFAULT_TIMESTAMP_COLUMN, complexMetrics );
    }

}
