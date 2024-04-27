package Keccak;

import java.util.Scanner;

public class Main {
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your message string: ");
        String message = scanner.next().trim();
        scanner.close();

        KeccakSponge spongeFunction = new KeccakSponge(576, 1024, "", 256);
        byte[] messageBytes = message.getBytes();
        byte[] hash = spongeFunction.apply(5, messageBytes);
        for (int i = 0; i < 900000; ++i) {
            hash = spongeFunction.apply(hash);
        }
        String hexRepOfHash = KeccakUtils.hexFromBytes(hash);
        System.out.println("The hash of your message is:\n" + hexRepOfHash);
    }

}
