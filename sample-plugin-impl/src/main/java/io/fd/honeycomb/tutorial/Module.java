/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.tutorial;

import static io.fd.honeycomb.tutorial.ModuleConfiguration.ELEMENT_SERVICE_NAME;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import io.fd.honeycomb.notification.ManagedNotificationProducer;
import io.fd.honeycomb.tutorial.init.ConfigDataInitializer;
import io.fd.honeycomb.tutorial.notif.SampleNotificationProducer;
import io.fd.honeycomb.tutorial.read.ModuleStateReaderFactory;
import io.fd.honeycomb.tutorial.write.ModuleWriterFactory;
import io.fd.honeycomb.data.init.DataTreeInitializer;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.write.WriterFactory;
import net.jmob.guice.conf.core.ConfigurationModule;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.sample.plugin.rev160918.sample.plugin.params.Element;

/**
 * Module class instantiating sample-plugin plugin components.
 */
public final class Module extends AbstractModule {

    // TODO This initiates all the plugin components, but it still needs to be registered/wired into an integration
    // module producing runnable distributions. There is one such distribution in honeycomb project:
    // vpp-integration/minimal-distribution
    // In order to integrate this plugin with the distribution:
    // 1. Add a dependency on this maven module to the the distribution's pom.xml
    // 2. Add an instance of this module into the distribution in its Main class

    @Override
    protected void configure() {
        // requests injection of properties
        install(ConfigurationModule.create());
        requestInjection(ModuleConfiguration.class);

        // creates binding for interface implementation by name
        bind(new TypeLiteral<CrudService<Element>>(){})
                .annotatedWith(Names.named(ELEMENT_SERVICE_NAME))
                .to(ElementCrudService.class);

        // creates reader factory binding
        // can hold multiple binding for separate yang modules
        final Multibinder<ReaderFactory> readerFactoryBinder = Multibinder.newSetBinder(binder(), ReaderFactory.class);
        readerFactoryBinder.addBinding().to(ModuleStateReaderFactory.class);

        // create writer factory binding
        // can hold multiple binding for separate yang modules
        final Multibinder<WriterFactory> writerFactoryBinder = Multibinder.newSetBinder(binder(), WriterFactory.class);
        writerFactoryBinder.addBinding().to(ModuleWriterFactory.class);

        // create initializer binding
        // can hold multiple binding for separate yang modules
        final Multibinder<DataTreeInitializer> initializerBinder =
                Multibinder.newSetBinder(binder(), DataTreeInitializer.class);
        initializerBinder.addBinding().to(ConfigDataInitializer.class);

        Multibinder.newSetBinder(binder(), ManagedNotificationProducer.class).addBinding()
                .to(SampleNotificationProducer.class);
    }
}
