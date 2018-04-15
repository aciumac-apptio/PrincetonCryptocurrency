import java.security.SecureRandom;

public class SampleRandom {

    public static int randomInt(int size) {
        SecureRandom rng = new SecureRandom();
        return (rng.nextInt(size));
    }

    public static double randomDouble(double maxValue) {
        SecureRandom rng = new SecureRandom();
        return rng.nextDouble()*maxValue;
    }

}
