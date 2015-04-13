package aop;

import javaapi.*;
import com.advantest.kei.*;
import java.util.*;
import org.aspectj.lang.JoinPoint;

privileged aspect TestFlowCheck extends Advice {

	pointcut isOldStyle() : cflowbelow( execution(public static void javaapi.*.main(..)));

	int tbIdx = -1;
	Map<String, List<TestFlow>> flowMap = new HashMap<String, List<TestFlow>>();

	//===================== Aspects for old-style programs ====================//
	TestItem[] around(TestFlow[] tf) : isOldStyle() && call(public * javaapi.TestItemList.getList(..)) && args(tf) {
		printJP( this, thisJoinPoint );
		registerFlow( SS.testStep, tf );
		return proceed( tf );
	}

	before(TestItem tb) : isOldStyle() && execution(public * javaapi.TestItem+.body()) && target( tb ) {
		printJP( this, thisJoinPoint );
		tbIdx++;
		rejectIfNot( tb.name );
	}

	after() throws NoMoreTargetDutException : isOldStyle() && execution(public * javaapi.TestItem.execute()) { // stop testing immediately
		printJP( this, thisJoinPoint );
		if( KTestSystem.getDut(KDutGroupType.MDUT).length == 0 ) {
			throw new NoMoreTargetDutException();
		}
	}

	//===================== Aspects for new-style programs ====================//
	before(KTestItemRegistry entry, String name, TestItem[] items) : !isOldStyle() && call(protected final void KDeviceTestProgram.registerTestItem(..)) && args(entry, name, items) {
		printJP( this, thisJoinPoint );
		registerFlow( name, items );
	}

	before() throws NoMoreTargetDutException : !isOldStyle() && execution(public void KTestItem+.execute()) {
		printJP( this, thisJoinPoint );
		tbIdx++;
		rejectIfNot( KTestSystem.getTestName() );
	}

	private <T> void registerFlow(String flowName, T[] tf){
		List<TestFlow> flow = new ArrayList<TestFlow>();
		Class cls = tf.getClass();
		if( TestFlow[].class == cls) {
			for( T t : tf ) {
				flow.add( (TestFlow)t );
			}
		} else if ( TestItem[].class == cls ) {
			for( T t : tf ) {
				TestItem tb = (TestItem)t;
				flow.add( new TestFlow( tb.name, tb.pass, tb.fail));
			}
		} else {
			throw new RuntimeException("Unsupported array type " + cls );
		}
		flowMap.put( flowName, flow );
		print("Regsitered Test Flow : %s\n", SS.testStep );
	}


	private void rejectIfNot( String testing_name ) {

		List<TestFlow> flow = flowMap.get(SS.testStep);
		String expect_name  = (this.tbIdx < flow.size()) ? flow.get( this.tbIdx ).testName : "";

		if( ! testing_name.equals( expect_name ) ) {
			print("Unexpected %s ( expecting %s )\n", testing_name, expect_name );

			int sortNumber = javaapi.Bin.failOpenOrId;  
			int[] rejDut = KTestSystem.getDut(KDutGroupType.MDUT);
			for( int dut : rejDut ) {
				print("DUT%02d is forced to hard bin %d\n", dut, javaapi.Bin.toSort( sortNumber ));
				SS.dutInfo.DUTList[dut - 1].currentTestPF = DUTInfo.PF.FAIL;		
				SS.dutInfo.addFailedTest(dut, 999);
				SS.dutInfo.setSortNumber(dut, sortNumber);
			}
			KSort.write( rejDut, javaapi.Bin.toSort( sortNumber ));
			KDutExclusion.set( rejDut, KDutExclusionType.PERMANENT, KDutExclusionType.ELECTRICALLY);
		}
	}
}

