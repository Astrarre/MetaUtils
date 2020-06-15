//import org.jetbrains.annotations.Nullable;
//import signature.MethodSignature;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class GenericsTest<T extends GenericsTest.InnerClass & Comparable<@Nullable String> & List<T>, U extends T> {
//    public static class InnerClass<V> {
//    }
//
//    public ArrayList<T> field1;
//    public ArrayList<? extends T> field2;
//
//    public <M1, M2 extends T, M3 extends Comparable<String>> void method1(M1 param1, Comparable<T> param2, List<? extends String> param3,
//                                                                         List<? super ArrayList<T>> param4, List<?> param5){
//        MethodSignature x = null;
////        x.
//    }
//
//    public <M1, M2 extends T, M3 extends Comparable<String>> M2 method2(M3 param) throws RuntimeException {
//        return null;
//    }
////
//}
