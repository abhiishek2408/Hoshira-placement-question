import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class TokenBombFromJson {
    static class Token {
        BigInteger x, y;
        Token(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
    }

    // Extended Euclidean Algorithm to compute modular inverse
    static BigInteger extendedModInverse(BigInteger a, BigInteger mod) {
        BigInteger m0 = mod, x0 = BigInteger.ZERO, x1 = BigInteger.ONE;
        if (mod.equals(BigInteger.ONE)) return BigInteger.ZERO;

        while (a.compareTo(BigInteger.ONE) > 0) {
            BigInteger q = a.divide(mod);
            BigInteger t = mod;

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

    static BigInteger interpolateAtZero(Token[] tokens, BigInteger mod) {
        BigInteger result = BigInteger.ZERO;
        int k = tokens.length;

        for (int i = 0; i < k; i++) {
            BigInteger xi = tokens[i].x;
            BigInteger yi = tokens[i].y;
            BigInteger num = BigInteger.ONE;
            BigInteger den = BigInteger.ONE;

            for (int j = 0; j < k; j++) {
                if (i == j) continue;
                BigInteger xj = tokens[j].x;
                num = num.multiply(xj.negate()).mod(mod);
                den = den.multiply(xi.subtract(xj)).mod(mod);
            }

            BigInteger li = num.multiply(extendedModInverse(den, mod)).mod(mod);
            result = result.add(yi.multiply(li)).mod(mod);
        }

        return result.mod(mod);
    }

    static BigInteger[] interpolatePolynomial(Token[] tokens, BigInteger mod) {
        int k = tokens.length;
        BigInteger[] coeff = new BigInteger[k];
        Arrays.fill(coeff, BigInteger.ZERO);

        for (int i = 0; i < k; i++) {
            BigInteger xi = tokens[i].x;
            BigInteger yi = tokens[i].y;
            BigInteger[] basis = new BigInteger[]{BigInteger.ONE};
            BigInteger den = BigInteger.ONE;

            for (int j = 0; j < k; j++) {
                if (i == j) continue;
                BigInteger xj = tokens[j].x;
                basis = multiplyPoly(basis, new BigInteger[]{xj.negate().mod(mod), BigInteger.ONE}, mod);
                den = den.multiply(xi.subtract(xj)).mod(mod);
            }

            BigInteger invDen = extendedModInverse(den, mod);
            for (int d = 0; d < basis.length; d++) {
                coeff[d] = coeff[d].add(yi.multiply(invDen).multiply(basis[d])).mod(mod);
            }
        }

        return coeff;
    }

    static BigInteger[] multiplyPoly(BigInteger[] a, BigInteger[] b, BigInteger mod) {
        BigInteger[] result = new BigInteger[a.length + b.length - 1];
        Arrays.fill(result, BigInteger.ZERO);
        for (int i = 0; i < a.length; i++)
            for (int j = 0; j < b.length; j++)
                result[i + j] = result[i + j].add(a[i].multiply(b[j])).mod(mod);
        return result;
    }

    static int countMatches(Token[] tokens, BigInteger[] poly, BigInteger mod) {
        int count = 0;
        for (Token token : tokens) {
            BigInteger xPow = BigInteger.ONE;
            BigInteger val = BigInteger.ZERO;
            for (BigInteger coeff : poly) {
                val = val.add(coeff.multiply(xPow)).mod(mod);
                xPow = xPow.multiply(token.x).mod(mod);
            }
            if (val.equals(token.y)) count++;
        }
        return count;
    }

    static List<List<Integer>> combinations(int n, int k) {
        List<List<Integer>> result = new ArrayList<>();
        combineHelper(result, new ArrayList<>(), 0, n, k);
        return result;
    }

    static void combineHelper(List<List<Integer>> result, List<Integer> temp, int start, int n, int k) {
        if (temp.size() == k) {
            result.add(new ArrayList<>(temp));
            return;
        }
        for (int i = start; i < n; i++) {
            temp.add(i);
            combineHelper(result, temp, i + 1, n, k);
            temp.remove(temp.size() - 1);
        }
    }

    static void findCorrectPolynomial(Token[] tokens, int k, BigInteger mod, int maxBadTokens) {
        int n = tokens.length;
        List<List<Integer>> subsets = combinations(n, k);

        for (List<Integer> indices : subsets) {
            Token[] subset = new Token[k];
            for (int i = 0; i < k; i++) {
                subset[i] = tokens[indices.get(i)];
            }

            BigInteger[] poly = interpolatePolynomial(subset, mod);
            int matches = countMatches(tokens, poly, mod);

            if (matches >= n - maxBadTokens) {
                System.out.println("Correct polynomial coefficients (mod " + mod + "):");
                for (int i = 0; i < poly.length; i++) {
                    System.out.println("x^" + i + " = " + poly[i]);
                }
                return;
            }
        }
        System.out.println("No valid polynomial found.");
    }

    public static void main(String[] args) throws Exception {
        String jsonText = new String(Files.readAllBytes(Paths.get("input.json")));
        List<Token> tokenList = new ArrayList<>();

        // Manual JSON parsing
        String tokensPart = jsonText.split("\"tokens\"\\s*:\\s*\\[")[1].split("]")[0];
        String[] entries = tokensPart.split("\\},\\s*\\{");

        for (String entry : entries) {
            entry = entry.replace("{", "").replace("}", "").replace("\"", "");
            String[] pairs = entry.split(",");
            BigInteger x = null, y = null;
            for (String pair : pairs) {
                String[] keyVal = pair.split(":");
                String key = keyVal[0].trim();
                String val = keyVal[1].trim();
                if (key.equals("x")) x = new BigInteger(val);
                if (key.equals("y")) y = new BigInteger(val);
            }
            tokenList.add(new Token(x, y));
        }

        BigInteger mod = new BigInteger(jsonText.split("\"mod\"\\s*:\\s*\"")[1].split("\"")[0]);
        int k = Integer.parseInt(jsonText.split("\"k\"\\s*:\\s*")[1].split(",")[0].trim());
        int maxBad = Integer.parseInt(jsonText.split("\"maxBadTokens\"\\s*:\\s*")[1].split("[,}]")[0].trim());

        Token[] tokens = tokenList.toArray(new Token[0]);
        findCorrectPolynomial(tokens, k, mod, maxBad);
    }
}
