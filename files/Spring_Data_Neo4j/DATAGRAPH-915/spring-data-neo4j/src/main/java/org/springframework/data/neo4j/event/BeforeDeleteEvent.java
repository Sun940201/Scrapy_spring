/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.event;

import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;

/**
 * {@link Neo4jDataManipulationEvent} published before a particular entity is deleted.
 *
 * @author Adam George
 * @deprecated Now automatically handled in {@link Neo4jTransactionManager}.
 */
@Deprecated
public class BeforeDeleteEvent extends Neo4jDataManipulationEvent {

    private static final long serialVersionUID = 1238219872542331942L;

    public BeforeDeleteEvent(Object source, Object entity) {
        super(source, entity);
    }

}