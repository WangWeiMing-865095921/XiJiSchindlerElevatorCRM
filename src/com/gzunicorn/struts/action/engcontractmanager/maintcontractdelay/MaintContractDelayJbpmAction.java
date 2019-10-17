package com.gzunicorn.struts.action.engcontractmanager.maintcontractdelay;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;
import org.apache.struts.actions.DispatchAction;
import org.apache.struts.util.MessageResources;

import com.gzunicorn.bean.ProcessBean;
import com.gzunicorn.common.dao.DBInterface;
import com.gzunicorn.common.dao.ObjectAchieveFactory;
import com.gzunicorn.common.grcnamelist.Grcnamelist1;
import com.gzunicorn.common.logic.BaseDataImpl;
import com.gzunicorn.common.util.CommonUtil;
import com.gzunicorn.common.util.DebugUtil;
import com.gzunicorn.common.util.HibernateUtil;
import com.gzunicorn.common.util.PropertiesUtil;
import com.gzunicorn.common.util.SysConfig;
import com.gzunicorn.common.util.SysRightsUtil;
import com.gzunicorn.common.util.WorkFlowConfig;
import com.gzunicorn.hibernate.basedata.customer.Customer;

import com.gzunicorn.hibernate.engcontractmanager.maintcontractdelaymaster.MaintContractDelayMaster;
import com.gzunicorn.hibernate.engcontractmanager.maintcontractdelayprocess.MaintContractDelayProcess;
import com.gzunicorn.hibernate.engcontractmanager.maintcontractdetail.MaintContractDetail;
import com.gzunicorn.hibernate.engcontractmanager.maintcontractmaster.MaintContractMaster;
import com.gzunicorn.hibernate.viewmanager.ViewLoginUserInfo;
import com.gzunicorn.workflow.bo.JbpmExtBridge;

public class MaintContractDelayJbpmAction extends DispatchAction {

	Log log = LogFactory.getLog(MaintContractDelayJbpmAction.class);
	
	BaseDataImpl bd = new BaseDataImpl();
	
