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

import com.google.gerrit.client.data.GerritConfig;
import com.google.gerrit.client.data.SystemInfoService;
import com.google.gerrit.client.reviewdb.Account;
import com.google.gerrit.client.reviewdb.AccountGeneralPreferences;
import com.google.gerrit.client.rpc.GerritCallback;
import com.google.gerrit.client.ui.LinkMenuBar;
import com.google.gerrit.client.ui.LinkMenuItem;
import com.google.gerrit.client.ui.Screen;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.Accessibility;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwtexpui.clippy.client.CopyableLabel;
import com.google.gwtexpui.user.client.UserAgent;
import com.google.gwtexpui.user.client.ViewSite;
import com.google.gwtjsonrpc.client.JsonUtil;

import java.util.ArrayList;

public class Gerrit implements EntryPoint {
  public static final GerritConstants C = GWT.create(GerritConstants.class);
  public static final GerritMessages M = GWT.create(GerritMessages.class);
  public static final GerritIcons ICONS = GWT.create(GerritIcons.class);
  public static final SystemInfoService SYSTEM_SVC;

  private static String myHost;
  private static String myVersion;
  private static GerritConfig myConfig;
  private static Account myAccount;

  private static TabPanel menuLeft;
  private static LinkMenuBar menuRight;
  private static RootPanel siteHeader;
  private static RootPanel siteFooter;
  private static SearchPanel searchPanel;
  private static ViewSite<Screen> body;

  static {
    SYSTEM_SVC = GWT.create(SystemInfoService.class);
    JsonUtil.bind(SYSTEM_SVC, "rpc/SystemInfoService");
  }

  public static void display(final String historyToken, final boolean go) {
    History.newItem(historyToken, go);
    if (!go && historyHooks != null) {
      dispatchHistoryHooks(historyToken);
    }
  }

  public static void display(final String historyToken, final Screen view) {
    History.newItem(historyToken, false);
    display(view);
    if (historyHooks != null) {
      dispatchHistoryHooks(historyToken);
    }
  }

  public static void display(final Screen view) {
    if (view.isRequiresSignIn() && !isSignedIn()) {
      doSignIn();
    } else {
      body.setView(view);
    }
  }

  public static void setQueryString(String query) {
    searchPanel.setText(query);
  }

  public static void setWindowTitle(final Screen screen, final String text) {
    if (screen == body.getView()) {
      if (text == null || text.length() == 0) {
        Window.setTitle(M.windowTitle1(myHost));
      } else {
        Window.setTitle(M.windowTitle2(text, myHost));
      }
    }
  }

  /** Get the public configuration data used by this Gerrit instance. */
  public static GerritConfig getConfig() {
    return myConfig;
  }

  /** @return the currently signed in user's account data; null if no account */
  public static Account getUserAccount() {
    return myAccount;
  }

  /** @return true if the user is currently authenticated */
  public static boolean isSignedIn() {
    return getUserAccount() != null;
  }

  /** Sign the user into the application. */
  public static void doSignIn() {
    switch (myConfig.getAuthType()) {
      case HTTP:
      case HTTP_LDAP:
        Location.assign("login/" + History.getToken());
        break;

      case DEVELOPMENT_BECOME_ANY_ACCOUNT:
        Location.assign("become");
        break;

      case OPENID:
      default:
        new SignInDialog().center();
        break;
    }
  }

