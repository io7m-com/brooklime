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

import java.io.Closeable;
import java.util.List;
import java.util.Optional;

/**
 * A Nexus client.
 */

public interface BLNexusClientType extends Closeable
{
  /**
   * List the available staging repositories.
   *
   * @return The list of repositories
   *
   * @throws BLException On errors
   */

  List<BLStagingProfileRepository> stagingRepositories()
    throws BLException;

  /**
   * Retrieve an existing staging repository.
   *
   * @param id The ID of the repository
   *
   * @return The repository, or nothing if the repository does not exist
   *
   * @throws BLException On errors
   */

  Optional<BLStagingProfileRepository> stagingRepositoryGet(
    String id
  )
    throws BLException;

  /**
   * Create a staging repository.
   *
   * @param create The creation parameters
   *
   * @return The ID of the created repository
   *
   * @throws BLException On errors
   */

  String stagingRepositoryCreate(
    BLStagingRepositoryCreate create
  )
    throws BLException;

  /**
   * Drop one or more staging repositories.
   *
   * @param drop The repository parameters
   *
   * @throws BLException On errors
   */

  void stagingRepositoryDrop(
    BLStagingRepositoryDrop drop
  )
    throws BLException;

  /**
   * Close one or more staging repositories.
   *
   * @param close The repository parameters
   *
   * @throws BLException On errors
   */

  void stagingRepositoryClose(
    BLStagingRepositoryClose close
  )
    throws BLException;

  /**
   * Release one or more staging repositories.
   *
   * @param release The repository parameters
   *
   * @throws BLException On errors
   */

  void stagingRepositoryRelease(
    BLStagingRepositoryRelease release
  )
    throws BLException;
}
