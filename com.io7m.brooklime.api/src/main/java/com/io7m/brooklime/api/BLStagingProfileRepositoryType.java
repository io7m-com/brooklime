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
  String profileId();

  String profileName();

  String profileType();

  String repositoryId();

  String type();

  String policy();

  String userId();

  String userAgent();

  String ipAddress();

  URI repositoryURI();

  OffsetDateTime created();

  OffsetDateTime updated();

  String description();

  String provider();

  String releaseRepositoryId();

  String releaseRepositoryName();

  String notifications();

  boolean transitioning();
}
