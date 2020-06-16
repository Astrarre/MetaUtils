public interface Foo<T> {
    static <T> Foo<T>[] array(int size) {
        return new Foo[size];
    }
}
