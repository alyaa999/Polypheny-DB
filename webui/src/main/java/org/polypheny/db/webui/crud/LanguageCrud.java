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

package org.polypheny.db.webui.crud;

import io.javalin.http.Context;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.websocket.api.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.polypheny.db.PolyImplementation;
import org.polypheny.db.PolyImplementation.ResultIterator;
import org.polypheny.db.adapter.Adapter;
import org.polypheny.db.adapter.AdapterManager;
import org.polypheny.db.algebra.AlgRoot;
import org.polypheny.db.algebra.type.AlgDataTypeField;
import org.polypheny.db.catalog.Catalog;
import org.polypheny.db.catalog.entity.CatalogDataPlacement;
import org.polypheny.db.catalog.entity.allocation.AllocationEntity;
import org.polypheny.db.catalog.entity.logical.LogicalCollection;
import org.polypheny.db.catalog.entity.logical.LogicalColumn;
import org.polypheny.db.catalog.entity.logical.LogicalGraph;
import org.polypheny.db.catalog.entity.logical.LogicalNamespace;
import org.polypheny.db.catalog.entity.logical.LogicalTable;
import org.polypheny.db.catalog.exceptions.GenericRuntimeException;
import org.polypheny.db.catalog.logistic.EntityType;
import org.polypheny.db.catalog.logistic.Pattern;
import org.polypheny.db.config.RuntimeConfig;
import org.polypheny.db.information.InformationManager;
import org.polypheny.db.information.InformationObserver;
import org.polypheny.db.languages.QueryLanguage;
import org.polypheny.db.processing.ExtendedQueryParameters;
import org.polypheny.db.processing.Processor;
import org.polypheny.db.transaction.Statement;
import org.polypheny.db.transaction.Transaction;
import org.polypheny.db.transaction.TransactionException;
import org.polypheny.db.transaction.TransactionManager;
import org.polypheny.db.type.entity.PolyValue;
import org.polypheny.db.type.entity.graph.PolyGraph;
import org.polypheny.db.webui.Crud;
import org.polypheny.db.webui.models.FieldDefinition;
import org.polypheny.db.webui.models.Index;
import org.polypheny.db.webui.models.Placement;
import org.polypheny.db.webui.models.Placement.DocumentStore;
import org.polypheny.db.webui.models.SortState;
import org.polypheny.db.webui.models.UiColumnDefinition;
import org.polypheny.db.webui.models.requests.EditCollectionRequest;
import org.polypheny.db.webui.models.requests.QueryRequest;
import org.polypheny.db.webui.models.requests.UIRequest;
import org.polypheny.db.webui.models.results.DocResult;
import org.polypheny.db.webui.models.results.GraphResult;
import org.polypheny.db.webui.models.results.RelationalResult;
import org.polypheny.db.webui.models.results.Result;

@Getter
@Slf4j
public class LanguageCrud {


    public static Crud crud;

    public final static Map<String, Consumer7<Session, QueryRequest, TransactionManager, Long, Long, Crud, List<Result<?, ?>>>> REGISTER = new HashMap<>();


    public LanguageCrud( Crud crud ) {
        LanguageCrud.crud = crud;
    }


    public static List<Result<?, ?>> anyQuery(
            QueryLanguage language,
            Session session,
            QueryRequest request,
            TransactionManager transactionManager,
            long userId,
            long databaseId,
            InformationObserver observer ) {

        return REGISTER.get( language.getSerializedName() ).apply( session, request, transactionManager, userId, databaseId, crud );
    }


    public static void commitAndFinish( Transaction transaction, InformationManager queryAnalyzer, List<Result<?, ?>> results, long executionTime ) {
        executionTime = System.nanoTime() - executionTime;
        String commitStatus;
        try {
            transaction.commit();
            commitStatus = "Committed";
        } catch ( TransactionException e ) {
            log.error( "Caught exception", e );
            results.add( RelationalResult.builder().error( e.getMessage() ).build() );
            try {
                transaction.rollback();
                commitStatus = "Rolled back";
            } catch ( TransactionException ex ) {
                log.error( "Caught exception while rollback", e );
                commitStatus = "Error while rolling back";
            }
        }

        if ( queryAnalyzer != null ) {
            Crud.attachQueryAnalyzer( queryAnalyzer, executionTime, commitStatus, results.size() );
        }
    }


