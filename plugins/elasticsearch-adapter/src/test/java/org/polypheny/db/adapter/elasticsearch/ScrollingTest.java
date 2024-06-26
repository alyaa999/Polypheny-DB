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
 * Tests usage of scrolling API like correct results and resource cleanup (delete scroll after scan).
 */
@Ignore
public class ScrollingTest {

//    @ClassRule
//    public static final EmbeddedElasticsearchPolicy NODE = EmbeddedElasticsearchPolicy.create();
//
//    private static final String NAME = "scroll";
//    private static final int SIZE = 10;
//
//
//    @BeforeClass
//    public static void setupInstance() throws Exception {
//        NODE.createIndex( NAME, Collections.singletonMap( "value", "long" ) );
//        final List<ObjectNode> docs = new ArrayList<>();
//        for ( int i = 0; i < SIZE; i++ ) {
//            String json = String.format( Locale.ROOT, "{\"value\": %d}", i );
//            docs.add( (ObjectNode) NODE.mapper().readTree( json ) );
//        }
//        NODE.insertBulk( NAME, docs );
//    }
//
//
//    private ConnectionFactory newConnectionFactory( int fetchSize ) {
//        return new ConnectionFactory() {
//            @Override
//            public Connection createConnection() throws SQLException {
//                final Connection connection = DriverManager.getConnection( "jdbc:polyphenydbembedded:" );
//                final SchemaPlus root = connection.unwrap( PolyphenyDbEmbeddedConnection.class ).getRootSchema();
//                ElasticsearchSchema schema = new ElasticsearchSchema( NODE.restClient(), NODE.mapper(), NAME, null, fetchSize );
//                root.add( "elastic", schema );
//                return connection;
//            }
//        };
//    }
//
//
//    @Test
//    public void scrolling() throws Exception {
//        final String[] expected = IntStream.range( 0, SIZE ).mapToObj( i -> "V=" + i ).toArray( String[]::new );
//        final String query = String.format( Locale.ROOT, "select _MAP['value'] as v from \"elastic\".\"%s\"", NAME );
//
//        for ( int fetchSize : Arrays.asList( 1, 2, 3, SIZE / 2, SIZE - 1, SIZE, SIZE + 1, 2 * SIZE ) ) {
//            PolyphenyDbAssert.that()
//                    .with( newConnectionFactory( fetchSize ) )
//                    .query( query )
//                    .returnsUnordered( expected );
//            assertNoActiveScrolls();
//        }
//    }
//
//
//    /**
//     * Ensures there are no pending scroll contexts in elastic search cluster. Queries {@code /_nodes/stats/indices/search} endpoint.
//     *
//     * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-stats.html">Indices Stats</a>
//     */
//    private void assertNoActiveScrolls() throws IOException {
//        // get node stats
//        final Response response = NODE.restClient().performRequest( "GET", "/_nodes/stats/indices/search" );
//
//        try ( InputStream is = response.getEntity().getContent() ) {
//            final ObjectNode node = NODE.mapper().readValue( is, ObjectNode.class );
//            final String path = "/indices/search/scroll_current";
//            final JsonNode scrollCurrent = node.with( "nodes" ).elements().next().at( path );
//            if ( scrollCurrent.isMissingNode() ) {
//                throw new IllegalStateException( "Couldn't find node at " + path );
//            }
//
//            if ( scrollCurrent.asInt() != 0 ) {
//                final String message = String.format( Locale.ROOT, "Expected no active scrolls but got %d. Current index stats %s", scrollCurrent.asInt(), node );
//                throw new AssertionError( message );
//            }
//        }
//    }

}

