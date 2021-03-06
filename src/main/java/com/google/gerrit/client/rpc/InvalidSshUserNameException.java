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

package com.google.gerrit.client.rpc;

import com.google.gerrit.client.reviewdb.Account;

/** Error indicating the SSH user name does not match {@link Account#SSH_USER_NAME_PATTERN} pattern. */
public class InvalidSshUserNameException extends Exception {

  private static final long serialVersionUID = 1L;

  public static final String MESSAGE = "Invalid SSH user name.";

  public InvalidSshUserNameException() {
    super(MESSAGE);
  }

}
