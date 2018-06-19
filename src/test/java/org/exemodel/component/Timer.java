package org.exemodel.component;
import java.util.Date;

/**
 * Created by zp on 16/9/8.
 */
public class Timer {
    private Date startTime = new Date();
    private Date endTime = null;

    public Timer() {
    }

    public Timer(boolean start) {
        if (start) {
            start();
        }
    }

    public void start() {
        startTime = new Date();
    }

    public void end() {
        endTime = new Date();
        log();
    }

    public void log() {
        if (endTime == null || endTime.before(startTime)) {
            return;
        }
        System.out.println(String.format("using %dms, start at %s, end at %s", getTime(), startTime.toString(), endTime.toString()));
    }

    public long getTime() {
        assert endTime != null && endTime.after(startTime);
        return endTime.getTime() - startTime.getTime();
    }



    public static void  compair(Execution execution1,Execution execution2,int times){
        Timer timer = new Timer();
        for(int i=0;i<times;i++){
            execution1.execute();
        }
        timer.end();

        Timer timer1 = new Timer();
        for(int i=0;i<times;i++){
            execution2.execute();
        }
        timer1.end();
    }

    public static void  compair( int times,Execution... executions){
        for(Execution execution:executions){
            Timer timer = new Timer();
            for(int i=0;i<times;i++){
                execution.execute();
            }
            timer.end();
        }
    }
}
