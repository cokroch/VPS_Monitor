package com.seassoon.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
/**
 * vps客户端
 * @author sxad
 *
 */
public class VPS_Client {
	private static Logger log = Logger.getLogger(VPS_Client.class);
	
	//属性
	private int port;
	private String host;
	private Properties config;
	private NetService service;
	
	private Socket socket;
	
	private String client_id;
	//构造方法
	public VPS_Client(){
		try{
			//若没有启动过，首先生成id
			produceID();
			config = new Properties();
//			config.load(VPS_Server.class.getClassLoader().getResourceAsStream("config.properties"));
			config.load(new FileInputStream(new File("config.properties")));
			port = Integer.parseInt(config.getProperty("server.port"));
			host = config.getProperty("server.host");
			service = new NetService();
			client_id = config.getProperty("client.id");
		}catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException("配置文件加载失败",e);
		}
	}	
	private void produceID() throws Exception {
		Properties prop = new Properties();
		try{
			prop.load(new FileInputStream(new File("config.properties")));
		}catch(Exception e){
			prop.load(VPS_Server.class.getClassLoader().getResourceAsStream("config.properties"));
		}
		if(prop.containsKey("client.id")){
			String id = prop.getProperty("client.id");
			if(id == null || "".equals(id)){
				String newId = UUID.randomUUID().toString();
				log.info("重新生成了id:"+newId);
				prop.setProperty("client.id",newId);
			}else{
				log.info("该VPSClient的ID为：" + prop.getProperty("client.id") );
			}
		}else{
			prop.setProperty("client.id",UUID.randomUUID().toString());
		}
		prop.store(new FileOutputStream(new File("config.properties")),"header");
	}
	public void start(){
		try{
			while(true){
				socket = new Socket(host, port);
				sendInfo(socket);
				InputStream in = socket.getInputStream();
				Document doc = null;
				while((doc=service.receive(in))!=null){
					log.info("收到服务端回复："+doc.asXML());
					Element response = (Element) doc.getRootElement();
					String script = null;
					try{
						script = response.element("script").getTextTrim();
					}catch(Exception e){
					}
					String state = null;
					try{
						state = response.element("state").getTextTrim();
					}catch(Exception e){
					}
					log.info("state:"+state);
					log.info("script:"+script);
					if(script != null && !"".equals(script)){
						log.info("开始切换ip并重新发起tcp连接");
						socket.close();
						socket = null;
				        Runtime run = Runtime.getRuntime();
				        BufferedReader br = null;
				        try {
//				            Process process = 
				            		run.exec(script);
				            		log.info("-- 执行命令完毕");
//				            log.info(script);
//				            InputStream is = process.getInputStream();
//				            br = new BufferedReader(new InputStreamReader(is,"utf-8"));
//				            String line = null;
//				            while ((line = br.readLine()) != null) {
//				               log.info(line);
//				            }
//				            process.waitFor();
				            Thread.sleep(15000);
				            break;
				        } catch (Exception e) {
				            e.printStackTrace();
				        }finally{
				        	try{
				        		if(br != null){
				        			br.close();
				        		}
				        		if(in != null){
//				        			in.close();
				        		}
				        	}catch(Exception e){
				        	}
				        }
					}else if(state != null && !"".equals(state)){
						log.info("当前不需要切换IP");
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	private void sendInfo(Socket socket2) {
		OutputStream os = null;
		try {
			os = socket2.getOutputStream();
			Document doc = DocumentHelper.createDocument();
			Element attr = doc.addElement("attrs");
			attr.addElement("type").addText("VPSClient");
			attr.addElement("id").addText(client_id);
			service.send(os, doc);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try{
				if(os != null){
//					os.close();
				}
			}catch(Exception e){
			}
		}
	}
	public static void main(String[]args){
		log.info(" * * - * * - start - * * - * * ");
		VPS_Client client = new VPS_Client();
		client.start();
	}
	
	
	
	
}
