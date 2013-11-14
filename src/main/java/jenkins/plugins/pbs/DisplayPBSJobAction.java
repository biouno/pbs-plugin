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

import hudson.Extension;
import hudson.model.RootAction;
import hudson.model.Computer;
import hudson.model.View;

import java.io.IOException;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import jenkins.plugins.pbs.PBSBuilder.PBSBuilderDescriptor;
import jenkins.plugins.pbs.model.PBSJob;
import jenkins.plugins.pbs.slaves.PBSSlaveComputer;
import jenkins.plugins.pbs.tasks.TraceJob;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Action used to display information on a PBS Job submitted to a
 * {@link PBSSlaveComputer}.
 * @since 0.1
 */
@Extension
public class DisplayPBSJobAction implements RootAction {

	public String getIconFileName() {
		return null;
	}

	public String getDisplayName() {
		return "/pbsJob";
	}

	public String getUrlName() {
		return "/pbsJob";
	}
	
	public View getOwner() {
		return Jenkins.getInstance().getPrimaryView();
	}
	
	public void doIndex(StaplerRequest request, StaplerResponse response) throws ServletException, IOException {
		String jobId = request.getParameter("jobId");
		String sNumberOfDays = request.getParameter("numberOfDays");
		int numberOfDays = 1;
		if (StringUtils.isNotBlank(sNumberOfDays)) {
			numberOfDays = Integer.parseInt(sNumberOfDays);
		} else {
			PBSBuilderDescriptor descriptor = (PBSBuilderDescriptor) Jenkins.getInstance().getDescriptor(PBSBuilder.class);
			numberOfDays = descriptor.getNumberOfDays();
		}
		String slaveName = request.getParameter("slaveName");
		
		Computer computer = Jenkins.getInstance().getComputer(slaveName);
		if (!(computer instanceof PBSSlaveComputer)) {
			throw new RuntimeException(String.format("%s is not a PBS Slave!", computer.getName()));
		}
		PBSSlaveComputer pbsSlaveComputer = (PBSSlaveComputer) computer;
		
		TraceJob traceJob = new TraceJob(jobId, numberOfDays);
		
		PBSJob pbsJob = null;
		try {
			pbsJob = pbsSlaveComputer.getChannel().call(traceJob);
			
			request.setAttribute("output", pbsJob.getOut());
			request.setAttribute("error", pbsJob.getErr());
			request.getView(this, "index.jelly").forward(request, response);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
}
