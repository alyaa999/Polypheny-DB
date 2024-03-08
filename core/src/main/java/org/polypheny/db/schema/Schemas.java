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

package org.polypheny.db.schema;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.lang.reflect.Type;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Queryable;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.linq4j.tree.Expressions;
import org.apache.commons.lang3.NotImplementedException;
import org.polypheny.db.adapter.DataContext;
import org.polypheny.db.adapter.java.JavaTypeFactory;
import org.polypheny.db.algebra.type.AlgDataType;
import org.polypheny.db.algebra.type.AlgDataTypeFactory;
import org.polypheny.db.algebra.type.AlgProtoDataType;
import org.polypheny.db.catalog.Catalog;
import org.polypheny.db.catalog.catalogs.AdapterCatalog;
import org.polypheny.db.catalog.catalogs.RelAdapterCatalog;
import org.polypheny.db.catalog.entity.logical.LogicalTable;
import org.polypheny.db.catalog.snapshot.Snapshot;
import org.polypheny.db.config.PolyphenyDbConnectionConfig;
import org.polypheny.db.config.PolyphenyDbConnectionConfigImpl;
import org.polypheny.db.config.PolyphenyDbConnectionProperty;
import org.polypheny.db.interpreter.Row;
import org.polypheny.db.prepare.Context;
import org.polypheny.db.prepare.PolyphenyDbPrepare;
import org.polypheny.db.schema.types.FilterableEntity;
import org.polypheny.db.schema.types.ProjectableFilterableEntity;
import org.polypheny.db.schema.types.QueryableEntity;
import org.polypheny.db.schema.types.ScannableEntity;
import org.polypheny.db.transaction.Statement;
import org.polypheny.db.type.PolyTypeUtil;
import org.polypheny.db.type.entity.PolyValue;
import org.polypheny.db.util.BuiltInMethod;
import org.polypheny.db.util.Pair;


/**
 * Utility functions for schemas.
 */
@Slf4j
@Deprecated
public final class Schemas {

    private Schemas() {
        throw new AssertionError( "no instances!" );
    }


    private static boolean matches( AlgDataTypeFactory typeFactory, Function member, List<AlgDataType> argumentTypes ) {
        List<FunctionParameter> parameters = member.getParameters();
        if ( parameters.size() != argumentTypes.size() ) {
            return false;
        }
        for ( int i = 0; i < argumentTypes.size(); i++ ) {
            AlgDataType argumentType = argumentTypes.get( i );
            FunctionParameter parameter = parameters.get( i );
            if ( !canConvert( argumentType, parameter.getType( typeFactory ) ) ) {
                return false;
            }
        }
        return true;
    }


    private static boolean canConvert( AlgDataType fromType, AlgDataType toType ) {
        return PolyTypeUtil.canAssignFrom( toType, fromType );
    }


    /**
     * Returns the expression for a schema.
     */
    public static Expression expression( AdapterCatalog snapshot ) {
        return snapshot.asExpression();
    }


    /**
     * Returns the expression for a sub-schema.
     */
    public static Expression subSchemaExpression( AdapterCatalog snapshot, long id, Long adapterId, Class<?> type ) {
        // (Type) schemaExpression.getSubSchema("name")
        /*final Expression schemaExpression = expression( snapshot );
        Expression call =
                Expressions.call(
                        schemaExpression,
                        BuiltInMethod.SNAPSHOT_GET_NAMESPACE.method,
                        Expressions.constant( id ) );
        if ( type != null && !type.isAssignableFrom( Namespace.class ) ) {
            return unwrap( call, type );
        }
        return call;*/
        log.warn( "should not longer be used" );
        return null;
    }


    /**
     * Converts a schema expression to a given type by calling the {@link SchemaPlus#unwrap(Class)} method.
     */
    public static Expression unwrap( Expression call, Class<?> type ) {
        return Expressions.convert_( Expressions.call( call, BuiltInMethod.SCHEMA_PLUS_UNWRAP.method, Expressions.constant( type ) ), type );
    }


