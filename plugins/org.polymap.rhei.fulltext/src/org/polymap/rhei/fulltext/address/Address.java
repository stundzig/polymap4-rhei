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
package org.polymap.rhei.fulltext.address;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public interface Address {

    public static final String FIELD_STREET = "strasse";
    public static final String FIELD_NUMBER = "nummer";
    public static final String FIELD_NUMBER_X = "nummerx";
    public static final String FIELD_CITY = "ort";
    public static final String FIELD_CITY_X = "ortx";
    public static final String FIELD_POSTALCODE = "plz";
    public static final String FIELD_DISTRICT = "district";

    public static final String[] ALL_FIELDS = {FIELD_STREET, FIELD_CITY, FIELD_CITY_X, FIELD_NUMBER, FIELD_NUMBER_X, FIELD_POSTALCODE, FIELD_DISTRICT};

}
