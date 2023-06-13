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
import lombok.EqualsAndHashCode;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.linq4j.tree.Expressions;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.polypheny.db.type.PolySerializable;
import org.polypheny.db.type.PolyType;
import org.polypheny.db.type.entity.category.PolyNumber;

@EqualsAndHashCode(callSuper = true)
public class PolyDouble extends PolyNumber {

    @Serialize
    public Double value;


    public PolyDouble( @Deserialize("value") Double value ) {
        super( PolyType.DOUBLE );
        this.value = value;
    }


    public static PolyDouble of( Double value ) {
        return new PolyDouble( value );
    }


    public static PolyDouble of( Number value ) {
        return new PolyDouble( value.doubleValue() );
    }


    @Override
    public int compareTo( @NotNull PolyValue o ) {
        if ( !o.isNumber() ) {
            return -1;
        }

        return ObjectUtils.compare( value, o.asNumber().DoubleValue() );
    }


    @Override
    public Expression asExpression() {
        return Expressions.new_( PolyDouble.class, Expressions.constant( value ) );
    }


    @Override
    public PolySerializable copy() {
        return PolySerializable.deserialize( serialize(), PolyDouble.class );
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
    public float floatValue() {
        return value.floatValue();
    }


    @Override
    public double doubleValue() {
        return value;
    }


    @Override
    public BigDecimal bigDecimalValue() {
        return BigDecimal.valueOf( value );
    }


    @Override
    public PolyDouble increment() {
        return PolyDouble.of( value + 1 );
    }


    @Override
    public @NotNull PolyDouble divide( @NotNull PolyNumber other ) {
        return PolyDouble.of( value / other.doubleValue() );
    }


    @Override
    public @NotNull PolyDouble multiply( @NotNull PolyNumber other ) {
        return PolyDouble.of( value * other.doubleValue() );
    }


    @Override
    public @NotNull PolyNumber plus( @NotNull PolyNumber b1 ) {
        return PolyDouble.of( value + b1.doubleValue() );
    }


    @Override
    public @NotNull PolyNumber subtract( @NotNull PolyNumber b1 ) {
        return PolyDouble.of( value - b1.doubleValue() );
    }


    @Override
    public PolyNumber negate() {
        return PolyDouble.of( -value );
    }


    public static class PolyDoubleSerializerDef extends SimpleSerializerDef<PolyDouble> {

        @Override
        protected BinarySerializer<PolyDouble> createSerializer( int version, CompatibilityLevel compatibilityLevel ) {
            return new BinarySerializer<>() {
                @Override
                public void encode( BinaryOutput out, PolyDouble item ) {
                    out.writeDouble( item.value );
                }


                @Override
                public PolyDouble decode( BinaryInput in ) throws CorruptedDataException {
                    return new PolyDouble( in.readDouble() );
                }
            };
        }

    }


    public static class PolyDoubleSerializer implements JsonDeserializer<PolyDouble>, JsonSerializer<PolyDouble> {

        @Override
        public PolyDouble deserialize( JsonElement json, Type typeOfT, JsonDeserializationContext context ) throws JsonParseException {
            return PolyDouble.of( json.getAsDouble() );
        }


        @Override
        public JsonElement serialize( PolyDouble src, Type typeOfSrc, JsonSerializationContext context ) {
            return new JsonPrimitive( src.value );
        }

    }


    @Override
    public String toString() {
        return value.toString();
    }

}
