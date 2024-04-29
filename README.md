# New Hash Generation Algorithms

For MATH-367: Codes and Ciphers. This project repository is a personal collection of multiple hash algorithms including: Streebog (aka GOST R 34.11-2012), Blake2, Blake3, Skein, and Keccak. They are implemented in Java and Python.


## Streebog (GOST R 34.11-2012)

A Java implementation of Streebog using Java's new Provider class. Both 256- and 512-bit versions are available.

Usage: 
simply run Streebog\Main.java. 
The output will be the 512-bit hash digest of "Hello, World!"

## Blake2

A python implementation of Blake2b with and without tree-hashing.

Usage:
run Blake2\blake2_demo.py
The output will be multiple demos of hashed inputs with their expected (>>>) and actual (???) results

## Blake3

A python implementation of Blake3 with extendable output, key derivation, and keyed hashing.

Usage:
run Blake3\blake3_demo.py
The output will show multiple usages of Blake3: regular hashing, extendable output, keyed hashing, and key derivation.

## Skein

A java implementation of Skein. Uses Bouncy Castle's crypto API.

Usage: 
run Skein\SkeinMain.java
The output will show the difference between the same input hashed a different number of iterations.

### Credit
Credit to
1. the Skein team for their reference and optimized C implementations of Skein and Threefish
2. Werner Dittman for his in depth C, Go, and Java implementations
3. The Bouncy Castle crypto library for their lightweight crypto API

## Keccak

A java implementation of Keccak. 

Usage:
run Keccak\Main.java and type a string when prompted, press enter.
The output will be the hash digest of the inputted string.

## Credits

This repo pulls from multiple sources including official reference implementations, the bouncy castle implementations of hash algorithms, and individual repositories. All code is pulled from open source projects under liberal licenses.