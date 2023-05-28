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

import io.activej.serializer.BinaryInput;
import io.activej.serializer.BinaryOutput;
import io.activej.serializer.BinarySerializer;
import io.activej.serializer.CompatibilityLevel;
import io.activej.serializer.CorruptedDataException;
import io.activej.serializer.SimpleSerializerDef;
import io.activej.serializer.annotations.Deserialize;
import io.activej.serializer.annotations.Serialize;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.linq4j.tree.Expressions;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.polypheny.db.type.PolySerializable;
import org.polypheny.db.type.PolyType;

@EqualsAndHashCode(callSuper = true)
@Value
public class PolyString extends PolyValue {


    @Serialize
    public String value;


    public PolyString( @Deserialize("value") String value ) {
        super( PolyType.VARCHAR );
        this.value = value;
    }


    public static PolyString of( String value ) {
        return new PolyString( value );
    }


    @Override
    public Expression asExpression() {
        return Expressions.new_( PolyString.class, Expressions.constant( value ) );
    }


    @Override
    public int compareTo( @NotNull PolyValue o ) {
        if ( !isSameType( o ) ) {
            return -1;
        }

        return ObjectUtils.compare( value, o.asString().value );
    }


    @Override
    public PolySerializable copy() {
        return null;
    }


    public static class PolyStringSerializerDef extends SimpleSerializerDef<PolyString> {

        @Override
        protected BinarySerializer<PolyString> createSerializer( int version, CompatibilityLevel compatibilityLevel ) {
            return new BinarySerializer<>() {
                @Override
                public void encode( BinaryOutput out, PolyString item ) {
                    out.writeUTF8( item.value );
                }


                @Override
                public PolyString decode( BinaryInput in ) throws CorruptedDataException {
                    return PolyString.of( in.readUTF8() );
                }
            };
        }

    }


    @Override
    public String toString() {
        return value;
    }

}
