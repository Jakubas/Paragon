package haven;

public class StringExtensions {
    public static String getstringbetween(String input, String leftdelimiter, String rightdelimiter) {
        int leftdelimiterposition = input.indexOf(leftdelimiter);
        if (leftdelimiterposition == -1)
            return "";

        int rightdelimiterposition = input.indexOf(rightdelimiter, leftdelimiterposition);
        if (rightdelimiterposition == -1)
            return "";

        return input.substring(leftdelimiterposition + leftdelimiter.length(), rightdelimiterposition);
    }

    public static String removehtmltags(String input) {
        return input.replaceAll("\\<[^>]*>", "");
    }
}
