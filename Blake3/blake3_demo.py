import blake3
import secrets

def main():
    
    # regular hashing
    hash1 = blake3.Hasher()
    hash1.update(b"foobar")
    output1 = hash1.finalize()
    print("Output of hash 1:", output1)
    
    print()

    # extendable output
    hash2 = blake3.Hasher()
    hash2.update(b"foobar")
    output2 = hash2.finalize(100)
    print("Output of hash 2:", output2)
    
    print()

    # keyed hashing
    random_key = secrets.token_bytes(32)
    message = b"a message to authenticate"
    keyed_hasher = blake3.Hasher.new_keyed(random_key)
    keyed_hasher.update(message)
    mac = keyed_hasher.finalize()
    print("Output of keyed hash:", mac)
    
    print()

    # key derivation
    context_string = "pure_blake3 2021-10-29 18:37:44 example context"
    key_material = b"usually at least 32 random bytes, not a password"
    kdf = blake3.Hasher.new_derive_key(context_string)
    kdf.update(key_material)
    derived_key = kdf.finalize()
    print("Output of key derivation:", derived_key)

if __name__ == "__main__":
    main()

