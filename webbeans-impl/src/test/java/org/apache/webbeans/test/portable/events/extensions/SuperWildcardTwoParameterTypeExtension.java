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

import org.apache.webbeans.test.portable.events.extensions.TwoParameterTypeWithTypeVariableExtension.BarImpl;
import org.apache.webbeans.test.portable.events.extensions.TwoParameterTypeWithTypeVariableExtension.FooImpl;
import org.apache.webbeans.test.portable.events.extensions.TwoParameterTypeWithTypeVariableExtension.KeyValueStore;

/**
 * Extension that observes a {@link ProcessAnnotatedType} whose type is
 * {@code KeyValueStore<? super FooImpl, ? super BarImpl>}. Used to verify that
 * an observer using {@code ? super} wildcards is triggered for a concrete
 * implementation {@code KeyValueStore<FooImpl, BarImpl>}.
 */
public class SuperWildcardTwoParameterTypeExtension implements Extension
{
    public static boolean CALLED = false;

    void processClasses(@Observes ProcessAnnotatedType<KeyValueStore<? super FooImpl, ? super BarImpl>> event)
    {
        CALLED = true;
    }
}

