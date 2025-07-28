import java.math.BigInteger;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class SecretFinder {

    static class Token {
        BigInteger x, y;
        Token(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
    }

    // Decode a string in given base to BigInteger
    public static BigInteger decodeValue(String value, int base) {
        BigInteger result = BigInteger.ZERO;
        BigInteger b = BigInteger.valueOf(base);
        for (char c : value.toCharArray()) {
            int digit = Character.digit(c, base);
            result = result.multiply(b).add(BigInteger.valueOf(digit));
        }
        return result;
    }

    // Modular inverse
    public static BigInteger modInverse(BigInteger a, BigInteger mod) {
        BigInteger m0 = mod, t, q;
        BigInteger x0 = BigInteger.ZERO, x1 = BigInteger.ONE;
        while (a.compareTo(BigInteger.ONE) > 0) {
            q = a.divide(mod);
            t = mod;
            mod = a.mod(mod);
            a = t;
            t = x0;
            x0 = x1.subtract(q.multiply(x0));
            x1 = t;
        }
        if (x1.compareTo(BigInteger.ZERO) < 0)
            x1 = x1.add(m0);
        return x1;
    }

    public static BigInteger[] multiply(BigInteger[] a, BigInteger[] b, BigInteger mod) {
        BigInteger[] res = new BigInteger[a.length + b.length - 1];
        Arrays.fill(res, BigInteger.ZERO);
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < b.length; j++) {
                res[i + j] = res[i + j].add(a[i].multiply(b[j])).mod(mod);
            }
        }
        return res;
    }

    public static BigInteger[] interpolate(Token[] tokens, BigInteger mod) {
        int k = tokens.length;
        BigInteger[] coeffs = new BigInteger[k];
        Arrays.fill(coeffs, BigInteger.ZERO);

        for (int i = 0; i < k; i++) {
            BigInteger xi = tokens[i].x;
            BigInteger yi = tokens[i].y;
            BigInteger[] basis = {BigInteger.ONE};
            BigInteger denom = BigInteger.ONE;

            for (int j = 0; j < k; j++) {
                if (i == j) continue;
                BigInteger xj = tokens[j].x;
                BigInteger[] term = {xj.negate().mod(mod), BigInteger.ONE};
                basis = multiply(basis, term, mod);
                denom = denom.multiply(xi.subtract(xj)).mod(mod);
            }

            BigInteger invDenom = modInverse(denom, mod);
            for (int d = 0; d < basis.length; d++) {
                coeffs[d] = coeffs[d].add(basis[d].multiply(yi).multiply(invDenom)).mod(mod);
            }
        }

        return coeffs;
    }

    // Manual JSON parsing using regex (no org.json)
    public static Token[] parseTokens(String json) {
        int n = extractInt(json, "\"n\"\\s*:\\s*(\\d+)");
        int k = extractInt(json, "\"k\"\\s*:\\s*(\\d+)"); // just to confirm structure
        List<Token> tokens = new ArrayList<>();

        Pattern tokenPattern = Pattern.compile("\"(\\d+)\"\\s*:\\s*\\{\\s*\"base\"\\s*:\\s*\"(\\d+)\",\\s*\"value\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = tokenPattern.matcher(json);

        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            int base = Integer.parseInt(matcher.group(2));
            String value = matcher.group(3);
            BigInteger x = BigInteger.valueOf(index);
            BigInteger y = decodeValue(value, base);
            tokens.add(new Token(x, y));
        }

        return tokens.toArray(new Token[0]);
    }

    // Extracts a single integer using a regex pattern
    public static int extractInt(String json, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(json);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        throw new RuntimeException("Expected key not found in JSON");
    }

    public static BigInteger solve(String filePath) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        int k = extractInt(content, "\"k\"\\s*:\\s*(\\d+)");
        Token[] allTokens = parseTokens(content);

        BigInteger mod = new BigInteger("100000000000000000000000000000000000000000000000000000000000000039");

        List<Token> shuffled = Arrays.asList(allTokens.clone());
        Collections.shuffle(shuffled);
        Token[] selected = shuffled.subList(0, k).toArray(new Token[0]);

        BigInteger[] coeffs = interpolate(selected, mod);
        return coeffs[0];
    }

    public static void main(String[] args) throws Exception {
        BigInteger secret1 = solve("testcase1.json");
        BigInteger secret2 = solve("testcase2.json");

        System.out.println("Secret from testcase1.json: " + secret1);
        System.out.println("Secret from testcase2.json: " + secret2);
    }
}
