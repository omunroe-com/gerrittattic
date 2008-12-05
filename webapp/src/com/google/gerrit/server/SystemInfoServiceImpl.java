// Copyright 2008 Google Inc.
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

package com.google.gerrit.server;

import com.google.gerrit.client.data.GerritConfig;
import com.google.gerrit.client.data.SystemInfoService;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class SystemInfoServiceImpl implements SystemInfoService {
  private final GerritServer server;

  public SystemInfoServiceImpl(final GerritServer server) {
    this.server = server;
  }

  public void loadGerritConfig(final AsyncCallback<GerritConfig> callback) {
    callback.onSuccess(server.getGerritConfig());
  }
}