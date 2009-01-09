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

package com.google.gerrit.client.changes;

import com.google.gerrit.client.Gerrit;
import com.google.gerrit.client.SignedInListener;
import com.google.gerrit.client.data.ApprovalType;
import com.google.gerrit.client.data.ChangeDetail;
import com.google.gerrit.client.data.PatchSetDetail;
import com.google.gerrit.client.reviewdb.ApprovalCategory;
import com.google.gerrit.client.reviewdb.ApprovalCategoryValue;
import com.google.gerrit.client.reviewdb.PatchSet;
import com.google.gerrit.client.rpc.Common;
import com.google.gerrit.client.rpc.GerritCallback;
import com.google.gerrit.client.ui.RefreshListener;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DisclosureEvent;
import com.google.gwt.user.client.ui.DisclosureHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwtjsonrpc.client.VoidResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class PatchSetPanel extends Composite implements DisclosureHandler {
  private static final int R_DOWNLOAD = 0;
  private static final int R_CNT = 1;

  private final ChangeDetail changeDetail;
  private final PatchSet patchSet;
  private final FlowPanel body;
  private List<RefreshListener> refreshListeners;

  private Grid infoTable;
  private Panel actionsPanel;
  private PatchTable patchTable;
  private SignedInListener signedInListener;

  PatchSetPanel(final ChangeDetail detail, final PatchSet ps) {
    changeDetail = detail;
    patchSet = ps;
    body = new FlowPanel();
    initWidget(body);
  }

  public void addRefreshListener(final RefreshListener r) {
    if (refreshListeners == null) {
      refreshListeners = new ArrayList<RefreshListener>();
    }
    if (!refreshListeners.contains(r)) {
      refreshListeners.add(r);
    }
  }

  public void removeRefreshListener(final RefreshListener r) {
    if (refreshListeners != null) {
      refreshListeners.remove(r);
    }
  }

  protected void fireOnSuggestRefresh() {
    if (refreshListeners != null) {
      for (final RefreshListener r : refreshListeners) {
        r.onSuggestRefresh();
      }
    }
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    if (signedInListener != null) {
      Gerrit.addSignedInListener(signedInListener);
    }
  }

  @Override
  protected void onUnload() {
    if (signedInListener != null) {
      Gerrit.removeSignedInListener(signedInListener);
    }
    super.onUnload();
  }

  public void ensureLoaded(final PatchSetDetail detail) {
    infoTable = new Grid(R_CNT, 2);
    infoTable.setStyleName("gerrit-InfoBlock");
    infoTable.addStyleName("gerrit-PatchSetInfoBlock");

    initRow(R_DOWNLOAD, Util.C.patchSetInfoDownload());

    final CellFormatter itfmt = infoTable.getCellFormatter();
    itfmt.addStyleName(0, 0, "topmost");
    itfmt.addStyleName(0, 1, "topmost");
    itfmt.addStyleName(R_CNT - 1, 0, "bottomheader");
    itfmt.addStyleName(R_DOWNLOAD, 1, "command");

    infoTable.setText(R_DOWNLOAD, 1, Util.M.repoDownload(changeDetail
        .getChange().getDest().getParentKey().get(), changeDetail.getChange()
        .getChangeId(), patchSet.getPatchSetId()));


    patchTable = new PatchTable();
    patchTable.setSavePointerId("patchTable "
        + changeDetail.getChange().getChangeId() + " "
        + patchSet.getPatchSetId());
    patchTable.display(detail.getPatches());
    patchTable.finishDisplay(false);

    body.add(infoTable);
    if (!changeDetail.getChange().getStatus().isClosed()
        && changeDetail.isCurrentPatchSet(detail)) {
      actionsPanel = new FlowPanel();
      actionsPanel.setStyleName("gerrit-PatchSetActions");
      signedInListener = new SignedInListener() {
        public void onSignIn() {
        }

        public void onSignOut() {
          actionsPanel.clear();
          actionsPanel.setVisible(false);
        }
      };
      Gerrit.addSignedInListener(signedInListener);
      body.add(actionsPanel);
      populateActions(detail);
    }
    body.add(patchTable);
  }

  private void populateActions(final PatchSetDetail detail) {
    final Set<ApprovalCategory.Id> allowed = changeDetail.getCurrentActions();
    if (allowed == null) {
      // No set of actions, perhaps the user is not signed in?
      return;
    }

    for (final ApprovalType at : Common.getGerritConfig().getActionTypes()) {
      final ApprovalCategoryValue max = at.getMax();
      if (max == null || max.getValue() <= 0) {
        // No positive assertion, don't draw a button.
        continue;
      }
      if (!allowed.contains(at.getCategory().getId())) {
        // User isn't permitted to invoke this.
        continue;
      }

      final Button b =
          new Button(Util.M.patchSetAction(at.getCategory().getName(), detail
              .getPatchSet().getPatchSetId()));
      b.addClickListener(new ClickListener() {
        public void onClick(Widget sender) {
          b.setEnabled(false);
          Util.MANAGE_SVC.patchSetAction(max.getId(), patchSet.getId(),
              new GerritCallback<VoidResult>() {
                public void onSuccess(VoidResult result) {
                  actionsPanel.remove(b);
                  fireOnSuggestRefresh();
                }

                @Override
                public void onFailure(Throwable caught) {
                  b.setEnabled(true);
                  super.onFailure(caught);
                }
              });
        }
      });
      actionsPanel.add(b);
    }
  }

  public void onOpen(final DisclosureEvent event) {
    if (infoTable == null) {
      Util.DETAIL_SVC.patchSetDetail(patchSet.getId(),
          new GerritCallback<PatchSetDetail>() {
            public void onSuccess(final PatchSetDetail result) {
              ensureLoaded(result);
            }
          });
    }
  }

  public void onClose(final DisclosureEvent event) {
  }

  private void initRow(final int row, final String name) {
    infoTable.setText(row, 0, name);
    infoTable.getCellFormatter().addStyleName(row, 0, "header");
  }
}