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

package org.polypheny.db.processing;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.avatica.MetaImpl;
import org.apache.calcite.linq4j.Enumerable;
import org.jetbrains.annotations.NotNull;
import org.polypheny.db.PolyImplementation;
import org.polypheny.db.algebra.AlgNode;
import org.polypheny.db.algebra.AlgRoot;
import org.polypheny.db.algebra.AlgStructuredTypeFlattener;
import org.polypheny.db.algebra.constant.Kind;
import org.polypheny.db.algebra.core.common.Modify;
import org.polypheny.db.algebra.logical.lpg.LogicalLpgModify;
import org.polypheny.db.algebra.logical.lpg.LogicalLpgScan;
import org.polypheny.db.algebra.logical.lpg.LogicalLpgValues;
import org.polypheny.db.algebra.logical.relational.LogicalValues;
import org.polypheny.db.algebra.type.AlgDataTypeFactory;
import org.polypheny.db.algebra.type.AlgDataTypeField;
import org.polypheny.db.algebra.type.AlgDataTypeFieldImpl;
import org.polypheny.db.algebra.type.AlgDataTypeSystem;
import org.polypheny.db.algebra.type.AlgRecordType;
import org.polypheny.db.catalog.Catalog;
import org.polypheny.db.catalog.entity.AllocationColumn;
import org.polypheny.db.catalog.entity.CatalogAdapter;
import org.polypheny.db.catalog.entity.CatalogPrimaryKey;
import org.polypheny.db.catalog.entity.logical.LogicalColumn;
import org.polypheny.db.catalog.entity.logical.LogicalGraph;
import org.polypheny.db.catalog.entity.logical.LogicalTable;
import org.polypheny.db.catalog.entity.physical.PhysicalTable;
import org.polypheny.db.catalog.refactor.ModifiableEntity;
import org.polypheny.db.catalog.snapshot.AllocSnapshot;
import org.polypheny.db.catalog.snapshot.LogicalRelSnapshot;
import org.polypheny.db.catalog.snapshot.Snapshot;
import org.polypheny.db.config.RuntimeConfig;
import org.polypheny.db.partition.PartitionManager;
import org.polypheny.db.partition.PartitionManagerFactory;
import org.polypheny.db.partition.properties.PartitionProperty;
import org.polypheny.db.plan.AlgOptCluster;
import org.polypheny.db.rex.RexBuilder;
import org.polypheny.db.rex.RexDynamicParam;
import org.polypheny.db.rex.RexNode;
import org.polypheny.db.routing.RoutingManager;
import org.polypheny.db.schema.ModelTrait;
import org.polypheny.db.schema.graph.PolyGraph;
import org.polypheny.db.tools.AlgBuilder;
import org.polypheny.db.transaction.Statement;
import org.polypheny.db.transaction.Transaction;
import org.polypheny.db.type.PolyType;
import org.polypheny.db.type.PolyTypeFactoryImpl;
import org.polypheny.db.util.LimitIterator;


@Slf4j
public class DataMigratorImpl implements DataMigrator {

    @Override
    public void copyGraphData( LogicalGraph target, Transaction transaction, Long existingAdapterId, CatalogAdapter to ) {
        Statement statement = transaction.createStatement();

        AlgBuilder builder = AlgBuilder.create( statement );

        LogicalLpgScan scan = (LogicalLpgScan) builder.lpgScan( target.id ).build();

        AlgNode routed = RoutingManager.getInstance().getFallbackRouter().handleGraphScan( scan, statement, existingAdapterId );

        AlgRoot algRoot = AlgRoot.of( routed, Kind.SELECT );

        AlgStructuredTypeFlattener typeFlattener = new AlgStructuredTypeFlattener(
                AlgBuilder.create( statement, algRoot.alg.getCluster() ),
                algRoot.alg.getCluster().getRexBuilder(),
                algRoot.alg::getCluster,
                true );
        algRoot = algRoot.withAlg( typeFlattener.rewrite( algRoot.alg ) );

        PolyImplementation result = statement.getQueryProcessor().prepareQuery(
                algRoot,
                algRoot.alg.getCluster().getTypeFactory().builder().build(),
                true,
                false,
                false );

        final Enumerable<Object> enumerable = result.enumerable( statement.getDataContext() );

        Iterator<Object> sourceIterator = enumerable.iterator();

        if ( sourceIterator.hasNext() ) {
            PolyGraph graph = (PolyGraph) sourceIterator.next();

            if ( graph.getEdges().isEmpty() && graph.getNodes().isEmpty() ) {
                // nothing to copy
                return;
            }

            // we have a new statement
            statement = transaction.createStatement();
            builder = AlgBuilder.create( statement );

            LogicalLpgValues values = getLogicalLpgValues( builder, graph );

            LogicalLpgModify modify = new LogicalLpgModify( builder.getCluster(), builder.getCluster().traitSetOf( ModelTrait.GRAPH ), target, values, Modify.Operation.INSERT, null, null );

            AlgNode routedModify = RoutingManager.getInstance().getDmlRouter().routeGraphDml( modify, statement, target, List.of( to.id ) );

            result = statement.getQueryProcessor().prepareQuery(
                    AlgRoot.of( routedModify, Kind.SELECT ),
                    routedModify.getCluster().getTypeFactory().builder().build(),
                    true,
                    false,
                    false );

            final Enumerable<Object> modifyEnumerable = result.enumerable( statement.getDataContext() );

            Iterator<Object> modifyIterator = modifyEnumerable.iterator();
            if ( modifyIterator.hasNext() ) {
                modifyIterator.next();
            }

        }


    }


