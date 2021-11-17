package edu.nju.practice.util;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.yarn.webapp.hamlet2.Hamlet;

public class HdfsUtil {
    /**
     * Timer任务时间间隔，1s
     */
    private final long MODIFY_TIME = 1000L;

    private FileSystem fs = null;

    /**
     * 初始化，加载hadoop配置信息
     * @throws Exception
     */
    public HdfsUtil() throws IOException{
        //读取classpath下的xxx-site.xml 配置文件，并解析其内容，封装到conf对象中
        Configuration conf = new Configuration();
        //根据配置信息，去获取一个具体文件系统的客户端操作实例对象
        fs = FileSystem.get(conf);
    }

    /**
     * 每隔MODIFY_TIME时间，对path文件夹下一个.jl文件的modiftTime进行修改
     * @param path 文件夹位置
     * @throws Exception
     */
    public void modifyTime(String path) throws IOException{
        //获取hdfs的path文件夹下所有文件的信息
        RemoteIterator<LocatedFileStatus> listFiles = fs.listFiles(new Path(path),true);

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            //每次修改一个.jl文件的modifyTime
            @Override
            public void run() {
                try {
                    if(listFiles.hasNext() == true) {
                        LocatedFileStatus status = listFiles.next();
                        Path filepath = status.getPath();
                        if (filepath.toString().endsWith(".jl") == true) {
                            long currentTime = System.currentTimeMillis();
                            fs.setTimes(filepath, currentTime, -1);
                        }
                    }
                    else {
                        timer.cancel();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, MODIFY_TIME, MODIFY_TIME);
    }


    //测试用的
    /**
     * 从本地拷贝文件到hdfs
     * @param src
     * @param dst
     * @throws IOException
     */
    public void copyFromLocal(String src, String dst) throws IOException{
        fs.copyFromLocalFile(new Path(src), new Path(dst));
    }

    //test
    public static void main(String[] args) throws Exception {
        HdfsUtil hdfsUtil = new HdfsUtil();
//        hdfsUtil.copyFromLocal("/Users/robot17/Documents/test 2.jl","/aa/bb/dd");
//        hdfsUtil.copyFromLocal("/Users/robot17/Documents/test 3.jl","/aa/bb/dd");
//        hdfsUtil.copyFromLocal("/Users/robot17/Documents/test 4.jl","/aa/bb/dd");
        hdfsUtil.modifyTime("/aa/bb/dd");
    }

}
