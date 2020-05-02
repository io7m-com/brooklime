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

package com.io7m.brooklime.vanilla.internal;

import com.io7m.brooklime.api.BLParseException;
import com.io7m.brooklime.api.BLStagingProfileRepository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class BLNexusParsers
{

  public BLNexusParsers()
  {

  }

  private static List<Element> requireChildElements(
    final URI source,
    final Element element,
    final String childElement)
    throws BLParseException
  {
    final List<Element> childElements =
      optionalChildElements(element, childElement);

    if (childElements.isEmpty()) {
      final BLPositionalXML.BLPosition position =
        BLPositionalXML.lexicalOf(element);
      throw new BLParseException(
        String.format(
          "Expected at least one element '%s' as a child of '%s'",
          childElement,
          element.getLocalName()
        ),
        position.line(),
        position.column(),
        source
      );
    }

    return childElements;
  }

  private static List<Element> optionalChildElements(
    final Element element,
    final String childElement)
  {
    final NodeList childElements =
      element.getElementsByTagName(childElement);

    final ArrayList<Element> elements =
      new ArrayList<>(childElements.getLength());
    for (int index = 0, size = childElements.getLength(); index < size; ++index) {
      elements.add((Element) childElements.item(index));
    }
    return elements;
  }

  private static Element requireChildElement(
    final URI source,
    final Element element,
    final String childElement)
    throws BLParseException
  {
    final NodeList childElements =
      element.getElementsByTagName(childElement);

    if (childElements.getLength() == 0) {
      final BLPositionalXML.BLPosition position =
        BLPositionalXML.lexicalOf(element);
      throw new BLParseException(
        String.format(
          "Expected an element '%s' as a child of '%s'",
          childElement,
          element.getLocalName()
        ),
        position.line(),
        position.column(),
        source
      );
    }

    return (Element) childElements.item(0);
  }

  private static void checkIsElement(
    final URI source,
    final Element element,
    final String expectedName)
    throws BLParseException
  {
    if (Objects.equals(element.getLocalName(), expectedName)) {
      final BLPositionalXML.BLPosition position =
        BLPositionalXML.lexicalOf(element);
      throw new BLParseException(
        String.format(
          "Expected an element '%s' but received '%s'",
          expectedName,
          element.getLocalName()
        ),
        position.line(),
        position.column(),
        source
      );
    }
  }

  public String parseStagingRepositoryCreate(
    final URI uri,
    final InputStream stream)
    throws BLParseException
  {
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(stream, "stream");

    try {
      final Document document = BLPositionalXML.readXML(uri, stream);
      final Element root = document.getDocumentElement();
      checkIsElement(uri, root, "promoteResponse");

      final Element data =
        requireChildElement(uri, root, "data");
      final Element stagedRepositoryId =
        requireChildElement(uri, data, "stagedRepositoryId");

      return stagedRepositoryId.getTextContent();
    } catch (final SAXParseException e) {
      throw new BLParseException(
        e.getMessage(),
        e,
        e.getLineNumber(),
        e.getColumnNumber(),
        uri
      );
    } catch (final IOException | SAXException | ParserConfigurationException e) {
      throw new BLParseException(e.getMessage(), e, -1, -1, uri);
    }
  }

  public List<BLStagingProfileRepository> parseRepositories(
    final URI uri,
    final InputStream stream)
    throws BLParseException
  {
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(stream, "stream");

    try {
      final Document document = BLPositionalXML.readXML(uri, stream);
      final Element root = document.getDocumentElement();
      checkIsElement(uri, root, "stagingRepositories");

      final Element data =
        requireChildElement(uri, root, "data");
      final List<Element> repositoryElements =
        optionalChildElements(data, "stagingProfileRepository");

      final List<BLStagingProfileRepository> repositories =
        new ArrayList<>(repositoryElements.size());
      for (final Element repositoryElement : repositoryElements) {
        repositories.add(this.parseRepository(uri, repositoryElement));
      }

      return Collections.unmodifiableList(repositories);
    } catch (final SAXParseException e) {
      throw new BLParseException(
        e.getMessage(),
        e,
        e.getLineNumber(),
        e.getColumnNumber(),
        uri
      );
    } catch (final IOException | SAXException | ParserConfigurationException e) {
      throw new BLParseException(e.getMessage(), e, -1, -1, uri);
    }
  }

  public BLStagingProfileRepository parseRepository(
    final URI uri,
    final InputStream stream)
    throws BLParseException
  {
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(stream, "stream");

    try {
      final Document document = BLPositionalXML.readXML(uri, stream);
      final Element root = document.getDocumentElement();
      return this.parseRepository(uri, root);
    } catch (final SAXParseException e) {
      throw new BLParseException(
        e.getMessage(),
        e,
        e.getLineNumber(),
        e.getColumnNumber(),
        uri
      );
    } catch (final IOException | SAXException | ParserConfigurationException e) {
      throw new BLParseException(e.getMessage(), e, -1, -1, uri);
    }
  }

  private BLStagingProfileRepository parseRepository(
    final URI uri,
    final Element repositoryElement)
    throws BLParseException
  {
    final BLStagingProfileRepository.Builder builder =
      BLStagingProfileRepository.builder();

    builder.setCreated(
      OffsetDateTime.from(
        DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(
          requireChildElement(uri, repositoryElement, "created")
            .getTextContent()
        )
      )
    );
    builder.setDescription(
      requireChildElement(uri, repositoryElement, "description")
        .getTextContent()
    );
    builder.setIpAddress(
      requireChildElement(uri, repositoryElement, "ipAddress")
        .getTextContent()
    );
    builder.setNotifications(
      requireChildElement(uri, repositoryElement, "notifications")
        .getTextContent()
    );
    builder.setPolicy(
      requireChildElement(uri, repositoryElement, "policy")
        .getTextContent()
    );
    builder.setProfileId(
      requireChildElement(uri, repositoryElement, "profileId")
        .getTextContent()
    );
    builder.setProfileName(
      requireChildElement(uri, repositoryElement, "profileName")
        .getTextContent()
    );
    builder.setProfileType(
      requireChildElement(uri, repositoryElement, "profileType")
        .getTextContent()
    );
    builder.setProvider(
      requireChildElement(uri, repositoryElement, "provider")
        .getTextContent()
    );
    builder.setRepositoryId(
      requireChildElement(uri, repositoryElement, "repositoryId")
        .getTextContent()
    );
    builder.setReleaseRepositoryId(
      requireChildElement(uri, repositoryElement, "releaseRepositoryId")
        .getTextContent()
    );
    builder.setReleaseRepositoryName(
      requireChildElement(uri, repositoryElement, "releaseRepositoryName")
        .getTextContent()
    );
    builder.setTransitioning(
      Boolean.valueOf(
        requireChildElement(uri, repositoryElement, "transitioning")
          .getTextContent()
      ).booleanValue()
    );
    builder.setType(
      requireChildElement(uri, repositoryElement, "type")
        .getTextContent()
    );
    builder.setUserId(
      requireChildElement(uri, repositoryElement, "userId")
        .getTextContent()
    );
    builder.setUserAgent(
      requireChildElement(uri, repositoryElement, "userAgent")
        .getTextContent()
    );
    builder.setRepositoryURI(
      URI.create(
        requireChildElement(uri, repositoryElement, "repositoryURI")
          .getTextContent()
      )
    );
    builder.setUpdated(
      OffsetDateTime.from(
        DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(
          requireChildElement(uri, repositoryElement, "updated")
            .getTextContent()
        )
      )
    );

    return builder.build();
  }
}
