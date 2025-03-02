/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.kubernetes.fabric8.discovery;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointPortBuilder;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.EndpointsList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import org.assertj.core.util.Strings;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.kubernetes.commons.discovery.KubernetesDiscoveryProperties;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.kubernetes.commons.discovery.KubernetesDiscoveryProperties.Metadata;

class KubernetesDiscoveryClientFilterMetadataTest {

	private static final KubernetesClient CLIENT = Mockito.mock(KubernetesClient.class);

	private final MixedOperation<Service, ServiceList, ServiceResource<Service>> serviceOperation = Mockito
			.mock(MixedOperation.class);

	private final MixedOperation<Endpoints, EndpointsList, Resource<Endpoints>> endpointsOperation = Mockito
			.mock(MixedOperation.class);

	private final ServiceResource<Service> serviceResource = Mockito.mock(ServiceResource.class);

	private final FilterWatchListDeletable<Endpoints, EndpointsList, Resource<Endpoints>> filter = Mockito
			.mock(FilterWatchListDeletable.class);

	@Test
	void testAllExtraMetadataDisabled() {
		String serviceId = "s";

		Metadata metadata = new Metadata(false, null, false, null, false, null);
		KubernetesDiscoveryProperties properties = new KubernetesDiscoveryProperties(true, false, Set.of(), true, 60,
				false, null, Set.of(), Map.of(), null, metadata, 0, true);

		KubernetesDiscoveryClient discoveryClient = new KubernetesDiscoveryClient(CLIENT, properties, a -> null);

		setupServiceWithLabelsAndAnnotationsAndPorts(serviceId, "ns", Map.of("l1", "lab"), Map.of("l1", "lab"),
				Map.of(80, "http", 5555, ""));

		List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
		assertThat(instances).hasSize(1);
		assertThat(instances.get(0).getMetadata()).isEmpty();
	}

	@Test
	void testLabelsEnabled() {
		String serviceId = "s";

		Metadata metadata = new Metadata(true, null, false, null, false, null);
		KubernetesDiscoveryProperties properties = new KubernetesDiscoveryProperties(true, false, Set.of(), true, 60,
				false, null, Set.of(), Map.of(), null, metadata, 0, true);

		KubernetesDiscoveryClient discoveryClient = new KubernetesDiscoveryClient(CLIENT, properties, a -> null);

		setupServiceWithLabelsAndAnnotationsAndPorts(serviceId, "ns", Map.of("l1", "v1", "l2", "v2"),
				Map.of("l1", "lab"), Map.of(80, "http", 5555, ""));

		List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
		assertThat(instances).hasSize(1);
		assertThat(instances.get(0).getMetadata()).containsOnly(entry("l1", "v1"), entry("l2", "v2"));
	}

	@Test
	void testLabelsEnabledWithPrefix() {
		String serviceId = "s";

		Metadata metadata = new Metadata(true, "l_", false, null, false, null);
		KubernetesDiscoveryProperties properties = new KubernetesDiscoveryProperties(true, false, Set.of(), true, 60,
				false, null, Set.of(), Map.of(), null, metadata, 0, true);

		KubernetesDiscoveryClient discoveryClient = new KubernetesDiscoveryClient(CLIENT, properties, a -> null);

		setupServiceWithLabelsAndAnnotationsAndPorts(serviceId, "ns", Map.of("l1", "v1", "l2", "v2"),
				Map.of("l1", "lab"), Map.of(80, "http", 5555, ""));

		List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
		assertThat(instances).hasSize(1);
		assertThat(instances.get(0).getMetadata()).containsOnly(entry("l_l1", "v1"), entry("l_l2", "v2"));
	}

	@Test
	void testAnnotationsEnabled() {
		String serviceId = "s";

		Metadata metadata = new Metadata(false, null, true, null, false, null);
		KubernetesDiscoveryProperties properties = new KubernetesDiscoveryProperties(true, false, Set.of(), true, 60,
				false, null, Set.of(), Map.of(), null, metadata, 0, true);

		KubernetesDiscoveryClient discoveryClient = new KubernetesDiscoveryClient(CLIENT, properties, a -> null);

		setupServiceWithLabelsAndAnnotationsAndPorts(serviceId, "ns", Map.of("l1", "v1"),
				Map.of("a1", "v1", "a2", "v2"), Map.of(80, "http", 5555, ""));

		List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
		assertThat(instances).hasSize(1);
		assertThat(instances.get(0).getMetadata()).containsOnly(entry("a1", "v1"), entry("a2", "v2"));
	}

	@Test
	void testAnnotationsEnabledWithPrefix() {
		String serviceId = "s";

		Metadata metadata = new Metadata(false, null, true, "a_", false, null);
		KubernetesDiscoveryProperties properties = new KubernetesDiscoveryProperties(true, false, Set.of(), true, 60,
				false, null, Set.of(), Map.of(), null, metadata, 0, true);

		KubernetesDiscoveryClient discoveryClient = new KubernetesDiscoveryClient(CLIENT, properties, a -> null);

		setupServiceWithLabelsAndAnnotationsAndPorts(serviceId, "ns", Map.of("l1", "v1"),
				Map.of("a1", "v1", "a2", "v2"), Map.of(80, "http", 5555, ""));

		List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
		assertThat(instances).hasSize(1);
		assertThat(instances.get(0).getMetadata()).containsOnly(entry("a_a1", "v1"), entry("a_a2", "v2"));
	}

