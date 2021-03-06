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

package com.google.gerrit.client.changes;

import com.google.gerrit.client.Gerrit;
import com.google.gerrit.client.Link;
import com.google.gerrit.client.data.AccountInfo;
import com.google.gerrit.client.data.AccountInfoCache;
import com.google.gerrit.client.data.ChangeDetail;
import com.google.gerrit.client.data.ChangeInfo;
import com.google.gerrit.client.data.GitwebLink;
import com.google.gerrit.client.reviewdb.Account;
import com.google.gerrit.client.reviewdb.Change;
import com.google.gerrit.client.reviewdb.ChangeMessage;
import com.google.gerrit.client.reviewdb.PatchSet;
import com.google.gerrit.client.rpc.GerritCallback;
import com.google.gerrit.client.rpc.ScreenLoadCallback;
import com.google.gerrit.client.ui.CommentPanel;
import com.google.gerrit.client.ui.ComplexDisclosurePanel;
import com.google.gerrit.client.ui.ExpandAllCommand;
import com.google.gerrit.client.ui.LinkMenuBar;
import com.google.gerrit.client.ui.NeedsSignInKeyCommand;
import com.google.gerrit.client.ui.RefreshListener;
import com.google.gerrit.client.ui.Screen;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwtexpui.globalkey.client.GlobalKey;
import com.google.gwtexpui.globalkey.client.KeyCommand;
import com.google.gwtexpui.globalkey.client.KeyCommandSet;
import com.google.gwtjsonrpc.client.VoidResult;

import java.sql.Timestamp;
import java.util.List;


public class ChangeScreen extends Screen {
  private final Change.Id changeId;

  private Image starChange;
  private boolean starred;
  private PatchSet.Id currentPatchSet;
  private ChangeDescriptionBlock descriptionBlock;
  private ApprovalTable approvals;

  private DisclosurePanel dependenciesPanel;
  private ChangeTable dependencies;
  private ChangeTable.Section dependsOn;
  private ChangeTable.Section neededBy;

  private FlowPanel patchSetPanels;

  private Panel comments;

  private KeyCommandSet keysNavigation;
  private KeyCommandSet keysAction;
  private HandlerRegistration regNavigation;
  private HandlerRegistration regAction;

  public ChangeScreen(final Change.Id toShow) {
    changeId = toShow;
  }

  public ChangeScreen(final ChangeInfo c) {
    this(c.getId());
  }

  @Override
  public void onSignOut() {
    super.onSignOut();
    if (starChange != null) {
      starChange.setVisible(false);
    }
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    refresh();
  }

  @Override
  protected void onUnload() {
    if (regNavigation != null) {
      regNavigation.removeHandler();
      regNavigation = null;
    }
    if (regAction != null) {
      regAction.removeHandler();
      regAction = null;
    }
    super.onUnload();
  }

  @Override
  public void registerKeys() {
    super.registerKeys();
    regNavigation = GlobalKey.add(this, keysNavigation);
    regAction = GlobalKey.add(this, keysAction);
  }

  public void refresh() {
    Util.DETAIL_SVC.changeDetail(changeId,
        new ScreenLoadCallback<ChangeDetail>(this) {
          @Override
          protected void preDisplay(final ChangeDetail r) {
            if (starChange != null) {
              setStarred(r.isStarred());
            }
            display(r);
          }
        });
  }

  private void setStarred(final boolean s) {
    if (s) {
      Gerrit.ICONS.starFilled().applyTo(starChange);
    } else {
      Gerrit.ICONS.starOpen().applyTo(starChange);
    }
    starred = s;
  }

