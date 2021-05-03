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

import java.net.URI;
import java.time.OffsetDateTime;

/**
 * A description of a staging repository.
 */

@Value.Immutable
@BLImmutableStyleType
public interface BLStagingProfileRepositoryType
{
  /**
   * @return The staging profile ID
   */

  String profileId();

  /**
   * @return The staging profile name
   */

  String profileName();

  /**
   * @return The staging profile type
   */

  String profileType();

  /**
   * @return The staging repository ID
   */

  String repositoryId();

  /**
   * @return The type (?)
   */

  String type();

  /**
   * @return The staging profile policy
   */

  String policy();

  /**
   * @return The requesting user ID
   */

  String userId();

  /**
   * @return The requesting user agent
   */

  String userAgent();

  /**
   * @return The requesting IP address
   */

  String ipAddress();

  /**
   * @return The repository URI
   */

  URI repositoryURI();

  /**
   * @return The creation time of the repository
   */

  OffsetDateTime created();

  /**
   * @return The modification time of the repository
   */

  OffsetDateTime updated();

  /**
   * @return The repository description
   */

  String description();

  /**
   * @return The repository provider
   */

  String provider();

  /**
   * @return The release repository ID
   */

  String releaseRepositoryId();

  /**
   * @return The release repository name
   */

  String releaseRepositoryName();

  /**
   * @return The notifications, if any
   */

  String notifications();

  /**
   * @return {@code true} if the repository state is currently in transition
   */

  boolean transitioning();
}