    @Nullable
    public static InformationManager attachAnalyzerIfSpecified( QueryRequest request, InformationObserver observer, Transaction transaction ) {
        // This is not a nice solution. In case of a sql script with auto commit only the first statement is analyzed
        // and in case of auto commit of, the information is overwritten
        InformationManager queryAnalyzer = null;
        if ( request.analyze ) {
            queryAnalyzer = transaction.getQueryAnalyzer().observe( observer );
        }
        return queryAnalyzer;
    }


    public static PolyGraph getGraph( String databaseName, TransactionManager manager ) {

        Transaction transaction = Crud.getTransaction( false, false, manager, Catalog.defaultUserId, Catalog.defaultNamespaceId, "getGraph" );
        Processor processor = transaction.getProcessor( QueryLanguage.from( "cypher" ) );
        Statement statement = transaction.createStatement();

        ExtendedQueryParameters parameters = new ExtendedQueryParameters( databaseName );
        AlgRoot logicalRoot = processor.translate( statement, null, parameters );
        PolyImplementation<PolyGraph> polyImplementation = statement.getQueryProcessor().prepareQuery( logicalRoot, true );

        ResultIterator<PolyGraph> iterator = polyImplementation.execute( statement, 1 );
        List<List<PolyGraph>> res = iterator.getRows();

        try {
            iterator.close();
            statement.getTransaction().commit();
        } catch ( Exception | TransactionException e ) {
            throw new GenericRuntimeException( "Error while committing graph retrieval query." );
        }

        return res.get( 0 ).get( 0 );
    }


    public static void printLog( Throwable t, QueryRequest request ) {
        log.warn( "Failed during execution\nquery:" + request.query + "\nMsg:" + t.getMessage() );
    }


    public static void attachError( Transaction transaction, List<Result<?, ?>> results, String query, Throwable t ) {
        //String msg = t.getMessage() == null ? "" : t.getMessage();
        RelationalResult result = RelationalResult.builder().error( t == null ? null : t.getMessage() ).generatedQuery( query ).xid( transaction.getXid().toString() ).build();

        if ( transaction.isActive() ) {
            try {
                transaction.rollback();
            } catch ( TransactionException e ) {
                throw new RuntimeException( "Error while rolling back the failed transaction." );
            }
        }

        results.add( result );
    }


    @NotNull
    public static Result<?, ?> getResult( QueryLanguage language, Statement statement, QueryRequest request, String query, PolyImplementation<PolyValue> implementation, Transaction transaction, final boolean noLimit ) {
        Catalog catalog = Catalog.getInstance();

        if ( language == QueryLanguage.from( "mongo" ) ) {
            return getDocResult( statement, request, query, implementation, transaction, noLimit );
        } else if ( language == QueryLanguage.from( "cypher" ) ) {
            return getGraphResult( statement, request, query, implementation, transaction, noLimit );
        }

        ResultIterator<PolyValue> iterator = implementation.execute( statement, noLimit ? -1 : language == QueryLanguage.from( "cypher" ) ? RuntimeConfig.UI_NODE_AMOUNT.getInteger() : RuntimeConfig.UI_PAGE_SIZE.getInteger() );
        List<List<PolyValue>> rows = iterator.getRows();
        try {
            iterator.close();
        } catch ( Exception e ) {
            throw new GenericRuntimeException( e );
        }

        boolean hasMoreRows = implementation.hasMoreRows();

        LogicalTable table = null;
        if ( request.entityId != null ) {
            table = Catalog.snapshot().rel().getTable( request.entityId ).orElseThrow();
        }

        List<UiColumnDefinition> header = new ArrayList<>();
        for ( AlgDataTypeField field : implementation.rowType.getFieldList() ) {
            String columnName = field.getName();

            String filter = getFilter( field, request.filter );

            SortState sort = getSortState( field, request.sortState );

            UiColumnDefinition dbCol = UiColumnDefinition.builder()
                    .name( field.getName() )
                    .dataType( field.getType().getFullTypeString() )
                    .nullable( field.getType().isNullable() == (ResultSetMetaData.columnNullable == 1) )
                    .precision( field.getType().getPrecision() )
                    .sort( sort )
                    .filter( filter ).build();

            // Get column default values
            if ( table != null ) {
                Optional<LogicalColumn> optional = catalog.getSnapshot().rel().getColumn( table.id, columnName );
                if ( optional.isPresent() ) {
                    if ( optional.get().defaultValue != null ) {
                        dbCol.defaultValue = optional.get().defaultValue.value;
                    }
                }
            }
            header.add( dbCol );
        }

        List<String[]> data = Crud.computeResultData( rows, header, statement.getTransaction() );

        return RelationalResult
                .builder()
                .header( header.toArray( new UiColumnDefinition[0] ) )
                .data( data.toArray( new String[0][] ) )
                .namespaceType( implementation.getNamespaceType() )
                .namespaceName( request.database )
                .language( language )
                .affectedRows( data.size() )
                .hasMoreRows( hasMoreRows )
                .xid( transaction.getXid().toString() )
                .generatedQuery( query )
                .build();
    }


