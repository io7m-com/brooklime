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

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/**
 * Functions relating to application versions.
 */

public final class BLApplicationVersions
{
  private BLApplicationVersions()
  {

  }

  /**
   * Load application version information from the given stream.
   *
   * @param stream The stream
   *
   * @return Application version information
   *
   * @throws IOException On errors
   */

  public static BLApplicationVersion ofStream(
    final InputStream stream)
    throws IOException
  {
    Objects.requireNonNull(stream, "stream");

    final Properties properties = new Properties();
    properties.load(stream);
    return ofProperties(properties);
  }

  /**
   * Load application version information from the given stream.
   *
   * @param properties The properties
   *
   * @return Application version information
   */

  public static BLApplicationVersion ofProperties(
    final Properties properties)
  {
    Objects.requireNonNull(properties, "properties");

    final String name =
      properties.getProperty("applicationName");
    final String version =
      properties.getProperty("applicationVersion");

    if (name != null && version != null) {
      return BLApplicationVersion.builder()
        .setApplicationName(name)
        .setApplicationVersion(version)
        .build();
    }

    throw new IllegalArgumentException(
      "Must specify applicationName and applicationVersion fields"
    );
  }
}
