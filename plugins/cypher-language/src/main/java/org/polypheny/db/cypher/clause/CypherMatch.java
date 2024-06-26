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

package org.polypheny.db.cypher.clause;

import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import org.polypheny.db.cypher.hint.CypherHint;
import org.polypheny.db.cypher.pattern.CypherPattern;
import org.polypheny.db.languages.ParserPos;


@Getter
public class CypherMatch extends CypherClause {

    private final boolean optional;
    private final List<CypherPattern> patterns;
    private final ParserPos pos1;
    private final List<CypherHint> hints;
    private final CypherWhere where;
    private final boolean isAll;


    public CypherMatch(
            ParserPos pos,
            boolean optional,
            List<CypherPattern> patterns,
            ParserPos pos1,
            List<CypherHint> hints,
            @Nullable CypherWhere where,
            boolean all ) {
        super( pos );
        this.optional = optional;
        this.patterns = patterns;
        this.pos1 = pos1;
        this.hints = hints;
        this.where = where;
        this.isAll = all;
    }


    @Override
    public CypherKind getCypherKind() {
        return CypherKind.MATCH;
    }

}
