package main.java.elegit;

import org.eclipse.jgit.lib.ProgressMonitor;

/**
 * An empty progress monitor for JGit commands
 */
public class SimpleProgressMonitor implements ProgressMonitor{

    public SimpleProgressMonitor(){
    }

    @Override
    public void start(int i){
    }

    @Override
    public void beginTask(String s, int i){
    }

    @Override
    public void update(int i){
    }

    @Override
    public void endTask(){
    }

    @Override
    public boolean isCancelled(){
        return false;
    }
}
