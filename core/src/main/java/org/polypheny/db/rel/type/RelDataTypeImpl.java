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

package org.polypheny.db.rel.type;


import org.polypheny.db.sql.SqlCollation;
import org.polypheny.db.sql.SqlIdentifier;
import org.polypheny.db.sql.SqlIntervalQualifier;
import org.polypheny.db.sql.parser.SqlParserPos;
import org.polypheny.db.sql.type.BasicSqlType;
import org.polypheny.db.sql.type.SqlTypeName;
import org.polypheny.db.util.Pair;
import org.polypheny.db.util.Util;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


/**
 * RelDataTypeImpl is an abstract base for implementations of {@link RelDataType}.
 *
 * Identity is based upon the {@link #digest} field, which each derived class should set during construction.
 */
public abstract class RelDataTypeImpl implements RelDataType, RelDataTypeFamily {

    protected final List<RelDataTypeField> fieldList;
    protected String digest;


    /**
     * Creates a RelDataTypeImpl.
     *
     * @param fieldList List of fields
     */
    protected RelDataTypeImpl( List<? extends RelDataTypeField> fieldList ) {
        if ( fieldList != null ) {
            // Create a defensive copy of the list.
            this.fieldList = ImmutableList.copyOf( fieldList );
        } else {
            this.fieldList = null;
        }
    }


    /**
     * Default constructor, to allow derived classes such as {@link BasicSqlType} to be {@link Serializable}.
     *
     * (The serialization specification says that a class can be serializable even if its base class is not serializable, provided that the base class has a public or protected zero-args constructor.)
     */
    protected RelDataTypeImpl() {
        this( null );
    }


    @Override
    public RelDataTypeField getField( String fieldName, boolean caseSensitive, boolean elideRecord ) {
        for ( RelDataTypeField field : fieldList ) {
            if ( Util.matches( caseSensitive, field.getName(), fieldName ) ) {
                return field;
            }
        }
        if ( elideRecord ) {
            final List<Slot> slots = new ArrayList<>();
            getFieldRecurse( slots, this, 0, fieldName, caseSensitive );
            loop:
            for ( Slot slot : slots ) {
                switch ( slot.count ) {
                    case 0:
                        break; // no match at this depth; try deeper
                    case 1:
                        return slot.field;
                    default:
                        break loop; // duplicate fields at this depth; abandon search
                }
            }
        }
        // Extra field
        if ( fieldList.size() > 0 ) {
            final RelDataTypeField lastField = Iterables.getLast( fieldList );
            if ( lastField.getName().equals( "_extra" ) ) {
                return new RelDataTypeFieldImpl( fieldName, -1, lastField.getType() );
            }
        }

        // a dynamic * field will match any field name.
        for ( RelDataTypeField field : fieldList ) {
            if ( field.isDynamicStar() ) {
                // the requested field could be in the unresolved star
                return field;
            }
        }

        return null;
    }


    private static void getFieldRecurse( List<Slot> slots, RelDataType type, int depth, String fieldName, boolean caseSensitive ) {
        while ( slots.size() <= depth ) {
            slots.add( new Slot() );
        }
        final Slot slot = slots.get( depth );
        for ( RelDataTypeField field : type.getFieldList() ) {
            if ( Util.matches( caseSensitive, field.getName(), fieldName ) ) {
                slot.count++;
                slot.field = field;
            }
        }
        // No point looking to depth + 1 if there is a hit at depth.
        if ( slot.count == 0 ) {
            for ( RelDataTypeField field : type.getFieldList() ) {
                if ( field.getType().isStruct() ) {
                    getFieldRecurse( slots, field.getType(), depth + 1, fieldName, caseSensitive );
                }
            }
        }
    }


    @Override
    public List<RelDataTypeField> getFieldList() {
        assert isStruct();
        return fieldList;
    }


    @Override
    public List<String> getFieldNames() {
        return Pair.left( fieldList );
    }


    public List<String> getPhysicalFieldNames() {
        // TODO MV: Is there a more efficient way for doing this?
        List<String> l = new ArrayList<>();
        fieldList.forEach( f -> l.add( f.getPhysicalName() ) );
        return l;
    }


    @Override
    public int getFieldCount() {
        assert isStruct() : this;
        return fieldList.size();
    }


    @Override
    public StructKind getStructKind() {
        return isStruct() ? StructKind.FULLY_QUALIFIED : StructKind.NONE;
    }


    @Override
    public RelDataType getComponentType() {
        // this is not a collection type
        return null;
    }


    @Override
    public RelDataType getKeyType() {
        // this is not a map type
        return null;
    }


    @Override
    public RelDataType getValueType() {
        // this is not a map type
        return null;
    }


    @Override
    public boolean isStruct() {
        return fieldList != null;
    }


    @Override
    public boolean equals( Object obj ) {
        if ( obj instanceof RelDataTypeImpl ) {
            final RelDataTypeImpl that = (RelDataTypeImpl) obj;
            return this.digest.equals( that.digest );
        }
        return false;
    }


    @Override
    public int hashCode() {
        return digest.hashCode();
    }


    @Override
    public String getFullTypeString() {
        return digest;
    }


    @Override
    public boolean isNullable() {
        return false;
    }


