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
package org.eclipse.microprofile.graphql.tck.apps.basic.api;

import org.eclipse.microprofile.graphql.Input;

/**
 * To Test the generation of a Input even if it's not used (directly) as an argument.
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
@Input("BasicMessageInput")
public class BasicInput implements BasicInterface {
    private String message;
    private BasicEnum countdownPlace;

    public BasicInput() {
    }
    
    public BasicInput(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public BasicEnum getCountdownPlace() {
        return countdownPlace;
    }

    public void setCountdownPlace(BasicEnum countdownPlace) {
        this.countdownPlace = countdownPlace;
    }
}