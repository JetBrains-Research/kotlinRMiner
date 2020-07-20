package org.jetbrains.research.kotlinrminer.util;

public class PrefixSuffixUtils {

    public static int separatorPosOfCommonSuffix(char separator, String s1, String s2) {
        int l1 = s1.length();
        int l2 = s2.length();
        int separatorPos = -1;
        int lmin = Math.min(s1.length(), s2.length());
        boolean equal = true;
        for (int i = 0; i < lmin; i++) {
            char c1 = s1.charAt(l1 - i - 1);
            char c2 = s2.charAt(l2 - i - 1);
            equal = equal && c1 == c2;
            if (equal && c1 == separator) {
                separatorPos = i;
            }
        }
        return separatorPos;
    }

}
