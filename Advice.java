package aop;

import com.advantest.kei.*;
import java.util.*;
import org.aspectj.lang.JoinPoint;

abstract aspect Advice {
	private static StringBuilder buf = new StringBuilder();
	final public static String prompt = "AJ> ";
	pointcut testEnd()	: execution(public void KDeviceTestProgram+.testEndAction());

	after() : testEnd() {
		if( null != buf ) {
			printJP( this, thisJoinPoint );
			System.out.println(prompt+"========================== Health Check Result Begin ==========================");
			System.out.println( buf );
			System.out.println(prompt+"========================== Health Check Result End   ==========================");
			buf = null;
		}
	}

	public static void print( String fmt, Object...args ) {
		StringBuilder sb = new StringBuilder();
		sb.append( prompt );
		sb.append( String.format(fmt, args));

		System.out.printf( sb.toString() );
		//buf.append( sb );
	}

	public static void print( Object o ) {
		if( null == o ) {
			return;
		}
		System.out.println( prompt + o.toString() );
		//buf.append( prompt + o.toString() );
	}


	public static void printJP ( Advice ad, JoinPoint jp ) {
		JoinPoint.StaticPart js = jp.getStaticPart();
		StringBuilder sb = new StringBuilder();
		sb.append( ad );
		sb.append( " --> " ); sb.append( js );
		sb.append( " --> " ); sb.append( js.getSourceLocation() );
		print("%s\n", sb );
	}


}

