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

import org.apache.webbeans.test.portable.events.extensions.NestedGenericsTypeVariableExtension.Bar;
import org.apache.webbeans.test.portable.events.extensions.NestedGenericsTypeVariableExtension.BazImpl;
import org.apache.webbeans.test.portable.events.extensions.NestedGenericsTypeVariableExtension.Foo;
import org.apache.webbeans.test.portable.events.extensions.NestedGenericsTypeVariableExtension.MyClass;

/**
 * Observer for a fully parameterized nested generic type
 * {@code MyClass<Foo<Bar<BazImpl>>>}. This is used with a raw implementation
 * of {@link MyClass} to verify that the parameterized observer does not match
 * a raw type.
 */
public class RawTypeParameterizedObserverExtension implements Extension
{
    public static boolean CALLED = false;

    void processClasses(@Observes ProcessAnnotatedType<MyClass<Foo<Bar<BazImpl>>>> event)
    {
        CALLED = true;
    }
}

