package aop;

import javaapi.*;
import com.advantest.kei.*;
import java.util.*;
import org.aspectj.lang.JoinPoint;


privileged aspect TimeOut extends Advice {
	final private static Map<String, Integer> timeOutMap = new HashMap<String, Integer>() {{
		put("tb_120_abcd_screen_nvcc"                 , 400 ); // time out unit : sec
	}};

	pointcut isTargetTB()   : if( timeOutMap.get( SS.currentTestName) !=null );
	pointcut bodyRun()      : isTargetTB() && call(* javaapi.TestItem+.body(..));
	pointcut normalPatRun() : isTargetTB() && call(* KAlpgPatternControl.startFunctionalTest()) && within( javaapi.*) ;
	pointcut timedPatRun()  : isTargetTB() && call(* KAlpgPatternControl.startFunctionalTest(int)) && within( javaapi.*) ;
	private long t0 = 0;
	private Integer maxTime = null;
	private boolean isTimeOut = false;
	private TestItem tb = null;

	before( TestItem o ) : bodyRun() && target(o) {
		t0 = System.currentTimeMillis();
		maxTime = timeOutMap.get( SS.currentTestName);
		isTimeOut = false;
		tb = o;
	}

	after() throws NoMoreTargetDutException : bodyRun() {
		if( isTimeOut ) {
			for( int dut : KTestSystem.getDut( KDutGroupType.MDUT )){
				SS.dutInfo.DUTList[dut-1].currentTestPF = DUTInfo.PF.FAIL;
				DutExclusion.setPermanent(dut);
			}	
		}
	}

	void around() : normalPatRun() {
		isTimeOut = runPat( timeLeft()) ? true : isTimeOut;
	}

	boolean around() : timedPatRun() {
		boolean tOut = runPat( timeLeft());
		isTimeOut = tOut ? true : isTimeOut;
		return tOut;
	}

	after() : normalPatRun() || timedPatRun() {
		if( isTimeOut ) {
			tb.fail = DeviceInfo.Timeout_Bin;
		}
	}

	private boolean runPat(int lmt) {
		if( lmt>0 ) {
			printPatInfo( String.format("is started with timeout = %d[s]", lmt) );

			boolean isTooLong = KAlpgPatternControl.startFunctionalTest( lmt );
			if( isTooLong ) {
				printPatInfo( "is stopped due to timeout" );
			}
			return isTooLong;
		}

		return false;
	}

	private int timeLeft(){
		return ( null == maxTime ) ? 0 : maxTime-(int)((System.currentTimeMillis()-t0)/1000);
	}

	private void printPatInfo(String msg){
		print("%s:%s#%X %s\n", SS.currentTestName, KAlpgPatternControl.getPatternProgramName(), KAlpgPatternControl.getStartPc(), msg);
	}

}

