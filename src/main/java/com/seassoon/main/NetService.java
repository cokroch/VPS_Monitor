package com.seassoon.main;
import org.dom4j.io.SAXReader;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.dom4j.io.XMLWriter;

import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.dom4j.Document;
/** 自定义网络传输协议 格式为TLV(type,length,value)*/
public class NetService {
	/**
	 * 给定网络输出流和要发送的文档对象，将其发出
	 * @param out 给定网络输出流
	 * @param doc 给定要发送的Document文档对象
	 * @throws Exception
	 */
	public void send(OutputStream out,Document doc)throws Exception{
		/**
		 * 1.发送文件格式  "X","M","L"三个字节
		 * 2.将给定的Document文档对象通过BAOS转换为字节数组(先通过XMLWriter将doc对象写入到BAOS中去)
		 * 3.发送长度 获取的doc转化后的字节数组的长度
		 * 4.发送doc转换后的字节数组
		 */
		DataOutputStream dos = new DataOutputStream(out);
		//1
		out.write('X');
		out.write('M');
		out.write('L');
		//2
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		XMLWriter writer = new XMLWriter(baos);
		writer.write(doc);
		writer.close();
		byte[] b = baos.toByteArray();
		//3
		
		dos.writeInt(b.length);
		//4
		dos.write(b);
		dos.flush();
	}
	/**
	 * 给定网络输入流，负责接收并返回Document对象
	 * @param in 给定网络输入流
	 * @return 返回Document对象
	 * @throws Exception
	 */
	public Document receive(InputStream in)throws Exception{
		/**
		 * 1.接收格式XML
		 * 2.接收长度length
		 * 3.读取Document对象转化后的数组
		 * 4.返回doc
		 */
		DataInputStream dis = new DataInputStream(in);
		//1
		char c1 = (char)in.read();
		char c2 = (char)in.read();
		char c3 = (char)in.read();
		System.out.println("type:"+c1+c2+c3);
		//2
		byte[] b = new byte[dis.readInt()];
		//3
		dis.read(b);
		System.out.println(new String(b,"utf-8"));
		return new SAXReader().read(new ByteArrayInputStream(b));
	}
}
