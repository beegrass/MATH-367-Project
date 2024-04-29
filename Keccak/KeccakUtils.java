package Keccak;

import java.util.Objects;

/**
 * utility and helper functions used in Keccak Sponge and State
 */
public class KeccakUtils {

    /**
     * returns a hexadecimal representation of the given byte array.
     * Based on the logic of Algorithm 11: b2h(S) in NIST.FIPS.202
     * 
     * The first hexadecimal digit pair in the returned {@code String} will
     * represent the byte at index zero of the given array. The first
     * hexadecimal digit in each pair represents the value of the
     * most-significant four bits of the corresponding byte, and the second
     * hexadecimal digit in each pair represents the value of the
     * least-significant four bits of that same byte.
     * 
     * @param bytes the non-null byte array
     * @return a {@code String} which contains two hex digits for every byte in the
     *         given array
     */
    public static String hexFromBytes(byte[] bytes) {
        Objects.requireNonNull(bytes, "Parameter `bytes` cannot be null.");
        StringBuilder hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            appendByteAsHexPair(b, hexString);
        }
        return hexString.toString();
    }

    private static void appendByteAsHexPair(byte b, StringBuilder sb) {
        assert sb != null;
        byte leastSignificantHalf = (byte) (b & 0x0f);
        byte mostSignificantHalf = (byte) ((b >> 4) & 0x0f);
        sb.append(getHexDigitWithValue(mostSignificantHalf));
        sb.append(getHexDigitWithValue(leastSignificantHalf));
    }

    private static char getHexDigitWithValue(byte value) {
        assert value >= 0 && value <= 16;
        if (value < 10) {
            return (char) ('0' + value);
        }
        return (char) ('A' + value - 10);
    }

    public static byte[] createSufficientlyLargeByteArray(int bitCount) {
        assert bitCount > 0;
        int bytesRequired = divideThenRoundUp(bitCount, Byte.SIZE);
        return new byte[bytesRequired];
    }

    public static int divideThenRoundUp(int dividend, int divisor) {
        assert dividend >= 0;
        assert divisor > 0;
        if (dividend == 0) {
            return 0;
        }
        if (dividend % divisor == 0) {
            return dividend / divisor;
        } else {
            return 1 + dividend / divisor;
        }
    }

    public static void validateBitrate(int bitrate) {
        if (bitrate < 1) {
            throw new IllegalArgumentException(
                    "bitrate must be greater than zero.");
        }
        if (bitrate % Byte.SIZE != 0) {
            throw new UnsupportedOperationException(
                    "Currently only bitrates exactly divisible by 8 are supported.");
        }
        if (bitrate >= 1600) {
            throw new IllegalArgumentException(
                    "bitrate must be less than 1600 bits.");
        }
    }

    public static void validateSuffixBits(String suffixBits) {
        Objects.requireNonNull(suffixBits);
        int length = suffixBits.length();
        for (int index = 0; index < length; ++index) {
            char c = suffixBits.charAt(index);
            if (c != '1' && c != '0') {
                throw new IllegalArgumentException(
                        "If suffixBits is provided then it must be a bitstring. "
                                + "It can contain only digits 0 and 1 and nothing else.");
            }
        }
    }

    public static void validateCapacity(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException(
                    "capacity must be greater than zero.");
        }
        if (capacity >= 1600) {
            throw new IllegalArgumentException(
                    "capacity must be less than 1600 bits.");
        }
    }

    public static void validateOutputLength(int outputLength) {
        if (outputLength < 1) {
            throw new IllegalArgumentException(
                    "outputLength must be greater than zero.");
        }
    }

    public static void validateMessageLength(byte[] message,
            int messageLengthInBits) {
        if (messageLengthInBits < 0) {
            throw new IllegalArgumentException(
                    "messageLengthInBits cannot be negative.");
        }
        if (messageLengthInBits > message.length * Byte.SIZE) {
            throw new IllegalArgumentException(
                    "messageLengthInBits cannot be greater than the bit length of the message byte array.");
        }
    }

    public static void moveMessageBitsIntoInput(byte[] message,
            int messageLengthInBits, byte[] input) {
        assert message != null;
        assert messageLengthInBits >= 0;
        assert input != null;
        if (messageLengthInBits % Byte.SIZE == 0) {
            System.arraycopy(message, 0, input, 0, messageLengthInBits
                    / Byte.SIZE);
        } else {
            partialByteCopy(message, input, messageLengthInBits);
        }
    }

    public static void partialByteCopy(byte[] source, byte[] destination,
            int bitLimit) {
        assert source != null;
        assert destination != null;
        assert bitLimit >= 0;
        int wholeByteCount = bitLimit / Byte.SIZE;
        System.arraycopy(source, 0, destination, 0, wholeByteCount);
        int remainingBits = bitLimit % Byte.SIZE;
        for (int bitIndex = 0; bitIndex < remainingBits; ++bitIndex) {
            int bitValue = (1 << bitIndex);
            boolean sourceBitHigh = (source[wholeByteCount] & bitValue) != 0;
            if (sourceBitHigh) {
                destination[wholeByteCount] += bitValue;
            }
        }
    }

    public static void requireWholeByteBitrate(int bitrate) {
        assert bitrate > 0;
        if (bitrate % Byte.SIZE != 0) {
            throw new UnsupportedOperationException(
                    "bitrate must be divisible by eight in order to process byte stream.");
        }
    }

}
