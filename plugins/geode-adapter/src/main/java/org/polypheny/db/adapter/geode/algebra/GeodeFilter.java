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


import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.polypheny.db.algebra.AlgNode;
import org.polypheny.db.algebra.constant.Kind;
import org.polypheny.db.algebra.core.Filter;
import org.polypheny.db.algebra.metadata.AlgMetadataQuery;
import org.polypheny.db.algebra.type.AlgDataType;
import org.polypheny.db.plan.AlgOptCluster;
import org.polypheny.db.plan.AlgOptCost;
import org.polypheny.db.plan.AlgOptPlanner;
import org.polypheny.db.plan.AlgOptUtil;
import org.polypheny.db.plan.AlgTraitSet;
import org.polypheny.db.rex.RexCall;
import org.polypheny.db.rex.RexInputRef;
import org.polypheny.db.rex.RexLiteral;
import org.polypheny.db.rex.RexNode;
import org.polypheny.db.type.PolyType;
import org.polypheny.db.util.DateString;
import org.polypheny.db.util.TimeString;
import org.polypheny.db.util.TimestampString;
import org.polypheny.db.util.Util;


/**
 * Implementation of {@link Filter} relational expression in Geode.
 */
public class GeodeFilter extends Filter implements GeodeAlg {

    private final String match;


    GeodeFilter( AlgOptCluster cluster, AlgTraitSet traitSet, AlgNode input, RexNode condition ) {
        super( cluster, traitSet, input, condition );

        Translator translator = new Translator( getRowType() );
        this.match = translator.translateMatch( condition );

        assert getConvention() == GeodeAlg.CONVENTION;
        assert getConvention() == input.getConvention();
    }


    @Override
    public AlgOptCost computeSelfCost( AlgOptPlanner planner, AlgMetadataQuery mq ) {
        return super.computeSelfCost( planner, mq ).multiplyBy( 0.1 );
    }


    @Override
    public GeodeFilter copy( AlgTraitSet traitSet, AlgNode input, RexNode condition ) {
        return new GeodeFilter( getCluster(), traitSet, input, condition );
    }


    @Override
    public void implement( GeodeImplementContext geodeImplementContext ) {
        // first call the input down the tree.
        geodeImplementContext.visitChild( getInput() );
        geodeImplementContext.addPredicates( Collections.singletonList( match ) );
    }


    /**
     * Translates {@link RexNode} expressions into Geode expression strings.
     */
    static class Translator {

        private final AlgDataType rowType;
        private final List<String> fieldNames;


        Translator( AlgDataType rowType ) {
            this.rowType = rowType;
            this.fieldNames = GeodeRules.geodeFieldNames( rowType );
        }


        /**
         * Converts the value of a literal to a string.
         *
         * @param literal Literal to translate
         * @return String representation of the literal
         */
        private static String literalValue( RexLiteral literal ) {
            final Comparable valueComparable = literal.getValueAs( Comparable.class );

            switch ( literal.getTypeName() ) {
                case TIMESTAMP:
                case TIMESTAMP_WITH_LOCAL_TIME_ZONE:
                    assert valueComparable instanceof TimestampString;
                    return "TIMESTAMP '" + valueComparable.toString() + "'";
                case DATE:
                    assert valueComparable instanceof DateString;
                    return "DATE '" + valueComparable.toString() + "'";
                case TIME:
                case TIME_WITH_LOCAL_TIME_ZONE:
                    assert valueComparable instanceof TimeString;
                    return "TIME '" + valueComparable.toString() + "'";
                default:
                    return String.valueOf( literal.getValue3() );
            }
        }


        /**
         * Produce the OQL predicate string for the given condition.
         *
         * @param condition Condition to translate
         * @return OQL predicate string
         */
        private String translateMatch( RexNode condition ) {
            // Returns condition decomposed by OR
            List<RexNode> disjunctions = AlgOptUtil.disjunctions( condition );
            if ( disjunctions.size() == 1 ) {
                return translateAnd( disjunctions.get( 0 ) );
            } else {
                return translateOr( disjunctions );
            }
        }


