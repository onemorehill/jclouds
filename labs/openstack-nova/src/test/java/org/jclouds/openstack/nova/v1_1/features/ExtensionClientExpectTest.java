/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.openstack.nova.v1_1.features;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.net.URI;

import org.jclouds.http.HttpRequest;
import org.jclouds.http.HttpResponse;
import org.jclouds.openstack.nova.v1_1.NovaClient;
import org.jclouds.openstack.nova.v1_1.internal.BaseNovaRestClientExpectTest;
import org.jclouds.openstack.nova.v1_1.parse.ParseExtensionListTest;
import org.jclouds.openstack.nova.v1_1.parse.ParseExtensionTest;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;

/**
 * Tests annotation parsing of {@code ExtensionAsyncClient}
 * 
 * @author Adrian Cole
 */
@Test(groups = "unit", testName = "ExtensionClientExpectTest")
public class ExtensionClientExpectTest extends BaseNovaRestClientExpectTest {

   public void testListExtensionsWhenResponseIs2xx() throws Exception {
      HttpRequest listExtensions = HttpRequest
            .builder()
            .method("GET")
            .endpoint(
                  URI.create("https://compute.north.host/v1.1/3456/extensions"))
            .headers(
                  ImmutableMultimap.<String, String> builder()
                        .put("Accept", "application/json")
                        .put("X-Auth-Token", authToken).build()).build();

      HttpResponse listExtensionsResponse = HttpResponse.builder().statusCode(200)
            .payload(payloadFromResource("/extension_list.json")).build();

      NovaClient clientWhenExtensionsExist = requestsSendResponses(
            keystoneAuthWithAccessKeyAndSecretKey, responseWithKeystoneAccess,
            listExtensions, listExtensionsResponse);

      assertEquals(clientWhenExtensionsExist.getConfiguredRegions(),
            ImmutableSet.of("North"));

      assertEquals(clientWhenExtensionsExist.getExtensionClientForRegion("North")
            .listExtensions().toString(), new ParseExtensionListTest().expected()
            .toString());
   }

   public void testListExtensionsWhenReponseIs404IsEmpty() throws Exception {
      HttpRequest listExtensions = HttpRequest
            .builder()
            .method("GET")
            .endpoint(
                  URI.create("https://compute.north.host/v1.1/3456/extensions"))
            .headers(
                  ImmutableMultimap.<String, String> builder()
                        .put("Accept", "application/json")
                        .put("X-Auth-Token", authToken).build()).build();

      HttpResponse listExtensionsResponse = HttpResponse.builder().statusCode(404)
            .build();

      NovaClient clientWhenNoServersExist = requestsSendResponses(
            keystoneAuthWithAccessKeyAndSecretKey, responseWithKeystoneAccess,
            listExtensions, listExtensionsResponse);

      assertTrue(clientWhenNoServersExist.getExtensionClientForRegion("North")
            .listExtensions().isEmpty());
   }

   // TODO: gson deserializer for Multimap
   public void testGetExtensionByAliasWhenResponseIs2xx() throws Exception {

      HttpRequest getExtension = HttpRequest
            .builder()
            .method("GET")
            .endpoint(
                  URI.create("https://compute.north.host/v1.1/3456/extensions/RS-PIE"))
            .headers(
                  ImmutableMultimap.<String, String> builder()
                        .put("Accept", "application/json")
                        .put("X-Auth-Token", authToken).build()).build();

      HttpResponse getExtensionResponse = HttpResponse.builder().statusCode(200)
            .payload(payloadFromResource("/extension_details.json")).build();

      NovaClient clientWhenExtensionsExist = requestsSendResponses(
            keystoneAuthWithAccessKeyAndSecretKey, responseWithKeystoneAccess,
            getExtension, getExtensionResponse);

      assertEquals(clientWhenExtensionsExist.getExtensionClientForRegion("North")
            .getExtensionByAlias("RS-PIE").toString(),
            new ParseExtensionTest().expected().toString());
   }

   public void testGetExtensionByAliasWhenResponseIs404() throws Exception {
      HttpRequest getExtension = HttpRequest
            .builder()
            .method("GET")
            .endpoint(
                  URI.create("https://compute.north.host/v1.1/3456/extensions/RS-PIE"))
            .headers(
                  ImmutableMultimap.<String, String> builder()
                        .put("Accept", "application/json")
                        .put("X-Auth-Token", authToken).build()).build();

      HttpResponse getExtensionResponse = HttpResponse.builder().statusCode(404)
            .payload(payloadFromResource("/extension_details.json")).build();

      NovaClient clientWhenNoExtensionsExist = requestsSendResponses(
            keystoneAuthWithAccessKeyAndSecretKey, responseWithKeystoneAccess,
            getExtension, getExtensionResponse);

      assertNull(clientWhenNoExtensionsExist.getExtensionClientForRegion("North").getExtensionByAlias("RS-PIE"));

   }

}