	@Test
	void testPortsEnabled() {
		String serviceId = "s";

		Metadata metadata = new Metadata(false, null, false, null, true, null);
		KubernetesDiscoveryProperties properties = new KubernetesDiscoveryProperties(true, false, Set.of(), true, 60,
				false, null, Set.of(), Map.of(), null, metadata, 0, true);

		KubernetesDiscoveryClient discoveryClient = new KubernetesDiscoveryClient(CLIENT, properties, a -> null);

		setupServiceWithLabelsAndAnnotationsAndPorts(serviceId, "ns", Map.of("l1", "v1"),
				Map.of("a1", "v1", "a2", "v2"), Map.of(80, "http", 5555, ""));

		List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
		assertThat(instances).hasSize(1);
		assertThat(instances.get(0).getMetadata()).containsOnly(entry("http", "80"));
	}

	@Test
	void testPortsEnabledWithPrefix() {
		String serviceId = "s";

		Metadata metadata = new Metadata(false, null, false, null, true, "p_");
		KubernetesDiscoveryProperties properties = new KubernetesDiscoveryProperties(true, false, Set.of(), true, 60,
				false, null, Set.of(), Map.of(), null, metadata, 0, true);

		KubernetesDiscoveryClient discoveryClient = new KubernetesDiscoveryClient(CLIENT, properties, a -> null);

		setupServiceWithLabelsAndAnnotationsAndPorts(serviceId, "ns", Map.of("l1", "v1"),
				Map.of("a1", "v1", "a2", "v2"), Map.of(80, "http", 5555, ""));

		List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
		assertThat(instances).hasSize(1);
		assertThat(instances.get(0).getMetadata()).containsOnly(entry("p_http", "80"));
	}

	@Test
	void testLabelsAndAnnotationsAndPortsEnabledWithPrefix() {
		String serviceId = "s";

		Metadata metadata = new Metadata(true, "l_", true, "a_", true, "p_");
		KubernetesDiscoveryProperties properties = new KubernetesDiscoveryProperties(true, false, Set.of(), true, 60,
				false, null, Set.of(), Map.of(), null, metadata, 0, true);

		KubernetesDiscoveryClient discoveryClient = new KubernetesDiscoveryClient(CLIENT, properties, a -> null);

		setupServiceWithLabelsAndAnnotationsAndPorts(serviceId, "ns", Map.of("l1", "la1"),
				Map.of("a1", "an1", "a2", "an2"), Map.of(80, "http", 5555, ""));

		List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
		assertThat(instances).hasSize(1);
		assertThat(instances.get(0).getMetadata()).containsOnly(entry("a_a1", "an1"), entry("a_a2", "an2"),
				entry("l_l1", "la1"), entry("p_http", "80"));
	}

	private void setupServiceWithLabelsAndAnnotationsAndPorts(String serviceId, String namespace,
			Map<String, String> labels, Map<String, String> annotations, Map<Integer, String> ports) {
		Service service = new ServiceBuilder().withNewMetadata().withNamespace(namespace).withLabels(labels)
				.withAnnotations(annotations).endMetadata().withNewSpec().withPorts(getServicePorts(ports)).endSpec()
				.build();
		when(this.serviceOperation.withName(serviceId)).thenReturn(this.serviceResource);
		when(this.serviceResource.get()).thenReturn(service);
		when(CLIENT.services()).thenReturn(this.serviceOperation);
		when(CLIENT.services().inNamespace(anyString())).thenReturn(this.serviceOperation);

		ObjectMeta objectMeta = new ObjectMeta();
		objectMeta.setNamespace(namespace);

		Endpoints endpoints = new EndpointsBuilder().withMetadata(objectMeta).addNewSubset()
				.addAllToPorts(getEndpointPorts(ports)).addNewAddress().endAddress().endSubset().build();

		when(CLIENT.endpoints()).thenReturn(this.endpointsOperation);

		EndpointsList endpointsList = new EndpointsList(null, Collections.singletonList(endpoints), null, null);
		when(filter.list()).thenReturn(endpointsList);
		when(filter.withLabels(anyMap())).thenReturn(filter);

		when(CLIENT.endpoints().withField(eq("metadata.name"), eq(serviceId))).thenReturn(filter);

	}

	private List<ServicePort> getServicePorts(Map<Integer, String> ports) {
		return ports.entrySet().stream().map(e -> {
			ServicePortBuilder servicePortBuilder = new ServicePortBuilder();
			servicePortBuilder.withPort(e.getKey());
			if (!Strings.isNullOrEmpty(e.getValue())) {
				servicePortBuilder.withName(e.getValue());
			}
			return servicePortBuilder.build();
		}).collect(toList());
	}

	private List<EndpointPort> getEndpointPorts(Map<Integer, String> ports) {
		return ports.entrySet().stream().map(e -> {
			EndpointPortBuilder endpointPortBuilder = new EndpointPortBuilder();
			endpointPortBuilder.withPort(e.getKey());
			if (!Strings.isNullOrEmpty(e.getValue())) {
				endpointPortBuilder.withName(e.getValue());
			}
			return endpointPortBuilder.build();
		}).collect(toList());
	}

}
