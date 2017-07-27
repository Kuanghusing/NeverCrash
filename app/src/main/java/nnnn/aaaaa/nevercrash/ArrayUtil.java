package nnnn.aaaaa.nevercrash;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kuahusing on 17/7/16.
 */

public class ArrayUtil {

    public static <T> List<T> filter(List<T> array, Filter<T> transform) {
        List<T> arrayAfterFilter = new ArrayList<>();
        for (T t :
                array) {
            if (transform.filter(t)) {
                arrayAfterFilter.add(t);
            }
        }
        return arrayAfterFilter;
    }

    public static <T, R> List<R> map(List<T> array, Map<T, R> transform) {
        List<R> arrayAfterMap = new ArrayList<>();
        for (T t :
                array) {
            arrayAfterMap.add(transform.map(t));
        }
        return arrayAfterMap;

    }


    interface Filter<T> {
        boolean filter(T t);
    }

    interface Map<T, R> {
        R map(T t);
    }
}
