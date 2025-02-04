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
package org.hisp.dhis.webapi.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.analytics.AnalyticsTableGenerator;
import org.hisp.dhis.analytics.AnalyticsTableService;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryManager;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.util.CategoryUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.maintenance.MaintenanceService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = MaintenanceController.RESOURCE_PATH )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class MaintenanceController
{
    public static final String RESOURCE_PATH = "/maintenance";

    @Autowired
    private WebMessageService webMessageService;

    @Autowired
    private MaintenanceService maintenanceService;

    @Autowired
    private RenderService renderService;

    @Autowired
    private ResourceTableService resourceTableService;

    @Autowired
    private AnalyticsTableGenerator analyticsTableGenerator;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private List<AnalyticsTableService> analyticsTableService;

    @Autowired
    private CategoryManager categoryManager;

    @Autowired
    private CategoryUtils categoryUtils;

    @Autowired
    private AppManager appManager;

    @Autowired
    private CategoryService categoryService;

    @RequestMapping( value = "/analyticsTablesClear", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void clearAnalyticsTables()
    {
        analyticsTableService.forEach( AnalyticsTableService::dropTables );
    }

    @RequestMapping( value = "/analyticsTablesAnalyze", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void analyzeAnalyticsTables()
    {
        analyticsTableService.forEach( AnalyticsTableService::analyzeAnalyticsTables );
    }

    @RequestMapping( value = "/expiredInvitationsClear", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void clearExpiredInvitations()
    {
        maintenanceService.removeExpiredInvitations();
    }

    @RequestMapping( value = "/ouPathsUpdate", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void forceUpdatePaths()
    {
        organisationUnitService.forceUpdatePaths();
    }

    @RequestMapping( value = "/periodPruning", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void prunePeriods()
    {
        maintenanceService.prunePeriods();
    }

    @RequestMapping( value = "/zeroDataValueRemoval", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteZeroDataValues()
    {
        maintenanceService.deleteZeroDataValues();
    }

    @RequestMapping( value = "/softDeletedDataValueRemoval", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteSoftDeletedDataValues()
    {
        maintenanceService.deleteSoftDeletedDataValues();
    }

    @RequestMapping( value = "/invalidRelationshipsRemoval", method = { RequestMethod.PUT,
        RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteInvalidRelationships()
    {
        maintenanceService.deleteInvalidRelationships();
    }

    @RequestMapping( value = "/softDeletedProgramStageInstanceRemoval", method = { RequestMethod.PUT,
        RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteSoftDeletedProgramStageInstances()
    {
        maintenanceService.deleteSoftDeletedProgramStageInstances();
    }

    @RequestMapping( value = "/softDeletedProgramInstanceRemoval", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteSoftDeletedProgramInstances()
    {
        maintenanceService.deleteSoftDeletedProgramInstances();
    }

    @RequestMapping( value = "/softDeletedTrackedEntityInstanceRemoval", method = { RequestMethod.PUT,
        RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteSoftDeletedTrackedEntityInstances()
    {
        maintenanceService.deleteSoftDeletedTrackedEntityInstances();
    }

    @RequestMapping( value = "/sqlViewsCreate", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void createSqlViews()
    {
        resourceTableService.createAllSqlViews();
    }

    @RequestMapping( value = "/sqlViewsDrop", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void dropSqlViews()
    {
        resourceTableService.dropAllSqlViews();
    }

    @RequestMapping( value = "/categoryOptionComboUpdate", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void updateCategoryOptionCombos()
    {
        categoryManager.addAndPruneAllOptionCombos();
    }

    @RequestMapping( value = "/categoryOptionComboUpdate/categoryCombo/{uid}", method = { RequestMethod.PUT,
        RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    public void updateCategoryOptionCombos( @PathVariable String uid, HttpServletRequest request,
        HttpServletResponse response )
    {
        CategoryCombo categoryCombo = categoryService.getCategoryCombo( uid );

        if ( categoryCombo == null )
        {
            webMessageService.sendJson( WebMessageUtils.conflict( "CategoryCombo does not exist: " + uid ), response );
            return;
        }

        ImportSummaries importSummaries = categoryUtils.addAndPruneOptionCombos( categoryCombo );

        webMessageService.send( WebMessageUtils.importSummaries( importSummaries ), response, request );
    }

    @RequestMapping( value = { "/cacheClear", "/cache" }, method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void clearCache()
    {
        maintenanceService.clearApplicationCaches();
    }

    @RequestMapping( value = "/dataPruning/organisationUnits/{uid}", method = { RequestMethod.PUT,
        RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void pruneDataByOrganisationUnit( @PathVariable String uid, HttpServletResponse response )
        throws Exception
    {
        OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( uid );

        if ( organisationUnit == null )
        {
            webMessageService
                .sendJson( WebMessageUtils.conflict( "Organisation unit does not exist: " + uid ), response );
            return;
        }

        boolean result = maintenanceService.pruneData( organisationUnit );

        WebMessage message = result ? WebMessageUtils.ok( "Data was pruned successfully" )
            : WebMessageUtils.conflict( "Data could not be pruned" );

        webMessageService.sendJson( message, response );
    }

    @RequestMapping( value = "/dataPruning/dataElements/{uid}", method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void pruneDataByDataElement( @PathVariable String uid, HttpServletResponse response )
        throws Exception
    {
        DataElement dataElement = dataElementService.getDataElement( uid );

        if ( dataElement == null )
        {
            webMessageService
                .sendJson( WebMessageUtils.conflict( "Data element does not exist: " + uid ), response );
            return;
        }

        boolean result = maintenanceService.pruneData( dataElement );

        WebMessage message = result ? WebMessageUtils.ok( "Data was pruned successfully" )
            : WebMessageUtils.conflict( "Data could not be pruned" );

        webMessageService.sendJson( message, response );
    }

    @RequestMapping( value = "/appReload", method = RequestMethod.GET )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    public void appReload( HttpServletResponse response )
        throws IOException
    {
        appManager.reloadApps();
        renderService.toJson( response.getOutputStream(), WebMessageUtils.ok( "Apps reloaded" ) );
    }

    @RequestMapping( method = { RequestMethod.PUT, RequestMethod.POST } )
    @PreAuthorize( "hasRole('ALL') or hasRole('F_PERFORM_MAINTENANCE')" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void performMaintenance(
        @RequestParam( required = false ) boolean analyticsTableClear,
        @RequestParam( required = false ) boolean analyticsTableAnalyze,
        @RequestParam( required = false ) boolean expiredInvitationsClear,
        @RequestParam( required = false ) boolean ouPathsUpdate,
        @RequestParam( required = false ) boolean periodPruning,
        @RequestParam( required = false ) boolean zeroDataValueRemoval,
        @RequestParam( required = false ) boolean softDeletedDataValueRemoval,
        @RequestParam( required = false ) boolean softDeletedEventRemoval,
        @RequestParam( required = false ) boolean softDeletedEnrollmentRemoval,
        @RequestParam( required = false ) boolean softDeletedTrackedEntityInstanceRemoval,
        // TODO: Remove default value when frontend is ready
        @RequestParam( required = false, defaultValue = "true" ) boolean invalidRelationshipsRemoval,
        @RequestParam( required = false ) boolean sqlViewsDrop,
        @RequestParam( required = false ) boolean sqlViewsCreate,
        @RequestParam( required = false ) boolean categoryOptionComboUpdate,
        @RequestParam( required = false ) boolean cacheClear,
        @RequestParam( required = false ) boolean appReload,
        @RequestParam( required = false ) boolean resourceTableUpdate )
    {
        if ( analyticsTableClear )
        {
            clearAnalyticsTables();
        }

        if ( analyticsTableAnalyze )
        {
            analyzeAnalyticsTables();
        }

        if ( expiredInvitationsClear )
        {
            clearExpiredInvitations();
        }

        if ( ouPathsUpdate )
        {
            forceUpdatePaths();
        }

        if ( periodPruning )
        {
            prunePeriods();
        }

        if ( zeroDataValueRemoval )
        {
            deleteZeroDataValues();
        }

        if ( softDeletedDataValueRemoval )
        {
            deleteSoftDeletedDataValues();
        }

        if ( softDeletedEventRemoval )
        {
            deleteSoftDeletedProgramStageInstances();
        }

        if ( softDeletedEnrollmentRemoval )
        {
            deleteSoftDeletedProgramInstances();
        }

        if ( softDeletedTrackedEntityInstanceRemoval )
        {
            deleteSoftDeletedTrackedEntityInstances();
        }

        if ( invalidRelationshipsRemoval )
        {
            deleteInvalidRelationships();
        }

        if ( sqlViewsDrop )
        {
            dropSqlViews();
        }

        if ( sqlViewsCreate )
        {
            createSqlViews();
        }

        if ( categoryOptionComboUpdate )
        {
            updateCategoryOptionCombos();
        }

        if ( cacheClear )
        {
            clearCache();
        }

        if ( appReload )
        {
            appManager.reloadApps();
        }

        if ( resourceTableUpdate )
        {
            analyticsTableGenerator.generateResourceTables( null );
        }
    }
}
