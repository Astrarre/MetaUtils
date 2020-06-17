public class Foo<T> {
    public class Bar {
    }

    static <T> Foo<T>[] array(int var0) {
        return new Foo[var0];
    }
}
