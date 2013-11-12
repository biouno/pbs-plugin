package jenkins.plugins.pbs.slaves;

import hudson.Extension;
import hudson.model.RootAction;
import hudson.model.View;

import java.io.IOException;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.tupilabs.pbs.PBS;
import com.tupilabs.pbs.util.CommandOutput;

@Extension
public class DisplayPBSJobAction implements RootAction {

	private int numberOfDays;

	public DisplayPBSJobAction() {
		this.numberOfDays = 1;
	}
	
	public int getNumberOfDays() {
		return numberOfDays;
	}
	
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
		// TODO: get logger
		String jobId = request.getParameter("jobId");
		if (StringUtils.isNotBlank(jobId)) {
			String numberOfDays = request.getParameter("numberOfDays");
			if (StringUtils.isNotBlank(numberOfDays))
				this.numberOfDays = Integer.parseInt(numberOfDays);
			CommandOutput commandOutput = PBS.traceJob(jobId, this.numberOfDays);
			request.setAttribute("output", commandOutput.getOutput());
			request.setAttribute("error", commandOutput.getError());
			request.getView(this, "index.jelly").forward(request, response);
			//return new PBSJob(jobId, commandOutput.getOutput(), commandOutput.getError());
		}
	}
	
}
