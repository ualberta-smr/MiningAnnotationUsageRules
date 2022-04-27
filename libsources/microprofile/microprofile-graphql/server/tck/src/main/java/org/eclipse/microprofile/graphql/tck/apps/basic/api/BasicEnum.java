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

/**
 * To Test the generation of a Enum even if it's not used (directly) as a return type or argument.
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
@org.eclipse.microprofile.graphql.Enum("CountDown")
public enum BasicEnum {
    THREE,TWO,ONE
}