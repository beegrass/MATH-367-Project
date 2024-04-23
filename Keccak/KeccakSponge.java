package Keccak;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

public class KeccakSponge implements UnaryOperator<byte[]> {

    private static final Set<Short> VALID_WIDTHS;

    static {
        Set<Short> widths = new HashSet<>(16);
        widths.addAll(Arrays.asList(new Short[] {
                25,
                50,
                100,
                200,
                400,
                800,
                1600
        }));
        VALID_WIDTHS = Collections.unmodifiableSet(widths);
    }

    private final short bitrate;
    private final short capacity;
    private final byte laneLength;
    private final int outputLengthInBits;

    /**
     * Suffix bits that define a hash application domain
     */
    private final String suffixBits;

    public KeccakSponge(int bitrate, int capacity, String suffixBits,
            int outputLength) {
        KeccakUtils.validateBitrate(bitrate);
        KeccakUtils.validateCapacity(capacity);
        KeccakUtils.validateSuffixBits(suffixBits);
        KeccakUtils.validateOutputLength(outputLength);
        short width = (short) (bitrate + capacity);
        validatePermutationWidth(width);
        this.bitrate = (short) bitrate;
        this.capacity = (short) capacity;
        this.suffixBits = suffixBits;
        this.laneLength = (byte) (width / 25);
        this.outputLengthInBits = outputLength;
    }

    /**
     * returns the bitrate aka the max number of bits within each block
     * absorbed into or squeezed from the permutation states
     * 
     * @return the bitrate in bits
     */
    public int getBitrate() {
        return bitrate;
    }

    /**
     * returns the capacity aka the size of the portion of the permutation state
     * which is not modded by the absortption of a new input block
     * 
     * @return the capacity in bits
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * returns the permutation width aka the size of the permutation state
     * Also equivalent to bitrate + capacity
     * 
     * @return the permutation width in bits
     */
    public int getPermutationWidth() {
        return bitrate + capacity;
    }

    public int getLaneLength() {
        return laneLength;
    }

    /**
     * returns the number of rounds within every permutation that should
     * be applied for a keccak sponge with the specified lane length
     * is 22 for laneLegnth of 32
     * @return
     */
    public int getNumberOfRoundsPerPermutation() {
        return 22;
    }

    public Optional<String> getSuffixBits() {
        if (suffixBits.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(suffixBits);
        }
    }

    public int getOutputLengthInBits() {
        return outputLengthInBits;
    }

    @Override
    public byte[] apply(byte[] message) {
        return apply(message.length * Byte.SIZE, message);
    }

    public byte[] apply(int messageLengthInBits, byte[] message) {
        KeccakUtils.validateMessageLength(message, messageLengthInBits);
        int inputLengthInBits = calculateTotalInputLength(messageLengthInBits);
        byte[] input = KeccakUtils.createSufficientlyLargeByteArray(inputLengthInBits);
        KeccakUtils.moveMessageBitsIntoInput(message, messageLengthInBits, input);
        appendDomainSuffixToInput(input, messageLengthInBits);
        padInput(input, messageLengthInBits);
        KeccakState state = new KeccakState800();
        state.absorb(input, inputLengthInBits, bitrate);
        byte[] hash = state.squeeze(bitrate, outputLengthInBits);
        return hash;
    }

    public byte[] apply(InputStream stream) throws IOException {
        KeccakUtils.requireWholeByteBitrate(bitrate);
        Objects.requireNonNull(stream);
        KeccakState state = new KeccakState800();
        byte[] block = KeccakUtils.createSufficientlyLargeByteArray(bitrate);
        int finalBlockMessageBits = absorbInitialStreamBlocksIntoState(stream,
                block, state);
        byte[] finalBlock = prepareFinalBlockArray(finalBlockMessageBits, block);
        appendDomainSuffixToInput(finalBlock, finalBlockMessageBits);
        padInput(finalBlock, finalBlockMessageBits);
        state.absorb(finalBlock, finalBlock.length * Byte.SIZE, bitrate);
        byte[] hash = state.squeeze(bitrate, outputLengthInBits);
        return hash;
    }

    private int calculateTotalInputLength(int messageLengthInBits) {
        assert messageLengthInBits >= 0;
        int minimumPaddedLength = calculateMinimumLengthAfterPadding(
                messageLengthInBits);
        if (minimumPaddedLength % bitrate == 0) {
            return minimumPaddedLength;
        } else {
            return minimumPaddedLength + bitrate - minimumPaddedLength % bitrate;
        }
    }

    private int calculateMinimumLengthAfterPadding(int messageLengthInBits) {
        // The padding always starts and ends with a high '1' bit, so the
        // padding length will always be at least two bits.
        return messageLengthInBits + suffixBits.length() + 2;
    }

    private void appendDomainSuffixToInput(byte[] input, int suffixStartBitIndex) {
        assert input != null;
        assert suffixStartBitIndex >= 0;
        assert suffixBits != null;
        for (int suffixBitIndex = 0; suffixBitIndex < suffixBits.length(); ++suffixBitIndex) {
            boolean suffixBitHigh = suffixBits.charAt(suffixBitIndex) == '1';
            if (suffixBitHigh) {
                int targetInputBit = suffixStartBitIndex + suffixBitIndex;
                int targetInputByte = targetInputBit / Byte.SIZE;
                int targetInputByteBitIndex = targetInputBit % Byte.SIZE;
                input[targetInputByte] += 1 << targetInputByteBitIndex;
            }
        }
    }

