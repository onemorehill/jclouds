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
package org.jclouds.cloudstack.features;

import org.jclouds.cloudstack.domain.Account;
import org.jclouds.cloudstack.domain.User;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Tests behavior of {@code GlobaUserClient}
 */
@Test(groups = "live", singleThreaded = true, testName = "GlobalUserClientLiveTest")
public class GlobalUserClientLiveTest extends BaseCloudStackClientLiveTest {

   private Account createTestAccount() {
      return globalAdminClient.getAccountClient().createAccount(
         prefix + "-account", Account.Type.USER, "dummy@example.com",
         "First", "Last", "hashed-password");

   }

   @Test
   public void testCreateUser() {
      Account testAccount = createTestAccount();
      User testUser = null;
      try {
         testUser = globalAdminClient.getUserClient().createUser(prefix + "-user",
            testAccount.getName(), "dummy2@example.com", "md5-password", "First", "Last");

         assertNotNull(testUser);
         assertEquals(testUser.getName(), prefix + "-user");
         assertEquals(testUser.getAccount(), prefix + "-account");

      } finally {
         if (testUser != null) {
            globalAdminClient.getUserClient().deleteUser(testUser.getId());
         }
         globalAdminClient.getAccountClient().deleteAccount(testAccount.getId());
      }

   }

}
