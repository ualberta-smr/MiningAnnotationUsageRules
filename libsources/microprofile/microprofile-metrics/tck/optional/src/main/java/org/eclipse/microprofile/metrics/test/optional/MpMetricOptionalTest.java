/*
 **********************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICES file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 **********************************************************************/

package org.eclipse.microprofile.metrics.test.optional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import org.hamcrest.Matcher;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.RestAssured;
import io.restassured.builder.ResponseBuilder;
import io.restassured.http.Header;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

@SuppressWarnings("CdiInjectionPointsInspection")
@RunWith(Arquillian.class)
public class MpMetricOptionalTest {

    private static final String JSON_APP_LABEL_REGEX = ";_app=[-/A-Za-z0-9]+([;\\\"]?)";
    private static final String JSON_APP_LABEL_REGEXS_SUB = "$1";

    private static final String OPENMETRICS_APP_LABEL_REGEX = "_app=\"[-/A-Za-z0-9]+\"";

    private static final String APPLICATION_JSON = "application/json";

    // context root under which the application JAX-RS resources are expected to be
    private static String contextRoot;

    private static int applicationPort;

    private static final String TEXT_PLAIN = "text/plain";

    private static final String STRING_PARAM = "_java.lang.String";
    private static final String INT_PARAM = "_int";
    private static final String INTW_PARAM = "_java.lang.Integer";
    private static final String DOUBLEW_PARAM = "_java.lang.Double";
    private static final String LONGW_PARAM = "_java.lang.Long";
    private static final String BOOLEANW_PARAM = "_java.lang.Boolean";
    private static final String DOUBLE_PARAM = "_double";
    private static final String LONG_PARAM = "_long";
    private static final String BOOLEAN_PARAM = "_boolean";
    private static final String HTTP_HEADERS_PARAM = "_jakarta.ws.rs.core.HttpHeaders";
    private static final String REQUEST_PARAM = "_jakarta.ws.rs.core.Request";
    private static final String URI_INFO_PARAM = "_jakarta.ws.rs.core.UriInfo";
    private static final String RESOURCE_CONTEXT_PARAM = "_jakarta.ws.rs.container.ResourceContext";
    private static final String PROVIDERS_PARAM = "_jakarta.ws.rs.ext.Providers";
    private static final String APPLICATION_PARAM = "_jakarta.ws.rs.core.Application";
    private static final String SECURITY_CONTEXT_PARAM = "_jakarta.ws.rs.core.SecurityContext";
    private static final String CONFIGURATION_PARAM = "_jakarta.ws.rs.core.Configuration";
    private static final String LIST_PARAM = "_java.util.List";
    private static final String SET_PARAM = "_java.util.Set";
    private static final String SORTED_SET_PARAM = "_java.util.SortedSet";
    private static final String OBJECT_PARAM = "_java.lang.Object";
    private static final String NAME_OBJECT_PARAM = "_org.eclipse.microprofile.metrics.test.optional.NameObject";
    private static final String AYNC_RESP_PARAM = "_jakarta.ws.rs.container.AsyncResponse";

    private static final String JSON_BASE_REQUEST_COUNT_START =
            "'REST.request'.'count;class=org.eclipse.microprofile.metrics.test.optional.MetricAppBeanOptional;method=";
    private static final String JSON_BASE_REQUEST_UNMAPPED_EXCEPTION_START =
            "'REST.request.unmappedException.total;class=org.eclipse.microprofile.metrics.test.optional.MetricAppBeanOptional;method=";
    private static final String JSON_BASE_REQUEST_TIME_START =
            "'REST.request'.'elapsedTime;class=org.eclipse.microprofile.metrics.test.optional.MetricAppBeanOptional;method=";
    private static final String JSON_BASE_MAX_TIME_START =
            "'REST.request'.'maxTimeDuration;class=org.eclipse.microprofile.metrics.test.optional.MetricAppBeanOptional;method=";
    private static final String JSON_BASE_MIN_TIME_START =
            "'REST.request'.'minTimeDuration;class=org.eclipse.microprofile.metrics.test.optional.MetricAppBeanOptional;method=";
    private static final String JSON_BASE_REQUEST_END = ";tier=integration'";

    private static final String OM_BASE_REQUEST_COUNT_START = "base_REST_request_total"
            + "{class=\"org.eclipse.microprofile.metrics.test.optional.MetricAppBeanOptional\",method=\"";
    private static final String OM_BASE_REQUEST_TIME_START = "base_REST_request_elapsedTime_seconds"
            + "{class=\"org.eclipse.microprofile.metrics.test.optional.MetricAppBeanOptional\",method=\"";
    private static final String OM_BASE_REQUEST_UNMAPPED_EXCEPTION_START = "base_REST_request_unmappedException_total"
            + "{class=\"org.eclipse.microprofile.metrics.test.optional.MetricAppBeanOptional\",method=\"";
    private static final String OM_BASE_MAX_TIME_START = "base_REST_request_maxTimeDuration_seconds"
            + "{class=\"org.eclipse.microprofile.metrics.test.optional.MetricAppBeanOptional\",method=\"";
    private static final String OM_BASE_MIN_TIME_START = "base_REST_request_minTimeDuration_seconds"
            + "{class=\"org.eclipse.microprofile.metrics.test.optional.MetricAppBeanOptional\",method=\"";
    private static final String OM_BASE_REQUEST_END = "\",tier=\"integration\"}";

    private static final String METRICS_ENDPOINT = "/metrics";
    private static final String BASE_METRIC_ENDPOINT = METRICS_ENDPOINT + "/base";
    private static final String RESTREQUEST_METRIC_ENDPOINT = BASE_METRIC_ENDPOINT + "/REST.request";
    private static final String RESTREQUEST_UNMAPPED_EXCEPION_METRIC_ENDPOINT =
            BASE_METRIC_ENDPOINT + "/REST.request.unmappedException.total";

    @ArquillianResource
    private URL deploymentURL;

    private static final String DEFAULT_PROTOCOL = "http";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8080;

    private static String filterOutAppLabelJSON(String responseBody) {
        return responseBody.replaceAll(JSON_APP_LABEL_REGEX, JSON_APP_LABEL_REGEXS_SUB);
    }

    private static String filterOutAppLabelOpenMetrics(String responseBody) {
        return responseBody.replaceAll(OPENMETRICS_APP_LABEL_REGEX, "").replaceAll("\\{,", "{").replaceAll(",\\}", "}");
    }