  public void onModuleLoad() {
    UserAgent.assertNotInIFrame();
    initHostname();
    Window.setTitle(M.windowTitle1(myHost));
    initHistoryHooks();
    populateBottomMenu();

    final RootPanel menuArea = RootPanel.get("gerrit_topmenu");
    final Grid menuLine = new Grid(1, 3);
    menuLeft = new TabPanel();
    menuRight = new LinkMenuBar();
    searchPanel = new SearchPanel();
    menuLeft.setStyleName("gerrit-topmenu-menuLeft");
    menuLine.setStyleName("gerrit-topmenu");
    menuArea.add(menuLine);
    final FlowPanel menuRightPanel = new FlowPanel();
    menuRightPanel.setStyleName("gerrit-topmenu-menuRight");
    menuRightPanel.add(menuRight);
    menuRightPanel.add(searchPanel);
    menuLine.setWidget(0, 0, menuLeft);
    menuLine.setWidget(0, 1, new FlowPanel());
    menuLine.setWidget(0, 2, menuRightPanel);
    final CellFormatter fmt = menuLine.getCellFormatter();
    fmt.setStyleName(0, 0, "gerrit-topmenu-TDmenu");
    fmt.setStyleName(0, 1, "gerrit-topmenu-TDglue");
    fmt.setStyleName(0, 2, "gerrit-topmenu-TDmenu");

    siteHeader = RootPanel.get("gerrit_header");
    siteFooter = RootPanel.get("gerrit_footer");

    body = new ViewSite<Screen>() {
      @Override
      protected void onShowView(Screen view) {
        super.onShowView(view);
        view.onShowView();
      }
    };
    RootPanel.get("gerrit_body").add(body);

    final RpcStatus rpcStatus = new RpcStatus(menuArea);
    JsonUtil.addRpcStartHandler(rpcStatus);
    JsonUtil.addRpcCompleteHandler(rpcStatus);

    final HostPageDataService hpd = GWT.create(HostPageDataService.class);
    hpd.load(new GerritCallback<HostPageData>() {
      public void onSuccess(final HostPageData result) {
        myConfig = result.config;
        if (result.xsrfToken != null) {
          JsonUtil.getDefaultXsrfManager().setToken(null, result.xsrfToken);
        }
        if (result.userAccount != null) {
          myAccount = result.userAccount;
          applyUserPreferences();
        }
        onModuleLoad2();
      }
    });
  }

  private static void initHostname() {
    myHost = Window.Location.getHostName();
    final int d1 = myHost.indexOf('.');
    if (d1 < 0) {
      return;
    }
    final int d2 = myHost.indexOf('.', d1 + 1);
    if (d2 >= 0) {
      myHost = myHost.substring(0, d2);
    }
  }

  private static ArrayList<JavaScriptObject> historyHooks;

  private static native void initHistoryHooks()
  /*-{ $wnd['gerrit_addHistoryHook'] = function(h) { @com.google.gerrit.client.Gerrit::addHistoryHook(Lcom/google/gwt/core/client/JavaScriptObject;)(h); }; }-*/;

  static void addHistoryHook(final JavaScriptObject hook) {
    if (historyHooks == null) {
      historyHooks = new ArrayList<JavaScriptObject>();
      History.addValueChangeHandler(new ValueChangeHandler<String>() {
        @Override
        public void onValueChange(ValueChangeEvent<String> event) {
          dispatchHistoryHooks(event.getValue());
        }
      });
    }
    historyHooks.add(hook);
  }

  private static native void callHistoryHook(JavaScriptObject hook, String url)
  /*-{ hook(url); }-*/;

  private static void dispatchHistoryHooks(final String historyToken) {
    final String url = Window.Location.getPath() + "#" + historyToken;
    for (final JavaScriptObject hook : historyHooks) {
      callHistoryHook(hook, url);
    }
  }

  private static void populateBottomMenu() {
    final RootPanel btmmenu = RootPanel.get("gerrit_btmmenu");

    final Label keyHelp = new Label(C.keyHelp());
    keyHelp.setStyleName("gerrit-keyhelp");
    btmmenu.add(keyHelp);

    final String vs = getVersion();
    final HTML version = new HTML(M.poweredBy(vs));
    version.setStyleName("gerrit-version");
    btmmenu.add(version);
  }

