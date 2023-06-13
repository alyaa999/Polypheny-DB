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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import io.activej.serializer.BinaryInput;
import io.activej.serializer.BinaryOutput;
import io.activej.serializer.BinarySerializer;
import io.activej.serializer.CompatibilityLevel;
import io.activej.serializer.CorruptedDataException;
import io.activej.serializer.SimpleSerializerDef;
import io.activej.serializer.annotations.Deserialize;
import io.activej.serializer.annotations.Serialize;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;
import lombok.Value;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.linq4j.tree.Expressions;
import org.jetbrains.annotations.NotNull;
import org.polypheny.db.catalog.exceptions.GenericRuntimeException;
import org.polypheny.db.type.PolySerializable;
import org.polypheny.db.type.PolyType;
import org.polypheny.db.type.entity.category.PolyNumber;

@Value
public class PolyInteger extends PolyNumber {

    @Serialize
    public Integer value;


    public PolyInteger( @Deserialize("value") Integer value ) {
        super( PolyType.INTEGER );
        this.value = value;
    }


    public static PolyInteger of( short value ) {
        return new PolyInteger( (int) value );
    }


    public static PolyInteger of( int value ) {
        return new PolyInteger( value );
    }


    public static PolyInteger of( Integer value ) {
        return new PolyInteger( value );
    }


    public static PolyInteger of( Number value ) {
        if ( value == null ) {
            return null;
        }
        return new PolyInteger( value.intValue() );
    }


    @Override
    public Expression asExpression() {
        return Expressions.new_( PolyInteger.class, Expressions.constant( value ) );
    }


    @Override
    public boolean equals( Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( !(o instanceof PolyValue) ) {
            return false;
        }
        PolyValue val = (PolyValue) o;

        if ( val.isNumber() ) {
            return Objects.equals( value, val.asNumber().intValue() );
        }

        return false;
    }


    @Override
    public int hashCode() {
        return Objects.hash( super.hashCode(), value );
    }


    @Override
    public int compareTo( @NotNull PolyValue o ) {
        if ( !o.isNumber() ) {
            return -1;
        }

        return this.value.compareTo( o.asNumber().intValue() );
    }


    @Override
    public PolySerializable copy() {
        return PolySerializable.deserialize( serialize(), PolyInteger.class );
    }


    @Override
    public boolean isNull() {
        return value == null;
    }


    @Override
    public int intValue() {
        return value;
    }


    public static PolyValue from( PolyValue value ) {
        if ( PolyType.NUMERIC_TYPES.contains( value.type ) ) {
            return PolyInteger.of( value.asNumber().intValue() );
        }

        throw new GenericRuntimeException( String.format( "%s does not support conversion to %s.", value, value.type ) );
    }


    @Override
    public long longValue() {
        return value;
    }


    @Override
    public float floatValue() {
        return value.floatValue();
    }


    @Override
    public double doubleValue() {
        return value;
    }


    @Override
    public BigDecimal bigDecimalValue() {
        return new BigDecimal( value );
    }


    @Override
    public PolyInteger increment() {
        return PolyInteger.of( value + 1 );
    }


    @Override
    public @NotNull PolyNumber divide( @NotNull PolyNumber other ) {
        return PolyBigDecimal.of( bigDecimalValue().divide( other.bigDecimalValue(), MathContext.DECIMAL64 ) );
    }


    @Override
    public @NotNull PolyNumber multiply( @NotNull PolyNumber other ) {
        return other.isDecimal() ? PolyBigDecimal.of( bigDecimalValue().multiply( other.bigDecimalValue() ) ) : PolyInteger.of( value * other.intValue() );
    }


    @Override
    public @NotNull PolyNumber plus( @NotNull PolyNumber other ) {
        return other.isDecimal() ? PolyBigDecimal.of( bigDecimalValue().add( other.bigDecimalValue() ) ) : PolyInteger.of( value + other.intValue() );
    }


    @Override
    public @NotNull PolyNumber subtract( @NotNull PolyNumber other ) {
        return other.isDecimal() ? PolyBigDecimal.of( bigDecimalValue().subtract( other.bigDecimalValue() ) ) : PolyInteger.of( value - other.intValue() );
    }


    @Override
    public PolyNumber negate() {
        return PolyInteger.of( -value );
    }


    public static class PolyIntegerSerializerDef extends SimpleSerializerDef<PolyInteger> {

        @Override
        protected BinarySerializer<PolyInteger> createSerializer( int version, CompatibilityLevel compatibilityLevel ) {
            return new BinarySerializer<>() {
                @Override
                public void encode( BinaryOutput out, PolyInteger item ) {
                    out.writeInt( item.value );
                }


                @Override
                public PolyInteger decode( BinaryInput in ) throws CorruptedDataException {
                    return new PolyInteger( in.readInt() );
                }
            };
        }

    }


    public static class PolyIntegerSerializer implements JsonSerializer<PolyInteger>, JsonDeserializer<PolyInteger> {

        @Override
        public PolyInteger deserialize( JsonElement json, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException {
            return PolyInteger.of( json.getAsInt() );
        }


        @Override
        public JsonElement serialize( PolyInteger src, Type typeOfSrc, JsonSerializationContext context ) {
            return new JsonPrimitive( src.value );
        }

    }


    @Override
    public String toString() {
        return value.toString();
    }

}