    @NotNull
    private static LogicalLpgValues getLogicalLpgValues( AlgBuilder builder, PolyGraph graph ) {
        List<AlgDataTypeField> fields = new ArrayList<>();
        int index = 0;
        if ( !graph.getNodes().isEmpty() ) {
            fields.add( new AlgDataTypeFieldImpl( "n", index, builder.getTypeFactory().createPolyType( PolyType.NODE ) ) );
            index++;
        }
        if ( !graph.getEdges().isEmpty() ) {
            fields.add( new AlgDataTypeFieldImpl( "e", index, builder.getTypeFactory().createPolyType( PolyType.EDGE ) ) );
        }

        return new LogicalLpgValues( builder.getCluster(), builder.getCluster().traitSetOf( ModelTrait.GRAPH ), graph.getNodes().values(), graph.getEdges().values(), ImmutableList.of(), new AlgRecordType( fields ) );
    }


    @Override
    public void copyData( Transaction transaction, CatalogAdapter store, List<LogicalColumn> columns, List<Long> partitionIds ) {
        Snapshot snapshot = Catalog.getInstance().getSnapshot();
        LogicalRelSnapshot relSnapshot = snapshot.rel();
        LogicalTable table = relSnapshot.getTable( columns.get( 0 ).tableId );
        CatalogPrimaryKey primaryKey = relSnapshot.getPrimaryKey( table.primaryKey );

        // Check Lists
        List<AllocationColumn> targetColumnPlacements = new LinkedList<>();
        for ( LogicalColumn logicalColumn : columns ) {
            targetColumnPlacements.add( Catalog.getInstance().getSnapshot().alloc().getColumnPlacement( store.id, logicalColumn.id ) );
        }

        List<LogicalColumn> selectColumnList = new LinkedList<>( columns );

        // Add primary keys to select column list
        for ( long cid : primaryKey.columnIds ) {
            LogicalColumn logicalColumn = relSnapshot.getColumn( cid );
            if ( !selectColumnList.contains( logicalColumn ) ) {
                selectColumnList.add( logicalColumn );
            }
        }

        // We need a columnPlacement for every partition
        Map<Long, List<AllocationColumn>> placementDistribution = new HashMap<>();
        PartitionProperty property = snapshot.alloc().getPartitionProperty( table.id );
        if ( property.isPartitioned ) {
            PartitionManagerFactory partitionManagerFactory = PartitionManagerFactory.getInstance();
            PartitionManager partitionManager = partitionManagerFactory.getPartitionManager( property.partitionType );
            placementDistribution = partitionManager.getRelevantPlacements( table, partitionIds, Collections.singletonList( store.id ) );
        } else {
            placementDistribution.put(
                    property.partitionIds.get( 0 ),
                    selectSourcePlacements( table, selectColumnList, targetColumnPlacements.get( 0 ).adapterId ) );
        }

        for ( long partitionId : partitionIds ) {
            Statement sourceStatement = transaction.createStatement();
            Statement targetStatement = transaction.createStatement();

            Map<Long, List<AllocationColumn>> subDistribution = new HashMap<>( placementDistribution );
            subDistribution.keySet().retainAll( List.of( partitionId ) );
            AlgRoot sourceAlg = getSourceIterator( sourceStatement, subDistribution );
            AlgRoot targetAlg;
            if ( Catalog.getInstance().getSnapshot().alloc().getColumnPlacementsOnAdapterPerTable( store.id, table.id ).size() == columns.size() ) {
                // There have been no placements for this table on this store before. Build insert statement
                targetAlg = buildInsertStatement( targetStatement, targetColumnPlacements, partitionId );
            } else {
                // Build update statement
                targetAlg = buildUpdateStatement( targetStatement, targetColumnPlacements, partitionId );
            }

            // Execute Query
            executeQuery( selectColumnList, sourceAlg, sourceStatement, targetStatement, targetAlg, false, false );
        }
    }


