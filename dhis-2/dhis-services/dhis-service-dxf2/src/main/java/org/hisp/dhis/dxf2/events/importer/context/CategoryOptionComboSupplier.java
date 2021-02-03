package org.hisp.dhis.dxf2.events.importer.context;

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

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.program.Program;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component( "workContextCategoryOptionCombosSupplier" )
public class CategoryOptionComboSupplier extends AbstractSupplier<Map<String, CategoryOptionCombo>>
{
    private final AttributeOptionComboLoader attributeOptionComboLoader;

    private final ProgramSupplier programSupplier;

    public CategoryOptionComboSupplier( NamedParameterJdbcTemplate jdbcTemplate, ProgramSupplier programSupplier,
        AttributeOptionComboLoader attributeOptionComboLoader )
    {
        super( jdbcTemplate );
        this.attributeOptionComboLoader = attributeOptionComboLoader;
        this.programSupplier = programSupplier;
    }

    @Override
    public Map<String, CategoryOptionCombo> get( ImportOptions importOptions, List<Event> events )
    {
        if ( events == null )
        {
            return emptyMap();
        }

        // TODO this should be optimized to execute less SQL queries
        IdScheme idScheme = importOptions.getIdSchemes().getCategoryOptionIdScheme();
        Map<String, CategoryOptionCombo> eventToCocMap = new HashMap<>();
        Map<String, Program> programMap = programSupplier.get( importOptions, events );

        for ( Event event : events )
        {
            Program program = programMap.get( event.getProgram() );

            // Can't proceed with null Program, this will fail during the validation stage
            if ( program == null )
            {
                return emptyMap();
            }

            final CategoryCombo programCatCombo = program.getCategoryCombo();
            final String aoc = event.getAttributeOptionCombo();
            final String attributeCatOptions = event.getAttributeCategoryOptions();

            CategoryOptionCombo categoryOptionCombo;

            if ( isNotEmpty( aoc ) )
            {
                categoryOptionCombo = attributeOptionComboLoader.getCategoryOptionCombo( idScheme, aoc );
                if ( categoryOptionCombo == null )
                {
                    categoryOptionCombo = attributeOptionComboLoader.getDefault();
                }
            }
            else if ( programCatCombo != null && isNotEmpty( attributeCatOptions ) )
            {
                categoryOptionCombo = attributeOptionComboLoader.getAttributeOptionCombo( programCatCombo,
                    attributeCatOptions, aoc, idScheme );
            }
            else
            {
                categoryOptionCombo = attributeOptionComboLoader.getDefault();
            }

            if ( categoryOptionCombo != null )
            {
                eventToCocMap.put( event.getUid(), categoryOptionCombo );
            }
        }

        return eventToCocMap;
    }

}
