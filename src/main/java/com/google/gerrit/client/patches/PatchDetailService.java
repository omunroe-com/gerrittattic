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

package com.google.gerrit.client.patches;

import com.google.gerrit.client.data.ApprovalSummary;
import com.google.gerrit.client.data.ApprovalSummarySet;
import com.google.gerrit.client.data.PatchScript;
import com.google.gerrit.client.data.PatchScriptSettings;
import com.google.gerrit.client.reviewdb.Account;
import com.google.gerrit.client.reviewdb.ApprovalCategoryValue;
import com.google.gerrit.client.reviewdb.Change;
import com.google.gerrit.client.reviewdb.Patch;
import com.google.gerrit.client.reviewdb.PatchLineComment;
import com.google.gerrit.client.reviewdb.PatchSet;
import com.google.gerrit.client.reviewdb.Patch.Key;
import com.google.gerrit.client.rpc.SignInRequired;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwtjsonrpc.client.RemoteJsonService;
import com.google.gwtjsonrpc.client.VoidResult;

import java.util.List;
import java.util.Set;

public interface PatchDetailService extends RemoteJsonService {
  void patchScript(Patch.Key key, PatchSet.Id a, PatchSet.Id b,
      PatchScriptSettings settings, AsyncCallback<PatchScript> callback);

  void patchComments(Patch.Key key, PatchSet.Id a, PatchSet.Id b,
      AsyncCallback<CommentDetail> callback);

  @SignInRequired
  void saveDraft(PatchLineComment comment,
      AsyncCallback<PatchLineComment> callback);

  @SignInRequired
  void deleteDraft(PatchLineComment.Key key, AsyncCallback<VoidResult> callback);

  @SignInRequired
  void publishComments(PatchSet.Id psid, String message,
      Set<ApprovalCategoryValue.Id> approvals,
      AsyncCallback<VoidResult> callback);

  @SignInRequired
  void addReviewers(Change.Id id, List<String> reviewers,
      AsyncCallback<AddReviewerResult> callback);

  @SignInRequired
  void abandonChange(PatchSet.Id patchSetId, String message,
      AsyncCallback<VoidResult> callback);

  void userApprovals(Set<Change.Id> cids, Account.Id aid,
      AsyncCallback<ApprovalSummarySet> callback);

  void strongestApprovals(Set<Change.Id> cids,
      AsyncCallback<ApprovalSummarySet> callback);

  /**
   * Update the reviewed status for the patch.
   */
  @SignInRequired
  void setReviewedByCurrentUser(Key patchKey, boolean reviewed, AsyncCallback<VoidResult> callback);
}
