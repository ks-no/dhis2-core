package org.hisp.dhis.artemis.audit;

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

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * @author Luciano Fiandesio
 */
@Value
@AllArgsConstructor
public class AuditableEntity
{
    /**
     * Class of the AuditableEntity It will be used by
     * {@link org.hisp.dhis.artemis.audit.legacy.AuditObjectFactory#collectAuditAttributes(Object)}
     */
    Class entityClass;

    /**
     * An object that is ready for serialized by Jackson. Means that this object
     * should: 1. Only includes referenced properties that are owned by the current
     * Audit Entity. Means that the property's schema has attribute "owner = true"
     * 2. Do not include any lazy HibernateProxy or PersistentCollection that is not
     * loaded. 3. All referenced properties that extend BaseIdentifiableObject
     * should be mapped to only UID string This object could be a Map<String,
     * Object> with key is property name and value is the property value
     */
    Object entity;
}
