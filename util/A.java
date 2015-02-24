import javassist.*;
import java.util.*;
//
//command line
//javac -Xlint -cp .:/home/kei/wufeng/junit_sample/lib/javassist.jar A.java && java -cp .:/home/kei/wufeng/junit_sample/lib/javassist.jar A
//
public class A {

        public static void main(String...args){

                try {
                        // be careful to check if the method signature is expected
                        CtClass ctclass = ClassPool.getDefault().get("com.advantest.kei.KDriverTimingEdge");
                        //CtMethod ctmd  = ctclass.getDeclaredMethod("set");
                        //System.out.println(ctmd.getLongName());
                        //ctclass.removeMethod( ctmd );
                        //ctmd.setModifiers( Modifier.PRIVATE );
                        //ctclass.writeFile();
                        CtMethod[] ctmd  = ctclass.getDeclaredMethods();
                        int i = 0;
                        Object o = new Object();
                        for( CtMethod m : ctmd ) {
                                String s = m.toString();
                                if( s.contains("transient set (I[D)V]") ||
                                        s.contains("transient set (I[Z)V]") ) {
                                        System.out.println( s );
                                        m.setModifiers( Modifier.PRIVATE );
                                }
                        }
                        ctclass.writeFile();
                } catch( Exception e ) {
                        throw new RuntimeException( e );
                }
        }
}
