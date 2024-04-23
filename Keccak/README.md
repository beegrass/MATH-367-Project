based on the "sponge construction"

Underlying function is a permutation chosen in a set of seven Keccak-f permutations, denoted keccak-f[b]
    where b in {25, 50, 100, 200, 400, 800, 1600} is the width of the permutation.
Width of permutation is also the width of the state in the sponge construction.

State is organized as an array of 5x5 langes, each of length w in {1, 2, 4, 8, 16, 32, 64} and b=25w.
On 640bit processor, a lane of keccak-f[1600] can be represented as a 64-bit CPU word

keccak[r, c] sponge function with parameters capacity c and bitrate r from applying sponge construction to keccak-f[r+c] and applying padding to the message input


### Keccak-f pseudocode:
\# rounds n depends on permuation width, given by n = 12+2l where 2**l = w. For keccak-f[1600] we have 24 rounds
All operations on the incides are done modulo 5.
A denotes the complete permutation state array
A[x, y] denotes a particular lane in that state
B[x, y], C[x], D[x] are intermediate values
The constants r[x, y] are the rotation offsets
RC[i] are the round constants
rot(W, r) is the bitwise cyclic shift operation, moving bit at position i into position i+r (modulo lane size)
Keccak-f[b](A):
    for i in 0...n-1:
        A = Round[b](A, RC[i])
    return A

Round[b](A, RC):
    \# θ step
    C[x] = A[x,0] xor A[x,1] xor A[x,2] xor A[x,3] xor A[x,4],   for x in 0…4
    D[x] = C[x-1] xor rot(C[x+1],1),                             for x in 0…4
    A[x,y] = A[x,y] xor D[x],                           for (x,y) in (0…4,0…4)
    \# ρ and π steps
    B[y,2*x+3*y] = rot(A[x,y], r[x,y]),                 for (x,y) in (0…4,0…4)
    # χ step
    A[x,y] = B[x,y] xor ((not B[x+1,y]) and B[x+2,y]),  for (x,y) in (0…4,0…4)

    # ι step
    A[0,0] = A[0,0] xor RC

    return A


### Spone Function pseudocode:
Assume for simplicity that r is a multiple of the lane size
input M is represented as a string of bytes: Mbytes, followed by a number (0...7) of trailing bits: Mbits
d is the delimited suffix, encodes the trailing bits Mbits and its length
padded message P is organized as an array of blocks PI
variable S holds the state as an array of lanes
the || operator denotes usual string concatenatipon

Keccak[r,c](Mbytes || Mbits):
    # Padding
    d = 2^|Mbits| + sum for i=0..|Mbits|-1 of 2^i*Mbits[i]
    P = Mbytes || d || 0x00 || … || 0x00
    P = P xor (0x00 || … || 0x00 || 0x80)

    \# Initialization
    S[x,y] = 0,                               for (x,y) in (0…4,0…4)

    \# Absorbing phase
    for each block Pi in P
        S[x,y] = S[x,y] xor Pi[x+5*y],          for (x,y) such that x+5*y < r/w
        S = Keccak-f[r+c](S)

    \# Squeezing phase
    Z = empty string
    while output is requested
        Z = Z || S[x,y],                        for (x,y) such that x+5*y < r/w
        S = Keccak-f[r+c](S)

    return Z

## Usage

