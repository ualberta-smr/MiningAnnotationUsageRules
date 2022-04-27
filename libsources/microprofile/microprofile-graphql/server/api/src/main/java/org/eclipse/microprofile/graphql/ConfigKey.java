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

package org.eclipse.microprofile.graphql;

/**
 * Holding all available config keys
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
public interface ConfigKey {
    public static final String DEFAULT_ERROR_MESSAGE = "mp.graphql.defaultErrorMessage";

    @Deprecated
    public static final String EXCEPTION_BLACK_LIST = "mp.graphql.exceptionsBlackList";
    public static final String EXCEPTION_HIDE_ERROR_MESSAGE_LIST = "mp.graphql.hideErrorMessage";

    @Deprecated
    public static final String EXCEPTION_WHITE_LIST = "mp.graphql.exceptionsWhiteList";
    public static final String EXCEPTION_SHOW_ERROR_MESSAGE_LIST = "mp.graphql.showErrorMessage";
}