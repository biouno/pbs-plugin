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
package jenkins.plugins.pbs;

import hudson.AbortException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.IOException;

import jenkins.plugins.pbs.slaves.PBSSlaveComputer;
import jenkins.plugins.pbs.tasks.Qsub;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.tupilabs.pbs.util.PBSException;

/**
 * PBS build step.
 * @since 0.1
 */
public class PBSBuilder extends Builder {

	@Extension
	public static final PBSBuilderDescriptor DESCRIPTOR = new PBSBuilderDescriptor();
	
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
		listener.getLogger().println("Submitting PBS job...");
		
		if (!(Computer.currentComputer() instanceof PBSSlaveComputer)) {
			throw new AbortException("You need  PBS Slave Computer in order to submit PBS jobs");
		}
		
		int numberOfDays = ((PBSBuilderDescriptor)this.getDescriptor()).getNumberOfDays();
		long span = ((PBSBuilderDescriptor)this.getDescriptor()).getSpan();
		
		Qsub submit = new Qsub(getScript(), numberOfDays, span, listener);
		try {
			launcher.getChannel().call(submit);
		} catch (PBSException e) {
			listener.fatalError(e.getMessage(), e);
			throw new AbortException(e.getMessage());
		}
		return true;
	}
	
	public static class PBSBuilderDescriptor extends BuildStepDescriptor<Builder> {

		private Integer numberOfDays;
		private Long span;

		public PBSBuilderDescriptor() {
			super();
			load();
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject json)
				throws hudson.model.Descriptor.FormException {
			this.numberOfDays = json.getInt("numberOfDays");
			this.span = json.getLong("span");
			return true;
		}

		@SuppressWarnings("rawtypes")
		// Jenkins API
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

		public Long getSpan() {
			if (this.span == null || this.span < 0)
				return new Long(300);
			return this.span;
		}

	}
	
}
