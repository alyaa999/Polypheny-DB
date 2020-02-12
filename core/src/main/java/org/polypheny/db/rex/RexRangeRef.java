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

package org.polypheny.db.rex;


import org.polypheny.db.rel.type.RelDataType;
import java.util.Objects;


/**
 * Reference to a range of columns.
 *
 * This construct is used only during the process of translating a {@link org.polypheny.db.sql.SqlNode SQL} tree to a {@link org.polypheny.db.rel.RelNode rel}/{@link RexNode rex} tree.
 * <em>Regular {@link RexNode rex} trees do not contain this construct.</em>
 *
 * While translating a join of EMP(EMPNO, ENAME, DEPTNO) to DEPT(DEPTNO2, DNAME) we create <code>RexRangeRef(DeptType,3)</code> to represent the pair of columns (DEPTNO2, DNAME) which came from DEPT.
 * The type has 2 columns, and therefore the range represents columns {3, 4} of the input.
 *
 * Suppose we later create a reference to the DNAME field of this RexRangeRef; it will return a <code>{@link RexInputRef}(5,Integer)</code>, and the {@link org.polypheny.db.rex.RexRangeRef} will disappear.
 */
public class RexRangeRef extends RexNode {

    private final RelDataType type;
    private final int offset;


    /**
     * Creates a range reference.
     *
     * @param rangeType Type of the record returned
     * @param offset Offset of the first column within the input record
     */
    RexRangeRef( RelDataType rangeType, int offset ) {
        this.type = rangeType;
        this.offset = offset;
    }


    @Override
    public RelDataType getType() {
        return type;
    }


    public int getOffset() {
        return offset;
    }


    @Override
    public <R> R accept( RexVisitor<R> visitor ) {
        return visitor.visitRangeRef( this );
    }


    @Override
    public <R, P> R accept( RexBiVisitor<R, P> visitor, P arg ) {
        return visitor.visitRangeRef( this, arg );
    }


    @Override
    public boolean equals( Object obj ) {
        return this == obj
                || obj instanceof RexRangeRef
                && type.equals( ((RexRangeRef) obj).type )
                && offset == ((RexRangeRef) obj).offset;
    }


    @Override
    public int hashCode() {
        return Objects.hash( type, offset );
    }
}
