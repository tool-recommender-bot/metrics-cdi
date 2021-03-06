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

import com.codahale.metrics.health.HealthCheck;
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
import java.util.SortedMap;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Arquillian.class)
public class HealthCheckMethodProducerTest {

    @Deployment
    public static Archive<?> createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class)
            // TestBean
            .addClass(HealthCheckProducerMethodBean.class)
            // Metrics CDI Extension
            .addPackage(MetricsExtension.class.getPackage())
            // Bean archive deployment descriptor
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    private HealthCheckRegistry registry;

    @Inject
    @Named("not_registered_healthcheck")
    private HealthCheck not_registerd_healthcheck;

    @Test
    @InSequence(1)
    public void healthChecksRegisteredProperly() {
        assertThat("HealthChecks are not registered correctly", registry.getNames(),
            contains("check1", "check2", "check3"));

        assertThat("HealthChecks are not registered correctly", registry.getNames(),
            not(contains("not_registered_healthcheck")));
    }

    @Test
    @InSequence(2)
    public void healthChecksRegistered() {
        assertThat("HealthChecks are not registered correctly", registry.getNames(),
            containsInRelativeOrder("check1", "check2"));

        SortedMap<String, HealthCheck.Result> results = registry.runHealthChecks();

        assertThat("check1 did not execute", results, hasKey("check1"));
        assertThat("check1 did not pass", results.get("check1").isHealthy(), is(true));

        assertThat("check2 did not execute", results, hasKey("check2"));
        assertThat("check2 did not fail", results.get("check2").isHealthy(), is(false));
    }
}