    @Override
    public void executeQuery( List<LogicalColumn> selectColumnList, AlgRoot sourceAlg, Statement sourceStatement, Statement targetStatement, AlgRoot targetAlg, boolean isMaterializedView, boolean doesSubstituteOrderBy ) {
        try {
            PolyImplementation result;
            if ( isMaterializedView ) {
                result = sourceStatement.getQueryProcessor().prepareQuery(
                        sourceAlg,
                        sourceAlg.alg.getCluster().getTypeFactory().builder().build(),
                        false,
                        false,
                        doesSubstituteOrderBy );
            } else {
                result = sourceStatement.getQueryProcessor().prepareQuery(
                        sourceAlg,
                        sourceAlg.alg.getCluster().getTypeFactory().builder().build(),
                        true,
                        false,
                        false );
            }
            final Enumerable<Object> enumerable = result.enumerable( sourceStatement.getDataContext() );
            //noinspection unchecked
            Iterator<Object> sourceIterator = enumerable.iterator();

            Map<Long, Integer> resultColMapping = new HashMap<>();
            for ( LogicalColumn logicalColumn : selectColumnList ) {
                int i = 0;
                for ( AlgDataTypeField metaData : result.getRowType().getFieldList() ) {
                    if ( metaData.getName().equalsIgnoreCase( logicalColumn.name ) ) {
                        resultColMapping.put( logicalColumn.id, i );
                    }
                    i++;
                }
            }
            if ( isMaterializedView ) {
                for ( LogicalColumn logicalColumn : selectColumnList ) {
                    if ( !resultColMapping.containsKey( logicalColumn.id ) ) {
                        int i = resultColMapping.values().stream().mapToInt( v -> v ).max().orElseThrow( NoSuchElementException::new );
                        resultColMapping.put( logicalColumn.id, i + 1 );
                    }
                }
            }

            int batchSize = RuntimeConfig.DATA_MIGRATOR_BATCH_SIZE.getInteger();
            int i = 0;
            while ( sourceIterator.hasNext() ) {
                List<List<Object>> rows = MetaImpl.collect( result.getCursorFactory(), LimitIterator.of( sourceIterator, batchSize ), new ArrayList<>() );
                Map<Long, List<Object>> values = new HashMap<>();

                for ( List<Object> list : rows ) {
                    for ( Map.Entry<Long, Integer> entry : resultColMapping.entrySet() ) {
                        if ( !values.containsKey( entry.getKey() ) ) {
                            values.put( entry.getKey(), new LinkedList<>() );
                        }
                        if ( isMaterializedView ) {
                            if ( entry.getValue() > list.size() - 1 ) {
                                values.get( entry.getKey() ).add( i );
                                i++;
                            } else {
                                values.get( entry.getKey() ).add( list.get( entry.getValue() ) );
                            }
                        } else {
                            values.get( entry.getKey() ).add( list.get( entry.getValue() ) );
                        }
                    }
                }
                List<AlgDataTypeField> fields;
                if ( isMaterializedView ) {
                    fields = targetAlg.alg.getEntity().getRowType().getFieldList();
                } else {
                    fields = sourceAlg.validatedRowType.getFieldList();
                }
                int pos = 0;
                for ( Map.Entry<Long, List<Object>> v : values.entrySet() ) {
                    targetStatement.getDataContext().addParameterValues(
                            v.getKey(),
                            fields.get( resultColMapping.get( v.getKey() ) ).getType(),
                            v.getValue() );
                    pos++;
                }

                Iterator<?> iterator = targetStatement.getQueryProcessor()
                        .prepareQuery( targetAlg, sourceAlg.validatedRowType, true, false, false )
                        .enumerable( targetStatement.getDataContext() )
                        .iterator();
                //noinspection WhileLoopReplaceableByForEach
                while ( iterator.hasNext() ) {
                    iterator.next();
                }
                targetStatement.getDataContext().resetParameterValues();
            }
        } catch ( Throwable t ) {
            throw new RuntimeException( t );
        }
    }


