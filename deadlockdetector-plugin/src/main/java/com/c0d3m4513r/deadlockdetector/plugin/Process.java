package com.c0d3m4513r.deadlockdetector.plugin;

import com.c0d3m4513r.deadlockdetector.api.ServerWatcher;
import com.c0d3m4513r.deadlockdetector.plugin.config.Config;
import com.c0d3m4513r.pluginapi.API;
import com.c0d3m4513r.pluginapi.config.TimeEntry;
import com.c0d3m4513r.pluginapi.config.iface.IConfigLoadableSaveable;
import lombok.NoArgsConstructor;
import lombok.val;

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
        try {
            API.getLogger().info("[DeadlockDetector] Getting current file path");
            CodeSource codeSource = Main.class.getProtectionDomain().getCodeSource();
            API.getLogger().info("[DeadlockDetector] Converting current file path to a String");
            String jarFile = codeSource
                    .getLocation()
                    .getPath();
            API.getLogger().info("[DeadlockDetector] Cleaning up the file path, to only point to the jar of the plugin.");
            jarFile=jarFile.substring(0,jarFile.indexOf('!')).replace("file:","");
            API.getLogger().info("[DeadlockDetector] Getting the Java-Instance, that executes the server.");
            String java = System.getProperties().getProperty("java.home") + File.separator + "bin" + File.separator + "java"+(System.getProperty("os.name").startsWith("Win")?".exe":"");
            API.getLogger().info("[DeadlockDetector] File location is: '"+jarFile+"'.");
            API.getLogger().info("[DeadlockDetector] Java executable is: '"+java+"'.");
            API.getLogger().info("[DeadlockDetector] Starting a new Thread to Observe the Server.");

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
        sendValue(
                ServerWatcher.config,"\n",
                Config.Instance.getTimeout().getValue().toString()," ",
                Config.Instance.getRestartWait().getValue().toString()," ",
                System.getenv("P_SERVER_UUID"),"\n",
                Config.Instance.getPanelUrl().getValue(),"\n",
                Config.Instance.getApiKey().getValue(), "\n"
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
        if(o==null) startProcess();
        try {
            for(val s : strings){
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