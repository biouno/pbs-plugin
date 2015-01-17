/*
 * The MIT License
 *
 * Copyright (c) <2012-2015> <Bruno P. Kinoshita>
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.remoting.RoleChecker;

import com.tupilabs.pbs.PBS;
import com.tupilabs.pbs.util.CommandOutput;
import com.tupilabs.pbs.util.PBSException;

/**
 * qsub command.
 * @since 0.1
 */
public class Qsub implements Callable<Boolean, PBSException> {

    private static final long serialVersionUID = -8294426519319612072L;

    private static final String REGEX_JOB_STATUS = "JOB_SUBSTATE_(.*)$";
    private static final String REGEX_JOB_SUBSTATUS = "Exit_status=([0-9]+)";
    private static final Pattern JOB_SUBSTATUS_REGEX = Pattern.compile(REGEX_JOB_SUBSTATUS, Pattern.MULTILINE);
    private static final Pattern JOB_STATUS_REGEX = Pattern.compile(REGEX_JOB_STATUS, Pattern.MULTILINE);

    private final String script;
    private final int numberOfDays;
    private final long span;
    private final String runUser;
    private final BuildListener listener;
    private final Map<String, String> environment;
    private final String executionDirectory;
    private final String errFileName;
    private final String outFileName;

    /**
     * Create a new qsub command.
     * 
     * @param script
     * @param numberOfDays
     * @param span
     * @param listener
     */
    public Qsub(String script, int numberOfDays, long span, String runUser,
            String logHostname, String logBasename,
            Map<String, String> environment, BuildListener listener) {
        this.script = script;
        this.numberOfDays = numberOfDays;
        this.span = span;
        this.runUser = runUser;
        this.listener = listener;
        this.environment = environment;

        final String myLogBasename = (logBasename.length() > 0) ? logBasename : System.getenv("java.io.tmpdir");

        try {
            // If we are running as another user, we are going to make sure we
            // set permissions more loosely
            this.executionDirectory = Files.createTempDirectory(Paths.get(myLogBasename), "jenkinsPBS_").toString();
            if (this.runUser.length() > 0) {
                Files.setPosixFilePermissions(
                        Paths.get(this.executionDirectory),
                        PosixFilePermissions.fromString("rwxrwxrwx")
                );
            }
            listener.getLogger().println(String.format("Created working directory '%s' with permissions '%s'", 
                    this.executionDirectory, 
                    PosixFilePermissions.toString(Files.getPosixFilePermissions(Paths.get(this.executionDirectory)))
                )
            );
        } catch (IOException e) {
            throw new PBSException("Failed to create working directory: " + e.getMessage(), e);
        }

        if (StringUtils.isNotBlank(logHostname)) {
            this.errFileName = String.format("%s:%s", logHostname, Paths.get(this.executionDirectory, "err"));
            this.outFileName = String.format("%s:%s", logHostname, Paths.get(this.executionDirectory, "out"));
        } else {
            this.errFileName = Paths.get(this.executionDirectory, "err").toString();
            this.outFileName = Paths.get(this.executionDirectory, "out").toString();
        }
    }

    public Boolean call() {
        OutputStream tmpScriptOut = null;
        try {
            Path tmpScript = Paths.get(this.executionDirectory, "script");
            tmpScriptOut = Files.newOutputStream(tmpScript);
            tmpScriptOut.write(script.getBytes());
            tmpScriptOut.flush();

            listener.getLogger().println("PBS script: " + tmpScript.toString());
            String[] argList;
            if (this.runUser.length() > 0) {
                argList = new String[] { "-P", this.runUser, "-e", this.errFileName, "-o", this.outFileName,
                        tmpScript.toString(), "-W", "umask=022" };
            } else {
                argList = new String[] { "-e", this.errFileName, "-o", this.outFileName, tmpScript.toString() };
            }
            String jobId = PBS.qsub(argList, this.environment);

            listener.getLogger().println("PBS Job submitted: " + jobId);

            return this.seekEnd(jobId, numberOfDays, span);
        } catch (IOException e) {
            throw new PBSException("Failed to create temp script");
        } finally {
            IOUtils.closeQuietly(tmpScriptOut);
        }
    }

    // adapted from
    // https://github.com/jenkinsci/call-remote-job-plugin/blob/master/src/main/java/org/ukiuni/callOtherJenkins/CallOtherJenkins/JenkinsRemoteIF.java
    private boolean seekEnd(String jobId, int numberOfDays, long span) {
        final CommandOutput cmd = PBS.traceJob(jobId, numberOfDays);

        String out = cmd.getOutput();
        String err = cmd.getError();

        if (StringUtils.isBlank(out)) {
            listener.getLogger().println(String.format("Could not find job %s in PBS logs...Marking build as UNSTABLE", jobId));
            listener.getLogger().println(err);
            return false;
        }

        listener.getLogger().println("Seeking job end...");
        return this.loopSeek(jobId);
    }

