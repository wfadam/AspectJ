package aop;

import java.io.*;
import java.util.*;
import ch.ethz.ssh2.*;
import com.advantest.kei.*;
import org.aspectj.lang.JoinPoint;

aspect InvokeTool extends Advice {
	pointcut programStart (  )  : execution ( public void KDeviceTestProgram+.programStartAction (  )  ) ;
	pointcut testStart    (  )  : execution ( public void KDeviceTestProgram+.testStartAction    (  )  ) ;
	pointcut testEnd      (  )  : execution ( public void KDeviceTestProgram+.testEndAction      (  )  ) ;

	before() : programStart() {
		printJP( this, thisJoinPoint );
		exec( "klog --dc on" );
	}

	final private void exec( String cmd ) {
		if ( null == cmd || cmd.isEmpty() )	return;

		try {
			SSH ssh = new SSH( "172.16.255.254", "kei", "******" );
			if( ssh.connect() ) {
				print( ssh.executeCommand( cmd ) );
				ssh.logout();
			} else {
				print( "Cannot connect to remote machine" );
			}
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}
}

class SSH {

	private String hostname;
	private String username;
	private String password;
	private Connection connection;

	public SSH( String hostname, String username, String password ) {
		this.hostname = hostname;
		this.username = username;
		this.password = password;
	}

	public boolean connect() {
		try {
			connection = new Connection( hostname );
			connection.connect();

			boolean result = connection.authenticateWithPassword( username, password );
			//System.out.println( "Connection result: " + result );
			return result;
		} catch( Exception e ) {
			throw new RuntimeException( "An exception occurred while trying to connect to the host: " + hostname + ", Exception=" + e.getMessage(), e ); 
		}
	}

	public String executeCommand( String command ) {
		try {
			Session session = connection.openSession();
			session.execCommand( command );

			StringBuilder sb   = new StringBuilder();
			InputStream stdout = new StreamGobbler( session.getStdout() );
			BufferedReader br  = new BufferedReader(new InputStreamReader(stdout));
			String line = br.readLine();
			while( line != null ) {
				sb.append( line + "\n" );
				line = br.readLine();
			}

			//System.out.println( "ExitCode: " + session.getExitStatus() );
			session.close();

			return sb.toString();
		} catch( Exception e ) {
			throw new RuntimeException( "An exception occurred while executing command: " + command + ". Exception = " + e.getMessage(), e );
		} 
	}

	public void logout() {
		try {
			connection.close();
		} catch( Exception e ) {
			throw new RuntimeException( "An exception occurred while closing the SSH connection: " + e.getMessage(), e );
		}
	}

	public boolean isAuthenticationComplete() {
		return connection.isAuthenticationComplete();
	}


	//	final public static void run( String cmd ) {
	//		if ( null == cmd || cmd.isEmpty() )	return;
	//	
	//		try {
	//			SSH ssh = new SSH( "172.16.255.254", "kei", "******" );
	//			if( ssh.connect() ) {
	//				String diskInfo = ssh.executeCommand( cmd );
	//				ssh.logout();
	//			}
	//		} catch( Exception e ) {
	//			e.printStackTrace();
	//		}
	//	}

	//	public static void main( String[] args ) {
	//		SSH.run( "df -k" );
	//	}
}
