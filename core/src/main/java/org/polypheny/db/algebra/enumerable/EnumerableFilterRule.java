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


import java.util.function.Predicate;
import org.polypheny.db.algebra.AlgNode;
import org.polypheny.db.algebra.convert.ConverterRule;
import org.polypheny.db.algebra.core.AlgFactories;
import org.polypheny.db.algebra.logical.relational.LogicalRelFilter;
import org.polypheny.db.plan.AlgOptRule;
import org.polypheny.db.plan.AlgOptUtil;
import org.polypheny.db.plan.Convention;


/**
 * Rule to convert a {@link LogicalRelFilter} to an {@link EnumerableFilter}.
 */
class EnumerableFilterRule extends ConverterRule {

    EnumerableFilterRule() {
        super( LogicalRelFilter.class,
                (Predicate<LogicalRelFilter>) AlgOptUtil::containsMultisetOrWindowedAgg,
                Convention.NONE, EnumerableConvention.INSTANCE,
                AlgFactories.LOGICAL_BUILDER, "EnumerableFilterRule" );
    }


    @Override
    public AlgNode convert( AlgNode alg ) {
        final LogicalRelFilter filter = (LogicalRelFilter) alg;
        return new EnumerableFilter(
                alg.getCluster(),
                alg.getTraitSet().replace( EnumerableConvention.INSTANCE ),
                AlgOptRule.convert( filter.getInput(), filter.getInput().getTraitSet().replace( EnumerableConvention.INSTANCE ) ),
                filter.getCondition() );
    }

}

