import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

public class ShamirSecret {

    static class Point {
        BigInteger x;
        BigInteger y;

        Point(BigInteger x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
    }

    public static void main(String[] args) throws IOException, ParseException {
        // Test case JSON files
        String[] testCases = { "test1.json", "test2.json" };
        JSONParser parser = new JSONParser();

        for (String testCase : testCases) {
            // Parse the JSON file
            JSONObject root = (JSONObject) parser.parse(new FileReader(testCase));
            JSONObject keys = (JSONObject) root.get("keys");
            int k = ((Long) keys.get("k")).intValue(); // Convert Long to int

            List<Point> points = new ArrayList<>();
            Set<BigInteger> xValues = new HashSet<>(); // To check for duplicate x-values

            // Iterate over JSON keys
            for (Object keyObj : root.keySet()) {
                String key = (String) keyObj;
                if (key.equals("keys"))
                    continue;

                JSONObject entry = (JSONObject) root.get(key);
                int base = Integer.parseInt((String) entry.get("base"));
                String valueStr = ((String) entry.get("value")).toLowerCase();
                BigInteger x = new BigInteger(key);
                BigInteger y = new BigInteger(valueStr, base);

                // Check for duplicate x-values
                if (xValues.contains(x)) {
                    System.err.println("Error: Duplicate x-value found in JSON: " + x);
                    return;
                }
                xValues.add(x);
                points.add(new Point(x, y));
            }

            // Ensure we have enough unique points
            if (points.size() < k) {
                System.err.println(
                        "Error: Not enough unique points available. Required: " + k + ", Found: " + points.size());
                return;
            }

            // Select first k points
            List<Point> selectedPoints = points.subList(0, k);
            BigInteger secret = computeConstantTerm(selectedPoints);
            System.out.println("Recovered Secret: " + secret);
        }
    }

    private static BigInteger computeConstantTerm(List<Point> points) {
        BigInteger sumNum = BigInteger.ZERO;
        BigInteger sumDen = BigInteger.ONE;
        int k = points.size();

        for (int i = 0; i < k; i++) {
            Point current = points.get(i);
            BigInteger xi = current.x;
            BigInteger yi = current.y;

            BigInteger termNum = yi;
            BigInteger termDen = BigInteger.ONE;

            for (int j = 0; j < k; j++) {
                if (j == i)
                    continue;
                BigInteger xj = points.get(j).x;
                termNum = termNum.multiply(xj.negate());
                termDen = termDen.multiply(xi.subtract(xj));
            }

            // Add to sum
            BigInteger newNum = sumNum.multiply(termDen).add(termNum.multiply(sumDen));
            BigInteger newDen = sumDen.multiply(termDen);

            // Simplify fraction
            BigInteger gcd = newNum.gcd(newDen);
            sumNum = newNum.divide(gcd);
            sumDen = newDen.divide(gcd);

            // Debugging output
            // System.out.println("Iteration " + i + " -> Numerator: " + sumNum + ",
            // Denominator: " + sumDen);
        }

        // Fix denominator sign issue
        if (sumDen.equals(BigInteger.valueOf(-1))) {
            sumNum = sumNum.negate();
            sumDen = BigInteger.ONE;
        }

        // Ensure denominator is 1
        if (!sumDen.equals(BigInteger.ONE)) {
            throw new ArithmeticException("Denominator is not 1, indicating a computation error.");
        }

        return sumNum;
    }
}
