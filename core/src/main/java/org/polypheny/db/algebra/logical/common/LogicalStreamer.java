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

package org.polypheny.db.algebra.logical.common;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.polypheny.db.algebra.AlgNode;
import org.polypheny.db.algebra.AlgShuttle;
import org.polypheny.db.algebra.core.Values;
import org.polypheny.db.algebra.core.common.Modify;
import org.polypheny.db.algebra.core.common.Streamer;
import org.polypheny.db.algebra.core.relational.RelModify;
import org.polypheny.db.algebra.core.relational.RelScan;
import org.polypheny.db.algebra.logical.relational.LogicalProject;
import org.polypheny.db.algebra.logical.relational.LogicalRelModify;
import org.polypheny.db.algebra.logical.relational.LogicalValues;
import org.polypheny.db.algebra.type.AlgDataType;
import org.polypheny.db.algebra.type.AlgDataTypeField;
import org.polypheny.db.plan.AlgOptCluster;
import org.polypheny.db.plan.AlgTraitSet;
import org.polypheny.db.plan.volcano.AlgSubset;
import org.polypheny.db.rex.RexBuilder;
import org.polypheny.db.rex.RexInputRef;
import org.polypheny.db.rex.RexNode;
import org.polypheny.db.tools.AlgBuilder;

@Slf4j
public class LogicalStreamer extends Streamer {

    /**
     * {@code
     * Streamer
     * ^               |
     * |               v
     * Provider    Collector
     * }
     *
     * @param provider provides the values which get streamed to the collector
     * @param collector uses the provided values and
     */
    public LogicalStreamer( AlgOptCluster cluster, AlgTraitSet traitSet, AlgNode provider, AlgNode collector ) {
        super( cluster, traitSet, provider, collector );
    }


    public static LogicalStreamer create( AlgNode provider, AlgNode collector ) {
        return new LogicalStreamer( provider.getCluster(), provider.getTraitSet(), provider, collector );
    }


    public static LogicalStreamer create( Modify<?> allModify, AlgBuilder algBuilder ) {
        RexBuilder rexBuilder = algBuilder.getRexBuilder();

        if ( !(allModify instanceof RelModify<?>) ) {
            log.warn( "non relational nodes are not supported for toModify streamer rule" );
            return null;
        }
        RelModify<?> modify = (RelModify<?>) allModify;

        if ( !isModifyApplicable( modify ) ) {
            return null;
        }

        /////// query
        // first we create the query, which could retrieve the values for the prepared modify
        // if underlying adapter cannot handle it natively
        AlgNode input = getChild( modify.getInput() );

        return getLogicalStreamer( modify, algBuilder, rexBuilder, input );
    }


    private static LogicalStreamer getLogicalStreamer( RelModify<?> modify, AlgBuilder algBuilder, RexBuilder rexBuilder, AlgNode input ) {
        if ( input == null ) {
            throw new RuntimeException( "Error while creating Streamer." );
        }

        // add all previous variables e.g. _id, _data(previous), _data(updated)
        // might only extract previous refs used in condition e.g. _data
        List<String> update = new ArrayList<>( getOldFieldsNames( input.getRowType().getFieldNames() ) );
        List<RexNode> source = new ArrayList<>( getOldFieldRefs( input.getRowType() ) );

        AlgNode query = input;

        if ( modify.getUpdateColumnList() != null && modify.getSourceExpressionList() != null ) {
            // update and source list are not null
            update.addAll( modify.getUpdateColumnList() );
            source.addAll( modify.getSourceExpressionList() );

            // we project the needed sources out and modify them to fit the prepared
            query = LogicalProject.create( modify.getInput(), source, update );
        }

        /////// prepared

        if ( !modify.isInsert() ) {
            // get collection, which is modified
            algBuilder.scan( modify.getEntity() );
            // at the moment no data model is able to conditionally insert
            attachFilter( modify, algBuilder, rexBuilder );
        } else {
            assert input.getRowType().getFieldCount() == modify.getEntity().getRowType().getFieldCount();
            // attach a projection, so the values can be inserted on execution
            algBuilder.push( getCollector( rexBuilder, input ) );
        }

        LogicalRelModify prepared = LogicalRelModify.create(
                modify.getEntity(),
                algBuilder.build(),
                modify.getOperation(),
                modify.getUpdateColumnList(),
                modify.getSourceExpressionList() == null ? null : createSourceList( modify, rexBuilder ),
                false );
        return new LogicalStreamer( modify.getCluster(), modify.getTraitSet(), query, prepared );
    }


    @NotNull
    private static LogicalProject getCollector( RexBuilder rexBuilder, AlgNode input ) {
        return LogicalProject.create(
                LogicalValues.createOneRow( input.getCluster() ),
                input.getRowType()
                        .getFieldList()
                        .stream()
                        .map( f -> rexBuilder.makeDynamicParam( f.getType(), f.getIndex() ) )
                        .collect( Collectors.toList() ),
                input.getRowType() );
    }


    private static List<RexNode> createSourceList( RelModify<?> modify, RexBuilder rexBuilder ) {
        return modify.getUpdateColumnList()
                .stream()
                .map( name -> {
                    int size = modify.getRowType().getFieldList().size();
                    int index = modify.getEntity().getRowType().getFieldNames().indexOf( name );
                    return rexBuilder.makeDynamicParam(
                            modify.getEntity().getRowType().getFieldList().get( index ).getType(), size + index );
                } ).collect( Collectors.toList() );
    }


    private static void attachFilter( RelModify<?> modify, AlgBuilder algBuilder, RexBuilder rexBuilder ) {
        List<RexNode> fields = new ArrayList<>();
        int i = 0;
        for ( AlgDataTypeField field : modify.getEntity().getRowType().getFieldList() ) {
            fields.add(
                    algBuilder.equals(
                            rexBuilder.makeInputRef( modify.getEntity().getRowType(), i ),
                            rexBuilder.makeDynamicParam( field.getType(), i ) ) );
            i++;
        }
        algBuilder.filter( fields.size() == 1
                ? fields.get( 0 )
                : algBuilder.and( fields ) );
    }


    private static AlgNode getChild( AlgNode child ) {
        if ( child instanceof AlgSubset ) {
            return getChild( ((AlgSubset) child).getOriginal() );
        }
        return child;
    }


    public static boolean isModifyApplicable( RelModify<?> modify ) {

        // simple delete, which all store should be able to handle by themselves
        if ( modify.isInsert() && modify.getInput() instanceof Values ) {
            // simple insert, which all store should be able to handle by themselves
            return false;
        } else {
            return !modify.isDelete() || !(modify.getInput() instanceof RelScan);
        }
    }


    private static List<RexInputRef> getOldFieldRefs( AlgDataType rowType ) {
        return rowType.getFieldList().stream().map( f -> RexInputRef.of( f.getIndex(), rowType ) ).collect( Collectors.toList() );
    }


    private static List<String> getOldFieldsNames( List<String> names ) {
        return names.stream().map( name -> name + "$old" ).collect( Collectors.toList() );
    }


    @Override
    public AlgNode copy( AlgTraitSet traitSet, List<AlgNode> inputs ) {
        return new LogicalStreamer( inputs.get( 0 ).getCluster(), traitSet, inputs.get( 0 ), inputs.get( 1 ) );
    }


    @Override
    public AlgNode accept( AlgShuttle shuttle ) {
        return shuttle.visit( this );
    }

}
