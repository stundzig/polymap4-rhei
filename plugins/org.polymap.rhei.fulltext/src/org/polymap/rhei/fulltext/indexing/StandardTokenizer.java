/* 
 * polymap.org
 * Copyright (C) 2014-2015, Falko Bräutigam. All rights reserved.
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

import org.polymap.rhei.fulltext.update.UpdateableFulltextIndex;

/**
 * Use whitespace and special chars (.,;:-\\/@"'()[]{}) as token delimiter.
 * 
 * @see UpdateableFulltextIndex
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class StandardTokenizer
        implements FulltextTokenizer {

    @Override
    public boolean isTokenChar( int c ) {
        switch (c) {
            case ' '    : return false;   
            case '\t'   : return false;
            case '\r'   : return false;
            case '\n'   : return false;
            case '.'    : return false;
            case ','    : return false;
            case ';'    : return false;
            case ':'    : return false;
            case '-'    : return false;
            case '\\'   : return false;
            case '/'    : return false;
            case '@'    : return false;
            case '"'    : return false;
            case '\''   : return false;
            case '{'    : return false;
            case '}'    : return false;
            case '['    : return false;
            case ']'    : return false;
            // http://polymap.org/atlas/ticket/77
            case '('    : return false;
            case ')'    : return false;
            
            default     : return true;
        }
    }
    
}