    @Override
    public Charset getCharset() {
        return null;
    }


    @Override
    public SqlCollation getCollation() {
        return null;
    }


    @Override
    public SqlIntervalQualifier getIntervalQualifier() {
        return null;
    }


    @Override
    public int getPrecision() {
        return PRECISION_NOT_SPECIFIED;
    }


    @Override
    public int getScale() {
        return SCALE_NOT_SPECIFIED;
    }


    @Override
    public SqlTypeName getSqlTypeName() {
        return null;
    }


    @Override
    public SqlIdentifier getSqlIdentifier() {
        SqlTypeName typeName = getSqlTypeName();
        if ( typeName == null ) {
            return null;
        }
        return new SqlIdentifier( typeName.name(), SqlParserPos.ZERO );
    }


    @Override
    public RelDataTypeFamily getFamily() {
        // by default, put each type into its own family
        return this;
    }


    /**
     * Generates a string representation of this type.
     *
     * @param sb StringBuilder into which to generate the string
     * @param withDetail when true, all detail information needed to compute a unique digest (and return from getFullTypeString) should be included;
     */
    protected abstract void generateTypeString( StringBuilder sb, boolean withDetail );


    /**
     * Computes the digest field. This should be called in every non-abstract subclass constructor once the type is fully defined.
     */
    protected void computeDigest() {
        StringBuilder sb = new StringBuilder();
        generateTypeString( sb, true );
        if ( !isNullable() ) {
            sb.append( " NOT NULL" );
        }
        digest = sb.toString();
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        generateTypeString( sb, false );
        return sb.toString();
    }


    @Override
    public RelDataTypePrecedenceList getPrecedenceList() {
        // By default, make each type have a precedence list containing only other types in the same family
        return new RelDataTypePrecedenceList() {
            @Override
            public boolean containsType( RelDataType type ) {
                return getFamily() == type.getFamily();
            }


            @Override
            public int compareTypePrecedence( RelDataType type1, RelDataType type2 ) {
                assert containsType( type1 );
                assert containsType( type2 );
                return 0;
            }
        };
    }


    @Override
    public RelDataTypeComparability getComparability() {
        return RelDataTypeComparability.ALL;
    }


    /**
     * Returns an implementation of {@link RelProtoDataType} that copies a given type using the given type factory.
     */
    public static RelProtoDataType proto( final RelDataType protoType ) {
        assert protoType != null;
        return typeFactory -> typeFactory.copyType( protoType );
    }


    /**
     * Returns a {@link org.polypheny.db.rel.type.RelProtoDataType} that will create a type {@code typeName}.
     *
     * For example, {@code proto(SqlTypeName.DATE), false} will create {@code DATE NOT NULL}.
     *
     * @param typeName Type name
     * @param nullable Whether nullable
     * @return Proto data type
     */
    public static RelProtoDataType proto( final SqlTypeName typeName, final boolean nullable ) {
        assert typeName != null;
        return typeFactory -> {
            final RelDataType type = typeFactory.createSqlType( typeName );
            return typeFactory.createTypeWithNullability( type, nullable );
        };
    }


    /**
     * Returns a {@link org.polypheny.db.rel.type.RelProtoDataType} that will create a type {@code typeName(precision)}.
     *
     * For example, {@code proto(SqlTypeName.VARCHAR, 100, false)} will create {@code VARCHAR(100) NOT NULL}.
     *
     * @param typeName Type name
     * @param precision Precision
     * @param nullable Whether nullable
     * @return Proto data type
     */
    public static RelProtoDataType proto( final SqlTypeName typeName, final int precision, final boolean nullable ) {
        assert typeName != null;
        return typeFactory -> {
            final RelDataType type = typeFactory.createSqlType( typeName, precision );
            return typeFactory.createTypeWithNullability( type, nullable );
        };
    }


    /**
     * Returns a {@link org.polypheny.db.rel.type.RelProtoDataType} that will create a type {@code typeName(precision, scale)}.
     *
     * For example, {@code proto(SqlTypeName.DECIMAL, 7, 2, false)} will create {@code DECIMAL(7, 2) NOT NULL}.
     *
     * @param typeName Type name
     * @param precision Precision
     * @param scale Scale
     * @param nullable Whether nullable
     * @return Proto data type
     */
    public static RelProtoDataType proto( final SqlTypeName typeName, final int precision, final int scale, final boolean nullable ) {
        return typeFactory -> {
            final RelDataType type = typeFactory.createSqlType( typeName, precision, scale );
            return typeFactory.createTypeWithNullability( type, nullable );
        };
    }


    /**
     * Returns the "extra" field in a row type whose presence signals that fields will come into existence just by asking for them.
     *
     * @param rowType Row type
     * @return The "extra" field, or null
     */
    public static RelDataTypeField extra( RelDataType rowType ) {
        // Even in a case-insensitive connection, the name must be precisely "_extra".
        return rowType.getField( "_extra", true, false );
    }


    @Override
    public boolean isDynamicStruct() {
        return false;
    }


    /**
     * Work space for {@link RelDataTypeImpl#getFieldRecurse}.
     */
    private static class Slot {

        int count;
        RelDataTypeField field;
    }
}
