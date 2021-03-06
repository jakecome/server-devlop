package com.glodon.pcop.cimstatsvc.statistic;

import com.glodon.pcop.cim.engine.dataServiceEngine.dataServiceBureau.CimDataSpace;
import com.glodon.pcop.cim.engine.dataServiceEngine.util.CimDataEngineConstant;
import com.glodon.pcop.cim.engine.dataServiceEngine.util.factory.CimDataEngineComponentFactory;
import com.glodon.pcop.cim.engine.dataServiceModelAPI.util.exception.DataServiceUserException;
import com.glodon.pcop.cimstatsvc.constant.ObjectTypeIdConstant;
import com.glodon.pcop.cimstatsvc.dao.DbExecute;
import net.sf.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.glodon.pcop.cimstatsvc.dao.BaseExDao.updateOrInsertOneToCim;
import static com.glodon.pcop.cimstatsvc.util.DateUtils.getDayRange;
import static com.glodon.pcop.cimstatsvc.util.DateUtils.getMonthRangeFormNow;


public class SafeStatistic {
    //    private static String allProjectId = "select distinct projectId from `GLD_IH_FACT_safetyProblem`  where projectId Not in (" +
//            "select projectId from `GLD_IH_FACT_projectDataFetchCfg`  where fetchKind.indexOf('2') > -1)";
    private static String projectTable = CimDataEngineConstant.CLASSPERFIX_IHFACT + ObjectTypeIdConstant.PROJECT;
    private static String projectDataFetchCfgTable = CimDataEngineConstant.CLASSPERFIX_IHFACT + ObjectTypeIdConstant.FETCH_CFG;
    private static String safetyProblemTable = CimDataEngineConstant.CLASSPERFIX_IHFACT + ObjectTypeIdConstant.SAFETY_PROULEM;

    private static String allProjectId = "select projectId from `" + projectTable + "` where projectId  Not in (" +
            "select distinct projectId from `" + projectDataFetchCfgTable + "`  where fetchKind.indexOf('2') > -1)";

    private static String baseSelect = "select projectId,countProject(*) as num  ";
    private static String selectProblem = "from `" + safetyProblemTable + "`  where  projectId = \"_PROJECTID_\" ";
    private static String countExistingProblem = selectProblem + "and   status in ['1','2','4'] ";
    //统计问题数量总和
    private static String existingProblemCount = baseSelect + countExistingProblem;
    private static String matterProblemCount = baseSelect + countExistingProblem + " and problemLevel = '2' ";
    private static String seriousProblemCount = baseSelect + countExistingProblem + " and  problemLevel = '3' ";
    private static String outOfDateProblemCount = baseSelect + countExistingProblem + " and changeLimitTime < \"_NOW_ \"";

    //未肖项按整改人统计
    private static String byPerson = "select projectId,countProject(*) as num,changeName,changeId,problemLevel " + countExistingProblem + "group by changeName,problemLevel";

    //未肖项按整改单位统计
    private static String byUnit = "select projectId,countProject(*) as num,unitName,unitId,problemLevel " + countExistingProblem + "group by unitName,problemLevel";

    //最近一个月问题按类型统计
    private static String byType = "select projectId,countProject(*) as num,problemTypeId,problemTypeName " + countExistingProblem + " and checkTime  _MONTH_  group by problemTypeId";

