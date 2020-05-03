/*
 * Copyright © 2020 Mark Raynsford <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.brooklime.api;

import org.immutables.value.Value;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * A request to upload a set of files to a staging repository.
 */

@BLImmutableStyleType
@Value.Immutable
public interface BLStagingRepositoryUploadType
{
  /**
   * @return The ID of the staging repository
   */

  String repositoryId();

  /**
   * @return The base directory containing files
   */

  Path baseDirectory();

  /**
   * @return The files to be uploaded
   */

  List<Path> files();

  /**
   * @return The duration by which to pause between failed upload attempts
   */

  Duration retryDelay();

  /**
   * @return The maximum number of times to retry uploading any given file
   */

  int retryCount();

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    if (!this.baseDirectory().isAbsolute()) {
      throw new IllegalArgumentException(
        "The base directory path must be absolute");
    }

    for (final Path file: this.files()) {
      if (file.isAbsolute()) {
        throw new IllegalArgumentException(String.format(
          "All file paths must be relative. %s is absolute.",
          file));
      }
    }
  }
}
