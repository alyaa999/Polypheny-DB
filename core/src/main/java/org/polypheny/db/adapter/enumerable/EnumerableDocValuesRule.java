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

package org.polypheny.db.adapter.enumerable;

import java.util.function.Predicate;
import org.polypheny.db.adapter.enumerable.document.EnumerableDocumentValues;
import org.polypheny.db.algebra.AlgNode;
import org.polypheny.db.algebra.convert.ConverterRule;
import org.polypheny.db.algebra.core.document.DocumentValues;
import org.polypheny.db.algebra.logical.document.LogicalDocumentValues;
import org.polypheny.db.plan.Convention;
import org.polypheny.db.tools.AlgBuilderFactory;

public class EnumerableDocValuesRule extends ConverterRule {

    public EnumerableDocValuesRule( AlgBuilderFactory algBuilderFactory ) {
        super( LogicalDocumentValues.class, (Predicate<AlgNode>) r -> true, Convention.NONE, EnumerableConvention.INSTANCE, algBuilderFactory, "EnumerableDocValuesRule" );
    }


    @Override
    public AlgNode convert( AlgNode alg ) {
        DocumentValues values = (DocumentValues) alg;
        return EnumerableDocumentValues.create( values );
    }

}
