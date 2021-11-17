package edu.nju.practice.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HdfsComponent 
{
	@Autowired
	private SocketUtil socketUtil;
	
	public int modifyFile()
	{
		return socketUtil.startSocket();
	}
}
