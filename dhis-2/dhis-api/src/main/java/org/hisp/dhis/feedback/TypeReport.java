/*
 * Copyright (c) 2004-2021, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.feedback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hisp.dhis.common.DxfNamespaces;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "typeReport", namespace = DxfNamespaces.DXF_2_0 )
public class TypeReport
{
    private static final Map<Class<?>, TypeReport> EMPTY_BY_TYPE = new ConcurrentHashMap<>();

    private Stats stats = new Stats();

    private final Class<?> klass;

    private final boolean empty;

    private Map<Integer, ObjectReport> objectReportMap = new HashMap<>();

    public static TypeReport empty( Class<?> klass )
    {
        return EMPTY_BY_TYPE.computeIfAbsent( klass, type -> new TypeReport( type, true ) );
    }

    @JsonCreator
    public TypeReport( @JsonProperty( "klass" ) Class<?> klass )
    {
        this( klass, false );
    }

    private TypeReport( Class<?> klass, boolean empty )
    {
        this.klass = klass;
        this.empty = empty;
    }

    public final boolean isEmptySingleton()
    {
        return empty;
    }

    // -----------------------------------------------------------------------------------
    // Utility Methods
    // -----------------------------------------------------------------------------------

    public TypeReport mergeAllowEmpty( TypeReport other )
    {
        if ( isEmptySingleton() )
        {
            return other;
        }
        if ( other.isEmptySingleton() )
        {
            return this;
        }
        merge( other );
        return this;
    }

    public void merge( TypeReport typeReport )
    {
        stats.merge( typeReport.getStats() );

        typeReport.getObjectReportMap().forEach( ( index, objectReport ) -> {
            if ( !objectReportMap.containsKey( index ) )
            {
                objectReportMap.put( index, objectReport );
                return;
            }

            objectReportMap.get( index ).addErrorReports( objectReport.getErrorReports() );
        } );
    }

    public void addObjectReport( ObjectReport objectReport )
    {
        if ( !objectReportMap.containsKey( objectReport.getIndex() ) )
        {
            objectReportMap.put( objectReport.getIndex(), objectReport );
            return;
        }

        objectReportMap.get( objectReport.getIndex() ).merge( objectReport );
    }

    // -----------------------------------------------------------------------------------
    // Getters and Setters
    // -----------------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public Class<?> getKlass()
    {
        return klass;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Stats getStats()
    {
        return stats;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "objectReports", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "objectReport", namespace = DxfNamespaces.DXF_2_0 )
    public List<ObjectReport> getObjectReports()
    {
        return new ArrayList<>( objectReportMap.values() );
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "objectReports", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "objectReport", namespace = DxfNamespaces.DXF_2_0 )
    public void setObjectReports( List<ObjectReport> objectReports )
    {
        if ( objectReports != null )
        {
            objectReports.forEach( or -> objectReportMap.put( or.getIndex(), or ) );
        }
    }

    public List<ErrorReport> getErrorReports()
    {
        List<ErrorReport> errorReports = new ArrayList<>();
        objectReportMap.values().forEach( objectReport -> errorReports.addAll( objectReport.getErrorReports() ) );

        return errorReports;
    }

    public Map<Integer, ObjectReport> getObjectReportMap()
    {
        return objectReportMap;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "klass", klass )
            .add( "stats", stats )
            .add( "objectReports", getObjectReports() )
            .toString();
    }
}
