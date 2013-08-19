/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.data.sqlserver;

import org.geotools.jdbc.JDBCDataStoreAPITestSetup;

/**
 * 
 *
 * @source $URL$
 */
public class SQLServerDataStoreAPITestSetup extends JDBCDataStoreAPITestSetup {

    public SQLServerDataStoreAPITestSetup() {
        super(new SQLServerTestSetup());
    }

    @Override
    protected void createRoadTable() throws Exception {
        run("CREATE TABLE road(fid int IDENTITY(0,1) PRIMARY KEY, id int, "
            + "geom geometry, name nvarchar(255) )"); //use nvarchar to test nvarchar mappings (GEOT-3609)
        run("INSERT INTO road (id,geom,name) VALUES (0,"
            + "geometry::STGeomFromText('LINESTRING(1 1, 2 2, 4 2, 5 1)',4326)," + "'r1')");
        run("INSERT INTO road (id,geom,name) VALUES ( 1,"
            + "geometry::STGeomFromText('LINESTRING(3 0, 3 2, 3 3, 3 4)',4326)," + "'r2')");
        run("INSERT INTO road (id,geom,name) VALUES ( 2,"
            + "geometry::STGeomFromText('LINESTRING(3 2, 4 2, 5 3)',4326)," + "'r3')");
        
        run("CREATE SPATIAL INDEX _road_geometry_index on road(geom) WITH (BOUNDING_BOX = (-10, -10, 10, 10))");
    }

    @Override
    protected void createRiverTable() throws Exception {
        run("CREATE TABLE river(fid int IDENTITY(0,1) PRIMARY KEY, id int, "
            + "geom geometry, river nvarchar(255) , flow float )");

        run("INSERT INTO river (id,geom,river, flow)  VALUES ( 0,"
            + "geometry::STGeomFromText('MULTILINESTRING((5 5, 7 4),(7 5, 9 7, 13 7),(7 5, 9 3, 11 3))',4326),"
            + "'rv1', 4.5)");
        run("INSERT INTO river (id,geom,river, flow) VALUES ( 1,"
            + "geometry::STGeomFromText('MULTILINESTRING((4 6, 4 8, 6 10))',4326)," + "'rv2', 3.0)");
        
        run("CREATE SPATIAL INDEX _river_geometry_index on river(geom) WITH (BOUNDING_BOX = (-10, -10, 10, 10))");
    }

    @Override
    protected void createLakeTable() throws Exception {
        run("CREATE TABLE lake(fid int IDENTITY(0,1) PRIMARY KEY, id int, "
            + "geom geometry, name varchar(255) )");

        run("INSERT INTO lake (id,geom,name) VALUES ( 0,"
            + "geometry::STGeomFromText('POLYGON((12 6, 14 8, 16 6, 16 4, 14 4, 12 6))',4326)," + "'muddy')");
        
        run("CREATE SPATIAL INDEX _lake_geometry_index on lake(geom) WITH (BOUNDING_BOX = (-100, -100, 100, 100))");
    }

    @Override
    protected void dropRoadTable() throws Exception {
        run("DROP TABLE road");
    }

    @Override
    protected void dropRiverTable() throws Exception {
        run("DROP TABLE river");
    }

    @Override
    protected void dropLakeTable() throws Exception {
        run("DROP TABLE lake");
    }

    @Override
    protected void dropBuildingTable() throws Exception {
        run("DROP TABLE building");
    }

	@Override
	protected void createBuildingTable() throws Exception {
		/**
		 * (10 40), (40 30), (20 20), (30 10)
		 */
		run("CREATE TABLE building(fid int IDENTITY(0,1) PRIMARY KEY, id int, "
	            + "geom geometry, name nvarchar(255) )"); //use nvarchar to test nvarchar mappings (GEOT-3609)
	        run("INSERT INTO building (id,geom,name) VALUES (0,"
	            + "geometry::STGeomFromText('POINT(10 40.00001)',4326)," + "'b1')");
	        run("INSERT INTO building (id,geom,name) VALUES ( 1,"
	            + "geometry::STGeomFromText('POINT(10 40.00002)',4326)," + "'b2')");
	        run("INSERT INTO building (id,geom,name) VALUES ( 2,"
	            + "geometry::STGeomFromText('POINT(10 40.00003)',4326)," + "'b3')");
	        run("INSERT INTO building (id,geom,name) VALUES ( 3,"
		        + "geometry::STGeomFromText('POINT(10 40.00004)',4326)," + "'b4')");
	        run("INSERT INTO building (id,geom,name) VALUES ( 3,"
		        + "geometry::STGeomFromText('POINT(10 40.00005)',4326)," + "'b5')");
	        
	        //run("CREATE SPATIAL INDEX _building_geometry_index on building(geom) WITH (BOUNDING_BOX = (-180, -90, 180, 90), GRIDS =(LEVEL_1 = MEDIUM,LEVEL_2 = MEDIUM,LEVEL_3 = MEDIUM,LEVEL_4 = MEDIUM)CELLS_PER_OBJECT = 16, PAD_INDEX  = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON)");
	        run("CREATE SPATIAL INDEX _building_geometry_index on building(geom) WITH (BOUNDING_BOX = (-180, -90, 180, 90), GRIDS =(LOW, LOW, LOW, LOW),CELLS_PER_OBJECT = 1, PAD_INDEX  = ON)");
			
	}
}
