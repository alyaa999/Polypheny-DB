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

package org.polypheny.db.adapter.pig;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.pig.scripting.Pig;
import org.polypheny.db.algebra.AlgNode;
import org.polypheny.db.algebra.core.Aggregate;
import org.polypheny.db.algebra.core.AggregateCall;
import org.polypheny.db.algebra.type.AlgDataTypeField;
import org.polypheny.db.plan.AlgOptCluster;
import org.polypheny.db.plan.AlgOptTable;
import org.polypheny.db.plan.AlgTraitSet;
import org.polypheny.db.util.ImmutableBitSet;


/**
 * Implementation of {@link org.polypheny.db.algebra.core.Aggregate} in {@link PigAlg#CONVENTION Pig calling convention}.
 */
public class PigAggregate extends Aggregate implements PigAlg {

    public static final String DISTINCT_FIELD_SUFFIX = "_DISTINCT";


    /**
     * Creates a PigAggregate.
     */
    public PigAggregate( AlgOptCluster cluster, AlgTraitSet traits, AlgNode child, boolean indicator, ImmutableBitSet groupSet, List<ImmutableBitSet> groupSets, List<AggregateCall> aggCalls ) {
        super( cluster, traits, child, indicator, groupSet, groupSets, aggCalls );
        assert getConvention() == CONVENTION;
    }


    @Override
    public Aggregate copy( AlgTraitSet traitSet, AlgNode input, boolean indicator, ImmutableBitSet groupSet, List<ImmutableBitSet> groupSets, List<AggregateCall> aggCalls ) {
        return new PigAggregate( input.getCluster(), traitSet, input, indicator, groupSet, groupSets, aggCalls );
    }


    @Override
    public void implement( Implementor implementor ) {
        implementor.visitChild( 0, getInput() );
        implementor.addStatement( getPigAggregateStatement( implementor ) );
    }


    /**
     * Generates a GROUP BY statement, followed by an optional FOREACH statement for all aggregate functions used. e.g.
     *
     * {@code
     * A = GROUP A BY owner;
     * A = FOREACH A GENERATE group, SUM(A.pet_num);
     * }
     */
    private String getPigAggregateStatement( Implementor implementor ) {
        return getPigGroupBy( implementor ) + '\n' + getPigForEachGenerate( implementor );
    }


    /**
     * Override this method so it looks down the tree to find the table this node is acting on.
     */
    @Override
    public AlgOptTable getTable() {
        return getInput().getTable();
    }


    /**
     * Generates the GROUP BY statement, e.g. <code>A = GROUP A BY (f1, f2);</code>
     */
    private String getPigGroupBy( Implementor implementor ) {
        final String relAlias = implementor.getPigRelationAlias( this );
        final List<AlgDataTypeField> allFields = getInput().getRowType().getFieldList();
        final List<Integer> groupedFieldIndexes = groupSet.asList();
        if ( groupedFieldIndexes.size() < 1 ) {
            return relAlias + " = GROUP " + relAlias + " ALL;";
        } else {
            final List<String> groupedFieldNames = new ArrayList<>( groupedFieldIndexes.size() );
            for ( int fieldIndex : groupedFieldIndexes ) {
                groupedFieldNames.add( allFields.get( fieldIndex ).getName() );
            }
            return relAlias + " = GROUP " + relAlias + " BY (" + String.join( ", ", groupedFieldNames ) + ");";
        }
    }


    /**
     * Generates a FOREACH statement containing invocation of aggregate functions and projection of grouped fields. e.g.
     * <code>A = FOREACH A GENERATE group, SUM(A.pet_num);</code>
     *
     * @see Pig documentation for special meaning of the "group" field after GROUP BY.
     */
    private String getPigForEachGenerate( Implementor implementor ) {
        final String relAlias = implementor.getPigRelationAlias( this );
        final String generateCall = getPigGenerateCall( implementor );
        final List<String> distinctCalls = getDistinctCalls( implementor );
        return relAlias + " = FOREACH " + relAlias + " {\n" + String.join( ";\n", distinctCalls ) + generateCall + "\n};";
    }


