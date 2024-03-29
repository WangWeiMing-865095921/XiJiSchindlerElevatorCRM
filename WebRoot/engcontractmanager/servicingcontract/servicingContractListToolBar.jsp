 <%@ page contentType="text/html;charset=GBK" %>
<%@ taglib uri="/WEB-INF/tld/struts-bean.tld" prefix="bean"%>
<%@ taglib uri="/WEB-INF/tld/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/tld/struts-logic.tld" prefix="logic"%>
<%@ page import="com.gzunicorn.common.util.SysConfig" %>

<script language="javascript">
//关于ToolBar
function CreateToolBar(){
  SetToolBarHandle(true);
  SetToolBarHeight(20);
  AddToolBarItemEx("ViewBtn","../../common/images/toolbar/search.gif","","",'<bean:message key="toolbar.search"/>',"65","0","searchMethod()");
  AddToolBarItemEx("ViewBtn","../../common/images/toolbar/view.gif","","",'<bean:message key="toolbar.read"/>',"65","1","viewMethod()");
  
  <%-- 是否有可写的权限--%>
  <logic:equal name="<%=SysConfig.TOOLBAR_RIGHT%>" property="nwgchangeContract" value="Y"> 
    AddToolBarItemEx("AddBtn","../../common/images/toolbar/add.gif","","",'新 建',"65","1","addFromQuoteMethod()");
    AddToolBarItemEx("ModifyBtn","../../common/images/toolbar/edit.gif","","",'<bean:message key="toolbar.modify"/>',"65","1","modifyMethod()");
    AddToolBarItemEx("ReferBtn","../../common/images/toolbar/digital_confirm.gif","","",'下 达',"65","1","referMethod2()");
    AddToolBarItemEx("ReferBtn","../../common/images/toolbar/digital_confirm.gif","","",'提交',"65","1","referMethod()");
    AddToolBarItemEx("DelBtn","../../common/images/toolbar/delete.gif","","",'<bean:message key="toolbar.delete"/>',"65","1","deleteMethod()");
  </logic:equal>
  
  //AddToolBarItemEx("ExcelBtn","../../common/images/toolbar/xls.gif","","",'<bean:message key="toolbar.xls"/>',"90","1","excelMethod()");
  window.document.getElementById("toolbar").innerHTML=GenToolBar("TabToolBar_Manage","TextToolBar_Black","Style_Over","Style_Out","Style_Down","Style_Check");
}


//查询
function searchMethod(){
  //serveTableForm.genReport.value = "";
  serveTableForm.target = "_self";
  document.serveTableForm.submit();
}

//查看
function viewMethod(){
if(serveTableForm.ids)
{
  var l = document.serveTableForm.ids.length;
  if(l)
  {
    for(i=0;i<l;i++)
    {
      if(document.serveTableForm.ids[i].checked == true)
      {
        window.location = '<html:rewrite page="/wgchangeContractAction.do"/>?id='+document.serveTableForm.ids[i].value +'&method=toDisplayRecord';
        return;
      }
    }
    if(l >0)
    {
      alert("<bean:message key="javascript.role.alert2"/>");
    }
  }else if(document.serveTableForm.ids.checked == true)
  {
    window.location = '<html:rewrite page="/wgchangeContractAction.do"/>?id='+document.serveTableForm.ids.value +'&method=toDisplayRecord';
  }
  else
  {
    alert("<bean:message key="javascript.role.alert2"/>");
  }
}
}

/* //新增 2016-5-4
function addFromQuoteMethod(){
  window.location = '<html:rewrite page="/wgchangeContractSearchAction.do"/>?method=toNextSearchRecord';
} */

//新增 
function addFromQuoteMethod(){
  window.location = '<html:rewrite page="/wgchangeContractAction.do"/>?method=toPrepareAddRecord';
}



//修改
function modifyMethod(){
if(serveTableForm.ids)
{
  var l = document.serveTableForm.ids.length;
  if(l)
  {
    for(i=0;i<l;i++)
    {   	
      if(document.serveTableForm.ids[i].checked == true )
      {
    	  var submitType=serveTableForm.submitType[i].value;//提交状态
    	if(submitType=="未提交"||submitType=="驳回"){//未提交
        window.location = '<html:rewrite page="/wgchangeContractAction.do"/>?id='+document.serveTableForm.ids[i].value +'&method=toPrepareUpdateRecord';
        return; 
    	 }else{
    		alert("该记录已提交,请选择未提交的记录!");
    	 }
    	return;
      }
    }
    if(l >0)
    {
      alert("<bean:message key="javascript.role.alert1"/>");
    }
  }else if(document.serveTableForm.ids.checked == true)
  {  
	  var submitType=serveTableForm.submitType.value;//提交状态
	  if(submitType=="未提交"||submitType=="驳回"){//未提交
        window.location = '<html:rewrite page="/wgchangeContractAction.do"/>?id='+document.serveTableForm.ids.value +'&method=toPrepareUpdateRecord';       
        return; 
	  }else{
  		alert("该记录已提交,请选择未提交的记录!");
  	 }
	  return;
  }
  else
  {
    alert("<bean:message key="javascript.role.alert1"/>");
  }
}

}

