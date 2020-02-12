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

package org.polypheny.db.rel.metadata;


import org.polypheny.db.rel.RelNode;
import com.google.common.collect.Multimap;
import java.lang.reflect.Method;


/**
 * RelMetadataProvider defines an interface for obtaining metadata about relational expressions. This interface is weakly-typed and is not intended to be called directly in most contexts;
 * instead, use a strongly-typed facade such as {@link RelMetadataQuery}.
 *
 * For background and motivation, see <a href="http://wiki.eigenbase.org/RelationalExpressionMetadata">wiki</a>.
 *
 * If your provider is not a singleton, we recommend that you implement {@link Object#equals(Object)} and {@link Object#hashCode()} methods. This makes the cache of {@link JaninoRelMetadataProvider} more effective.
 */
public interface RelMetadataProvider {

    /**
     * Retrieves metadata of a particular type and for a particular sub-class of relational expression.
     *
     * The object returned is a function. It can be applied to a relational expression of the given type to create a metadata object.
     *
     * For example, you might call
     *
     * <blockquote><pre>
     * RelMetadataProvider provider;
     * LogicalFilter filter;
     * RexNode predicate;
     * Function&lt;RelNode, Metadata&gt; function = provider.apply(LogicalFilter.class, Selectivity.class};
     * Selectivity selectivity = function.apply(filter);
     * Double d = selectivity.selectivity(predicate);
     * </pre></blockquote>
     *
     * @param relClass Type of relational expression
     * @param metadataClass Type of metadata
     * @return Function that will field a metadata instance; or null if this provider cannot supply metadata of this type
     */
    <M extends Metadata> UnboundMetadata<M> apply( Class<? extends RelNode> relClass, Class<? extends M> metadataClass );

    <M extends Metadata> Multimap<Method, MetadataHandler<M>> handlers( MetadataDef<M> def );
}
