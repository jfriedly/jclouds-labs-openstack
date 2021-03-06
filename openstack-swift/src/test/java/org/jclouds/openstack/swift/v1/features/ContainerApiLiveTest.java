/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.openstack.swift.v1.features;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Map;
import java.util.Map.Entry;

import org.jclouds.openstack.swift.v1.SwiftApi;
import org.jclouds.openstack.swift.v1.domain.Container;
import org.jclouds.openstack.swift.v1.internal.BaseSwiftApiLiveTest;
import org.jclouds.openstack.swift.v1.options.CreateContainerOptions;
import org.jclouds.openstack.swift.v1.options.ListContainerOptions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;

/**
 * @author Adrian Cole
 * @author Jeremy Daggett
 */
@Test(groups = "live", testName = "ContainerApiLiveTest")
public class ContainerApiLiveTest extends BaseSwiftApiLiveTest<SwiftApi> {

   private String name = getClass().getSimpleName();

   public void testList() throws Exception {
      for (String regionId : regions) {
         ContainerApi containerApi = api.containerApiInRegion(regionId);
         FluentIterable<Container> response = containerApi.list();
         assertNotNull(response);
         for (Container container : response) {
            assertNotNull(container.getName());
            assertTrue(container.getObjectCount() >= 0);
            assertTrue(container.getBytesUsed() >= 0);
         }
      }
   }

   public void testListWithOptions() throws Exception {
      String lexicographicallyBeforeName = name.substring(0, name.length() - 1);
      for (String regionId : regions) {
         ListContainerOptions options = ListContainerOptions.Builder.marker(lexicographicallyBeforeName);
         Container container = api.containerApiInRegion(regionId).list(options).get(0);
         assertEquals(container.getName(), name);
         assertTrue(container.getObjectCount() == 0);
         assertTrue(container.getBytesUsed() == 0);
      }
   }
   
   public void testGet() throws Exception {
      for (String regionId : regions) {
         Container container = api.containerApiInRegion(regionId).get(name);
         assertEquals(container.getName(), name);
         assertTrue(container.getObjectCount() == 0);
         assertTrue(container.getBytesUsed() == 0);
      }
   }

   public void testUpdateMetadata() throws Exception {
      Map<String, String> meta = ImmutableMap.of("MyAdd1", "foo", "MyAdd2", "bar");

      for (String regionId : regions) {
         ContainerApi containerApi = api.containerApiInRegion(regionId);
         assertTrue(containerApi.updateMetadata(name, meta));
         containerHasMetadata(containerApi, name, meta);
      }
   }

   public void testDeleteMetadata() throws Exception {
      Map<String, String> meta = ImmutableMap.of("MyDelete1", "foo", "MyDelete2", "bar");

      for (String regionId : regions) {
         ContainerApi containerApi = api.containerApiInRegion(regionId);
         // update
         assertTrue(containerApi.updateMetadata(name, meta));
         containerHasMetadata(containerApi, name, meta);
         // delete
         assertTrue(containerApi.deleteMetadata(name, meta));
         Container container = containerApi.get(name);
         for (Entry<String, String> entry : meta.entrySet()) {
            // note keys are returned in lower-case!
            assertFalse(container.getMetadata().containsKey(entry.getKey().toLowerCase()));
         }
      }
   }

   static void containerHasMetadata(ContainerApi containerApi, String name, Map<String, String> meta) {
      Container container = containerApi.get(name);
      for (Entry<String, String> entry : meta.entrySet()) {
         // note keys are returned in lower-case!
         assertEquals(container.getMetadata().get(entry.getKey().toLowerCase()), entry.getValue(),
               container + " didn't have metadata: " + entry);
      }
   }

   @Override
   @BeforeClass(groups = "live")
   public void setup() {
      super.setup();
      for (String regionId : regions) {
         api.containerApiInRegion(regionId).createIfAbsent(name, CreateContainerOptions.NONE);
      }
   }

   @Override
   @AfterClass(groups = "live")
   public void tearDown() {
      for (String regionId : regions) {
         api.containerApiInRegion(regionId).deleteIfEmpty(name);
      }
      super.tearDown();
   }
}
