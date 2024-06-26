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

package org.polypheny.db.cypher.set;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.polypheny.db.cypher.cypher2alg.CypherToAlgConverter.CypherContext;
import org.polypheny.db.cypher.expression.CypherVariable;
import org.polypheny.db.cypher.parser.StringPos;
import org.polypheny.db.rex.RexNode;
import org.polypheny.db.util.Pair;

@Getter
public class CypherSetLabels extends CypherSetItem {

    private final CypherVariable variable;
    private final List<StringPos> labels;


    public CypherSetLabels( CypherVariable variable, List<StringPos> labels ) {
        this.variable = variable;
        this.labels = labels;
    }


    @Override
    public void convertItem( CypherContext context ) {
        String nodeName = variable.getName();
        RexNode op = context.getLabelUpdate( labels.stream().map( StringPos::getImage ).collect( Collectors.toList() ), nodeName, false );

        context.add( Pair.of( nodeName, op ) );
    }


}
