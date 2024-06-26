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

package org.polypheny.db.adapter.geode.algebra;


import org.junit.Ignore;


/**
 * Test with different types of data like boolean, time, timestamp
 */
@Ignore
public class GeodeAllDataTypesTest extends AbstractGeodeTest {

    // TODO MV: enable
//    @BeforeClass
//    public static void setUp() {
//        final Cache cache = POLICY.cache();
//        final Region<?, ?> region = cache.<String, Object>createRegionFactory().create( "allDataTypesRegion" );
//
//        final List<Map<String, Object>> mapList = createMapList();
//
//        new JsonLoader( region ).loadMapList( mapList );
//    }
//
//
//    private static List<Map<String, Object>> createMapList() {
//        return ImmutableList.of(
//                ImmutableMap.<String, Object>builder()
//                        .put( "booleanValue", true )
//                        .put( "dateValue", Date.valueOf( "2018-02-03" ) )
//                        .put( "timeValue", Time.valueOf( "02:22:23" ) )
//                        .put( "timestampValue", Timestamp.valueOf( "2018-02-03 02:22:33" ) )
//                        .put( "stringValue", "abc" )
//                        .put( "floatValue", 1.5678 )
//                        .build(),
//                ImmutableMap.<String, Object>builder()
//                        .put( "booleanValue", false )
//                        .put( "dateValue", Date.valueOf( "2018-02-04" ) )
//                        .put( "timeValue", Time.valueOf( "03:22:23" ) )
//                        .put( "timestampValue", Timestamp.valueOf( "2018-02-04 04:22:33" ) )
//                        .put( "stringValue", "def" )
//                        .put( "floatValue", 3.5678 )
//                        .build(),
//                ImmutableMap.<String, Object>builder()
//                        .put( "booleanValue", true )
//                        .put( "dateValue", Date.valueOf( "2018-02-05" ) )
//                        .put( "timeValue", Time.valueOf( "04:22:23" ) )
//                        .put( "timestampValue", Timestamp.valueOf( "2018-02-05 04:22:33" ) )
//                        .put( "stringValue", "ghi" )
//                        .put( "floatValue", 8.9267 )
//                        .build() );
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
//                root.add( "geode", new GeodeSchema( POLICY.cache(), Collections.singleton( "allDataTypesRegion" ) ) );
//
//                return connection;
//            }
//        };
//    }
//
//
//    private AssertThat polyphenyDbAssert() {
//        return PolyphenyDbAssert.that().with( newConnectionFactory() );
//    }
//
//
//    @Test
//    public void testSqlSingleBooleanWhereFilter() {
//        polyphenyDbAssert()
//                .query( "SELECT booleanValue as booleanValue FROM geode.allDataTypesRegion WHERE booleanValue = true" )
//                .returnsCount( 2 )
//                .queryContains( GeodeAssertions.query( "SELECT booleanValue AS booleanValue FROM /allDataTypesRegion WHERE booleanValue = true" ) );
//    }
//
//
//    @Test
//    public void testSqlMultipleBooleanWhereFilter() {
//        polyphenyDbAssert()
//                .query( "SELECT booleanValue as booleanValue FROM geode.allDataTypesRegion WHERE booleanValue = true OR booleanValue = false" )
//                .returnsCount( 3 )
//                .queryContains( GeodeAssertions.query( "SELECT booleanValue AS booleanValue FROM /allDataTypesRegion WHERE booleanValue IN SET(true, false)" ) );
//    }
//
//
//    @Test
//    public void testSqlWhereWithMultipleOrForLiteralFields() {
//        polyphenyDbAssert()
//                .query( "SELECT stringValue "
//                        + "FROM geode.allDataTypesRegion WHERE (stringValue = 'abc' OR stringValue = 'def') OR "
//                        + "(floatValue = 1.5678 OR floatValue = null) OR "
//                        + "(booleanValue = true OR booleanValue = false OR booleanValue = null)" )
//                .returnsCount( 3 )
//                .queryContains( GeodeAssertions.query( "SELECT stringValue AS stringValue FROM /allDataTypesRegion WHERE stringValue IN SET('abc', 'def') OR floatValue IN SET(1.5678, null) OR booleanValue IN SET(true, false, null)" ) );
//    }
//
//
//    @Test
//    public void testSqlSingleDateWhereFilter() {
//        polyphenyDbAssert()
//                .query( "SELECT dateValue\n"
//                        + "FROM geode.allDataTypesRegion\n"
//                        + "WHERE dateValue = DATE '2018-02-03'" )
//                .returnsCount( 1 )
//                .queryContains( GeodeAssertions.query( "SELECT dateValue AS dateValue FROM /allDataTypesRegion WHERE dateValue = DATE '2018-02-03'" ) );
//
//        polyphenyDbAssert()
//                .query( "SELECT dateValue\n"
//                        + "FROM geode.allDataTypesRegion\n"
//                        + "WHERE dateValue > DATE '2018-02-03'" )
//                .returnsCount( 2 )
//                .queryContains( GeodeAssertions.query( "SELECT dateValue AS dateValue FROM /allDataTypesRegion WHERE dateValue > DATE '2018-02-03'" ) );
//
//        polyphenyDbAssert()
//                .query( "SELECT dateValue\n"
//                        + "FROM geode.allDataTypesRegion\n"
//                        + "WHERE dateValue < DATE '2018-02-03'" )
//                .returnsCount( 0 )
//                .queryContains( GeodeAssertions.query( "SELECT dateValue AS dateValue FROM /allDataTypesRegion WHERE dateValue < DATE '2018-02-03'" ) );
//    }
//
//
//    @Test
//    public void testSqlMultipleDateWhereFilter() {
//        polyphenyDbAssert()
//                .query( "SELECT dateValue\n"
//                        + "FROM geode.allDataTypesRegion\n"
//                        + "WHERE dateValue = DATE '2018-02-03'\n"
//                        + "  OR dateValue = DATE '2018-02-04'" )
//                .returnsCount( 2 )
//                .queryContains( GeodeAssertions.query( "SELECT dateValue AS dateValue FROM /allDataTypesRegion WHERE dateValue IN SET(DATE '2018-02-03', DATE '2018-02-04')" ) );
//    }
//
//
//    @Test
//    public void testSqlSingleTimeWhereFilter() {
//        polyphenyDbAssert()
//                .query( "SELECT timeValue\n"
//                        + "FROM geode.allDataTypesRegion\n"
//                        + "WHERE timeValue = TIME '02:22:23'" )
//                .returnsCount( 1 )
//                .queryContains( GeodeAssertions.query( "SELECT timeValue AS timeValue FROM /allDataTypesRegion WHERE timeValue = TIME '02:22:23'" ) );
//
//        polyphenyDbAssert()
//                .query( "SELECT timeValue\n"
//                        + "FROM geode.allDataTypesRegion\n"
//                        + "WHERE timeValue > TIME '02:22:23'" )
//                .returnsCount( 2 )
//                .queryContains( GeodeAssertions.query( "SELECT timeValue AS timeValue FROM /allDataTypesRegion WHERE timeValue > TIME '02:22:23'" ) );
//
//        polyphenyDbAssert()
//                .query( "SELECT timeValue\n"
//                        + "FROM geode.allDataTypesRegion\n"
//                        + "WHERE timeValue < TIME '02:22:23'" )
//                .returnsCount( 0 )
//                .queryContains( GeodeAssertions.query( "SELECT timeValue AS timeValue FROM /allDataTypesRegion WHERE timeValue < TIME '02:22:23'" ) );
//    }
//
//
//    @Test
//    public void testSqlMultipleTimeWhereFilter() {
//        polyphenyDbAssert()
//                .query( "SELECT timeValue\n"
//                        + "FROM geode.allDataTypesRegion\n"
//                        + "WHERE timeValue = TIME '02:22:23'\n"
//                        + "  OR timeValue = TIME '03:22:23'" )
//                .returnsCount( 2 )
//                .queryContains( GeodeAssertions.query( "SELECT timeValue AS timeValue FROM /allDataTypesRegion WHERE timeValue IN SET(TIME '02:22:23', TIME '03:22:23')" ) );
//    }
//
//
//    @Test
//    public void testSqlSingleTimestampWhereFilter() {
//        polyphenyDbAssert()
//                .query( "SELECT timestampValue\n"
//                        + "FROM geode.allDataTypesRegion\n"
//                        + "WHERE timestampValue = TIMESTAMP '2018-02-03 02:22:33'" )
//                .returnsCount( 1 )
//                .queryContains( GeodeAssertions.query( "SELECT timestampValue AS timestampValue FROM /allDataTypesRegion WHERE timestampValue = TIMESTAMP '2018-02-03 02:22:33'" ) );
//
//        polyphenyDbAssert()
//                .query( "SELECT timestampValue\n"
//                        + "FROM geode.allDataTypesRegion\n"
//                        + "WHERE timestampValue > TIMESTAMP '2018-02-03 02:22:33'" )
//                .returnsCount( 2 )
//                .queryContains( GeodeAssertions.query( "SELECT timestampValue AS timestampValue FROM /allDataTypesRegion WHERE timestampValue > TIMESTAMP '2018-02-03 02:22:33'" ) );
//
//        polyphenyDbAssert()
//                .query( "SELECT timestampValue\n"
//                        + "FROM geode.allDataTypesRegion\n"
//                        + "WHERE timestampValue < TIMESTAMP '2018-02-03 02:22:33'" )
//                .returnsCount( 0 )
//                .queryContains( GeodeAssertions.query( "SELECT timestampValue AS timestampValue FROM /allDataTypesRegion WHERE timestampValue < TIMESTAMP '2018-02-03 02:22:33'" ) );
//    }
//
//
//    @Test
//    public void testSqlMultipleTimestampWhereFilter() {
//        polyphenyDbAssert()
//                .query( "SELECT timestampValue\n"
//                        + "FROM geode.allDataTypesRegion\n"
//                        + "WHERE timestampValue = TIMESTAMP '2018-02-03 02:22:33'\n"
//                        + "  OR timestampValue = TIMESTAMP '2018-02-05 04:22:33'" )
//                .returnsCount( 2 )
//                .queryContains( GeodeAssertions.query( "SELECT timestampValue AS timestampValue FROM /allDataTypesRegion WHERE timestampValue IN SET(TIMESTAMP '2018-02-03 02:22:33', TIMESTAMP '2018-02-05 04:22:33')" ) );
//    }
//
//
//    @Test
//    public void testSqlWhereWithMultipleOrForAllFields() {
//        polyphenyDbAssert()
//                .query( "SELECT stringValue "
//                        + "FROM geode.allDataTypesRegion WHERE (stringValue = 'abc' OR stringValue = 'def') OR "
//                        + "(floatValue = 1.5678 OR floatValue = null) OR "
//                        + "(dateValue = DATE '2018-02-05' OR dateValue = DATE '2018-02-06' ) OR "
//                        + "(timeValue = TIME '03:22:23' OR timeValue = TIME '07:22:23') OR "
//                        + "(timestampValue = TIMESTAMP '2018-02-05 04:22:33' OR "
//                        + "timestampValue = TIMESTAMP '2017-02-05 04:22:33') OR "
//                        + "(booleanValue = true OR booleanValue = false OR booleanValue = null)" )
//                .returnsCount( 3 )
//                .queryContains(
//                        GeodeAssertions.query( "SELECT stringValue AS stringValue "
//                                + "FROM /allDataTypesRegion WHERE "
//                                + "stringValue IN SET('abc', 'def') OR floatValue IN SET(1.5678, null) OR dateValue "
//                                + "IN SET(DATE '2018-02-05', DATE '2018-02-06') OR timeValue "
//                                + "IN SET(TIME '03:22:23', TIME '07:22:23') OR timestampValue "
//                                + "IN SET(TIMESTAMP '2018-02-05 04:22:33', TIMESTAMP '2017-02-05 04:22:33') "
//                                + "OR booleanValue IN SET(true, false, null)" ) );
//    }
}