  @Override
  protected void onInitUI() {
    super.onInitUI();
    addStyleName("gerrit-ChangeScreen");

    keysNavigation = new KeyCommandSet(Gerrit.C.sectionNavigation());
    keysAction = new KeyCommandSet(Gerrit.C.sectionActions());
    keysNavigation.add(new DashboardKeyCommand(0, 'u', Util.C.upToDashboard()));

    if (Gerrit.isSignedIn()) {
      keysAction.add(new StarKeyCommand(0, 's', Util.C.changeTableStar()));
      keysAction.add(new PublishCommentsKeyCommand(0, 'r', Util.C
          .keyPublishComments()));

      starChange = Gerrit.ICONS.starOpen().createImage();
      starChange.setStyleName("gerrit-ChangeScreen-StarIcon");
      starChange.setVisible(Gerrit.isSignedIn());
      starChange.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(final ClickEvent event) {
          toggleStar();
        }
      });
      insertTitleWidget(starChange);
    }

    descriptionBlock = new ChangeDescriptionBlock();
    add(descriptionBlock);

    approvals = new ApprovalTable();
    add(approvals);

    dependencies = new ChangeTable();
    dependsOn = new ChangeTable.Section(Util.C.changeScreenDependsOn());
    neededBy = new ChangeTable.Section(Util.C.changeScreenNeededBy());
    dependencies.addSection(dependsOn);
    dependencies.addSection(neededBy);

    dependenciesPanel = new DisclosurePanel(Util.C.changeScreenDependencies());
    dependenciesPanel.setContent(dependencies);
    dependenciesPanel.setWidth("95%");
    add(dependenciesPanel);

    patchSetPanels = new FlowPanel();
    add(patchSetPanels);

    comments = new FlowPanel();
    comments.setStyleName("gerrit-ChangeComments");
    add(comments);
  }

  private void displayTitle(final Change.Key changeId, final String subject) {
    final StringBuilder titleBuf = new StringBuilder();
    if (LocaleInfo.getCurrentLocale().isRTL()) {
      if (subject != null) {
        titleBuf.append(subject);
        titleBuf.append(" :");
      }
      titleBuf.append(Util.M.changeScreenTitleId(changeId.abbreviate()));
    } else {
      titleBuf.append(Util.M.changeScreenTitleId(changeId.abbreviate()));
      if (subject != null) {
        titleBuf.append(": ");
        titleBuf.append(subject);
      }
    }
    setPageTitle(titleBuf.toString());
  }

  private void display(final ChangeDetail detail) {
    displayTitle(detail.getChange().getKey(), detail.getChange().getSubject());

    dependencies.setAccountInfoCache(detail.getAccounts());
    approvals.setAccountInfoCache(detail.getAccounts());

    descriptionBlock.display(detail.getChange(), detail
        .getCurrentPatchSetDetail().getInfo(), detail.getAccounts());
    dependsOn.display(detail.getDependsOn());
    neededBy.display(detail.getNeededBy());
    approvals.display(detail.getChange(), detail.getMissingApprovals(), detail
        .getApprovals());

    addPatchSets(detail);
    addComments(detail);

    // If any dependency change is still open, show our dependency list.
    //
    boolean depsOpen = false;
    if (!detail.getChange().getStatus().isClosed()
        && detail.getDependsOn() != null) {
      for (final ChangeInfo ci : detail.getDependsOn()) {
        if (ci.getStatus() != Change.Status.MERGED) {
          depsOpen = true;
          break;
        }
      }
    }

    dependenciesPanel.setOpen(depsOpen);
  }

  private void addPatchSets(final ChangeDetail detail) {
    patchSetPanels.clear();

    final PatchSet currps = detail.getCurrentPatchSet();
    final GitwebLink gw = Gerrit.getConfig().getGitwebLink();
    for (final PatchSet ps : detail.getPatchSets()) {
      final ComplexDisclosurePanel panel =
          new ComplexDisclosurePanel(Util.M.patchSetHeader(ps.getPatchSetId()),
              ps == currps);
      final PatchSetPanel psp = new PatchSetPanel(detail, ps);
      panel.setContent(psp);

      final InlineLabel revtxt = new InlineLabel(ps.getRevision().get() + " ");
      revtxt.addStyleName("gerrit-PatchSetRevision");
      panel.getHeader().add(revtxt);
      if (gw != null) {
        final Anchor revlink =
            new Anchor("(gitweb)", false, gw.toRevision(detail.getChange()
                .getProject(), ps));
        revlink.addStyleName("gerrit-PatchSetLink");
        panel.getHeader().add(revlink);
      }

      if (ps == currps) {
        psp.ensureLoaded(detail.getCurrentPatchSetDetail());
        psp.addRefreshListener(new RefreshListener() {
          public void onSuggestRefresh() {
            refresh();
          }
        });
      } else {
        panel.addOpenHandler(psp);
      }
      add(panel);
      patchSetPanels.add(panel);
    }
    currentPatchSet = currps.getId();
  }

  private void addComments(final ChangeDetail detail) {
    comments.clear();

    final Label hdr = new Label(Util.C.changeScreenComments());
    hdr.setStyleName("gerrit-BlockHeader");
    comments.add(hdr);

    final AccountInfoCache accts = detail.getAccounts();
    final List<ChangeMessage> msgList = detail.getMessages();
    if (msgList.size() > 1) {
      comments.add(messagesMenuBar());
    }

    final long AGE = 7 * 24 * 60 * 60 * 1000L;
    final Timestamp aged = new Timestamp(System.currentTimeMillis() - AGE);

    for (int i = 0; i < msgList.size(); i++) {
      final ChangeMessage msg = msgList.get(i);

      final AccountInfo author;
      if (msg.getAuthor() != null) {
        author = accts.get(msg.getAuthor());
      } else {
        final Account gerrit = new Account(null);
        gerrit.setFullName(Util.C.messageNoAuthor());
        author = new AccountInfo(gerrit);
      }

      boolean isRecent;
      if (i == msgList.size() - 1) {
        isRecent = true;
      } else {
        // TODO Instead of opening messages by strict age, do it by "unread"?
        isRecent = msg.getWrittenOn().after(aged);
      }

      final CommentPanel cp =
          new CommentPanel(author, msg.getWrittenOn(), msg.getMessage());
      cp.setRecent(isRecent);
      if (i == msgList.size() - 1) {
        cp.addStyleName("gerrit-CommentPanel-Last");
        cp.setOpen(true);
      }
      comments.add(cp);
    }

    if (msgList.size() > 1) {
      comments.add(messagesMenuBar());
    }
    comments.setVisible(msgList.size() > 0);
  }

  private LinkMenuBar messagesMenuBar() {
    final Panel c = comments;
    final LinkMenuBar m = new LinkMenuBar();
    m.addItem(Util.C.messageExpandRecent(), new ExpandAllCommand(c, true) {
      @Override
      protected void expand(final CommentPanel w) {
        w.setOpen(w.isRecent());
      }
    });
    m.addItem(Util.C.messageExpandAll(), new ExpandAllCommand(c, true));
    m.addItem(Util.C.messageCollapseAll(), new ExpandAllCommand(c, false));
    return m;
  }

  private void toggleStar() {
    final boolean prior = starred;
    setStarred(!prior);

    final ToggleStarRequest req = new ToggleStarRequest();
    req.toggle(changeId, starred);
    Util.LIST_SVC.toggleStars(req, new GerritCallback<VoidResult>() {
      public void onSuccess(final VoidResult result) {
      }

      @Override
      public void onFailure(final Throwable caught) {
        super.onFailure(caught);
        setStarred(prior);
      }
    });
  }

  public class DashboardKeyCommand extends KeyCommand {
    public DashboardKeyCommand(int mask, char key, String help) {
      super(mask, key, help);
    }

    @Override
    public void onKeyPress(final KeyPressEvent event) {
      if (Gerrit.isSignedIn()) {
        Gerrit.display(Link.MINE, true);
      } else {
        Gerrit.display(Link.ALL_OPEN, true);
      }
    }
  }

  public class StarKeyCommand extends NeedsSignInKeyCommand {
    public StarKeyCommand(int mask, char key, String help) {
      super(mask, key, help);
    }

    @Override
    public void onKeyPress(final KeyPressEvent event) {
      toggleStar();
    }
  }

  public class PublishCommentsKeyCommand extends NeedsSignInKeyCommand {
    public PublishCommentsKeyCommand(int mask, char key, String help) {
      super(mask, key, help);
    }

    @Override
    public void onKeyPress(final KeyPressEvent event) {
      Gerrit.display("change,publish," + currentPatchSet.toString(),
          new PublishCommentScreen(currentPatchSet));
    }
  }
}
