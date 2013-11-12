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

import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.slaves.SlaveComputer;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.tupilabs.pbs.PBS;
import com.tupilabs.pbs.model.Job;
import com.tupilabs.pbs.model.Queue;

/**
 * 
 * @author Bruno P. Kinoshita - http://www.kinoshita.eti.br
 * @since 0.1
 */
public class PBSSlaveComputer extends SlaveComputer {
	
	protected static final Logger LOGGER = Logger.getLogger(PBSSlaveComputer.class.getName());

	/**
	 * @param slave
	 */
	public PBSSlaveComputer(PBSSlave slave) {
		super(slave);
	}
	
	/* (non-Javadoc)
	 * @see hudson.slaves.SlaveComputer#getNode()
	 */
	@Override
	public PBSSlave getNode() {
		return (PBSSlave)super.getNode();
	}
	
	public List<Queue> getQueues() throws IOException, InterruptedException {
		Channel channel = getChannel();
		if (channel == null)
			return Collections.emptyList();
		List<Queue> queues = channel.call(new GetPBSQueues());
		return queues;
	}
	
	public List<Job> getJobs(Queue queue) throws IOException, InterruptedException {
		List<Job> jobs = getChannel().call(new GetPBSJobs(queue));
		return jobs;
	}
	
	private static final class GetPBSQueues implements Callable<List<Queue>,RuntimeException> {
		private static final long serialVersionUID = -9174853723996041340L;

		public List<Queue> call() {
            return PBS.qstatQueues();
        }
    }
	
	private static final class GetPBSJobs implements Callable<List<Job>,RuntimeException> {
		private static final long serialVersionUID = -8224467401457563865L;
		private final Queue queue;
		public GetPBSJobs(Queue queue) {
			this.queue = queue;
		}
        public List<Job> call() {
            List<Job> jobs = PBS.qstat(queue.getName());
            for(Job job : jobs) {
            	job.setId(job.getId().trim());
            }
            return jobs;
        }
    }

}
