/*
 * Copyright 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.fiat.providers;

import com.netflix.spinnaker.fiat.model.resources.Role;
import com.netflix.spinnaker.fiat.model.resources.ServiceAccount;
import com.netflix.spinnaker.fiat.providers.internal.Front50Service;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import retrofit.RetrofitError;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DefaultServiceAccountProvider extends BaseProvider implements ServiceAccountProvider {

  @Autowired
  private Front50Service front50Service;

  @Override
  public Set<ServiceAccount> getAll() throws ProviderException {
    try {
      val returnVal = front50Service
          .getAllServiceAccounts()
          .stream()
          .collect(Collectors.toSet());
      success();
      return returnVal;
    } catch (RetrofitError re) {
      failure();
      throw new ProviderException(re);
    }
  }

  /**
   * Return the set of service accounts to which a user with the specified collection of groups
   * has access.
   * Service accounts are usually defined using a full email address, but the specified groups are
   * normally just the first part before the "@" symbol. This implementation strips everything
   * after the "@" symbol for the purposes of service account/group matching.
   * @param roles
   */
  @Override
  public Set<ServiceAccount> getAllRestricted(@NonNull Collection<Role> roles) {
    val groupNames = roles.stream().map(Role::getName).collect(Collectors.toList());
    return getAll()
        .stream()
        .filter(svcAcct -> !Collections.disjoint(svcAcct.getRequiredGroupMembership(), groupNames))
        .collect(Collectors.toSet());
  }

  @Override
  public Set<ServiceAccount> getAllUnrestricted() throws ProviderException {
    return Collections.emptySet();
  }
}
