package api;


import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GenericTest<@Nullable T extends Object & Comparable> extends ArrayList<T> {
    @ Nullable Integer x;
    List<?> y;

    <U> U x(@NonNls @Nullable List<@Nullable ? super Object> foo){
        List<?> bar = new ArrayList<>();
    }

    T f(@Nullable T param){
        @Nullable T x = null;
        return null;
    }
}
