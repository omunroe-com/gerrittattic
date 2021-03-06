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

package com.google.gerrit.client;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwtjsonrpc.client.RpcCompleteEvent;
import com.google.gwtjsonrpc.client.RpcCompleteHandler;
import com.google.gwtjsonrpc.client.RpcStartEvent;
import com.google.gwtjsonrpc.client.RpcStartHandler;

public class RpcStatus implements RpcStartHandler, RpcCompleteHandler {
  private static int hideDepth;

  /** Execute code, hiding the RPCs they execute from being shown visually. */
  public static void hide(final Runnable run) {
    try {
      hideDepth++;
      run.run();
    } finally {
      hideDepth--;
    }
  }

  private final Label loading;
  private int activeCalls;

  RpcStatus(final Panel p) {
    final FlowPanel r = new FlowPanel();
    r.setStyleName("gerrit-RpcStatusPanel");
    p.add(r);

    loading = new InlineLabel();
    loading.setText(Gerrit.C.rpcStatusLoading());
    loading.setStyleName("gerrit-RpcStatus");
    loading.addStyleDependentName("Loading");
    loading.setVisible(false);
    r.add(loading);
  }

  @Override
  public void onRpcStart(final RpcStartEvent event) {
    if (++activeCalls == 1) {
      if (hideDepth == 0) {
        loading.setVisible(true);
      }
    }
  }

  @Override
  public void onRpcComplete(final RpcCompleteEvent event) {
    if (--activeCalls == 0) {
      loading.setVisible(false);
    }
  }
}
