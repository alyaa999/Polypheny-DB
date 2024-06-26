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

package org.polypheny.db.adapter.elasticsearch;


import org.junit.Ignore;


/**
 * Checks renaming of fields (also upper, lower cases) during projections
 */
@Ignore
public class ProjectionTest {

//    @ClassRule
//    public static final EmbeddedElasticsearchPolicy NODE = EmbeddedElasticsearchPolicy.create();
//
//    private static final String NAME = "docs";
//
//
//    @BeforeClass
//    public static void setupInstance() throws Exception {
//
//        final Map<String, String> mappings = ImmutableMap.of( "A", "keyword", "b", "keyword", "cCC", "keyword", "DDd", "keyword" );
//
//        NODE.createIndex( NAME, mappings );
//
//        String doc = "{'A': 'aa', 'b': 'bb', 'cCC': 'cc', 'DDd': 'dd'}".replace( '\'', '"' );
//        NODE.insertDocument( NAME, (ObjectNode) NODE.mapper().readTree( doc ) );
//    }
//
//
//    private ConnectionFactory newConnectionFactory() {
//        return new ConnectionFactory() {
//            @Override
//            public Connection createConnection() throws SQLException {
//                final Connection connection = DriverManager.getConnection( "jdbc:polyphenydbembedded:" );
//                final SchemaPlus root = connection.unwrap( PolyphenyDbEmbeddedConnection.class ).getRootSchema();
//
//                root.add( "elastic", new ElasticsearchSchema( NODE.restClient(), NODE.mapper(), NAME ) );
//
//                // add Polypheny-DB view programmatically
//                final String viewSql = String.format( Locale.ROOT,
//                        "select cast(_MAP['A'] AS varchar(2)) AS a,"
//                                + " cast(_MAP['b'] AS varchar(2)) AS b, "
//                                + " cast(_MAP['cCC'] AS varchar(2)) AS c, "
//                                + " cast(_MAP['DDd'] AS varchar(2)) AS d "
//                                + " from \"elastic\".\"%s\"", NAME );
//
//                ViewTableMacro macro = ViewTable.viewMacro( root, viewSql, Collections.singletonList( "elastic" ), Arrays.asList( "elastic", "view" ), false );
//                root.add( "VIEW", macro );
//
//                return connection;
//            }
//        };
//    }
//
//
//    @Test
//    public void projection() {
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select * from view" )
//                .returns( "A=aa; B=bb; C=cc; D=dd\n" );
//
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select a, b, c, d from view" )
//                .returns( "A=aa; B=bb; C=cc; D=dd\n" );
//
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select d, c, b, a from view" )
//                .returns( "D=dd; C=cc; B=bb; A=aa\n" );
//
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select a from view" )
//                .returns( "A=aa\n" );
//
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select a, b from view" )
//                .returns( "A=aa; B=bb\n" );
//
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select b, a from view" )
//                .returns( "B=bb; A=aa\n" );
//
//    }

}

