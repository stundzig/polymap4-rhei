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
package org.polymap.rhei.fulltext.indexing;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.LowerCaseFilter;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

import org.polymap.rhei.fulltext.FulltextIndex;
import org.polymap.rhei.fulltext.QueryDecorator;

/**
 * Provides a {@link FulltextTokenFilter} and {@link QueryDecorator} to normalize
 * proposal and query strings to lower case. The proposal results are capitalized.
 * 
 * @see LowerCaseFilter
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class LowerCaseTokenFilter
        extends QueryDecorator
        implements FulltextTokenFilter {

    /**
     * Ctor for {@link FulltextTokenFilter}.
     */
    public LowerCaseTokenFilter() {
        super( null );
    }

    /**
     * Ctor for {@link QueryDecorator}.
     */
    public LowerCaseTokenFilter( FulltextIndex next ) {
        super( next );
    }


    @Override
    public Iterable<String> propose( String query, int maxResults, String field ) throws Exception {
        // just the "_analyzed_" field gets analyzed by Lucene -> just
        // this field gets lowered by my #apply()
        if (field != null) {
            return next.propose( query, maxResults, field );
        }
        else {
            query = query.toLowerCase();

            Iterable<String> results = next.propose( query, maxResults, field );

            return Iterables.transform( results, new Function<String,String>() {
                public String apply( String input ) {
                    return StringUtils.capitalize( input );
                }
            });
        }
    }


    @Override
    public String apply( String term ) {
        return term.toLowerCase();
    }
    
}