    @Override
    public AlgRoot buildDeleteStatement( Statement statement, List<AllocationColumn> to, long partitionId ) {
        PhysicalTable physical = statement.getTransaction().getSnapshot().physical().getPhysicalTable( partitionId );
        ModifiableEntity modifiableTable = physical.unwrap( ModifiableEntity.class );

        AlgOptCluster cluster = AlgOptCluster.create(
                statement.getQueryProcessor().getPlanner(),
                new RexBuilder( statement.getTransaction().getTypeFactory() ), null, statement.getTransaction().getSnapshot() );
        AlgDataTypeFactory typeFactory = new PolyTypeFactoryImpl( AlgDataTypeSystem.DEFAULT );

        List<String> columnNames = new LinkedList<>();
        List<RexNode> values = new LinkedList<>();
        for ( AllocationColumn ccp : to ) {
            LogicalColumn logicalColumn = Catalog.getInstance().getSnapshot().rel().getColumn( ccp.columnId );
            columnNames.add( ccp.getLogicalColumnName() );
            values.add( new RexDynamicParam( logicalColumn.getAlgDataType( typeFactory ), (int) logicalColumn.id ) );
        }
        AlgBuilder builder = AlgBuilder.create( statement, cluster );
        builder.push( LogicalValues.createOneRow( cluster ) );
        builder.project( values, columnNames );

        AlgNode node = modifiableTable.toModificationAlg(
                cluster,
                cluster.traitSet(),
                physical,
                builder.build(),
                Modify.Operation.DELETE,
                null,
                null
        );

        return AlgRoot.of( node, Kind.DELETE );
    }


    @Override
    public AlgRoot buildInsertStatement( Statement statement, List<AllocationColumn> to, long partitionId ) {
        PhysicalTable physical = statement.getTransaction().getSnapshot().physical().getPhysicalTable( partitionId );
        ModifiableEntity modifiableTable = physical.unwrap( ModifiableEntity.class );

        AlgOptCluster cluster = AlgOptCluster.create(
                statement.getQueryProcessor().getPlanner(),
                new RexBuilder( statement.getTransaction().getTypeFactory() ), null, statement.getDataContext().getSnapshot() );
        AlgDataTypeFactory typeFactory = new PolyTypeFactoryImpl( AlgDataTypeSystem.DEFAULT );

        // while adapters should be able to handle unsorted columnIds for prepared indexes,
        // this often leads to errors, and can be prevented by sorting
        List<AllocationColumn> placements = to.stream().sorted( Comparator.comparingLong( p -> p.columnId ) ).collect( Collectors.toList() );

        List<String> columnNames = new LinkedList<>();
        List<RexNode> values = new LinkedList<>();
        for ( AllocationColumn ccp : placements ) {
            LogicalColumn logicalColumn = Catalog.getInstance().getSnapshot().rel().getColumn( ccp.columnId );
            columnNames.add( ccp.getLogicalColumnName() );
            values.add( new RexDynamicParam( logicalColumn.getAlgDataType( typeFactory ), (int) logicalColumn.id ) );
        }
        AlgBuilder builder = AlgBuilder.create( statement, cluster );
        builder.push( LogicalValues.createOneRow( cluster ) );
        builder.project( values, columnNames );

        AlgNode node = modifiableTable.toModificationAlg(
                cluster,
                cluster.traitSet(),
                physical,
                builder.build(),
                Modify.Operation.INSERT,
                null,
                null
        );
        return AlgRoot.of( node, Kind.INSERT );
    }


