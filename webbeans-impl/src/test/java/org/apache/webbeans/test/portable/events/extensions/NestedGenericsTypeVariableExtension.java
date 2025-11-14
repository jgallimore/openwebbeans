/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.portable.events.extensions;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;

/**
 * Extension and helper types demonstrating nested generics like
 * {@code MyClass<? extends Foo<? extends Bar<? extends Baz>>>}.
 */
public class NestedGenericsTypeVariableExtension implements Extension
{
    public static boolean CALLED = false;

    <T extends MyClass<? extends Foo<? extends Bar<? extends Baz>>>> void processClasses(
            @Observes ProcessAnnotatedType<T> event)
    {
        CALLED = true;
    }

    public interface Baz
    {
    }

    public interface Bar<T extends Baz>
    {
    }

    public interface Foo<T extends Bar<? extends Baz>>
    {
    }

    public interface MyClass<T extends Foo<? extends Bar<? extends Baz>>>
    {
    }

    public static class BazImpl implements Baz
    {
    }

    public static class BarImpl implements Bar<BazImpl>
    {
    }

    public static class FooImpl implements Foo<BarImpl>
    {
    }

    public static class MyClassImpl implements MyClass<FooImpl>
    {
    }

    /**
     * Raw implementation of {@link MyClass} without exposing its generic
     * parameterization. Used to verify that a parameterized observer does not
     * match a raw implementation.
     */
    @SuppressWarnings("rawtypes")
    public static class MyClassRaw implements MyClass
    {
    }

    public static class OtherBazImpl implements Baz
    {
    }

    public static class OtherBarImpl implements Bar<OtherBazImpl>
    {
    }

    public static class OtherFooImpl implements Foo<OtherBarImpl>
    {
    }

    public static class OtherMyClassImpl implements MyClass<OtherFooImpl>
    {
    }
}
