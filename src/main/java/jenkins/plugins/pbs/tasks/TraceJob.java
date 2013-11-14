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

import hudson.remoting.Callable;
import jenkins.plugins.pbs.model.PBSJob;

import com.tupilabs.pbs.PBS;
import com.tupilabs.pbs.util.CommandOutput;

/**
 * tracejob command.
 * @since 0.1
 */
public class TraceJob implements Callable<PBSJob, Throwable>{

	private static final long serialVersionUID = -3021968398281203773L;
	
	private final String jobId;
	private final int numberOfDays;

	public TraceJob(String jobId, int numberOfDays) {
		this.jobId = jobId;
		this.numberOfDays = numberOfDays;
	}
	
	public String getJobId() {
		return jobId;
	}
	
	public int getNumberOfDays() {
		return numberOfDays;
	}
	
	public PBSJob call() throws Throwable {
		final CommandOutput commandOutput = PBS.traceJob(jobId, this.numberOfDays);
		return new PBSJob(jobId, commandOutput.getOutput(), commandOutput.getError());
	}

}