    private AlgRoot buildUpdateStatement( Statement statement, List<AllocationColumn> to, long partitionId ) {
        PhysicalTable physical = statement.getTransaction().getSnapshot().physical().getPhysicalTable( partitionId );
        ModifiableEntity modifiableTable = physical.unwrap( ModifiableEntity.class );

        AlgOptCluster cluster = AlgOptCluster.create(
                statement.getQueryProcessor().getPlanner(),
                new RexBuilder( statement.getTransaction().getTypeFactory() ), null, statement.getDataContext().getSnapshot() );
        AlgDataTypeFactory typeFactory = new PolyTypeFactoryImpl( AlgDataTypeSystem.DEFAULT );

        AlgBuilder builder = AlgBuilder.create( statement, cluster );
        builder.scan( physical );

        // build condition
        RexNode condition = null;
        LogicalRelSnapshot snapshot = Catalog.getInstance().getSnapshot().rel();
        LogicalTable catalogTable = snapshot.getTable( to.get( 0 ).tableId );
        CatalogPrimaryKey primaryKey = snapshot.getPrimaryKey( catalogTable.primaryKey );
        for ( long cid : primaryKey.columnIds ) {
            AllocationColumn ccp = Catalog.getInstance().getSnapshot().alloc().getColumnPlacement( to.get( 0 ).adapterId, cid );
            LogicalColumn logicalColumn = snapshot.getColumn( cid );
            RexNode c = builder.equals(
                    builder.field( ccp.getLogicalColumnName() ),
                    new RexDynamicParam( logicalColumn.getAlgDataType( typeFactory ), (int) logicalColumn.id )
            );
            if ( condition == null ) {
                condition = c;
            } else {
                condition = builder.and( condition, c );
            }
        }
        builder = builder.filter( condition );

        List<String> columnNames = new LinkedList<>();
        List<RexNode> values = new LinkedList<>();
        for ( AllocationColumn ccp : to ) {
            LogicalColumn logicalColumn = snapshot.getColumn( ccp.columnId );
            columnNames.add( ccp.getLogicalColumnName() );
            values.add( new RexDynamicParam( logicalColumn.getAlgDataType( typeFactory ), (int) logicalColumn.id ) );
        }

        builder.projectPlus( values );

        AlgNode node = modifiableTable.toModificationAlg(
                cluster,
                cluster.traitSet(),
                physical,
                builder.build(),
                Modify.Operation.UPDATE,
                columnNames,
                values
        );
        AlgRoot algRoot = AlgRoot.of( node, Kind.UPDATE );
        AlgStructuredTypeFlattener typeFlattener = new AlgStructuredTypeFlattener(
                AlgBuilder.create( statement, algRoot.alg.getCluster() ),
                algRoot.alg.getCluster().getRexBuilder(),
                algRoot.alg::getCluster,
                true );
        return algRoot.withAlg( typeFlattener.rewrite( algRoot.alg ) );
    }


    @Override
    public AlgRoot getSourceIterator( Statement statement, Map<Long, List<AllocationColumn>> placementDistribution ) {

        // Build Query
        AlgOptCluster cluster = AlgOptCluster.create(
                statement.getQueryProcessor().getPlanner(),
                new RexBuilder( statement.getTransaction().getTypeFactory() ), null, statement.getDataContext().getSnapshot() );

        AlgNode node = RoutingManager.getInstance().getFallbackRouter().buildJoinedScan( statement, cluster, null );
        return AlgRoot.of( node, Kind.SELECT );
    }


    public static List<AllocationColumn> selectSourcePlacements( LogicalTable table, List<LogicalColumn> columns, long excludingAdapterId ) {
        // Find the adapter with the most column placements
        Catalog catalog = Catalog.getInstance();
        Snapshot snapshot = catalog.getSnapshot();
        long adapterIdWithMostPlacements = -1;
        int numOfPlacements = 0;
        for ( Entry<Long, List<Long>> entry : snapshot.alloc().getColumnPlacementsByAdapter( table.id ).entrySet() ) {
            if ( entry.getKey() != excludingAdapterId && entry.getValue().size() > numOfPlacements ) {
                adapterIdWithMostPlacements = entry.getKey();
                numOfPlacements = entry.getValue().size();
            }
        }

        List<Long> columnIds = new LinkedList<>();
        for ( LogicalColumn logicalColumn : columns ) {
            columnIds.add( logicalColumn.id );
        }

        // Take the adapter with most placements as base and add missing column placements
        List<AllocationColumn> placementList = new LinkedList<>();
        for ( LogicalColumn column : snapshot.rel().getColumns( table.id ) ) {
            if ( columnIds.contains( column.id ) ) {
                if ( snapshot.alloc().getDataPlacement( adapterIdWithMostPlacements, table.id ).columnPlacementsOnAdapter.contains( column.id ) ) {
                    placementList.add( snapshot.alloc().getColumnPlacement( adapterIdWithMostPlacements, column.id ) );
                } else {
                    for ( AllocationColumn placement : snapshot.alloc().getColumnPlacements( column.id ) ) {
                        if ( placement.adapterId != excludingAdapterId ) {
                            placementList.add( placement );
                            break;
                        }
                    }
                }
            }
        }

        return placementList;
    }


