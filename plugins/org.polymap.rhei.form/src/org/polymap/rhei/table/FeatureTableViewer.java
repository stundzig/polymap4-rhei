/*
 * polymap.org
 * Copyright (C) 2011-2015, Falko Br�utigam, and other contributors as
 * indicated by the @authors tag. All rights reserved.
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
package org.polymap.rhei.table;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.opengis.filter.Filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;

import org.polymap.core.runtime.ListenerList;
import org.polymap.core.ui.SelectionAdapter;
import org.polymap.core.ui.UIUtils;

/**
 *
 *
 * @author <a href="http://www.polymap.de">Falko Br�utigam</a>
 */
public class FeatureTableViewer
        extends TableViewer {

    private static Log log = LogFactory.getLog( FeatureTableViewer.class );

    /** Property type that is fired when the content of the viewer has changed. */
    public static final String              PROP_CONTENT_SIZE = "contentsize";

    public static final Object              LOADING_ELEMENT = new Object();

    public static final Color               LOADING_FOREGROUND = UIUtils.getColor( 0xa0, 0xa0, 0xa0 );
    public static final Color               LOADING_BACKGROUND = UIUtils.getColor( 0xfa, 0xfb, 0xff );
    
    protected Map<String,IFeatureTableColumn> displayed = new HashMap();

    protected Map<String,IFeatureTableColumn> available = new HashMap();

    private Map<String,CellEditor>          editors = new HashMap();
    
    private ListenerList<PropertyChangeListener> listeners = new ListenerList();

    private Color                           foreground, background;

    private Composite                       layoutParent;

    private TableColumnLayout               tableColumnLayout;


    public FeatureTableViewer( Composite parent, int style ) {
        super( parent, style | SWT.VIRTUAL );

        ColumnViewerToolTipSupport.enableFor( this );
        getTable().setLinesVisible( true );
        getTable().setHeaderVisible( true );
        getTable().setLayout( new TableLayout() );

        this.foreground = getTable().getForeground();
        this.background = getTable().getBackground();

        //setUseHashlookup( true );
        
        parent.addControlListener( new ControlAdapter() {
            @Override
            public void controlResized( ControlEvent ev ) {
                FeatureTableViewer.this.controlResized( ev );
            }
        });
        
//        tableColumnLayout = new TableColumnLayout();
//        parent.setLayout( tableColumnLayout );
    }


    private int         cachedWidth = -1;
    
    protected void controlResized( ControlEvent ev ) {
        if (!getTable().isDisposed() && !displayed.isEmpty()) {
            Rectangle area = getTable().getParent().getClientArea();
            
            if (area.width != cachedWidth) {
                cachedWidth = area.width;                
                int avgWidth = area.width / displayed.size();

                int sumWeight = displayed.values().stream().mapToInt( c -> c.getWeight() ).sum();

                for (IFeatureTableColumn column : displayed.values()) {
                    TableViewerColumn viewerColumn = column.getViewerColumn();

                    if (column.getWeight() > 0) {
                        int width = ((area.width / sumWeight) * column.getWeight()) - 5;
                        viewerColumn.getColumn().setWidth( width );
                    }
                    else {
                        viewerColumn.getColumn().setWidth( avgWidth );
                    }
                }
            }
        }
    }


    public void dispose() {
        listeners.clear();
        listeners = null;
        displayed.clear();
        available.clear();
        editors.clear();
    }

    
    /**
     * Suppress selection of rows altogether. This is useful for tables with editing enabled.
     */
    public void suppressSelection() {
        Table table = getTable();
        table.addListener( SWT.Selection, new Listener() {
            @Override
            public void handleEvent( Event ev ) {
                ev.detail = SWT.NONE;
                ev.type = SWT.None;
                ev.doit = false;
                try {
                    table.setRedraw( false );
                    table.deselectAll();
                }
                finally {
                    table.setRedraw( true );
                }
            }
        });    
    }

    
    @Override
    public void addFilter( ViewerFilter filter ) {
        assert getContentProvider() != null;
        super.addFilter( filter );

        if (getContentProvider() instanceof DeferredFeatureContentProvider2) {
            DeferredFeatureContentProvider2 provider = (DeferredFeatureContentProvider2)getContentProvider();
            provider.addViewerFilter( filter );
        }
    }


    @Override
    public void removeFilter( ViewerFilter filter ) {
        super.removeFilter( filter );
        if (getContentProvider() instanceof DeferredFeatureContentProvider2) {
            DeferredFeatureContentProvider2 provider = (DeferredFeatureContentProvider2)getContentProvider();
            provider.removeViewerFilter( filter );
        }
    }

    
    /**
     *
     */
    @Override
    public void refresh() {
        super.refresh();
        if (getContentProvider() instanceof DeferredFeatureContentProvider2) {
            DeferredFeatureContentProvider2 provider = (DeferredFeatureContentProvider2)getContentProvider();
            provider.inputChanged( this, null, null );
            provider.setSortOrder( provider.getSortOrder() );
        }        
    }
    
    
    public int getElementCount() {
        return doGetItemCount();
    }
    

    public IFeatureTableElement[] getElements() {
        IFeatureTableElement[] result = new IFeatureTableElement[ getElementCount() ];
        for (int i=0; i<getElementCount(); i++) {
            result[i] = (IFeatureTableElement)getElementAt( i );
        }
        return result;
    }

    
    public IFeatureTableElement[] getSelectedElements() {
        return SelectionAdapter.on( getSelection() ).asArray( IFeatureTableElement.class ); 
    }

    
    public void clearSelection() {
        getTable().deselectAll();    
    }
    
    
    /**
     * Selects the element with the given feature id.
     * 
     * @param fid
     * @param reveal
     */
    public void selectElement( final String fid, boolean reveal, boolean fireEvent ) {
        assert fid != null;
        IFeatureTableElement search = new DefaultFeatureTableElement() {
            @Override
            public Object getValue( String name ) { 
                throw new RuntimeException( "not yet implemented." ); 
            }
            @Override
            public void setValue( String name, Object value ) { 
                throw new RuntimeException( "not yet implemented." ); 
            }
            @Override
            public String fid() { 
                return fid; 
            }            
        };
        
        int index = -1;
        if (getContentProvider() instanceof IDeferredFeatureContentProvider) {
            // find index from content provider
            index = ((IDeferredFeatureContentProvider)getContentProvider()).findElement( search );
            // select table
            getTable().setSelection( index );
            log.debug( "getTable().getSelectionIndex(): " + getTable().getSelectionIndex() );
            if (reveal) {
                getTable().showSelection();
            }
            // fire event
            if (fireEvent) {
                ISelection sel = getSelection();
                log.debug( "getSelection(): " + sel );
                updateSelection( sel );
            }
        }
        else {
            ISelection sel = new StructuredSelection( search );
            setSelection( sel, reveal );
        }
        getTable().layout();
    }


    public IFeatureTableColumn addColumn( IFeatureTableColumn column ) {
        column.setViewer( this );
        TableViewerColumn viewerColumn = column.newViewerColumn();
        viewerColumn.getColumn().setData( "name", column.getName() );

//        tableColumnLayout.setColumnData( viewerColumn.getColumn(), 
//                new ColumnPixelData( 120, true ) );
        
        available.put( column.getName(), column );
        displayed.put( column.getName(), column );
        return column;
    }

    
    public <T extends IFeatureTableColumn> T getColumn( String propName ) {
        return (T)available.get( propName );
    }

    
    /**
     * Set the content of this viewer. A {@link DeferredFeatureContentProvider} is
     * used to fetch and sort the features.
     * 
     * @param fs
     * @param filter
     */
    public void setContent( final FeatureSource fs, final Filter filter ) {
        TableColumn sortColumn = getTable().getSortColumn();
        int sortDir = SWT.DOWN;
        if (sortColumn == null) {
            sortColumn = getTable().getColumn( 0 );
            getTable().setSortColumn( sortColumn );
            getTable().setSortDirection( sortDir );
        }
        else {
            sortDir = getTable().getSortDirection();
        }
        
        String colName = (String)sortColumn.getData( "name" );
        IFeatureTableColumn sortTableColumn = displayed.get( colName );

        setContentProvider( new DeferredFeatureContentProvider2( 
                this, fs, filter, sortTableColumn.newComparator( sortDir ), getFilters() ) );
        setInput( fs );
    }


    public void setContent( FeatureCollection coll ) {
        setContentProvider( new FeatureCollectionContentProvider( coll ) );
        setInput( coll );
    }


    public void setContent( IFeatureContentProvider provider ) {
        setContentProvider( provider );
    }


    public boolean addPropertyChangeListener( PropertyChangeListener listener ) {
        return listeners.add( listener );
    }


    public boolean removePropertyChangeListener( PropertyChangeListener listener ) {
        return listeners.remove( listener );
    }


    protected void firePropChange( final String name, Object oldValue, final Object newValue ) {
        final PropertyChangeEvent ev = new PropertyChangeEvent( this, name, oldValue, newValue );

        Display display = getTable().getDisplay();
        display.asyncExec( new Runnable() {
            public void run() {
                if (getTable().isDisposed()) {
                    return;
                }
//                if (PROP_CONTENT_SIZE.equals( name ) ) {
//                    getTable().setForeground( Graphics.getColor( 0x70, 0x70, 0x80 ) );
//                }
                for (PropertyChangeListener l : listeners.getListeners()) {
                    l.propertyChange( ev );
                }
            }
        });
    }


    /**
     * Sorts the table entries by delegating the call to the content provider.
     * 
     * @param comparator
     * @param dir
     * @param column
     */
    public void sortContent( final Comparator<IFeatureTableElement> comparator, int dir, TableColumn column ) {
        IContentProvider contentProvider = getContentProvider();
        // deferred
        if (contentProvider instanceof IDeferredFeatureContentProvider) {
            ((IDeferredFeatureContentProvider)contentProvider).setSortOrder( comparator );
        }
        // normal
        else {
            setComparator( new ViewerComparator( comparator ) {
                public int compare( Viewer viewer, Object e1, Object e2 ) {
                    return comparator.compare( (IFeatureTableElement)e1, (IFeatureTableElement)e2 );
                }
            });
        }
        getTable().setSortColumn( column );
        getTable().setSortDirection( dir );
    }

    
    protected void markTableLoading( final boolean loading ) {
        Display display = getTable().getDisplay();
        display.asyncExec( new Runnable() {
            public void run() {
                if (getTable().isDisposed()) {
                    return;
                }
                if (loading) {
                    getTable().setForeground( LOADING_FOREGROUND );
                    getTable().setBackground( LOADING_BACKGROUND );
//                    setBusy( true );
                }
                else {
                    getTable().setForeground( foreground );
                    getTable().setBackground( background );
//                    setBusy( false );
                }
            }
        });
    }

}
