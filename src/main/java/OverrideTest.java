import java.util.ArrayList;
import java.util.List;

public class OverrideTest<T> {
    public  List foo(List x) {return null;}

    static class Overriding extends OverrideTest<List> {
        @Override
        public ArrayList foo(List x) {
            return null;
        }
    }

    String getX;
    String getX(int y){
        return null;
    }
}
