import java.util.ArrayList;
import java.util.List;

public class InnerClassGenericTest extends ArrayList<int[]> {
    public static class InnerClass<T> {

    }

    public static void foo(List<? extends Object>bar , List<? super List>baz, List<?> bal){

    }
}
