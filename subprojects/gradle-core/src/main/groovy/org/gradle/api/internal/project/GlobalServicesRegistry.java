/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.project;

import org.gradle.cache.CacheFactory;
import org.gradle.cache.DefaultCacheFactory;
import org.gradle.initialization.CommandLine2StartParameterConverter;
import org.gradle.initialization.DefaultCommandLine2StartParameterConverter;
import org.gradle.initialization.DefaultLoggingConfigurer;
import org.gradle.initialization.LoggingConfigurer;

/**
 * Contains the services shared by all builds in a given process.
 */
public class GlobalServicesRegistry extends AbstractServiceRegistry {
    public GlobalServicesRegistry() {
        add(LoggingConfigurer.class, new DefaultLoggingConfigurer());
        add(CommandLine2StartParameterConverter.class, new DefaultCommandLine2StartParameterConverter());
        add(CacheFactory.class, new DefaultCacheFactory());
    }
}
