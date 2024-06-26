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

package org.polypheny.db.adapter.geode.util;


import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.avatica.util.DateTimeUtils;
import org.apache.calcite.linq4j.tree.Primitive;
import org.apache.commons.lang3.StringUtils;
import org.apache.geode.cache.CacheClosedException;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.query.Struct;
import org.apache.geode.pdx.PdxInstance;
import org.apache.geode.pdx.ReflectionBasedAutoSerializer;
import org.polypheny.db.algebra.type.AlgDataType;
import org.polypheny.db.algebra.type.AlgDataTypeField;
import org.polypheny.db.util.Util;


/**
 * Utilities for the Geode adapter.
 */
@Slf4j
public class GeodeUtils {

    /**
     * Cache for the client proxy regions created in the current ClientCache.
     */
    private static final Map<String, Region> REGION_MAP = new ConcurrentHashMap<>();

    private static String currentLocatorHost = "";
    private static int currentLocatorPort = -1;

    private static final JavaTypeFactoryExtImpl JAVA_TYPE_FACTORY = new JavaTypeFactoryExtImpl();


    private GeodeUtils() {
    }


    /**
     * Creates a Geode client instance connected to locator and configured to support PDX instances.
     *
     * If an old instance exists, it will be destroyed and re-created.
     *
     * @param locatorHost Locator's host address
     * @param locatorPort Locator's port
     * @param autoSerializerPackagePath package name of the Domain classes loaded in the regions
     * @return Returns a Geode {@link ClientCache} instance connected to Geode cluster
     */
    public static synchronized ClientCache createClientCache( String locatorHost, int locatorPort, String autoSerializerPackagePath, boolean readSerialized ) {
        if ( locatorPort != currentLocatorPort || !StringUtils.equalsIgnoreCase( currentLocatorHost, locatorHost ) ) {
            log.info( "Close existing ClientCache [{}:{}] for new Locator connection at: [{}:{}]", currentLocatorHost, currentLocatorPort, locatorHost, locatorPort );
            currentLocatorHost = locatorHost;
            currentLocatorPort = locatorPort;
            closeClientCache();
        }

        try {
            // If exists returns the existing client cache. This requires that the pre-created client proxy regions can also be resolved from the regionMap
            return ClientCacheFactory.getAnyInstance();
        } catch ( CacheClosedException cce ) {
            // Do nothing if there is no existing instance
        }

        return new ClientCacheFactory()
                .addPoolLocator( locatorHost, locatorPort )
                .setPdxSerializer( new ReflectionBasedAutoSerializer( autoSerializerPackagePath ) )
                .setPdxReadSerialized( readSerialized )
                .setPdxPersistent( false )
                .create();
    }


    public static synchronized void closeClientCache() {
        try {
            ClientCacheFactory.getAnyInstance().close();
        } catch ( CacheClosedException cce ) {
            // Do nothing if there is no existing instance
        }
        REGION_MAP.clear();
    }


    /**
     * Obtains a proxy pointing to an existing Region on the server
     *
     * @param cache {@link GemFireCache} instance to interact with the Geode server
     * @param regionName Name of the region to create proxy for.
     * @return Returns a Region proxy to a remote (on the Server) regions.
     */
    public static synchronized Region createRegion( GemFireCache cache, String regionName ) {
        Objects.requireNonNull( cache, "cache" );
        Objects.requireNonNull( regionName, "regionName" );
        Region region = REGION_MAP.get( regionName );
        if ( region == null ) {
            try {
                region = ((ClientCache) cache).createClientRegionFactory( ClientRegionShortcut.PROXY ).create( regionName );
            } catch ( IllegalStateException e ) {
                // means this is a server cache (probably part of embedded testing)
                region = cache.getRegion( regionName );
            }

            REGION_MAP.put( regionName, region );
        }

        return region;
    }


    /**
     * Converts a Geode object into a Row tuple.
     *
     * @param algDataTypeFields Table relation types
     * @param geodeResultObject Object value returned by Geode query
     * @return List of objects values corresponding to the relDataTypeFields
     */
    public static Object convertToRowValues( List<AlgDataTypeField> algDataTypeFields, Object geodeResultObject ) {
        Object values;
        if ( geodeResultObject instanceof Struct ) {
            values = handleStructEntry( algDataTypeFields, geodeResultObject );
        } else if ( geodeResultObject instanceof PdxInstance ) {
            values = handlePdxInstanceEntry( algDataTypeFields, geodeResultObject );
        } else {
            values = handleJavaObjectEntry( algDataTypeFields, geodeResultObject );
        }

        return values;
    }


