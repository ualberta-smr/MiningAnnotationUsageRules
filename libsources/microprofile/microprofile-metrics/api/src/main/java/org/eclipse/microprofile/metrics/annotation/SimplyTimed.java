/*
 * ********************************************************************
 *  Copyright (c) 2019, 2020 Contributors to the Eclipse Foundation
 *
 *  See the NOTICES file(s) distributed with this work for additional
 *  information regarding copyright ownership.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 * ********************************************************************
 *
 */
package org.eclipse.microprofile.metrics.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.microprofile.metrics.MetricUnits;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

/**
 * An annotation for marking a method, constructor, or class as simply timed. The underlying
 * {@link org.eclipse.microprofile.metrics.SimpleTimer SimpleTimer} metric tracks elapsed time duration and count. This
 * is a lightweight alternative to {@link org.eclipse.microprofile.metrics.Timer timer} (@{@link Timed}). The metric
 * will be registered in the application MetricRegistry.
 * <p>
 * Given a method annotated with {@literal @}SimplyTimed like this:
 * </p>
 * 
 * <pre>
 * <code>
 *     {@literal @}SimplyTimed(name = "fancyName")
 *     public String fancyName(String name) {
 *         return "Sir Captain " + name;
 *     }
 * </code>
 * </pre>
 * 
 * A simple timer with the fully qualified class name + {@code fancyName} will be created and each time the
 * {@code #fancyName(String)} method is invoked, the method's execution will be timed.
 *
 * <p>
 * Given a class annotated with {@literal @}SimplyTimed like this:
 * </p>
 * 
 * <pre>
 * <code>
 *     {@literal @}SimplyTimed
 *     public class SimplyTimedBean {
 *         public void simplyTimedMethod1() {}
 *         public void simplyTimedMethod2() {}
 *     }
 * </code>
 * </pre>
 * 
 * A simple timer for the defining class will be created for each of the constructors/methods. Each time a
 * constructor/method is invoked, the execution will be timed with the respective simple timer.
 *
 * This annotation will throw an IllegalStateException if the constructor/method is invoked, but the metric no longer
 * exists in the MetricRegistry.
 */
@Inherited
@Documented
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface SimplyTimed {

    /**
     * @return The name of the simple timer.
     */
    @Nonbinding
    String name() default "";

    /**
     * @return The tags of the simple timer. Each {@code String} tag must be in the form of 'key=value'. If the input is
     *         empty or does not contain a '=' sign, the entry is ignored.
     *
     * @see org.eclipse.microprofile.metrics.Metadata
     */
    @Nonbinding
    String[] tags() default {};

    /**
     * @return If {@code true}, use the given name as an absolute name. If {@code false} (default), use the given name
     *         relative to the annotated class. When annotating a class, this must be {@code false}.
     */
    @Nonbinding
    boolean absolute() default false;

    /**
     * @return The display name of the simple timer.
     *
     * @see org.eclipse.microprofile.metrics.Metadata
     */
    @Nonbinding
    String displayName() default "";

    /**
     * @return The description of the simple timer
     * @see org.eclipse.microprofile.metrics.Metadata
     */
    @Nonbinding
    String description() default "";

    /**
     * @return The unit of the simple timer. By default, the value is {@link MetricUnits#NANOSECONDS}.
     *
     * @see org.eclipse.microprofile.metrics.Metadata
     * @see org.eclipse.microprofile.metrics.MetricUnits
     */
    @Nonbinding
    String unit() default MetricUnits.NANOSECONDS;

}
