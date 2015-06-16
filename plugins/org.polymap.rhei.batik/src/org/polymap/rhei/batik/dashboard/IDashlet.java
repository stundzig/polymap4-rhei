/* 
 * polymap.org
 * Copyright (C) 2015, Falko Bräutigam. All rights reserved.
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
package org.polymap.rhei.batik.dashboard;

import org.eclipse.swt.widgets.Composite;

/**
 * A dashlet is a UI component that is managed by a {@link Dashboard}.
 * 
 * @see Dashboard
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public interface IDashlet {

    public void init( DashletSite site );
   
    /**
     * 
     *
     * @param parent
     * @return Currently not used. May return {@code parent}. Just makes the
     *         signature compatible with
     *         {@link org.polymap.rhei.form.batik.FormContainer}.
     */
   public void createContents( Composite parent );
   
}
