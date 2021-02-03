package org.hisp.dhis.tracker.programrule.implementers;

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

import static org.hisp.dhis.tracker.validation.hooks.ValidationUtils.needsToValidateDataValues;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.rules.models.AttributeType;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionAttribute;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.programrule.EnrollmentActionRule;
import org.hisp.dhis.tracker.programrule.EventActionRule;
import org.hisp.dhis.tracker.programrule.ProgramRuleIssue;

import com.google.common.collect.Maps;

abstract public class AbstractRuleActionImplementer<T extends RuleAction>
{
    /**
     * @return the class of the action that the implementer work with
     */
    abstract Class<T> getActionClass();

    /**
     * Get the field from the action
     *
     * @param ruleAction to get the field from
     * @return the field of the action
     */
    abstract String getField( T ruleAction );

    /**
     * Apply rule actions to events in the bundle
     *
     * @param eventActionRules Actions to be applied to the bundle
     * @param bundle where to get the events from
     * @return A list of program rule issues that can be either warnings or errors
     */
    abstract List<ProgramRuleIssue> applyToEvents( Map.Entry<String, List<EventActionRule>> eventActionRules,
        TrackerBundle bundle );

    /**
     * Apply rule actions to enrollments in the bundle
     *
     * @param enrollmentActionRules Actions to be applied to the bundle
     * @param bundle where to get the enrollments from
     * @return A list of program rule issues that can be either warnings or errors
     */
    abstract List<ProgramRuleIssue> applyToEnrollments(
        Map.Entry<String, List<EnrollmentActionRule>> enrollmentActionRules, TrackerBundle bundle );

    /**
     * Get the content from the action.
     *
     * @param ruleAction to get the content from
     * @return the content of the action
     */
    protected String getContent( T ruleAction )
    {
        return null;
    }

    public Map<String, List<ProgramRuleIssue>> validateEvents( TrackerBundle bundle )
    {
        Map<String, List<EventActionRule>> eventEffects = getEventEffects( bundle.getEventRuleEffects(), bundle );

        return eventEffects
            .entrySet()
            .stream()
            .map( entry -> Maps.immutableEntry( entry.getKey(), applyToEvents( entry, bundle ) ) )
            .filter( entry -> !entry.getValue().isEmpty() )
            .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );
    }

    public Map<String, List<ProgramRuleIssue>> validateEnrollments( TrackerBundle bundle )
    {
        Map<String, List<EnrollmentActionRule>> enrollmentEffects = getEnrollmentEffects(
            bundle.getEnrollmentRuleEffects(), bundle );

        return enrollmentEffects
            .entrySet()
            .stream()
            .map( entry -> Maps.immutableEntry( entry.getKey(), applyToEnrollments( entry, bundle ) ) )
            .filter( entry -> !entry.getValue().isEmpty() )
            .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );
    }

    /**
     * Filter the actions by - the action class of the implementer - events linked
     * to data values that are part of a different Program Stage - events linked to
     * data values that do not need to be validated
     *
     * @param effects a map of event and effects
     * @param bundle
     * @return A map of actions by event
     */
    public Map<String, List<EventActionRule>> getEventEffects(
        Map<String, List<RuleEffect>> effects, TrackerBundle bundle )
    {
        return effects
            .entrySet()
            .stream()
            .filter( entry -> getEvent( bundle, entry.getKey() ).isPresent() )
            .collect( Collectors.toMap( Map.Entry::getKey,
                e -> {
                    Event event = getEvent( bundle, e.getKey() ).get();
                    ProgramStage programStage = bundle.getPreheat().get( ProgramStage.class, event.getProgramStage() );

                    List<EventActionRule> eventActionRules = e.getValue()
                        .stream()
                        .filter( effect -> getActionClass().isAssignableFrom( effect.ruleAction().getClass() ) )
                        .map( effect -> new EventActionRule( event, effect.data(),
                            getField( (T) effect.ruleAction() ), getAttributeType( effect.ruleAction() ),
                            getContent( (T) effect.ruleAction() ) ) )
                        .filter( effect -> effect.getAttributeType() != AttributeType.DATA_ELEMENT ||
                            isDataElementPartOfProgramStage( effect.getField(), programStage ) )
                        .filter(
                            effect -> effect.getAttributeType() != AttributeType.DATA_ELEMENT ||
                                needsToValidateDataValues( event, programStage ) )
                        .collect( Collectors.toList() );
                    return eventActionRules;
                } ) );
    }

    /**
     * Filter the actions by the action class of the implementer
     *
     * @param effects a map of enrollments and effects
     * @param bundle
     * @return A map of actions by enrollment
     */
    public Map<String, List<EnrollmentActionRule>> getEnrollmentEffects(
        Map<String, List<RuleEffect>> effects, TrackerBundle bundle )
    {
        return effects
            .entrySet()
            .stream()
            .filter( entry -> getEnrollment( bundle, entry.getKey() ).isPresent() )
            .collect( Collectors.toMap( Map.Entry::getKey,
                e -> e.getValue()
                    .stream()
                    .filter( effect -> getActionClass().isAssignableFrom( effect.ruleAction().getClass() ) )
                    .map( effect -> new EnrollmentActionRule(
                        getEnrollment( bundle, e.getKey() ).get(), effect.data(),
                        getField( (T) effect.ruleAction() ), getAttributeType( effect.ruleAction() ),
                        getContent( (T) effect.ruleAction() ) ) )
                    .collect( Collectors.toList() ) ) );
    }

    protected Optional<TrackedEntity> getTrackedEntityFromEnrollment( TrackerBundle bundle, String enrollmentUid )
    {
        return bundle.getEnrollments()
            .stream()
            .filter( e -> e.getEnrollment().equals( enrollmentUid ) )
            .map( Enrollment::getTrackedEntity )
            .findAny()
            .flatMap( tei -> getTrackedEntity( bundle, tei ) );
    }

    private boolean isDataElementPartOfProgramStage( String dataElementUid, ProgramStage programStage )
    {
        return programStage.getDataElements()
            .stream()
            .map( de -> de.getUid() )
            .anyMatch( de -> de.equals( dataElementUid ) );
    }

    private AttributeType getAttributeType( RuleAction ruleAction )
    {
        if ( ruleAction instanceof RuleActionAttribute )
        {
            return ((RuleActionAttribute) ruleAction).attributeType();
        }

        return AttributeType.UNKNOWN;
    }

    protected Optional<Event> getEvent( TrackerBundle bundle, String eventUid )
    {
        return bundle.getEvents()
            .stream()
            .filter( e -> e.getEvent().equals( eventUid ) )
            .findAny();
    }

    private Optional<Enrollment> getEnrollment( TrackerBundle bundle, String enrollmentUid )
    {
        return bundle.getEnrollments()
            .stream()
            .filter( e -> e.getEnrollment().equals( enrollmentUid ) )
            .findAny();
    }

    private Optional<TrackedEntity> getTrackedEntity( TrackerBundle bundle, String teiUid )
    {
        return bundle.getTrackedEntities()
            .stream()
            .filter( e -> e.getTrackedEntity().equals( teiUid ) )
            .findAny();
    }
}
