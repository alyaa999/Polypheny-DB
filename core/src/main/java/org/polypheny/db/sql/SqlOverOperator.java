/*
 * Copyright 2019-2020 The Polypheny Project
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

package org.polypheny.db.sql;


import static org.polypheny.db.util.Static.RESOURCE;

import org.polypheny.db.rel.type.RelDataType;
import org.polypheny.db.sql.type.OperandTypes;
import org.polypheny.db.sql.type.ReturnTypes;
import org.polypheny.db.sql.util.SqlBasicVisitor;
import org.polypheny.db.sql.util.SqlVisitor;
import org.polypheny.db.sql.validate.SqlValidator;
import org.polypheny.db.sql.validate.SqlValidatorImpl;
import org.polypheny.db.sql.validate.SqlValidatorScope;
import org.apache.calcite.linq4j.Ord;


/**
 * An operator describing a window function specification.
 *
 * Operands are as follows:
 *
 * <ul>
 * <li>0: name of window function ({@link org.polypheny.db.sql.SqlCall})</li>
 * <li>1: window name ({@link org.polypheny.db.sql.SqlLiteral}) or window in-line specification ({@link SqlWindow})</li>
 * </ul>
 */
public class SqlOverOperator extends SqlBinaryOperator {


    public SqlOverOperator() {
        super(
                "OVER",
                SqlKind.OVER,
                20,
                true,
                ReturnTypes.ARG0_FORCE_NULLABLE,
                null,
                OperandTypes.ANY_ANY );
    }


    @Override
    public void validateCall( SqlCall call, SqlValidator validator, SqlValidatorScope scope, SqlValidatorScope operandScope ) {
        assert call.getOperator() == this;
        assert call.operandCount() == 2;
        SqlCall aggCall = call.operand( 0 );
        if ( !aggCall.getOperator().isAggregator() ) {
            throw validator.newValidationError( aggCall, RESOURCE.overNonAggregate() );
        }
        final SqlNode window = call.operand( 1 );
        validator.validateWindow( window, scope, aggCall );
    }


    @Override
    public RelDataType deriveType( SqlValidator validator, SqlValidatorScope scope, SqlCall call ) {
        // Validate type of the inner aggregate call
        validateOperands( validator, scope, call );

        // Assume the first operand is an aggregate call and derive its type.
        // When we are sure the window is not empty, pass that information to the aggregate's operator return type inference as groupCount=1
        // Otherwise pass groupCount=0 so the agg operator understands the window can be empty
        SqlNode agg = call.operand( 0 );

        if ( !(agg instanceof SqlCall) ) {
            throw new IllegalStateException( "Argument to SqlOverOperator should be SqlCall, got " + agg.getClass() + ": " + agg );
        }

        SqlNode window = call.operand( 1 );
        SqlWindow w = validator.resolveWindow( window, scope, false );

        final int groupCount = w.isAlwaysNonEmpty() ? 1 : 0;
        final SqlCall aggCall = (SqlCall) agg;

        SqlCallBinding opBinding = new SqlCallBinding( validator, scope, aggCall ) {
            @Override
            public int getGroupCount() {
                return groupCount;
            }
        };

        RelDataType ret = aggCall.getOperator().inferReturnType( opBinding );

        // Copied from validateOperands
        ((SqlValidatorImpl) validator).setValidatedNodeType( call, ret );
        ((SqlValidatorImpl) validator).setValidatedNodeType( agg, ret );
        return ret;
    }


    /**
     * Accepts a {@link SqlVisitor}, and tells it to visit each child.
     *
     * @param visitor Visitor
     */
    @Override
    public <R> void acceptCall( SqlVisitor<R> visitor, SqlCall call, boolean onlyExpressions, SqlBasicVisitor.ArgHandler<R> argHandler ) {
        if ( onlyExpressions ) {
            for ( Ord<SqlNode> operand : Ord.zip( call.getOperandList() ) ) {
                // If the second param is an Identifier then it's supposed to be a name from a window clause and isn't part of the group by check
                if ( operand == null ) {
                    continue;
                }
                if ( operand.i == 1 && operand.e instanceof SqlIdentifier ) {
                    continue;
                }
                argHandler.visitChild( visitor, call, operand.i, operand.e );
            }
        } else {
            super.acceptCall( visitor, call, onlyExpressions, argHandler );
        }
    }
}
