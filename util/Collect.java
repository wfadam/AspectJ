package util;
import java.util.*;

public class Collect {

        public static Collection<Integer> filter(Collection<Integer> src, Function f ) {
                Collection<Integer> outC = new ArrayList<Integer>();
                Function3 f3 = (Function3)f;
                for( int v : src ) {
                        if( f3.doIt( v )) {
                                outC.add( v );
                        }
                }
                return outC;
        }

        public static Collection<Integer> filter(int[] src, int st, int len, Function f ) {
                Collection<Integer> outC = new ArrayList<Integer>();
                Function3 f3 = (Function3)f;
                for( int i=st; i<st+len; i++ ) {
                        if( f3.doIt( src[ i ] )) {
                                outC.add( i );
                        }
                }
                return outC;
        }

        public static Collection<Integer> filter(int[] src, Function f ) {
                return filter( src, 0, src.length, f );
        }

        public static <T> Collection<T> map(Collection<T> c, Function f ) {
                Collection<T> outC = new ArrayList<T>();
                Function1<T> f1 = (Function1)f;
                for ( T i : c ) {
                        outC.add( f1.doIt( i ) );
                }
                return outC;
        }

        public static <T,V> Collection<V> trans(Collection<T> c, Function f ) {
                Collection<V> outC = new ArrayList<V>();
                FuncTrans<T,V> ft = (FuncTrans)f;
                for ( T i : c ) {
                        outC.add( ft.doIt( i ) );
                }
                return outC;
        }


}
