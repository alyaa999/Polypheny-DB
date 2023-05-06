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

package org.polypheny.db.algebra.rules;

import lombok.extern.slf4j.Slf4j;
import org.polypheny.db.algebra.core.document.DocumentModify;
import org.polypheny.db.algebra.core.document.DocumentValues;
import org.polypheny.db.plan.AlgOptRule;
import org.polypheny.db.plan.AlgOptRuleCall;
import org.polypheny.db.plan.Convention;

@Slf4j
public class SimpleAllocationToPhysicalModifyRule extends AlgOptRule {

    public static final SimpleAllocationToPhysicalModifyRule DOC_INSTANCE = new SimpleAllocationToPhysicalModifyRule();


    public SimpleAllocationToPhysicalModifyRule() {
        super( operand( DocumentModify.class, operand( DocumentValues.class, Convention.NONE, none() ) ) );
    }


    @Override
    public void onMatch( AlgOptRuleCall call ) {
        log.warn( "todo" );
        return;
        /*DocumentModify<?> modify = call.alg( 0 );
        DocumentValues values = call.alg( 1 );

        AlgBuilder builder = call.builder();

        builder.push( modify.copy( modify.getTraitSet(), List.of( values.getRelationalEquivalent() ) ) );
        AlgNode node = builder.transform( ModelTrait.DOCUMENT, modify.getRowType(), false ).build();
        call.transformTo( node );*/

    }

}