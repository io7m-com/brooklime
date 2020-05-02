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

package com.io7m.brooklime.cmdline;

import java.io.IOException;

/**
 * Main command line entry point that does not call {@code exit()}.
 */

public final class MainExitless
{
  private MainExitless()
  {

  }

  /**
   * The main entry point.
   *
   * @param args Command line arguments
   *
   * @throws IOException On errors
   */

  // CHECKSTYLE:OFF
  public static void main(
    final String[] args)
    throws IOException
  {
    // CHECKSTYLE:ON
    final Main cm = new Main(args);
    cm.run();

    if (cm.exitCode() != 0) {
      throw new IOException("Returned exit code " + cm.exitCode());
    }
  }
}
