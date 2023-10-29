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

package org.polypheny.db.adapter.mongodb.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Value;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.linq4j.tree.Expressions;
import org.jetbrains.annotations.Nullable;
import org.polypheny.db.algebra.enumerable.EnumUtils;
import org.polypheny.db.algebra.type.AlgDataType;
import org.polypheny.db.algebra.type.AlgDataTypeField;
import org.polypheny.db.catalog.impl.Expressible;
import org.polypheny.db.type.PolyType;

@Value
public class MongoTupleType implements Expressible {

    public List<MongoTupleType> subs;
    public String name;
    public PolyType type;
    public boolean nullable;


    public MongoTupleType( @Nullable String name, PolyType type, List<MongoTupleType> subs, boolean nullable ) {
        this.type = type;
        this.subs = subs;
        this.nullable = nullable;
        this.name = name;
    }


    public MongoTupleType( MongoTupleType type ) {
        this( type.name, type.type, List.of( type ), type.nullable );
    }


    public static MongoTupleType from( AlgDataType type ) {
        if ( !type.isStruct() ) {
            return from( (AlgDataTypeField) type );
        }

        List<MongoTupleType> types = new ArrayList<>();
        switch ( type.getPolyType() ) {
            case ROW:
                type.getFieldList().forEach( field -> types.add( from( field ) ) );
        }
        return new MongoTupleType( null, type.getPolyType(), types, type.isNullable() );
    }


    private static MongoTupleType from( AlgDataTypeField field ) {
        List<MongoTupleType> types = new ArrayList<>();
        switch ( field.getType().getPolyType() ) {
            case ROW:
                //f.forEach( field -> types.add( from( field ) ) );
        }
        return new MongoTupleType( field.getName(), field.getType().getPolyType(), types, field.getType().isNullable() );
    }


    @Override
    public Expression asExpression() {
        return Expressions.new_(
                MongoTupleType.class,
                Expressions.constant( name ),
                Expressions.constant( type ),
                EnumUtils.expressionList( subs.stream().map( MongoTupleType::asExpression ).collect( Collectors.toList() ) ),
                Expressions.constant( nullable ) );
    }


    public int size() {
        return subs.size();
    }


    public boolean isStruct() {
        switch ( type ) {
            case ROW:
                return true;
        }
        return false;
    }

}