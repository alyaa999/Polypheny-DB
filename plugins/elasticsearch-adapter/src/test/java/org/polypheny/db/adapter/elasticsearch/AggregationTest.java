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
 * Testing Elasticsearch aggregation transformations.
 */
@Ignore
public class AggregationTest {

//    @ClassRule
//    public static final EmbeddedElasticsearchPolicy NODE = EmbeddedElasticsearchPolicy.create();
//
//    private static final String NAME = "aggs";
//
//
//    @BeforeClass
//    public static void setupInstance() throws Exception {
//
//        final Map<String, String> mappings = ImmutableMap.<String, String>builder()
//                .put( "cat1", "keyword" )
//                .put( "cat2", "keyword" )
//                .put( "cat3", "keyword" )
//                .put( "cat4", "date" )
//                .put( "cat5", "integer" )
//                .put( "val1", "long" )
//                .put( "val2", "long" )
//                .build();
//
//        NODE.createIndex( NAME, mappings );
//
//        String doc1 = "{cat1:'a', cat2:'g', val1:1, cat4:'2018-01-01', cat5:1}";
//        String doc2 = "{cat2:'g', cat3:'y', val2:5, cat4:'2019-12-12'}";
//        String doc3 = "{cat1:'b', cat2:'h', cat3:'z', cat5:2, val1:7, val2:42}";
//
//        final ObjectMapper mapper = new ObjectMapper()
//                .enable( JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES ) // user-friendly settings to
//                .enable( JsonParser.Feature.ALLOW_SINGLE_QUOTES ); // avoid too much quoting
//
//        final List<ObjectNode> docs = new ArrayList<>();
//        for ( String text : Arrays.asList( doc1, doc2, doc3 ) ) {
//            docs.add( (ObjectNode) mapper.readTree( text ) );
//        }
//
//        NODE.insertBulk( NAME, docs );
//    }
//
//
//    private ConnectionFactory newConnectionFactory() {
//        return new ConnectionFactory() {
//            @Override
//            public Connection createConnection() throws SQLException {
//                final Connection connection = DriverManager.getConnection( "jdbc:polyphenydbembedded:lex=JAVA" );
//                final SchemaPlus root = connection.unwrap( PolyphenyDbEmbeddedConnection.class ).getRootSchema();
//
//                root.add( "elastic", new ElasticsearchSchema( NODE.restClient(), NODE.mapper(), NAME ) );
//
//                // add Polypheny-DB view programmatically
//                final String viewSql = String.format( Locale.ROOT,
//                        "select _MAP['cat1'] AS \"cat1\", "
//                                + " _MAP['cat2']  AS \"cat2\", "
//                                + " _MAP['cat3'] AS \"cat3\", "
//                                + " _MAP['cat4'] AS \"cat4\", "
//                                + " _MAP['cat5'] AS \"cat5\", "
//                                + " _MAP['val1'] AS \"val1\", "
//                                + " _MAP['val2'] AS \"val2\" "
//                                + " from \"elastic\".\"%s\"", NAME );
//
//                ViewTableMacro macro = ViewTable.viewMacro( root, viewSql, Collections.singletonList( "elastic" ), Arrays.asList( "elastic", "view" ), false );
//                root.add( "view", macro );
//                return connection;
//            }
//        };
//    }
//
//
//    @Test
//    public void countStar() {
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select count(*) from view" )
//                .queryContains( ElasticsearchChecker.elasticsearchChecker( "_source:false, size:0" ) )
//                .returns( "EXPR$0=3\n" );
//
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select count(*) from view where cat1 = 'a'" )
//                .returns( "EXPR$0=1\n" );
//
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select count(*) from view where cat1 in ('a', 'b')" )
//                .returns( "EXPR$0=2\n" );
//    }
//
//
//    @Test
//    public void all() {
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select count(*), sum(val1), sum(val2) from view" )
//                .queryContains( ElasticsearchChecker.elasticsearchChecker( "_source:false, size:0", "aggregations:{'EXPR$0.value_count.field': '_id'", "'EXPR$1.sum.field': 'val1'", "'EXPR$2.sum.field': 'val2'}" ) )
//                .returns( "EXPR$0=3; EXPR$1=8.0; EXPR$2=47.0\n" );
//
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select min(val1), max(val2), count(*) from view" )
//                .queryContains( ElasticsearchChecker.elasticsearchChecker( "_source:false, size:0", "aggregations:{'EXPR$0.min.field': 'val1'", "'EXPR$1.max.field': 'val2'", "'EXPR$2.value_count.field': '_id'}" ) )
//                .returns( "EXPR$0=1.0; EXPR$1=42.0; EXPR$2=3\n" );
//    }
//
//
//    @Test
//    public void cat1() {
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select cat1, sum(val1), sum(val2) from view group by cat1" )
//                .returnsUnordered( "cat1=null; EXPR$1=0.0; EXPR$2=5.0", "cat1=a; EXPR$1=1.0; EXPR$2=0.0", "cat1=b; EXPR$1=7.0; EXPR$2=42.0" );
//
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select cat1, count(*) from view group by cat1" )
//                .returnsUnordered( "cat1=null; EXPR$1=1", "cat1=a; EXPR$1=1", "cat1=b; EXPR$1=1" );
//
//        // different order for agg functions
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select count(*), cat1 from view group by cat1" )
//                .returnsUnordered( "EXPR$0=1; cat1=a", "EXPR$0=1; cat1=b", "EXPR$0=1; cat1=null" );
//
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select cat1, count(*), sum(val1), sum(val2) from view group by cat1" )
//                .returnsUnordered( "cat1=a; EXPR$1=1; EXPR$2=1.0; EXPR$3=0.0", "cat1=b; EXPR$1=1; EXPR$2=7.0; EXPR$3=42.0", "cat1=null; EXPR$1=1; EXPR$2=0.0; EXPR$3=5.0" );
//    }
//
//
//    @Test
//    public void cat2() {
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select cat2, min(val1), max(val1), min(val2), max(val2) from view group by cat2" )
//                .returnsUnordered( "cat2=g; EXPR$1=1.0; EXPR$2=1.0; EXPR$3=5.0; EXPR$4=5.0", "cat2=h; EXPR$1=7.0; EXPR$2=7.0; EXPR$3=42.0; EXPR$4=42.0" );
//
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select cat2, sum(val1), sum(val2) from view group by cat2" )
//                .returnsUnordered( "cat2=g; EXPR$1=1.0; EXPR$2=5.0", "cat2=h; EXPR$1=7.0; EXPR$2=42.0" );
//
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select cat2, count(*) from view group by cat2" )
//                .returnsUnordered( "cat2=g; EXPR$1=2", "cat2=h; EXPR$1=1" );
//    }
//
//
//    @Test
//    public void cat1Cat2() {
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select cat1, cat2, sum(val1), sum(val2) from view group by cat1, cat2" )
//                .returnsUnordered( "cat1=a; cat2=g; EXPR$2=1.0; EXPR$3=0.0", "cat1=null; cat2=g; EXPR$2=0.0; EXPR$3=5.0", "cat1=b; cat2=h; EXPR$2=7.0; EXPR$3=42.0" );
//
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select cat1, cat2, count(*) from view group by cat1, cat2" )
//                .returnsUnordered( "cat1=a; cat2=g; EXPR$2=1", "cat1=null; cat2=g; EXPR$2=1", "cat1=b; cat2=h; EXPR$2=1" );
//    }
//
//
//    @Test
//    public void cat1Cat3() {
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select cat1, cat3, sum(val1), sum(val2) from view group by cat1, cat3" )
//                .returnsUnordered( "cat1=a; cat3=null; EXPR$2=1.0; EXPR$3=0.0", "cat1=null; cat3=y; EXPR$2=0.0; EXPR$3=5.0", "cat1=b; cat3=z; EXPR$2=7.0; EXPR$3=42.0" );
//    }
//
//
//    /**
//     * Testing {@link Kind#ANY_VALUE} aggregate function
//     */
//    @Test
//    public void anyValue() {
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select cat1, any_value(cat2) from view group by cat1" )
//                .returnsUnordered( "cat1=a; EXPR$1=g", "cat1=null; EXPR$1=g", "cat1=b; EXPR$1=h" );
//
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select cat2, any_value(cat1) from view group by cat2" )
//                .returnsUnordered( "cat2=g; EXPR$1=a", "cat2=h; EXPR$1=b" ); // EXPR$1=null is also valid
//
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select cat2, any_value(cat3) from view group by cat2" )
//                .returnsUnordered( "cat2=g; EXPR$1=y", "cat2=h; EXPR$1=z" ); // EXPR$1=null is also valid
//    }
//
//
//    @Test
//    public void cat1Cat2Cat3() {
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select cat1, cat2, cat3, count(*), sum(val1), sum(val2) from view group by cat1, cat2, cat3" )
//                .returnsUnordered( "cat1=a; cat2=g; cat3=null; EXPR$3=1; EXPR$4=1.0; EXPR$5=0.0",
//                        "cat1=b; cat2=h; cat3=z; EXPR$3=1; EXPR$4=7.0; EXPR$5=42.0",
//                        "cat1=null; cat2=g; cat3=y; EXPR$3=1; EXPR$4=0.0; EXPR$5=5.0" );
//    }
//
//
//    /**
//     * Group by <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/date.html">date</a> data type.
//     */
//    @Test
//    public void dateCat() {
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select cat4, sum(val1) from view group by cat4" )
//                .returnsUnordered( "cat4=1514764800000; EXPR$1=1.0", "cat4=1576108800000; EXPR$1=0.0", "cat4=null; EXPR$1=7.0" );
//    }
//
//
//    /**
//     * Group by <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/number.html">number</a> data type.
//     */
//    @Test
//    public void integerCat() {
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select cat5, sum(val1) from view group by cat5" )
//                .returnsUnordered( "cat5=1; EXPR$1=1.0", "cat5=null; EXPR$1=0.0", "cat5=2; EXPR$1=7.0" );
//    }
//
//
//    /**
//     * Validate {@link StdOperatorRegistry.get( OperatorName.APPROX_COUNT_DISTINCT )}.
//     */
//    @Test
//    public void approximateCountDistinct() {
//        // approx_count_distinct counts distinct *non-null* values
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select approx_count_distinct(cat1) from view" )
//                .returnsUnordered( "EXPR$0=2" );
//
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select approx_count_distinct(cat2) from view" )
//                .returnsUnordered( "EXPR$0=2" );
//
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select cat1, approx_count_distinct(val1) from view group by cat1" )
//                .returnsUnordered( "cat1=a; EXPR$1=1", "cat1=b; EXPR$1=1", "cat1=null; EXPR$1=0" );
//        PolyphenyDbAssert.that()
//                .with( newConnectionFactory() )
//                .query( "select cat1, approx_count_distinct(val2) from view group by cat1" )
//                .returnsUnordered( "cat1=a; EXPR$1=0", "cat1=b; EXPR$1=1", "cat1=null; EXPR$1=1" );
//    }
}
