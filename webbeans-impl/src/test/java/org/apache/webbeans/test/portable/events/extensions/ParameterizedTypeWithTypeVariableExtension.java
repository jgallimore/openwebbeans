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

public class ParameterizedTypeWithTypeVariableExtension implements Extension
{

    public static boolean CALLED = false;

    <T extends EmitterFactory<?>> void processClasses( @Observes ProcessAnnotatedType<T> event)
    {
        CALLED = true;
    }

    public static class EmitterFactoryImpl implements EmitterFactory<EmitterImpl<Object>>
    {
        @Override
        public EmitterImpl<Object> createEmitter()
        {
            return new EmitterImpl<>();
        }
    }

    public static class EmitterImpl<T> implements Emitter<T>, MessagePublisherProvider<T>
    {
        @Override
        public void send(T message)
        {
            // no-op
        }

        @Override
        public T getProvider()
        {
            return null;
        }
    }

    public interface Emitter<T>
    {
        void send(T message);
    }

    public interface EmitterFactory<T extends MessagePublisherProvider<?>>
    {
        T createEmitter();
    }

    public interface MessagePublisherProvider<T>
    {
        T getProvider();
    }
}