    /**
     * Returns the expression to access a table within a schema.
     */
    public static Expression tableExpression( RelAdapterCatalog snapshot, Type elementType, String tableName, Class<?> clazz ) {
        /*final MethodCallExpression expression;
        if ( Entity.class.isAssignableFrom( clazz ) ) {
            expression = Expressions.call(
                    expression( snapshot ),
                    BuiltInMethod.SCHEMA_GET_TABLE.method,
                    Expressions.constant( tableName ) );
            if ( ScannableEntity.class.isAssignableFrom( clazz ) ) {
                return Expressions.call(
                        BuiltInMethod.SCHEMAS_ENUMERABLE_SCANNABLE.method,
                        Expressions.convert_( expression, ScannableEntity.class ),
                        DataContext.ROOT );
            }
            if ( FilterableEntity.class.isAssignableFrom( clazz ) ) {
                return Expressions.call(
                        BuiltInMethod.SCHEMAS_ENUMERABLE_FILTERABLE.method,
                        Expressions.convert_( expression, FilterableEntity.class ),
                        DataContext.ROOT );
            }
            if ( ProjectableFilterableEntity.class.isAssignableFrom( clazz ) ) {
                return Expressions.call(
                        BuiltInMethod.SCHEMAS_ENUMERABLE_PROJECTABLE_FILTERABLE.method,
                        Expressions.convert_( expression, ProjectableFilterableEntity.class ),
                        DataContext.ROOT );
            }
        } else {
            expression = Expressions.call(
                    BuiltInMethod.SCHEMAS_QUERYABLE.method,
                    DataContext.ROOT,
                    expression( snapshot ),
                    Expressions.constant( elementType ),
                    Expressions.constant( tableName ) );
        }
        return Types.castIfNecessary( clazz, expression );*/
        return null;
    }


    public static DataContext createDataContext( Snapshot snapshot ) {
        return new DummyDataContext( snapshot );
    }


    /**
     * Returns a {@link Queryable}, given a fully-qualified table name.
     */
    public static Queryable<PolyValue[]> queryable( DataContext root, Class<PolyValue> clazz, String... names ) {
        return queryable( root, Arrays.asList( names ) );
    }


    /**
     * Returns a {@link Queryable}, given a fully-qualified table name as an iterable.
     */
    public static Queryable<PolyValue[]> queryable( DataContext root, Iterable<? extends String> names ) {
        Snapshot snapshot = root.getSnapshot();

        return queryable( root, snapshot, names.iterator().next() );

    }


    public static Enumerable<Row<PolyValue>> queryableRow( DataContext root, Class<Object> clazz, List<String> names ) {
        Snapshot snapshot = root.getSnapshot();

        throw new NotImplementedException();
        //return queryable( root, snapshot, names.iterator().next() );
    }


    /**
     * Returns a {@link Queryable}, given a schema and entity name.
     */
    public static Queryable<PolyValue[]> queryable( DataContext root, Snapshot snapshot, String entityName ) {
        //QueryableEntity table = (QueryableEntity) schema.getEntity( tableName );
        LogicalTable table = snapshot.rel().getTable( null, entityName ).orElseThrow();
        return table.unwrap( QueryableEntity.class ).orElseThrow().asQueryable( root, snapshot );
    }


    /**
     * Returns an {@link org.apache.calcite.linq4j.Enumerable} over the rows of a given table, representing each row as an object array.
     */
    public static Enumerable<PolyValue[]> enumerable( final ScannableEntity table, final DataContext root ) {
        return table.scan( root );
    }


    /**
     * Returns an {@link org.apache.calcite.linq4j.Enumerable} over the rows of a given table, not applying any filters, representing each row as an object array.
     */
    public static Enumerable<PolyValue[]> enumerable( final FilterableEntity table, final DataContext root ) {
        return table.scan( root, ImmutableList.of() );
    }


    /**
     * Returns an {@link org.apache.calcite.linq4j.Enumerable} over the rows of a given table, not applying any filters and projecting all columns, representing each row as an object array.
     */
    public static Enumerable<PolyValue[]> enumerable( final ProjectableFilterableEntity table, final DataContext root ) {
        return table.scan( root, ImmutableList.of(), identity( table.getTupleType( root.getTypeFactory() ).getFieldCount() ) );
    }


    private static int[] identity( int count ) {
        final int[] integers = new int[count];
        for ( int i = 0; i < integers.length; i++ ) {
            integers[i] = i;
        }
        return integers;
    }


    /**
     * Creates a context for the purposes of preparing a statement.
     *
     * @param schemaPath Path wherein to look for functions
     * @param objectPath Path of the object being analyzed (usually a view), or null
     * @param propValues Connection properties
     * @return Context
     */
    private static Context makeContext( Snapshot snapshot, List<String> schemaPath, List<String> objectPath, final ImmutableMap<PolyphenyDbConnectionProperty, String> propValues ) {
        final Context context0 = PolyphenyDbPrepare.Dummy.peek();
        final PolyphenyDbConnectionConfig config = mutate( context0.config(), propValues );
        return makeContext( config, context0.getTypeFactory(), context0.getDataContext(), snapshot, schemaPath, objectPath );
    }