    @BeforeClass
    static public void setup() throws MalformedURLException {
        // set base URI and port number to use for all requests
        String serverUrl = System.getProperty("test.url");
        String protocol = DEFAULT_PROTOCOL;
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (serverUrl != null) {
            URL url = new URL(serverUrl);
            protocol = url.getProtocol();
            host = url.getHost();
            port = (url.getPort() == -1) ? DEFAULT_PORT : url.getPort();
        }

        RestAssured.baseURI = protocol + "://" + host;
        RestAssured.port = port;

        // set user name and password to use for basic authentication for all requests
        String userName = System.getProperty("test.user");
        String password = System.getProperty("test.pwd");

        if (userName != null && password != null) {
            RestAssured.authentication = RestAssured.basic(userName, password);
            RestAssured.useRelaxedHTTPSValidation();
        }

        contextRoot = System.getProperty("context.root", "/optionalTCK");

        applicationPort = Integer.parseInt(System.getProperty("application.port", Integer.toString(port)));

    }

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive jar = ShrinkWrap.create(WebArchive.class, "optionalTCK.war")
                .addPackage(MetricAppBeanOptional.class.getPackage())
                .addClasses(MetricsRESTActivator.class, MetricAppBeanOptional.class, NameObject.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        System.out.println(jar.toString(true));
        return jar;
    }

