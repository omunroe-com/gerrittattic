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

package com.google.gerrit.server.mail;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

abstract class EmailHeader {
  abstract boolean isEmpty();

  abstract void write(Writer w) throws IOException;

  static class String extends EmailHeader {
    private java.lang.String value;

    String(java.lang.String v) {
      value = v;
    }

    @Override
    boolean isEmpty() {
      return value == null || value.length() == 0;
    }

    @Override
    void write(Writer w) throws IOException {
      w.write(value);
    }
  }

  static class Date extends EmailHeader {
    private java.util.Date value;

    Date(java.util.Date v) {
      value = v;
    }

    @Override
    boolean isEmpty() {
      return value == null;
    }

    @Override
    void write(Writer w) throws IOException {
      final SimpleDateFormat fmt;
      // Mon, 1 Jun 2009 10:49:44 -0700
      fmt = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
      w.write(fmt.format(value));
    }
  }

  static class AddressList extends EmailHeader {
    private final List<Address> list = new ArrayList<Address>();

    AddressList() {
    }

    AddressList(Address addr) {
      add(addr);
    }

    void add(Address addr) {
      list.add(addr);
    }

    @Override
    boolean isEmpty() {
      return list.isEmpty();
    }

    @Override
    void write(Writer w) throws IOException {
      int len = 8;
      boolean firstAddress = true;
      boolean needComma = false;
      for (final Address addr : list) {
        java.lang.String s = addr.toHeaderString();
        if (firstAddress) {
          firstAddress = false;
        } else if (72 < len + s.length()) {
          w.write(",\r\n\t");
          len = 8;
          needComma = false;
        }

        if (needComma) {
          w.write(", ");
        }
        w.write(s);
        len += s.length();
        needComma = true;
      }
    }
  }
}
