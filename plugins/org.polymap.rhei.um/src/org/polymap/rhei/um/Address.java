/* 
 * polymap.org
 * Copyright (C) 2013, Polymap GmbH. All rights reserved.
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
package org.polymap.rhei.um;

/**
 * Postal address.
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public interface Address {

    public Property<String> street();
    
    public Property<String> number();
    
    public Property<String> postalCode();
    
    public Property<String> city();

    public Property<String> district();

    public Property<String> province();    
    
    public Property<String> country();    
    
}
