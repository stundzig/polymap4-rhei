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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Predicate;

import org.polymap.core.runtime.session.SessionContext;
import org.polymap.core.runtime.session.SessionSingleton;

import org.polymap.rhei.um.providers.UserRepositorySPI;

/**
 * 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
public class UserRepository
        extends SessionSingleton {

    private static Log log = LogFactory.getLog( UserRepository.class );

    /**
     * Returns the instance of the current {@link SessionContext session}.
     */
    public static UserRepository instance() {
        return instance( UserRepository.class );
    }
    
    
    // instance *******************************************

    private UserRepositorySPI   provider;
    
    /**
     * Creates a new instance of the current session.
     */
    protected UserRepository() {
        // FIXME
        throw new RuntimeException( "not yet implemented: provider = QiUserRepository.instance();" );
    }
    
    
    public <T extends Entity> Iterable<T> find( Class<T> type, Predicate<T> filter ) {
        return provider.find( type, filter );
    }
    
    
    public List<String> groupsOf( Groupable groupable ) {
        return provider.groupsOf( groupable );
    }


    public boolean asignGroup( Groupable user, String group ) {
        return provider.asignGroup( user, group );
    }


    public boolean resignGroup( Groupable user, String group ) {
        return provider.resignGroup( user, group );
    }


    public User newUser() {
        return provider.newUser();
    }

    
    public void deleteUser( User user ) {
        provider.deleteUser( user );
    }


    public User findUser( String username ) {
        return provider.findUser( username );
    }


    public void commitChanges() {
        provider.commit();
    }

    
    public void revertChanges() {
        provider.revert();        
    }

}
