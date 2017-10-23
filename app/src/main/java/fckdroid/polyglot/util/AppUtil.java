package fckdroid.polyglot.util;


public class AppUtil {
    public static final String EMPTY_STRING = "";
    private static final String WORD_DELIMITER = " / ";

    private AppUtil() { }

    public static String formatWord(String word, boolean inBrackets) {
        if (word.isEmpty()) {
            return EMPTY_STRING;
        }
        char[] charArray = word.toLowerCase().toCharArray();
        char firstChar = charArray[0];
        charArray[0] = Character.toUpperCase(firstChar);
        if (inBrackets) {
            return "(" + String.valueOf(charArray) + ")";
        }
        return String.valueOf(charArray);
    }

    public static boolean checkAnswer(String translation, String userAnswer) {
        String formTransl = translation.replace("ё", "е");
        String formUserAnsw = userAnswer.replace("ё", "е");
        for (String translationVariant : formTransl.split(WORD_DELIMITER)) {
            if (translationVariant.equalsIgnoreCase(formUserAnsw)) {
                return true;
            }
        }
        return false;
    }
}
