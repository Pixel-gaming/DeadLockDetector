package com.c0d3m4513r.deadlockdetector.plugin;

import com.c0d3m4513r.deadlockdetector.api.ActionSenderImpl;
import com.c0d3m4513r.deadlockdetector.api.PanelInfo;
import com.c0d3m4513r.deadlockdetector.api.ServerWatcher;
import com.c0d3m4513r.deadlockdetector.plugin.config.Config;
import com.c0d3m4513r.pluginapi.API;
import com.c0d3m4513r.pluginapi.config.TimeEntry;
import com.c0d3m4513r.pluginapi.config.iface.IConfigLoadableSaveable;
import lombok.NoArgsConstructor;
import lombok.var;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.CodeSource;

@NoArgsConstructor
public class Process implements IConfigLoadableSaveable {
    public static final Process PROCESS = new Process();

    private static java.lang.Process proc;
    private static Writer o;

    private void startProcess(){
        if (proc!=null && proc.isAlive()){
            if(o==null) o=new OutputStreamWriter(proc.getOutputStream());
            return;
        }
        API.getLogger().info("Starting Process");
        try {
            API.getLogger().info("[DeadlockDetector] Getting current file path");
            CodeSource codeSource = Main.class.getProtectionDomain().getCodeSource();
            API.getLogger().info("[DeadlockDetector] Converting current file path to a String");
            String jarFile = codeSource
                    .getLocation()
                    .getPath();
            API.getLogger().info("[DeadlockDetector] Cleaning up the file path, to only point to the jar of the plugin.");

            int index = jarFile.indexOf('!');
            if (index >= 0) jarFile=jarFile.substring(0, index);
            jarFile = jarFile.replace("file:","");

            API.getLogger().info("[DeadlockDetector] Getting the Java-Instance, that executes the server.");
            String java = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java"+(System.getProperty("os.name").startsWith("Win")?".exe":"");
            API.getLogger().info("[DeadlockDetector] File location is: '"+jarFile+"'.");
            API.getLogger().info("[DeadlockDetector] Java executable is: '"+java+"'.");
            API.getLogger().info("[DeadlockDetector] Starting a new Process to Observe the Server.");

            proc=new ProcessBuilder(new File(java).getPath(),
                    "-jar",
                    new File(jarFile).getPath())
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectInput(ProcessBuilder.Redirect.PIPE)
                    .start();

            API.getLogger().info("[DeadlockDetector] Thread started successfully. Getting it's output stream now.");
            o=new OutputStreamWriter(proc.getOutputStream());
            sendConfig();
            heartbeat();
        } catch (Exception e) {
            API.getLogger().warn("[DeadlockDetector] Process exception:",e);
        }
    }

    private void sendConfig(){
        if(o==null || proc==null || !proc.isAlive()) startProcess();

        API.getLogger().info("[DeadlockDetector] Sending Config to Observer Process.");
        ActionSenderImpl.logger = API.getLogger();
        PanelInfo panelInfo = new PanelInfo(Config.Instance.getPanelType().getValue(),
                Config.Instance.getPanelUrl().getValue(),
                Config.Instance.getIgnore_ssl_cert_errors().getValue(),
                Config.Instance.getApiKey().getValue(),
                Config.Instance.getId().getValue());
        var uuid = Config.Instance.getPanelType().getValue().getPanel().getUUID(ActionSenderImpl.SENDER, panelInfo);
        sendValue(
                ServerWatcher.config,"\n",
                Config.Instance.getTimeout().getValue().toString()," ",
                Config.Instance.getRestartWait().getValue().toString()," ",
                Config.Instance.getIgnore_ssl_cert_errors().getValue().toString()," ",
                uuid ,"\n",
                Config.Instance.getPanelType().getValue().name(),"\n",
                Config.Instance.getApiKey().getValue(), "\n",
                Config.Instance.getPanelUrl().getValue(),"\n"
        );
        API.getLogger().info("[DeadlockDetector] Config Send has completed.");
    }

    public void stopAction(TimeEntry timeout){
        API.getLogger().info("[DeadlockDetector] DeadLockDetector was instructed to not care about the server's state in the next "+timeout.toString()+".");
        long seconds = timeout.getSeconds();
        timeout.days = 0;
        timeout.hours = 0;
        timeout.minutes = 0;
        timeout.seconds = 0;
        long nanos = timeout.getNs();
        sendValue(ServerWatcher.stopActions,"\n",
                Long.toString(seconds)," ",
                Long.toString(nanos),"\n"
        );
    }

    public void startAction(){
        API.getLogger().info("[DeadlockDetector] DeadLockDetector was instructed to start caring about the server's state.");
        sendValue(ServerWatcher.startActions,"\n");
    }

    public void heartbeat(){
        //no log here. This is a hot path, and would spam console.
        if(proc!=null && proc.isAlive()) sendValue(ServerWatcher.heartbeat,"\n");
    }

    private void sendValue(String... strings){
        if(o==null || proc == null || proc.isAlive()){
            if (!Config.Instance.getStartOnServerStart().getValue()) return;
            startProcess();
        }
        try {
            for(var s : strings){
                o.write(s);
            }
            o.flush();
        }catch (IOException e){
            API.getLogger().warn("[DeadlockDetector] error whilst sending value: ",e);
        }
    }


    @Override
    public void loadValue() {
        if(proc!=null && proc.isAlive()) sendConfig();
    }

    @Override
    public void saveValue() {

    }
}
