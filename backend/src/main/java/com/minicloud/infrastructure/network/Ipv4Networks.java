package com.minicloud.infrastructure.network;

import com.minicloud.common.exception.InvalidNetworkConfigurationException;

/**
 * IPv4 helpers for libvirt NAT networks. No invented addresses — only validation.
 */
public final class Ipv4Networks {

    private static final Block RFC1918_10 = parseCidrUnchecked("10.0.0.0/8");
    private static final Block RFC1918_172 = parseCidrUnchecked("172.16.0.0/12");
    private static final Block RFC1918_192 = parseCidrUnchecked("192.168.0.0/16");

    private Ipv4Networks() {
    }

    public record Block(int network, int prefix, int mask) {
        public boolean contains(int ip) {
            return (ip & mask) == network;
        }

        public int broadcast() {
            return network | ~mask;
        }

        public String cidrString() {
            return formatIp(network) + "/" + prefix;
        }

        public String netmaskString() {
            return formatIp(mask);
        }

        public boolean overlaps(Block other) {
            return Integer.compareUnsigned(network, other.broadcast()) <= 0
                    && Integer.compareUnsigned(other.network, broadcast()) <= 0;
        }
    }

    public static Block parseCidr(String cidr) {
        Block block = parseCidrUnchecked(cidr);
        if (block.prefix() < 16 || block.prefix() > 29) {
            throw new InvalidNetworkConfigurationException("CIDR prefix must be between /16 and /29");
        }
        return block;
    }

    static Block parseCidrUnchecked(String cidr) {
        if (cidr == null || !cidr.contains("/")) {
            throw new InvalidNetworkConfigurationException("CIDR must look like 192.168.100.0/24");
        }
        String[] parts = cidr.trim().split("/");
        if (parts.length != 2) {
            throw new InvalidNetworkConfigurationException("CIDR must look like 192.168.100.0/24");
        }
        int prefix;
        try {
            prefix = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ex) {
            throw new InvalidNetworkConfigurationException("CIDR prefix must be an integer");
        }
        if (prefix < 0 || prefix > 32) {
            throw new InvalidNetworkConfigurationException("CIDR prefix must be between 0 and 32");
        }
        int address = parseIp(parts[0]);
        int mask = prefix == 0 ? 0 : (int) (0xFFFFFFFFL << (32 - prefix));
        int network = address & mask;
        return new Block(network, prefix, mask);
    }

    public static int parseIp(String value) {
        if (value == null) {
            throw new InvalidNetworkConfigurationException("IPv4 address is required");
        }
        String[] parts = value.trim().split("\\.");
        if (parts.length != 4) {
            throw new InvalidNetworkConfigurationException("Invalid IPv4 address: " + value);
        }
        int ip = 0;
        try {
            for (String part : parts) {
                int octet = Integer.parseInt(part);
                if (octet < 0 || octet > 255) {
                    throw new InvalidNetworkConfigurationException("Invalid IPv4 address: " + value);
                }
                ip = (ip << 8) | octet;
            }
        } catch (NumberFormatException ex) {
            throw new InvalidNetworkConfigurationException("Invalid IPv4 address: " + value);
        }
        return ip;
    }

    public static String formatIp(int ip) {
        return ((ip >>> 24) & 0xFF) + "." + ((ip >>> 16) & 0xFF) + "." + ((ip >>> 8) & 0xFF) + "." + (ip & 0xFF);
    }

    public static String cidrFromGatewayAndMask(String gateway, String netmask) {
        if (gateway == null || netmask == null) {
            return null;
        }
        int ip = parseIp(gateway);
        int mask = parseIp(netmask);
        int prefix = Integer.bitCount(mask);
        int network = ip & mask;
        return formatIp(network) + "/" + prefix;
    }

    public static void validateCreate(String cidr, String gateway, String dhcpStart, String dhcpEnd) {
        Block block = parseCidr(cidr);
        if (!isRfc1918(block)) {
            throw new InvalidNetworkConfigurationException("Only RFC1918 private ranges are allowed (10/8, 172.16/12, 192.168/16)");
        }
        int gw = parseIp(gateway);
        int start = parseIp(dhcpStart);
        int end = parseIp(dhcpEnd);
        if (!block.contains(gw) || gw == block.network() || gw == block.broadcast()) {
            throw new InvalidNetworkConfigurationException("Gateway must be a usable address inside " + block.cidrString());
        }
        if (!block.contains(start) || !block.contains(end)) {
            throw new InvalidNetworkConfigurationException("DHCP range must be inside " + block.cidrString());
        }
        if (Integer.compareUnsigned(start, end) > 0) {
            throw new InvalidNetworkConfigurationException("DHCP start must be less than or equal to DHCP end");
        }
        if (start == block.network() || end == block.broadcast() || start == gw || end == gw) {
            throw new InvalidNetworkConfigurationException("DHCP range cannot include the network, broadcast, or gateway address");
        }
        if (cidrAddressDoesNotMatch(cidr, block)) {
            throw new InvalidNetworkConfigurationException("CIDR host bits must be zero, e.g. 192.168.100.0/24");
        }
    }

    private static boolean cidrAddressDoesNotMatch(String cidr, Block block) {
        String address = cidr.substring(0, cidr.indexOf('/')).trim();
        return parseIp(address) != block.network();
    }

    private static boolean isRfc1918(Block block) {
        return (RFC1918_10.contains(block.network()) && Integer.compareUnsigned(block.broadcast(), RFC1918_10.broadcast()) <= 0)
                || (RFC1918_172.contains(block.network()) && Integer.compareUnsigned(block.broadcast(), RFC1918_172.broadcast()) <= 0)
                || (RFC1918_192.contains(block.network()) && Integer.compareUnsigned(block.broadcast(), RFC1918_192.broadcast()) <= 0);
    }
}
