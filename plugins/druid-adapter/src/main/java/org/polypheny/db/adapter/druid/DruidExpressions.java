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


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Chars;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import javax.annotation.Nullable;
import org.polypheny.db.algebra.constant.Kind;
import org.polypheny.db.algebra.type.AlgDataType;
import org.polypheny.db.nodes.Operator;
import org.polypheny.db.rex.RexCall;
import org.polypheny.db.rex.RexInputRef;
import org.polypheny.db.rex.RexLiteral;
import org.polypheny.db.rex.RexNode;
import org.polypheny.db.type.PolyType;
import org.polypheny.db.type.PolyTypeFamily;


/**
 * Expression utility class to transform Polypheny-DB expressions to Druid expressions when possible.
 */
public class DruidExpressions {

    /**
     * Type mapping between Polypheny-DB SQL family types and native Druid expression types
     */
    static final Map<PolyType, DruidType> EXPRESSION_TYPES;
    /**
     * Druid expression safe chars, must be sorted.
     */
    private static final char[] SAFE_CHARS = " ,._-;:(){}[]<>!@#$%^&*`~?/".toCharArray();


    static {
        final ImmutableMap.Builder<PolyType, DruidType> builder = ImmutableMap.builder();

        for ( PolyType type : PolyType.FRACTIONAL_TYPES ) {
            builder.put( type, DruidType.DOUBLE );
        }

        for ( PolyType type : PolyType.INT_TYPES ) {
            builder.put( type, DruidType.LONG );
        }

        for ( PolyType type : PolyType.STRING_TYPES ) {
            builder.put( type, DruidType.STRING );
        }

        // booleans in expressions are returned from druid as long. Druid will return 0 for false, non-zero value for true and null for absent value.
        for ( PolyType type : PolyType.BOOLEAN_TYPES ) {
            builder.put( type, DruidType.LONG );
        }

        // Timestamps are treated as longs (millis since the epoch) in Druid expressions.
        builder.put( PolyType.TIMESTAMP, DruidType.LONG );
        builder.put( PolyType.DATE, DruidType.LONG );
        builder.put( PolyType.TIMESTAMP_WITH_LOCAL_TIME_ZONE, DruidType.LONG );
        builder.put( PolyType.OTHER, DruidType.COMPLEX );
        EXPRESSION_TYPES = builder.build();
        // Safe chars must be sorted
        Arrays.sort( SAFE_CHARS );
    }


    private DruidExpressions() {
    }


    /**
     * Translates Polypheny-DB rexNode to Druid Expression when possible
     *
     * @param rexNode rexNode to convert to a Druid Expression
     * @param inputRowType input row type of the rexNode to translate
     * @param druidRel Druid query
     * @return Druid Expression or null when can not convert the RexNode
     */
    @Nullable
    public static String toDruidExpression( final RexNode rexNode, final AlgDataType inputRowType, final DruidQuery druidRel ) {
        Kind kind = rexNode.getKind();
        PolyType polyType = rexNode.getType().getPolyType();

        if ( kind == Kind.INPUT_REF ) {
            final RexInputRef ref = (RexInputRef) rexNode;
            final String columnName = inputRowType.getFieldNames().get( ref.getIndex() );
            if ( columnName == null ) {
                return null;
            }
            if ( druidRel.getDruidTable().timestampFieldName.equals( columnName ) ) {
                return DruidExpressions.fromColumn( DruidTable.DEFAULT_TIMESTAMP_COLUMN );
            }
            return DruidExpressions.fromColumn( columnName );
        }

        if ( rexNode instanceof RexCall ) {
            final Operator operator = ((RexCall) rexNode).getOperator();
            final DruidSqlOperatorConverter conversion = druidRel.getOperatorConversionMap().get( operator );
            if ( conversion == null ) {
                //unknown operator can not translate
                return null;
            } else {
                return conversion.toDruidExpression( rexNode, inputRowType, druidRel );
            }
        }
        if ( kind == Kind.LITERAL ) {
            // Translate literal.
            if ( RexLiteral.isNullLiteral( rexNode ) ) {
                //case the filter/project might yield to unknown let Polypheny-DB deal with this for now
                return null;
            } else if ( PolyType.NUMERIC_TYPES.contains( polyType ) ) {
                return DruidExpressions.numberLiteral( (Number) RexLiteral.value( rexNode ) );
            } else if ( PolyTypeFamily.INTERVAL_DAY_TIME == polyType.getFamily() ) {
                // Polypheny-DB represents DAY-TIME intervals in milliseconds.
                final long milliseconds = ((Number) RexLiteral.value( rexNode )).longValue();
                return DruidExpressions.numberLiteral( milliseconds );
            } else if ( PolyTypeFamily.INTERVAL_YEAR_MONTH == polyType.getFamily() ) {
                // Polypheny-DB represents YEAR-MONTH intervals in months.
                final long months = ((Number) RexLiteral.value( rexNode )).longValue();
                return DruidExpressions.numberLiteral( months );
            } else if ( PolyType.STRING_TYPES.contains( polyType ) ) {
                return DruidExpressions.stringLiteral( RexLiteral.stringValue( rexNode ) );
            } else if ( PolyType.DATE == polyType || PolyType.TIMESTAMP == polyType || PolyType.TIME_WITH_LOCAL_TIME_ZONE == polyType ) {
                return DruidExpressions.numberLiteral( DruidDateTimeUtils.literalValue( rexNode ) );
            } else if ( PolyType.BOOLEAN == polyType ) {
                return DruidExpressions.numberLiteral( RexLiteral.booleanValue( rexNode ) ? 1 : 0 );
            }
        }
        // Not Literal/InputRef/RexCall or unknown type?
        return null;
    }


