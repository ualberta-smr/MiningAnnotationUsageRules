/*
 **********************************************************************
 * Copyright (c) 2017, 2019 Contributors to the Eclipse Foundation
 *               2017 Red Hat, Inc. and/or its affiliates
 *               and other contributors as indicated by the @author tags.
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
package org.eclipse.microprofile.metrics;

import java.util.EnumSet;

/**
 * An enumeration representing the different types of metrics.
 *
 * @author hrupp, Raymond Lam, Ouyang Zhou
 */
public enum MetricType {

    /**
     * A concurrent gauge counts the number of parallel invocations of a target (method). Upon entering the target the
     * value is increased. It is decreased again upon exiting the target.
     */

    CONCURRENT_GAUGE("concurrent gauge", ConcurrentGauge.class),
    /**
     * A Counter monotonically increases its values. An example could be the number of Transactions committed.
     */
    COUNTER("counter", Counter.class),

    /**
     * A Gauge has values that 'arbitrarily' goes up/down at each sampling. An example could be CPU load
     */
    GAUGE("gauge", Gauge.class),

    /**
     * A Meter measures the rate at which a set of events occur. An example could be amount of Transactions per Hour.
     */
    METERED("meter", Meter.class),

    /**
     * A Histogram calculates the distribution of a value.
     */
    HISTOGRAM("histogram", Histogram.class),

    /**
     * A timer aggregates timing durations and provides duration statistics, plus throughput statistics
     */
    TIMER("timer", Timer.class),

    /**
     * A simple timer aggregates timing durations
     */
    SIMPLE_TIMER("simple timer", SimpleTimer.class),

    /**
     * Invalid - Placeholder
     */
    INVALID("invalid", null);

    private String type;
    private Class<?> classtype;

    MetricType(String type, Class<?> classtype) {
        this.type = type;
        this.classtype = classtype;
    }

    public String toString() {
        return type;
    }

    private static final EnumSet<MetricType> METRIC_TYPES = EnumSet.allOf(MetricType.class);

    /**
     * Convert the string representation into an enum
     *
     * @param in
     *            the String representation (NOT the class name but rather a string like "counter", "gauge",...)
     * @return the matching Enum
     * @throws IllegalArgumentException
     *             if in is not a valid enum value
     */
    public static MetricType from(String in) {
        for (MetricType u : METRIC_TYPES) {
            if (u.type.equals(in)) {
                return u;
            }
        }
        throw new IllegalArgumentException(in + " is not a valid MetricType");
    }

    /**
     * Convert the metric Java class into a MetricType
     *
     * @param in
     *            The metric class
     * @return the matching MetricType value
     * @throws IllegalArgumentException
     *             if in is not a valid metric class
     */
    public static MetricType from(Class<?> in) {
        for (MetricType u : METRIC_TYPES) {
            if (u.classtype != null && u.classtype.isAssignableFrom(in)) {
                return u;
            }
        }
        throw new IllegalArgumentException(in + " is not a valid metric type");
    }

    /**
     * Convert the metric Java class name into a MetricType
     *
     * @param className
     *            The name of a metric class
     * @return the matching MetricType value
     * @throws IllegalArgumentException
     *             if className is not a valid metric class
     */
    public static MetricType fromClassName(String className) {
        for (MetricType u : METRIC_TYPES) {
            if (u.classtype != null && u.classtype.getName().equals(className)) {
                return u;
            }
        }
        throw new IllegalArgumentException(className + " is not a valid metric class");
    }

}
