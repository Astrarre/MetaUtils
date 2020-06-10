import java.util.ArrayList;
import java.util.List;

public class OverrideTest<T extends ArrayList<String>> {
    public  List foo(List x) {return null;}

//    static class Overriding extends OverrideTest<List<String>> {
//        @Override
//        public ArrayList foo(List x) {
//            return null;
//        }
//    }

    String getX;
    String getX(int y){
        return null;
    }

    String x(List<?> foo){
        return null;
    }
}
