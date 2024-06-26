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

import com.beust.jcommander.Parameters;
import com.io7m.brooklime.api.BLApplicationVersion;
import com.io7m.brooklime.api.BLException;

import java.io.IOException;

/**
 * The "version" command.
 */

@Parameters(commandDescription = "Show the application version")
public final class BLCommandVersion extends BLCommandRoot
{
  /**
   * The "version" command.
   */

  public BLCommandVersion()
  {

  }

  @Override
  public Status execute()
    throws BLException, IOException
  {
    if (super.execute() == Status.FAILURE) {
      return Status.FAILURE;
    }

    final BLApplicationVersion version =
      BLServices.findApplicationVersion();

    System.out.printf(
      "%s %s\n",
      version.applicationName(),
      version.applicationVersion()
    );
    return Status.SUCCESS;
  }
}
