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

import java.io.Serializable;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;

import org.apache.webbeans.test.portable.events.extensions.NestedGenericsTypeVariableExtension.Bar;
import org.apache.webbeans.test.portable.events.extensions.NestedGenericsTypeVariableExtension.BazImpl;
import org.apache.webbeans.test.portable.events.extensions.NestedGenericsTypeVariableExtension.Foo;

/**
 * Extension demonstrating multiple bounds on a nested type variable:
 * {@code <T extends Bar<? extends Baz> & Serializable>} used inside
 * {@code Foo<T>}.
 */
public class MultipleBoundsNestedTypeVariableExtension implements Extension
{
    public static boolean CALLED = false;

    <T extends Bar<? extends NestedGenericsTypeVariableExtension.Baz> & Serializable> void processClasses(
            @Observes ProcessAnnotatedType<Foo<T>> event)
    {
        CALLED = true;
    }

    /**
     * Bar implementation which satisfies both bounds: it is a Bar<BazImpl>
     * and also {@link Serializable}.
     */
    public static class SerializableBarImpl implements Bar<BazImpl>, Serializable
    {
    }

    /**
     * Bar implementation which only satisfies the Bar<BazImpl> bound but does
     * not implement {@link Serializable}.
     */
    public static class NonSerializableBarImpl implements Bar<BazImpl>
    {
    }

    /**
     * Foo implementation using the fully constrained {@link SerializableBarImpl}.
     */
    public static class FooWithSerializableBar implements Foo<SerializableBarImpl>
    {
    }

    /**
     * Foo implementation using the non-serializable {@link NonSerializableBarImpl}.
     */
    public static class FooWithNonSerializableBar implements Foo<NonSerializableBarImpl>
    {
    }
}

