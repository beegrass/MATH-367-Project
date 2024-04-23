import struct

blake2b_IV = [
    0x6a09e667f3bcc908, 0xbb67ae8584caa73b, 
    0x3c6ef372fe94f82b, 0xa54ff53a5f1d36f1, 
    0x510e527fade682d1, 0x9b05688c2b3e6c1f,
    0x1f83d9abfb41bd6b, 0x5be0cd19137e2179
]

blake2b_sigma = [
    [  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 ] ,
    [ 14, 10,  4,  8,  9, 15, 13,  6,  1, 12,  0,  2, 11,  7,  5,  3 ] ,
    [ 11,  8, 12,  0,  5,  2, 15, 13, 10, 14,  3,  6,  7,  1,  9,  4 ] ,
    [  7,  9,  3,  1, 13, 12, 11, 14,  2,  6,  5, 10,  4,  0, 15,  8 ] ,
    [  9,  0,  5,  7,  2,  4, 10, 15, 14,  1, 11, 12,  6,  8,  3, 13 ] ,
    [  2, 12,  6, 10,  0, 11,  8,  3,  4, 13,  7,  5, 15, 14,  1,  9 ] ,
    [ 12,  5,  1, 15, 14, 13,  4, 10,  0,  7,  6,  3,  9,  2,  8, 11 ] ,
    [ 13, 11,  7, 14, 12,  1,  3,  9,  5,  0, 15,  4,  8,  6,  2, 10 ] ,
    [  6, 15, 14,  9, 11,  3,  0,  8, 12,  2, 13,  7,  1,  4, 10,  5 ] ,
    [ 10,  2,  8,  4,  7,  6,  1,  5, 15, 11,  9, 14,  3, 12, 13 , 0 ] ,
    [  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 ] ,
    [ 14, 10,  4,  8,  9, 15, 13,  6,  1, 12,  0,  2, 11,  7,  5,  3 ]
]

def rotr64(x, n):
    """
    Performs a 64-bit right rotation

    Args:
        x (int): The 64-bit number to be rotated
        n (int): The number of bits to rotate right

    Returns:
        int: the integer 'x' rotated to the right by 'n' bits
    """
    return ((x >> n) | (x << (64 - n)))

def G(m, r, i, a, b, c, d):
    """
    Performs 64-bit word rotations of respectively 32, 25, 16, and 11 bits.

    Args:
        m (list): A list containing the 16 64-bit message block words
        r (_type_): _description_
        i (_type_): _description_
        a (_type_): _description_
        b (_type_): _description_
        c (_type_): _description_
        d (_type_): _description_

    Returns:
        tuple: the new values of a, b, c, d
    """
    a = a + b + m[blake2b_sigma[r][2*i]]
    d = rotr64((d ^ a), 32)
    c = c + d
    b = rotr64((b ^ c), 24)
    a + b + m[blake2b_sigma[r][2*i+1]]
    d = (d ^ a) >> 16
    c = c + d
    b = rotr64((b ^ c), 63)
    return a, b, c, d

def ROUND(m, r, v):
    """
    Performs a round of the BLAKE2b G function

    Args:
        m (list): A list containing the 16 64-bit message block words
        r (int): the round number
        v (list): A list containing the 16 64-bit internal state words.
    
    Returns:
        list: the updated internal state after 12 rounds of G
    """
    v[0], v[4], v[ 8], v[12] = G(m, r, 0, v[ 0], v[ 4], v[ 8], v[12])
    v[1], v[5], v[ 9], v[13] = G(m, r, 1, v[ 1], v[ 5], v[ 9], v[13])
    v[2], v[6], v[10], v[14] = G(m, r, 2, v[ 2], v[ 6], v[10], v[14])
    v[3], v[7], v[11], v[15] = G(m, r, 3, v[ 3], v[ 7], v[11], v[15])
    v[0], v[5], v[10], v[15] = G(m, r, 4, v[ 0], v[ 5], v[10], v[15])
    v[1], v[6], v[11], v[12] = G(m, r, 5, v[ 1], v[ 6], v[11], v[12])
    v[2], v[7], v[ 8], v[13] = G(m, r, 6, v[ 2], v[ 7], v[ 8], v[13])
    v[3], v[4], v[ 9], v[14] = G(m, r, 7, v[ 3], v[ 4], v[ 9], v[14])
    return v

def new_chain(h, v):
    """
    Defines the new chain value h' after the 12 rounds of G

    Args:
        h (list): A list of 8 64-bit chain value words
        v (list): A list containing the 16 64-bit internal state words.

    Returns:
        list: the updated chain value words
    """
    h = [(h[i] ^ v[i] ^ v[i + 8]) for i in range(len(h))]

    return h

def compress(h, m, t, f):
    """
    Compresses the BLAKE2b hash function for a given 128-byte message block

    Args:
        h (list): A list of 8 64-byte chain values
        m (list): A 128-byte message block
        t (tuple): A counter
        f (tuple): the Finalization flags f0 and f1

    Returns:
        list: A list of the updated chain value words after compression
    """
    v = [0] * 16

    for i in range(8):
        v[i] = h[i]

    v[8] = blake2b_IV[0]
    v[9] = blake2b_IV[1]
    v[10] = blake2b_IV[2]
    v[11] = blake2b_IV[3]
    v[12] = blake2b_IV[4] ^ t[0]
    v[13] = blake2b_IV[5] ^ t[1]
    v[14] = blake2b_IV[6] ^ f[0]
    v[15] = blake2b_IV[7] ^ f[1]

    for i in range(12):
        v = ROUND(m, i, v)
    
    h = new_chain(h, v)
    
    return h

def pad_data(data):
    """Pads the last data block if and only if necessary with null bytes

    Args:
        data (_type_): _description_
    """
    data_size = len(data)

    pad_len = 128 - (data_size % 128)

    if pad_len != 128:
        data += b'x00' * pad_len

    return data


def blake2b(data, digest_size=64):
    """
    Computes the BLAKE2b hash for the given data

    Args:
        data (bytes): The input data to hash
        digest_size (int): The desired size of the hash digest in bytes

    Returns:
        bytes: The respective hash digest.
    """
    h = blake2b_IV[:]

    # Pre-processing, pad the message
    m = bytearray(data)
    m = pad_data(m)

    n_blocks = len(m) // 128

    # Compress each 128-byte chunk
    for i in range(0, len(m), 128):
        chunk = m[i : i + 128]
        h = compress(h, struct.unpack('<16Q', chunk), [0,0], [0,0])
    
    # print("Length of h:", len(h))  # Add this line
    # print("Content of h:", h)       # Add this line

    # Finalization
    return struct.pack('<{}Q'.format(len(h)), *h)[:digest_size]

data = b"Hello, world!"
digest_size = 64
hash_value = blake2b(data, digest_size)
print(hash_value.hex())
