// Copyright (C) 2009 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.server.rpc.account;

import com.google.gerrit.client.account.AgreementInfo;
import com.google.gerrit.client.reviewdb.AccountAgreement;
import com.google.gerrit.client.reviewdb.AccountGroup;
import com.google.gerrit.client.reviewdb.AccountGroupAgreement;
import com.google.gerrit.client.reviewdb.ContributorAgreement;
import com.google.gerrit.client.reviewdb.ReviewDb;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.rpc.Handler;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AgreementInfoFactory extends Handler<AgreementInfo> {
  interface Factory {
    AgreementInfoFactory create();
  }

  private final ReviewDb db;
  private final IdentifiedUser user;

  private AgreementInfo info;

  @Inject
  AgreementInfoFactory(final ReviewDb db, final IdentifiedUser user) {
    this.db = db;
    this.user = user;
  }

  @Override
  public AgreementInfo call() throws Exception {
    final List<AccountAgreement> userAccepted =
        db.accountAgreements().byAccount(user.getAccountId()).toList();
    final List<AccountGroupAgreement> groupAccepted =
        new ArrayList<AccountGroupAgreement>();
    for (final AccountGroup.Id groupId : user.getEffectiveGroups()) {
      groupAccepted.addAll(db.accountGroupAgreements().byGroup(groupId)
          .toList());
    }

    final Map<ContributorAgreement.Id, ContributorAgreement> agreements =
        new HashMap<ContributorAgreement.Id, ContributorAgreement>();
    for (final AccountAgreement a : userAccepted) {
      final ContributorAgreement.Id id = a.getAgreementId();
      if (!agreements.containsKey(id)) {
        agreements.put(id, db.contributorAgreements().get(id));
      }
    }
    for (final AccountGroupAgreement a : groupAccepted) {
      final ContributorAgreement.Id id = a.getAgreementId();
      if (!agreements.containsKey(id)) {
        agreements.put(id, db.contributorAgreements().get(id));
      }
    }

    info = new AgreementInfo();
    info.setUserAccepted(userAccepted);
    info.setGroupAccepted(groupAccepted);
    info.setAgreements(agreements);
    return info;
  }
}
