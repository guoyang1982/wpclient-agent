package com.gy.woodpecker.log;

import com.alibaba.fastjson.JSONObject;
import com.gy.woodpecker.message.MessageBean;
import com.gy.woodpecker.redis.RedisClient;
import com.gy.woodpecker.tools.ConfigPropertyUtile;
import com.gy.woodpecker.tools.IPUtile;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

/**
 * Created by guoyang on 17/10/27.
 */
public class LoggerFacility {
    private static volatile LoggerFacility loggerFacility;

    protected RedisClient redisClient;

    private volatile String  appName;

    private volatile String healthCheck = "true";

    public volatile boolean telHealthCheck = true;

    private int healthCheckDelay = 10000;

    public static volatile boolean f = true;

    private static int corePoolSize = 4;
    private static int maximumPoolSize = 4;
    private static int keepAliveTime = 10;
    private static int queueCount = 5000;

    //private static ArrayBlockingQueue arrayBlockingQueue = new ArrayBlockingQueue<Runnable>(queueCount);
    private static ThreadPoolExecutor executorPools =
            new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(queueCount),
            new MessageRejectedExecutionHandler());

    public static String threadPoolsMonitor(){
        int activeCount = executorPools.getActiveCount();
        long completedTaskCount = executorPools.getCompletedTaskCount();
        int corePoolSize = executorPools.getCorePoolSize();
        int maximumPoolSize = executorPools.getMaximumPoolSize();
        long taskCount = executorPools.getTaskCount();
        int poolSize = executorPools.getPoolSize();
        int queueSize = executorPools.getQueue().size();
        StringBuffer str = new StringBuffer();
        str.append("the activeCount=").append(activeCount).append("\r\n").append("the completedTaskCount=")
                .append(completedTaskCount).append("\r\n").append("the corePoolSize=").append(corePoolSize)
                .append("\r\n").append("the maximumPoolSize=").append(maximumPoolSize).append("\r\n").append("the taskCount=")
                .append(taskCount).append("\r\n").append("the poolSize=").append(poolSize).append("\r\n").append("the queueSize=")
                .append(queueSize);
        return str.toString();
    }


    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    private LoggerFacility(){
        try{
            initConfig();
            redisClient = new RedisClient();
            redisClient.init();
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    private void initConfig() {
        this.appName = ConfigPropertyUtile.getProperties().getProperty("application.name");
        String healthCheckT = ConfigPropertyUtile.getProperties().getProperty("redis.health.Check");
        if(null != healthCheckT && !healthCheckT.equals("")){
            this.healthCheck = healthCheckT;
        }
        String delay = ConfigPropertyUtile.getProperties().getProperty("redis.health.check.delay");
        if(null != delay && !delay.equals("")){
            this.healthCheckDelay = Integer.parseInt(delay);
        }
        String corePoolSizeT = ConfigPropertyUtile.getProperties().getProperty("log.thread.corePoolSize");
        if(null != corePoolSizeT && !corePoolSizeT.equals("")){
            this.corePoolSize = Integer.parseInt(corePoolSizeT);
        }
        String maximumPoolSizeT = ConfigPropertyUtile.getProperties().getProperty("log.thread.maximumPoolSize");
        if(null != maximumPoolSizeT && !maximumPoolSizeT.equals("")){
            this.maximumPoolSize = Integer.parseInt(maximumPoolSizeT);
        }
        String keepAliveTimeT = ConfigPropertyUtile.getProperties().getProperty("log.thread.keepAliveTime");
        if(null != keepAliveTimeT && !keepAliveTimeT.equals("")){
            this.keepAliveTime = Integer.parseInt(keepAliveTimeT);
        }
        String queueCountT = ConfigPropertyUtile.getProperties().getProperty("log.thread.queue.count");
        if(null != queueCountT && !queueCountT.equals("")){
            this.queueCount = Integer.parseInt(queueCountT);
        }
    }

    /**
     *
     * @return
     */
    public static LoggerFacility getInstall(){
        if(null == loggerFacility){
            synchronized (LoggerFacility.class){
                if(null == loggerFacility){
                    System.out.println("create logger!!!!!!!");
                    loggerFacility = new LoggerFacility();
                }
            }
        }

        return loggerFacility;
    }

    /**
     * 发送消息
     * @param msg
     */
    public void sendToRedis(final String msg) {
        //log.info("发送异常日志消息!"+msg);
        System.out.println("发送异常日志消息!"+msg);

        if (!f) {
            //log.info("redis集群不健康, 不处理操作!");
            System.out.println("redis集群不健康, 不处理操作!");
            return;
        }
        if(null == appName || appName.equals("")){
            System.out.println("应用名为空, 不处理操作!");
            return;
        }
        executorPools.execute(new Runnable() {
            public void run() {
                try {
                    MessageBean messageBean = new MessageBean();
                    messageBean.setAppName(appName);
                    messageBean.setIp(IPUtile.getIntranetIP());
                    messageBean.setMsg(msg);
                    messageBean.setCreateTime(timeForNow());

                    redisClient.rightPush(appName, JSONObject.toJSONString(messageBean));
                } catch (Exception e) {
                    System.out.println("发送异常日志消息失败!"+e.getMessage());
                }

            }
        });
    }
    private String timeForNow(){
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        return format.format(now);
    }

    /**
     * 打印拒绝任务时 线程池的详细信息
     */
    private static class MessageRejectedExecutionHandler implements RejectedExecutionHandler {
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            // 监控
//            if (log.isInfoEnabled()) {
//                log.info("LoggerFacility rejectedExecution, ThreadPoolExecutor:{}", executor.toString());
//           }
            System.out.println("LoggerFacility rejectedExecution, ThreadPoolExecutor:" + executor.toString());
        }
    }

    public void healthCheck(){
        if(healthCheck.equals("false")){
            return;
        }
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        long delay = healthCheckDelay;
        long initDelay = 0;
        executor.scheduleAtFixedRate(
                new Runnable() {
                    public void run() {
                        if(!telHealthCheck){
                            return;
                        }
                       // log.info("执行redis健康检查!");
                        System.out.println("执行redis健康检查!");
                        try{
                            if(null != redisClient){
                                redisClient.set(appName+"-ping","1",1);
                            }
                            f = true;
                        }catch (Exception e){
                            //log.info("redis健康检查异常,{}",e);
                            System.out.println("执行redis健康检查异常!");

                            f = false;
                        }
                    }
                },
                initDelay,
                delay,
                TimeUnit.MILLISECONDS);
    }
}
