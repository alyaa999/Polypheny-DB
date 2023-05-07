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

package org.polypheny.db.algebra.enumerable.impl;


import java.util.List;
import org.apache.calcite.linq4j.tree.BlockBuilder;
import org.apache.calcite.linq4j.tree.Expression;
import org.polypheny.db.algebra.enumerable.AggAddContext;


/**
 * Implementation of {@link AggAddContext}.
 */
public abstract class AggAddContextImpl extends AggResultContextImpl implements AggAddContext {

    public AggAddContextImpl( BlockBuilder block, List<Expression> accumulator ) {
        super( block, null, accumulator, null, null );
    }


    @Override
    public final List<Expression> arguments() {
        return rowTranslator().translateList( rexArguments() );
    }

}