    /*一个月趋势
        每天定时统计一次
        新增隐患   检查时间为今天的
        消除隐患   更新时间为今天的数量
        未肖项次数 每天的时间
    */
    private static String newProblem = baseSelect + selectProblem + "and checkTime  _TODAY_ ";
    private static String closeProblem = baseSelect + selectProblem + "and   status in ['0','3'] and updateTime  _TODAY_ ";
    public static String count() {
        CimDataSpace cds = CimDataEngineComponentFactory.connectInfoDiscoverSpace(DbExecute.dbName);
        try {
            countImpl(cds);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cds != null) {
                cds.closeSpace();
            }
        }
        return "";
    }
    public static String countImpl(CimDataSpace cds) {
        List<String> list = DbExecute.executeQuery(cds,allProjectId);
        for (int i = 0; i < list.size(); i++) {
            JSONObject obj = new JSONObject().fromObject(list.get(i));
            String projectId = obj.getString("projectId");
            if( projectId != null ){
                if(!projectId.toLowerCase().equals("null")  && !projectId.toLowerCase().equals("")){
                    countProject(cds,projectId);
                }
            }
        }
        return "";
    }


    public static String countByDay() {
        CimDataSpace cds = CimDataEngineComponentFactory.connectInfoDiscoverSpace(DbExecute.dbName);
        try {
            countImpl(cds);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cds != null) {
                cds.closeSpace();
            }
        }
        return "";
    }

    public static String countByDay(CimDataSpace cds) {
        List<String> list = DbExecute.executeQuery(cds,allProjectId);
        for (int i = 0; i < list.size(); i++) {
            JSONObject obj = new JSONObject().fromObject(list.get(i));
            String projectId = obj.getString("projectId");
            if( projectId != null ){
                if(!projectId.toLowerCase().equals("null")  && !projectId.toLowerCase().equals("")){
                    countByThend(cds,projectId);
                }
            }
        }
        return "";
    }

    public static void countProject(CimDataSpace cds, String projectId) {
        countWithSum(cds,projectId);
        countByPerson(cds,projectId);
        countByUnit(cds,projectId);
        countByType(cds,projectId);
    }

    //统计总和
    public static void countWithSum(CimDataSpace cds,String projectId) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("projectId", projectId);
        map.put("timeoutAndUnchangedCount", 0);
        map.put("unSolvedCount", 0);
        map.put("matterCount", 0);
        map.put("seriousCount", 0);

        List<String> list = DbExecute.executeQuery(cds,existingProblemCount.replace("_PROJECTID_", projectId));
        for (int i = 0; i < list.size(); i++) {
            JSONObject obj = new JSONObject().fromObject(list.get(i));
            map.put("unSolvedCount", obj.getInt("num"));
            break;
        }

        list = DbExecute.executeQuery(cds,matterProblemCount.replace("_PROJECTID_", projectId));
        for (int i = 0; i < list.size(); i++) {
            JSONObject obj = new JSONObject().fromObject(list.get(i));
            map.put("matterCount", obj.getInt("num"));
            break;
        }


        list = DbExecute.executeQuery(cds,seriousProblemCount.replace("_PROJECTID_", projectId));
        for (int i = 0; i < list.size(); i++) {
            JSONObject obj = new JSONObject().fromObject(list.get(i));
            map.put("seriousCount", obj.getInt("num"));
            break;
        }

        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String now = sdf2.format((new Date()).getTime());
        String sql = outOfDateProblemCount.replace("_NOW_", now).replace("_PROJECTID_", projectId);
        list = DbExecute.executeQuery(cds,sql);
        for (int i = 0; i < list.size(); i++) {
            JSONObject obj = new JSONObject().fromObject(list.get(i));
            map.put("timeoutAndUnchangedCount", obj.getInt("num"));
            break;
        }

        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("projectId", projectId);
        try {
            updateOrInsertOneToCim(ObjectTypeIdConstant.SAFETY_PROBLEM_SUM, map, queryMap);
        } catch (DataServiceUserException e) {
            e.printStackTrace();
        }
    }

    //按整改人统计
    public static void countByPerson(CimDataSpace cds,String projectId) {
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String now = sdf2.format((new Date()).getTime());
        String sql = byPerson.replace("_NOW_", now).replace("_PROJECTID_", projectId);
        List<String> list = DbExecute.executeQuery(cds,sql);

        HashMap<String, HashMap<String, Object>> recordMap = new HashMap<String, HashMap<String, Object>>();
        for (int i = 0; i < list.size(); i++) {
            JSONObject obj = new JSONObject().fromObject(list.get(i));
            HashMap<String, Object> record = new HashMap<>();
            String key = obj.getString("changeId");
            int num = obj.getInt("num");
            if (!recordMap.containsKey(key)) {
                record.put("changeName", obj.getString("changeName"));
                record.put("changeId", obj.getString("changeId"));
                record.put("projectId", projectId);
                record.put("seriousCount", 0);
                record.put("normalCount", 0);
                record.put("matterCount", 0);
            } else {
                record = recordMap.get(key);
            }

            if (obj.getInt("problemLevel") == 1) {
                record.put("normalCount", num);
            }
            if (obj.getInt("problemLevel") == 2) {
                record.put("matterCount", num);
            }
            if (obj.getInt("problemLevel") == 3) {
                record.put("seriousCount", num);
            }
            recordMap.put(key, record);
        }

        for (String key : recordMap.keySet()) {
            HashMap<String, Object> map = recordMap.get(key);
            HashMap<String, Object> queryMap = new HashMap<>();
            queryMap.put("projectId", projectId);
            queryMap.put("changeId", key);
            try {
                updateOrInsertOneToCim(ObjectTypeIdConstant.SAFETY_PROBLEM_BY_DUTYPERSON, map, queryMap);
            } catch (DataServiceUserException e) {
                e.printStackTrace();
            }
        }
    }

    //按整改单位统计
    public static void countByUnit(CimDataSpace cds,String projectId) {
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String now = sdf2.format((new Date()).getTime());
        String sql = byUnit.replace("_NOW_", now).replace("_PROJECTID_", projectId);
        List<String> list = DbExecute.executeQuery(cds,sql);

        HashMap<String, HashMap<String, Object>> recordMap = new HashMap<String, HashMap<String, Object>>();
        for (int i = 0; i < list.size(); i++) {
            JSONObject obj = new JSONObject().fromObject(list.get(i));
            HashMap<String, Object> record = new HashMap<>();
            String key = obj.getString("unitId");
            int num = obj.getInt("num");
            if (!recordMap.containsKey(key)) {
                record.put("unitName", obj.getString("unitName"));
                record.put("unitId", obj.getString("unitId"));
                record.put("projectId", projectId);
                record.put("seriousCount", 0);
                record.put("normalCount", 0);
                record.put("matterCount", 0);

            } else {
                record = recordMap.get(key);
            }
            if (obj.getInt("problemLevel") == 1) {
                record.put("normalCount", num);
            }
            if (obj.getInt("problemLevel") == 2) {
                record.put("matterCount", num);
            }
            if (obj.getInt("problemLevel") == 3) {
                record.put("seriousCount", num);
            }
            recordMap.put(key, record);
        }

        for (String key : recordMap.keySet()) {
            HashMap<String, Object> map = recordMap.get(key);
            HashMap<String, Object> queryMap = new HashMap<>();
            queryMap.put("projectId", projectId);
            queryMap.put("unitId", key);
            try {
                updateOrInsertOneToCim(ObjectTypeIdConstant.SAFETY_PROBLEM_BY_SUBCONSTRATOR, map, queryMap);
            } catch (DataServiceUserException e) {
                e.printStackTrace();
            }
        }
    }


    //按问题类型进行统计
    public static void countByType(CimDataSpace cds,String projectId) {
        String today = getMonthRangeFormNow();
        String sql = byType.replace("_PROJECTID_", projectId);
        sql = sql.replace("_MONTH_", today);
        List<String> list = DbExecute.executeQuery(cds,sql);
        for (int i = 0; i < list.size(); i++) {
            JSONObject obj = new JSONObject().fromObject(list.get(i));
            HashMap<String, Object> map = new HashMap<>();
            map.put("projectId", projectId);
            if (obj.get("problemTypeId") != null && obj.getString("problemTypeId").toUpperCase().equals("null")) {
                map.put("safeProblemTypeId", obj.getInt("problemTypeId"));
            } else {
                map.put("safeProblemTypeId", 0);
            }
            map.put("safeProblemTypeName", obj.getString("problemTypeName"));
            if (((String) map.get("safeProblemTypeName")).toUpperCase().equals("null")) {
                map.put("safeProblemTypeName", "其他类型");
            }
            map.put("countProject", obj.getInt("num"));
            HashMap<String, Object> queryMap = new HashMap<>();
            queryMap.put("projectId", projectId);
            queryMap.put("safeProblemTypeId", map.get("safeProblemTypeId"));
            try {
                updateOrInsertOneToCim(ObjectTypeIdConstant.SAFETY_PROBLEM_TREND_BY_TYPE, map, queryMap);
            } catch (DataServiceUserException e) {
                e.printStackTrace();
            }
        }
    }


    //问题趋势分析
    public static void countByThend(CimDataSpace cds,String projectId) {
        String today = getDayRange();
        String sql = newProblem.replace("_PROJECTID_", projectId);
        sql = sql.replace("_TODAY_", today);
        List<String> list = DbExecute.executeQuery(cds,sql);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String day = sdf.format((new Date()).getTime());


        HashMap<String, Object> map = new HashMap<>();
        map.put("projectId", projectId);
        map.put("date", day);
        map.put("ariseCount", 0);
        map.put("fixedCount", 0);
        map.put("leftCount", 0);

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        map.put("dateTime", cal.getTime());


        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("projectId", projectId);
        queryMap.put("dateTime", cal.getTime());

        for (int i = 0; i < list.size(); i++) {
            JSONObject obj = new JSONObject().fromObject(list.get(i));
            map.put("ariseCount", obj.getInt("num"));
            break;
        }

        sql = closeProblem.replace("_PROJECTID_", projectId);
        sql = sql.replace("_TODAY_", today);
        list = DbExecute.executeQuery(cds,sql);
        for (int i = 0; i < list.size(); i++) {
            JSONObject obj = new JSONObject().fromObject(list.get(i));
            map.put("fixedCount", obj.getInt("num"));
            break;
        }

        sql = existingProblemCount.replace("_PROJECTID_", projectId);
        list = DbExecute.executeQuery(cds,sql);
        for (int i = 0; i < list.size(); i++) {
            JSONObject obj = new JSONObject().fromObject(list.get(i));
            map.put("leftCount", obj.getInt("num"));
            break;
        }
        try {
            updateOrInsertOneToCim(ObjectTypeIdConstant.SAFETY_PROBLEM_TREND, map, queryMap);
        } catch (DataServiceUserException e) {
            e.printStackTrace();
        }
    }
}