    private boolean loopSeek(String jobId) {
        boolean toReturn = false;
        while (true) {
            CommandOutput cmd = PBS.traceJob(jobId, numberOfDays);

            final String out = cmd.getOutput();
            // String err = cmd.getError();

            // listener.getLogger().println(out);
            // listener.getLogger().println("----");
            Matcher matcher = JOB_STATUS_REGEX.matcher(out.toString());
            if (matcher.find()) {
                String state = matcher.group(1);
                listener.getLogger().println("Found job state " + state);
                if ("COMPLETE".equals(state)) {
                    // Now we look for the status
                    matcher = JOB_SUBSTATUS_REGEX.matcher(out.toString());
                    if (matcher.find()) {
                        state = matcher.group(1);
                        listener.getLogger().println("Found run job status of " + state);
                        listener.getLogger().println("---- Remote job output log ----");

                        InputStream outFile = null;
                        try {
                            outFile = Files.newInputStream(Paths.get(this.executionDirectory, "out"));
                            this.outputFileToLogger(outFile);
                        } catch (IOException e) {
                            listener.getLogger().println("ERROR: CANNOT PRINT OUT OUTPUT LOG - " + e.getMessage());
                            e.printStackTrace(listener.getLogger());
                        } finally {
                            IOUtils.closeQuietly(outFile);
                        }

                        listener.getLogger().println("---- End of remote job output log ----");
                        listener.getLogger().println("---- Remote job error log ----");
                        outFile = null;
                        try {
                            outFile = Files.newInputStream(Paths.get(this.executionDirectory, "err"));
                            this.outputFileToLogger(outFile);
                        } catch (IOException e) {
                            listener.getLogger().println("ERROR: CANNOT PRINT OUT ERROR LOG - " + e.getMessage());
                            e.printStackTrace(listener.getLogger());
                        } finally {
                            IOUtils.closeQuietly(outFile);
                        }
                        listener.getLogger().println("---- End of remote job error log ----");

                        // Return error code of the sub job
                        toReturn = "0".equals(state);
                        break;
                    }
                }
                break;
            }
            try {
                // listener.getLogger().println("Sleeping for " + span + "ms");
                Thread.sleep(span);
            } catch (InterruptedException e) {
                e.printStackTrace(listener.getLogger());
            }
        }
        // We now know what to return but we can destroy the directory
        try {
            Files.delete(Paths.get(this.executionDirectory, "out"));
            Files.delete(Paths.get(this.executionDirectory, "err"));
        } catch (IOException e) {
            // Ignore
            listener.getLogger().println("Warning: Cannot remove log and/or error files");
            e.printStackTrace(listener.getLogger());
        }
        try {
            Files.delete(Paths.get(this.executionDirectory, "script"));
            Files.delete(Paths.get(this.executionDirectory));
        } catch (IOException e) {
            // Ignore
            listener.getLogger().println("Warning: Cannot remove script and work directory");
            e.printStackTrace(listener.getLogger());
        }
        return toReturn;
    }

    private void outputFileToLogger(InputStream toOutput) throws IOException {
        byte[] buffer = new byte[4096];
        int readCount = toOutput.read(buffer);
        while (readCount > 0) {
            listener.getLogger().write(buffer, 0, readCount);
            readCount = toOutput.read(buffer);
        }
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        // TODO Auto-generated method stub
    }

    // public static void main(String[] args) {
    // String out = "Job: 128.localhost\n" +
    // "\n" +
    // "11/10/2013 12:52:40  S    enqueuing into batch, state 1 hop 1\n" +
    // "11/10/2013 12:52:40  S    Job Queued at request of kinow@localhost, owner =\n"
    // +
    // "                          kinow@localhost, job name = jenkins_job, queue =\n"
    // +
    // "                          batch\n" +
    // "11/10/2013 12:52:40  S    Job Modified at request of Scheduler@localhost\n"
    // +
    // "11/10/2013 12:52:40  L    Job Run\n" +
    // "11/10/2013 12:52:40  S    Job Run at request of Scheduler@localhost\n" +
    // "11/10/2013 12:52:40  S    Not sending email: User does not want mail of this\n"
    // +
    // "                          type.\n" +
    // "11/10/2013 12:52:40  A    queue=batch\n" +
    // "11/10/2013 12:52:40  A    user=kinow group=kinow jobname=jenkins_job queue=batch\n"
    // +
    // "                          ctime=1384095160 qtime=1384095160 etime=1384095160\n"
    // +
    // "                          start=1384095160 owner=kinow@localhost\n" +
    // "                          exec_host=chuva/0 Resource_List.neednodes=1\n"
    // +
    // "                          Resource_List.nodect=1 Resource_List.nodes=1\n"
    // +
    // "                          Resource_List.walltime=240:00:00 \n" +
    // "11/10/2013 12:53:40  S    Not sending email: User does not want mail of this\n"
    // +
    // "                          type.\n" +
    // "11/10/2013 12:53:40  S    Exit_status=0 resources_used.cput=00:00:00\n"
    // +
    // "                          resources_used.mem=3196kb resources_used.vmem=31756kb\n"
    // +
    // "                          resources_used.walltime=00:01:00\n" +
    // "11/10/2013 12:53:40  S    dequeuing from batch, state COMPLETE\n" +
    // "11/10/2013 12:53:40  M    scan_for_terminated: job 128.localhost task 1\n"
    // +
    // "                          terminated, sid=4801\n" +
    // "11/10/2013 12:53:40  M    job was terminated\n" +
    // "11/10/2013 12:53:40  M    obit sent to server\n" +
    // "11/10/2013 12:53:40  A    user=kinow group=kinow jobname=jenkins_job queue=batch\n"
    // +
    // "                          ctime=1384095160 qtime=1384095160 etime=1384095160\n"
    // +
    // "                          start=1384095160 owner=kinow@localhost\n" +
    // "                          exec_host=chuva/0 Resource_List.neednodes=1\n"
    // +
    // "                          Resource_List.nodect=1 Resource_List.nodes=1\n"
    // +
    // "                          Resource_List.walltime=240:00:00 session=4801\n"
    // +
    // "                          end=1384095220 Exit_status=0\n" +
    // "                          resources_used.cput=00:00:00 resources_used.mem=3196kb\n"
    // +
    // "                          resources_used.vmem=31756kb\n" +
    // "                          resources_used.walltime=00:01:00\n" +
    // "";
    //
    // SubmitPbsJob submit = new SubmitPbsJob("", 10, 10, System.out);
    // submit.loopSeek(out);
    // }

}
