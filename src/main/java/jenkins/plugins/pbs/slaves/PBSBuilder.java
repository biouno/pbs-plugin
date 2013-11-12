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
import hudson.model.Computer;
import hudson.tasks.Builder;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

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
		listener.getLogger().println("Submitting PBS job...");
		
		if (!(Computer.currentComputer() instanceof PBSSlaveComputer)) {
			throw new AbortException("You need  PBS Slave Computer in order to submit PBS jobs");
		}
		
		int numberOfDays = ((PBSBuilderDescriptor)this.getDescriptor()).getNumberOfDays();
		long span = ((PBSBuilderDescriptor)this.getDescriptor()).getSpan();
		
		SubmitPbsJob submit = new SubmitPbsJob(getScript(), numberOfDays, span, listener);
		try {
			launcher.getChannel().call(submit);
		} catch (PBSException e) {
			listener.fatalError(e.getMessage(), e);
			throw new AbortException(e.getMessage());
		}
		return true;
	}
	
}
