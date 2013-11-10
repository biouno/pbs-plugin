/*
 * The MIT License
 *
 * Copyright (c) <2012> <Bruno P. Kinoshita>
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
package jenkins.plugins.pbs.slaves;

import hudson.AbortException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.remoting.Callable;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.tupilabs.pbs.PBS;
import com.tupilabs.pbs.util.CommandOutput;
import com.tupilabs.pbs.util.PBSException;

/**
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 0.1
 */
public class PBSBuilder extends Builder {

	@Extension
	public static final PBSBuilderDescriptor descriptor = new PBSBuilderDescriptor();
	
	private static final String REGEX_JOB_STATUS = "dequeuing .*, state (.*)";
	private static final Pattern JOB_STATUS_REGEX = Pattern.compile(REGEX_JOB_STATUS);
	
	/**
	 * PBS script.
	 */
	private final String script;
	
	@DataBoundConstructor
	public PBSBuilder(String script) {
		super();
		this.script = script;
	}
	
	public String getScript() {
		return script;
	}
	
	/* (non-Javadoc)
	 * @see hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.AbstractBuild, hudson.Launcher, hudson.model.BuildListener)
	 */
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			final BuildListener listener) throws InterruptedException, IOException {
		// TODO: qsub a script
		listener.getLogger().println("Submitting PBS job...");
		
		int numberOfDays = ((PBSBuilderDescriptor)this.getDescriptor()).getNumberOfDays();
		int span = ((PBSBuilderDescriptor)this.getDescriptor()).getSpan();
		
		SubmitPbsJob submit = new SubmitPbsJob(getScript(), numberOfDays, span, listener);
		try {
			launcher.getChannel().call(submit);
		} catch (PBSException e) {
			listener.fatalError(e.getMessage(), e);
			throw new AbortException(e.getMessage());
		}
		return true;
	}
	
	protected static final class SubmitPbsJob implements Callable<Result, PBSException> {

		private static final long serialVersionUID = -8294426519319612072L;
		
		private final String script;
		private final int numberOfDays;
		private final long span;
		private final BuildListener listener;

		public SubmitPbsJob(String script, int numberOfDays, long span, BuildListener listener) {
			this.script = script;
			this.numberOfDays = numberOfDays;
			this.span = span;
			this.listener = listener;
		}
		
		public Result call() {
			FileWriter writer = null;
			try {
				File tmpScript = File.createTempFile("pbs", "script");
				writer = new FileWriter(tmpScript);
				writer.write(script);
				writer.flush();
				writer.close();
				listener.getLogger().println("PBS script: " + tmpScript.getAbsolutePath());
				String jobId = PBS.qsub(tmpScript.getAbsolutePath());
				
				Result result = this.seekEnd(listener.getLogger(), jobId, numberOfDays, span);
				return result;
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
		private Result seekEnd(PrintStream logger, String jobId, int numberOfDays, long span) {
			CommandOutput cmd = PBS.traceJob(jobId, numberOfDays);
			
			String out = cmd.getOutput();
			String err = cmd.getError();
			
			if (StringUtils.isBlank(out)) {
				listener.getLogger().println("Could not find job " + jobId + " in PBS logs...Marking build as UNSTABLE");
				listener.getLogger().println(err);
				return Result.UNSTABLE;
			}
			
			listener.getLogger().println("Seeking job end...");
			while (true) {
				Matcher matcher = JOB_STATUS_REGEX.matcher(out);
				if (matcher.matches()) {
					String state = matcher.group(1);
					listener.getLogger().println("Found job state " + state);
					if ("COMPLETE".equals(state))
						return Result.SUCCESS;
					return Result.UNSTABLE;
				}
			}
		}
		
	}
	
	public static final class PBSBuilderDescriptor extends BuildStepDescriptor<Builder> {

		private Integer numberOfDays;
		private Integer span;
		
		public PBSBuilderDescriptor() {
			super();
			load();
		}
		
		@Override
		public boolean configure(StaplerRequest req, JSONObject json)
				throws hudson.model.Descriptor.FormException {
			this.numberOfDays = json.getInt("numberOfDays");
			this.span = json.getInt("span");
			return true;
		}
		
		@SuppressWarnings("rawtypes") // Jenkins API
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Submit PBS job";
		}
		
		public Integer getNumberOfDays() {
			if (this.numberOfDays == null || this.numberOfDays < 0)
				return new Integer(1);
			return this.numberOfDays;
		}
		
		public Integer getSpan() {
			if (this.span == null || this.span < 0)
				return new Integer(30);
			return this.span;
		}
		
	}
	
}
