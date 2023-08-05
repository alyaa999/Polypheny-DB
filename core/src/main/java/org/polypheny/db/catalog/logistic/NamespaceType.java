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

package org.polypheny.db.catalog.logistic;

import com.google.gson.annotations.SerializedName;
import org.polypheny.db.schema.trait.ModelTrait;

public enum NamespaceType {
    @SerializedName("relational")
    RELATIONAL( 1 ),
    @SerializedName("document")
    DOCUMENT( 2 ),
    @SerializedName("graph")
    GRAPH( 3 );

    // GRAPH, DOCUMENT, ...

    public final int id;


    NamespaceType( int id ) {
        this.id = id;
    }


    public int getId() {
        return id;
    }


    public static NamespaceType getDefault() {
        //return (NamespaceType) ConfigManager.getInstance().getConfig( "runtime/defaultSchemaModel" ).getEnum();
        return NamespaceType.RELATIONAL;
    }


    public static NamespaceType getById( final int id ) {
        for ( NamespaceType t : values() ) {
            if ( t.id == id ) {
                return t;
            }
        }
        throw new RuntimeException( "Unknown NamespaceType with id: " + id );
    }


    public static NamespaceType getByName( final String name ) {
        for ( NamespaceType t : values() ) {
            if ( t.name().equalsIgnoreCase( name ) ) {
                return t;
            }
        }
        throw new RuntimeException( "Unknown NamespaceType with name: " + name );
    }


    public ModelTrait getModelTrait() {
        if ( this == NamespaceType.RELATIONAL ) {
            return ModelTrait.RELATIONAL;
        } else if ( this == NamespaceType.DOCUMENT ) {
            return ModelTrait.DOCUMENT;
        } else if ( this == NamespaceType.GRAPH ) {
            return ModelTrait.GRAPH;
        }
        throw new RuntimeException( "Not found a suitable NamespaceType." );
    }
}