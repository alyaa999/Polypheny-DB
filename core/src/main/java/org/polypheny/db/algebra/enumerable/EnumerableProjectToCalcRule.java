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

package org.polypheny.db.algebra.enumerable;


import org.polypheny.db.algebra.AlgNode;
import org.polypheny.db.plan.AlgOptRule;
import org.polypheny.db.plan.AlgOptRuleCall;
import org.polypheny.db.rex.RexProgram;
import org.polypheny.db.tools.AlgBuilderFactory;


/**
 * Variant of {@link org.polypheny.db.algebra.rules.ProjectToCalcRule} for {@link EnumerableConvention enumerable calling convention}.
 */
public class EnumerableProjectToCalcRule extends AlgOptRule {

    /**
     * Creates an EnumerableProjectToCalcRule.
     *
     * @param algBuilderFactory Builder for relational expressions
     */
    public EnumerableProjectToCalcRule( AlgBuilderFactory algBuilderFactory ) {
        super( operand( EnumerableProject.class, any() ), algBuilderFactory, null );
    }


    @Override
    public void onMatch( AlgOptRuleCall call ) {
        final EnumerableProject project = call.alg( 0 );
        final AlgNode input = project.getInput();
        final RexProgram program = RexProgram.create( input.getRowType(), project.getProjects(), null, project.getRowType(), project.getCluster().getRexBuilder() );
        final EnumerableCalc calc = EnumerableCalc.create( input, program );
        call.transformTo( calc );
    }

}