    /*
     * TEST ALL DIFFERENT TYPE OF REQUESTS WITH NO PARAMETERS
     */
    @Test
    @RunAsClient
    @InSequence(1)
    public void testSimpleRESTGet() throws InterruptedException {
        Header acceptHeader = new Header("Accept", TEXT_PLAIN);

        given().header(acceptHeader).port(applicationPort).when().get(contextRoot + "/get-noparam").then()
                .statusCode(200);

        Response resp = given().header(acceptHeader).when().get(METRICS_ENDPOINT);
        ResponseBuilder responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filterOutAppLabelOpenMetrics(resp.getBody().asString()));
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(TEXT_PLAIN).body(
                containsString(OM_BASE_REQUEST_COUNT_START + "getNoParam" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_REQUEST_TIME_START + "getNoParam" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "getNoParam" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "getNoParam" + OM_BASE_REQUEST_END));
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void testSimpleRESTGetExplicit() throws InterruptedException {
        Header acceptHeader = new Header("Accept", TEXT_PLAIN);

        given().header(acceptHeader).port(applicationPort).when().get(contextRoot + "/get-noparam").then()
                .statusCode(200);
        /*
         * Explicitly hitting /metrics/base/REST.request from now on
         */
        Response resp = given().header(acceptHeader).when().get(RESTREQUEST_METRIC_ENDPOINT);
        ResponseBuilder responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filterOutAppLabelOpenMetrics(resp.getBody().asString()));
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(TEXT_PLAIN).body(
                containsString(OM_BASE_REQUEST_COUNT_START + "getNoParam" + OM_BASE_REQUEST_END + " 2"),
                containsString(OM_BASE_REQUEST_TIME_START + "getNoParam" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "getNoParam" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "getNoParam" + OM_BASE_REQUEST_END));
    }

    @Test
    @RunAsClient
    @InSequence(3)
    public void testSimpleRESTOptions() throws InterruptedException {
        Header acceptHeader = new Header("Accept", TEXT_PLAIN);

        given().header(acceptHeader).port(applicationPort).when().options(contextRoot + "/options-noparam").then()
                .statusCode(200);

        Response resp = given().header(acceptHeader).when().get(RESTREQUEST_METRIC_ENDPOINT);
        ResponseBuilder responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filterOutAppLabelOpenMetrics(resp.getBody().asString()));
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(TEXT_PLAIN).body(
                containsString(OM_BASE_REQUEST_COUNT_START + "optionsNoParam" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_REQUEST_TIME_START + "optionsNoParam" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "optionsNoParam" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "optionsNoParam" + OM_BASE_REQUEST_END));

    }

    @Test
    @RunAsClient
    @InSequence(4)
    public void testSimpleRESTHead() throws InterruptedException {
        Header acceptHeader = new Header("Accept", TEXT_PLAIN);

        given().header(acceptHeader).port(applicationPort).when().head(contextRoot + "/head-noparam").then()
                .statusCode(200);

        Response resp = given().header(acceptHeader).when().get(RESTREQUEST_METRIC_ENDPOINT);
        ResponseBuilder responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filterOutAppLabelOpenMetrics(resp.getBody().asString()));
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(TEXT_PLAIN).body(
                containsString(OM_BASE_REQUEST_COUNT_START + "headNoParam" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_REQUEST_TIME_START + "headNoParam" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "headNoParam" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "headNoParam" + OM_BASE_REQUEST_END));

    }

    @Test
    @RunAsClient
    @InSequence(5)
    public void testSimpleRESTPut() throws InterruptedException {
        Header acceptHeader = new Header("Accept", TEXT_PLAIN);

        given().header(acceptHeader).port(applicationPort).when().put(contextRoot + "/put-noparam").then()
                .statusCode(200);

        Response resp = given().header(acceptHeader).when().get(RESTREQUEST_METRIC_ENDPOINT);
        ResponseBuilder responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filterOutAppLabelOpenMetrics(resp.getBody().asString()));
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(TEXT_PLAIN).body(
                containsString(OM_BASE_REQUEST_COUNT_START + "putNoParam" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_REQUEST_TIME_START + "putNoParam" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "putNoParam" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "putNoParam" + OM_BASE_REQUEST_END));
    }

    @Test
    @RunAsClient
    @InSequence(6)
    public void testSimpleRESTPost() throws InterruptedException {
        Header acceptHeader = new Header("Accept", TEXT_PLAIN);

        given().header(acceptHeader).port(applicationPort).when().post(contextRoot + "/post-noparam").then()
                .statusCode(200);

        Response resp = given().header(acceptHeader).when().get(RESTREQUEST_METRIC_ENDPOINT);
        ResponseBuilder responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filterOutAppLabelOpenMetrics(resp.getBody().asString()));
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(TEXT_PLAIN).body(
                containsString(OM_BASE_REQUEST_COUNT_START + "postNoParam" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_REQUEST_TIME_START + "postNoParam" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "postNoParam" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "postNoParam" + OM_BASE_REQUEST_END));

    }

    @Test
    @RunAsClient
    @InSequence(7)
    public void testDeleteNoParam() throws InterruptedException {
        Header acceptHeader = new Header("Accept", TEXT_PLAIN);

        given().header(acceptHeader).port(applicationPort).when().delete(contextRoot + "/delete-noparam").then()
                .statusCode(200);

        Response resp = given().header(acceptHeader).when().get(RESTREQUEST_METRIC_ENDPOINT);
        ResponseBuilder responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filterOutAppLabelOpenMetrics(resp.getBody().asString()));
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(TEXT_PLAIN).body(
                containsString(OM_BASE_REQUEST_COUNT_START + "deleteNoParam" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_REQUEST_TIME_START + "deleteNoParam" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "deleteNoParam" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "deleteNoParam" + OM_BASE_REQUEST_END));

    }

    /*
     * TEST GET REQUESTS WITH SINGLE PARAMETER
     */

    @Test
    @RunAsClient
    @InSequence(8)
    public void testGetSingleParams() throws InterruptedException {
        Header acceptHeader = new Header("Accept", TEXT_PLAIN);

        given().header(acceptHeader).port(applicationPort).queryParam("qp1", "s1").when()
                .get(contextRoot + "/get-single-string-param").then().statusCode(200);

        given().header(acceptHeader).port(applicationPort).queryParam("qp1", 123).when()
                .get(contextRoot + "/get-single-int-param").then().statusCode(200);

        given().header(acceptHeader).port(applicationPort).queryParam("qp1", 123.45).when()
                .get(contextRoot + "/get-single-double-param").then().statusCode(200);

        given().header(acceptHeader).port(applicationPort).queryParam("qp1", 123L).when()
                .get(contextRoot + "/get-single-long-param").then().statusCode(200);

        given().header(acceptHeader).port(applicationPort).queryParam("qp1", true).when()
                .get(contextRoot + "/get-single-boolean-param").then().statusCode(200);

        Response resp = given().header(acceptHeader).when().get(RESTREQUEST_METRIC_ENDPOINT);
        ResponseBuilder responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filterOutAppLabelOpenMetrics(resp.getBody().asString()));
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(TEXT_PLAIN).body(
                containsString(
                        OM_BASE_REQUEST_COUNT_START + "getSingleStringParam" + STRING_PARAM + OM_BASE_REQUEST_END),
                containsString(
                        OM_BASE_REQUEST_TIME_START + "getSingleStringParam" + STRING_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "getSingleStringParam" + STRING_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "getSingleStringParam" + STRING_PARAM + OM_BASE_REQUEST_END)

                , containsString(OM_BASE_REQUEST_COUNT_START + "getSingleIntParam" + INT_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_REQUEST_TIME_START + "getSingleIntParam" + INT_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "getSingleIntParam" + INT_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "getSingleIntParam" + INT_PARAM + OM_BASE_REQUEST_END)

                ,
                containsString(
                        OM_BASE_REQUEST_COUNT_START + "getSingleDoubleParam" + DOUBLE_PARAM + OM_BASE_REQUEST_END),
                containsString(
                        OM_BASE_REQUEST_TIME_START + "getSingleDoubleParam" + DOUBLE_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "getSingleDoubleParam" + DOUBLE_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "getSingleDoubleParam" + DOUBLE_PARAM + OM_BASE_REQUEST_END)

                , containsString(OM_BASE_REQUEST_COUNT_START + "getSingleLongParam" + LONG_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_REQUEST_TIME_START + "getSingleLongParam" + LONG_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "getSingleLongParam" + LONG_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "getSingleLongParam" + LONG_PARAM + OM_BASE_REQUEST_END)

                ,
                containsString(
                        OM_BASE_REQUEST_COUNT_START + "getSingleBooleanParam" + BOOLEAN_PARAM + OM_BASE_REQUEST_END),
                containsString(
                        OM_BASE_REQUEST_TIME_START + "getSingleBooleanParam" + BOOLEAN_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "getSingleBooleanParam" + BOOLEAN_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "getSingleBooleanParam" + BOOLEAN_PARAM + OM_BASE_REQUEST_END));

    }

    /*
     * TEST GET REQUEST WITH CONTEXT PARAMETERS
     */

    @Test
    @RunAsClient
    @InSequence(9)
    public void testGetContextParams() throws InterruptedException {
        Header acceptHeader = new Header("Accept", TEXT_PLAIN);

        given().header(acceptHeader).port(applicationPort).when().get(contextRoot + "/get-context-params").then()
                .statusCode(200);

        Response resp = given().header(acceptHeader).when().get(RESTREQUEST_METRIC_ENDPOINT);
        ResponseBuilder responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filterOutAppLabelOpenMetrics(resp.getBody().asString()));
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(TEXT_PLAIN).body(
                containsString(
                        OM_BASE_REQUEST_COUNT_START
                                + "getContextParams"
                                + HTTP_HEADERS_PARAM
                                + REQUEST_PARAM
                                + URI_INFO_PARAM
                                + RESOURCE_CONTEXT_PARAM
                                + PROVIDERS_PARAM
                                + APPLICATION_PARAM
                                + SECURITY_CONTEXT_PARAM
                                + CONFIGURATION_PARAM
                                + OM_BASE_REQUEST_END),
                containsString(
                        OM_BASE_REQUEST_TIME_START
                                + "getContextParams"
                                + HTTP_HEADERS_PARAM
                                + REQUEST_PARAM
                                + URI_INFO_PARAM
                                + RESOURCE_CONTEXT_PARAM
                                + PROVIDERS_PARAM
                                + APPLICATION_PARAM
                                + SECURITY_CONTEXT_PARAM
                                + CONFIGURATION_PARAM
                                + OM_BASE_REQUEST_END),
                containsString(
                        OM_BASE_MAX_TIME_START
                                + "getContextParams"
                                + HTTP_HEADERS_PARAM
                                + REQUEST_PARAM
                                + URI_INFO_PARAM
                                + RESOURCE_CONTEXT_PARAM
                                + PROVIDERS_PARAM
                                + APPLICATION_PARAM
                                + SECURITY_CONTEXT_PARAM
                                + CONFIGURATION_PARAM
                                + OM_BASE_REQUEST_END),
                containsString(
                        OM_BASE_MIN_TIME_START
                                + "getContextParams"
                                + HTTP_HEADERS_PARAM
                                + REQUEST_PARAM
                                + URI_INFO_PARAM
                                + RESOURCE_CONTEXT_PARAM
                                + PROVIDERS_PARAM
                                + APPLICATION_PARAM
                                + SECURITY_CONTEXT_PARAM
                                + CONFIGURATION_PARAM
                                + OM_BASE_REQUEST_END));

    }

    /*
     * TEST GET REQUEST WITH LIST PARAMETERS
     */

    @Test
    @RunAsClient
    @InSequence(10)
    public void testGetListParam() throws InterruptedException {
        Header acceptHeader = new Header("Accept", TEXT_PLAIN);

        given().header(acceptHeader).port(applicationPort).queryParam("qp1", Arrays.asList("a", "b", "c")).when()
                .get(contextRoot + "/get-list-param1").then().statusCode(200);

        given().header(acceptHeader).port(applicationPort).queryParam("qp1", Arrays.asList(1, 2))
                .when()
                .get(contextRoot + "/get-list-param2")
                .then()
                .statusCode(200);

        given().header(acceptHeader).port(applicationPort).queryParam("qp1", Arrays.asList(1.0, 2.0))
                .queryParam("qp2", Arrays.asList(1L, 2L))
                .when()
                .get(contextRoot + "/get-list-param3")
                .then()
                .statusCode(200);

        Response resp = given().header(acceptHeader).when().get(RESTREQUEST_METRIC_ENDPOINT);
        ResponseBuilder responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filterOutAppLabelOpenMetrics(resp.getBody().asString()));
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(TEXT_PLAIN).body(
                containsString(OM_BASE_REQUEST_COUNT_START + "getListParam1" + LIST_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_REQUEST_TIME_START + "getListParam1" + LIST_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "getListParam1" + LIST_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "getListParam1" + LIST_PARAM + OM_BASE_REQUEST_END)

                , containsString(OM_BASE_REQUEST_COUNT_START + "getListParam2" + LIST_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_REQUEST_TIME_START + "getListParam2" + LIST_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "getListParam2" + LIST_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "getListParam2" + LIST_PARAM + OM_BASE_REQUEST_END)

                ,
                containsString(
                        OM_BASE_REQUEST_COUNT_START + "getListParam3" + LIST_PARAM + LIST_PARAM + OM_BASE_REQUEST_END),
                containsString(
                        OM_BASE_REQUEST_TIME_START + "getListParam3" + LIST_PARAM + LIST_PARAM + OM_BASE_REQUEST_END),
                containsString(
                        OM_BASE_MAX_TIME_START + "getListParam3" + LIST_PARAM + LIST_PARAM + OM_BASE_REQUEST_END),
                containsString(
                        OM_BASE_MIN_TIME_START + "getListParam3" + LIST_PARAM + LIST_PARAM + OM_BASE_REQUEST_END));

    }

    /*
     * TEST GET REQUEST WITH MULTIPLE TYPE OF PARAMETERS
     */

    @Test
    @RunAsClient
    @InSequence(12)
    public void testGetMultiParam() throws InterruptedException {
        Header acceptHeader = new Header("Accept", TEXT_PLAIN);

        given().header(acceptHeader).port(applicationPort).queryParam("qp1", true).queryParam("qp2", 1)
                .queryParam("qp3", 1.0).queryParam("qp4", "a").queryParam("qp5", 1L).when()
                .get(contextRoot + "/get-multiple-param1").then().statusCode(200);

        given().header(acceptHeader).port(applicationPort).queryParam("qp1", "a")
                .queryParam("qp2", Arrays.asList("b", "c"))
                .when()
                .get(contextRoot + "/get-multiple-param2")
                .then()
                .statusCode(200);

        given().header(acceptHeader).port(applicationPort).queryParam("qp1", Arrays.asList("a", "b", "c"))
                .queryParam("qp1", Arrays.asList("x", "y", "z")).queryParam("qp3", Arrays.asList(1.0, 2.0, 3.0)).when()
                .get(contextRoot + "/get-multiple-param4")
                .then()
                .statusCode(200);

        Response resp = given().header(acceptHeader).when().get(RESTREQUEST_METRIC_ENDPOINT);
        ResponseBuilder responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filterOutAppLabelOpenMetrics(resp.getBody().asString()));
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(TEXT_PLAIN).body(
                containsString(OM_BASE_REQUEST_COUNT_START + "getMultipleParam1"
                        + BOOLEAN_PARAM + INT_PARAM + DOUBLE_PARAM + STRING_PARAM + LONG_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_REQUEST_TIME_START + "getMultipleParam1"
                        + BOOLEAN_PARAM + INT_PARAM + DOUBLE_PARAM + STRING_PARAM + LONG_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "getMultipleParam1"
                        + BOOLEAN_PARAM + INT_PARAM + DOUBLE_PARAM + STRING_PARAM + LONG_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "getMultipleParam1"
                        + BOOLEAN_PARAM + INT_PARAM + DOUBLE_PARAM + STRING_PARAM + LONG_PARAM + OM_BASE_REQUEST_END)

                , containsString(OM_BASE_REQUEST_COUNT_START + "getMultipleParam2"
                        + STRING_PARAM + LIST_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_REQUEST_TIME_START + "getMultipleParam2"
                        + STRING_PARAM + LIST_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "getMultipleParam2"
                        + STRING_PARAM + LIST_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "getMultipleParam2"
                        + STRING_PARAM + LIST_PARAM + OM_BASE_REQUEST_END));

    }

    /*
     * TEST GET REQUEST WITH org.eclipse.microprofile.metrics.test.optional.NameObject
     */
    @Test
    @RunAsClient
    @InSequence(13)
    public void testGetNameObject() throws InterruptedException {
        Header acceptHeader = new Header("Accept", TEXT_PLAIN);

        given().header(acceptHeader).port(applicationPort).when().get(contextRoot + "/get-name-object").then()
                .statusCode(200);

        Response resp = given().header(acceptHeader).when().get(RESTREQUEST_METRIC_ENDPOINT);
        ResponseBuilder responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filterOutAppLabelOpenMetrics(resp.getBody().asString()));
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(TEXT_PLAIN).body(
                containsString(OM_BASE_REQUEST_COUNT_START + "getNameObject" + NAME_OBJECT_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_REQUEST_TIME_START + "getNameObject" + NAME_OBJECT_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "getNameObject" + NAME_OBJECT_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "getNameObject" + NAME_OBJECT_PARAM + OM_BASE_REQUEST_END));

    }

    /*
     * TEST GET REQUEST ASYNC
     */

    @Test
    @RunAsClient
    @InSequence(14)
    public void testGetAsync() throws InterruptedException {
        Header acceptHeader = new Header("Accept", TEXT_PLAIN);

        given().header(acceptHeader).port(applicationPort).when().get(contextRoot + "/get-async").then()
                .statusCode(200);

        Response resp = given().header(acceptHeader).when().get(RESTREQUEST_METRIC_ENDPOINT);
        ResponseBuilder responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filterOutAppLabelOpenMetrics(resp.getBody().asString()));
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(TEXT_PLAIN).body(
                containsString(OM_BASE_REQUEST_COUNT_START + "getAsync" + AYNC_RESP_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_REQUEST_TIME_START + "getAsync" + AYNC_RESP_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "getAsync" + AYNC_RESP_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "getAsync" + AYNC_RESP_PARAM + OM_BASE_REQUEST_END));

        // Proceed to test that expected duration has elapsed

        acceptHeader = new Header("Accept", APPLICATION_JSON);

        resp = given().header(acceptHeader).when().get(RESTREQUEST_METRIC_ENDPOINT);
        JsonPath filteredJSONPath = new JsonPath(filterOutAppLabelJSON(resp.jsonPath().prettify()));
        responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filteredJSONPath.prettify());
        resp = responseBuilder.build();
        Object asyncDurationObject = resp.then().statusCode(200).contentType(APPLICATION_JSON).extract()
                .path(JSON_BASE_REQUEST_TIME_START + "getAsync" + AYNC_RESP_PARAM + JSON_BASE_REQUEST_END);

        String asyncDurationString = (asyncDurationObject instanceof Number)
                ? ((Number) asyncDurationObject).toString()
                : (String) asyncDurationObject;
        Double asyncDurationDouble = Double.parseDouble(asyncDurationString);

        assertTrue("Expected duration to be greater than 5000000000 nanoseconds (i.e 5 seconds)",
                (asyncDurationDouble >= 5000000000.00));
    }

    /*
     * TEST POST REQUEST MULTI PARAM
     */

    @Test
    @RunAsClient
    @InSequence(15)
    public void testPostMultiParam() throws InterruptedException {
        Header acceptHeader = new Header("Accept", TEXT_PLAIN);

        given().header(acceptHeader).port(applicationPort).queryParam("qp1", true).queryParam("qp2", 1)
                .queryParam("qp3", 1.0).queryParam("qp4", "a").queryParam("qp5", 1L).when()
                .post(contextRoot + "/post-multiple-param1").then().statusCode(200);

        given().header(acceptHeader).port(applicationPort).queryParam("qp1", "a")
                .queryParam("qp2", Arrays.asList("b", "c"))
                .when()
                .post(contextRoot + "/post-multiple-param2")
                .then()
                .statusCode(200);

        given().header(acceptHeader).port(applicationPort).queryParam("qp1", Arrays.asList("a", "b", "c"))
                .queryParam("qp1", Arrays.asList("x", "y", "z")).queryParam("qp3", Arrays.asList(1.0, 2.0, 3.0)).when()
                .post(contextRoot + "/post-multiple-param4")
                .then()
                .statusCode(200);

        Response resp = given().header(acceptHeader).when().get(RESTREQUEST_METRIC_ENDPOINT);
        ResponseBuilder responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filterOutAppLabelOpenMetrics(resp.getBody().asString()));
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(TEXT_PLAIN).body(
                containsString(OM_BASE_REQUEST_COUNT_START + "postMultipleParam1"
                        + BOOLEAN_PARAM + INT_PARAM + DOUBLE_PARAM + STRING_PARAM + LONG_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_REQUEST_TIME_START + "postMultipleParam1"
                        + BOOLEAN_PARAM + INT_PARAM + DOUBLE_PARAM + STRING_PARAM + LONG_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "postMultipleParam1"
                        + BOOLEAN_PARAM + INT_PARAM + DOUBLE_PARAM + STRING_PARAM + LONG_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "postMultipleParam1"
                        + BOOLEAN_PARAM + INT_PARAM + DOUBLE_PARAM + STRING_PARAM + LONG_PARAM + OM_BASE_REQUEST_END)

                , containsString(OM_BASE_REQUEST_COUNT_START + "postMultipleParam2"
                        + STRING_PARAM + LIST_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_REQUEST_TIME_START + "postMultipleParam2"
                        + STRING_PARAM + LIST_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "postMultipleParam2"
                        + STRING_PARAM + LIST_PARAM + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "postMultipleParam2"
                        + STRING_PARAM + LIST_PARAM + OM_BASE_REQUEST_END));

    }

    /*
     * TEST GET REQUEST JSON TO RETRIEVE ABOVE VALUES
     */

    @Test
    @RunAsClient
    @InSequence(16)
    public void testValidateGetJSONnoParam() throws InterruptedException {
        Header acceptHeader = new Header("Accept", APPLICATION_JSON);

        Response resp = given().header(acceptHeader).when().get(RESTREQUEST_METRIC_ENDPOINT);
        JsonPath filteredJSONPath = new JsonPath(filterOutAppLabelJSON(resp.jsonPath().prettify()));
        ResponseBuilder responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filteredJSONPath.prettify());
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(APPLICATION_JSON)
                .body(JSON_BASE_REQUEST_COUNT_START + "getNoParam" + JSON_BASE_REQUEST_END, equalTo(2)) // end-point was
                                                                                                        // hit twice
                .body(JSON_BASE_REQUEST_TIME_START + "getNoParam" + JSON_BASE_REQUEST_END, not(0))
                .body(JSON_BASE_MAX_TIME_START + "getNoParam" + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "getNoParam" + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())

                .body(JSON_BASE_REQUEST_COUNT_START + "optionsNoParam" + JSON_BASE_REQUEST_END, equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "optionsNoParam" + JSON_BASE_REQUEST_END, not(0))
                .body(JSON_BASE_MAX_TIME_START + "optionsNoParam" + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "optionsNoParam" + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())

                .body(JSON_BASE_REQUEST_COUNT_START + "headNoParam" + JSON_BASE_REQUEST_END, equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "headNoParam" + JSON_BASE_REQUEST_END, not(0))
                .body(JSON_BASE_MAX_TIME_START + "headNoParam" + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "headNoParam" + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())

                .body(JSON_BASE_REQUEST_COUNT_START + "putNoParam" + JSON_BASE_REQUEST_END, equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "putNoParam" + JSON_BASE_REQUEST_END, not(0))
                .body(JSON_BASE_MAX_TIME_START + "putNoParam" + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "putNoParam" + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())

                .body(JSON_BASE_REQUEST_COUNT_START + "postNoParam" + JSON_BASE_REQUEST_END, equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "postNoParam" + JSON_BASE_REQUEST_END, not(0))
                .body(JSON_BASE_MAX_TIME_START + "postNoParam" + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "postNoParam" + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())

                .body(JSON_BASE_REQUEST_COUNT_START + "deleteNoParam" + JSON_BASE_REQUEST_END, equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "deleteNoParam" + JSON_BASE_REQUEST_END, not(0))
                .body(JSON_BASE_MAX_TIME_START + "deleteNoParam" + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "deleteNoParam" + JSON_BASE_REQUEST_END, nullOrGreaterThanZero());

    }

    @Test
    @RunAsClient
    @InSequence(17)
    public void testValidateGetJSONParam() throws InterruptedException {
        Header acceptHeader = new Header("Accept", APPLICATION_JSON);
        Response resp = given().header(acceptHeader).when().get(RESTREQUEST_METRIC_ENDPOINT);
        JsonPath filteredJSONPath = new JsonPath(filterOutAppLabelJSON(resp.jsonPath().prettify()));
        ResponseBuilder responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filteredJSONPath.prettify());
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(APPLICATION_JSON)
                .body(JSON_BASE_REQUEST_COUNT_START + "getSingleStringParam" + STRING_PARAM + JSON_BASE_REQUEST_END,
                        equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "getSingleStringParam" + STRING_PARAM + JSON_BASE_REQUEST_END,
                        not(0))
                .body(JSON_BASE_MAX_TIME_START + "getSingleStringParam" + STRING_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "getSingleStringParam" + STRING_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_REQUEST_COUNT_START + "getSingleIntParam" + INT_PARAM + JSON_BASE_REQUEST_END,
                        equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "getSingleIntParam" + INT_PARAM + JSON_BASE_REQUEST_END,
                        not(0))
                .body(JSON_BASE_MAX_TIME_START + "getSingleIntParam" + INT_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "getSingleIntParam" + INT_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_REQUEST_COUNT_START + "getSingleDoubleParam" + DOUBLE_PARAM + JSON_BASE_REQUEST_END,
                        equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "getSingleDoubleParam" + DOUBLE_PARAM + JSON_BASE_REQUEST_END,
                        not(0))
                .body(JSON_BASE_MAX_TIME_START + "getSingleDoubleParam" + DOUBLE_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "getSingleDoubleParam" + DOUBLE_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_REQUEST_COUNT_START + "getSingleLongParam" + LONG_PARAM + JSON_BASE_REQUEST_END,
                        equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "getSingleLongParam" + LONG_PARAM + JSON_BASE_REQUEST_END,
                        not(0))
                .body(JSON_BASE_MAX_TIME_START + "getSingleLongParam" + LONG_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "getSingleLongParam" + LONG_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_REQUEST_COUNT_START + "getSingleBooleanParam" + BOOLEAN_PARAM + JSON_BASE_REQUEST_END,
                        equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "getSingleBooleanParam" + BOOLEAN_PARAM + JSON_BASE_REQUEST_END,
                        not(0))
                .body(JSON_BASE_MAX_TIME_START + "getSingleBooleanParam" + BOOLEAN_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "getSingleBooleanParam" + BOOLEAN_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_REQUEST_COUNT_START + "getListParam1" + LIST_PARAM + JSON_BASE_REQUEST_END, equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "getListParam1" + LIST_PARAM + JSON_BASE_REQUEST_END,
                        not(0))
                .body(JSON_BASE_MAX_TIME_START + "getListParam1" + LIST_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "getListParam1" + LIST_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_REQUEST_COUNT_START + "getListParam2" + LIST_PARAM + JSON_BASE_REQUEST_END, equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "getListParam2" + LIST_PARAM + JSON_BASE_REQUEST_END,
                        not(0))
                .body(JSON_BASE_MAX_TIME_START + "getListParam2" + LIST_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "getListParam2" + LIST_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_REQUEST_COUNT_START + "getListParam3" + LIST_PARAM + LIST_PARAM + JSON_BASE_REQUEST_END,
                        equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "getListParam3" + LIST_PARAM + LIST_PARAM + JSON_BASE_REQUEST_END,
                        not(0))
                .body(JSON_BASE_MAX_TIME_START + "getListParam3" + LIST_PARAM + LIST_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "getListParam3" + LIST_PARAM + LIST_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_REQUEST_COUNT_START + "getSingleStringParam" + STRING_PARAM + JSON_BASE_REQUEST_END,
                        equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "getSingleStringParam" + STRING_PARAM + JSON_BASE_REQUEST_END,
                        not(0))
                .body(JSON_BASE_MAX_TIME_START + "getSingleStringParam" + STRING_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "getSingleStringParam" + STRING_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_REQUEST_COUNT_START + "getSingleIntParam" + INT_PARAM + JSON_BASE_REQUEST_END,
                        equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "getSingleIntParam" + INT_PARAM + JSON_BASE_REQUEST_END,
                        not(0))
                .body(JSON_BASE_MAX_TIME_START + "getSingleIntParam" + INT_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "getSingleIntParam" + INT_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_REQUEST_COUNT_START + "getSingleDoubleParam" + DOUBLE_PARAM + JSON_BASE_REQUEST_END,
                        equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "getSingleDoubleParam" + DOUBLE_PARAM + JSON_BASE_REQUEST_END,
                        not(0))
                .body(JSON_BASE_MAX_TIME_START + "getSingleDoubleParam" + DOUBLE_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "getSingleDoubleParam" + DOUBLE_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_REQUEST_COUNT_START + "getSingleLongParam" + LONG_PARAM + JSON_BASE_REQUEST_END,
                        equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "getSingleLongParam" + LONG_PARAM + JSON_BASE_REQUEST_END,
                        not(0))
                .body(JSON_BASE_MAX_TIME_START + "getSingleLongParam" + LONG_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "getSingleLongParam" + LONG_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_REQUEST_COUNT_START + "getSingleBooleanParam" + BOOLEAN_PARAM
                        + JSON_BASE_REQUEST_END, equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "getSingleBooleanParam" + BOOLEAN_PARAM + JSON_BASE_REQUEST_END,
                        not(0))
                .body(JSON_BASE_MAX_TIME_START + "getSingleBooleanParam" + BOOLEAN_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "getSingleBooleanParam" + BOOLEAN_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_REQUEST_COUNT_START + "getMultipleParam1" + BOOLEAN_PARAM + INT_PARAM + DOUBLE_PARAM
                        + STRING_PARAM + LONG_PARAM + JSON_BASE_REQUEST_END, equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "getMultipleParam1" + BOOLEAN_PARAM + INT_PARAM + DOUBLE_PARAM
                        + STRING_PARAM + LONG_PARAM + JSON_BASE_REQUEST_END, not(0))
                .body(JSON_BASE_MAX_TIME_START + "getMultipleParam1" + BOOLEAN_PARAM + INT_PARAM + DOUBLE_PARAM
                        + STRING_PARAM + LONG_PARAM + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "getMultipleParam1" + BOOLEAN_PARAM + INT_PARAM + DOUBLE_PARAM
                        + STRING_PARAM + LONG_PARAM + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())
                .body(JSON_BASE_REQUEST_COUNT_START + "getMultipleParam2" + STRING_PARAM + LIST_PARAM
                        + JSON_BASE_REQUEST_END, equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "getMultipleParam2" + STRING_PARAM + LIST_PARAM
                        + JSON_BASE_REQUEST_END, not(0))
                .body(JSON_BASE_MAX_TIME_START + "getMultipleParam2" + STRING_PARAM + LIST_PARAM
                        + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "getMultipleParam2" + STRING_PARAM + LIST_PARAM
                        + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())
                .body(JSON_BASE_REQUEST_COUNT_START + "getMultipleParam4" + SET_PARAM + SORTED_SET_PARAM
                        + JSON_BASE_REQUEST_END, equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "getMultipleParam4" + SET_PARAM + SORTED_SET_PARAM
                        + JSON_BASE_REQUEST_END, not(0))
                .body(JSON_BASE_MAX_TIME_START + "getMultipleParam4" + SET_PARAM + SORTED_SET_PARAM
                        + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "getMultipleParam4" + SET_PARAM + SORTED_SET_PARAM
                        + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())
                .body(JSON_BASE_REQUEST_COUNT_START + "postMultipleParam1" + BOOLEAN_PARAM + INT_PARAM + DOUBLE_PARAM
                        + STRING_PARAM + LONG_PARAM + JSON_BASE_REQUEST_END, equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "postMultipleParam1" + BOOLEAN_PARAM + INT_PARAM + DOUBLE_PARAM
                        + STRING_PARAM + LONG_PARAM + JSON_BASE_REQUEST_END, not(0))
                .body(JSON_BASE_MAX_TIME_START + "postMultipleParam1" + BOOLEAN_PARAM + INT_PARAM + DOUBLE_PARAM
                        + STRING_PARAM + LONG_PARAM + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "postMultipleParam1" + BOOLEAN_PARAM + INT_PARAM + DOUBLE_PARAM
                        + STRING_PARAM + LONG_PARAM + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())
                .body(JSON_BASE_REQUEST_COUNT_START + "postMultipleParam2" + STRING_PARAM + LIST_PARAM
                        + JSON_BASE_REQUEST_END, equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "postMultipleParam2" + STRING_PARAM + LIST_PARAM
                        + JSON_BASE_REQUEST_END, not(0))
                .body(JSON_BASE_MAX_TIME_START + "postMultipleParam2" + STRING_PARAM + LIST_PARAM
                        + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "postMultipleParam2" + STRING_PARAM + LIST_PARAM
                        + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())
                .body(JSON_BASE_REQUEST_COUNT_START + "postMultipleParam4" + SET_PARAM + SORTED_SET_PARAM
                        + JSON_BASE_REQUEST_END, equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "postMultipleParam4" + SET_PARAM + SORTED_SET_PARAM
                        + JSON_BASE_REQUEST_END, not(0))
                .body(JSON_BASE_MAX_TIME_START + "postMultipleParam4" + SET_PARAM + SORTED_SET_PARAM
                        + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "postMultipleParam4" + SET_PARAM + SORTED_SET_PARAM
                        + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())
                .body(JSON_BASE_REQUEST_COUNT_START + "getAsync" + AYNC_RESP_PARAM + JSON_BASE_REQUEST_END, equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "getAsync" + AYNC_RESP_PARAM + JSON_BASE_REQUEST_END, not(0))
                .body(JSON_BASE_MAX_TIME_START + "getAsync" + AYNC_RESP_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "getAsync" + AYNC_RESP_PARAM + JSON_BASE_REQUEST_END,
                        nullOrGreaterThanZero());
    }

    @Test
    @RunAsClient
    @InSequence(18)
    public void testGetMappedArithException() throws InterruptedException {
        /*
         * Check Prometheus/OpenMetrics
         *
         * Need to explicitly hard code the values expected
         */
        Header acceptHeader = new Header("Accept", TEXT_PLAIN);

        given().header(acceptHeader).port(applicationPort).when().get(contextRoot + "/get-mapped-arithmetic-exception")
                .then().statusCode(200);

        Response resp = given().header(acceptHeader).when().get(METRICS_ENDPOINT);
        ResponseBuilder responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filterOutAppLabelOpenMetrics(resp.getBody().asString()));
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(TEXT_PLAIN).body(
                containsString(OM_BASE_REQUEST_COUNT_START + "getMappedArithException" + OM_BASE_REQUEST_END + " 1"),
                containsString(OM_BASE_REQUEST_TIME_START + "getMappedArithException" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "getMappedArithException" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "getMappedArithException" + OM_BASE_REQUEST_END));

        resp = given().header(acceptHeader).when().get(RESTREQUEST_UNMAPPED_EXCEPION_METRIC_ENDPOINT);
        responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filterOutAppLabelOpenMetrics(resp.getBody().asString()));
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(TEXT_PLAIN).body(containsString(
                OM_BASE_REQUEST_UNMAPPED_EXCEPTION_START + "getMappedArithException" + OM_BASE_REQUEST_END + " 0"));

        /*
         * Check JSON
         */

        acceptHeader = new Header("Accept", APPLICATION_JSON);
        resp = given().header(acceptHeader).when().get(RESTREQUEST_METRIC_ENDPOINT);
        JsonPath filteredJSONPath = new JsonPath(filterOutAppLabelJSON(resp.jsonPath().prettify()));
        responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filteredJSONPath.prettify());
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(APPLICATION_JSON)
                .body(JSON_BASE_REQUEST_COUNT_START + "getMappedArithException"
                        + JSON_BASE_REQUEST_END, equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "getMappedArithException"
                        + JSON_BASE_REQUEST_END, not(0))
                .body(JSON_BASE_MAX_TIME_START + "getMappedArithException"
                        + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "getMappedArithException"
                        + JSON_BASE_REQUEST_END, nullOrGreaterThanZero());

        acceptHeader = new Header("Accept", APPLICATION_JSON);
        resp = given().header(acceptHeader).when().get(RESTREQUEST_UNMAPPED_EXCEPION_METRIC_ENDPOINT);
        filteredJSONPath = new JsonPath(filterOutAppLabelJSON(resp.jsonPath().prettify()));
        responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filteredJSONPath.prettify());
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(APPLICATION_JSON)
                .body(JSON_BASE_REQUEST_UNMAPPED_EXCEPTION_START + "getMappedArithException"
                        + JSON_BASE_REQUEST_END, equalTo(0));
    }

    @Test
    @RunAsClient
    @InSequence(19)
    public void testPostMappedArithException() {
        /*
         * Check Prometheus/OpenMetrics
         *
         * Need to explicitly hard code the values expected
         */
        Header acceptHeader = new Header("Accept", TEXT_PLAIN);

        given().header(acceptHeader).port(applicationPort).when()
                .post(contextRoot + "/post-mapped-arithmetic-exception").then().statusCode(200);

        Response resp = given().header(acceptHeader).when().get(METRICS_ENDPOINT);
        ResponseBuilder responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filterOutAppLabelOpenMetrics(resp.getBody().asString()));
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(TEXT_PLAIN).body(
                containsString(OM_BASE_REQUEST_COUNT_START + "postMappedArithException" + OM_BASE_REQUEST_END + " 1"),
                containsString(OM_BASE_REQUEST_TIME_START + "postMappedArithException" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "postMappedArithException" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "postMappedArithException" + OM_BASE_REQUEST_END));

        resp = given().header(acceptHeader).when().get(RESTREQUEST_UNMAPPED_EXCEPION_METRIC_ENDPOINT);
        responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filterOutAppLabelOpenMetrics(resp.getBody().asString()));
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(TEXT_PLAIN).body(containsString(
                OM_BASE_REQUEST_UNMAPPED_EXCEPTION_START + "postMappedArithException" + OM_BASE_REQUEST_END + " 0"));

        /*
         * Check JSON
         */

        acceptHeader = new Header("Accept", APPLICATION_JSON);
        resp = given().header(acceptHeader).when().get(RESTREQUEST_METRIC_ENDPOINT);
        JsonPath filteredJSONPath = new JsonPath(filterOutAppLabelJSON(resp.jsonPath().prettify()));
        responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filteredJSONPath.prettify());
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(APPLICATION_JSON)
                .body(JSON_BASE_REQUEST_COUNT_START + "postMappedArithException"
                        + JSON_BASE_REQUEST_END, equalTo(1))
                .body(JSON_BASE_REQUEST_TIME_START + "postMappedArithException"
                        + JSON_BASE_REQUEST_END, not(0))
                .body(JSON_BASE_MAX_TIME_START + "postMappedArithException"
                        + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "postMappedArithException"
                        + JSON_BASE_REQUEST_END, nullOrGreaterThanZero());

        acceptHeader = new Header("Accept", APPLICATION_JSON);
        resp = given().header(acceptHeader).when().get(RESTREQUEST_UNMAPPED_EXCEPION_METRIC_ENDPOINT);
        filteredJSONPath = new JsonPath(filterOutAppLabelJSON(resp.jsonPath().prettify()));
        responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filteredJSONPath.prettify());
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(APPLICATION_JSON)
                .body(JSON_BASE_REQUEST_UNMAPPED_EXCEPTION_START + "postMappedArithException"
                        + JSON_BASE_REQUEST_END, equalTo(0));
    }

    @Test
    @RunAsClient
    @InSequence(20)
    public void testGetUnmappedArithException() {
        /*
         * Check Prometheus/OpenMetrics
         *
         * Need to explicitly hard code the values expected
         */
        Header acceptHeader = new Header("Accept", TEXT_PLAIN);

        given().header(acceptHeader).port(applicationPort).when().get(contextRoot + "/get-unmapped-exception").then()
                .statusCode(500);

        Response resp = given().header(acceptHeader).when().get(METRICS_ENDPOINT);
        ResponseBuilder responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filterOutAppLabelOpenMetrics(resp.getBody().asString()));
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(TEXT_PLAIN).body(
                containsString(OM_BASE_REQUEST_COUNT_START + "getUnmappedArithException" + OM_BASE_REQUEST_END + " 0"),
                containsString(OM_BASE_REQUEST_TIME_START + "getUnmappedArithException" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "getUnmappedArithException" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "getUnmappedArithException" + OM_BASE_REQUEST_END));

        resp = given().header(acceptHeader).when().get(RESTREQUEST_UNMAPPED_EXCEPION_METRIC_ENDPOINT);
        responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filterOutAppLabelOpenMetrics(resp.getBody().asString()));
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(TEXT_PLAIN).body(containsString(
                OM_BASE_REQUEST_UNMAPPED_EXCEPTION_START + "getUnmappedArithException" + OM_BASE_REQUEST_END + " 1"));
        /*
         * Check JSON
         */

        acceptHeader = new Header("Accept", APPLICATION_JSON);
        resp = given().header(acceptHeader).when().get(RESTREQUEST_METRIC_ENDPOINT);
        JsonPath filteredJSONPath = new JsonPath(filterOutAppLabelJSON(resp.jsonPath().prettify()));
        responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filteredJSONPath.prettify());
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(APPLICATION_JSON)
                .body(JSON_BASE_REQUEST_COUNT_START + "getUnmappedArithException"
                        + JSON_BASE_REQUEST_END, zero())
                .body(JSON_BASE_REQUEST_TIME_START + "getUnmappedArithException"
                        + JSON_BASE_REQUEST_END, zero())
                .body(JSON_BASE_MAX_TIME_START + "getUnmappedArithException"
                        + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "getUnmappedArithException"
                        + JSON_BASE_REQUEST_END, nullOrGreaterThanZero());

        acceptHeader = new Header("Accept", APPLICATION_JSON);
        resp = given().header(acceptHeader).when().get(RESTREQUEST_UNMAPPED_EXCEPION_METRIC_ENDPOINT);
        filteredJSONPath = new JsonPath(filterOutAppLabelJSON(resp.jsonPath().prettify()));
        responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filteredJSONPath.prettify());
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(APPLICATION_JSON)
                .body(JSON_BASE_REQUEST_UNMAPPED_EXCEPTION_START + "getUnmappedArithException"
                        + JSON_BASE_REQUEST_END, equalTo(1));
    }

    @Test
    @RunAsClient
    @InSequence(21)
    public void testPostUnmappedArithException() {
        /*
         * Check Prometheus/OpenMetrics
         *
         * Need to explicitly hard code the values expected
         */
        Header acceptHeader = new Header("Accept", TEXT_PLAIN);

        given().header(acceptHeader).port(applicationPort).when().post(contextRoot + "/post-unmapped-exception").then()
                .statusCode(500);

        Response resp = given().header(acceptHeader).when().get(METRICS_ENDPOINT);
        ResponseBuilder responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filterOutAppLabelOpenMetrics(resp.getBody().asString()));
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(TEXT_PLAIN).body(
                containsString(OM_BASE_REQUEST_COUNT_START + "postUnmappedArithException" + OM_BASE_REQUEST_END + " 0"),
                containsString(OM_BASE_REQUEST_TIME_START + "postUnmappedArithException" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MAX_TIME_START + "postUnmappedArithException" + OM_BASE_REQUEST_END),
                containsString(OM_BASE_MIN_TIME_START + "postUnmappedArithException" + OM_BASE_REQUEST_END));

        resp = given().header(acceptHeader).when().get(RESTREQUEST_UNMAPPED_EXCEPION_METRIC_ENDPOINT);
        responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filterOutAppLabelOpenMetrics(resp.getBody().asString()));
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(TEXT_PLAIN).body(containsString(
                OM_BASE_REQUEST_UNMAPPED_EXCEPTION_START + "postUnmappedArithException" + OM_BASE_REQUEST_END + " 1"));

        /*
         * Check JSON
         */

        acceptHeader = new Header("Accept", APPLICATION_JSON);
        resp = given().header(acceptHeader).when().get(RESTREQUEST_METRIC_ENDPOINT);
        JsonPath filteredJSONPath = new JsonPath(filterOutAppLabelJSON(resp.jsonPath().prettify()));
        responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filteredJSONPath.prettify());
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(APPLICATION_JSON)
                .body(JSON_BASE_REQUEST_COUNT_START + "postUnmappedArithException"
                        + JSON_BASE_REQUEST_END, zero())
                .body(JSON_BASE_REQUEST_TIME_START + "postUnmappedArithException"
                        + JSON_BASE_REQUEST_END, zero())
                .body(JSON_BASE_MAX_TIME_START + "postUnmappedArithException"
                        + JSON_BASE_REQUEST_END, nullOrGreaterThanZero())
                .body(JSON_BASE_MIN_TIME_START + "postUnmappedArithException"
                        + JSON_BASE_REQUEST_END, nullOrGreaterThanZero());

        acceptHeader = new Header("Accept", APPLICATION_JSON);
        resp = given().header(acceptHeader).when().get(RESTREQUEST_UNMAPPED_EXCEPION_METRIC_ENDPOINT);
        filteredJSONPath = new JsonPath(filterOutAppLabelJSON(resp.jsonPath().prettify()));
        responseBuilder = new ResponseBuilder();
        responseBuilder.clone(resp);
        responseBuilder.setBody(filteredJSONPath.prettify());
        resp = responseBuilder.build();
        resp.then().statusCode(200).contentType(APPLICATION_JSON)
                .body(JSON_BASE_REQUEST_UNMAPPED_EXCEPTION_START + "postUnmappedArithException"
                        + JSON_BASE_REQUEST_END, equalTo(1));
    }

    Matcher<Object> nullOrGreaterThanZero() {
        return new LambdaMatcher<>((value) -> {
            if (value == null) {
                return true;
            } else {
                return value instanceof Number && ((Number) value).doubleValue() > 0;
            }
        }, "Value should be either null, or a number greater than zero");
    }

    Matcher<Object> zero() {
        return new LambdaMatcher<>((value) -> {
            if (value == null) {
                return false;
            } else {
                return value instanceof Number && ((Number) value).doubleValue() == 0.0;
            }
        }, "Value should be zero");
    }

}
