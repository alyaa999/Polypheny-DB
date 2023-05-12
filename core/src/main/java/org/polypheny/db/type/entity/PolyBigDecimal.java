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

package org.polypheny.db.type.entity;

import java.math.BigDecimal;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.linq4j.tree.Expressions;
import org.jetbrains.annotations.NotNull;
import org.polypheny.db.type.PolySerializable;
import org.polypheny.db.type.PolyType;

@EqualsAndHashCode(callSuper = true)
@Value
public class PolyBigDecimal extends PolyNumber {

    public BigDecimal value;


    public PolyBigDecimal( BigDecimal value, boolean nullable ) {
        super( PolyType.DECIMAL, nullable );
        this.value = value;
    }


    public static PolyBigDecimal of( BigDecimal value ) {
        return new PolyBigDecimal( value, false );
    }


    @Override
    public int intValue() {
        return value.intValue();
    }


    @Override
    public long longValue() {
        return value.longValue();
    }


    @Override
    public double doubleValue() {
        return value.doubleValue();
    }


    @Override
    public BigDecimal bigDecimalValue() {
        return value;
    }


    @Override
    public int compareTo( @NotNull PolyValue o ) {
        if ( !isSameType( o ) ) {
            return -1;
        }

        return value.compareTo( o.asPolyBigDecimal().value );
    }


    @Override
    public Expression asExpression() {
        return Expressions.new_( PolyBigDecimal.class, Expressions.constant( value ) );
    }


    @Override
    public PolySerializable copy() {
        return PolySerializable.deserialize( serialize(), PolyBigDecimal.class );
    }

}