    private static Object handleStructEntry( List<AlgDataTypeField> algDataTypeFields, Object obj ) {
        Struct struct = (Struct) obj;
        Object[] values = new Object[algDataTypeFields.size()];

        int index = 0;
        for ( AlgDataTypeField algDataTypeField : algDataTypeFields ) {
            Type javaType = JAVA_TYPE_FACTORY.getJavaClass( algDataTypeField.getType() );
            Object rawValue;
            try {
                rawValue = struct.get( algDataTypeField.getName() );
            } catch ( IllegalArgumentException e ) {
                rawValue = "<error>";
                System.err.println( "Could find field : " + algDataTypeField.getName() );
                log.error( "Caught exception", e );
            }
            values[index++] = convert( rawValue, (Class) javaType );
        }

        if ( values.length == 1 ) {
            return values[0];
        }

        return values;
    }


    private static Object handlePdxInstanceEntry( List<AlgDataTypeField> algDataTypeFields, Object obj ) {
        PdxInstance pdxEntry = (PdxInstance) obj;

        Object[] values = new Object[algDataTypeFields.size()];

        int index = 0;
        for ( AlgDataTypeField algDataTypeField : algDataTypeFields ) {
            Type javaType = JAVA_TYPE_FACTORY.getJavaClass( algDataTypeField.getType() );
            Object rawValue = pdxEntry.getField( algDataTypeField.getName() );
            values[index++] = convert( rawValue, (Class) javaType );
        }

        if ( values.length == 1 ) {
            return values[0];
        }

        return values;
    }


    private static Object handleJavaObjectEntry( List<AlgDataTypeField> algDataTypeFields, Object obj ) {

        Class<?> clazz = obj.getClass();
        if ( algDataTypeFields.size() == 1 ) {
            try {
                Field javaField = clazz.getDeclaredField( algDataTypeFields.get( 0 ).getName() );
                javaField.setAccessible( true );
                return javaField.get( obj );
            } catch ( Exception e ) {
                log.error( "Caught exception", e );
            }
            return null;
        }

        Object[] values = new Object[algDataTypeFields.size()];

        int index = 0;
        for ( AlgDataTypeField algDataTypeField : algDataTypeFields ) {
            try {
                Field javaField = clazz.getDeclaredField( algDataTypeField.getName() );
                javaField.setAccessible( true );
                values[index++] = javaField.get( obj );
            } catch ( Exception e ) {
                log.error( "Caught exception", e );
            }
        }
        return values;
    }


    private static Object convert( Object o, Class clazz ) {
        if ( o == null ) {
            return null;
        }
        Primitive primitive = Primitive.of( clazz );
        if ( primitive != null ) {
            clazz = primitive.boxClass;
        } else {
            primitive = Primitive.ofBox( clazz );
        }
        if ( clazz == null ) {
            // This is in case of nested Objects!
            if ( o instanceof PdxInstance ) {
                return Util.toString( ((PdxInstance) o).getFieldNames(), "PDX[", ",", "]" );
            }
            return o.toString();
        }
        if ( clazz.isInstance( o ) ) {
            return o;
        }
        if ( o instanceof Date && primitive != null ) {
            o = ((Date) o).getTime() / DateTimeUtils.MILLIS_PER_DAY;
        }
        if ( o instanceof Number && primitive != null ) {
            return primitive.number( (Number) o );
        }
        return o;
    }


    /**
     * Extract the first entity of each Regions and use it to build a table types.
     *
     * @param region existing region
     * @return derived data type.
     */
    public static AlgDataType autodetectRelTypeFromRegion( Region<?, ?> region ) {
        Objects.requireNonNull( region, "region" );

        // try to detect type using value constraints (if they exists)
        final Class<?> constraint = region.getAttributes().getValueConstraint();
        if ( constraint != null && !PdxInstance.class.isAssignableFrom( constraint ) ) {
            return new JavaTypeFactoryExtImpl().createStructType( constraint );
        }

        final Iterator<?> iter;
        if ( region.getAttributes().getPoolName() == null ) {
            // means current cache is server (not ClientCache)
            iter = region.keySet().iterator();
        } else {
            // for ClientCache
            iter = region.keySetOnServer().iterator();
        }

        if ( !iter.hasNext() ) {
            String message = String.format( Locale.ROOT, "Region %s is empty, can't autodetect type(s)", region.getName() );
            throw new IllegalStateException( message );
        }

        final Object entry = region.get( iter.next() );
        return createRelDataType( entry );
    }


    // Create Relational Type by inferring a Geode entry or response instance.
    private static AlgDataType createRelDataType( Object regionEntry ) {
        JavaTypeFactoryExtImpl typeFactory = new JavaTypeFactoryExtImpl();
        if ( regionEntry instanceof PdxInstance ) {
            return typeFactory.createPdxType( (PdxInstance) regionEntry );
        } else {
            return typeFactory.createStructType( regionEntry.getClass() );
        }
    }

}
