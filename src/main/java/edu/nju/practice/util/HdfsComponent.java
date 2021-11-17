package edu.nju.practice.util;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HdfsComponent 
{
	@Value("${hdfs.directory}")
	private String directory;
	
	@Value("${hadoop.conf.dir}")
	private String dir;
	
	@Autowired
	private SocketUtil socketUtil;
	
	public int modifyFile()
	{
		try {
			new HdfsUtil(dir).modifyTime(directory);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return 1;
		//return socketUtil.startSocket();
	}
}
