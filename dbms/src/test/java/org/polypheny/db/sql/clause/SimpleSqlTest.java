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

package org.polypheny.db.sql.clause;

import java.sql.SQLException;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.polypheny.db.TestHelper;

@SuppressWarnings({ "SqlDialectInspection", "SqlNoDataSourceInspection" })
public class SimpleSqlTest {

    @BeforeClass
    public static void start() throws SQLException {
        // Ensures that Polypheny-DB is running
        //noinspection ResultOfMethodCallIgnored
        TestHelper.getInstance();
        insertData();
    }


    private static void insertData() {

    }


    @Test
    @Ignore
    public void createTable() {
        TestHelper.executeSql(
                ( c, s ) -> s.executeUpdate( "CREATE TABLE TableA(ID INTEGER NOT NULL, NAME VARCHAR(20), AGE INTEGER, PRIMARY KEY (ID))" )
        );
    }


    @Test
    public void dropTable() {
        TestHelper.executeSql(
                ( c, s ) -> s.executeUpdate( "CREATE TABLE TableA(ID INTEGER NOT NULL, NAME VARCHAR(20), AGE INTEGER, PRIMARY KEY (ID))" ),
                ( c, s ) -> s.executeUpdate( "DROP TABLE TableA" )
        );
    }


    @Test
    public void insert() throws SQLException {
        TestHelper.executeSql(
                ( c, s ) -> s.executeUpdate( "CREATE TABLE TableA(ID INTEGER NOT NULL, NAME VARCHAR(20), AGE INTEGER, PRIMARY KEY (ID))" ),
                ( c, s ) -> s.executeUpdate( "INSERT INTO TableA VALUES (12, 'Name1', 60)" ),
                ( c, s ) -> s.executeUpdate( "INSERT INTO TableA VALUES (15, 'Name2', 24)" ),
                ( c, s ) -> s.executeUpdate( "INSERT INTO TableA VALUES (99, 'Name3', 11)" ),
                ( c, s ) -> s.executeUpdate( "DROP TABLE TableA" ),
                ( c, s ) -> c.commit()
        );

    }


    @Test
    public void select() throws SQLException {
        List<Object[]> data = List.of(
                new Object[]{ 12, "Name1", 60 },
                new Object[]{ 15, "Name2", 24 },
                new Object[]{ 99, "Name3", 11 }
        );
        TestHelper.executeSql(
                ( c, s ) -> s.executeUpdate( "CREATE TABLE TableA(ID INTEGER NOT NULL, NAME VARCHAR(20), AGE INTEGER, PRIMARY KEY (ID))" ),
                ( c, s ) -> s.executeUpdate( "INSERT INTO TableA VALUES (12, 'Name1', 60)" ),
                ( c, s ) -> s.executeUpdate( "INSERT INTO TableA VALUES (15, 'Name2', 24)" ),
                ( c, s ) -> s.executeUpdate( "INSERT INTO TableA VALUES (99, 'Name3', 11)" ),
                ( c, s ) -> TestHelper.checkResultSet( s.executeQuery( "SELECT * FROM TableA" ), data, true ),
                ( c, s ) -> s.executeUpdate( "DROP TABLE TableA" ),
                ( c, s ) -> c.commit()
        );

    }

}
