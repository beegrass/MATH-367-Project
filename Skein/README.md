## Skein-512
Skein-512, the primary proposal of the hash, is built from three components
1. Threefish: a tweakable block cipher with a 512-bit block size
2. Unique Block Iteration (UBI): a chaining mode that uses Threefish to build a compression function that maps an arbitrary size to a fixed output size
3. Optimal Argument Size: allows Skein to support a variety of optional features without imposing overhead on implementations and features that don't use the features.

### 512-bit Threefish Block Cipher
With a 512-bit key and a 128-bit tweak value, the core principle is that a larger number of simple rounds is more secure than fewer complex rounds. Threefish uses three mathematical operations:
1. exclusive-or (XOR)
2. addition
3. contant rotations on 64-bit words
The core of Threefish is a simple non-linear mixing function called MIX, that operates on two 64-bit words. Each MIX function consists of a single addition, a rotation by a constantm and an XOR.

### Credit
Credit to
1. the Skein team for their reference and optimized C implementations of Skein and Threefish
2. Werner Dittman for his in depth C, Go, and Java implementations
3. The Bouncy Castle crypto library for their lightweight crypto API