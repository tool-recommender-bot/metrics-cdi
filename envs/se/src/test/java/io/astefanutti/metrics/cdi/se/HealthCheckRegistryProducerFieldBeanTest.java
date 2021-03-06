/**
 * Copyright © 2013 Antonin Stefanutti (antonin.stefanutti@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.astefanutti.metrics.cdi.se;

import com.codahale.metrics.health.HealthCheckRegistry;
import io.astefanutti.metrics.cdi.MetricsExtension;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.inject.Named;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


@RunWith(Arquillian.class)
public class HealthCheckRegistryProducerFieldBeanTest {

    @Deployment
    public static Archive<?> createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class)
            // HealthCheck registry bean
            .addClass(HealthCheckRegistryProducerFieldBean.class)
            // Test bean
            .addClass(HealthCheckBean.class)
            .addClass(UnnamedHealthCheckBean.class)
            // MetricsCDI extension
            .addPackage(MetricsExtension.class.getPackage())
            // Bean archive deployment descriptor
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    private HealthCheckRegistry registry;

    @Inject
    private UnnamedHealthCheckBean unnamedHealthCheckBean;

    @Inject
    @Named("HealthCheckBean")
    private HealthCheckBean bean;

    @Test
    @InSequence(1)
    public void healthCheckNotCalledYet() {
        assertThat("HealthCheck is not registered correctly", registry.getNames(), contains("HealthCheckBean", UnnamedHealthCheckBean.class.getName()));

        assertThat("Execution hasn't occurred yet", bean.getCheckCount(), is(equalTo(0l)));
    }

    @Test
    @InSequence(2)
    public void healthCheckInvoked() {
        assertThat("HealthCheck is not registered correctly", registry.getNames(), contains("HealthCheckBean", UnnamedHealthCheckBean.class.getName()));

        registry.runHealthChecks();

        assertThat("Execution count is incorrect", bean.getCheckCount(), is(equalTo(1l)));
        assertThat("Execution count on unnamed bean is incorrect", unnamedHealthCheckBean.getCheckCount(), is(equalTo(1l)));
    }
}
