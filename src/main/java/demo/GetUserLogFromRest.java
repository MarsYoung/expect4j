package demo;

import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetUserLogFromRest {

	private Logger log = LoggerFactory.getLogger(GetUserLogFromRest.class);

	public static void main(String[] args) {
		new GetUserLogFromRest().analyzeUserLog("ppag867871a15fab");
	}

	// ssh to every server
	// using simple 'grep' to get the user log from the server
	// analyze and get all the log (include request and response)
	// sort
	public void analyzeUserLog(String user) {

		String ips[] = new String[] { "replace with your ip address" };
		String logPath = "/data/logs/filter.log";
		StringBuffer allLog = new StringBuffer();
		ExecutorService executer = Executors.newFixedThreadPool(1);
		CountDownLatch latch = new CountDownLatch(ips.length);
		for (String ip : ips) {
			executer.execute(new Runnable() {
				@Override
				public void run() {
					SSHClient sc = getSShClient();
					allLog.append(getUserLogFromServer(sc, ip, user, logPath)
							+ "\n");
					sc.disconnect();
					log.info("{}的日志抓取完毕", ip);
					latch.countDown();
				}
			});
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			log.error(e.toString());
		}
		log.info("\n" + allLog.toString());
		executer.shutdown();
	}

	public String getUserLogFromServer(SSHClient sc, String host, String word,
			String logPath) {
		sc.executeCommands(new String[] { "ssh root@" + host,
				"egrep '" + word + "' " + logPath });
		String result = sc.getResult();
		// 找出所有的requestid
		String[] ids = StringUtils.substringsBetween(result, "request-id\":\"",
				"\"");
		String words = "";
		if (ids!=null&&ids.length!=0) {
			for (String id : ids) {
				words += "request-id\":\""+id + "|";
			}
			words = StringUtils.substringBeforeLast(words, "|");
			String cmd = "egrep '" + words + "' " + logPath;
			sc.executeCommands(new String[] { cmd});		
		}
		result=sc.getResult();
		sc.executeCommands(new String[]{"exit"});//最后再退出
		return result;
	}

	public SSHClient getSShClient() {
		ResourceBundle rb = ResourceBundle.getBundle("ssh");
		String ip = rb.getString("ssh.ip");
		int port = Integer.parseInt(rb.getString("ssh.port"));
		String user = rb.getString("ssh.user");
		String password = rb.getString("ssh.password");
		SSHClient ssh = new SSHClient(ip, port, user, password);
		return ssh;
	}
}
