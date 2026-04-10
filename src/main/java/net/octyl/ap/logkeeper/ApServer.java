/*
 * SPDX-FileCopyrightText: Octavia Togami <octy@octyl.net>
 * SPDX-License-Identifier: MPL-2.0
 */

package net.octyl.ap.logkeeper;

import picocli.CommandLine;

/**
 * Represents an Archipelago server to connect to.
 *
 * @param hostname the hostname or IP address of the server
 * @param port the port number of the server
 */
public record ApServer(
    String hostname,
    int port
) {
    static final class ApServerConverter implements CommandLine.ITypeConverter<ApServer> {
        @Override
        public ApServer convert(String value) {
            if (value.startsWith("[")) {
                return parseIpv6Address(value);
            } else {
                return parseOther(value);
            }
        }

        private ApServer parseIpv6Address(String value) {
            int closeBracket = value.indexOf(']');
            if (closeBracket == -1) {
                throw new CommandLine.TypeConversionException("Invalid server address: " + value);
            }
            String hostname = value.substring(1, closeBracket);
            String remainder = value.substring(closeBracket + 1);
            if (!remainder.startsWith(":")) {
                throw new CommandLine.TypeConversionException("Invalid server address: " + value);
            }
            try {
                int port = Integer.parseInt(remainder.substring(1));
                return new ApServer(hostname, port);
            } catch (NumberFormatException e) {
                throw new CommandLine.TypeConversionException("Invalid port number in server address: " + value);
            }
        }

        private ApServer parseOther(String value) {
            String[] parts = value.split(":");
            if (parts.length != 2) {
                throw new CommandLine.TypeConversionException("Invalid server address: " + value);
            }
            String hostname = parts[0];
            try {
                int port = Integer.parseInt(parts[1]);
                return new ApServer(hostname, port);
            } catch (NumberFormatException e) {
                throw new CommandLine.TypeConversionException("Invalid port number in server address: " + value);
            }
        }
    }
}
