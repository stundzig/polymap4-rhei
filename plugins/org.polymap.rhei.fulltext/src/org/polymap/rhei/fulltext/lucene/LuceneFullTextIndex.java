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
package org.polymap.rhei.fulltext.lucene;

import static com.google.common.collect.Iterables.limit;
import static com.google.common.collect.Iterables.transform;
import static java.util.Arrays.asList;

import java.util.Comparator;
import java.util.Map.Entry;
import java.util.TreeMap;

import java.io.File;
import java.io.IOException;

import org.json.JSONObject;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.util.Version;

import com.google.common.base.Function;
import org.polymap.core.runtime.recordstore.lucene.GeometryValueCoder;
import org.polymap.core.runtime.recordstore.lucene.LuceneRecordState;
import org.polymap.core.runtime.recordstore.lucene.LuceneRecordStore;
import org.polymap.core.runtime.recordstore.lucene.StringValueCoder;

import org.polymap.rhei.fulltext.FullTextIndex;
import org.polymap.rhei.fulltext.update.UpdateableFullTextIndex;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class LuceneFullTextIndex
        extends UpdateableFullTextIndex
        implements FullTextIndex {

    private static Log log = LogFactory.getLog( LuceneFullTextIndex.class );

    /** The Lucene version we are using, current 3.4 from core.libs. */
    public final static Version     LUCENE_VERSION = Version.LUCENE_34;

    public final static String      FIELD_ANALZED = "_analyzed_";
    
    protected LuceneRecordStore     store;

    private LuceneAnalyzer          analyzer;
    

    public LuceneFullTextIndex( File dir ) throws IOException {
        store = dir != null 
                ? new LuceneRecordStore( dir, false )
                : new LuceneRecordStore();
        store.getValueCoders().clear();
        // StringValueCoder is *last*
        store.getValueCoders().addValueCoder( new StringValueCoder() );
        store.getValueCoders().addValueCoder( new AnalyzedStringValueCoder() );
        store.getValueCoders().addValueCoder( new GeometryValueCoder() );
        
        analyzer = new LuceneAnalyzer( this );
        store.setAnalyzer( analyzer );
    }


    @Override
    public void close() {
        if (store != null) {
            store.close();
            store = null;
        }
    }


    @Override
    protected void finalize() throws Throwable {
        close();
    }


    @Override
    public boolean isClosed() {
        return store != null;
    }


    @Override
    public boolean isEmpty() {
        long storeSize = store.storeSizeInByte();
        log.info( "Store size: " + storeSize );
        return storeSize < 100;
    }


    @Override
    public Iterable<String> propose( String term, int maxResults )
            throws Exception {
        IndexSearcher searcher = store.getIndexSearcher();
        TermEnum terms = searcher.getIndexReader().terms( new Term( FIELD_ANALZED, term ) );
        try {
            // sort descending; accept equal keys
            TreeMap<Integer,String> result = new TreeMap( new Comparator<Integer>() {
                public int compare( Integer o1, Integer o2 ) {
                    return o1.equals( o2 ) ? -1 : -o1.compareTo( o2 );
                }
            });
            // sort
            for (int i=0; i<maxResults*3 && terms.next(); i++) {
                String proposalTerm = terms.term().text();
                int docFreq = terms.docFreq();
                if (!proposalTerm.startsWith( term )) {
                    break;
                }
                log.info( "   Term: " + proposalTerm + ", docFreq: " + docFreq );
                result.put( docFreq, proposalTerm );
            }
            // take first maxResults
            return limit( result.values(), maxResults );
        }
        //
        catch (Exception e) {
            log.warn( e );
            return ListUtils.EMPTY_LIST;
        }
        finally {
            terms.close();
        }
    }


    @Override
    public Iterable<JSONObject> search( String queryStr, int maxResults )
            throws Exception {
        // parse query
        QueryParser parser = new ComplexPhraseQueryParser( LUCENE_VERSION, FIELD_ANALZED, analyzer );
        parser.setAllowLeadingWildcard( true );
        parser.setDefaultOperator( QueryParser.AND_OPERATOR );
        Query query = parser.parse( queryStr );
        log.info( "    ===> Lucene query: " + query );

//        Sort asc = new Sort( new SortField( FIELD_TITLE, SortField.STRING ) );
        IndexSearcher searcher = store.getIndexSearcher();
        ScoreDoc[] hits = searcher.search( query, null, maxResults ).scoreDocs;
        
        // transform result: scroreDoc -> JSONObject
        return transform( asList( hits ), new Function<ScoreDoc,JSONObject>() {
            public JSONObject apply( ScoreDoc input ) {
                try {
                    LuceneRecordState record = store.get( input.doc, null );
                    JSONObject result = new JSONObject();                
                    for (Entry<String,Object> entry : record) {
                        result.put( entry.getKey(), entry.getValue() );
                    }
                    return result;
                }
                catch (Exception e) {
                    throw new RuntimeException( e );
                }
            }
        });
    }


    @Override
    public Updater prepareUpdate() {
        return new LuceneUpdater( this );
    }
    
}