    /**
     * Currently used to to transfer data if partitioned table is about to be merged.
     * For Table Partitioning use {@link #copyPartitionData(Transaction, CatalogAdapter, LogicalTable, LogicalTable, List, List, List)}  } instead
     *
     * @param transaction Transactional scope
     * @param store Target Store where data should be migrated to
     * @param sourceTable Source Table from where data is queried
     * @param targetTable Source Table from where data is queried
     * @param columns Necessary columns on target
     * @param placementDistribution Pre-computed mapping of partitions and the necessary column placements
     * @param targetPartitionIds Target Partitions where data should be inserted
     */
    @Override
    public void copySelectiveData( Transaction transaction, CatalogAdapter store, LogicalTable sourceTable, LogicalTable targetTable, List<LogicalColumn> columns, Map<Long, List<AllocationColumn>> placementDistribution, List<Long> targetPartitionIds ) {
        CatalogPrimaryKey sourcePrimaryKey = Catalog.getInstance().getSnapshot().rel().getPrimaryKey( sourceTable.primaryKey );
        AllocSnapshot snapshot = Catalog.getInstance().getSnapshot().alloc();

        // Check Lists
        List<AllocationColumn> targetColumnPlacements = new LinkedList<>();
        for ( LogicalColumn logicalColumn : columns ) {
            targetColumnPlacements.add( snapshot.getColumnPlacement( store.id, logicalColumn.id ) );
        }

        List<LogicalColumn> selectColumnList = new LinkedList<>( columns );

        // Add primary keys to select column list
        for ( long cid : sourcePrimaryKey.columnIds ) {
            LogicalColumn logicalColumn = Catalog.getInstance().getSnapshot().rel().getColumn( cid );
            if ( !selectColumnList.contains( logicalColumn ) ) {
                selectColumnList.add( logicalColumn );
            }
        }

        Statement sourceStatement = transaction.createStatement();
        Statement targetStatement = transaction.createStatement();

        AlgRoot sourceAlg = getSourceIterator( sourceStatement, placementDistribution );
        AlgRoot targetAlg;
        if ( snapshot.getColumnPlacementsOnAdapterPerTable( store.id, targetTable.id ).size() == columns.size() ) {
            // There have been no placements for this table on this store before. Build insert statement
            targetAlg = buildInsertStatement( targetStatement, targetColumnPlacements, targetPartitionIds.get( 0 ) );
        } else {
            // Build update statement
            targetAlg = buildUpdateStatement( targetStatement, targetColumnPlacements, targetPartitionIds.get( 0 ) );
        }

        // Execute Query
        try {
            PolyImplementation result = sourceStatement.getQueryProcessor().prepareQuery( sourceAlg, sourceAlg.alg.getCluster().getTypeFactory().builder().build(), true, false, false );
            final Enumerable<Object> enumerable = result.enumerable( sourceStatement.getDataContext() );
            //noinspection unchecked
            Iterator<Object> sourceIterator = enumerable.iterator();

            Map<Long, Integer> resultColMapping = new HashMap<>();
            for ( LogicalColumn logicalColumn : selectColumnList ) {
                int i = 0;
                for ( AlgDataTypeField metaData : result.getRowType().getFieldList() ) {
                    if ( metaData.getName().equalsIgnoreCase( logicalColumn.name ) ) {
                        resultColMapping.put( logicalColumn.id, i );
                    }
                    i++;
                }
            }

            int batchSize = RuntimeConfig.DATA_MIGRATOR_BATCH_SIZE.getInteger();
            while ( sourceIterator.hasNext() ) {
                List<List<Object>> rows = MetaImpl.collect(
                        result.getCursorFactory(),
                        LimitIterator.of( sourceIterator, batchSize ),
                        new ArrayList<>() );
                Map<Long, List<Object>> values = new HashMap<>();
                for ( List<Object> list : rows ) {
                    for ( Map.Entry<Long, Integer> entry : resultColMapping.entrySet() ) {
                        if ( !values.containsKey( entry.getKey() ) ) {
                            values.put( entry.getKey(), new LinkedList<>() );
                        }
                        values.get( entry.getKey() ).add( list.get( entry.getValue() ) );
                    }
                }
                for ( Map.Entry<Long, List<Object>> v : values.entrySet() ) {
                    targetStatement.getDataContext().addParameterValues( v.getKey(), null, v.getValue() );
                }
                Iterator<?> iterator = targetStatement.getQueryProcessor()
                        .prepareQuery( targetAlg, sourceAlg.validatedRowType, true, false, true )
                        .enumerable( targetStatement.getDataContext() )
                        .iterator();

                //noinspection WhileLoopReplaceableByForEach
                while ( iterator.hasNext() ) {
                    iterator.next();
                }
                targetStatement.getDataContext().resetParameterValues();
            }
        } catch ( Throwable t ) {
            throw new RuntimeException( t );
        }
    }