    private static GraphResult getGraphResult( Statement statement, QueryRequest request, String query, PolyImplementation<PolyValue> implementation, Transaction transaction, boolean noLimit ) {

        ResultIterator<PolyValue> iterator = implementation.execute( statement, noLimit ? -1 : RuntimeConfig.UI_PAGE_SIZE.getInteger() );
        List<PolyValue[]> data = iterator.getArrayRows();
        try {
            iterator.close();
        } catch ( Exception e ) {
            throw new GenericRuntimeException( e );
        }

        return GraphResult.builder()
                .data( data.stream().map( r -> Arrays.stream( r ).map( LanguageCrud::toJson ).toArray( String[]::new ) ).toArray( String[][]::new ) )
                .header( implementation.rowType.getFieldList().stream().map( FieldDefinition::of ).toArray( FieldDefinition[]::new ) )
                .query( query )
                .xid( transaction.getXid().toString() )
                .namespaceName( request.database )
                .build();
    }


    private static DocResult getDocResult( Statement statement, QueryRequest request, String query, PolyImplementation<PolyValue> implementation, Transaction transaction, boolean noLimit ) {

        ResultIterator<PolyValue> iterator = implementation.execute( statement, noLimit ? -1 : RuntimeConfig.UI_PAGE_SIZE.getInteger() );
        List<PolyValue> data = iterator.getSingleRows();
        try {
            iterator.close();
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

        return DocResult.builder()
                .data( data.stream().map( LanguageCrud::toJson ).toArray( String[]::new ) )
                .query( query )
                .xid( transaction.getXid().toString() )
                .namespaceName( request.database )
                .build();
    }


    private static String toJson( @Nullable PolyValue src ) {
        return src == null
                ? null
                : src.toJson();
    }


    private static String getFilter( AlgDataTypeField field, Map<String, String> filter ) {
        if ( filter != null && filter.containsKey( field.getName() ) ) {
            return filter.get( field.getName() );
        }
        return "";
    }


    private static SortState getSortState( AlgDataTypeField field, Map<String, SortState> sortState ) {
        if ( sortState != null && sortState.containsKey( field.getName() ) ) {
            return sortState.get( field.getName() );
        }
        return new SortState();
    }


    /**
     * Creates a new document collection
     */
    public void createCollection( final Context ctx ) {
        EditCollectionRequest request = ctx.bodyAsClass( EditCollectionRequest.class );
        Transaction transaction = crud.getTransaction();

        String query = String.format( "db.createCollection(%s)", request.collection );

        RelationalResult result;
        try {
            anyQuery( QueryLanguage.from( "mongo" ), null, new QueryRequest( query, false, false, "CYPHER", request.database ), crud.getTransactionManager(), crud.getUserId(), crud.getNamespaceId(), null );

            result = RelationalResult.builder().affectedRows( 1 ).generatedQuery( query ).build();
            transaction.commit();
        } catch ( TransactionException e ) {
            log.error( "Caught exception while creating a table", e );
            result = RelationalResult.builder().error( e.getMessage() ).generatedQuery( query ).build();
            try {
                transaction.rollback();
            } catch ( TransactionException ex ) {
                log.error( "Could not rollback createCollection statement: {}", ex.getMessage(), ex );
            }
        }
        ctx.json( result );
    }


    /**
     * This query returns a list of all available document databases (Polypheny schema),
     * as a query result
     */
    public void getDocumentDatabases( final Context ctx ) {
        Map<String, String> names = Catalog.getInstance().getSnapshot()
                .getNamespaces( null )
                .stream()
                .collect( Collectors.toMap( LogicalNamespace::getName, s -> s.namespaceType.name() ) );

        String[][] data = names.entrySet().stream().map( n -> new String[]{ n.getKey(), n.getValue() } ).toArray( String[][]::new );
        ctx.json( RelationalResult
                .builder()
                .header( new UiColumnDefinition[]{ UiColumnDefinition.builder().name( "Database/Schema" ).build(), UiColumnDefinition.builder().name( "Type" ).build() } )
                .data( data )
                .build() );
    }


    public void getGraphPlacements( final Context ctx ) {
        Index index = ctx.bodyAsClass( Index.class );
        ctx.json( getPlacements( index ) );
    }


    private Placement getPlacements( final Index index ) {
        Catalog catalog = Catalog.getInstance();
        String graphName = index.getSchema();
        List<LogicalNamespace> namespaces = catalog.getSnapshot().getNamespaces( new Pattern( graphName ) );
        if ( namespaces.size() != 1 ) {
            throw new RuntimeException();
        }
        List<LogicalGraph> graphs = catalog.getSnapshot().graph().getGraphs( new Pattern( graphName ) );
        if ( graphs.size() != 1 ) {
            log.error( "The requested graph does not exist." );
            return new Placement( new RuntimeException( "The requested graph does not exist." ) );
        }
        LogicalGraph graph = graphs.get( 0 );
        EntityType type = EntityType.ENTITY;
        Placement p = new Placement( false, List.of(), EntityType.ENTITY );
        if ( type == EntityType.VIEW ) {
            return p;
        } else {
            List<CatalogDataPlacement> placements = catalog.getSnapshot().alloc().getDataPlacements( graph.id );
            for ( CatalogDataPlacement placement : placements ) {
                Adapter<?> adapter = AdapterManager.getInstance().getAdapter( placement.adapterId );
                p.addAdapter( new Placement.GraphStore(
                        adapter.getUniqueName(),
                        adapter.getUniqueName(),
                        catalog.getSnapshot().alloc().getFromLogical( placement.adapterId ),
                        false ) );
            }
            return p;
        }

    }


    public void getFixedFields( Context context ) {
        Catalog catalog = Catalog.getInstance();
        UIRequest request = context.bodyAsClass( UIRequest.class );
        RelationalResult result;
        List<UiColumnDefinition> cols = new ArrayList<>();

        result = RelationalResult.builder().header( cols.toArray( new UiColumnDefinition[0] ) ).data( null ).build();
        context.json( result );

    }


    public void getCollectionPlacements( Context context ) {
        Index index = context.bodyAsClass( Index.class );
        String namespace = index.getSchema();
        String collectionName = index.getTable();
        Catalog catalog = Catalog.getInstance();
        long namespaceId = catalog.getSnapshot().getNamespace( namespace ).orElseThrow().id;
        List<LogicalCollection> collections = catalog.getSnapshot().doc().getCollections( namespaceId, new Pattern( collectionName ) );

        if ( collections.size() != 1 ) {
            context.json( new Placement( new RuntimeException( "The collation is not know" ) ) );
            return;
        }

        LogicalCollection collection = collections.get( 0 );

        Placement p = new Placement( false, List.of(), EntityType.ENTITY );

        List<AllocationEntity> allocs = catalog.getSnapshot().alloc().getFromLogical( collection.id );

        for ( AllocationEntity allocation : allocs ) {
            Adapter<?> adapter = AdapterManager.getInstance().getAdapter( allocation.adapterId );
            p.addAdapter( new DocumentStore(
                    adapter.getUniqueName(),
                    adapter.getUniqueName(),
                    catalog.getSnapshot().alloc().getEntitiesOnAdapter( allocation.adapterId )
                            .orElse( List.of() ),
                    false ) );
        }

        context.json( p );
    }


    public void addLanguage(
            String language,
            Consumer7<Session,
                    QueryRequest,
                    TransactionManager,
                    Long,
                    Long,
                    Crud,
                    List<Result<?, ?>>> function ) {
        REGISTER.put( language, function );
    }


    public void removeLanguage( String name ) {
        REGISTER.remove( name );
    }


    @FunctionalInterface
    public interface Consumer7<One, Two, Three, Four, Five, Six, Seven> {

        Seven apply( One one, Two two, Three three, Four four, Five five, Six six );

    }


}

