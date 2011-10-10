/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2007-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.data.ogr;

import static org.bridj.Pointer.*;
import static org.geotools.data.ogr.bridj.OgrLibrary.*;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.bridj.Pointer;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Maps OGR features into Geotools ones, and vice versa. Chances are that if you need to update a
 * decode method a simmetric modification will be needed in the encode method. This class is not
 * thread safe, so each thread should create its own instance.
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
class FeatureMapper {

    SimpleFeatureBuilder builder;
    
    SimpleFeatureType schema;
    
    GeometryMapper geomMapper;
    
    GeometryFactory geomFactory;

    /**
     * The date time format used by OGR when getting/setting times using strings
     */
    DateFormat dateTimeFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");

    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

    DateFormat timeFormat = new SimpleDateFormat("hh:mm:ss");

    HashMap<String, Integer> attributeIndexes;

    public FeatureMapper(SimpleFeatureType targetSchema, SimpleFeatureType sourceSchema, GeometryFactory geomFactory) {
        this.schema = targetSchema;
        this.builder = new SimpleFeatureBuilder(schema);
        this.geomMapper = new GeometryMapper.WKB(geomFactory);
        this.geomFactory = geomFactory;
        
        attributeIndexes = new HashMap<String, Integer>();
        List<AttributeDescriptor> descriptors = sourceSchema.getAttributeDescriptors();
        for (int i = 0; i < descriptors.size(); i++) {
            AttributeDescriptor ad = descriptors.get(i);
            if(!(ad instanceof GeometryDescriptor) && targetSchema.getDescriptor(ad.getLocalName()) != null) {
                // the first one is the geometry, which does not count in ogr as an index
                attributeIndexes.put(ad.getLocalName(), i - 1);
            }
        }
    }

    /**
     * Converts an OGR feature into a GeoTools one
     * 
     * @param schema
     * @param ogrFeature
     * @return
     * @throws IOException
     */
    SimpleFeature convertOgrFeature(Pointer<?> ogrFeature)
            throws IOException {
        // Extract all attributes (do not assume any specific order, the feature
        // type may have been re-ordered by the Query)
        Object[] attributes = new Object[schema.getAttributeCount()];

        // .. then extract each attribute using the attribute type to determine
        // which extraction method to call
        for (int i = 0; i < attributes.length; i++) {
            AttributeDescriptor at = schema.getDescriptor(i);
            builder.add(getOgrField(at, ogrFeature));
        }

        // .. gather the FID
        String fid = convertOGRFID(schema, ogrFeature);

        // .. finally create the feature
        return builder.buildFeature(fid);
    }

//    /**
//     * Turns a GeoTools feature into an OGR one
//     * 
//     * @param feature
//     * @return
//     * @throws DataSourceException
//     */
//    org.gdal.ogr.Feature convertGTFeature(FeatureDefn ogrSchema, SimpleFeature feature)
//            throws IOException {
//        // create a new empty OGR feature
//        org.gdal.ogr.Feature result = new org.gdal.ogr.Feature(ogrSchema);
//
//        // go thru GeoTools feature attributes, and convert
//        SimpleFeatureType schema = feature.getFeatureType();
//        for (int i = 0, j = 0; i < schema.getAttributeCount(); i++) {
//            AttributeDescriptor at = schema.getDescriptor(i);
//            Object attribute = feature.getAttribute(i);
//            if (at instanceof GeometryDescriptor) {
//                // using setGeoemtryDirectly the feature becomes the owner of the generated
//                // OGR geometry and we don't have to .delete() it (it's faster, too)
//                result.SetGeometryDirectly(geomMapper.parseGTGeometry((Geometry) attribute));
//                continue;
//            }
//
//            if (attribute == null) {
//                result.UnsetField(j);
//            } else {
//                final FieldDefn ogrField = ogrSchema.GetFieldDefn(j);
//                final int ogrType = ogrField.GetFieldType();
//                ogrField.delete();
//                if (ogrType == ogr.OFTInteger)
//                    result.SetField(j, ((Number) attribute).intValue());
//                else if (ogrType == ogr.OFTReal)
//                    result.SetField(j, ((Number) attribute).doubleValue());
//                else if (ogrType == ogr.OFTDateTime)
//                    result.SetField(j, dateTimeFormat.format((java.util.Date) attribute));
//                else if (ogrType == ogr.OFTDate)
//                    result.SetField(j, dateFormat.format((java.util.Date) attribute));
//                else if (ogrType == ogr.OFTTime)
//                    result.SetField(j, timeFormat.format((java.util.Date) attribute));
//                else
//                    result.SetField(j, attribute.toString());
//            }
//            j++;
//        }
//
//        return result;
//    }

    /**
     * Turns line and polygon into multiline and multipolygon. This is a stop-gap measure to make
     * things works against shapefiles, I've asked the GDAL mailing list on how to properly handle
     * this in the meantime
     * 
     * @param ogrGeometry
     * @param ad
     * @return
     */
    Geometry fixGeometryType(Geometry ogrGeometry, AttributeDescriptor ad) {
        if (MultiPolygon.class.equals(ad.getType())) {
            if (ogrGeometry instanceof MultiPolygon)
                return ogrGeometry;
            else
                return geomFactory.createMultiPolygon(new Polygon[] { (Polygon) ogrGeometry });
        } else if (MultiLineString.class.equals(ad.getType())) {
            if (ogrGeometry instanceof MultiLineString)
                return ogrGeometry;
            else
                return geomFactory
                        .createMultiLineString(new LineString[] { (LineString) ogrGeometry });
        }
        return ogrGeometry;

    }


