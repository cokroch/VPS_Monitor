package com.seassoon.main;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
//import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

/**
 * 作为切换ip的工具类
 * @author sxad
 *
 */
public class SpiderClient {
	private static Logger log = Logger.getLogger(SpiderClient.class);
	private static final int port;
	private static final String host;
//	private static Properties config;
	private static NetService service ;
	static {
//		config = new Properties();
//		try {
//			config.load(VPS_Server.class.getClassLoader().getResourceAsStream("config.properties"));
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		port = 1111111;
		host = "***";
		service = new NetService();
	}
	public static String changeIP(String needChangedIp){
		try {
			Socket s = new Socket(host, port);
			String ip = sendInfo(s,needChangedIp);
			return ip;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		return null;
	}

	private static String sendInfo(Socket s,String needChangedIp) {
		OutputStream os = null;
		try {
			os = s.getOutputStream();
			Document doc = DocumentHelper.createDocument();
			Element attr = doc.addElement("order");
			attr.addElement("ip").addText(needChangedIp);
			attr.addElement("id").addText(UUID.randomUUID().toString());
			attr.addElement("type").addText("SpiderClient");
//			attr.addElement("script").addText("cmd.exe /k start C:\\\"Documents and Settings\"\\Administrator\\桌面\\ChangeHost.bat");
			attr.addElement("script").addText("cmd.exe /c C:\\\"Documents and Settings\"\\Administrator\\桌面\\ChangeHost.bat");
			service.send(os, doc);
			InputStream in = s.getInputStream();
			Document receive = null;
			while((receive = service.receive(in))!=null){
				log.info(receive.asXML());
				String afterChangeIp =  receive.getRootElement().elementTextTrim("afterChangeIp");
				String state = receive.getRootElement().elementTextTrim("state");
				if("OK".equals(state) && afterChangeIp != null){
					return afterChangeIp;
				}
			}
		} catch (Exception e) {
			log.error(e);
		}finally{
			try{
				if(os != null){
//					os.close();
				}
			}catch(Exception e){
			}
		}
		return null;
	}
	
	
	public static void main(String[] args) {
		String afterIP = changeIP("***");
		System.out.println(afterIP);
	}
	
}
