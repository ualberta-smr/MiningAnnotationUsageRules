/*
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
 *
 * See the NOTICES file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.eclipse.microprofile.health.tck;

import static org.eclipse.microprofile.health.tck.DeploymentUtils.createWarFileWithClasses;

import org.eclipse.microprofile.health.tck.deployment.FailedStartup;
import org.eclipse.microprofile.health.tck.deployment.SuccessfulLiveness;
import org.eclipse.microprofile.health.tck.deployment.SuccessfulReadiness;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.shrinkwrap.api.Archive;
import org.testng.Assert;
import org.testng.annotations.Test;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

/**
 * @author Martin Stefanko
 */
public class MultipleStartupFailedTest extends TCKBase {

    @Deployment
    public static Archive getDeployment() {
        return createWarFileWithClasses(MultipleStartupFailedTest.class.getSimpleName(),
                SuccessfulLiveness.class, SuccessfulReadiness.class, FailedStartup.class);
    }

    /**
     * Test that Startup is down
     */
    @Test
    @RunAsClient
    public void testFailingStartupResponsePayload() {
        Response response = getUrlStartedContents();

        // status code
        Assert.assertEquals(response.getStatus(), 503);

        JsonObject json = readJson(response);

        // response size
        JsonArray checks = json.getJsonArray("checks");
        Assert.assertEquals(checks.size(), 1, "Expected a single check response");

        // single procedure response
        assertFailureCheck(checks.getJsonObject(0), "failed-check");

        assertOverallFailure(json);
    }

    /**
     * Test that Liveness is up
     */
    @Test
    @RunAsClient
    public void testSuccessfulLivenessResponsePayload() {
        Response response = getUrlLiveContents();

        // status code
        Assert.assertEquals(response.getStatus(), 200);

        JsonObject json = readJson(response);

        // response size
        JsonArray checks = json.getJsonArray("checks");
        Assert.assertEquals(checks.size(), 1, "Expected a single check response");

        // single procedure response
        assertSuccessfulCheck(checks.getJsonObject(0), "successful-check");

        assertOverallSuccess(json);
    }

    /**
     * Test that Readiness is up
     */
    @Test
    @RunAsClient
    public void testSuccessfulReadinessResponsePayload() {
        Response response = getUrlReadyContents();

        // status code
        Assert.assertEquals(response.getStatus(), 200);

        JsonObject json = readJson(response);

        // response size
        JsonArray checks = json.getJsonArray("checks");
        Assert.assertEquals(checks.size(), 1, "Expected a single check response");

        // single procedure response
        assertSuccessfulCheck(checks.getJsonObject(0), "successful-check");

        assertOverallSuccess(json);
    }

    /**
     * Test that Health is down
     */
    @Test
    @RunAsClient
    public void testFailingHealthResponsePayload() {
        Response response = getUrlHealthContents();

        // status code
        Assert.assertEquals(response.getStatus(), 503);

        JsonObject json = readJson(response);

        // response size
        JsonArray checks = json.getJsonArray("checks");
        Assert.assertEquals(checks.size(), 3, "Expected three check responses");

        for (JsonObject check : checks.getValuesAs(JsonObject.class)) {
            String id = check.getString("name");
            switch (id) {
                case "successful-check" :
                    verifySuccessStatus(check);
                    break;
                case "failed-check" :
                    verifyFailureStatus(check);
                    break;
                default :
                    Assert.fail("Unexpected response payload structure");
            }
        }

        assertOverallFailure(json);
    }
}
