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

package org.polypheny.db.sql.language.validate;


import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.polypheny.db.algebra.constant.Monotonicity;
import org.polypheny.db.algebra.type.AlgDataType;
import org.polypheny.db.algebra.type.AlgDataTypeFactory.Builder;
import org.polypheny.db.algebra.type.AlgDataTypeField;
import org.polypheny.db.catalog.entity.CatalogEntity;
import org.polypheny.db.plan.AlgOptEntity;
import org.polypheny.db.schema.Entity;
import org.polypheny.db.schema.ExtensibleEntity;
import org.polypheny.db.sql.language.SqlIdentifier;
import org.polypheny.db.sql.language.SqlNode;
import org.polypheny.db.sql.language.SqlNodeList;
import org.polypheny.db.util.Static;
import org.polypheny.db.util.Util;
import org.polypheny.db.util.ValidatorUtil;


/**
 * Namespace based on a table from the catalog.
 */
class EntityNamespace extends AbstractNamespace {

    @Getter
    private final CatalogEntity table;
    public final ImmutableList<AlgDataTypeField> extendedFields;


    /**
     * Creates a TableNamespace.
     */
    private EntityNamespace( SqlValidatorImpl validator, CatalogEntity entity, List<AlgDataTypeField> fields ) {
        super( validator, null );
        this.table = entity;
        this.extendedFields = ImmutableList.copyOf( fields );
    }


    EntityNamespace( SqlValidatorImpl validator, CatalogEntity table ) {
        this( validator, table, ImmutableList.of() );
    }


    @Override
    protected AlgDataType validateImpl( AlgDataType targetRowType ) {
        if ( extendedFields.isEmpty() ) {
            return table.getRowType();
        }
        final Builder builder = validator.getTypeFactory().builder();
        builder.addAll( table.getRowType().getFieldList() );
        builder.addAll( extendedFields );
        return builder.build();
    }


    @Override
    public SqlNode getNode() {
        // This is the only kind of namespace not based on a node in the parse tree.
        return null;
    }



    @Override
    public Monotonicity getMonotonicity( String columnName ) {
        final CatalogEntity table = getTable();
        return Util.getMonotonicity( table, columnName );
    }


    /**
     * Creates a TableNamespace based on the same table as this one, but with extended fields.
     *
     * Extended fields are "hidden" or undeclared fields that may nevertheless be present if you ask for them.
     */
    public EntityNamespace extend( SqlNodeList extendList ) {
        final List<SqlNode> identifierList = Util.quotientList( extendList.getSqlList(), 2, 0 );
        SqlValidatorUtil.checkIdentifierListForDuplicates( identifierList, validator.getValidationErrorFunction() );
        final ImmutableList.Builder<AlgDataTypeField> builder = ImmutableList.builder();
        builder.addAll( this.extendedFields );
        builder.addAll( SqlValidatorUtil.getExtendedColumns( validator.getTypeFactory(), getTable(), extendList ) );
        final List<AlgDataTypeField> extendedFields = builder.build();
        final Entity schemaEntity = table.unwrap( Entity.class );
        if ( schemaEntity != null && table instanceof AlgOptEntity && schemaEntity instanceof ExtensibleEntity ) {
            checkExtendedColumnTypes( extendList );
            //final AlgOptEntity algOptEntity = ((AlgOptEntity) table).extend( extendedFields );
            //final CatalogEntity validatorTable = algOptEntity.unwrap( ValidatorTable.class );
            return new EntityNamespace( validator, table, ImmutableList.of() );
        }
        return new EntityNamespace( validator, table, extendedFields );
    }


    /**
     * Gets the data-type of all columns in a table (for a view table: including columns of the underlying table)
     */
    private AlgDataType getBaseRowType() {
        final Entity schemaEntity = table.unwrap( Entity.class );
        return schemaEntity.getRowType( validator.typeFactory );
    }


    /**
     * Ensures that extended columns that have the same name as a base column also have the same data-type.
     */
    private void checkExtendedColumnTypes( SqlNodeList extendList ) {
        final List<AlgDataTypeField> extendedFields = SqlValidatorUtil.getExtendedColumns( validator.getTypeFactory(), table, extendList );
        final List<AlgDataTypeField> baseFields = getBaseRowType().getFieldList();
        final Map<String, Integer> nameToIndex = ValidatorUtil.mapNameToIndex( baseFields );

        for ( final AlgDataTypeField extendedField : extendedFields ) {
            final String extFieldName = extendedField.getName();
            if ( nameToIndex.containsKey( extFieldName ) ) {
                final Integer baseIndex = nameToIndex.get( extFieldName );
                final AlgDataType baseType = baseFields.get( baseIndex ).getType();
                final AlgDataType extType = extendedField.getType();

                if ( !extType.equals( baseType ) ) {
                    // Get the extended column node that failed validation.
                    final SqlNode extColNode =
                            extendList.getSqlList().stream().filter( sqlNode -> sqlNode instanceof SqlIdentifier && Util.last( ((SqlIdentifier) sqlNode).names ).equals( extendedField.getName() ) ).findFirst().get();

                    throw validator.getValidationErrorFunction().apply(
                            extColNode,
                            Static.RESOURCE.typeNotAssignable(
                                    baseFields.get( baseIndex ).getName(),
                                    baseType.getFullTypeString(),
                                    extendedField.getName(),
                                    extType.getFullTypeString() ) );
                }
            }
        }
    }

}

