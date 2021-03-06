/*
 * polymap.org 
 * Copyright 2011-2013, Polymap GmbH. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.polymap.rhei.data.entitystore.lucene;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.qi4j.api.common.QualifiedName;
import org.qi4j.api.common.TypeName;
import org.qi4j.api.entity.EntityReference;
import org.qi4j.api.property.Property;
import org.qi4j.api.property.StateHolder;
import org.qi4j.api.structure.Module;
import org.qi4j.api.value.ValueBuilder;
import org.qi4j.api.value.ValueComposite;
import org.qi4j.runtime.property.PersistentPropertyModel;
import org.qi4j.runtime.types.CollectionType;
import org.qi4j.runtime.types.ValueCompositeType;
import org.qi4j.spi.entity.EntityDescriptor;
import org.qi4j.spi.entity.EntityState;
import org.qi4j.spi.entity.EntityStatus;
import org.qi4j.spi.entity.EntityType;
import org.qi4j.spi.entity.ManyAssociationState;
import org.qi4j.spi.property.PropertyDescriptor;
import org.qi4j.spi.property.PropertyType;
import org.qi4j.spi.property.PropertyTypeDescriptor;
import org.qi4j.spi.property.ValueType;

import com.google.common.base.Joiner;

import org.polymap.core.runtime.recordstore.IRecordState;

/**
 *
 *
 * @author <a href="http://www.polymap.de">Falko Br�utigam</a>
 */
