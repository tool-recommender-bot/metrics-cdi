/**
 * Copyright (C) 2013 Antonin Stefanutti (antonin.stefanutti@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.stefanutti.metrics.cdi;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Gauge;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Interceptor
@TimedBinding
class TimedInterceptor {

    @Inject
    MetricRegistry registry;

    @Inject
    private TimedInterceptor() {

    }

    @AroundInvoke
    private Object timeMethod(InvocationContext ic) throws Exception {
        Timed timed = ic.getMethod().getAnnotation(Timed.class);
        String finalName = timed.name().isEmpty() ? ic.getMethod().getName() : timed.name();
        Timer timer = registry.timer(timed.absolute() ? finalName : MetricRegistry.name(ic.getMethod().getDeclaringClass(), finalName));
        Timer.Context context = timer.time();
        try {
            return ic.proceed();
        } finally {
            context.stop();
        }
    }
}