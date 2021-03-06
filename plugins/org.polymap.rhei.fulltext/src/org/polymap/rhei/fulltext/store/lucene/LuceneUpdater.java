/* 
 * polymap.org
 * Copyright (C) 2014, Falko Bräutigam. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.polymap.rhei.fulltext.store.lucene;

import static org.polymap.rhei.fulltext.FulltextIndex.FIELD_ID;
import static org.polymap.rhei.fulltext.FulltextIndex.FIELD_SRS;

import org.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.jface.util.Geometry;

import org.polymap.rhei.fulltext.update.UpdateableFulltextIndex.Updater;

import org.polymap.recordstore.IRecordState;
import org.polymap.recordstore.IRecordStore;
import org.polymap.recordstore.lucene.LuceneRecordStore;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
class LuceneUpdater
        implements Updater {

    private static Log log = LogFactory.getLog( LuceneUpdater.class );

    private LuceneFulltextIndex     index;

    private LuceneRecordStore       store;

    private IRecordStore.Updater    updator;
    

    public LuceneUpdater( LuceneFulltextIndex index ) {
        this.index = index;
        this.store = index.store;
        this.updator = store.prepareUpdate();
    }


    @Override
    public void store( JSONObject feature, boolean update ) throws Exception {
        assert updator != null : "Updator is already closed.";
        String fid = feature.getString( FIELD_ID );
        assert fid != null : "No FIELD_ID in feature: " + feature;

        IRecordState record = update ? store.get( fid ) : null;
        if (record == null) {
            record = store.newRecord( fid );
        }
        
        StringBuilder buf = new StringBuilder( 256 );
        for (Object key : feature.keySet()) {
            Object value = feature.get( (String)key );
            // no value
            if (value == JSONObject.NULL) {
                continue;
            }
            // String
            else if (value instanceof String ) {
                record.put( (String)key, value );
                if (key != FIELD_ID && key != FIELD_SRS) {
                    buf.append( value ).append( ' ' );
                }
            }
            // Geometry
            else if (value instanceof Geometry ) {
                record.put( (String)key, value );
            }
            else {
                throw new RuntimeException( "Feature is not simple. Property: " + key + " = " + value );
            }
        }
        record.put( LuceneFulltextIndex.FIELD_ANALYZED, buf.toString() );
        
        updator.store( record );
    }


    @Override
    public void remove( String fid ) throws Exception {
        assert updator != null : "Updator is already closed.";
        IRecordState record = store.get( fid );
        updator.remove( record );
    }


    @Override
    public void apply() {
        assert updator != null : "Updator is already closed.";
        updator.apply();
        updator = null;
    }


    @Override
    public void close() {
        if (updator != null) {
            updator.discard();
            updator = null;
        }
    }

}
