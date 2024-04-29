package Skein;

import Keccak.KeccakUtils;

public class SkeinMain {

    /**
     * Benchmarks an instance of the Skein hash function.
     *
     * @param iterations
     *                   Number of hash computations to perform.
     * @param skein
     *                   Resulting speed in megabytes per second.
     * @param warmup
     *                   If set then don't print results, just warmup JIT compiler
     * @return
     */
    public double Benchmark(long iterations, Skein skein) {
        int hashBytes = skein.getHashSize() / 8;
        byte[] hash = new byte[hashBytes];

        for (long i = 0; i < iterations; i++)
            skein.update(hash, 0, hashBytes);

        hash = skein.doFinal();

        System.out.println("Skien hash digest: ");
        System.out.println(KeccakUtils.hexFromBytes(hash));

        return 0.0;
    }

    public static void main(String args[]) {

        try {
            SkeinMain skm = new SkeinMain();
            skm.Benchmark(1000000, new Skein(512, 512));
            skm.Benchmark(10000000, new Skein(512, 512));
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Skein benchmark done.");
    }

}
