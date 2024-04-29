package Streebog;

import java.security.Provider;

public final class StreebogProvider extends Provider {

    @SuppressWarnings("deprecation")
    public StreebogProvider() {
        super("GOST", 0.1, "The Russian Federal standard (GOST) provider " +
                "(implements client mechanisms for: GOST R 34.11-2012)");
        put("MessageDigest.GOST3411-2012.256", Streebog256.class.getCanonicalName());
        put("MessageDigest.GOST3411-2012.512", Streebog512.class.getCanonicalName());
    }
}