package com.renren.userinc.servicemonitor.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.renren.userinc.servicemonitor.core.Constants;
import com.xiaonei.platform.core.opt.DataAccessMgr;
import com.xiaonei.platform.core.opt.OpBatchUpdate;
import com.xiaonei.platform.core.opt.OpUniq;
import com.xiaonei.platform.core.opt.OpUpdate;


public class DBService {
	private static DBService appIdService = new DBService();
	
	private int appId = 1;
	
	private DBService(){
		String selectSql = "select id from alert_business where name = '" + Constants.TARGET_PROJECT_NAME + "'";
		OpUniq opUniq = new OpUniq(selectSql, Constants.META_DATA_DATASOURCE);
		
		try {
			appId = DataAccessMgr.getInstance().queryId(opUniq);
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		
		if(appId <= 0){
			String sql = "insert into alert_business(name) values('" + Constants.TARGET_PROJECT_NAME + "')";
			OpUpdate opUpdate = new OpUpdate(sql, Constants.META_DATA_DATASOURCE);
			try {
				appId = DataAccessMgr.getInstance().insertReturnId(opUpdate);
			} catch (SQLException e) {
				appId = -1;
				e.printStackTrace();
			}
			
		}
		
	}
	
	public static DBService getInstance(){
		return appIdService;
	}
	
	public int getAppId(){
		return appId;
	}
	
	public void storeMetaData(){
		List<OpBatchUpdate> batchInsert = new ArrayList<OpBatchUpdate>();
		
		String deleteSql = "delete from alert_method where business_id = " + appId;
		OpUpdate opDelete = new OpUpdate(deleteSql, Constants.META_DATA_DATASOURCE);
		try {
			DataAccessMgr.getInstance().update(opDelete);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(Constants.GENERATED_FILE_DIR + "/" + Constants.FILE_META_DATA));
			String str = null;
			while((str = br.readLine()) != null){
				String[] metaDataArray = str.split(" ");
				int id = Integer.valueOf(metaDataArray[0]);
				String desc = metaDataArray[1] + "." + metaDataArray[2];
				String sql = "insert into alert_method values(" + id + "," + appId + ", '" + desc + "')";
				OpBatchUpdate opUpdate = new OpBatchUpdate(sql, Constants.META_DATA_DATASOURCE);
				batchInsert.add(opUpdate);
			}
			try {
				DataAccessMgr.getInstance().insertBatchReturnFirstSqlId(batchInsert);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}  
	}
	
	public static void main(String[] args){
		System.out.println(DBService.getInstance().getAppId());
	}
}
