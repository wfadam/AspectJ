package aop;

import javaapi.*;
import com.advantest.kei.*;
import java.util.*;
import java.io.*;
import org.aspectj.lang.JoinPoint;

privileged aspect DieSort extends Advice {
	final private static int defaultBin = 1; // set when no hit in any ds_bin#.txt
	final private static int unReadableBin = 7; // set when char is not any of [A-Z0-9.]
	final private static List<Integer> binArr = new ArrayList<Integer>(){{
		for( int bin=1; bin<=7; bin++ ) {
			switch( bin ) {
				case defaultBin:
				case unReadableBin:
					continue;
				default:
					add( bin );
			}
		}}
	};
	final private static boolean[] enableWXYL = { true, true, true, true }; //W,X,Y,LOTID
	private static Map<Integer, Set<String>> binMap;

	int around(int sbin) : call( * javaapi.Bin.toSort(..)) && args( sbin ) {
		if( sbin > 8 ) { 
			return proceed( sbin );// use original bin scramble
		}
		return sbin;// directly return soft bin as hard bin
	}

	before() : call(public void javaapi.TestItem.setCategory() ) && within ( javaapi.TestItem+ ) {
		if( ! SS.currentTestName.toUpperCase().contains("_DIE_XY_LOC_")) {
			return;
		}
		printJP( this, thisJoinPoint );
		this.binMap = loadBinMap();

		setBin( getUnReadableDut(), unReadableBin);

		for( int bin : binArr ) {
			List<Integer> dutlst = getDutOfBin( bin );
			setBin( dutlst, bin);
		}

		setBin( KTestSystem.getDut( KDutGroupType.CDUT ), defaultBin);
	}

	after() throws NoMoreTargetDutException : call(public void javaapi.TestItem.execute() )  {//forces test program stop
		if (KTestSystem.getDut(KDutGroupType.MDUT).length == 0) {
			printJP( this, thisJoinPoint );
			throw new NoMoreTargetDutException();
		}
	}

	final static private List<Integer> getUnReadableDut() {
		List<Integer> dlst = new ArrayList<Integer>();
NEXTDUTLP:
		for( int dut : KTestSystem.getDut( KDutGroupType.CDUT )) {
			for (int channel = 1; channel <= DeviceInfo.CHANNEL_COUNT; channel++) {
				for (int chip = 1; chip <= DeviceInfo.MULTIDIECHIPS; chip++) {
					String wxyL = getWXYLString( dut, channel, chip );
					if ( ! isValid( wxyL )) {
						dlst.add( dut );
						continue NEXTDUTLP;
					}
				}
			}
		}
		return dlst;
	}

	final static private List<Integer> getDutOfBin( int bin ) {
		Set<String> dsSet = binMap.get( bin );

		List<Integer> dlst = new ArrayList<Integer>();
NEXTDUT:
		for( int dut : KTestSystem.getDut( KDutGroupType.CDUT )) {
			for (int channel = 1; channel <= DeviceInfo.CHANNEL_COUNT; channel++) {
				for (int chip = 1; chip <= DeviceInfo.MULTIDIECHIPS; chip++) {
					String wxyL = getWXYLString( dut, channel, chip );
					if ( dsSet.contains( wxyL )) {
						dlst.add( dut );
						continue NEXTDUT;
					}
				}
			}
		}

		return dlst;
	}

	private static void setBin( int[] tgtDut, int bin) {
		for( int dut : tgtDut ) {
			setBin( dut, bin );
		}
	}

	private static void setBin( List<Integer> tgtDut, int bin) {
		for( int dut : tgtDut ) {
			setBin( dut, bin );
		}
	}

	private static void setBin( int dut, int bin) {
		//print( "setBin( DUT=%d, bin=%d )\n", dut, bin );
		try{
			SS.dutInfo.DUTList[dut - 1].currentTestPF = DUTInfo.PF.PASS;
			SS.dutInfo.addFailedTest(dut, bin);
			SS.dutInfo.setSortNumber(dut, bin);
			KSort.write(dut, bin);
			DutExclusion.setPermanent(dut);
		} catch( NoMoreTargetDutException e ) {
			KDeviceTestProgram dp = KDeviceTestProgram.getCurrent();
			if ( null != dp ) {
				((IKNoMoreTargetDutAction)dp).noMoreTargetDutAction();
			}
		}
	}

	private static Map<Integer,Set<String>> loadBinMap() {
		return new HashMap<Integer, Set<String>>(){{
			for( int bin : binArr ) {
				put( bin, loadFile( "RandomData/ds_bin"+ bin +".txt" ));
			}
		}};
	}

	final static private String getWXYLString( int dut, int channel, int chip ) {
		DUTInfo.DUT.LotInfo lot = SS.dutInfo.DUTList[dut - 1].lotInfo;
		StringBuilder sb = new StringBuilder();
		if( enableWXYL[0] ) {
			sb.append( (char)lot.waferNoUpper[channel-1][chip-1] );
			sb.append( (char)lot.waferNoLower[channel-1][chip-1] );
		}
		if( enableWXYL[1] ) {
			sb.append( (char)lot.xLocUpper[channel-1][chip-1]    );   
			sb.append( (char)lot.xLocLower[channel-1][chip-1]    );   	
		}
		if( enableWXYL[2] ) {
			sb.append( (char)lot.yLocUpper[channel-1][chip-1]    );   
			sb.append( (char)lot.yLocLower[channel-1][chip-1]    );   
		}
		if( enableWXYL[3] ) {
			for( int i : lot.lotId[channel-1][chip-1] ) {
				sb.append( (char)i );
			}
		}
		return sb.toString().toUpperCase();
	}

	final static private Set<String> loadFile ( String fName ) {
		Set<String> ds = new HashSet<String>();
		File f = new File( fName );
		if( f.isFile()) {
			try{
				BufferedReader br = new BufferedReader( new FileReader(f) );
				String line = null;
				while( (line=br.readLine()) != null ) {
					String trimmed = line.replaceAll("[ \t,]+", "");
					ds.add( trimmed.toUpperCase());
					//System.out.println( trimmed );
				}
				br.close();
				print("Loaded %s for die sorting\n", fName );
			} catch( Exception e ) {
				throw new RuntimeException( e );
			}
		}
		return ds;
	}
	final static private boolean isValid( String str ) {
		return str.matches("[A-Z0-9.]+");
	}
}

