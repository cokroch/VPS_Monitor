package com.seassoon.main;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.seassoon.db.DataSource;




public class VPS_Server {
	
	private static Logger log = Logger.getLogger(VPS_Server.class);
	
	private Thread processor;
	private Properties config;
	private ServerSocket ss;
	private int port;
	private BlockingQueue<Order> ordersQueue;
	private NetService service = new NetService();
	private ExecutorService threadPool;
	private Map<String,Socket> vpsMap;
	private Map<String,Socket> spiderMap;
	private Map<String,String> ipHostMap;
	private Map<String,String> spider_vps_map;
	
	public VPS_Server(){
		try{
			config = new Properties();
			config.load(VPS_Server.class.getClassLoader().getResourceAsStream("config.properties"));
			port = Integer.parseInt(config.getProperty("server.port"));
			ordersQueue = new LinkedBlockingQueue<Order>(200);
			threadPool = Executors.newFixedThreadPool(50);
			vpsMap = new Hashtable<String, Socket>();
			spiderMap = new Hashtable<String,Socket>();
			ipHostMap = new Hashtable<String, String>();
			spider_vps_map = new Hashtable<String, String>();
		}catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		log.info("VPS_Server初始化完毕");
	}

	private class ProcessorThread extends Thread{
		public void run(){
			while(true){
				Socket s = null;
				try{
					log.info("等待从请求队列中获取请求...");
					Order order = ordersQueue.take();
					log.info("从请求队列中获取到请求：\n"+order);
					s = vpsMap.get(findVPSIdByIP(order.getIp()));
					log.info("请求目标vps的套接字："+s);
					sendOrder(s, order);
					Thread.sleep(1500);
//					if(s.isClosed()){
					s.close();
					log.info("vps主机端socket已经关闭");
					vpsMap.remove(order.getHostName());
//					}
				}catch(Exception e){
					e.printStackTrace();
				}finally{
					try {
						if(s!=null){s.close();}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

		private void sendOrder(Socket s, Order order) {
			OutputStream os = null;
			try {
				Document doc = DocumentHelper.createDocument();
				doc.addElement("order").addElement("script").addText(order.getScript());
				os = s.getOutputStream();
				log.info(" -- 向目标vps主机发出信息："+doc.asXML());
				service.send(os, doc);
				log.info(" -- 发送完毕");
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (os != null) {
					try {
//						os.close();
					} catch (Exception e) {
					}
				}
			}
		}
	}
	
	public void start(){
		try{
			ss = new ServerSocket(port);
			processor = new ProcessorThread();
			processor.start();
			while(true){
				Thread.sleep(1000);
				Socket s = ss.accept();
				log.info("ip："+ s.getInetAddress().getHostAddress());
				threadPool.execute(new ClientHandler(s));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	/**
	 * 根据client传来的文档将socket放入socket池
	 */
	private void addToSocketPoolAndReply(Document doc, Socket s) {
		OutputStream os = null;
		try {
			log.info("收到连接发送的文档：\n"+doc.asXML());
			String type = doc.getRootElement().element("type").getTextTrim();
			String id = doc.getRootElement().element("id").getTextTrim();
			if (type != null && !"".equals(type) && "VPSClient".equals(type)) {
				log.info("判定连接方为：vps主机");
				//存入vps主机的id——Socket映射
				vpsMap.put(id, s);
				log.info("新连接存入前ipHostMap："+ipHostMap);
				String lastTimeIp = ipHostMap.get(id);
				String afterChangedIp = s.getInetAddress().getHostAddress();
				//存入vps主机id——当前ip映射作为更新ip的依据
				ipHostMap.put(id,afterChangedIp);
				log.info("新连接存入后ipHostMap："+ipHostMap);
				//ip切换完成之后重新连接	对spider发出消息
				if(lastTimeIp != null && !"".equals(lastTimeIp)){
					updateDataBase(lastTimeIp,afterChangedIp);
				}
				String toReturnSpiderId = findToReturnSpider(id);
				if(toReturnSpiderId != null){
					log.info("该连接为切换ip之后重新进行连接："+s);
					log.info("切换后的ip："+ afterChangedIp);
					replySpider(toReturnSpiderId);	
					log.info("返回spider结束");
				}
			} else if (type != null && !"".equals(type) && "SpiderClient".equals(type)) {
				log.info("判定链接方为：spider程序");
				if(spider_vps_map.size() == 0){
					Element order = doc.getRootElement();
					String ip = order.element("ip").getTextTrim();
					String vpsId = findVPSIdByIP(ip);
						if(vpsId != null && !"".equals(vpsId)){
							String spiderId = order.element("id").getTextTrim();
							String script = order.element("script").getTextTrim();
							Order o = new Order(spiderId, ip, /*port,*/ script);
							log.info("该spider主机id："+ spiderId);
							log.info("要求切换的ip为："+ ip);
							log.info("要求vps执行的脚本为："+ script);
							//放入请求消息队列
							ordersQueue.offer(o);
							//存入	爬虫程序的计算机名——vps主机的计算机名	   的映射
							spider_vps_map.put(spiderId, vpsId);
							//存入等待响应的爬虫程序的  id——Socket  将在vps客户端切换ip完成后再次请求时，处理并删除
							spiderMap.put(spiderId, s);
						}else{
							Document response = DocumentHelper.createDocument();
							Element root = response.addElement("response");
							root.addElement("state").addText("error");
							root.addElement("caseBy").addText("不存在该ip,请检查输入的ip");
							os = s.getOutputStream();
							service.send(os, response);
							Thread.sleep(1000);
							s.close();
							return ;
						}
				}else{
					Document response = DocumentHelper.createDocument();
					Element root = response.addElement("response");
					root.addElement("state").addText("error");
					root.addElement("caseBy").addText("ip正在切换中,请稍后重试...");
					os = s.getOutputStream();
					service.send(os, response);
					Thread.sleep(1000);
					s.close();
					return ;
				}
			}
			Document response = DocumentHelper.createDocument();
			response.addElement("response").addElement("state").addText("OK");
			os = s.getOutputStream();
			log.info("对所有连接动作首先进行该响应"+response.asXML());
			service.send(os, response);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			if (os != null) {
				try {
//					os.close();
				} catch (Exception e) {
				}
			}
		}
	}
	private void updateDataBase(String lastTimeIp, String afterChangedIp) {
		DataSource ds = new DataSource("db_vps_host");
		ds.executeUpdate("update vps_host set host = '"+ afterChangedIp +"' where host = '"+ lastTimeIp +"'");
	}
	private void replySpider(String toReturnSpiderId) {
		String vpsId = spider_vps_map.get(toReturnSpiderId);
		if(vpsId != null){
			String afterChangeIp = ipHostMap.get(vpsId);
			if(afterChangeIp!=null && !"".equals(afterChangeIp)){
				Socket s = spiderMap.get(toReturnSpiderId);
				Document response = DocumentHelper.createDocument();
				Element responseTag = response.addElement("response");
				responseTag.addElement("state").addText("OK");
				responseTag.addElement("afterChangeIp").addText(afterChangeIp);
				OutputStream os = null;
				try {
					os = s.getOutputStream();
					log.info("返回spider的响应：\n"+response.asXML());
					service.send(os, response);
					log.info("返回spider的响应完毕");
				} catch (Exception e) {
					e.printStackTrace();
				}finally{
					//删除 spider_vps_map 中的关联映射
					try{
						spider_vps_map.remove(toReturnSpiderId);
						log.info("删除后  spider_vps_map："+spider_vps_map);
						if(os != null){
//							os.close();
						}
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		}
	}
	private String findToReturnSpider(String id) {
		if(id == null || "".equals(id)){
			return null;
		}
		for(String spiderId : spider_vps_map.keySet()){
			String value = spider_vps_map.get(spiderId);
			if(id.equals(value)){
				return spiderId;
			}
		}
		return null;
		
	}
	private String findVPSIdByIP(String ip) {
		if(ip == null || "".equals(ip)){
			return null;
		}
		for(String vpsId : ipHostMap.keySet()){
			String value = ipHostMap.get(vpsId);
			if(ip.equals(value)){
				return vpsId;
			}
		}
		return null;
	}
	
	class ClientHandler implements Runnable{
		private Socket socket;
		public ClientHandler(Socket socket){
			this.socket = socket;
		}
		public void run(){
			try{
				InputStream in = socket.getInputStream();
				Document doc = service.receive(in);
				addToSocketPoolAndReply(doc,socket);
			}catch(Exception e){
				log.error(e);
			}
		}
	}
	
	
	public static void main(String[]args){
		log.info(" * * - * * - start - * * - * * ");
		VPS_Server server = new VPS_Server();
		server.start();
	}
	
	
	
}

class Order {
	
	/**  客户端主机名   */
	private String hostName;
	/**  客户端ip   */
	private String ip;
//	/**  客户端port   */
//	private String port;
	/**  客户端脚本   */
	private String script;
	
	public Order (){
		super();
	}
	
	public Order (String hostName, String ip,/* String port,*/ String script){
		this.hostName = hostName;
		this.ip = ip ;
//		this.port = port ;
		this.script = script ;
	}
	
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
//	public String getPort() {
//		return port;
//	}
//	public void setPort(String port) {
//		this.port = port;
//	}
	public String getScript() {
		return script;
	}
	public void setScript(String script) {
		this.script = script;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	@Override
	public String toString() {
		return "Order [hostName=" + hostName + ", ip=" + ip + /*", port=" + port +*/ ", script=" + script + "]";
	}

}


