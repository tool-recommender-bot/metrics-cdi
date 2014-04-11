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
package org.stefanutti.metrics.cdi.se;

import com.codahale.metrics.*;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.stefanutti.metrics.cdi.Metric;
import org.stefanutti.metrics.cdi.MetricsExtension;

import javax.inject.Inject;

import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(Arquillian.class)
public class MetricProducerFieldBeanTest {

    @Deployment
    static Archive<?> createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class)
            // Test bean
            .addClass(MetricProducerFieldBean.class)
            // Metrics CDI extension
            .addPackage(MetricsExtension.class.getPackage())
            // Bean archive deployment descriptor
            // FIXME: use EmptyAsset.INSTANCE when OWB supports CDI 1.1
            .addAsManifestResource("beans.xml");
    }

    @Inject
    private MetricRegistry registry;

    @Test
    @InSequence(1)
    public void countersNotIncrementedYet() {
        assertThat("Counters are not registered correctly", registry.getCounters(), allOf(hasKey("counter1"), hasKey("counter2")));
        Counter counter1 = registry.getCounters().get("counter1");
        Counter counter2 = registry.getCounters().get("counter2");

        assertThat("Gauge is not registered correctly", registry.getGauges(), hasKey("ratioGauge"));
        @SuppressWarnings("unchecked")
        Gauge<Double> gauge = (Gauge<Double>) registry.getGauges().get("ratioGauge");

        assertThat("Gauge value is incorrect", gauge.getValue(), is(equalTo(((double) counter1.getCount()) / ((double) counter2.getCount()))));
    }

    @Test
    @InSequence(2)
    public void incrementCountersFromRegistry() {
        assertThat("Counters are not registered correctly", registry.getCounters(), allOf(hasKey("counter1"), hasKey("counter2")));
        Counter counter1 = registry.getCounters().get("counter1");
        Counter counter2 = registry.getCounters().get("counter2");

        assertThat("Gauge is not registered correctly", registry.getGauges(), hasKey("ratioGauge"));
        @SuppressWarnings("unchecked")
        Gauge<Double> gauge = (Gauge<Double>) registry.getGauges().get("ratioGauge");

        counter1.inc(Math.round(Math.random() * Integer.MAX_VALUE));
        counter2.inc(Math.round(Math.random() * Integer.MAX_VALUE));

        assertThat("Gauge value is incorrect", gauge.getValue(), is(equalTo(((double) counter1.getCount()) / ((double) counter2.getCount()))));
    }

    @Test
    @InSequence(3)
    public void incrementCountersFromInjection(@Metric(name = "ratioGauge", absolute = true) Gauge<Double> gauge,
                                               @Metric(name = "counter1", absolute = true) Counter counter1,
                                               @Metric(name = "counter2", absolute = true) Counter counter2) {
        counter1.inc(Math.round(Math.random() * Integer.MAX_VALUE));
        counter2.inc(Math.round(Math.random() * Integer.MAX_VALUE));

        assertThat("Gauge value is incorrect", gauge.getValue(), is(equalTo(((double) counter1.getCount()) / ((double) counter2.getCount()))));

        assertThat("Gauge is not registered correctly", registry.getGauges(), hasKey("ratioGauge"));
        @SuppressWarnings("unchecked")
        Gauge<Double> gaugeFromRegistry = (Gauge<Double>) registry.getGauges().get("ratioGauge");

        assertThat("Gauge values from registry and injection do not match", gauge.getValue(), is(equalTo(gaugeFromRegistry.getValue())));
    }
}