        /**
         * Translate a conjunctive predicate to a OQL string.
         *
         * @param condition A conjunctive predicate
         * @return OQL string for the predicate
         */
        private String translateAnd( RexNode condition ) {
            List<String> predicates = new ArrayList<>();
            for ( RexNode node : AlgOptUtil.conjunctions( condition ) ) {
                predicates.add( translateMatch2( node ) );
            }

            return Util.toString( predicates, "", " AND ", "" );
        }


        /**
         * Get the field name for the left node to use for IN SET query
         */
        private String getLeftNodeFieldName( RexNode left ) {
            switch ( left.getKind() ) {
                case INPUT_REF:
                    final RexInputRef left1 = (RexInputRef) left;
                    return fieldNames.get( left1.getIndex() );
                case CAST:
                    // FIXME This will not work in all cases (for example, we ignore string encoding)
                    return getLeftNodeFieldName( ((RexCall) left).operands.get( 0 ) );
                case OTHER_FUNCTION:
                    return left.accept( new GeodeRules.RexToGeodeTranslator( this.fieldNames ) );
                default:
                    return null;
            }
        }


        /**
         * Check if we can use IN SET Query clause to improve query performance
         */
        private boolean useInSetQueryClause( List<RexNode> disjunctions ) {
            // Only use the in set for more than one disjunctions
            if ( disjunctions.size() <= 1 ) {
                return false;
            }

            return disjunctions.stream().allMatch( node -> {
                // IN SET query can only be used for EQUALS
                if ( node.getKind() != Kind.EQUALS ) {
                    return false;
                }

                RexCall call = (RexCall) node;
                final RexNode left = call.operands.get( 0 );
                final RexNode right = call.operands.get( 1 );

                // The right node should always be literal
                if ( right.getKind() != Kind.LITERAL ) {
                    return false;
                }

                String name = getLeftNodeFieldName( left );
                if ( name == null ) {
                    return false;
                }

                return true;
            } );
        }


        /**
         * Creates OQL IN SET predicate string
         */
        private String translateInSet( List<RexNode> disjunctions ) {
            Preconditions.checkArgument( !disjunctions.isEmpty(), "empty disjunctions" );

            RexNode firstNode = disjunctions.get( 0 );
            RexCall firstCall = (RexCall) firstNode;

            final RexNode left = firstCall.operands.get( 0 );
            String name = getLeftNodeFieldName( left );

            Set<String> rightLiteralValueList = new LinkedHashSet<>();

            disjunctions.forEach( node -> {
                RexCall call = (RexCall) node;
                RexLiteral rightLiteral = (RexLiteral) call.operands.get( 1 );

                rightLiteralValueList.add( quoteCharLiteral( rightLiteral ) );
            } );

            return String.format( Locale.ROOT, "%s IN SET(%s)", name, String.join( ", ", rightLiteralValueList ) );
        }


        private String getLeftNodeFieldNameForNode( RexNode node ) {
            final RexCall call = (RexCall) node;
            final RexNode left = call.operands.get( 0 );
            return getLeftNodeFieldName( left );
        }


        private List<RexNode> getLeftNodeDisjunctions( RexNode node, List<RexNode> disjunctions ) {
            List<RexNode> leftNodeDisjunctions = new ArrayList<>();
            String leftNodeFieldName = getLeftNodeFieldNameForNode( node );

            if ( leftNodeFieldName != null ) {
                leftNodeDisjunctions = disjunctions.stream().filter( rexNode -> {
                    RexCall rexCall = (RexCall) rexNode;
                    RexNode rexCallLeft = rexCall.operands.get( 0 );
                    return leftNodeFieldName.equals( getLeftNodeFieldName( rexCallLeft ) );
                } ).collect( Collectors.toList() );
            }

            return leftNodeDisjunctions;
        }