	/**
	 * Method execute
	 * 
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return ActionForward
	 */
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		/** **********开始用户权限过滤*********** */
		SysRightsUtil.filterModuleRight(request, response,SysRightsUtil.NODE_ID_FORWARD + "myTaskOA", null);
		/** **********结束用户权限过滤*********** */
		ActionForward forward = super.execute(mapping, form, request,response);
		return forward;
	}
	
	
	/**
	 * Get the navigation description from the properties file by navigation
	 * key;
	 * 
	 * @param request
	 * @param navigation
	 */

	private void setNavigation(HttpServletRequest request, String navigation) {
		Locale locale = this.getLocale(request);
		MessageResources messages = getResources(request);
		request.setAttribute("navigator.location", messages.getMessage(locale,
				navigation));
	}
	
	/**
	 * 进入修改界面
	 * 
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return ActionForward
	 */

	public ActionForward  toPrepareUpdateApprove(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,
			HttpServletResponse response)  throws IOException, ServletException{

		request.setAttribute("navigator.location", "维保报价申请表审核 >> 修  改");
		ActionErrors errors = new ActionErrors();
		
		display(form, request, errors);
		
		saveToken(request); //生成令牌，防止表单重复提交

		if (!errors.isEmpty()) {
			saveErrors(request, errors);
		}
		return mapping.findForward("toApproveModify");
	}
	/**
	 * 保存修改
	 * page or modifiy page
	 * 
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return ActionForward
	 */
	public ActionForward toSaveUpdateApprove(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		
		HttpSession session = request.getSession();
		DynaActionForm dform = (DynaActionForm)form;
		ViewLoginUserInfo userInfo = (ViewLoginUserInfo)session.getAttribute(SysConfig.LOGIN_USER_INFO);
		ActionErrors errors = new ActionErrors();
		
		Session hs = null;
		Transaction tx = null;
		
		String approveresult=(String)dform.get("approveresult");
		String taskname=(String)dform.get("taskname");
		String jnlno=(String) dform.get("jnlno");// 延保流水号

		String flowname=(String) dform.get("flowname");
		//if(flowname!=null && !flowname.trim().equals("")){
		//	flowname=new String(flowname.getBytes("ISO-8859-1"),"gbk");
		//}

		JbpmExtBridge jbpmExtBridge=null;
		
		//防止表单重复提交
		if(isTokenValid(request, true)){
			try {
				
				hs = HibernateUtil.getSession();
				tx = hs.beginTransaction();			
				MaintContractDelayMaster master = (MaintContractDelayMaster) hs.get(MaintContractDelayMaster.class, jnlno);
				
				String operid = master.getOperId();
				String operDate = master.getOperDate();
				String maintDivision = bd.getName("Loginuser", "grcid", "userid", operid);
	
				String processDefineID = Grcnamelist1.getProcessDefineID("maintcontractdelaymaster", operid);// 流程环节id
	
	    		/**======== 流程审批启动开始 ========*/
				jbpmExtBridge=new JbpmExtBridge();
		    	ProcessBean pd=jbpmExtBridge.getProcessBeanUseTI(Long.parseLong(dform.get("taskid").toString()));
		    	
		    	pd.addAppointActors("");// 将动态添加的审核人清除。
				Grcnamelist1.setJbpmAuditopers_class(pd, processDefineID, PropertiesUtil.getProperty("MaintStationManagerJbpm"), maintDivision, master.getMaintStation());// 添加审核人
	
		    	pd=jbpmExtBridge.goToNext(Long.parseLong(dform.get("taskid").toString()),approveresult,userInfo.getUserID(),null);//审核
		    	/**======== 流程审批启动结束 ========*/
		    	
		    	//保存审批流程相关信息
		    	MaintContractDelayProcess process=new MaintContractDelayProcess();
				process.setJnlno(jnlno.trim());
				process.setTaskId(new Integer(pd.getTaskid().intValue()));//任务号
				process.setTaskName(taskname);//任务名称
				process.setTokenId(pd.getToken());//流程令牌
				process.setUserId(userInfo.getUserID());
				process.setDate1(CommonUtil.getToday());
				process.setTime1(CommonUtil.getTodayTime());
				process.setApproveResult(approveresult);
				process.setApproveRem((String)dform.get("approverem"));
				hs.save(process);
	
		    	//保存主信息
				master.setJnlno(jnlno);
				master.setSubmitType("Y");// 提交标志
		    	master.setTokenId(pd.getToken());
		    	master.setStatus(pd.getStatus());
		    	master.setProcessName(pd.getNodename());
				master.setOperId(operid);// 录入人
				master.setOperDate(operDate);// 录入时间
				hs.saveOrUpdate(master);
						
				// 更新延保明细
				String[] rowids = request.getParameterValues("rowid");
				String[] realityEdates = request.getParameterValues("realityEdate");
				
				String sql = "update MaintContractDelayDetail set delayEdate = ? from MaintContractDelayDetail where rowid=?";
	
				Connection conn = hs.connection();
				PreparedStatement ps = conn.prepareStatement(sql);
	
				for(int i = 0; i < rowids.length; i++){	
					ps.setString(1, realityEdates[i]);
					ps.setString(2, rowids[i]);
					ps.addBatch();
				}
				ps.executeBatch();
	
				tx.commit();
		
			} catch (Exception e2) {
				errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("insert.fail"));
				if (jbpmExtBridge != null) {
					jbpmExtBridge.setRollBack();
				}
				if (tx != null) {
					tx.rollback();
				}
				e2.printStackTrace();
				log.error(e2.getMessage());
				
			} finally {
				if(jbpmExtBridge!=null){				
					jbpmExtBridge.close();
				}
				try {
					if(hs!=null){
						hs.close();
					}
				} catch (HibernateException hex) {
					log.error(hex.getMessage());
					DebugUtil.print(hex, "Hibernate close error!");
				}
			}
		}else{
			saveToken(request);
			errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("navigator.submit.error"));//不能重复提交
		}

		ActionForward forward = null;

		if(errors.isEmpty()){
			errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("application.submit.sucess"));
		}
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
		}
		request.setAttribute("display","Y");
		forward=mapping.findForward("returnApprove");
		
		return forward;
	}
	
	
	/**
	 * 准备审批，注意检查是否已有审批内容，或是否已审
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 * @throws ServletException
	 */
	public ActionForward toPrepareApprove(ActionMapping mapping,
		ActionForm form, HttpServletRequest request,
		HttpServletResponse response) throws IOException, ServletException {

		request.setAttribute("navigator.location", "维保合同延保审核 >> 审 批");

		ActionErrors errors = new ActionErrors();

		display(form, request, errors); //查看方法
		
		request.setAttribute("approve", "Y");
		
		saveToken(request); //生成令牌，防止表单重复提交
		
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
		}
		return mapping.findForward("toApprove");
	}
	
	/**
	 * 保存提交审批内容
	 * 此方法将扩展为：保存并提交、或保存不提交，根据提交参数区分
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 * @throws ServletException
	 */
	public ActionForward toSaveApprove(ActionMapping mapping,
			ActionForm form, HttpServletRequest request,HttpServletResponse response) 
					throws IOException, ServletException {

		ActionForward forward = null;
		ActionErrors errors = new ActionErrors();

		ViewLoginUserInfo userInfo = (ViewLoginUserInfo) request.getSession().getAttribute(SysConfig.LOGIN_USER_INFO);
		DynaActionForm dform = (DynaActionForm) form;

		String flowname = (String) dform.get("flowname");// 流程名称
		//if (flowname != null && !flowname.trim().equals("")) {
		//	flowname = new String(flowname.getBytes("ISO-8859-1"), "gbk");
		//}
		
		String jnlno = (String) dform.get("jnlno");// 延保流水号
		String taskname = (String) dform.get("taskname");// 任务名称
		Long taskid = (Long) dform.get("taskid");// 任务名称
		String approveresult = (String) dform.get("approveresult");// 审批结果
		String approverem = (String) dform.get("approverem");// 审批意见
			
		Session hs = null;
		Transaction tx = null;
		JbpmExtBridge jbpmExtBridge = null;
			
		//防止表单重复提交
		if(isTokenValid(request, true)){
			try {
				hs = HibernateUtil.getSession();
				tx = hs.beginTransaction();
				
				if (jnlno != null && !jnlno.trim().equals("")) {
					
					MaintContractDelayMaster mcdm = (MaintContractDelayMaster) hs.get(MaintContractDelayMaster.class, jnlno);
					
					String processDefineID = Grcnamelist1.getProcessDefineID("maintcontractdelaymaster", mcdm.getOperId());// 流程环节id
					
					/*=============== 流程审批启动开始 =================*/
					jbpmExtBridge = new JbpmExtBridge();
					HashMap paraMap = new HashMap();
					ProcessBean pd = jbpmExtBridge.getProcessBeanUseTI(Long.parseLong(dform.get("taskid").toString()));
					
					pd.addAppointActors("");// 将动态添加的审核人清除。				
					Grcnamelist1.setAllProcessAuditoper(pd, processDefineID, approveresult.replace("提交", ""), mcdm.getOperId());// 添加审核人
					
					pd = jbpmExtBridge.goToNext(taskid, approveresult, userInfo.getUserID(), paraMap);// 审核
					/*=============== 流程审批启动结束 =================*/
					
					// 保存审批流程相关信息
					MaintContractDelayProcess process = new MaintContractDelayProcess();
					process.setJnlno(jnlno);
					process.setTaskId(pd.getTaskid().intValue());// 任务号
					process.setTaskName(taskname);// 任务名称
					process.setTokenId(pd.getToken());// 流程令牌
					process.setUserId(userInfo.getUserID());
					process.setDate1(CommonUtil.getToday());
					process.setTime1(CommonUtil.getTodayTime());
					process.setApproveResult(approveresult);
					process.setApproveRem(approverem);
					hs.save(process);
					
					// 保存主信息
					mcdm.setTokenId(pd.getToken());
					mcdm.setStatus(pd.getStatus());
					mcdm.setProcessName(pd.getNodename());			
					hs.save(mcdm);
					
					String[] rowids = request.getParameterValues("rowid");
					String[] realityEdates = request.getParameterValues("realityEdate");
					
					// 当审批通过时，将延保日期更新到维保管理合同明细中的实际结束日期	
					/*if(pd.getStatus() == 1){
	
						String sql = "update MaintContractDetail set realityEdate = ? from MaintContractDetail where rowid=?";
		
						Connection conn = hs.connection();
						PreparedStatement ps = conn.prepareStatement(sql);
		
						for(int i = 0; i < rowids.length; i++){	
							ps.setString(1, realityEdates[i]);
							ps.setString(2, rowids[i]);
							ps.addBatch();						
						}
						ps.executeBatch();
						

					}*/
					
					tx.commit();	
					
					if(pd.getStatus() == 1){
						//生成作业计划
						for (String rowid : rowids) {
							CommonUtil.toMaintenanceWorkPlan(rowid, null, userInfo, errors);
						}
					}

				}

			} catch (Exception e) {
				if (tx != null) {
					tx.rollback();
				}
				if (jbpmExtBridge != null) {
					jbpmExtBridge.setRollBack();
				}
				errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("application.submit.fail"));
				e.printStackTrace();
			} finally {
				if (jbpmExtBridge != null) {
					jbpmExtBridge.close();
				}
				if (hs != null) {
					hs.close();
				}
			}
			
		}else{
			saveToken(request);
			errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("navigator.submit.error"));//不能重复提交
		}

		if (errors.isEmpty()) {
			errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("application.submit.sucess"));
		}
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
		}
		request.setAttribute("display", "Y");
		forward = mapping.findForward("returnApprove");
		return forward;
	}
	
	
	/**
	 * 页面查看内容
	 * @param form
	 * @param request
	 * @param errors
	 * @throws UnsupportedEncodingException
	 */
	public void display(ActionForm form, HttpServletRequest request ,ActionErrors errors) throws UnsupportedEncodingException{
		
		HttpSession session = request.getSession();
		ViewLoginUserInfo userInfo = (ViewLoginUserInfo)session.getAttribute(SysConfig.LOGIN_USER_INFO);
		DynaActionForm dform = (DynaActionForm) form;
				
		String id = request.getParameter("id");
		
		String tokenid = request.getParameter("tokenid");// 流程令牌
		if (tokenid == null) {
			tokenid = (String) dform.get("tokenid");
		}
		String taskid = request.getParameter("taskid");// 任务id
		if (taskid == null) {
			taskid = (String) dform.get("taskid");
		}
		String taskname = request.getParameter("taskname");// 当前任务名称
		if (taskname == null) {
			taskname = (String) dform.get("taskname");
		}else{
			taskname = new String(taskname.getBytes("ISO-8859-1"), "gbk");
		}
		String tasktype = request.getParameter("tasktype");// 任务类型
		if (tasktype == null) {
			tasktype = (String) dform.get("tasktype");
		}
		
		String jnlno = "";
		String billNo = "";
		Integer status = null;
		MaintContractDelayMaster delayMaster = null;	
		List maintContractDetailList = null;
		
		Session hs = null;
		if (tokenid != null) {
			try {
				hs = HibernateUtil.getSession();
				//维保合同延保主表
				Query query = hs.createQuery("from MaintContractDelayMaster where tokenId = '"+ tokenid + "'");
				
				List list = query.list();				
				if(list != null && list.size() > 0){
					delayMaster = (MaintContractDelayMaster) list.get(0);
					jnlno = delayMaster.getJnlno(); //延保流水号
					billNo = delayMaster.getBillno();// 维保合同管理流水号
					status = delayMaster.getStatus();//审核状态
				}

				query = hs.createQuery("from MaintContractMaster where billNo = '"+billNo+"'");
				list = query.list();
				if (list != null && list.size() > 0) {
	
					// 维保合同管理主信息
					MaintContractMaster master = (MaintContractMaster) list.get(0);															
					master.setAttn(bd.getName(hs, "Loginuser","username", "userid",master.getAttn()));// 经办人
					master.setAuditOperid(bd.getName(hs, "Loginuser", "username", "userid",master.getAuditOperid()));// 审核人
					master.setTaskUserId(bd.getName(hs, "Loginuser", "username", "userid",master.getTaskUserId()));// 下达人	
					master.setMaintDivision(bd.getName(hs, "Company", "comname", "comid",master.getMaintDivision()));// 维保分部														
		
					// 甲方单位信息
					Customer companyA = (Customer) hs.get(Customer.class,master.getCompanyId());															
					// 乙方方单位信息
					Customer companyB = (Customer) hs.get(Customer.class,master.getCompanyId2());
										
					// 维保延保管理明细列表

					String sql = "select a,b.delayEdate from MaintContractDetail a,MaintContractDelayDetail b"
							+ " where a.rowid = b.rowid"
							+ " and b.jnlno = '" + jnlno + "'"
							+ " and a.assignedMainStation like '"
							+ delayMaster.getMaintStation() + "'";
					
					query = hs.createQuery(sql);	
					list = query.list();
					maintContractDetailList = new ArrayList();
					List signWayList = bd.getPullDownList("MaintContractDetail_SignWay");// 签署方式下拉列表
					List elevatorTypeList = bd.getPullDownList("ElevatorSalesInfo_type");// 电梯类型下拉列表
					for (Object object : list) {
						Object[] objs = (Object[]) object;
						MaintContractDetail detail = (MaintContractDetail) objs[0];
						detail.setRealityEdate((String) objs[1]);
						detail.setSignWay(bd.getOptionName(detail.getSignWay(), signWayList));
						detail.setElevatorType(bd.getOptionName(detail.getElevatorType(), elevatorTypeList));
						maintContractDetailList.add(detail);
					}
					
					//审批流程信息
					query = hs.createQuery("from MaintContractDelayProcess where jnlno = '"+ jnlno + "' order by itemId");
					List processApproveList = query.list();
					for (Object object : processApproveList) {
						MaintContractDelayProcess process = (MaintContractDelayProcess) object;
						process.setUserId(bd.getName(hs, "Loginuser", "username", "userid", process.getUserId()));
					}
					request.setAttribute("processApproveList",processApproveList);
					
					//审核下拉框内容
					List tranList=this.getTransition(hs.connection(),3,null,Long.parseLong(taskid));
					request.setAttribute("ResultList",tranList);
					
					request.setAttribute("maintContractBean", master);	
					request.setAttribute("companyA",companyA);
					request.setAttribute("companyB",companyB);
					request.setAttribute("maintContractDetailList", maintContractDetailList);
					request.setAttribute("maintStationList", bd.getMaintStationList(userInfo.getComID()));// 维保站下拉列表
							
				} else {
					errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("display.recordnotfounterror"));
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					hs.close();
				} catch (HibernateException hex) {
					log.error(hex.getMessage());
					DebugUtil.print(hex, "HibernateUtil Hibernate Session ");
				}
			}
		}
		
		if(request.getAttribute("error") == null || request.getAttribute("error").equals("")){//准备进入审批页面
			dform.set("tokenid",new Long(tokenid));
			dform.set("taskid",new Long(taskid));				
			dform.set("taskname",taskname);
			dform.set("flowname", WorkFlowConfig.getProcessDefine("enginequotemasterProcessName"));
			dform.set("status", status);
			dform.set("jnlno",jnlno);
			dform.set("id",jnlno);				
			dform.set("tasktype",tasktype);
		}
	
	}
	
	/**
	 * Type 0:根据流程定义，node start找; 1:根据task id找;2:根据node id 找,3:TaskInstance
	 * @param type
	 * @param process	流程
	 * @param tasknode  task/node
	 * @return
	 * @throws SQLException 
	 */
	public List getTransition(Connection con,int type,String process,long tasknode) throws SQLException{
		DBInterface db=ObjectAchieveFactory.getDBInterface(ObjectAchieveFactory.MssqlImp);
		db.setCon(con);
		String sql="Sp_JbpmGetTransition "+type+",'"+process+"',"+tasknode;
		//System.out.println(sql);
		return db.queryToList(sql);
		
	}
	
}
