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

package org.polypheny.db.ddl;

import java.util.Map;
import org.apache.calcite.linq4j.function.Deterministic;
import org.polypheny.db.adapter.DeployMode;
import org.polypheny.db.catalog.Catalog;
import org.polypheny.db.catalog.entity.CatalogAdapter.AdapterType;
import org.polypheny.db.catalog.logistic.NamespaceType;
import org.polypheny.db.iface.QueryInterfaceManager;
import org.polypheny.db.iface.QueryInterfaceManager.QueryInterfaceType;

@Deterministic
public class DefaultInserter {


    /**
     * Fills the catalog database with default data, skips if data is already inserted
     */
    public static void resetData( DdlManager ddlManager ) {
        final Catalog catalog = Catalog.getInstance();
        restoreUsers( catalog );

        //////////////
        // init schema

        if ( catalog.getSnapshot().getNamespace( "public" ).isEmpty() ) {
            catalog.addNamespace( "public", NamespaceType.getDefault(), false );
        }


        //////////////
        // init adapters

        restoreAdapters( ddlManager, catalog );

        catalog.commit();

    }


    public static void restoreInterfaces() {
        restoreAvatica();
        restoreInterfacesIfNecessary();
    }


    private static void restoreAdapters( DdlManager ddlManager, Catalog catalog ) {
        if ( !catalog.getAdapters().isEmpty() ) {
            catalog.commit();
            return;
        }

        catalog.updateSnapshot();

        // Deploy default storeId
        Map<String, String> defaultStore = Catalog.snapshot().getAdapterTemplate( Catalog.defaultStore.getAdapterName(), AdapterType.STORE ).orElseThrow().getDefaultSettings();
        ddlManager.addAdapter( "hsqldb", Catalog.defaultStore.getAdapterName(), AdapterType.STORE, defaultStore, DeployMode.EMBEDDED );
        // Deploy default CSV view
        Map<String, String> defaultSource = Catalog.snapshot().getAdapterTemplate( Catalog.defaultSource.getAdapterName(), AdapterType.SOURCE ).orElseThrow().getDefaultSettings();
        ddlManager.addAdapter( "hr", Catalog.defaultSource.getAdapterName(), AdapterType.SOURCE, defaultSource, DeployMode.REMOTE );
    }


    private static void restoreUsers( Catalog catalog ) {
        //////////////
        // init users
        long systemId = catalog.addUser( "system", "" );

        catalog.addUser( "pa", "" );

        Catalog.defaultUserId = systemId;
    }


    public static void restoreInterfacesIfNecessary() {
        ////////////////////////
        // init query interfaces
        if ( !Catalog.getInstance().getInterfaces().isEmpty() ) {
            return;
        }
        QueryInterfaceManager.getREGISTER().values().forEach( i -> Catalog.getInstance().addQueryInterface( i.interfaceName, i.clazz.getName(), i.defaultSettings ) );
        Catalog.getInstance().commit();

    }


    public static void restoreAvatica() {
        if ( Catalog.snapshot().getQueryInterface( "avatica" ).isPresent() ) {
            return;
        }
        QueryInterfaceType avatica = QueryInterfaceManager.getREGISTER().get( "AvaticaInterface" );
        Catalog.getInstance().addQueryInterface( "avatica", avatica.clazz.getName(), avatica.defaultSettings );
    }

}
