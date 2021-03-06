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

import com.google.gwtorm.client.Column;
import com.google.gwtorm.client.StringKey;

/** Global configuration needed to serve web requests. */
public final class SystemConfig {
  public static final class Key extends
      StringKey<com.google.gwtorm.client.Key<?>> {
    private static final long serialVersionUID = 1L;

    private static final String VALUE = "X";

    @Column(length = 1)
    protected String one = VALUE;

    public Key() {
    }

    @Override
    public String get() {
      return VALUE;
    }

    @Override
    protected void set(final String newValue) {
      assert get().equals(newValue);
    }
  }

  /** Construct a new, unconfigured instance. */
  public static SystemConfig create() {
    final SystemConfig r = new SystemConfig();
    r.singleton = new SystemConfig.Key();
    return r;
  }

  @Column
  protected Key singleton;

  /** Private key to sign account identification cookies. */
  @Column(length = 36)
  public transient String registerEmailPrivateKey;

  /**
   * Local filesystem location of header/footer/CSS configuration files
   */
  @Column(notNull = false)
  public transient String sitePath;

  /** Identity of the administration group; those with full access. */
  @Column
  public AccountGroup.Id adminGroupId;

  /** Identity of the anonymous group, which permits anyone. */
  @Column
  public AccountGroup.Id anonymousGroupId;

  /** Identity of the registered users group, which permits anyone. */
  @Column
  public AccountGroup.Id registeredGroupId;

  protected SystemConfig() {
  }
}
