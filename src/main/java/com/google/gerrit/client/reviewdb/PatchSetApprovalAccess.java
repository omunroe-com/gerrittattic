// Copyright (C) 2008 The Android Open Source Project
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

package com.google.gerrit.client.reviewdb;

import com.google.gwtorm.client.Access;
import com.google.gwtorm.client.OrmException;
import com.google.gwtorm.client.PrimaryKey;
import com.google.gwtorm.client.Query;
import com.google.gwtorm.client.ResultSet;

public interface PatchSetApprovalAccess extends
    Access<PatchSetApproval, PatchSetApproval.Key> {
  @PrimaryKey("key")
  PatchSetApproval get(PatchSetApproval.Key key) throws OrmException;

  @Query("WHERE key.patchSetId.changeId = ?")
  ResultSet<PatchSetApproval> byChange(Change.Id id) throws OrmException;

  @Query("WHERE key.patchSetId = ?")
  ResultSet<PatchSetApproval> byPatchSet(PatchSet.Id id) throws OrmException;

  @Query("WHERE key.patchSetId = ? AND key.accountId = ?")
  ResultSet<PatchSetApproval> byPatchSetUser(PatchSet.Id patchSet,
      Account.Id account) throws OrmException;

  @Query("WHERE changeOpen = true AND key.accountId = ?")
  ResultSet<PatchSetApproval> openByUser(Account.Id account)
      throws OrmException;

  @Query("WHERE changeOpen = false AND key.accountId = ?"
      + " ORDER BY changeSortKey DESC LIMIT 10")
  ResultSet<PatchSetApproval> closedByUser(Account.Id account)
      throws OrmException;

  @Query("WHERE changeOpen = false AND key.accountId = ? ORDER BY changeSortKey")
  ResultSet<PatchSetApproval> closedByUserAll(Account.Id account)
      throws OrmException;
}
