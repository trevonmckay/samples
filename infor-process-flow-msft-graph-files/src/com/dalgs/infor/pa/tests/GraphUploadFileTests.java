package com.dalgs.infor.pa.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.dalgs.infor.pa.activity.GraphUploadFile;

public class GraphUploadFileTests {
    
    @Test
    void testUploadFile() {
    	String driveId = "<!DRIVE_ID>";
        String uploadPath = "root:/ROSTERS";
        String fileName = "now.csv";
        String content = "FIRST_NAME,LAST_NAME,EMAIL\r\SARAH,PARKER,PARK.SAR_EXP.COM\r\nJOHN,WAYNE,JOHN_EXP.COM";
        String contentType = "text/csv";
    	
    	GraphUploadFile activity = new GraphUploadFile();
        activity.setClientId("<!CLIENT_ID>");
        activity.setClientSecret("<!CLIENT_SECRET>");
        activity.setDriveId(driveId);
        activity.setPath("<!ROOT_FOLDER>");
        activity.setTenantId("<!TENANT_ID>");
        
        activity.uploadFile(driveId, uploadPath, fileName, content, contentType);  
    }
    
    @Test
    void testUploadLargeFile() {
    	String filePath = "<!FILE_PATH>";
    	String driveId = "<!DRIVE_ID>";
        String uploadPath = "root:/ROSTERS";
        String fileName = "Reed.xml";
        String contentType = "application/xml";
        
        String content = null;
		try {
			byte[] encoded = Files.readAllBytes(Paths.get(filePath));
			content = new String(encoded, "UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
    	GraphUploadFile activity = new GraphUploadFile();
        activity.setClientId("<!CLIENT_ID>");
        activity.setClientSecret("<!CLIENT_SECRET>");
        activity.setDriveId(driveId);
        activity.setPath("<!ROOT_FOLDER>");
        activity.setTenantId("<!TENANT_ID>");
        
        activity.uploadFile(driveId, uploadPath, fileName, content, contentType);
    }
    
}