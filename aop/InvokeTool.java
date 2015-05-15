package aop;

import java.io.*;
import java.util.*;
import ch.ethz.ssh2.*;
import com.advantest.kei.*;
import org.aspectj.lang.JoinPoint;

privileged aspect InvokeTool extends Advice {
	pointcut programStart (  )  : execution ( public void KDeviceTestProgram+.programStartAction (  )  ) ;
	pointcut testStart    (  )  : execution ( public void KDeviceTestProgram+.testStartAction    (  )  ) ;
	pointcut testEnd      (  )  : execution ( public void KDeviceTestProgram+.testEndAction      (  )  ) ;

	before() : programStart() {
		printJP( this, thisJoinPoint );
		String cmd = "klog --dc on --func off";

		File flock = new File("/home/kei/." + thisJoinPoint.toString());
		if ( flock.mkdir() ) {
			try {
				print( "Running command on remote machine --> Begin" );
				print( SSH.run( cmd ));
				print( "Running command on remote machine <-- End" );
			} finally {
				flock.delete();
			}
		}
	}

	after() : testEnd() {
		printJP( this, thisJoinPoint );
		String cmd = "ksinfo";

		String aliveCnt = SSH.run( "kstat  | grep '^Site .*TESTING' -c" );
		if ( aliveCnt.trim().equals("1") ) {
			print( "Running command on remote machine --> Begin" );
			print( SSH.run( cmd ) );
			print( "Running command on remote machine <-- End" );
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


	final private static String run( String cmd ) {
		if ( null == cmd || cmd.isEmpty() )	return "";

		try {
			SSH ssh = new SSH( "172.16.255.254", "kei", "haojie123" );
			if( ssh.connect() ) {
				String msg = ssh.executeCommand( cmd );
				ssh.logout();
				return msg;
			} else {
				System.out.println( "Cannot connect to remote machine" );
			}
		} catch( Exception e ) {
			e.printStackTrace();
		}
		return "";
	}

}
