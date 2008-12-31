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

package com.google.gerrit.server.ssh;

import com.google.gerrit.client.reviewdb.AccountSshKey;
import com.google.gerrit.client.reviewdb.ReviewDb;
import com.google.gwtorm.client.OrmException;
import com.google.gwtorm.client.SchemaFactory;

import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.List;

/**
 * Authenticates by public key through {@link AccountSshKey} entities.
 * <p>
 * The username supplied by the client must be the user's preferred email
 * address, as listed in their Account entity. Only keys listed under that
 * account as authorized keys are permitted to access the account.
 */
class DatabasePubKeyAuth implements PublickeyAuthenticator {
  private final SchemaFactory<ReviewDb> schema;

  DatabasePubKeyAuth(final SchemaFactory<ReviewDb> rdf) {
    schema = rdf;
  }

  public boolean hasKey(final String username, final PublicKey inkey,
      final ServerSession session) {
    final List<AccountSshKey> keyList = SshUtil.keysFor(schema, username);
    for (final AccountSshKey k : keyList) {
      try {
        if (SshUtil.parse(k).equals(inkey)) {
          updateLastUsed(k);
          session.setAttribute(SshUtil.CURRENT_ACCOUNT, k.getAccount());
          return true;
        }
      } catch (NoSuchAlgorithmException e) {
        markInvalid(k);
      } catch (InvalidKeySpecException e) {
        markInvalid(k);
      } catch (NoSuchProviderException e) {
        markInvalid(k);
      } catch (RuntimeException e) {
        markInvalid(k);
      }
    }
    return false;
  }

  private void markInvalid(final AccountSshKey k) {
    try {
      final ReviewDb db = schema.open();
      try {
        k.setInvalid();
        db.accountSshKeys().update(Collections.singleton(k));
      } finally {
        db.close();
      }
    } catch (OrmException e) {
      // TODO log mark invalid failure
    }
  }

  private void updateLastUsed(final AccountSshKey k) {
    try {
      final ReviewDb db = schema.open();
      try {
        k.setLastUsedOn();
        db.accountSshKeys().update(Collections.singleton(k));
      } finally {
        db.close();
      }
    } catch (OrmException e) {
      // TODO log update last used failure
    }
  }
}