    private void padInput(byte[] input, int messageLengthInBits) {
        assert input != null;
        assert messageLengthInBits >= 0;
        int lengthOfMessageWithSuffix = messageLengthInBits + suffixBits.length();
        int zeroPaddingBitsRequired = calculateZeroPaddingBitsRequired(
                messageLengthInBits);
        int padStartIndex = lengthOfMessageWithSuffix;
        int padEndIndex = lengthOfMessageWithSuffix + 1
                + zeroPaddingBitsRequired;
        setInputBitHigh(input, padStartIndex);
        setInputBitHigh(input, padEndIndex);
    }

    private int calculateZeroPaddingBitsRequired(int messageLengthInBits) {
        int bitsIncludingPadEnds = calculateMinimumLengthAfterPadding(
                messageLengthInBits);
        int zeroPaddingBitsRequired;
        if (bitsIncludingPadEnds % bitrate == 0) {
            zeroPaddingBitsRequired = 0;
        } else {
            zeroPaddingBitsRequired = bitrate - bitsIncludingPadEnds % bitrate;
        }
        return zeroPaddingBitsRequired;
    }

    private void setInputBitHigh(byte[] input, int inputBitIndex) {
        assert input != null;
        assert inputBitIndex >= 0;
        int inputByteIndex = inputBitIndex / Byte.SIZE;
        byte outputByteBitIndex = (byte) (inputBitIndex % Byte.SIZE);
        byte byteBitValue = (byte) (1 << outputByteBitIndex);
        input[inputByteIndex] += byteBitValue;
    }

    private int absorbInitialStreamBlocksIntoState(InputStream stream,
            byte[] block, KeccakState state) throws IOException {
        assert stream != null;
        assert block != null;
        assert state != null;
        int bitsInCurrentBlock = readBlockFromStream(stream, block);
        while (bitsInCurrentBlock == bitrate) {
            state.absorbBitsIntoState(block, 0, bitsInCurrentBlock);
            state.permute();
            bitsInCurrentBlock = readBlockFromStream(stream, block);
        }
        return bitsInCurrentBlock;
    }

    private int readBlockFromStream(InputStream stream, byte[] block) throws IOException {
        assert block != null;
        assert block.length * Byte.SIZE == bitrate;
        assert stream != null;
        int filledBytes = 0;
        int readBytes = stream.read(block);
        while (readBytes > 0) {
            filledBytes += readBytes;
            readBytes = stream.read(block, filledBytes, block.length
                    - filledBytes);
        }
        if (filledBytes < block.length) {
            Arrays.fill(block, filledBytes, block.length, (byte) 0);
        }
        return filledBytes * Byte.SIZE;
    }

    private byte[] prepareFinalBlockArray(int finalBlockMessageLengthInBits,
            byte[] finalBlock) {
        assert finalBlockMessageLengthInBits >= 0;
        assert finalBlock != null;
        int minimumLengthAfterPadding = calculateMinimumLengthAfterPadding(
                finalBlockMessageLengthInBits);
        if (minimumLengthAfterPadding <= bitrate) {
            // The existing byte array is large enough so simply return it.
            return finalBlock;
        } else {
            return resizedFinalBlockArray(finalBlockMessageLengthInBits,
                    finalBlock, minimumLengthAfterPadding);
        }
    }

    private byte[] resizedFinalBlockArray(int finalBlockMessageLengthInBits,
            byte[] finalBlock, int minimumLengthAfterPadding) {
        int blocksRequired = KeccakUtils.divideThenRoundUp(minimumLengthAfterPadding, bitrate);
        byte[] finalBlocks = new byte[blocksRequired * bitrate / Byte.SIZE];
        int bytesToCopy = KeccakUtils.divideThenRoundUp(finalBlockMessageLengthInBits,
                Byte.SIZE);
        System.arraycopy(finalBlock, 0, finalBlocks, 0, bytesToCopy);
        return finalBlocks;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("Keccak[");
        sb.append(getBitrate());
        sb.append(", ");
        sb.append(getCapacity());
        sb.append("](M");
        if (getSuffixBits().isPresent()) {
            sb.append(" || ");
            sb.append(getSuffixBits().get());
            sb.append(',');
        } else {
            sb.append(',');
        }
        sb.append(' ');
        sb.append(getOutputLengthInBits());
        sb.append(')');
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof KeccakSponge)) {
            return false;
        }
        KeccakSponge that = (KeccakSponge) obj;
        return this.bitrate == that.bitrate
                && this.capacity == that.capacity
                && this.outputLengthInBits == that.outputLengthInBits
                && this.suffixBits.equals(that.suffixBits);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + this.bitrate;
        hash = 41 * hash + this.capacity;
        hash = 41 * hash + Objects.hashCode(this.suffixBits);
        hash = 41 * hash + this.outputLengthInBits;
        return hash;
    }

    private static void validatePermutationWidth(short width) {
        if (width < 200) {
            throw new UnsupportedOperationException(
                    "Support is not yet in place for permutations widths smaller than 200 bits.");
        }
        if (!VALID_WIDTHS.contains(width)) {
            List<Short> validWidthList = new ArrayList<>(VALID_WIDTHS);
            validWidthList.sort((a, b) -> a - b);
            throw new IllegalArgumentException(
                    "Sum of bitrate and capacity must equal a valid width: "
                            + validWidthList + ".");
        }
    }

    public static void main(String[] args) {
        KeccakSponge spongeFunction = new KeccakSponge(576, 1024, "", 512);
        byte[] message = new byte[] { (byte) 19 };
        byte[] hash = spongeFunction.apply(5, message);
        for (int i = 0; i < 900000; ++i) {
            hash = spongeFunction.apply(hash);
        }
        String hexRepOfHash = KeccakUtils.hexFromBytes(hash);
        System.out.println(hexRepOfHash);
    }

}
