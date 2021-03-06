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

import static org.eclipse.microprofile.graphql.client.core.utils.ServiceUtils.getNewInstanceOf;
import static java.util.Arrays.asList;

public interface InputObject extends Buildable {
    /*
        Static factory methods
    */
    static InputObject inputObject(InputObjectField... inputObjectFields) {
        InputObject inputObject = getNewInstanceOf(InputObject.class);

        inputObject.setInputObjectFields(asList(inputObjectFields));

        return inputObject;
    }

    /*
        Getter/Setter
     */
    List<InputObjectField> getInputObjectFields();
    void setInputObjectFields(List<InputObjectField> inputObjectFields);
}
