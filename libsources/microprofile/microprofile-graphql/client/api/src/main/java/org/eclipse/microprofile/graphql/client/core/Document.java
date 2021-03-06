/*
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.microprofile.graphql.client.core;

import java.util.List;

import static java.util.Arrays.asList;
import static org.eclipse.microprofile.graphql.client.core.utils.ServiceUtils.getNewInstanceOf;

public interface Document extends Buildable {

    /*
        Static factory methods
    */
    static Document document(FragmentOrOperation... operations) {
        Document document = getNewInstanceOf(Document.class);

        document.setOperations(asList(operations));

        return document;
    }

    /*
        Getter/Setter
    */
    List<Operation> getOperations();

    void setOperations(List<FragmentOrOperation> operations);
}