    /**
     * Reads the current feature's specified field using the most appropriate OGR field extraction
     * method
     * 
     * @param ad
     * @return
     */
    Object getOgrField(AttributeDescriptor ad, Pointer<?> ogrFeature) throws IOException {
        if(ad instanceof GeometryDescriptor) {
            // gets the geometry as a reference, we don't own it, we should not deallocate it
            Pointer<?> ogrGeometry = OGR_F_GetGeometryRef(ogrFeature);
            return fixGeometryType(geomMapper.parseOgrGeometry(ogrGeometry), ad);
        }
        
        Integer idx = attributeIndexes.get(ad.getLocalName());

        // check for null fields
        if (idx == null || OGR_F_IsFieldSet(ogrFeature, idx) == 0) {
            return null;
        }

        // hum, ok try and parse it
        Class clazz = ad.getType().getBinding();
        if (clazz.equals(String.class)) {
            return  OGR_F_GetFieldAsString(ogrFeature, idx).getCString();
        } else if (clazz.equals(Integer.class)) {
            return OGR_F_GetFieldAsInteger(ogrFeature, idx);
        } else if (clazz.equals(Double.class)) {
            return OGR_F_GetFieldAsDouble(ogrFeature, idx);
        } else if (clazz.equals(Float.class)) {
            return (float) OGR_F_GetFieldAsDouble(ogrFeature, idx);
        } else if (clazz.equals(java.sql.Date.class)) {
            Calendar cal = getDateField(ogrFeature, idx);
            cal.clear(Calendar.HOUR_OF_DAY);
            cal.clear(Calendar.MINUTE);
            cal.clear(Calendar.SECOND);
            return new java.sql.Date(cal.getTimeInMillis());
        } else if (clazz.equals(java.sql.Time.class)) {
            Calendar cal = getDateField(ogrFeature, idx);
            cal.clear(Calendar.YEAR);
            cal.clear(Calendar.MONTH);
            cal.clear(Calendar.DAY_OF_MONTH);
            return new java.sql.Time(cal.getTimeInMillis());
        } else if (clazz.equals(java.sql.Timestamp.class)) {
            Calendar cal = getDateField(ogrFeature, idx);
            return new java.sql.Time(cal.getTimeInMillis());
        } else if (clazz.equals(java.util.Date.class)) {
            Calendar cal = getDateField(ogrFeature, idx);
            return cal.getTime();
        } else {
            throw new IllegalArgumentException("Don't know how to read " + clazz.getName()
                    + " fields");
        }
    }

    /**
     * Reads a date field from the OGR api
     * @param ogrFeature
     * @param idx
     * @return
     */
    private Calendar getDateField(Pointer<?> ogrFeature, Integer idx) {
        Pointer<Integer> year = allocateInt();
        Pointer<Integer> month = allocateInt();
        Pointer<Integer> day = allocateInt();
        Pointer<Integer> hour = allocateInt();
        Pointer<Integer> minute = allocateInt();
        Pointer<Integer> second = allocateInt();
        Pointer<Integer> timeZone = allocateInt();
        
        OGR_F_GetFieldAsDateTime(ogrFeature, idx, year, month, day, hour, minute, second, timeZone);
        
        Calendar cal = Calendar.getInstance();
        // from ogr_core.h 
        // 0=unknown, 1=localtime(ambiguous), 100=GMT, 104=GMT+1, 80=GMT-5, etc
        int tz = timeZone.getInt();
        if(tz != 0 && tz != 1) {
            int offset = tz - 100 / 4;
            if(offset < 0) {
                cal.setTimeZone(TimeZone.getTimeZone("GMT" + offset));
            } else if(offset == 0) {
                cal.setTimeZone(TimeZone.getTimeZone("GMT"));
            } else {
                cal.setTimeZone(TimeZone.getTimeZone("GMT+" + offset));
            }               
        }
        cal.clear();
        cal.set(Calendar.YEAR, year.getInt());
        cal.set(Calendar.MONTH, month.getInt());
        cal.set(Calendar.DAY_OF_MONTH, day.getInt());
        cal.set(Calendar.HOUR_OF_DAY, hour.getInt());
        cal.set(Calendar.MINUTE, minute.getInt());
        cal.set(Calendar.SECOND, second.getInt());
        return cal;
    }

    /**
     * Generates a GT2 feature id given its feature type and an OGR feature
     * 
     * @param schema
     * @param ogrFeature
     * @return
     */
    String convertOGRFID(SimpleFeatureType schema, Pointer<?> ogrFeature) {
        long id = OGR_F_GetFID(ogrFeature);
        return schema.getTypeName() + "." + id;
    }

    /**
     * Decodes a GT2 feature id into an OGR one
     * 
     * @param feature
     * @return
     */
    long convertGTFID(SimpleFeature feature) {
        String id = feature.getID();
        return Long.parseLong(id.substring(id.indexOf(".") + 1));
    }

}