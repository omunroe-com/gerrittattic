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

package com.google.gerrit.server.patch;


import static com.google.gerrit.server.ioutil.BasicSerialization.readBytes;
import static com.google.gerrit.server.ioutil.BasicSerialization.readVarInt32;
import static com.google.gerrit.server.ioutil.BasicSerialization.writeBytes;
import static com.google.gerrit.server.ioutil.BasicSerialization.writeVarInt32;
import static org.spearce.jgit.lib.ObjectIdSerialization.readCanBeNull;
import static org.spearce.jgit.lib.ObjectIdSerialization.readNotNull;
import static org.spearce.jgit.lib.ObjectIdSerialization.writeCanBeNull;
import static org.spearce.jgit.lib.ObjectIdSerialization.writeNotNull;

import com.google.gerrit.client.reviewdb.Patch;
import com.google.gerrit.client.reviewdb.PatchSet;
import com.google.gerrit.server.config.Nullable;

import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.lib.ObjectId;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class PatchList implements Serializable {
  private static final long serialVersionUID = PatchListKey.serialVersionUID;
  private static final Comparator<PatchListEntry> PATCH_CMP =
      new Comparator<PatchListEntry>() {
        @Override
        public int compare(final PatchListEntry a, final PatchListEntry b) {
          return a.getNewName().compareTo(b.getNewName());
        }
      };

  @Nullable
  private transient ObjectId oldId;
  private transient ObjectId newId;
  private transient PatchListEntry[] patches;

  PatchList(@Nullable final AnyObjectId oldId, final AnyObjectId newId,
      final PatchListEntry[] patches) {
    this.oldId = oldId != null ? oldId.copy() : null;
    this.newId = newId.copy();

    Arrays.sort(patches, PATCH_CMP);
    this.patches = patches;
  }

  /** Old side tree or commit; null only if this is a combined diff. */
  @Nullable
  public ObjectId getOldId() {
    return oldId;
  }

  /** New side commit. */
  public ObjectId getNewId() {
    return newId;
  }

  /** Get a sorted, unmodifiable list of all files in this list. */
  public List<PatchListEntry> getPatches() {
    return Collections.unmodifiableList(Arrays.asList(patches));
  }

  /**
   * Get a sorted, modifiable list of all files in this list.
   * <p>
   * The returned list items do not populate:
   * <ul>
   * <li>{@link Patch#getCommentCount()}
   * <li>{@link Patch#getDraftCount()}
   * <li>{@link Patch#isReviewedByCurrentUser()}
   * </ul>
   *
   * @param setId the patch set identity these patches belong to. This really
   *        should not need to be specified, but is a current legacy artifact of
   *        how the cache is keyed versus how the database is keyed.
   */
  public List<Patch> toPatchList(final PatchSet.Id setId) {
    final ArrayList<Patch> r = new ArrayList<Patch>(patches.length);
    for (final PatchListEntry e : patches) {
      r.add(e.toPatch(setId));
    }
    return r;
  }

  /** Find an entry by name, returning an empty entry if not present. */
  public PatchListEntry get(final String fileName) {
    final int index = search(fileName);
    return 0 <= index ? patches[index] : PatchListEntry.empty(fileName);
  }

  private int search(final String fileName) {
    int high = patches.length;
    int low = 0;
    while (low < high) {
      final int mid = (low + high) >>> 1;
      final int cmp = patches[mid].getNewName().compareTo(fileName);
      if (cmp < 0)
        low = mid + 1;
      else if (cmp == 0)
        return mid;
      else
        high = mid;
    }
    return -(low + 1);
  }

  private void writeObject(final ObjectOutputStream output) throws IOException {
    final ByteArrayOutputStream buf = new ByteArrayOutputStream();
    final DeflaterOutputStream out = new DeflaterOutputStream(buf);
    try {
      writeCanBeNull(out, oldId);
      writeNotNull(out, newId);
      writeVarInt32(out, patches.length);
      for (PatchListEntry p : patches) {
        p.writeTo(out);
      }
    } finally {
      out.close();
    }
    writeBytes(output, buf.toByteArray());
  }

  private void readObject(final ObjectInputStream input) throws IOException {
    final ByteArrayInputStream buf = new ByteArrayInputStream(readBytes(input));
    final InflaterInputStream in = new InflaterInputStream(buf);
    try {
      oldId = readCanBeNull(in);
      newId = readNotNull(in);
      final int cnt = readVarInt32(in);
      final PatchListEntry[] all = new PatchListEntry[cnt];
      for (int i = 0; i < all.length; i++) {
        all[i] = PatchListEntry.readFrom(in);
      }
      patches = all;
    } finally {
      in.close();
    }
  }
}
