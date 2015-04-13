package aop;

import javaapi.*;
import com.advantest.kei.*;
import java.util.*;
import java.io.*;
import org.aspectj.lang.JoinPoint;
import util.*;
import org.aspectj.lang.reflect.AdviceSignature;

privileged aspect ViewFAM extends Advice {

	pointcut isTargetTB() : if ( true ); //if ( SS.currentTestName.equals("") );
	final static List<Integer> targetDUT = Arrays.asList( 31, 32 );
	final int ofstX = 0x550*0x100;

	final int IOCH = 0;
	final int chSize = DeviceInfo.NANDBLOCKS*DeviceInfo.MAXCHIPS_IN_PKG;
	final KAddressLinkType direction = KAddressLinkType.Y_TO_X;
	CBox cb;

	int xBitCnt;
	int yBitCnt;
	int xSize;
	int ySize;
	KAddressRegion ar;

	after() : isTargetTB() && call(public * KAlpgPatternControl.startFunctionalTest(..)) {
		cb = CBox.build( KFailAddressMemory.getAppliedData() );
		if( ! (Boolean)cb.get( "enable" ) ) {
			return;
		}

		printJP( this, thisJoinPoint );
		print("Pattern:%s#%X\n", KAlpgPatternControl.getPatternProgramName(), KAlpgPatternControl.getStartPc());

		resize();
		dumpFAM();
	}

	private void dumpFAM() {

		print( "Data 00 is left blank during displaying" );
		print( "Lines that with all data 00 is not displayed" );
		for( int dut : KTestSystem.getDut( KDutGroupType.CDUT )) {
			if( targetDUT.contains( dut ) ) {
				int[] data = new int[ this.ySize * this.xSize ];
				print("DUT%02d FAM Dumping ...\n", dut );
				KFailAddressMemory.read( dut, IOCH, ar, direction, data );
				fmtOut( data );
			} else {
				print("DUT%02d is not specified for displaying\n", dut );
			}
		}
	}

	private void resize(){
		cb = CBox.build( KFailAddressMemory.getAppliedData() );
		int i = 0;
		for( KSignalType sig : (KSignalType[])cb.get("mainAddress") ) {
			print("AFMA%02d ( %s )\n", i++, sig.name());
		}

		xBitCnt = (Integer)cb.get( "xAddrBitCount" );	
		yBitCnt = (Integer)cb.get( "yAddrBitCount" ); 
		xSize = (1<<xBitCnt);
		ySize = (1<<yBitCnt);

		print( "X=%d [0, 0x%X]\n", xBitCnt, xSize-1);
		print( "Y=%d [0, 0x%X]\n", yBitCnt, ySize-1);
		print( "%s\n", direction );

		if( xBitCnt>16 || yBitCnt> 16 ) {
			xBitCnt = (xBitCnt>16) ? 16 : xBitCnt;
			yBitCnt = (yBitCnt>16) ? 16 : yBitCnt;
			xSize = (1<<xBitCnt);
			ySize = (1<<yBitCnt);

			print( "Resized: X=%d [0, 0x%X]\n", xBitCnt, xSize-1);
			print( "Resized: Y=%d [0, 0x%X]\n", yBitCnt, ySize-1);
		}

		ar = new KAddressRegion( 0+ofstX, xSize, 0, ySize );
	}

	private void fmtOut( int[] data ) {
		int maxDataInLine = ySize; //16
		StringBuilder sb = new StringBuilder();
		int i = 0 ;
		int sum = 0;
		for( int v : data ) {
			if( 0 == (i%maxDataInLine) ) {
				if( sum > 0 ) {
					print( sb );
					sum = 0;
				}
				sb = new StringBuilder(String.format("X:%04X Y:%04X >", i/ySize + ofstX, i%ySize));
			}

			sum += v;
			sb.append( (0==v) ? " __" : String.format(" %02X", v ) );
			i++;
		}
		if( sum > 0 ) {
			print( sb );
		}
	}

}



