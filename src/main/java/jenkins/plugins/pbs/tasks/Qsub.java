/*
 * The MIT License
 *
 * Copyright (c) <2012-2013> <Bruno P. Kinoshita>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.plugins.pbs.tasks;

import hudson.model.BuildListener;
import hudson.remoting.Callable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.tupilabs.pbs.PBS;
import com.tupilabs.pbs.util.CommandOutput;
import com.tupilabs.pbs.util.PBSException;

/**
 * qsub command.
 * @since 0.1
 */
public class Qsub implements Callable<Boolean, PBSException> {

	private static final long serialVersionUID = -8294426519319612072L;
	
	private static final String REGEX_JOB_STATUS = "dequeuing .*, state (.*)$";
	private static final Pattern JOB_STATUS_REGEX = Pattern.compile(REGEX_JOB_STATUS, Pattern.MULTILINE);
	
	private final String script;
	private final int numberOfDays;
	private final long span;
	private final BuildListener listener;

	/**
	 * Create a new qsub command.
	 * @param script
	 * @param numberOfDays
	 * @param span
	 * @param listener
	 */
	public Qsub(String script, int numberOfDays, long span, BuildListener listener) {
		this.script = script;
		this.numberOfDays = numberOfDays;
		this.span = span;
		this.listener = listener;
	}
	
	public Boolean call() {
		FileWriter writer = null;
		try {
			File tmpScript = File.createTempFile("pbs", "script");
			writer = new FileWriter(tmpScript);
			writer.write(script);
			writer.flush();
			writer.close();
			listener.getLogger().println("PBS script: " + tmpScript.getAbsolutePath());
			String jobId = PBS.qsub(tmpScript.getAbsolutePath());
			
			listener.getLogger().println("PBS Job submitted: " + jobId);
			
			return this.seekEnd(jobId, numberOfDays, span);
		} catch (IOException e) {
			throw new PBSException("Failed to create temp script");
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// adapted from https://github.com/jenkinsci/call-remote-job-plugin/blob/master/src/main/java/org/ukiuni/callOtherJenkins/CallOtherJenkins/JenkinsRemoteIF.java
	private boolean seekEnd(String jobId, int numberOfDays, long span) {
		CommandOutput cmd = PBS.traceJob(jobId, numberOfDays);
		
		String out = cmd.getOutput();
		String err = cmd.getError();
		
		if (StringUtils.isBlank(out)) {
			listener.getLogger().println("Could not find job " + jobId + " in PBS logs...Marking build as UNSTABLE");
			listener.getLogger().println(err);
			return false;
		}
		
		listener.getLogger().println("Seeking job end...");
		return this.loopSeek(jobId);
	}
	
	private boolean loopSeek(String jobId) {
		while (true) {
			CommandOutput cmd = PBS.traceJob(jobId, numberOfDays);
			
			String out = cmd.getOutput();
			//String err = cmd.getError();
			
//			listener.getLogger().println(out);
//			listener.getLogger().println("----");
			Matcher matcher = JOB_STATUS_REGEX.matcher(out.toString());
			if (matcher.find()) {
				String state = matcher.group(1);
				listener.getLogger().println("Found job state " + state);
				if ("COMPLETE".equals(state))
					return true;
				return false;
			}
			try {
				//listener.getLogger().println("Sleeping for " + span + "ms");
				Thread.sleep(span);
			} catch (InterruptedException e) {}
		}
	}

//	public static void main(String[] args) {
//		String out = "Job: 128.localhost\n" + 
//				"\n" + 
//				"11/10/2013 12:52:40  S    enqueuing into batch, state 1 hop 1\n" + 
//				"11/10/2013 12:52:40  S    Job Queued at request of kinow@localhost, owner =\n" + 
//				"                          kinow@localhost, job name = jenkins_job, queue =\n" + 
//				"                          batch\n" + 
//				"11/10/2013 12:52:40  S    Job Modified at request of Scheduler@localhost\n" + 
//				"11/10/2013 12:52:40  L    Job Run\n" + 
//				"11/10/2013 12:52:40  S    Job Run at request of Scheduler@localhost\n" + 
//				"11/10/2013 12:52:40  S    Not sending email: User does not want mail of this\n" + 
//				"                          type.\n" + 
//				"11/10/2013 12:52:40  A    queue=batch\n" + 
//				"11/10/2013 12:52:40  A    user=kinow group=kinow jobname=jenkins_job queue=batch\n" + 
//				"                          ctime=1384095160 qtime=1384095160 etime=1384095160\n" + 
//				"                          start=1384095160 owner=kinow@localhost\n" + 
//				"                          exec_host=chuva/0 Resource_List.neednodes=1\n" + 
//				"                          Resource_List.nodect=1 Resource_List.nodes=1\n" + 
//				"                          Resource_List.walltime=240:00:00 \n" + 
//				"11/10/2013 12:53:40  S    Not sending email: User does not want mail of this\n" + 
//				"                          type.\n" + 
//				"11/10/2013 12:53:40  S    Exit_status=0 resources_used.cput=00:00:00\n" + 
//				"                          resources_used.mem=3196kb resources_used.vmem=31756kb\n" + 
//				"                          resources_used.walltime=00:01:00\n" + 
//				"11/10/2013 12:53:40  S    dequeuing from batch, state COMPLETE\n" + 
//				"11/10/2013 12:53:40  M    scan_for_terminated: job 128.localhost task 1\n" + 
//				"                          terminated, sid=4801\n" + 
//				"11/10/2013 12:53:40  M    job was terminated\n" + 
//				"11/10/2013 12:53:40  M    obit sent to server\n" + 
//				"11/10/2013 12:53:40  A    user=kinow group=kinow jobname=jenkins_job queue=batch\n" + 
//				"                          ctime=1384095160 qtime=1384095160 etime=1384095160\n" + 
//				"                          start=1384095160 owner=kinow@localhost\n" + 
//				"                          exec_host=chuva/0 Resource_List.neednodes=1\n" + 
//				"                          Resource_List.nodect=1 Resource_List.nodes=1\n" + 
//				"                          Resource_List.walltime=240:00:00 session=4801\n" + 
//				"                          end=1384095220 Exit_status=0\n" + 
//				"                          resources_used.cput=00:00:00 resources_used.mem=3196kb\n" + 
//				"                          resources_used.vmem=31756kb\n" + 
//				"                          resources_used.walltime=00:01:00\n" + 
//				"";
//		
//		SubmitPbsJob submit = new SubmitPbsJob("", 10, 10, System.out);
//		submit.loopSeek(out);
//	}
	
}