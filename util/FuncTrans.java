package util;

public interface FuncTrans<T,V> extends Function {
        public V doIt( T t );
}

interface Function1<T> extends Function {
        public T doIt( T t );
}

interface Function2<T> extends Function {
        public boolean doIt( T t );
}

interface Function3 extends Function{
        public boolean doIt( int i );
}
