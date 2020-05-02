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

package com.io7m.brooklime.cmdline.internal;

import com.io7m.brooklime.api.BLApplicationVersion;
import com.io7m.brooklime.api.BLApplicationVersions;
import com.io7m.brooklime.api.BLNexusClientProviderType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.ServiceLoader;

final class BLServices
{
  private BLServices()
  {

  }

  public static BLApplicationVersion findApplicationVersion()
    throws IOException
  {
    final URL resource =
      BLServices.class.getResource(
        "/com/io7m/brooklime/cmdline/version.properties"
      );

    try (InputStream stream = resource.openStream()) {
      return BLApplicationVersions.ofStream(stream);
    }
  }

  public static BLNexusClientProviderType findClients()
  {
    final ServiceLoader<BLNexusClientProviderType> loader =
      ServiceLoader.load(BLNexusClientProviderType.class);
    final Iterator<BLNexusClientProviderType> serviceIter =
      loader.iterator();

    while (serviceIter.hasNext()) {
      return serviceIter.next();
    }

    throw new IllegalStateException(noServicesMessage());
  }

  private static String noServicesMessage()
  {
    return String.format(
      "No available services of type: %s",
      BLNexusClientProviderType.class.getCanonicalName());
  }
}