//删除
function deleteMethod(){
if(serveTableForm.ids)
{
  var l = document.serveTableForm.ids.length;
  
  if(l)
  {
    for(i=0;i<l;i++)
    {

      if(document.serveTableForm.ids[i].checked == true )
      {
    	var submitType=serveTableForm.submitType[i].value;//提交状态
		if(submitType=="未提交"||submitType=="驳回"){//未提交
        if(confirm("<bean:message key="javascript.role.deletecomfirm"/>"+document.serveTableForm.ids[i].value))
        {
          window.location = '<html:rewrite page="/wgchangeContractAction.do"/>?id='+document.serveTableForm.ids[i].value +'&method=toDeleteRecord';
        }
		}else{
			alert("该记录已提交,请选择未提交的记录!"); 
		}
        return;
      }
    }
    if(l >0)
    {
      alert("<bean:message key="javascript.role.alert3"/>");
    }
  }
  else if(document.serveTableForm.ids.checked == true )
  {
	  var submitType=serveTableForm.submitType.value;//提交状态
	  if(submitType=="未提交"||submitType=="驳回"){//未提交
    if(confirm("<bean:message key="javascript.role.deletecomfirm"/>"+document.serveTableForm.ids.value))
      {
        window.location = '<html:rewrite page="/wgchangeContractAction.do"/>?id='+document.serveTableForm.ids.value +'&method=toDeleteRecord';
      }
	  }else{
		  alert("该记录已提交,请选择未提交的记录!");  
	  }
	  return;
  }
  else
  {
    alert("<bean:message key="javascript.role.alert3"/>");
  }
}
}

// 提交
function referMethod(){
if(serveTableForm.ids)
{
	var l = document.serveTableForm.ids.length;
	if(l)
	{
		for(i=0;i<l;i++)
		{
			if(document.serveTableForm.ids[i].checked == true)
			{
				var submitType=serveTableForm.submitType[i].value;//提交状态
				if(submitType=="未提交"||submitType=="驳回"){//未提交
					window.location = '<html:rewrite page="/wgchangeContractAction.do"/>?id='+document.serveTableForm.ids[i].value +'&method=toReferRecord';
				}else{
					alert("该记录已提交,请选择未提交的记录!"); 
				}
				return;
			}
		}
		if(l>0){
			alert("<bean:message key="javascript.role.alert.jilu"/>");
		}
	}
	else if(document.serveTableForm.ids.checked == true)
	{
		var submitType=serveTableForm.submitType.value;//提交状态
		if(submitType=="未提交"||submitType=="驳回"){
			var url="";
			window.location = '<html:rewrite page="/wgchangeContractAction.do"/>?id='+document.serveTableForm.ids.value +'&method=toReferRecord';
			return;
		}else{
			alert("该记录已提交,请选择未提交的记录!");
			return;
		}
	}
	else
	{
		alert("<bean:message key="javascript.role.alert.jilu"/>");
	}
}	
}


function referMethod2(){
	if(serveTableForm.ids)
	{
		var l = document.serveTableForm.ids.length;
		if(l)
		{
			for(i=0;i<l;i++)
			{
				if(document.serveTableForm.ids[i].checked == true)
				{
					var auditStatus=serveTableForm.auditStatus[i].value;//审核状态
					var taskSubFlag=serveTableForm.taskSubFlag[i].value;//下达状态
					if(auditStatus=="已审核" && taskSubFlag=="未下达"){//未提交
						window.location = '<html:rewrite page="/wgchangeContractAction.do"/>?id='+document.serveTableForm.ids[i].value +'&method=totask';
					}else{
						alert("该记录未通过或未审核或已下达,请选择已审核,未下达的记录!"); 
					}
					return;
				}
			}
			if(l>0){
				alert("<bean:message key="javascript.role.alert.jilu"/>");
			}
		}
		else if(document.serveTableForm.ids.checked == true)
		{
			var auditStatus=serveTableForm.auditStatus.value;//审核状态
			var taskSubFlag=serveTableForm.taskSubFlag.value;//下达状态
			if(auditStatus=="已审核" && taskSubFlag=="未下达"){
				var url="";
				window.location = '<html:rewrite page="/wgchangeContractAction.do"/>?id='+document.serveTableForm.ids.value +'&method=totask';
				return;
			}else{
				alert("该记录未通过或未审核或已下达,请选择已审核,未下达的记录!");
				return;
			}
		}
		else
		{
			alert("<bean:message key="javascript.role.alert.jilu"/>");
		}
	}
}
//导出Excel
function excelMethod(){
  serveTableForm.genReport.value="Y";
  serveTableForm.target = "_blank";
  document.serveTableForm.submit();
}

</script>

  <tr>
    <td width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr>
          <td height="22" class="table_outline3" valign="bottom" width="100%">
            <div id="toolbar" align="center">
            <script language="javascript">
              CreateToolBar();
            </script>
            </div>
            </td>
        </tr>
      </table>
    </td>
  </tr>
</table>