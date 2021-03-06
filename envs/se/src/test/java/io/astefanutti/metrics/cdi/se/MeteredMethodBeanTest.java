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

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import io.astefanutti.metrics.cdi.MetricsExtension;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(Arquillian.class)
public class MeteredMethodBeanTest {

    private final static String METER_NAME = MetricRegistry.name(MeteredMethodBean.class, "meteredMethod");

    private final static AtomicLong METER_COUNT = new AtomicLong();

    @Deployment
    static Archive<?> createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class)
            // Test bean
            .addClass(MeteredMethodBean.class)
            // Metrics CDI extension
            .addPackage(MetricsExtension.class.getPackage())
            // Bean archive deployment descriptor
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    private MetricRegistry registry;

    @Inject
    private MeteredMethodBean bean;

    @Test
    @InSequence(1)
    public void meteredMethodNotCalledYet() {
        assertThat("Meter is not registered correctly", registry.getMeters(), hasKey(METER_NAME));
        Meter meter = registry.getMeters().get(METER_NAME);

        // Make sure that the meter hasn't been marked yet
        assertThat("Meter count is incorrect", meter.getCount(), is(equalTo(METER_COUNT.get())));
    }

    @Test
    @InSequence(2)
    public void callMeteredMethodOnce() {
        assertThat("Meter is not registered correctly", registry.getMeters(), hasKey(METER_NAME));
        Meter meter = registry.getMeters().get(METER_NAME);

        // Call the metered method and assert it's been marked
        bean.meteredMethod();

        // Make sure that the meter has been marked
        assertThat("Timer count is incorrect", meter.getCount(), is(equalTo(METER_COUNT.incrementAndGet())));
    }

    @Test
    @InSequence(3)
    public void removeMeterFromRegistry() {
        assertThat("Meter is not registered correctly", registry.getMeters(), hasKey(METER_NAME));
        Meter meter = registry.getMeters().get(METER_NAME);

        // Remove the meter from metrics registry
        registry.remove(METER_NAME);

        try {
            // Call the metered method and assert an exception is thrown
            bean.meteredMethod();
        } catch (RuntimeException cause) {
            assertThat(cause, is(instanceOf(IllegalStateException.class)));
            assertThat(cause.getMessage(), is(equalTo("No meter with name [" + METER_NAME + "] found in registry [" + registry + "]")));
            // Make sure that the meter hasn't been marked
            assertThat("Meter count is incorrect", meter.getCount(), is(equalTo(METER_COUNT.get())));
            return;
        }

        fail("No exception has been re-thrown!");
    }
}