  /** @return version number of the Gerrit application software */
  public static String getVersion() {
    if (myVersion == null) {
      if (GWT.isScript()) {
        final GerritVersion v = GWT.create(GerritVersion.class);
        myVersion = v.version();
      } else {
        myVersion = "dev";
      }
    }
    return myVersion;
  }

  private void onModuleLoad2() {

    refreshMenuBar();

    final RootPanel sg = RootPanel.get("gerrit_startinggerrit");
    sg.getElement().getParentElement().removeChild(sg.getElement());
    RootPanel.detachNow(sg);

    History.addValueChangeHandler(new Link());
    JumpKeys.register(body);

    if ("".equals(History.getToken())) {
      if (isSignedIn()) {
        History.newItem(Link.MINE);
      } else {
        History.newItem(Link.ALL_OPEN);
      }
    } else {
      History.fireCurrentHistoryState();
    }
  }

  public static void refreshMenuBar() {
    menuLeft.clear();
    menuRight.clear();

    final boolean signedIn = isSignedIn();
    LinkMenuBar m;

    m = new LinkMenuBar();
    addLink(m, C.menuAllOpen(), Link.ALL_OPEN);
    addLink(m, C.menuAllMerged(), Link.ALL_MERGED);
    addLink(m, C.menuAllAbandoned(), Link.ALL_ABANDONED);
    menuLeft.add(m, C.menuAll());

    if (signedIn) {
      m = new LinkMenuBar();
      addLink(m, C.menuMyChanges(), Link.MINE);
      addLink(m, C.menyMyDrafts(), Link.MINE_DRAFTS);
      addLink(m, C.menuMyStarredChanges(), Link.MINE_STARRED);
      menuLeft.add(m, C.menuMine());
      menuLeft.selectTab(1);
    } else {
      menuLeft.selectTab(0);
    }

    if (signedIn) {
      m = new LinkMenuBar();
      addLink(m, C.menuGroups(), Link.ADMIN_GROUPS);
      addLink(m, C.menuProjects(), Link.ADMIN_PROJECTS);
      menuLeft.add(m, C.menuAdmin());
    }

    if (signedIn) {
      whoAmI();
      addLink(menuRight, C.menuSettings(), Link.SETTINGS);
      menuRight.add(anchor(C.menuSignOut(), "logout"));
    } else {
      switch (getConfig().getAuthType()) {
        case HTTP:
        case HTTP_LDAP:
          break;

        case OPENID:
          menuRight.addItem(C.menuRegister(), new Command() {
            public void execute() {
              new SignInDialog(SignInDialog.Mode.REGISTER).center();
            }
          });
          menuRight.addItem(C.menuSignIn(), new Command() {
            public void execute() {
              doSignIn();
            }
          });
          break;

        case DEVELOPMENT_BECOME_ANY_ACCOUNT:
          menuRight.add(anchor("Become", "become"));
          break;
      }
    }
  }

  public static void applyUserPreferences() {
    final AccountGeneralPreferences p = myAccount.getGeneralPreferences();
    CopyableLabel.setFlashEnabled(p.isUseFlashClipboard());
    if (siteHeader != null) {
      siteHeader.setVisible(p.isShowSiteHeader());
    }
    if (siteFooter != null) {
      siteFooter.setVisible(p.isShowSiteHeader());
    }
  }

  private static void whoAmI() {
    final String name = FormatUtil.nameEmail(getUserAccount());
    final InlineLabel l = new InlineLabel(name);
    l.setStyleName("gerrit-MenuBarUserName");
    menuRight.add(l);
  }

  private static Anchor anchor(final String text, final String to) {
    final Anchor a = new Anchor(text, to);
    a.setStyleName("gerrit-MenuItem");
    Accessibility.setRole(a.getElement(), Accessibility.ROLE_MENUITEM);
    return a;
  }

  private static void addLink(final LinkMenuBar m, final String text,
      final String historyToken) {
    m.addItem(new LinkMenuItem(text, historyToken));
  }
}
