package com.dalgs.infor.pa.tests;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.dalgs.infor.pa.activity.GraphGetDriveItem;

class GraphGetDriveItemTests {

	@BeforeEach
	void setUp() throws Exception {
	}

	@Test
	void test() {
        String driveId = "b!csOoXNWjqkO6J_ggNnLtIeGd_1m-gZ5Lpc20x49rWYHRPjvMMXbbSKeTDOzBv8nk";
		GraphGetDriveItem activity = new GraphGetDriveItem();
        activity.setClientId("<!CLIENT_ID>");
        activity.setClientSecret("<!CLIENT_SECRET>");
        activity.setDriveId(driveId);
        activity.setPath("<!ROOT_FOLDER>");
        activity.setTenantId("<!TENANT_ID>");
        
        JSONObject item = activity.getItem(driveId, "root:/ROSTERS/roster file.csv");
        
        System.out.println(item);
	}

}
