package aop;

import javaapi.*;
import com.advantest.kei.*;
import java.util.*;
import java.io.*;
import org.aspectj.lang.JoinPoint;

privileged aspect DieSort extends Advice {
	final private static boolean[] enableWXYL = { true, true, true, true }; //W,X,Y,LOTID
	final private static int unReadableBin = 4; // bin# when any char is invalid
	final private static int defaultBin = 1; // bin# when no hit in any ds_bin#.txt

	//insertion for new WXYL struct
	public int[][] DUTInfo.DUT.wxyL = new int[DeviceInfo.MULTIDIECHIPS][15];//a8w: 2 + 2 + 2 + 9

	before() : isTargetTB() && call(public void javaapi.TestItem.setCategory() ) && within ( javaapi.TestItem+ ) {
		printJP( this, thisJoinPoint );

		setBin( unReadableBin, getDut( new Filter() {
					@Override public boolean isMatch( String str ) { return (! isValid( str )); }
					}));

		for( int bin : binLst ) {
			if ( binMap.get( bin ).isEmpty() ) continue;// skip if no ds_bin#.txt found

			final int b = bin;
			setBin( b, getDut( new Filter() {
						// Reject if any match
						@Override public boolean isMatch( String str ) { return binMap.get( b ).contains( str ); }

						// Reject if any unmatch
						//m8w@Override public boolean isMatch( String str ) { return ! binMap.get( b ).contains( str ); }
						}));
		}

		setBin( defaultBin, KTestSystem.getDut( KDutGroupType.CDUT ) );
	}

	after() throws NoMoreTargetDutException : call(public void javaapi.TestItem.execute() )  {//forces test program stop
		if (KTestSystem.getDut(KDutGroupType.MDUT).length == 0) {
			printJP( this, thisJoinPoint );
			throw new NoMoreTargetDutException();
		}
	}

	final static private List<Integer> getDut( Filter flt ) {
		List<Integer> dlst = new ArrayList<Integer>();
NEXTDUT:
		for( int dut : KTestSystem.getDut( KDutGroupType.CDUT )) {
			for (int channel = 1; channel <= DeviceInfo.CHANNEL_COUNT; channel++) {
				for (int chip = 1; chip <= DeviceInfo.MULTIDIECHIPS; chip++) {
					String wxyL = getWXYLString( dut, channel, chip );
					if ( flt.isMatch( wxyL )) {
						dlst.add( dut );
						continue NEXTDUT;
					}
				}
			}
		}

		return dlst;
	}


	private static void setBin( int bin, List<Integer> tgtDut ) {
		for( int dut : tgtDut ) {
			setBin( bin, dut );
		}
	}

	private static void setBin( int bin, int...tgtDut) {

		for( int dut : tgtDut ) {
			try{
				SS.dutInfo.DUTList[dut - 1].currentTestPF = DUTInfo.PF.PASS;
				SS.dutInfo.addFailedTest(dut, bin);
				SS.dutInfo.setSortNumber(dut, bin);
				KSort.write(dut, bin);
				print ( "DUT%02d is set to hard bin %d\n", dut, bin );
				DutExclusion.setPermanent(dut);
			} catch( NoMoreTargetDutException e ) {
				KDeviceTestProgram dp = KDeviceTestProgram.getCurrent();
				if ( null != dp ) {
					((IKNoMoreTargetDutAction)dp).noMoreTargetDutAction();
				}
			}
		}
	}

	//uses WXYL struct	final static private String getWXYLString( int dut, int channel, int chip ) {
	//uses WXYL struct		StringBuilder sb = new StringBuilder();
	//uses WXYL struct		DUTInfo.DUT.LotInfo lot = SS.dutInfo.DUTList[dut - 1].lotInfo;
	//uses WXYL struct		if( enableWXYL[0] ) {
	//uses WXYL struct			sb.append( (char)lot.waferNoUpper[channel-1][chip-1] );
	//uses WXYL struct			sb.append( (char)lot.waferNoLower[channel-1][chip-1] );
	//uses WXYL struct		}
	//uses WXYL struct		if( enableWXYL[1] ) {
	//uses WXYL struct			sb.append( (char)lot.xLocUpper[channel-1][chip-1]    );   
	//uses WXYL struct			sb.append( (char)lot.xLocLower[channel-1][chip-1]    );   	
	//uses WXYL struct		}
	//uses WXYL struct		if( enableWXYL[2] ) {
	//uses WXYL struct			sb.append( (char)lot.yLocUpper[channel-1][chip-1]    );   
	//uses WXYL struct			sb.append( (char)lot.yLocLower[channel-1][chip-1]    );   
	//uses WXYL struct		}
	//uses WXYL struct		if( enableWXYL[3] ) {
	//uses WXYL struct			for( int i : lot.lotId[channel-1][chip-1] ) {
	//uses WXYL struct				sb.append( (char)i );
	//uses WXYL struct			}
	//uses WXYL struct		}
	//uses WXYL struct		return sb.toString().toUpperCase();
	//uses WXYL struct	}

	//uses new WXYL struct
	final static private String getWXYLString( int dut, int channel, int chip ) {
		StringBuilder sb = new StringBuilder();
		int[] wxyL = SS.dutInfo.DUTList[dut - 1].wxyL[ chip - 1];
		if( enableWXYL[0] ) { //w
			sb.append( (char)wxyL[0] );
			sb.append( (char)wxyL[1] );
		}
		if( enableWXYL[1] ) { //x
			sb.append( (char)wxyL[2] );
			sb.append( (char)wxyL[3] );
		}
		if( enableWXYL[2] ) { //y
			sb.append( (char)wxyL[4] );
			sb.append( (char)wxyL[5] );
		}
		if( enableWXYL[3] ) { //l
			for( int i=6; i<wxyL.length; i++ ){
				sb.append( (char)wxyL[i] );
			}
		}
		return sb.toString().toUpperCase();
	}

	int around(int sbin) : call( * javaapi.Bin.toSort(..)) && args( sbin ) {
		if( sbin > 8 ) { 
			return proceed( sbin );// use original bin scramble
		}
		return sbin;
	}

	final private static Map<Integer,Set<String>> loadBinMap(final List<Integer> blst) {
		return new HashMap<Integer, Set<String>>(){{
			for( int bin : blst ) {
				put( bin, loadFile( "RandomData/ds_bin"+ bin +".txt" ));
			}
		}};
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

	interface Filter {
		public boolean isMatch( String str );
	}

	final private static List<Integer> binLst = new ArrayList<Integer>(){{
		for( int bin=1; bin<=7; bin++ ) {
			switch( bin ) {
				case defaultBin: case unReadableBin: continue;
				default: add( bin );
			}
		}
	}};
	final private static Map<Integer, Set<String>> binMap = loadBinMap(binLst);
	pointcut isTargetTB() : if( SS.currentTestName.toUpperCase().contains("_DIE_XY_LOC_"));

}