    private String getPigGenerateCall( Implementor implementor ) {
        final List<Integer> groupedFieldIndexes = groupSet.asList();
        Set<String> groupFields = new HashSet<>( groupedFieldIndexes.size() );
        for ( int fieldIndex : groupedFieldIndexes ) {
            final String fieldName = getInputFieldName( fieldIndex );
            // Pig appends group field name if grouping by multiple fields
            final String groupField = (groupedFieldIndexes.size() == 1 ? "group" : ("group." + fieldName)) + " AS " + fieldName;

            groupFields.add( groupField );
        }
        final List<String> pigAggCalls = getPigAggregateCalls( implementor );
        List<String> allFields = new ArrayList<>( groupFields.size() + pigAggCalls.size() );
        allFields.addAll( groupFields );
        allFields.addAll( pigAggCalls );
        return "  GENERATE " + String.join( ", ", allFields ) + ';';
    }


    private List<String> getPigAggregateCalls( Implementor implementor ) {
        final String relAlias = implementor.getPigRelationAlias( this );
        final List<String> result = new ArrayList<>( aggCalls.size() );
        for ( AggregateCall ac : aggCalls ) {
            result.add( getPigAggregateCall( relAlias, ac ) );
        }
        return result;
    }


    private String getPigAggregateCall( String relAlias, AggregateCall aggCall ) {
        final PigAggFunction aggFunc = toPigAggFunc( aggCall );
        final String alias = aggCall.getName();
        final String fields = String.join( ", ", getArgNames( relAlias, aggCall ) );
        return aggFunc.name() + "(" + fields + ") AS " + alias;
    }


    private PigAggFunction toPigAggFunc( AggregateCall aggCall ) {
        return PigAggFunction.valueOf( aggCall.getAggregation().getKind(), aggCall.getArgList().size() < 1 );
    }


    private List<String> getArgNames( String relAlias, AggregateCall aggCall ) {
        final List<String> result = new ArrayList<>( aggCall.getArgList().size() );
        for ( int fieldIndex : aggCall.getArgList() ) {
            result.add( getInputFieldNameForAggCall( relAlias, aggCall, fieldIndex ) );
        }
        return result;
    }


    private String getInputFieldNameForAggCall( String relAlias, AggregateCall aggCall, int fieldIndex ) {
        final String inputField = getInputFieldName( fieldIndex );
        return aggCall.isDistinct() ? (inputField + DISTINCT_FIELD_SUFFIX) : (relAlias + '.' + inputField);
    }


    /**
     * A agg function call like <code>COUNT(DISTINCT COL)</code> in Pig is achieved via two statements in a FOREACH that follows a GROUP statement:
     *
     * <code>
     * TABLE = GROUP TABLE ALL;<br>
     * TABLE = FOREACH TABLE {<br>
     * &nbsp;&nbsp;<b>COL.DISTINCT = DISTINCT COL;<br>
     * &nbsp;&nbsp;GENERATE COUNT(COL.DISTINCT) AS C;</b><br>
     * }</code>
     */
    private List<String> getDistinctCalls( Implementor implementor ) {
        final String relAlias = implementor.getPigRelationAlias( this );
        final List<String> result = new ArrayList<>();
        for ( AggregateCall aggCall : aggCalls ) {
            if ( aggCall.isDistinct() ) {
                for ( int fieldIndex : aggCall.getArgList() ) {
                    String fieldName = getInputFieldName( fieldIndex );
                    result.add( "  " + fieldName + DISTINCT_FIELD_SUFFIX + " = DISTINCT " + relAlias + '.' + fieldName + ";\n" );
                }
            }
        }
        return result;
    }


    private String getInputFieldName( int fieldIndex ) {
        return getInput().getRowType().getFieldList().get( fieldIndex ).getName();
    }

}

