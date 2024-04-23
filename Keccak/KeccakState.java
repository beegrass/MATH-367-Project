package Keccak;

/**
 * Defines the Keccak permutation state which can absorb message input,
 * be permuted, and be 'squeezed' to produce hash output.
 */
abstract class KeccakState {
    abstract byte getLaneLengthInBits();

    abstract byte getNumberOfRoundsPerPermutation();

    /**
     * Absorbs the given input into the Keccak State, reading blocks of at most
     * {@code bitrate}
     * bits at a time and permuting the entire state after each block
     * 
     * @param input             a byte array that contains the input bits. Already
     *                          suffixed and padded
     * @param inputLengthInBits the num bits from the input byte array
     * @param bitrate           the max number of bits to read from the input in
     *                          each block
     */
    void absorb(byte[] input, int inputLengthInBits, short bitrate) {
        assert input != null;
        assert inputLengthInBits >= 0;
        assert bitrate > 0;
        int inputBitIndex = 0;
        while (inputBitIndex < inputLengthInBits) {
            int readLength = Math.min(bitrate, inputLengthInBits - inputBitIndex);
            absorbBitsIntoState(input, inputBitIndex, readLength);
            permute();
            inputBitIndex += bitrate;
        }
    }

    void absorbBitsIntoState(byte[] input, int inputStartBitIndex, int readLengthInBits) {
        byte laneLength = getLaneLengthInBits();
        assert input != null;
        assert inputStartBitIndex >= 0;
        assert readLengthInBits >= 0 && readLengthInBits <= laneLength * 25;
        int inputBitIndex = inputStartBitIndex;
        int readRemaining = readLengthInBits;
        for (int y = 0; y < 5; ++y) {
            for (int x = 0; x < 5; ++x) {
                if (inputBitIndex % Byte.SIZE == 0 && readRemaining >= laneLength) {
                    absorbEntireLaneIntoState(input, inputBitIndex, x, y);
                    inputBitIndex += laneLength;
                    readRemaining -= laneLength;
                } else {
                    absorbBitByBitIntoState(input, inputBitIndex, readRemaining,
                            x, y);
                    return;
                }
            }
        }
    }

    abstract void absorbEntireLaneIntoState(byte[] input, int inputBitIndex, int x, int y);

    abstract void absorbBitByBitIntoState(byte[] input, int inputStartBitIndex, int readLengthInBits, int x, int y);

    /**
     * applies the keccak-f permutation function to this state
     */
    void permute() {
        byte roundsPerPermutation = getNumberOfRoundsPerPermutation();
        for (int roundIndex = 0; roundIndex < roundsPerPermutation; ++roundIndex) {
            permutationRound(roundIndex);
        }
    }

    abstract void applyComplementingPattern();

    private void permutationRound(int roundIndex) {
        assert roundIndex >= 0 && roundIndex < getNumberOfRoundsPerPermutation();
        theta();
        rhoPi();
        chi();
        iota(roundIndex);
    }

    abstract void theta();

    abstract void rhoPi();

    abstract void chi();

    abstract void iota(int roundIndex);

    /**
     * Squeezes Keccak sponge state as many times as needed to generate an output of
     * the requested length
     * 
     * @param bitrate            the max number of bits to squeeze out of the state
     *                           in each block before the state is permuted
     * @param outputLengthInBits required output size in bits
     * @return a byte array that represents the output squeeze from the keccak
     *         permuation state
     */
    byte[] squeeze(short bitrate, int outputLengthInBits) {
        assert bitrate > 0;
        assert outputLengthInBits > 0;
        byte[] output = createOutputArray(outputLengthInBits);
        int writeLength = Math.min(bitrate, outputLengthInBits);
        squeezeBitsFromState(output, 0, writeLength);
        for (int outputBitIndex = bitrate; outputBitIndex < outputLengthInBits; outputBitIndex += bitrate) {
            permute();
            writeLength = Math.min(bitrate, outputLengthInBits - outputBitIndex);
            squeezeBitsFromState(output, outputBitIndex, writeLength);
        }
        return output;
    }

    private byte[] createOutputArray(int outputLengthInBits) {
        assert outputLengthInBits > 0;
        int requiredBytes = outputLengthInBits / Byte.SIZE;
        if (outputLengthInBits % Byte.SIZE != 0) {
            ++requiredBytes;
        }
        return new byte[requiredBytes];
    }

    private void squeezeBitsFromState(byte[] output, int outputStartBitIndex, int writeLength) {
        byte laneLength = getLaneLengthInBits();
        assert output != null;
        assert outputStartBitIndex >= 0;
        assert writeLength >= 0;
        assert laneLength >= Byte.SIZE;
        int outputBitIndex = outputStartBitIndex;
        int outputStopIndex = outputStartBitIndex + writeLength;
        for (int y = 0; y < 5; ++y) {
            for (int x = 0; x < 5; ++x) {
                if (outputBitIndex == outputStopIndex) {
                    return;
                }
                if (outputBitIndex % Byte.SIZE == 0 && writeLength
                        - outputBitIndex >= laneLength) {
                    squeezeEntireLaneIntoOutput(x, y, output, outputBitIndex);
                    outputBitIndex += laneLength;
                } else {
                    outputBitIndex = squeezeLaneBitByBitIntoOutput(output,
                            outputBitIndex, outputStopIndex, x, y);
                }
            }
        }
    }

    abstract void squeezeEntireLaneIntoOutput(int x, int y, byte[] output,
            int outputBitIndex);

    abstract int squeezeLaneBitByBitIntoOutput(byte[] output, int outputBitIndex,
            int outputStopIndex, int x, int y);

    /**
     * Returns the state of the bit within the byte array at the given bit index
     * 
     * @param input         a non null byte array
     * @param inputBitIndex the array-wide index of the bit of interest
     * @return {@code true} if the specified bit is binary "1": high
     *         {@code false} if the bit is binary "0": low
     */
    protected static boolean isInputBitHigh(byte[] input, int inputBitIndex) {
        assert input != null;
        assert inputBitIndex >= 0 && inputBitIndex < input.length * Byte.SIZE;
        int inputByteIndex = inputBitIndex / Byte.SIZE;
        int inputByteBitIndex = inputBitIndex % Byte.SIZE;
        return 0 != (input[inputByteIndex] & (1 << inputByteBitIndex));
    }

    /**
     * Modified the given byte array to set to high the state of the specified
     * array-wide bit index
     * Assumes the specified bit is initially low
     * 
     * @param output         a byte array holding the output squeezed from a keccak
     *                       sponge
     * @param outputBitIndex the array wide index of the bit to modify
     */
    protected static void setOutputBitHigh(byte[] output, int outputBitIndex) {
        assert output != null;
        assert outputBitIndex >= 0;
        int outputByteIndex = outputBitIndex / Byte.SIZE;
        byte outputByteBitIndex = (byte) (outputBitIndex % Byte.SIZE);
        byte byteBitValue = (byte) (1 << outputByteBitIndex);
        output[outputByteIndex] += byteBitValue;
    }
}