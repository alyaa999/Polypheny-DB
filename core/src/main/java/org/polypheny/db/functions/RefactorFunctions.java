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

package org.polypheny.db.functions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.polypheny.db.type.entity.PolyBinary;
import org.polypheny.db.type.entity.PolyInterval;
import org.polypheny.db.type.entity.PolyLong;
import org.polypheny.db.type.entity.PolyString;
import org.polypheny.db.type.entity.PolyValue;
import org.polypheny.db.type.entity.category.PolyNumber;
import org.polypheny.db.type.entity.document.PolyDocument;
import org.polypheny.db.util.Pair;


public class RefactorFunctions {

    public static PolyValue get( PolyDocument map, PolyString key ) {
        return map.get( key );
    }


    @SuppressWarnings("unused")
    public static PolyDocument removeNames( PolyDocument object, List<String> names ) {
        names.forEach( n -> object.remove( PolyString.of( n ) ) );
        return object;
    }


    @SuppressWarnings("unused")
    public static PolyValue[] toObjectArray( PolyValue... obj ) {
        return obj;
    }


    @SuppressWarnings("unused")
    public static PolyBinary fromDocument( PolyValue doc ) {
        return PolyBinary.of( doc.serialize().getBytes() );
    }


    @SuppressWarnings("unused")
    public static PolyValue toDocument( PolyBinary json ) {
        return PolyValue.deserialize( new String( json.value.getBytes() ) );
    }


    @SafeVarargs
    @SuppressWarnings("unused")
    public static PolyDocument mergeDocuments( PolyValue target, Pair<String, PolyValue>... additional ) {
        Map<PolyString, PolyValue> map = new HashMap<>( target.asDocument() );
        for ( Pair<String, PolyValue> pair : additional ) {
            map.put( new PolyString( pair.left ), pair.right );
        }

        return PolyDocument.ofDocument( map );
    }


    @SuppressWarnings("unused")
    public static PolyDocument mergeDocuments( PolyString[] keys, PolyValue... values ) {
        Map<PolyString, PolyValue> map = new HashMap<>();
        for ( int i = 0; i < keys.length; i++ ) {
            map.put( keys[i], values[i] );
        }

        return PolyDocument.ofDocument( map );
    }


    public static PolyNumber unwrap( PolyInterval interval ) {
        return PolyLong.of( interval.value );
    }

}