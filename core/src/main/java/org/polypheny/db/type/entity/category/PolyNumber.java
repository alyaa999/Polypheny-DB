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

package org.polypheny.db.type.entity.category;

import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.polypheny.db.type.PolyType;
import org.polypheny.db.type.entity.PolyBigDecimal;
import org.polypheny.db.type.entity.PolyValue;

@Slf4j
public abstract class PolyNumber extends PolyValue {

    public PolyNumber( PolyType type ) {
        super( type );
    }


    /**
     * Returns the value of the specified number as an {@code int}, which may involve rounding or truncation.
     *
     * @return the numeric value represented by this object after conversion to type {@code int}.
     */
    public abstract int intValue();


    public Integer IntValue() {
        if ( isNull() ) {
            return null;
        }
        return intValue();
    }


    /**
     * Returns the value of the specified number as an {@code long}, which may involve rounding or truncation.
     *
     * @return the numeric value represented by this object after conversion to type {@code long}.
     */
    public abstract long longValue();


    public Long LongValue() {
        if ( isNull() ) {
            return null;
        }
        return longValue();
    }


    /**
     * Returns the value of the specified number as an {@code float}, which may involve rounding or truncation.
     *
     * @return the numeric value represented by this object after conversion to type {@code float}.
     */
    public abstract float floatValue();


    public Float FloatValue() {
        if ( isNull() ) {
            return null;
        }
        return floatValue();
    }


    /**
     * Returns the value of the specified number as a {@code double}, which may involve rounding.
     *
     * @return the numeric value represented by this object after conversion to type {@code double}.
     */
    public abstract double doubleValue();


    public Double DoubleValue() {
        if ( isNull() ) {
            return null;
        }
        return doubleValue();
    }


    /**
     * Returns the value of the specified number as a {@code BigDecimal}, which may involve rounding.
     *
     * @return the numeric value represented by this object after conversion to type {@code BigDecimal}.
     */
    public abstract BigDecimal bigDecimalValue();


    public BigDecimal BigDecimalValue() {
        if ( isNull() ) {
            return null;
        }
        return bigDecimalValue();
    }


    public abstract PolyNumber increment();

    public abstract PolyNumber divide( PolyNumber other );


    public abstract PolyNumber multiply( PolyNumber other );


    public abstract PolyNumber plus( PolyNumber b1 );


    public PolyNumber floor( PolyNumber b1 ) {
        log.warn( "optimize" );
        final BigDecimal[] bigDecimals = bigDecimalValue().divideAndRemainder( b1.bigDecimalValue() );
        BigDecimal r = bigDecimals[1];
        if ( r.signum() < 0 ) {
            r = r.add( b1.bigDecimalValue() );
        }
        return PolyBigDecimal.of( bigDecimalValue().subtract( r ) );
    }

}
