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

package org.polypheny.db.sql.language;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.polypheny.db.algebra.AlgNode;
import org.polypheny.db.algebra.AlgShuttleImpl;
import org.polypheny.db.algebra.externalize.AlgJsonWriter;
import org.polypheny.db.algebra.logical.relational.LogicalRelScan;
import org.polypheny.db.catalog.entity.Entity;
import org.polypheny.db.runtime.Hook;
import org.polypheny.db.runtime.Hook.Closeable;


/**
 * Runs {@link SqlToAlgConverterTest} with extensions.
 */
public class SqlToAlgConverterExtendedTest extends SqlToAlgConverterTest {

    Closeable closeable;


    @BeforeEach
    public void before() {
        this.closeable = Hook.CONVERTED.addThread( SqlToAlgConverterExtendedTest::foo );
    }


    @AfterEach
    public void after() {
        if ( this.closeable != null ) {
            this.closeable.close();
            this.closeable = null;
        }
    }


    public static void foo( AlgNode alg ) {
        // Convert alg tree to JSON.
        final AlgJsonWriter writer = new AlgJsonWriter();
        alg.explain( writer );
        final String json = writer.asString();

        // Find the schema. If there are no tables in the plan, we won't need one.
        final Entity[] entities = { null };
        alg.accept( new AlgShuttleImpl() {
            @Override
            public AlgNode visit( LogicalRelScan scan ) {
                entities[0] = scan.getEntity();
                return super.visit( scan );
            }
        } );

        // Convert JSON back to alg tree.
        /*Frameworks.withPlanner( ( cluster, algOptSchema, rootSchema ) -> {
            final AlgJsonReader reader = new AlgJsonReader( cluster, null, null );
            try {
                AlgNode x = reader.read( json );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
            return null;
        } );*/
    }

}

