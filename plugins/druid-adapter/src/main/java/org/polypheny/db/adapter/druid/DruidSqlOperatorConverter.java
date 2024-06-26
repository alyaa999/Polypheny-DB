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

package org.polypheny.db.adapter.druid;


import javax.annotation.Nullable;
import org.polypheny.db.algebra.type.AlgDataType;
import org.polypheny.db.nodes.Operator;
import org.polypheny.db.rex.RexNode;


/**
 * Defines how to convert RexNode with a given Polypheny-DB SQL operator to Druid expressions
 */
public interface DruidSqlOperatorConverter {

    /**
     * Returns the Polypheny-DB SQL operator corresponding to Druid operator.
     *
     * @return operator
     */
    Operator polyphenyDbOperator();


    /**
     * Translate rexNode to valid Druid expression.
     *
     * @param rexNode rexNode to translate to Druid expression
     * @param rowType row type associated with rexNode
     * @param druidQuery druid query used to figure out configs/fields related like timeZone
     * @return valid Druid expression or null if it can not convert the rexNode
     */
    @Nullable
    String toDruidExpression( RexNode rexNode, AlgDataType rowType, DruidQuery druidQuery );

}

