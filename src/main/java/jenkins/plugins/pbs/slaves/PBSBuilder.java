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
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.remoting.Callable;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

import com.tupilabs.pbs.PBS;
import com.tupilabs.pbs.util.PBSException;

/**
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 0.1
 */
public class PBSBuilder extends Builder {

	@Extension
	public static final PBSBuilderDescriptor descriptor = new PBSBuilderDescriptor();
	
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
		
		SubmitPbsJob submit = new SubmitPbsJob(getScript(), listener);
		try {
			launcher.getChannel().call(submit);
		} catch (PBSException e) {
			listener.fatalError(e.getMessage(), e);
			throw new AbortException(e.getMessage());
		}
		return true;
	}
	
	protected static final class SubmitPbsJob implements Callable<Void, PBSException> {

		private static final long serialVersionUID = -8294426519319612072L;
		
		private final String script;
		private final BuildListener listener;

		public SubmitPbsJob(String script, BuildListener listener) {
			this.script = script;
			this.listener = listener;
		}
		
		public Void call() {
			FileWriter writer = null;
			try {
				File tmpScript = File.createTempFile("pbs", "script");
				writer = new FileWriter(tmpScript);
				writer.write(script);
				writer.flush();
				writer.close();
				listener.getLogger().println("PBS script: " + tmpScript.getAbsolutePath());
				PBS.qsub(tmpScript.getAbsolutePath());
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
			return null;
		}
		
	}
	
	public static final class PBSBuilderDescriptor extends BuildStepDescriptor<Builder> {

		@SuppressWarnings("rawtypes") // Jenkins API
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Submit PBS job";
		}
		
	}
	
}