    public static String fromColumn( String columnName ) {
        return DruidQuery.format( "\"%s\"", columnName );
    }


    public static String nullLiteral() {
        return "null";
    }


    public static String numberLiteral( final Number n ) {
        return n == null ? nullLiteral() : n.toString();
    }


    public static String stringLiteral( final String s ) {
        return s == null ? nullLiteral() : "'" + escape( s ) + "'";
    }


    private static String escape( final String s ) {
        final StringBuilder escaped = new StringBuilder();
        for ( int i = 0; i < s.length(); i++ ) {
            final char c = s.charAt( i );
            if ( Character.isLetterOrDigit( c ) || Arrays.binarySearch( SAFE_CHARS, c ) >= 0 ) {
                escaped.append( c );
            } else {
                escaped.append( "\\u" ).append( BaseEncoding.base16().encode( Chars.toByteArray( c ) ) );
            }
        }
        return escaped.toString();
    }


    public static String functionCall( final String functionName, final List<String> args ) {
        Objects.requireNonNull( functionName, "druid functionName" );
        Objects.requireNonNull( args, "args" );

        final StringBuilder builder = new StringBuilder( functionName );
        builder.append( "(" );
        for ( int i = 0; i < args.size(); i++ ) {
            int finalI = i;
            final String arg = Objects.requireNonNull( args.get( i ), () -> "arg #" + finalI );
            builder.append( arg );
            if ( i < args.size() - 1 ) {
                builder.append( "," );
            }
        }
        builder.append( ")" );
        return builder.toString();
    }


    public static String nAryOperatorCall( final String druidOperator, final List<String> args ) {
        Objects.requireNonNull( druidOperator, "druid operator missing" );
        Objects.requireNonNull( args, "args" );
        final StringBuilder builder = new StringBuilder();
        builder.append( "(" );
        for ( int i = 0; i < args.size(); i++ ) {
            int finalI = i;
            final String arg = Objects.requireNonNull( args.get( i ), () -> "arg #" + finalI );
            builder.append( arg );
            if ( i < args.size() - 1 ) {
                builder.append( druidOperator );
            }
        }
        builder.append( ")" );
        return builder.toString();
    }


    /**
     * Translate a list of Polypheny-DB {@code RexNode} to Druid expressions.
     *
     * @param rexNodes list of Polypheny-DB expressions meant to be applied on top of the rows
     * @return list of Druid expressions in the same order as rexNodes, or null if not possible. If a non-null list is returned, all elements will be non-null.
     */
    @Nullable
    public static List<String> toDruidExpressions( final DruidQuery druidRel, final AlgDataType rowType, final List<RexNode> rexNodes ) {
        final List<String> retVal = new ArrayList<>( rexNodes.size() );
        for ( RexNode rexNode : rexNodes ) {
            final String druidExpression = toDruidExpression( rexNode, rowType, druidRel );
            if ( druidExpression == null ) {
                return null;
            }

            retVal.add( druidExpression );
        }
        return retVal;
    }


    public static String applyTimestampFloor( final String input, final String granularity, final String origin, final TimeZone timeZone ) {
        Objects.requireNonNull( input, "input" );
        Objects.requireNonNull( granularity, "granularity" );
        return DruidExpressions.functionCall( "timestamp_floor", ImmutableList.of( input, DruidExpressions.stringLiteral( granularity ), DruidExpressions.stringLiteral( origin ), DruidExpressions.stringLiteral( timeZone.getID() ) ) );
    }


    public static String applyTimestampCeil( final String input, final String granularity, final String origin, final TimeZone timeZone ) {
        Objects.requireNonNull( input, "input" );
        Objects.requireNonNull( granularity, "granularity" );
        return DruidExpressions.functionCall( "timestamp_ceil", ImmutableList.of( input, DruidExpressions.stringLiteral( granularity ), DruidExpressions.stringLiteral( origin ), DruidExpressions.stringLiteral( timeZone.getID() ) ) );
    }


    public static String applyTimeExtract( String timeExpression, String druidUnit, TimeZone timeZone ) {
        return DruidExpressions.functionCall( "timestamp_extract", ImmutableList.of( timeExpression, DruidExpressions.stringLiteral( druidUnit ), DruidExpressions.stringLiteral( timeZone.getID() ) ) );
    }

}

