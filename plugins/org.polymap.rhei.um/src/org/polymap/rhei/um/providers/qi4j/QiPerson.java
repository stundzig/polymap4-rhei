/* 
 * polymap.org
 * Copyright 2013, Polymap GmbH. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.polymap.rhei.um.providers.qi4j;

import org.qi4j.api.common.Optional;
import org.qi4j.api.common.UseDefaults;
import org.qi4j.api.concern.Concerns;
import org.qi4j.api.entity.EntityComposite;
import org.qi4j.api.mixin.Mixins;
import org.qi4j.api.property.Property;
import org.qi4j.polymap.QiEntity;
import org.qi4j.polymap.event.ModelChangeSupport;
import org.qi4j.polymap.event.PropertyChangeSupport;

import org.polymap.rhei.um.Address;
import org.polymap.rhei.um.Person;
import org.polymap.rhei.um.User;

/**
 * The Qi4j implementation of {@link User}. 
 *
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
@Concerns( {
    PropertyChangeSupport.Concern.class
})
@Mixins( {
    QiPerson.Mixin.class, 
    PropertyChangeSupport.Mixin.class,
    ModelChangeSupport.Mixin.class,
    QiEntity.Mixin.class,
    QiPerson.Mixin.class
})
public interface QiPerson
        extends Person, QiEntity, PropertyChangeSupport, EntityComposite {
    
    @UseDefaults
    Property<String>            _name();
    
    @Optional
    @UseDefaults
    Property<String>            _firstname();
    
    @Optional
    Property<String>            _salutation();

    @Optional
    Property<QiAddressValue>    _address();
    
    @Optional
    Property<String>            _email();
    
    @Optional
    Property<String>            _web();
    
    @Optional
    Property<String>            _phone();
    
    @Optional
    Property<String>            _mobilePhone();
    
    @Optional
    Property<String>            _fax();
    
    
    /**
     * Methods and transient fields.
     */
    public static abstract class Mixin
            implements QiPerson {
        
        @Override
        public org.polymap.rhei.um.Property<String> name() {
            return QiProperty.create( _name() );
        }

        @Override
        public org.polymap.rhei.um.Property<String> firstname() {
            return QiProperty.create( _firstname() );
        }

        @Override
        public org.polymap.rhei.um.Property<String> salutation() {
            return QiProperty.create( _salutation() );
        }

        @Override
        public org.polymap.rhei.um.Property<String> email() {
            return QiProperty.create( _email() );
        }

        @Override
        public org.polymap.rhei.um.Property<String> phone() {
            return QiProperty.create( _phone() );
        }

        @Override
        public org.polymap.rhei.um.Property<String> mobilePhone() {
            return QiProperty.create( _mobilePhone() );
        }

        @Override
        public org.polymap.rhei.um.Property<String> fax() {
            return QiProperty.create( _fax() );
        }

        @Override
        public org.polymap.rhei.um.Property<String> web() {
            return QiProperty.create( _web() );
        }

        public org.polymap.rhei.um.Property<Address> address() {
            return new org.polymap.rhei.um.Property() {
                @Override
                public Object get() {
                    return new QiAddressValue.QiAddress( _address().get(), _address() );
                }
                @Override
                public void set( Object value ) {
                    throw new RuntimeException( "Value property. Use the properties of the value to modify." );
                }
                @Override
                public Class type() {
                    throw new RuntimeException( "not yet implemented." );
                }
                @Override
                public String name() {
                    return "address";
                }
            };
        }

        public String getLabelString( String sep ) {
            throw new RuntimeException( "not yet implemented" );
        }
        
    }
    
}
