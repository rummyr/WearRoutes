package uk.me.ponies.wearroutes.utils;

import android.os.Process;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by rummy on 28/06/2016.
 */
public abstract class CPUMeasurer {
    public static long currentCPUUsed() {
        int myPid = Process.myPid();
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/" + myPid + "/stat", "r");
            String load = reader.readLine();
            //Log.d("CPUMeasurer", "Read: " + load);

            String[] toks = load.split(" ");

            long uTime =Long.parseLong(toks[13]);
            long sTime = Long.parseLong(toks[14]);
            long cuTime = Long.parseLong(toks[15]);
            long csTime = Long.parseLong(toks[16]);
            //noinspection UnnecessaryLocalVariable
            long cpu1 =  uTime + sTime + cuTime + csTime;
            reader.close();
            return cpu1;
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return 0;
    }
}
