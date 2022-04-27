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


import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.NonNull;

public class ParentType extends GrandParentType {

    @Description("Field from parent")
    private String parentTypeField;

    @NonNull
    @Name("nonNullParentField")
    private String nonNullParentTypeField;

    public String getParentTypeField() {
        return parentTypeField;
    }

    public void setParentTypeField(final String parentTypeField) {
        this.parentTypeField = parentTypeField;
    }

    public String getNonNullParentTypeField() {
        return nonNullParentTypeField;
    }

    public void setNonNullParentTypeField(final String nonNullParentTypeField) {
        this.nonNullParentTypeField = nonNullParentTypeField;
    }
}