    /**
     * Currently used to transfer data if unpartitioned is about to be partitioned.
     * For Table Merge use {@link #copySelectiveData(Transaction, CatalogAdapter, LogicalTable, LogicalTable, List, Map, List)}   } instead
     *
     * @param transaction Transactional scope
     * @param store Target Store where data should be migrated to
     * @param sourceTable Source Table from where data is queried
     * @param targetTable Target Table where data is to be inserted
     * @param columns Necessary columns on target
     * @param sourcePartitionIds Source Partitions which need to be considered for querying
     * @param targetPartitionIds Target Partitions where data should be inserted
     */
    @Override
    public void copyPartitionData( Transaction transaction, CatalogAdapter store, LogicalTable sourceTable, LogicalTable targetTable, List<LogicalColumn> columns, List<Long> sourcePartitionIds, List<Long> targetPartitionIds ) {
        if ( sourceTable.id != targetTable.id ) {
            throw new RuntimeException( "Unsupported migration scenario. Table ID mismatch" );
        }
        Snapshot snapshot = Catalog.getInstance().getSnapshot();
        CatalogPrimaryKey primaryKey = snapshot.rel().getPrimaryKey( sourceTable.primaryKey );

        // Check Lists
        List<AllocationColumn> targetColumnPlacements = new LinkedList<>();
        for ( LogicalColumn logicalColumn : columns ) {
            targetColumnPlacements.add( snapshot.alloc().getColumnPlacement( store.id, logicalColumn.id ) );
        }

        List<LogicalColumn> selectColumnList = new LinkedList<>( columns );

        // Add primary keys to select column list
        for ( long cid : primaryKey.columnIds ) {
            LogicalColumn logicalColumn = snapshot.rel().getColumn( cid );
            if ( !selectColumnList.contains( logicalColumn ) ) {
                selectColumnList.add( logicalColumn );
            }
        }

        PartitionProperty targetProperty = snapshot.alloc().getPartitionProperty( targetTable.id );
        // Add partition columns to select column list
        long partitionColumnId = targetProperty.partitionColumnId;
        LogicalColumn partitionColumn = snapshot.rel().getColumn( partitionColumnId );
        if ( !selectColumnList.contains( partitionColumn ) ) {
            selectColumnList.add( partitionColumn );
        }

        PartitionManagerFactory partitionManagerFactory = PartitionManagerFactory.getInstance();
        PartitionManager partitionManager = partitionManagerFactory.getPartitionManager( targetProperty.partitionType );

        //We need a columnPlacement for every partition
        Map<Long, List<AllocationColumn>> placementDistribution = new HashMap<>();
        PartitionProperty sourceProperty = snapshot.alloc().getPartitionProperty( sourceTable.id );
        placementDistribution.put( sourceProperty.partitionIds.get( 0 ), selectSourcePlacements( sourceTable, selectColumnList, -1 ) );

        Statement sourceStatement = transaction.createStatement();

        //Map PartitionId to TargetStatementQueue
        Map<Long, Statement> targetStatements = new HashMap<>();

        //Creates queue of target Statements depending
        targetPartitionIds.forEach( id -> targetStatements.put( id, transaction.createStatement() ) );

        Map<Long, AlgRoot> targetAlgs = new HashMap<>();

        AlgRoot sourceAlg = getSourceIterator( sourceStatement, placementDistribution );
        if ( Catalog.getInstance().getSnapshot().alloc().getColumnPlacementsOnAdapterPerTable( store.id, sourceTable.id ).size() == columns.size() ) {
            // There have been no placements for this table on this store before. Build insert statement
            targetPartitionIds.forEach( id -> targetAlgs.put( id, buildInsertStatement( targetStatements.get( id ), targetColumnPlacements, id ) ) );
        } else {
            // Build update statement
            targetPartitionIds.forEach( id -> targetAlgs.put( id, buildUpdateStatement( targetStatements.get( id ), targetColumnPlacements, id ) ) );
        }

        // Execute Query
        try {
            PolyImplementation result = sourceStatement.getQueryProcessor().prepareQuery( sourceAlg, sourceAlg.alg.getCluster().getTypeFactory().builder().build(), true, false, false );
            final Enumerable<?> enumerable = result.enumerable( sourceStatement.getDataContext() );
            //noinspection unchecked
            Iterator<Object> sourceIterator = (Iterator<Object>) enumerable.iterator();

            Map<Long, Integer> resultColMapping = new HashMap<>();
            for ( LogicalColumn logicalColumn : selectColumnList ) {
                int i = 0;
                for ( AlgDataTypeField metaData : result.getRowType().getFieldList() ) {
                    if ( metaData.getName().equalsIgnoreCase( logicalColumn.name ) ) {
                        resultColMapping.put( logicalColumn.id, i );
                    }
                    i++;
                }
            }

            int partitionColumnIndex = -1;
            String parsedValue = null;
            String nullifiedPartitionValue = partitionManager.getUnifiedNullValue();
            if ( targetProperty.isPartitioned ) {
                if ( resultColMapping.containsKey( targetProperty.partitionColumnId ) ) {
                    partitionColumnIndex = resultColMapping.get( targetProperty.partitionColumnId );
                } else {
                    parsedValue = nullifiedPartitionValue;
                }
            }

            int batchSize = RuntimeConfig.DATA_MIGRATOR_BATCH_SIZE.getInteger();
            while ( sourceIterator.hasNext() ) {
                List<List<Object>> rows = MetaImpl.collect( result.getCursorFactory(), LimitIterator.of( sourceIterator, batchSize ), new ArrayList<>() );

                Map<Long, Map<Long, List<Object>>> partitionValues = new HashMap<>();

                for ( List<Object> row : rows ) {
                    long currentPartitionId = -1;
                    if ( partitionColumnIndex >= 0 ) {
                        parsedValue = nullifiedPartitionValue;
                        if ( row.get( partitionColumnIndex ) != null ) {
                            parsedValue = row.get( partitionColumnIndex ).toString();
                        }
                    }

                    currentPartitionId = partitionManager.getTargetPartitionId( targetTable, parsedValue );

                    for ( Map.Entry<Long, Integer> entry : resultColMapping.entrySet() ) {
                        if ( entry.getKey() == partitionColumn.id && !columns.contains( partitionColumn ) ) {
                            continue;
                        }
                        if ( !partitionValues.containsKey( currentPartitionId ) ) {
                            partitionValues.put( currentPartitionId, new HashMap<>() );
                        }
                        if ( !partitionValues.get( currentPartitionId ).containsKey( entry.getKey() ) ) {
                            partitionValues.get( currentPartitionId ).put( entry.getKey(), new LinkedList<>() );
                        }
                        partitionValues.get( currentPartitionId ).get( entry.getKey() ).add( row.get( entry.getValue() ) );
                    }
                }

                // Iterate over partitionValues in that way we don't even execute a statement which has no rows
                for ( Map.Entry<Long, Map<Long, List<Object>>> dataOnPartition : partitionValues.entrySet() ) {
                    long partitionId = dataOnPartition.getKey();
                    Map<Long, List<Object>> values = dataOnPartition.getValue();
                    Statement currentTargetStatement = targetStatements.get( partitionId );

                    for ( Map.Entry<Long, List<Object>> columnDataOnPartition : values.entrySet() ) {
                        // Check partitionValue
                        currentTargetStatement.getDataContext().addParameterValues( columnDataOnPartition.getKey(), null, columnDataOnPartition.getValue() );
                    }

                    Iterator iterator = currentTargetStatement.getQueryProcessor()
                            .prepareQuery( targetAlgs.get( partitionId ), sourceAlg.validatedRowType, true, false, false )
                            .enumerable( currentTargetStatement.getDataContext() )
                            .iterator();
                    //noinspection WhileLoopReplaceableByForEach
                    while ( iterator.hasNext() ) {
                        iterator.next();
                    }
                    currentTargetStatement.getDataContext().resetParameterValues();
                }
            }
        } catch ( Throwable t ) {
            throw new RuntimeException( t );
        }
    }

}