public class LuceneEntityState
        implements EntityState, Serializable {

    static final Log log = LogFactory.getLog( LuceneEntityState.class );

    static final String                     PREFIX_PROP = "";
    static final String                     PREFIX_ASSOC = "";
    static final String                     PREFIX_MANYASSOC = "";
    static final String                     SEPARATOR_PROP = "/";

    private static final Object             NULL = new Object();

    protected LuceneEntityStoreUnitOfWork   uow;

    protected EntityStatus                  status;

    protected String                        version;

    protected long                          lastModified;

    private final EntityReference           identity;

    private final EntityDescriptor          entityDescriptor;

    protected final IRecordState            record;

//    /** Lazily filled cache of the manyAssociations in use. */
//    private Map<QualifiedName,LuceneManyAssociationState> manyAssociations;
//
//    /** Lazily filled cache of the associations in use. */
//    private Map<QualifiedName,EntityReference> associations;
//
//    /** Lazily filled cache of the associations in use. Values might be {@link NULL}. */
//    private Map<QualifiedName,Object>       properties;


    public LuceneEntityState( LuceneEntityStoreUnitOfWork uow, EntityReference identity,
            EntityDescriptor entityDescriptor, IRecordState initialState ) {
        this( uow, identity, EntityStatus.LOADED, entityDescriptor, initialState );
    }


    public LuceneEntityState( LuceneEntityStoreUnitOfWork uow,
            EntityReference identity, EntityStatus status,
            EntityDescriptor entityDescriptor, IRecordState state ) {
        this.uow = uow;
        this.identity = identity;
        this.status = status;
        this.entityDescriptor = entityDescriptor;
        this.record = state;

        version = record.get( "version" );
        version = version != null ? version : uow.identity();

        String lastModifiedValue = record.get( "modified" );
        lastModified = lastModifiedValue != null
                ? Long.parseLong( lastModifiedValue )
                : System.currentTimeMillis();
    }


    // EntityState implementation *************************

    public final String version() {
        return version;
    }


    public long lastModified() {
        return lastModified;
    }


    public EntityReference identity() {
        return identity;
    }


    public Object getProperty( QualifiedName stateName ) {
        PropertyDescriptor propertyDescriptor = entityDescriptor.state()
                .getPropertyByQualifiedName( stateName );

        if (propertyDescriptor == null) {
            throw new RuntimeException( "No PropertyDescriptor for: " + stateName.name() );
        }
        Type targetType = ((PropertyTypeDescriptor)propertyDescriptor).type();
        ValueType propertyType = ((PropertyTypeDescriptor)propertyDescriptor).propertyType().type();
        String fieldName = PREFIX_PROP + stateName.name();

        Object result = loadProperty( fieldName, propertyType );
        return result != NULL ? result : null;
    }


    public Object setProperty( QualifiedName stateName, Object newValue ) {
        PropertyTypeDescriptor propertyDescriptor = entityDescriptor.state()
                .getPropertyByQualifiedName( stateName );

        Type type = ((PersistentPropertyModel)propertyDescriptor).type();

//        propertyDescriptor.accessor().getReturnType()
        ValueType propertyType = propertyDescriptor.propertyType().type();
        String fieldName = PREFIX_PROP + stateName.name();

        Object result = storeProperty( fieldName, propertyType, newValue != NULL ? newValue : null );

        markUpdated();
        
        return result;
    }


    public EntityReference getAssociation( QualifiedName stateName ) {
        String id = record.get( PREFIX_ASSOC + stateName.name() );
        return id != null ? EntityReference.parseEntityReference( id ) : null;
    }


    public void setAssociation( QualifiedName stateName, EntityReference newEntity ) {
        String id = newEntity == null ? null : newEntity.identity();
        String fieldName = PREFIX_ASSOC + stateName.name();

        if (id != null) {
            record.put( fieldName, id );
        }
        else {
            record.remove( fieldName );
        }

        markUpdated();
    }


    public ManyAssociationState getManyAssociation( QualifiedName stateName ) {
        return new LuceneManyAssociationState( this, stateName );
    }

    
//    void setManyAssociation( LuceneManyAssociationState assoc, JSONArray references ) {
//        record.put( assoc.getFieldName(), references.toString() );
//        markUpdated();
//    }
    
    
    public void remove() {
        status = EntityStatus.REMOVED;
        uow.statusChanged( this );
    }


    public EntityStatus status() {
        return status;
    }


    public boolean isOfType( TypeName type ) {
        return entityDescriptor.entityType().type().equals( type );
    }


    public EntityDescriptor entityDescriptor() {
        return entityDescriptor;
    }


    public IRecordState state() {
        return record;
    }


    public String toString() {
        return identity + "[" + record.toString() + "]";
    }


    public void hasBeenApplied() {
        status = EntityStatus.LOADED;
        //version = uow.identity();
        uow.statusChanged( this );
    }


    public void markUpdated() {
        if (status == EntityStatus.LOADED) {
            status = EntityStatus.UPDATED;
        }
        uow.statusChanged( this );
    }


    protected Object loadProperty( final String fieldName, final ValueType propertyType ) {
        // ValueComposite
        if (propertyType instanceof ValueCompositeType) {
            // XXX subTypes are not yet supported
            ValueCompositeType actualValueType = (ValueCompositeType)propertyType;
            List<PropertyType> actualTypes = actualValueType.types();

            //log.debug( "    loadProperty(): ValueComposite: " + actualValueType );
            final Map<QualifiedName, Object> values = new HashMap<QualifiedName, Object>();
            for (PropertyType actualType : actualTypes) {
                Object value = loadProperty( Joiner.on( SEPARATOR_PROP ).join(
                        fieldName, actualType.qualifiedName().name() ),
                        actualType.type() );
                if (value != null) {
                    values.put( actualType.qualifiedName(), value );
                    //log.debug( "        property: " + actualType.qualifiedName() + ", value: " + value );
                }
            }

            try {
                Module module = uow.module();

                ValueBuilder valueBuilder = module.valueBuilderFactory().newValueBuilder(
                        module.classLoader().loadClass( actualValueType.type().name() ) );

                valueBuilder.withState( new StateHolder() {
                    public <T> Property<T> getProperty( Method propertyMethod ) {
                        return null;
                    }
                    public <T> Property<T> getProperty( QualifiedName name ) {
                        return null;
                    }
                    public void visitProperties( StateVisitor visitor ) {
                        for (Map.Entry<QualifiedName, Object> entry : values.entrySet()) {
                            visitor.visitProperty( entry.getKey(), entry.getValue() );
                        }
                    }
                });

                return valueBuilder.newInstance();
            }
            catch (ClassNotFoundException e) {
                throw new IllegalStateException( "Could not deserialize value", e );
            }
        }

        // Collection
        else if (propertyType instanceof CollectionType) {
            return new ValueCollection( this, fieldName, propertyType );
        }

        // primitive type
        try {
            Class<?> targetType = uow.module().classLoader().loadClass( propertyType.type().name() );
            Object result = record.get( fieldName );
            //log.debug( "    loadProperty(): name: " + fieldName + ", value: " + result );
            return result;
        }
        catch (Exception e) {
            log.warn( "Unable to decode property: " + fieldName + ", type=" + propertyType.type(), e );
            return null;
            //            Object defaultValue = DefaultValues.getDefaultValue( module.classLoader()
            //                    .loadClass( propertyType.type().type().name() ) );
            //            values.put( propertyType.qualifiedName(), defaultValue );
        }
    }


    protected Object storeProperty( String fieldName, ValueType propertyType, Object newValue) {
        // ValueComposite
        if (propertyType instanceof ValueCompositeType) {
            if (newValue == null) {
                // XXX what to do for null?
                return newValue;
            }
            ValueComposite valueComposite = (ValueComposite)newValue;
            final Map<QualifiedName, Object> values = new HashMap<QualifiedName, Object>();
    
            valueComposite.state().visitProperties( new StateHolder.StateVisitor() {
                public void visitProperty( QualifiedName name, Object value ) {
                    values.put( name, value );
                }
            } );
    
            List<PropertyType> actualTypes = ((ValueCompositeType)propertyType).types();
            if (!newValue.getClass().getInterfaces()[ 0 ].getName().equals(
                    propertyType.type().name() )) {
                throw new RuntimeException( "Actual value is a subtype - use it instead" );
                //                    ValueModel valueModel = (ValueModel)ValueInstance.getValueInstance( (ValueComposite)newValue )
                //                            .compositeModel();
                //
                //                    actualTypes = valueModel.valueType().types();
                //                    json.key( "_type" ).value( valueModel.valueType().type().name() );
            }
    
            for (PropertyType actualType : actualTypes) {
                storeProperty( Joiner.on( SEPARATOR_PROP ).join( fieldName, actualType.qualifiedName().name() ),
                        actualType.type(),
                        values.get( actualType.qualifiedName() ) );
            }
            return newValue;
        }
    
        // Collection
        else if (propertyType instanceof CollectionType) {
            assert newValue != null : "Setting collection to null is not supported.";
            
            if (newValue instanceof ValueCollection) {
                ValueCollection coll = (ValueCollection)newValue;
                if (coll.state == this && coll.fieldName.equals( fieldName )) {        
                    return newValue;
                }
            }

            ValueCollection coll = new ValueCollection( this, fieldName, propertyType, (Collection)newValue );
            coll.store();
            return coll;
        }
    
        // values
        else {
            if (newValue == null) {
                record.remove( fieldName );
            }
            else {
                record.put( fieldName, newValue );
            }
            return newValue;
        }
    }


    void writeBack( String newVersion ) {
        // entity header info
        EntityType entityType = entityDescriptor.entityType();
        record.put( "type", entityType.type().name() );

        version = newVersion;
        record.put( "version", version );
//        // XXX what is the semantic?
//        if (!newVersion.equals( identity.identity() )) {
//            log.warn( "writeBack(): VERSIONS: " + newVersion + " - " + identity.identity() );
//        }

        lastModified = System.currentTimeMillis();
        record.put( "modified", String.valueOf( lastModified ) );
    }
    
}
