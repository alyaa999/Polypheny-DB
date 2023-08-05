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

package org.polypheny.db.algebra.enumerable.document;

import java.util.List;
import org.polypheny.db.algebra.AlgNode;
import org.polypheny.db.algebra.convert.ConverterRule;
import org.polypheny.db.algebra.core.AlgFactories;
import org.polypheny.db.algebra.core.document.DocumentProject;
import org.polypheny.db.algebra.enumerable.EnumerableCalc;
import org.polypheny.db.algebra.enumerable.EnumerableConvention;
import org.polypheny.db.algebra.logical.document.LogicalDocumentProject;
import org.polypheny.db.plan.Convention;
import org.polypheny.db.rex.RexProgram;

public class DocumentProjectToCalcRule extends ConverterRule {

    public static final DocumentProjectToCalcRule INSTANCE = new DocumentProjectToCalcRule();


    public DocumentProjectToCalcRule() {
        super( DocumentProject.class, r -> true, Convention.NONE, EnumerableConvention.INSTANCE, AlgFactories.LOGICAL_BUILDER, "DocumentToCalcRule_LogicalDocumentProject" );
    }


    @Override
    public AlgNode convert( AlgNode alg ) {
        final LogicalDocumentProject project = (LogicalDocumentProject) alg;
        final AlgNode input = project.getInput();
        final RexProgram program = RexProgram.create( input.getRowType(), List.of( project.asSingleProject() ), null, project.getRowType(), project.getCluster().getRexBuilder() );
        return EnumerableCalc.create( convert( input, input.getTraitSet().replace( EnumerableConvention.INSTANCE ) ), program );
    }


}