    private static PolyphenyDbConnectionConfig mutate( PolyphenyDbConnectionConfig config, ImmutableMap<PolyphenyDbConnectionProperty, String> propValues ) {
        for ( Map.Entry<PolyphenyDbConnectionProperty, String> e : propValues.entrySet() ) {
            config = ((PolyphenyDbConnectionConfigImpl) config).set( e.getKey(), e.getValue() );
        }
        return config;
    }


    private static Context makeContext(
            final PolyphenyDbConnectionConfig connectionConfig,
            final JavaTypeFactory typeFactory,
            final DataContext dataContext,
            final Snapshot shot,
            final List<String> schemaPath,
            final List<String> objectPath_ ) {
        final ImmutableList<String> objectPath = objectPath_ == null ? null : ImmutableList.copyOf( objectPath_ );
        return new Context() {

            Snapshot snapshot = shot;


            @Override
            public JavaTypeFactory getTypeFactory() {
                return typeFactory;
            }


            @Override
            public Snapshot getSnapshot() {
                return snapshot;
            }


            @Override
            public String getDefaultNamespaceName() {
                return null;
            }


            @Override
            public void updateSnapshot() {
                snapshot = Catalog.snapshot();
            }


            @Override
            public List<String> getObjectPath() {
                return objectPath;
            }


            @Override
            public PolyphenyDbConnectionConfig config() {
                return connectionConfig;
            }


            @Override
            public DataContext getDataContext() {
                return dataContext;
            }


            @Override
            public Statement getStatement() {
                return null;
            }


        };
    }


    /**
     * Returns an implementation of {@link AlgProtoDataType} that asks a given table for its row type with a given type factory.
     */
    public static AlgProtoDataType proto( final Entity entity ) {
        return entity::getTupleType;
    }


    /**
     * Returns an implementation of {@link AlgProtoDataType} that asks a given scalar function for its return type with a given type factory.
     */
    public static AlgProtoDataType proto( final ScalarFunction function ) {
        return function::getReturnType;
    }


    /**
     * Returns a sub-schema of a given schema obtained by following a sequence of names.
     *
     * The result is null if the initial schema is null or any sub-schema does not exist.
     */
    public static Snapshot subSchema( Snapshot snapshot, Iterable<String> names ) {
        for ( String string : names ) {
            if ( snapshot == null ) {
                return null;
            }
        }
        return snapshot;
    }


    public static PathImpl path( ImmutableList<Pair<String, Namespace>> build ) {
        return new PathImpl( build );
    }


    /**
     * Returns the path to get to a schema from its root.
     */
    public static Path path( SchemaPlus schema ) {
        List<Pair<String, Namespace>> list = new ArrayList<>();
        for ( SchemaPlus s = schema; s != null; s = s.getParentSchema() ) {
            list.add( Pair.of( s.getName(), s ) );
        }
        return new PathImpl( ImmutableList.copyOf( Lists.reverse( list ) ) );
    }


    /**
     * Implementation of {@link Path}.
     */
    private static class PathImpl extends AbstractList<Pair<String, Namespace>> implements Path {

        private final ImmutableList<Pair<String, Namespace>> pairs;

        private static final PathImpl EMPTY = new PathImpl( ImmutableList.of() );


        PathImpl( ImmutableList<Pair<String, Namespace>> pairs ) {
            this.pairs = pairs;
        }


        @Override
        public boolean equals( Object o ) {
            return this == o
                    || o instanceof PathImpl
                    && pairs.equals( ((PathImpl) o).pairs );
        }


        @Override
        public int hashCode() {
            return pairs.hashCode();
        }


        @Override
        public Pair<String, Namespace> get( int index ) {
            return pairs.get( index );
        }


        @Override
        public int size() {
            return pairs.size();
        }


        @Override
        public Path parent() {
            if ( pairs.isEmpty() ) {
                throw new IllegalArgumentException( "at root" );
            }
            return new PathImpl( pairs.subList( 0, pairs.size() - 1 ) );
        }


        @Override
        public List<String> names() {
            return new AbstractList<String>() {
                @Override
                public String get( int index ) {
                    return pairs.get( index + 1 ).left;
                }


                @Override
                public int size() {
                    return pairs.size() - 1;
                }
            };
        }


        @Override
        public List<Namespace> schemas() {
            return Pair.right( pairs );
        }

    }

}
