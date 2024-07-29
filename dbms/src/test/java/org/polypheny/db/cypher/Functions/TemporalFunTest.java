/*
 * Copyright 2019-2024 The Polypheny Project
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

package org.polypheny.db.cypher.Functions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.polypheny.db.cypher.CypherTestTemplate;
import org.polypheny.db.webui.models.results.GraphResult;

public class TemporalFunTest extends CypherTestTemplate {

    @BeforeEach
    public void reset() {
        tearDown();
        createGraph();
    }


    @Test
    public void dateFunTest() {

        GraphResult res = execute( "RETURN DATE('2023-05-18')\n" );


    }





    @Test
    public void timeFunTest() {

        GraphResult res = execute( "RETURN TIME('12:34:56')" );


    }


    @Test
    public void dateTimeFunTest() {

        GraphResult res = execute( "RETURN DATETIME('2023-05-18T12:34:56')" );


    }


    @Test
    public void durationBetweenFunTest() {

        GraphResult res = execute( "RETURN duration.between(DATE('2023-05-18'), DATE('2023-06-18'))" );


    }


}