        private String translateOr( List<RexNode> disjunctions ) {
            List<String> predicates = new ArrayList<>();

            List<String> leftFieldNameList = new ArrayList<>();
            List<String> inSetLeftFieldNameList = new ArrayList<>();

            for ( RexNode node : disjunctions ) {
                final String leftNodeFieldName = getLeftNodeFieldNameForNode( node );
                // If any one left node is processed with IN SET predicate all the nodes are already handled
                if ( inSetLeftFieldNameList.contains( leftNodeFieldName ) ) {
                    continue;
                }

                List<RexNode> leftNodeDisjunctions = new ArrayList<>();
                boolean useInSetQueryClause = false;

                // In case the left field node name is already processed and not applicable for IN SET query clause, we can skip the checking
                if ( !leftFieldNameList.contains( leftNodeFieldName ) ) {
                    leftNodeDisjunctions = getLeftNodeDisjunctions( node, disjunctions );
                    useInSetQueryClause = useInSetQueryClause( leftNodeDisjunctions );
                }

                if ( useInSetQueryClause ) {
                    predicates.add( translateInSet( leftNodeDisjunctions ) );
                    inSetLeftFieldNameList.add( leftNodeFieldName );
                } else if ( AlgOptUtil.conjunctions( node ).size() > 1 ) {
                    predicates.add( "(" + translateMatch( node ) + ")" );
                } else {
                    predicates.add( translateMatch2( node ) );
                }
                leftFieldNameList.add( leftNodeFieldName );
            }

            return Util.toString( predicates, "", " OR ", "" );
        }


        /**
         * Translate a binary relation.
         */
        private String translateMatch2( RexNode node ) {
            // We currently only use equality, but inequalities on clustering keys should be possible in the future
            switch ( node.getKind() ) {
                case EQUALS:
                    return translateBinary( "=", "=", (RexCall) node );
                case LESS_THAN:
                    return translateBinary( "<", ">", (RexCall) node );
                case LESS_THAN_OR_EQUAL:
                    return translateBinary( "<=", ">=", (RexCall) node );
                case GREATER_THAN:
                    return translateBinary( ">", "<", (RexCall) node );
                case GREATER_THAN_OR_EQUAL:
                    return translateBinary( ">=", "<=", (RexCall) node );
                default:
                    throw new AssertionError( "cannot translate " + node );
            }
        }


        /**
         * Translates a call to a binary operator, reversing arguments if necessary.
         */
        private String translateBinary( String op, String rop, RexCall call ) {
            final RexNode left = call.operands.get( 0 );
            final RexNode right = call.operands.get( 1 );
            String expression = translateBinary2( op, left, right );
            if ( expression != null ) {
                return expression;
            }
            expression = translateBinary2( rop, right, left );
            if ( expression != null ) {
                return expression;
            }
            throw new AssertionError( "cannot translate op " + op + " call " + call );
        }


        /**
         * Translates a call to a binary operator. Returns null on failure.
         */
        private String translateBinary2( String op, RexNode left, RexNode right ) {
            switch ( right.getKind() ) {
                case LITERAL:
                    break;
                default:
                    return null;
            }

            final RexLiteral rightLiteral = (RexLiteral) right;
            switch ( left.getKind() ) {
                case INPUT_REF:
                    final RexInputRef left1 = (RexInputRef) left;
                    String name = fieldNames.get( left1.getIndex() );
                    return translateOp2( op, name, rightLiteral );
                case CAST:
                    // FIXME This will not work in all cases (for example, we ignore string encoding)
                    return translateBinary2( op, ((RexCall) left).operands.get( 0 ), right );
                case OTHER_FUNCTION:
                    String item = left.accept( new GeodeRules.RexToGeodeTranslator( this.fieldNames ) );
                    return (item == null) ? null : item + " " + op + " " + quoteCharLiteral( rightLiteral );
                default:
                    return null;
            }
        }


        private String quoteCharLiteral( RexLiteral literal ) {
            String value = literalValue( literal );
            if ( literal.getTypeName() == PolyType.CHAR ) {
                value = "'" + value + "'";
            }
            return value;
        }


        /**
         * Combines a field name, operator, and literal to produce a predicate string.
         */
        private String translateOp2( String op, String name, RexLiteral right ) {
            String valueString = quoteCharLiteral( right );
            return name + " " + op + " " + valueString;
        }

    }

}

